import os.path
import re
import time
from urllib import urlencode
from urlparse import ParseResult
import urlparse

from requests import HTTPError, Request, Session
from requests.auth import HTTPDigestAuth


def add_intermediate(url, intermediate):
    if intermediate:
        url = urlparse.urlparse(url)
        queryDict = dict(urlparse.parse_qsl(url.query))
        newQuery = urlencode(queryDict)
        queryDict.update({'intermediate', intermediate})
        url = url._replace(query=newQuery)
    return url


class JobResult(object):

    def __init__(self, client, info, intermediate):
        self.client = client
        self.info = info
        self.url = info.get('resourceUri')
        self.intermediate = intermediate
    
    def response(self):
        resultUrl = add_intermediate(self.url, self.intermediate)
        request = Request('GET', resultUrl)
        prepared_request = self.client.session.prepare_request(request)  
        response = self.client.session.send(prepared_request)
        if response.status_code != 200:
            response.raise_for_status()
        return response

    def is_type(self, result_type):
        return result_type == self.info.get('batchJobResultType')

    
class Job(object):

    def __init__(self, client, url):
        self.client = client;
        self.url = url;
        self.status_response = {}
        self.id = re.sub('.+/', '', re.sub('/$', '', url))

    def cancel(self):
        url = self.url + 'cancel'
        response = self.client.session.post(url)
        response.stream = False

    def status(self):
        status = self.status_response.get('jobStatus')
        if 'resultsCreated' == status or 'downloadInitiated' == status:
            return self.status_response
        try:
            self.status_response = self.client.get_json_resource(self.url)
            return self.status_response
        except HTTPError as e:
            if e.errno == 404:
                return {}
            else:
                raise

    def is_completed(self, max_wait=None):
        if max_wait == None:
            max_wait = 24 * 60 * 60;  # 24 hours
        if (max_wait > 0):
            start_time = time.time()
            max_end_time = start_time + max_wait
            current_time = start_time
            while (current_time < max_end_time):
                status_map = self.status()
                status = status_map.get('jobStatus')
                if 'resultsCreated' == status or 'downloadInitiated' == status:
                    return True

                sleepTime = status_map.get('secondsToWaitForStatusCheck')
                if sleepTime == 0:
                    sleepTime = 1

                sleepTime = min(sleepTime, max_end_time - time.time())
                if sleepTime > 0:
                    time.sleep(sleepTime)
                current_time = time.time()
        status_map = self.status()
        status = status_map.get('status')
        return 'resultsCreated' == status

    def has_errors(self, max_wait=None):
        self.is_completed(max_wait)
        return self.status_response.get('numFailedRequests') > 0

    def delete(self):
        response = self.client.session.delete(self.url)
        if response.status_code != 200 and response.status_code != 404:
            response.raise_for_status()

    def results(self, max_wait=None, intermediate=False, result_type=None):
        if intermediate or self.is_completed(max_wait):
            url = add_intermediate(self.url + 'results/', intermediate)
            response = self.client.get_json_resource(url)
            resources = response.get('resources')
            results = []
            if resources:
                for resource in resources:
                    if result_type == None or result_type == resource.get('batchJobResultType'):
                        result = JobResult(self.client, resource, intermediate)
                        results.append(result)
            return results
        else:
            raise RuntimeError('Job results have not yet been created')

    def error_result(self, max_wait=None, intermediate=False):
        results = self.results(max_wait, intermediate, 'errorResultData')
        if (len(results) == 0):
            return None
        else:
            return results[0]

    def structured_result(self, max_wait=None, intermediate=False):
        results = self.results(max_wait, intermediate, 'structuredResultData')
        if (len(results) == 0):
            return None
        else:
            return results[0]

    def __str__(self):
        return self.url

    def __add__(self, other):
        return str(self) + other

    
class CpfClient(object):

    def __init__(self, url, username, password):
        url = re.sub('/+$', '', url)
        self.url = re.sub('(/ws)?/*$', '', url)
        self.session = Session()
        self.session.auth = HTTPDigestAuth(username, password)

    def create_job_with_opaque_resource_requests(self, app_name, jobParameters, inputDataContentType, result_content_type, opaqueRequests):
        url = self.__get_url(
            '/ws/apps/' + app_name + '/multiple/')

        headers = {}
        data = {  
            'numRequests' : len(opaqueRequests),
            'resultDataContentType' : result_content_type,
            'inputDataContentType': inputDataContentType,
             'media': 'application/json'
        }
        for inputData in opaqueRequests:
            data['inputData'] = inputData
            data['inputDataContentType'] = inputDataContentType

        request = Request('POST', url, headers=headers, data=data) 
        self.add_job_parameters(request, jobParameters)

        return self.__new_job(request)

    def create_job_multiple(self, app_name, input_data, input_data_content_type='text/csv', parameters=None, numRequests=None, result_content_type='text/csv'):
        url = self.__get_url('/ws/apps/' + app_name + '/multiple/')

        file_name = None
        input_data_url = None
        files = None
        data = {
            'inputDataContentType' : input_data_content_type
        }
        if numRequests:
            data['numRequests'] = numRequests
            
        if isinstance(input_data, str):
            if os.path.isfile(input_data):
                file_name = input_data;
            else:
                input_data_url_parsed = urlparse.urlparse(input_data)
                if input_data_url_parsed.scheme:
                    input_data = input_data_url_parsed
                else:
                    file_name = input_data
        
        if isinstance(input_data, ParseResult):
            scheme = input_data.scheme
            if scheme == 'http' or scheme == 'https':
                input_data_url = urlparse.urlunparse(input_data)
            elif scheme == 'file':
                file_name = input_data.path;
        
        if file_name:
            file_handle = open(file_name, 'rb')
            files = {'inputData': (input_data, file_handle, input_data_content_type, {'Expires': '0'})}
        elif input_data_url:
            data['inputDataUrl'] = urlparse.urlunparse(input_data)
        else:
            raise RuntimeError('Only a file path or http, https, file URLs are supported: ' + input_data.__str__())
        request = Request('POST', url, data=data, files=files)
        return self.__new_job(request, parameters, result_content_type)

    def create_job_single(self, app_name, parameters, result_content_type='text/csv'):
        url = self.__get_url('/ws/apps/' + app_name + '/single/')
        request = Request('POST', url, data={})
        return self.__new_job(request, parameters, result_content_type)

    def app_names(self):
        url = self.__get_url('/ws/apps/')
        result = self.get_json_resource(url)
        items = result.get('resources')
        app_names = []
        if items:
            for item in items:
                app_name = item.get('businessApplicationName')
                if app_name:
                    app_names.append(app_name)
        return app_names

    def app_spec_instant(self, app_name):
        url = self.__get_url(
            '/ws/apps/' + app_name + '/instant/?format=json&specification=true')
        return self.get_json_resource(url)

    def app_spec_multiple(self, app_name):
        url = self.__get_url(
            '/ws/apps/' + app_name + '/multiple/')
        return self.get_json_resource(url)

    def app_spec_single(self, app_name):
        url = self.__get_url(
            '/ws/apps/' + app_name + '/single/')
        return self.get_json_resource(url)

    def jobs(self, app_name=None):
        if app_name == None:
            path = '/ws/jobs/'
        else:
            path = '/ws/apps/' + app_name + '/jobs/'
        url = self.__get_url(path)
        response = self.get_json_resource(url)
        resources = response.get('resources')
        jobs = []
        if resources:
            for resource in resources:
                job_url = resource.get('batchJobUrl')
                if job_url:
                    job = Job(self, job_url)
                    jobs.append(job)
        return jobs

    def __get_url(self, path):
        return self.url + re.sub('/+', '/', path)

    def get_json_resource(self, url):
        headers = {'accept': 'application/json'}
        response = self.session.get(url, headers=headers)
        if response.status_code == 200:
            return response.json()
        else:
            response.raise_for_status()

    def __get_resource(self, url):
        response = self.session.get(url)
        if response.status_code == 200:
            return response
        else:
            response.raise_for_status()

    def __request_resource(self, request):
        preparedRequest = self.session.prepare_request(request)
        response = self.session.send(preparedRequest)
        if response.status_code >= 400:
            response.raise_for_status()
        else:
            return response

    def __new_job(self, request, parameters, result_content_type):
        request.headers['Accept'] = 'application/json'
        if parameters:
            for name in parameters.keys():
                value = parameters[name]
                request.data[name] = value
        request.data['resultDataContentType'] = result_content_type
        response = self.__request_resource(request)
        url = response.json().get('id')
        if url:
            return  Job(self, url)
        else:
            raise Exception('Job did not include an id parameter')

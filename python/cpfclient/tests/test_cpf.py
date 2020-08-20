import re
import time

import pytest

from context import cpfclient


def new_client():
    return cpfclient.CpfClient('http://localhost:8336/cpf/ws', 'cpftest', 'cpftest')

      
def callback_print(result):
    print result.response().text


def run_multiple(app_name, input_data, input_data_content_type='text/tab-separated-values', parameters={}):
    client = new_client()
    job = client.create_job_multiple(app_name, input_data, input_data_content_type, parameters=parameters)
    assert re.match('.+/jobs/\\d+/', job.url)
    try:
        time.sleep(0.5)
        if job.is_completed():
            structured_result = job.structured_result()
            if structured_result:
                print structured_result.response().text
            
            error_result = job.error_result()
            if error_result:
                print error_result.response().text
        else:
            pytest.fail('Job ' + job + ' did not complete in a timely manner')
    finally:
        job.delete()


class TestCpf:

    def test_get_business_application_names(self):
        client = new_client()
        names = client.app_names()
        assert 'MapTileByLocation' in names

    def test_get_business_application_specification(self):
        client = new_client()
        instantSpec = client.app_spec_instant('MapTileByLocation')
        assert instantSpec.get('businessApplicationName') == 'MapTileByLocation'
 
        multipleSpec = client.app_spec_multiple('MapTileByLocation')
        assert multipleSpec.get('businessApplicationName') == 'MapTileByLocation'

        singleSpec = client.app_spec_single('MapTileByLocation')
        assert singleSpec.get('businessApplicationName') == 'MapTileByLocation'

    def test_structured_single(self):
        client = new_client()
        job = client.create_job_single('MapTileByTileId', {'mapTileId':'92g025'})
        assert re.match('.+/jobs/\\d+/', job.url)
        try:
            time.sleep(0.5)
            if job.is_completed():
                structured_result = job.structured_result()
                if structured_result:
                    print structured_result.response().text
                
                error_result = job.error_result()
                if error_result:
                    print error_result.response().text
            else:
                pytest.fail('Job ' + job + ' did not complete in a timely manner')
        finally:
            job.delete()

    def test_structured_multiple(self):
        run_multiple('MapTileByTileId', 'tests/mapTile.tsv')

    def test_jobs(self):
        client = new_client()
        client.jobs()
        client.jobs('MapTileByLocation')

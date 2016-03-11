## Batch Job Processing

A Job in the CPF is a collection of requests to be processed by a business application on behalf
of a user. The CPF uses the business application to process each request in the job to create the 
results. When all requests in a job have been completed the user can download the results.

The following diagram shows all the steps required to process a job using the CPF. Boxes in blue
are client specific tasks and those in green are CPF web service API calls.

<div class="diagram"><img src="../images/jobProcessing.png" /></div>
 
Follow these instructions to see a demo of submitting a job to the CPF.

NOTE:  to use the demo.


1. Create the mapGrid.txt job request file on your local drive with the following contents.
```"mapGridName","numBoundaryPoints","mapTileId"
"NTS 1:250 000",10,"92j"
"BCGS 1:20 000",10,"92j016"
"BCGS 1:20 000",,"92j025"```
2. Using a web browser go to (https://apps.gov.bc.ca/pub/cpf/secure/ws/apps/MapTileByTileId/1.0.0/multiple/).
3. Enter the values for the following parameters.
<table>
<tbody>
<tr><th>Num Requests</th><td>3</td></tr>
<tr><th>Input Data Content Type</th><td>text/csv</td></tr>
<tr><th>Input Data</th><td>mapGrid.txt (select the file created in step #1)</td></tr>
<tr><th>Result Data Content Type</th><td>text/html (Try the others as well as a later exercise)</td></tr>
<tr><th>Map Grid Name</th><td>BCGS 1:20 000</td></tr>
<tr><th>Num Boundary Points</th><td>5</td></tr>
</tbody></table>
4. Click Submit, this is the submit job step.
5. After a few seconds the job status HTML page will be displayed. The Id is the URL to this page,
   which can be saved and used later to check the status of the job.
6. If the status page does not have a Results Url entry in the table refresh the page until it 
   appears. This is the Check Job Status step.
7. Click on the Results Url link to view the list of result files, this is the list result files 
   step.
8. If there were no errors there should be a single link on the result files page. Click this link 
   to download/view the results, this is the download result files step. The format of the result file 
   is the value entered in the Result Data Content Type on the job submission page. If there was an 
   error file it will always be returned as a CSV file.

In the above example the business application returned structured data records as the result of 
each request. The CPF merged these result records into a single file for download. Some business 
applications will return a binary file for each request (e.g. JPG, PNG, PDF). In this case there 
will be one file link on the results page for the result for each request in the job. The order of 
the result file links will be the same as the order of the requests in the input file.

## Structured Data
Many business applications either accept structured data for the input or
output data for the requests.

For structured input data the input data is specified in a structured data
file that contains one record for each request in the batch job. Each record 
contains the input parameters for that request. The records can be specified in 
any of the supported <a href="fileFormats.html">file formats</a>.

Structured data is a record that contains a value for a named attribute, each 
named attribute can have a <a href="dataTypes.html">data type</a> and other
metadata such as maximum length or decimal places. For example a geo-coder may
have the address String attribute in the input data and the location Point
attribute in the result data.

>For structured result data a single result file is created containing one 
record for the result of each successful request.

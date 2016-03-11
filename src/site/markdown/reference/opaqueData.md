## Opaque Data
Opaque data is a file of data that is passed directly to the business
application without any processing by the CPF. For example a business application
could convert an input GIF image to a JPEG image, or a web map service could
return an PNG image of the requested map.

For business applications that support opaque input data, one file is specified 
for each request to be processed (e.g. a GIF image). A business application
cannot support both opaque and structured input data. But it can support batch
job parameters.

For business applications that support opaque result data, one result file is 
generated (e.g. a GIF image) for each request. A business application cannot 
support both opaque and structured result data.

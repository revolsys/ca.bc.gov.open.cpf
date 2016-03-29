## Data Types
The CPF supports common data types that can be used for batch job parameters,
[structured data](structuredData.html) parameters, and result fields
for a business application. The CPF does not support custom data types or Enums,
these must be converted to/from one of the standard data types by the business
application.

If the [File Format](fileFormats.html) supports the data type then
the value must be encoded according to that file format.

Otherwise the values must be converted to a string representation using the
following rules.

* UTF-8 or US-ASCII encoding must be used for strings and text files.
* Numbers will use the sign (-) prefix for negative numbers, the digits (0-9) for the integer
  portion, a period ('.') for decimal numbers, and the digits (0-9) for the fractional part. For
  example -1234.56789.
* Booleans use the values true and false.
* Geometries can be encoded using OGC [Well-Known Text](http://en.wikipedia.org/wiki/Well-known_text) (WKT)
  or PostGIS [Extended-WKT](http://postgis.net/docs/manual-2.0/using_postgis_dbmanagement.html#EWKB_EWKT) (EWKT).
  For example <code>SRID=4326;POINT(-124 50)</code>.

Internally the CPF converts all values to the string encoded form.

The table below shows the supported data types.

<div class="table-responsive">
<table class="table table-condensed table-striped tabled-bordered">
  <thead>
    <tr>
      <th>Data Type</th>
      <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr id="boolean">
      <td>boolean</td>
      <td>true or false.</td>
    </tr>
    <tr id="byte">
      <td>byte</td>
      <td>8-bit signed number.</td>
    </tr>
    <tr id="short">
      <td>short</td>
      <td>16-bit signed number.</td>
    </tr>
    <tr id="int">
      <td>int</td>
      <td>32-bit signed number.</td>
    </tr>
    <tr id="long">
      <td>long</td>
      <td>64-bit signed number.</td>
    </tr>
    <tr id="float">
      <td>float</td>
      <td>32-bit IEEE 754 floating point number.</td>
    </tr>
    <tr id="double">
      <td>double</td>
      <td>64-bit IEEE 754 floating point number.</td>
    </tr>
    <tr id="String">
      <td>String</td>
      <td>A character string.</td>
    </tr>
    <tr id="Date">
      <td>Date</td>
      <td>A Date in the format yyyy-MM-dd.</td>
    </tr>
    <tr id="DateTime">
      <td>DateTime</td>
      <td>A Date with millisecond precision time in the format yyyy-MM-dd HH:mm:ss.SSS format.</td>
    </tr>
    <tr id="Timestamp">
      <td>DateTime</td>
      <td>A Date with nanosecond precision time in the format yyyy-MM-dd HH:mm:ss.SSSSSSSSS format.</td>
    </tr>
    <tr id="URL">
      <td>URL</td>
      <td>A URL or the string encoding of a URL.</td>
    </tr>
    <tr id="Point">
      <td>Point</td>
      <td>A Point geometry.</td>
    </tr>
    <tr id="LineString">
      <td>LineString</td>
      <td>A LineString geometry.</td>
    </tr>
    <tr id="Polygon">
      <td>Polygon</td>
      <td>A Polygon geometry.</td>
    </tr>
    <tr id="MultiPoint">
      <td>MultiPoint</td>
      <td>A MultiPoint geometry.</td>
    </tr>
    <tr id="MultiLineString">
      <td>MultiLineString</td>
      <td>A MultiLineString geometry.</td>
    </tr>
    <tr id="MultiPolygon">
      <td>MultiPolygon</td>
      <td>A MultiPolygon geometry.</td>
    </tr>
  </tbody>
</table>
</div>
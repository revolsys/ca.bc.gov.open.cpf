package ca.bc.gov.open.cpg.test.plugin;

import java.net.URL;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import ca.bc.gov.open.cpf.plugin.api.BusinessApplicationPlugin;
import ca.bc.gov.open.cpf.plugin.api.DefaultValue;
import ca.bc.gov.open.cpf.plugin.api.RequestParameter;
import ca.bc.gov.open.cpf.plugin.api.ResultAttribute;

import com.vividsolutions.jts.geom.Geometry;

@BusinessApplicationPlugin(
    numRequestsPerWorker = 100,
    instantModePermission = "permitAll",
    description = "Test plug-in to validate handling of all supported data types.")
public class TestAllDataTypes {

  private Boolean bool;

  private java.sql.Date date;

  private Date dateTime;

  private Float float32;

  private Double float64;

  private Geometry geometry;

  private Short int16;

  private Integer int32;

  private Long int64;

  private Byte int8;

  private String string;

  private Timestamp timestamp;

  private URL url;

  public void execute() {
  }

  @ResultAttribute(index = 1)
  public Boolean getBool() {
    return this.bool;
  }

  public Map<String, Object> getCustomizationProperties() {
    final Map<String, Object> properties = new HashMap<String, Object>();
    properties.put("kmlWriteNulls", true);
    return properties;
  }

  @ResultAttribute(index = 9)
  public java.sql.Date getDate() {
    return this.date;
  }

  @ResultAttribute(index = 10)
  public Date getDateTime() {
    return this.dateTime;
  }

  @ResultAttribute(index = 6)
  public Float getFloat32() {
    return this.float32;
  }

  @ResultAttribute(index = 7)
  public Double getFloat64() {
    return this.float64;
  }

  @ResultAttribute(index = 12)
  public Geometry getGeometry() {
    return geometry;
  }

  @ResultAttribute(index = 3)
  public Short getInt16() {
    return this.int16;
  }

  @ResultAttribute(index = 4)
  public Integer getInt32() {
    return this.int32;
  }

  @ResultAttribute(index = 5)
  public Long getInt64() {
    return this.int64;
  }

  @ResultAttribute(index = 2)
  public Byte getInt8() {
    return this.int8;
  }

  @ResultAttribute(index = 14)
  public String getNull() {
    return null;
  }

  @ResultAttribute(index = 8)
  public String getString() {
    return this.string;
  }

  @ResultAttribute(index = 11)
  public Timestamp getTimestamp() {
    return this.timestamp;
  }

  @ResultAttribute(index = 13)
  public URL getUrl() {
    return url;
  }

  @RequestParameter(index = 1)
  @DefaultValue("true")
  public void setBool(final Boolean bool) {
    this.bool = bool;
  }

  @RequestParameter(index = 9)
  @DefaultValue("2000-01-23")
  public void setDate(final java.sql.Date date) {
    this.date = date;
  }

  @RequestParameter(index = 10)
  @DefaultValue("2000-01-23 01:23:45.123")
  public void setDateTime(final Date dateTime) {
    this.dateTime = dateTime;
  }

  @RequestParameter(index = 6)
  @DefaultValue("1234.567")
  public void setFloat32(final Float float32) {
    this.float32 = float32;
  }

  @RequestParameter(index = 7)
  @DefaultValue("7654.321")
  public void setFloat64(final Double float64) {
    this.float64 = float64;
  }

  @RequestParameter(index = 12)
  @DefaultValue("SRID=4326;POINT(-126 52)")
  public void setGeometry(final Geometry geometry) {
    this.geometry = geometry;
  }

  @RequestParameter(index = 3)
  @DefaultValue("32767")
  public void setInt16(final Short int16) {
    this.int16 = int16;
  }

  @RequestParameter(index = 4)
  @DefaultValue("2147483647")
  public void setInt32(final Integer int32) {
    this.int32 = int32;
  }

  @RequestParameter(index = 5)
  @DefaultValue("9223372036854775807")
  public void setInt64(final Long int64) {
    this.int64 = int64;
  }

  @RequestParameter(index = 2)
  @DefaultValue("127")
  public void setInt8(final Byte int8) {
    this.int8 = int8;
  }

  @RequestParameter(index = 8)
  @DefaultValue("test string")
  public void setString(final String string) {
    this.string = string;
  }

  @RequestParameter(index = 11)
  @DefaultValue("2000-01-23 01:23:45.123456789")
  public void setTimestamp(final Timestamp timestamp) {
    this.timestamp = timestamp;
  }

  @RequestParameter(index = 13)
  @DefaultValue("http://www.google.com/")
  public void setUrl(final URL url) {
    this.url = url;
  }

}

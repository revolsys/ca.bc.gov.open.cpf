package ca.bc.gov.open.cpf.plugin.samples;

import java.util.Collections;
import java.util.Map;

import ca.bc.gov.open.cpf.plugin.api.ResultAttribute;

import com.revolsys.io.kml.Kml22Constants;
import com.vividsolutions.jts.geom.Point;

public class BcGeocoderResult {
  private String unitNumber;

  private String civicNumber;

  private String civicNumberSuffix;

  private String streetName;

  private String streetType;

  private String streetDirection;

  private String city;

  private String province;

  private String score;

  private String precision;

  private String address;

  private Point addressPoint;

  private String x;

  private String y;

  private String srsCode;

  private String spatialReferenceSystemName;

  public Map<String, ? extends Object> getCustomizationProperties() {
    return Collections.singletonMap(
      Kml22Constants.PLACEMARK_NAME_ATTRIBUTE_PROPERTY, "address");
  }

  @ResultAttribute(index = 11)
  public String getAddress() {
    return address;
  }

  @ResultAttribute(index = 16)
  public Point getAddressPoint() {
    return addressPoint;
  }

  @ResultAttribute(index = 7)
  public String getCity() {
    return city;
  }

  @ResultAttribute(index = 2)
  public String getCivicNumber() {
    return civicNumber;
  }

  @ResultAttribute(index = 3)
  public String getCivicNumberSuffix() {
    return civicNumberSuffix;
  }

  @ResultAttribute(index = 10)
  public String getPrecision() {
    return precision;
  }

  @ResultAttribute(index = 8)
  public String getProvince() {
    return province;
  }

  @ResultAttribute(index = 9)
  public String getScore() {
    return score;
  }

  @ResultAttribute(index = 15)
  public String getSpatialReferenceSystemName() {
    return spatialReferenceSystemName;
  }

  @ResultAttribute(index = 14)
  public String getSrsCode() {
    return srsCode;
  }

  @ResultAttribute(index = 6)
  public String getStreetDirection() {
    return streetDirection;
  }

  @ResultAttribute(index = 4)
  public String getStreetName() {
    return streetName;
  }

  @ResultAttribute(index = 5)
  public String getStreetType() {
    return streetType;
  }

  @ResultAttribute(index = 1)
  public String getUnitNumber() {
    return unitNumber;
  }

  @ResultAttribute(index = 12)
  public String getX() {
    return x;
  }

  @ResultAttribute(index = 13)
  public String getY() {
    return y;
  }

  public void setAddress(final String address) {
    this.address = address;
  }

  public void setAddressPoint(final Point addressPoint) {
    this.addressPoint = addressPoint;
  }

  public void setCity(final String city) {
    this.city = city;
  }

  public void setCivicNumber(final String civicNumber) {
    this.civicNumber = civicNumber;
  }

  public void setCivicNumberSuffix(final String civicNumberSuffix) {
    this.civicNumberSuffix = civicNumberSuffix;
  }

  public void setPrecision(final String precision) {
    this.precision = precision;
  }

  public void setProvince(final String province) {
    this.province = province;
  }

  public void setScore(final String score) {
    this.score = score;
  }

  public void setSpatialReferenceSystemName(
    final String spatialReferenceSystemName) {
    this.spatialReferenceSystemName = spatialReferenceSystemName;
  }

  public void setSrsCode(final String srsCode) {
    this.srsCode = srsCode;
  }

  public void setStreetDirection(final String streetDirection) {
    this.streetDirection = streetDirection;
  }

  public void setStreetName(final String streetName) {
    this.streetName = streetName;
  }

  public void setStreetType(final String streetType) {
    this.streetType = streetType;
  }

  public void setUnitNumber(final String unitNumber) {
    this.unitNumber = unitNumber;
  }

  public void setX(final String x) {
    this.x = x;
  }

  public void setY(final String y) {
    this.y = y;
  }

}

package ca.bc.gov.open.cpf.plugin.api.test;

import java.util.List;
import java.util.Map;

import ca.bc.gov.open.cpf.plugin.api.BusinessApplicationPlugin;
import ca.bc.gov.open.cpf.plugin.api.JobParameter;
import ca.bc.gov.open.cpf.plugin.api.RequestParameter;
import ca.bc.gov.open.cpf.plugin.api.ResultAttribute;

@BusinessApplicationPlugin(name = "StructuredToStructured", version = "1.0.0")
public class StructuredToStructured {
  private List<Boolean> booleanListParameter;

  private Map<String, Boolean> booleanMapParameter;

  private Boolean booleanObjectParameter;

  private boolean booleanParameter;

  private List<Byte> byteListParameter;

  private Map<String, Byte> byteMapParameter;

  private Byte byteObjectParameter;

  private byte byteParameter;

  private List<Double> doubleListParameter;

  private Map<String, Double> doubleMapParameter;

  private Double doubleObjectParameter;

  private double doubleParameter;

  private List<Float> floatListParameter;

  private Map<String, Float> floatMapParameter;

  private Float floatObjectParameter;

  private float floatParameter;

  private List<Integer> intListParameter;

  private Map<String, Integer> intMapParameter;

  private Integer intObjectParameter;

  private int intParameter;

  private List<Long> longListParameter;

  private Map<String, Long> longMapParameter;

  private Long longObjectParameter;

  private long longParameter;

  private List<Short> shortListParameter;

  private Map<String, Short> shortMapParameter;

  private Short shortObjectParameter;

  private short shortParameter;

  private List<String> stringListParameter;

  private Map<String, String> stringMapParameter;

  private String stringParameter;

  public void execute() {

  }

  @ResultAttribute
  public List<Boolean> getBooleanListParameter() {
    return booleanListParameter;
  }

  @ResultAttribute
  public Map<String, Boolean> getBooleanMapParameter() {
    return booleanMapParameter;
  }

  @ResultAttribute
  public Boolean getBooleanObjectParameter() {
    return booleanObjectParameter;
  }

  @ResultAttribute
  public List<Byte> getByteListParameter() {
    return byteListParameter;
  }

  @ResultAttribute
  public Map<String, Byte> getByteMapParameter() {
    return byteMapParameter;
  }

  @ResultAttribute
  public Byte getByteObjectParameter() {
    return byteObjectParameter;
  }

  @ResultAttribute
  public byte getByteParameter() {
    return byteParameter;
  }

  @ResultAttribute
  public List<Double> getDoubleListParameter() {
    return doubleListParameter;
  }

  @ResultAttribute
  public Map<String, Double> getDoubleMapParameter() {
    return doubleMapParameter;
  }

  @ResultAttribute
  public Double getDoubleObjectParameter() {
    return doubleObjectParameter;
  }

  @ResultAttribute
  public double getDoubleParameter() {
    return doubleParameter;
  }

  @ResultAttribute
  public List<Float> getFloatListParameter() {
    return floatListParameter;
  }

  @ResultAttribute
  public Map<String, Float> getFloatMapParameter() {
    return floatMapParameter;
  }

  @ResultAttribute
  public Float getFloatObjectParameter() {
    return floatObjectParameter;
  }

  @ResultAttribute
  public float getFloatParameter() {
    return floatParameter;
  }

  @ResultAttribute
  public List<Integer> getIntListParameter() {
    return intListParameter;
  }

  @ResultAttribute
  public Map<String, Integer> getIntMapParameter() {
    return intMapParameter;
  }

  @ResultAttribute
  public Integer getIntObjectParameter() {
    return intObjectParameter;
  }

  @ResultAttribute
  public int getIntParameter() {
    return intParameter;
  }

  @ResultAttribute
  public List<Long> getLongListParameter() {
    return longListParameter;
  }

  @ResultAttribute
  public Map<String, Long> getLongMapParameter() {
    return longMapParameter;
  }

  @ResultAttribute
  public Long getLongObjectParameter() {
    return longObjectParameter;
  }

  @ResultAttribute
  public long getLongParameter() {
    return longParameter;
  }

  @ResultAttribute
  public List<Short> getShortListParameter() {
    return shortListParameter;
  }

  @ResultAttribute
  public Map<String, Short> getShortMapParameter() {
    return shortMapParameter;
  }

  @ResultAttribute
  public Short getShortObjectParameter() {
    return shortObjectParameter;
  }

  @ResultAttribute
  public short getShortParameter() {
    return shortParameter;
  }

  @ResultAttribute
  public List<String> getStringListParameter() {
    return stringListParameter;
  }

  @ResultAttribute
  public Map<String, String> getStringMapParameter() {
    return stringMapParameter;
  }

  @ResultAttribute
  public String getStringParameter() {
    return stringParameter;
  }

  @ResultAttribute
  public boolean isBooleanParameter() {
    return booleanParameter;
  }

  @JobParameter
  @RequestParameter
  public void setBooleanListParameter(final List<Boolean> booleanListParameter) {
    this.booleanListParameter = booleanListParameter;
  }

  @JobParameter
  @RequestParameter
  public void setBooleanMapParameter(
    final Map<String, Boolean> booleanMapParameter) {
    this.booleanMapParameter = booleanMapParameter;
  }

  @JobParameter
  @RequestParameter
  public void setBooleanObjectParameter(final Boolean booleanObjectParameter) {
    this.booleanObjectParameter = booleanObjectParameter;
  }

  @JobParameter
  @RequestParameter
  public void setBooleanParameter(final boolean booleanParameter) {
    this.booleanParameter = booleanParameter;
  }

  @JobParameter
  @RequestParameter
  public void setByteListParameter(final List<Byte> byteListParameter) {
    this.byteListParameter = byteListParameter;
  }

  @JobParameter
  @RequestParameter
  public void setByteMapParameter(final Map<String, Byte> byteMapParameter) {
    this.byteMapParameter = byteMapParameter;
  }

  @JobParameter
  @RequestParameter
  public void setByteObjectParameter(final Byte byteObjectParameter) {
    this.byteObjectParameter = byteObjectParameter;
  }

  @JobParameter
  @RequestParameter
  public void setByteParameter(final byte byteParameter) {
    this.byteParameter = byteParameter;
  }

  @JobParameter
  @RequestParameter
  public void setDoubleListParameter(final List<Double> doubleListParameter) {
    this.doubleListParameter = doubleListParameter;
  }

  @JobParameter
  @RequestParameter
  public void setDoubleMapParameter(final Map<String, Double> doubleMapParameter) {
    this.doubleMapParameter = doubleMapParameter;
  }

  @JobParameter
  @RequestParameter
  public void setDoubleObjectParameter(final Double doubleObjectParameter) {
    this.doubleObjectParameter = doubleObjectParameter;
  }

  @JobParameter
  @RequestParameter
  public void setDoubleParameter(final double doubleParameter) {
    this.doubleParameter = doubleParameter;
  }

  @JobParameter
  @RequestParameter
  public void setFloatListParameter(final List<Float> floatListParameter) {
    this.floatListParameter = floatListParameter;
  }

  @JobParameter
  @RequestParameter
  public void setFloatMapParameter(final Map<String, Float> floatMapParameter) {
    this.floatMapParameter = floatMapParameter;
  }

  @JobParameter
  @RequestParameter
  public void setFloatObjectParameter(final Float floatObjectParameter) {
    this.floatObjectParameter = floatObjectParameter;
  }

  @JobParameter
  @RequestParameter
  public void setFloatParameter(final float floatParameter) {
    this.floatParameter = floatParameter;
  }

  @JobParameter
  @RequestParameter
  public void setIntListParameter(final List<Integer> intListParameter) {
    this.intListParameter = intListParameter;
  }

  @JobParameter
  @RequestParameter
  public void setIntMapParameter(final Map<String, Integer> intMapParameter) {
    this.intMapParameter = intMapParameter;
  }

  @JobParameter
  @RequestParameter
  public void setIntObjectParameter(final Integer intObjectParameter) {
    this.intObjectParameter = intObjectParameter;
  }

  @JobParameter
  @RequestParameter
  public void setIntParameter(final int intParameter) {
    this.intParameter = intParameter;
  }

  @JobParameter
  @RequestParameter
  public void setLongListParameter(final List<Long> longListParameter) {
    this.longListParameter = longListParameter;
  }

  @JobParameter
  @RequestParameter
  public void setLongMapParameter(final Map<String, Long> longMapParameter) {
    this.longMapParameter = longMapParameter;
  }

  @JobParameter
  @RequestParameter
  public void setLongObjectParameter(final Long longObjectParameter) {
    this.longObjectParameter = longObjectParameter;
  }

  @JobParameter
  @RequestParameter
  public void setLongParameter(final long longParameter) {
    this.longParameter = longParameter;
  }

  @JobParameter
  @RequestParameter
  public void setShortListParameter(final List<Short> shortListParameter) {
    this.shortListParameter = shortListParameter;
  }

  @JobParameter
  @RequestParameter
  public void setShortMapParameter(final Map<String, Short> shortMapParameter) {
    this.shortMapParameter = shortMapParameter;
  }

  @JobParameter
  @RequestParameter
  public void setShortObjectParameter(final Short shortObjectParameter) {
    this.shortObjectParameter = shortObjectParameter;
  }

  @JobParameter
  @RequestParameter
  public void setShortParameter(final short shortParameter) {
    this.shortParameter = shortParameter;
  }

  @JobParameter
  @RequestParameter
  public void setStringListParameter(final List<String> stringListParameter) {
    this.stringListParameter = stringListParameter;
  }

  @JobParameter
  @RequestParameter
  public void setStringMapParameter(final Map<String, String> stringMapParameter) {
    this.stringMapParameter = stringMapParameter;
  }

  @JobParameter
  @RequestParameter
  public void setStringParameter(final String stringParameter) {
    this.stringParameter = stringParameter;
  }
}

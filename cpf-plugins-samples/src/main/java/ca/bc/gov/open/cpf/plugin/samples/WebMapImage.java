package ca.bc.gov.open.cpf.plugin.samples;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import ca.bc.gov.open.cpf.plugin.api.BusinessApplicationPlugin;
import ca.bc.gov.open.cpf.plugin.api.JobParameter;
import ca.bc.gov.open.cpf.plugin.api.RequestParameter;
import ca.bc.gov.open.cpf.plugin.api.Required;

import com.revolsys.io.FileUtil;
import com.revolsys.util.UrlUtil;

@BusinessApplicationPlugin(
    name = "WebMapImage",
    version = "1.0.0",
    perRequestResultData = true,
    resultDataContentTypes = {
      "image/png", "image/gif"
    },
    instantModePermission = "permitAll")
public class WebMapImage {
  private String crs;

  private int height;

  private String layers;

  private double maxX;

  private double maxY;

  private double minX;

  private double minY;

  private OutputStream resultData;

  private String resultDataContentType;

  private String styles;

  private int width;

  private String wmsUrl;

  public void execute() {
    final Map<String, Object> parameters = new LinkedHashMap<String, Object>();
    parameters.put("SERVICE", "WMS");
    parameters.put("VERSION", "1.1.1");
    parameters.put("REQUEST", "GetMap");
    parameters.put("LAYERS", layers);
    parameters.put("STYLES", styles);
    parameters.put("CRS", crs);
    parameters.put("SRS", crs);
    parameters.put("BBOX", minX + "," + minY + "," + maxX + "," + maxY);
    parameters.put("WIDTH", width);
    parameters.put("HEIGHT", height);
    parameters.put("FORMAT", resultDataContentType);
    parameters.put("EXCEPTIONS", "INIMAGE");
    final String url = UrlUtil.getUrl(wmsUrl, parameters);
    try {
      final InputStream in = new URL(url).openStream();
      try {
        FileUtil.copy(in, resultData);
      } finally {
        FileUtil.closeSilent(resultData);
        FileUtil.closeSilent(in);
      }
    } catch (final MalformedURLException e) {
      throw new IllegalArgumentException(url + " is not valid URL");
    } catch (final IOException e) {
      throw new RuntimeException("Unable to get map", e);
    }

  }

  public String getCrs() {
    return crs;
  }

  public int getHeight() {
    return height;
  }

  public String getLayers() {
    return layers;
  }

  public double getMaxX() {
    return maxX;
  }

  public double getMaxY() {
    return maxY;
  }

  public double getMinX() {
    return minX;
  }

  public double getMinY() {
    return minY;
  }

  public String getResultDataContentType() {
    return resultDataContentType;
  }

  public String getStyles() {
    return styles;
  }

  public int getWidth() {
    return width;
  }

  public String getWmsUrl() {
    return wmsUrl;
  }

  @Required
  @JobParameter
  @RequestParameter
  public void setCrs(final String crs) {
    this.crs = crs;
  }

  @Required
  @JobParameter
  @RequestParameter
  public void setHeight(final int height) {
    this.height = height;
  }

  @Required
  @JobParameter
  @RequestParameter
  public void setLayers(final String layers) {
    this.layers = layers;
  }

  @Required
  @RequestParameter
  public void setMaxX(final double maxX) {
    this.maxX = maxX;
  }

  @Required
  @RequestParameter
  public void setMaxY(final double maxY) {
    this.maxY = maxY;
  }

  @Required
  @RequestParameter
  public void setMinX(final double minX) {
    this.minX = minX;
  }

  @Required
  @RequestParameter
  public void setMinY(final double minY) {
    this.minY = minY;
  }

  public void setResultData(final OutputStream resultData) {
    this.resultData = resultData;
  }

  public void setResultDataContentType(final String format) {
    this.resultDataContentType = format;
  }

  @JobParameter
  @RequestParameter
  public void setStyles(final String styles) {
    this.styles = styles;
  }

  @Required
  @JobParameter
  @RequestParameter
  public void setWidth(final int width) {
    this.width = width;
  }

  @Required
  @JobParameter
  @RequestParameter
  public void setWmsUrl(final String wmsUrl) {
    this.wmsUrl = wmsUrl;
  }
}

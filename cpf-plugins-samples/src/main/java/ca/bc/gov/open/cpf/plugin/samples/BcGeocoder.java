package ca.bc.gov.open.cpf.plugin.samples;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.beanutils.BeanUtils;
import org.springframework.core.io.Resource;

import ca.bc.gov.open.cpf.plugin.api.BusinessApplicationPlugin;
import ca.bc.gov.open.cpf.plugin.api.DefaultValue;
import ca.bc.gov.open.cpf.plugin.api.RequestParameter;
import ca.bc.gov.open.cpf.plugin.api.Required;
import ca.bc.gov.open.cpf.plugin.api.ResultList;

import com.revolsys.gis.cs.GeometryFactory;
import com.revolsys.gis.model.coordinates.DoubleCoordinates;
import com.revolsys.io.AbstractMapReaderFactory;
import com.revolsys.io.Reader;
import com.revolsys.spring.InputStreamResource;
import com.revolsys.util.MathUtil;
import com.revolsys.util.UrlUtil;

@BusinessApplicationPlugin(name = "BcGeocoder", version = "1.0.0")
public class BcGeocoder {

  private String geoCoderUrl = "http://delivery.apps.gov.bc.ca/pub/app_gc/lookup.do";

  private String query;

  private int maxResults = 5;

  private ArrayList<BcGeocoderResult> results;

  private boolean echo;

  public void execute() {
    final Map<String, Object> parameters = new LinkedHashMap<String, Object>();
    parameters.put("address", query);
    parameters.put("outputSrs", "4326");
    parameters.put("outputFormat", "csv");
    parameters.put("maxResults", maxResults);

    final String url = UrlUtil.getUrl(geoCoderUrl, parameters);
    try {
      final Resource resource = new InputStreamResource("t.csv",
        UrlUtil.getUrl(url).openStream());
      final Reader<Map<String, Object>> reader = AbstractMapReaderFactory.mapReader(resource);
      results = new ArrayList<BcGeocoderResult>();
      for (final Map<String, Object> map : reader) {
        final BcGeocoderResult record = new BcGeocoderResult();
        for (final Entry<String, Object> field : map.entrySet()) {
          final String key = field.getKey();
          Object value = field.getValue();
          if (key.equals("addressPoint")) {
            final double[] coordinates = MathUtil.toDoubleArraySplit(
              value.toString(), " ");
            final DoubleCoordinates point = new DoubleCoordinates(coordinates);
            final int srid = Integer.parseInt(map.get("srsCode").toString());
            value = GeometryFactory.getFactory(srid).createPoint(point);
          }
          try {
            BeanUtils.setProperty(record, key, value);
          } catch (final Throwable e) {
          }
        }
        results.add(record);
      }
    } catch (final MalformedURLException e) {
      throw new IllegalArgumentException(url + " is not valid URL");
    } catch (final IOException e) {
      throw new IllegalArgumentException("Unable to geocode from " + url);
    }
  }

  public String getGeoCoderUrl() {
    return geoCoderUrl;
  }

  @ResultList
  public List<BcGeocoderResult> getResults() {
    return results;
  }

  @DefaultValue("true")
  @RequestParameter
  public void setEcho(final boolean echo) {
    this.echo = echo;
  }

  public void setGeoCoderUrl(final String geoCoderUrl) {
    this.geoCoderUrl = geoCoderUrl;
  }

  @RequestParameter
  public void setMaxResults(final int maxResults) {
    this.maxResults = maxResults;
  }

  @Required
  @RequestParameter
  public void setQuery(final String query) {
    this.query = query;
  }

}

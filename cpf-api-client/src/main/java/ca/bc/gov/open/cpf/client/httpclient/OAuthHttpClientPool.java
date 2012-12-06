package ca.bc.gov.open.cpf.client.httpclient;

import java.util.HashSet;
import java.util.Set;

public class OAuthHttpClientPool {

  private String webServiceUrl;

  private String consumerKey;

  private String consumerSecret;

  private Set<OAuthHttpClient> clients = new HashSet<OAuthHttpClient>();

  private int maxConnections = 10;

  public OAuthHttpClientPool() {
  }

  public OAuthHttpClientPool(final String webServiceUrl,
    final String consumerKey, final String consumerSecret, int maxConnections) {
    this.webServiceUrl = webServiceUrl;
    this.consumerKey = consumerKey;
    this.consumerSecret = consumerSecret;
    this.maxConnections = maxConnections;
  }

  public OAuthHttpClient getClient() {
    synchronized (clients) {
      while (clients != null && clients.size() >= maxConnections) {
        try {
          clients.wait();
        } catch (final InterruptedException e) {
        }
      }
      if (clients == null) {
        throw new IllegalStateException("Connection pool closed");
      } else {
        OAuthHttpClient client = new OAuthHttpClient(this, webServiceUrl,
          consumerKey, consumerSecret);
        return client;
      }
    }
  }

  public String getConsumerKey() {
    return consumerKey;
  }

  public String getConsumerSecret() {
    return consumerSecret;
  }

  public int getMaxConnections() {
    return maxConnections;
  }

  public String getWebServiceUrl() {
    return webServiceUrl;
  }

  public void releaseClient(final OAuthHttpClient client) {
    synchronized (clients) {
      if (client != null) {
        client.getConnectionManager().shutdown();
        if (clients != null) {
          clients.remove(client);
          clients.notifyAll();
        }
      }
    }
  }

  public void setConsumerKey(final String consumerKey) {
    this.consumerKey = consumerKey;
  }

  public void setConsumerSecret(final String consumerSecret) {
    this.consumerSecret = consumerSecret;
  }

  public void setMaxConnections(final int maxConnections) {
    synchronized (clients) {
      this.maxConnections = maxConnections;
      clients.notifyAll();
    }
  }

  public void setWebServiceUrl(final String webServiceUrl) {
    this.webServiceUrl = webServiceUrl;
  }

  public void close() {
    synchronized (clients) {
      for (OAuthHttpClient client : clients) {
        client.close();
      }
      clients = null;
      clients.notifyAll();
    }
  }
}

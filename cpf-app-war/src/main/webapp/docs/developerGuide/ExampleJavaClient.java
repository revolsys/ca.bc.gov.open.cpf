package ExampleJavaClient;

import java.util.Arrays;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import com.revolsys.security.oauth.OAuthCredentials;
import com.revolsys.security.oauth.OAuthSchemeFactory;

public class ExampleJavaClient extends TestCase {

   public void quickTest() throws Exception {
      HttpContext context = new BasicHttpContext();
      context.setAttribute(ClientContext.AUTH_SCHEME_PREF,
Arrays.asList("oauth"));

     DefaultHttpClient httpclient = new DefaultHttpClient();

     httpclient.getAuthSchemes().register("oauth", new OAuthSchemeFactory());

     httpclient.getCredentialsProvider().setCredentials(
       new AuthScope("http://bpf.bcgov.revolsys.com", 80),
       new OAuthCredentials("ec1e0b2b-0f3e-401d-844d-0a649eca19d0",
         "559a4162-8167-4eca-9010-603ad707ecbf"));

     HttpGet users = new HttpGet("http://bpf.bcgov.revolsys.com/ws/");

     HttpResponse response = httpclient.execute(users, context);
     HttpEntity entity = response.getEntity();

     IOUtils.copy(entity.getContent(), System.out);
   }

   public static void main(String[] args) throws Exception {
     new ExampleJavaClient().quickTest();
   }
}

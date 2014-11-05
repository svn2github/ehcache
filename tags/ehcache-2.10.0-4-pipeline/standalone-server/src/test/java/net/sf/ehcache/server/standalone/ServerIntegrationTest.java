package net.sf.ehcache.server.standalone;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.glassfish.embeddable.GlassFishException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the server on its own.
 *
 * Note: It is recommended to run the suite of tests manually from the ehcache-server module after first starting
 * GFV3 from a distribution, changing its default port to 9090 first.
 *
 *
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public class ServerIntegrationTest {


    @BeforeClass
    public static void startup() throws Exception {
        Server.main(new String[]{"9090", "target/war/work/net.sf.ehcache/ehcache-server/"});
        waitForServerAvailability(300, TimeUnit.SECONDS);
    }

    private static void waitForServerAvailability(long time, TimeUnit unit) {
      boolean interrupted = false;
      try {
        long start = System.nanoTime();
        while (System.nanoTime() - start < unit.toNanos(time)) {
          URI server = URI.create("http://localhost:9090/ehcache/rest/sampleCache1");
          int response = -1;
          try {
            HttpURLConnection conn = (HttpURLConnection) server.toURL().openConnection();
            try {
              conn.setRequestMethod("HEAD");
              response = conn.getResponseCode();
            } finally {
              conn.disconnect();
            }
          } catch (IOException e) {
            System.err.println("Server Ping Failed : " + e.getMessage());
          }
          if (200 == response) {
            System.err.println("Server Startup Took : " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) + "ms");
            return;
          } else {
            try {
              Thread.sleep(1000);
            } catch (InterruptedException e) {
              interrupted = true;
            }
          }
        }
      } finally {
        if (interrupted) {
          Thread.currentThread().interrupt();
        }
      }
      throw new AssertionError("Server Startup Failed");
    }
    
//    @Ignore("MNK-1415")
    /**
     * Checks that the SOAP Web Service is actually running
     */
    @Test
    public void testEhcacheWebServiceEndPointExists() throws Exception {
        URI u = URI.create("http://localhost:9090/ehcache/soap/EhcacheWebServiceEndpoint");
        HttpURLConnection httpURLConnection = (HttpURLConnection) u.toURL().openConnection();
        httpURLConnection.setRequestMethod("GET");

        assertEquals(200, httpURLConnection.getResponseCode());
        //String mediaType = httpURLConnection.getContentType();

        String responseBody = inputStreamToText(httpURLConnection.getInputStream());
        assertTrue(responseBody.indexOf("Implementation class:") != 0);
    }

    /**
     * Checks that the RESTful WebService is actually running
     */
    @Test
    public void testGetRESTfulCache() throws Exception {
        URI u = URI.create("http://localhost:9090/ehcache/rest/sampleCache1");
        HttpURLConnection httpURLConnection = (HttpURLConnection) u.toURL().openConnection();
        httpURLConnection.setRequestMethod("GET");

        assertEquals(200, httpURLConnection.getResponseCode());
        //String mediaType = httpURLConnection.getContentType();

        String responseBody = inputStreamToText(httpURLConnection.getInputStream());
        assertTrue(responseBody.indexOf("sampleCache1") != 0);


    }


    @AfterClass
    public static void shutdown() throws InterruptedException, GlassFishException {
        Server.stopStatic();
    }



    /**
     * Converts a response in an InputStream to a byte[] for easy manipulation
     */
    public static byte[] inputStreamToBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[10000];
        int r;
        while ((r = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, r);
        }
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Converts a response in an InputStream to a String for easy comparisons
     */
    public static String inputStreamToText(InputStream inputStream) throws IOException {
        byte[] bytes = inputStreamToBytes(inputStream);
        return new String(bytes);
    }

}

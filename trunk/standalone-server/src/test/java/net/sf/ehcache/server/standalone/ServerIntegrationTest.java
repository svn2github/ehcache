package net.sf.ehcache.server.standalone;

import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.glassfish.embed.EmbeddedException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import static java.lang.Thread.sleep;

/**
 * Tests the server on its own.
 *
 * You need to run mvn war:exploded before running this test from the IDE. It explodes the ehcache-server war
 *
 *
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public class ServerIntegrationTest {


    @BeforeClass
    public static void startup() throws Exception, InterruptedException {
        Server.main(new String[]{"9090", "target/war/work/net.sf.ehcache/ehcache-server/"});
        sleep(10000);
    }


    /**
     * Checks that the SOAP Web Service is actually running
     */
    @Test
    public void testEhcacheWebServiceEndPointExists() throws IOException, ParserConfigurationException, SAXException {
        URL u = new URL("http://localhost:9090/ehcache/soap/EhcacheWebServiceEndpoint");
        HttpURLConnection httpURLConnection = (HttpURLConnection) u.openConnection();
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
    public void testGetRESTfulCache() throws IOException, ParserConfigurationException, SAXException {
        URL u = new URL("http://localhost:9090/ehcache/rest/sampleCache1");
        HttpURLConnection httpURLConnection = (HttpURLConnection) u.openConnection();
        httpURLConnection.setRequestMethod("GET");

        assertEquals(200, httpURLConnection.getResponseCode());
        //String mediaType = httpURLConnection.getContentType();

        String responseBody = inputStreamToText(httpURLConnection.getInputStream());
        assertTrue(responseBody.indexOf("sampleCache1") != 0);

    }


    @AfterClass
    public static void shutdown() throws EmbeddedException, InterruptedException {
        Server.stopStatic();
    }

    //Test
    public void testManual() throws InterruptedException {
        sleep(1000000);
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

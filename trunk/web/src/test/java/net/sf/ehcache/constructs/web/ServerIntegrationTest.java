package net.sf.ehcache.constructs.web;

import net.sf.ehcache.server.standalone.Server;
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
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * Tests the server on its own.
 *
 * You need to run mvn war:exploded before running this test from the IDE. It explodes the ehcache-server war
 *
 * You need to have the war built first.
 *
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id: ServerIntegrationTest.java 862 2008-12-26 01:20:05Z gregluck $
 */
public class ServerIntegrationTest extends AbstractWebTest {
    private static Server server;


//    @BeforeClass
//    public static void startup() throws Exception, InterruptedException {
//        server = new Server(8080, new File("target/ehcache-web-1.6/"));
//        server.start();
//        Thread.sleep(20000);
//    }

    /**
     */
    @Test
    public void testSmokeTest() throws IOException, ParserConfigurationException, SAXException {
        URL u = new URL("http://localhost:8080");
        HttpURLConnection httpURLConnection = (HttpURLConnection) u.openConnection();
        httpURLConnection.setRequestMethod("GET");

        assertEquals(200, httpURLConnection.getResponseCode());
        //String mediaType = httpURLConnection.getContentType();

    }



//    @AfterClass
//    public static void shutdown() throws EmbeddedException, InterruptedException {
//        server.stop();
//    }

    //Test
    public void testManual() throws InterruptedException {
        Thread.sleep(1000000);
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


package net.sf.ehcache.server.rest.resources;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import net.sf.ehcache.Status;
import net.sf.ehcache.server.AbstractWebTest;
import net.sf.ehcache.server.util.HttpUtil;
import net.sf.ehcache.server.util.StopWatch;
import net.sf.ehcache.util.MemoryEfficientByteArrayOutputStream;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;


/**
 * @author Greg Luck
 * @version $Id: SpeedTest.java 796 2008-10-09 02:39:03Z gregluck $
 */
public class SpeedTest extends AbstractWebTest {

    private static final Logger LOG = LoggerFactory.getLogger(SpeedTest.class);

    /**
     * Make sure there is something in there
     */
    @BeforeClass
    public static void setUp() throws IOException, ParserConfigurationException, SAXException {
        Status somethingThatIsSerializable = Status.STATUS_ALIVE;
        byte[] serializedForm = MemoryEfficientByteArrayOutputStream.serialize(somethingThatIsSerializable).getBytes();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(serializedForm);
        int status = HttpUtil.put("http://localhost:9090/ehcache/rest/sampleCache2/1", "application/x-java-serialized-object",
                byteArrayInputStream);
        assertEquals(201, status);

        HttpURLConnection urlConnection = HttpUtil.get("http://localhost:9090/ehcache/rest/sampleCache2/1");
        assertEquals(200, urlConnection.getResponseCode());
    }


    /**
     * Time to get 200 Cached Pages
     * StopWatch time: 947ms
     */
//    @Test
//    public void testSpeedHttpClientNotCached() throws IOException {
//        StopWatch stopWatch = new StopWatch();
//        String url = "http://localhost:9090/Login.jsp";
//        HttpClient httpClient = new HttpClient();
//        HttpMethod httpMethod = new GetMethod(url);
//        stopWatch.getElapsedTime();
//        for (int i = 0; i < 200; i++) {
//            httpClient.executeMethod(httpMethod);
//            httpMethod.getResponseBodyAsStream();
//        }
//        long time = stopWatch.getElapsedTime();
//        LOG.info("Time for 200 uncached page requests: " + time);
//    }

    /**
     * Latency 35 - 42ms
     */
    @Test
    public void testSpeedHttpClient() throws IOException, SAXException, ParserConfigurationException {
        StopWatch stopWatch = new StopWatch();
        String url = "http://localhost:9090/ehcache/rest/sampleCache2/1";
        HttpClient httpClient = new HttpClient();
        HttpMethod httpMethod = new GetMethod(url);
        stopWatch.getElapsedTime();
        for (int i = 0; i < 1000; i++) {
            httpClient.executeMethod(httpMethod);
            httpMethod.getResponseBodyAsStream();
        }
        long time = stopWatch.getElapsedTime();
        LOG.info("Time for 1000 cache requests: " + time + ". Latency " + 1000f / time + "ms");

    }

    /**
     * Latency .97ms
     */
    @Test
    public void testSpeedUrlConnection() throws IOException, SAXException, ParserConfigurationException {
        StopWatch stopWatch = new StopWatch();
        for (int i = 0; i < 1000; i++) {
            HttpURLConnection urlConnection = HttpUtil.get("http://localhost:9090/ehcache/rest/sampleCache2/1");
            assertEquals(200, urlConnection.getResponseCode());
        }
        long time = stopWatch.getElapsedTime();
        LOG.info("Time for 1000 cache requests: " + time + ". Latency " + 1000f / time + "ms");

    }

    /**
     * 1ms latency
     */
    @Test
    public void testSpeedNoDom() throws Exception {

        StopWatch stopWatch = new StopWatch();
        final WebConversation conversation = createWebConversation(true);

        String requestUrl = "http://localhost:9090/ehcache/rest/sampleCache2/1";
        stopWatch.getElapsedTime();
        for (int i = 0; i < 1000; i++) {
            WebResponse response = conversation.getResponse(requestUrl);
            response.getText().indexOf("timestamp");
        }
        long time = stopWatch.getElapsedTime();
        LOG.info("Time for 1000 cache requests: " + time + ". Latency " + 1000f / time + "ms");

    }


    @Test
    public void testConcurrentRequests() throws Exception {

        final List executables = new ArrayList();
        for (int i = 0; i < 40; i++) {
            final AbstractWebTest.Executable executable = new AbstractWebTest.Executable() {
                public void execute() throws Exception {
                    testSpeedNoDom();
                }
            };
            executables.add(executable);
        }
        runThreads(executables);
    }


}

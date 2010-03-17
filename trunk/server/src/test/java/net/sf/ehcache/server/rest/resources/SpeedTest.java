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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
        LOG.debug("Time for 1000 cache requests: " + time + ". Latency " + 1000f / time + "ms");

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


    /**
     * Memcached 1.2.1 with memcache Java lib 1.5.1
     * 10000 sets: 3396ms
     * 10000 gets: 3551ms
     * 10000 getMulti: 2132ms
     * 10000 deletes: 2065ms
     *
     * Ehcache 0.9 with Ehcache 2.0.0
     * 10000 puts: 3717ms (single threaded put)
     * 10000 puts: 2691ms (single threaded put after Web Container warm up)
     * 10000 gets: 31600ms (single threaded get)
     * 10000 gets: 17398ms (single threaded get)
     */
//    @Test
    public void testMemCachedBench() throws Exception {

        //warm up Java Web Container, which Memcached does not need
//        testConcurrentRequests();


        int cacheOperations = 10000;
        String cacheUrl = "http://localhost:9090/ehcache/rest/sampleCache1";
        String mediaType = "text/plain";
        String keyBase = "testKey";
        String object = "This is a test of an object blah blah es, serialization does not seem to slow things down so much.  The gzip compression is horrible horrible performance, so we only use it for very large objects.  I have not done any heavy benchmarking recently";
        byte[] objectAsBytes = object.getBytes();


//        ExecutorService executor = Executors.newFixedThreadPool(300);
//
//        final AbstractWebTest.Executable executable = new AbstractWebTest.Executable() {
//            public void execute() throws Exception {
//                HttpURLConnection urlConnection = HttpUtil.get("http://localhost:9090/ehcache/rest/sampleCache2/1");
//                if (urlConnection.getResponseCode() != 200) {
//                    LOG.info("Error on response: " + urlConnection.getResponseCode());
//                }
//            }
//        };

        StopWatch stopWatch = new StopWatch();
        for (int i = 0; i < cacheOperations; i++) {
            String keyUrl = new StringBuffer(cacheUrl).append('/').append(keyBase).append(i).toString();
            assertEquals(201, HttpUtil.put(keyUrl, mediaType, new ByteArrayInputStream(objectAsBytes)));
        }
        LOG.info(cacheOperations + " puts: " + stopWatch.getElapsedTime() + "ms");


        stopWatch = new StopWatch();

        for (int i = 0; i < cacheOperations; i++) {
            String keyUrl = new StringBuffer(cacheUrl).append('/').append(keyBase).append(i).toString();

            URL u = new URL(keyUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) u.openConnection();
            urlConnection.setUseCaches(true);
            urlConnection.setRequestMethod("GET");
            int status = urlConnection.getResponseCode();

            assertEquals(200, HttpUtil.get(keyUrl).getResponseCode());
        }
        LOG.info(cacheOperations + " gets: " + stopWatch.getElapsedTime() + "ms");


//
//        long begin = System.currentTimeMillis();
//        for (int i = start; i < start + runs; i++) {
//            mc.set(keyBase + i, object);
//        }
//        long end = System.currentTimeMillis();
//        long time = end - begin;
//        System.out.println(runs + " sets: " + time + "ms");
//
//        begin = System.currentTimeMillis();
//        for (int i = start; i < start + runs; i++) {
//            String str = (String) mc.get(keyBase + i);
//        }
//        end = System.currentTimeMillis();
//        time = end - begin;
//        System.out.println(runs + " gets: " + time + "ms");
//
//        String[] keys = new String[runs];
//        int j = 0;
//        for (int i = start; i < start + runs; i++) {
//            keys[j] = keyBase + i;
//            j++;
//        }
//        begin = System.currentTimeMillis();
//        Map vals = mc.getMulti(keys);
//        end = System.currentTimeMillis();
//        time = end - begin;
//        System.out.println(runs + " getMulti: " + time + "ms");
//
//        begin = System.currentTimeMillis();
//        for (int i = start; i < start + runs; i++) {
//            mc.delete(keyBase + i);
//        }
//        end = System.currentTimeMillis();
//        time = end - begin;
//        System.out.println(runs + " deletes: " + time + "ms");
    }


}

/**
 *  Copyright 2003-2009 Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache.server.rest.resources;

import net.sf.ehcache.server.HttpUtil;
import net.sf.ehcache.server.jaxb.Cache;
import net.sf.ehcache.server.jaxb.Caches;
import net.sf.ehcache.server.jaxb.JAXBContextResolver;
import net.sf.ehcache.server.soap.jaxws.CacheException_Exception;
import net.sf.ehcache.server.soap.jaxws.EhcacheWebServiceEndpoint;
import net.sf.ehcache.server.soap.jaxws.EhcacheWebServiceEndpointService;
import net.sf.ehcache.server.soap.jaxws.IllegalStateException_Exception;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import samples.ExampleJavaClient;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;


/**
 * Tests the REST web resource using the lightweight http container
 * <p/>
 *
 * @author Greg Luck
 */
public class CacheResourceTest {

    private static final Logger LOG = LoggerFactory.getLogger(CacheResourceTest.class);

    private static EhcacheWebServiceEndpoint cacheService;
    private String cacheName = "sampleCache1";

    @BeforeClass
    public static void beforeClass() {
        cacheService = new EhcacheWebServiceEndpointService().getEhcacheWebServiceEndpointPort();
    }

    @Before
    public void zeroOutCache() throws CacheException_Exception, IllegalStateException_Exception {
        cacheService.removeAll(cacheName);
        cacheService.removeAll("sampleCache3");
        cacheService.removeCache("newcache1");
    }

    @Test
    public void testHead() throws IOException, ParserConfigurationException, SAXException {
        HttpURLConnection result = HttpUtil.head("http://localhost:9090/ehcache/rest/sampleCache1");
        assertEquals(200, result.getResponseCode());
        assertEquals("application/xml", result.getContentType());

        LOG.info("Result of HEAD: " + result);
        byte[] bytes = HttpUtil.inputStreamToBytes(result.getInputStream());
        assertEquals(0, bytes.length);
        assertEquals(0, result.getContentLength());
        Map headers = result.getHeaderFields();
        assertNotNull(headers);
    }


    @Test
    public void testHeadCacheDoesNotExist() throws Exception {

        HttpURLConnection result = HttpUtil.head("http://localhost:9090/ehcache/rest/doesnotexist");
        assertEquals(404, result.getResponseCode());
        //0 for Jetty. Stack trace for Glassfish
        assertTrue(result.getContentLength() >= 0);
        if (result.getHeaderField("Server").matches("(.*)Jetty(.*)")) {
            assertEquals("text/plain", result.getContentType());
        } else if (result.getHeaderField("Server").matches("(.*)Glassfish(.*)")) {
            assertEquals("text/html", result.getContentType());
        }

        try {
            HttpUtil.inputStreamToText(result.getInputStream());
        } catch (IOException e) {
            //expected
        }
    }

    /**
     * Returns the WADL for the cache operations, which should list put, get, delete, post
     *
     * @throws Exception
     */
    @Test
    public void testOptions() throws Exception {

        HttpURLConnection result = HttpUtil.options("http://localhost:9090/ehcache/rest/doesnotexist");
        assertEquals(200, result.getResponseCode());
        assertEquals("application/vnd.sun.wadl+xml", result.getContentType());

        String responseBody = HttpUtil.inputStreamToText(result.getInputStream());
        assertNotNull(responseBody);

        assertTrue(responseBody.matches("(.*)GET(.*)"));
        assertTrue(responseBody.matches("(.*)PUT(.*)"));
        assertTrue(responseBody.matches("(.*)DELETE(.*)"));
        assertTrue(responseBody.matches("(.*)HEAD(.*)"));
    }


    @Test
    public void testGetCache() throws Exception {

        HttpURLConnection result = HttpUtil.get("http://localhost:9090/ehcache/rest/sampleCache1");
        assertEquals(200, result.getResponseCode());
        assertEquals("application/xml", result.getContentType());

        JAXBContext jaxbContext = new JAXBContextResolver().getContext(Caches.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        Cache cache = (Cache) unmarshaller.unmarshal(result.getInputStream());

        assertEquals("sampleCache1", cache.getName());
        assertEquals("http://localhost:9090/ehcache/rest/sampleCache1", cache.getUri());
        assertNotNull("http://localhost:9090/ehcache/rest/sampleCache1", cache.getDescription());
    }

    @Test
    public void testGetCacheDoesNotExist() throws Exception {

        HttpURLConnection result = HttpUtil.get("http://localhost:9090/ehcache/rest/doesnotexist");
        assertEquals(404, result.getResponseCode());
        JAXBContext jaxbContext = new JAXBContextResolver().getContext(Caches.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        Cache cache = null;
        try {
            cache = (Cache) unmarshaller.unmarshal(result.getInputStream());
        } catch (FileNotFoundException e) {
            //expected
        }
    }


    @Test
    public void testAddCache() throws Exception {

        //add a cache that does not exist
        HttpURLConnection urlConnection = HttpUtil.put("http://localhost:9090/ehcache/rest/newcache1");
        assertEquals(201, urlConnection.getResponseCode());

        if (urlConnection.getHeaderField("Server").matches("(.*)Glassfish(.*)")) {
            //others do not set it because the response body is empty
            assertTrue(urlConnection.getContentType().matches("text/plain(.*)"));
        }
        String location = urlConnection.getHeaderField("Location");
        assertEquals("http://localhost:9090/ehcache/rest/newcache1", location);
        String responseBody = HttpUtil.inputStreamToText(urlConnection.getInputStream());
        assertEquals("", responseBody);
        assertEquals(0, urlConnection.getContentLength());


        //attempt to add an existing cache
        urlConnection = HttpUtil.put("http://localhost:9090/ehcache/rest/newcache1");
        assertEquals(409, urlConnection.getResponseCode());

        if (urlConnection.getHeaderField("Server").matches("(.*)Glassfish(.*)")) {
            //others do not set it because the response body is empty
            assertTrue(urlConnection.getContentType().matches("text/plain(.*)"));
        }

        //todo HttpURLConnection is not giving the actual response message
        try {
            responseBody = HttpUtil.inputStreamToText(urlConnection.getInputStream());
        } catch (IOException e) {
            //expected
        }
    }

    @Test
    public void testDeleteCache() throws Exception {

        //add cache
        HttpURLConnection urlConnection = HttpUtil.put("http://localhost:9090/ehcache/rest/newcache1");

        //remove cache
        urlConnection = HttpUtil.delete("http://localhost:9090/ehcache/rest/newcache1");
        assertEquals(200, urlConnection.getResponseCode());


        if (urlConnection.getHeaderField("Server").matches("(.*)Glassfish(.*)")) {
            //others do not set it because the response body is empty
            assertTrue(urlConnection.getContentType().matches("text/plain(.*)"));
        }
        String responseBody = HttpUtil.inputStreamToText(urlConnection.getInputStream());
        assertEquals("", responseBody);

        urlConnection = HttpUtil.delete("http://localhost:9090/ehcache/rest/newcache1");
        assertEquals(404, urlConnection.getResponseCode());
        assertTrue(urlConnection.getContentType().matches("text/plain(.*)"));

        //todo HttpURLConnection is not giving the actual response message
        try {
            responseBody = HttpUtil.inputStreamToText(urlConnection.getInputStream());
        } catch (IOException e) {
            //expected
        }

    }

    @Test
    public void testCacheStatus() throws Exception {

        HttpURLConnection result = HttpUtil.get("http://localhost:9090/ehcache/rest/sampleCache1");
        assertEquals(200, result.getResponseCode());
        assertEquals("application/xml", result.getContentType());

        JAXBContext jaxbContext = new JAXBContextResolver().getContext(Caches.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        Cache cache = (Cache) unmarshaller.unmarshal(result.getInputStream());

        assertEquals("sampleCache1", cache.getName());
        assertEquals("http://localhost:9090/ehcache/rest/sampleCache1", cache.getUri());
        assertNotNull("http://localhost:9090/ehcache/rest/sampleCache1", cache.getDescription());


//        Status status = cacheService.getStatus("sampleCache1");
//        assertTrue(status == Status.STATUS_ALIVE);
    }

    @Test
    public void testExampleJavaClient() {
        ExampleJavaClient.main(null);
    }


//    @Test
//    public void testGetStatistics() throws NoSuchCacheException_Exception,
//            CacheException_Exception, IllegalStateException_Exception {
//        cacheService.clearStatistics("sampleCache1");
//
//        Statistics statistics = cacheService.getStatistics("sampleCache1");
//        assertEquals(0L, statistics.getCacheHits());
//
//        putElementIntoCache();
//        getElementFromCache();
//
//        statistics = cacheService.getStatistics("sampleCache1");
//        assertEquals(1L, statistics.getCacheHits());
//        assertTrue(statistics.getAverageGetTime() >= 0);
//        assertEquals(0L, statistics.getEvictionCount());
//        assertEquals(1L, statistics.getInMemoryHits());
//        assertEquals(0L, statistics.getOnDiskHits());
//        assertEquals(StatisticsAccuracy.STATISTICS_ACCURACY_BEST_EFFORT, statistics.getStatisticsAccuracy());
//    }
//
//    @Test
//    public void testGetStatisticsAccuracy() throws NoSuchCacheException_Exception,
//            CacheException_Exception, IllegalStateException_Exception {
//        assertEquals(StatisticsAccuracy.STATISTICS_ACCURACY_BEST_EFFORT,
//                cacheService.getStatisticsAccuracy("sampleCache1"));
//    }
//
//    @Test
//    public void testClearStatistics() throws NoSuchCacheException_Exception,
//            CacheException_Exception, IllegalStateException_Exception {
//        putElementIntoCache();
//        getElementFromCache();
//
//        cacheService.clearStatistics("sampleCache1");
//        Statistics statistics = cacheService.getStatistics("sampleCache1");
//        assertEquals(0L, statistics.getCacheHits());
//    }
//
//    @Test
//    public void testCacheStatus() throws Exception {
//
//        HttpURLConnection result = HttpUtil.get("http://localhost:9090/ehcache/rest/sampleCache1");
//        assertEquals(200, result.getResponseCode());
//        assertEquals("application/xml", result.getContentType());
//
//        JAXBContext jaxbContext = new JAXBContextResolver().getContext(Caches.class);
//        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
//        Cache cache = (Cache) unmarshaller.unmarshal(result.getInputStream());
//
//        assertEquals("sampleCache1", cache.getName());
//        assertEquals("http://localhost:9090/ehcache/rest/sampleCache1", cache.getUri());
//        assertNotNull("http://localhost:9090/ehcache/rest/sampleCache1", cache.getDescription());
//
//
////        Status status = cacheService.getStatus("sampleCache1");
////        assertTrue(status == Status.STATUS_ALIVE);
//    }


}
/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
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
import net.sf.ehcache.server.jaxb.Element;
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
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathConstants;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.logging.Logger;


/**
 * Tests the REST web resource using the lightweight http container
 * <p/>
 *
 * @author Greg Luck
 * @version $Id$
 */
public class ElementResourceTest {

    public static final Logger LOG = Logger.getLogger(CachesResourceTest.class.getName());

    private static EhcacheWebServiceEndpoint cacheService;
    private String cacheName = "sampleCache1";

    @BeforeClass
    public static void setup() {
        cacheService = new EhcacheWebServiceEndpointService().getEhcacheWebServiceEndpointPort();
    }

    @Before
    public void zeroOutCache() throws CacheException_Exception, IllegalStateException_Exception {
        cacheService.removeAll(cacheName);
        cacheService.removeAll("sampleCache3");
        cacheService.removeAll("sampleCache2");
        cacheService.removeCache("newcache1");
    }

    @Test
    public void testHead() throws Exception {

        String originalString = "Some string";
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(originalString.getBytes());
        HttpUtil.put("http://localhost:8080/ehcache/rest/sampleCache1/1", "text/plain", byteArrayInputStream);

        HttpURLConnection urlConnection = HttpUtil.head("http://localhost:8080/ehcache/rest/sampleCache1/1");
        assertEquals(200, urlConnection.getResponseCode());

        if (urlConnection.getHeaderField("Server").matches("(.*)Glassfish(.*)")) {
            //others do not set it because the response body is empty
            assertTrue(urlConnection.getContentType().matches("text/plain(.*)"));
        }

        LOG.info("Result of HEAD: " + urlConnection);
        byte[] bytes = HttpUtil.inputStreamToBytes(urlConnection.getInputStream());
        assertEquals(0, bytes.length);
        assertEquals(0, urlConnection.getContentLength());
        Map headers = urlConnection.getHeaderFields();
        assertNotNull(headers);
    }


    @Test
    public void testHeadElementDoesNotExist() throws Exception {

        HttpURLConnection result = HttpUtil.head("http://localhost:8080/ehcache/rest/sampleCache3/doesnotexist");
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

        HttpURLConnection result = HttpUtil.options("http://localhost:8080/ehcache/rest/doesnotexist/1");
        assertEquals(200, result.getResponseCode());
        assertEquals("application/vnd.sun.wadl+xml", result.getContentType());

        String responseBody = HttpUtil.inputStreamToText(result.getInputStream());
        assertNotNull(responseBody);
        assertTrue(responseBody.matches("(.*)GET(.*)"));
        assertTrue(responseBody.matches("(.*)PUT(.*)"));
        assertTrue(responseBody.matches("(.*)DELETE(.*)"));
        assertTrue(responseBody.matches("(.*)HEAD(.*)"));
    }


    /**
     * Stick in some text with MIME Type plain/text and make sure it comes back.
     *
     * @throws java.io.IOException
     * @throws javax.xml.parsers.ParserConfigurationException
     *
     * @throws org.xml.sax.SAXException
     */
    @Test
    public void testPutGetElementPlainText() throws Exception {
        long beforeCreated = System.currentTimeMillis();
        String originalString = "The rain in Spain falls mainly on the plain";
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(originalString.getBytes());

        HttpUtil.put("http://localhost:8080/ehcache/rest/sampleCache2/1", "text/plain", byteArrayInputStream);
        HttpURLConnection urlConnection = HttpUtil.get("http://localhost:8080/ehcache/rest/sampleCache2/1");
        assertEquals(200, urlConnection.getResponseCode());
        assertTrue(urlConnection.getContentType().matches("text/plain"));
        byte[] bytes = HttpUtil.inputStreamToBytes(urlConnection.getInputStream());
        String plainText = new String(bytes);
        assertEquals(originalString, plainText);
        LOG.info("" + beforeCreated);
        LOG.info("" + urlConnection.getLastModified());
        LOG.info("" + System.currentTimeMillis());
        //todo last modified is before put time! Why?
        assertTrue(
                urlConnection.getLastModified() > 0 &&
                        urlConnection.getLastModified() < System.currentTimeMillis());
        //We just use the Element version
        assertEquals("\"1\"", urlConnection.getHeaderField("ETag"));
    }

    /**
     * Note: The server does not return Elements. It returns values, with meta data in the headers.
     */
    @Test
    public void testPutGetElementXMLXPath() throws Exception {

        String xmlDocument = "<?xml version=\"1.0\"?>\n" +
                "<oldjoke>\n" +
                "<burns>Say <quote>goodnight</quote>,\n" +
                "Gracie.</burns>\n" +
                "<allen><quote>Goodnight, \n" +
                "Gracie.</quote></allen>\n" +
                "<applause/>\n" +
                "</oldjoke>";

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(xmlDocument.getBytes());


        HttpUtil.put("http://localhost:8080/ehcache/rest/sampleCache2/2", "application/xml", byteArrayInputStream);
        Thread.sleep(100);
        HttpURLConnection urlConnection = HttpUtil.get("http://localhost:8080/ehcache/rest/sampleCache2/2");
        assertEquals(200, urlConnection.getResponseCode());
        assertTrue(urlConnection.getContentType().matches("application/xml"));

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = documentBuilder.parse(urlConnection.getInputStream());

        XPath xpath = XPathFactory.newInstance().newXPath();
        String expression = "/oldjoke/burns";
        Node node = (Node) xpath.evaluate(expression, document, XPathConstants.NODE);

        assertEquals("burns", node.getNodeName());


        String eTag = urlConnection.getHeaderField("Etag");
        LOG.info("eTag: " + eTag);
        String lastModified = urlConnection.getHeaderField("Last-Modified");
        LOG.info("lastModified: " + lastModified);

        //Check ETag and Last-Modified are the same
        URL u = new URL("http://localhost:8080/ehcache/rest/sampleCache2/2");
        urlConnection = (HttpURLConnection) u.openConnection();
        urlConnection.setRequestMethod("GET");
        assertEquals(200, urlConnection.getResponseCode());
        assertTrue(urlConnection.getContentType().matches("application/xml"));
        assertEquals(eTag, urlConnection.getHeaderField("Etag"));
        assertEquals(lastModified, urlConnection.getHeaderField("Last-Modified"));

        //Check ETag and Last-Modified are different after the element was updated.
        HttpUtil.put("http://localhost:8080/ehcache/rest/sampleCache2/2", "application/xml", byteArrayInputStream);
        Thread.sleep(100);
        u = new URL("http://localhost:8080/ehcache/rest/sampleCache2/2");
        urlConnection = (HttpURLConnection) u.openConnection();
        urlConnection.setRequestMethod("GET");
        assertEquals(200, urlConnection.getResponseCode());
        assertTrue(urlConnection.getContentType().matches("application/xml"));
        assertTrue(!eTag.equals(urlConnection.getHeaderField("Etag")));
        LOG.info("eTag: " + urlConnection.getHeaderField("Etag"));
        LOG.info("lastModified: " + urlConnection.getHeaderField("Last-Modified"));
        assertTrue(!lastModified.equals(urlConnection.getHeaderField("Last-Modified")));

        //Check Caching Behaviour
        u = new URL("http://localhost:8080/ehcache/rest/sampleCache2/2");
        urlConnection = (HttpURLConnection) u.openConnection();
        urlConnection.setRequestMethod("GET");
//        urlConnection.setRequestProperty("");
        assertEquals(200, urlConnection.getResponseCode());
        assertTrue(urlConnection.getContentType().matches("application/xml"));
        assertTrue(!eTag.equals(urlConnection.getHeaderField("Etag")));
        assertTrue(!lastModified.equals(urlConnection.getHeaderField("Last-Modified")));

    }


    @Test
    public void testGetElement() throws Exception {

        HttpURLConnection result = HttpUtil.get("http://localhost:8080/ehcache/rest/sampleCache1");
        assertEquals(200, result.getResponseCode());
        assertEquals("application/xml", result.getContentType());

        JAXBContext jaxbContext = new JAXBContextResolver().getContext(Caches.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        Cache cache = (Cache) unmarshaller.unmarshal(result.getInputStream());

        assertEquals("sampleCache1", cache.getName());
        assertEquals("http://localhost:8080/ehcache/rest/sampleCache1", cache.getUri());
        assertNotNull("http://localhost:8080/ehcache/rest/sampleCache1", cache.getDescription());
    }

    @Test
    public void testGetCacheDoesNotExist() throws Exception {

        HttpURLConnection result = HttpUtil.get("http://localhost:8080/ehcache/rest/doesnotexist");
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

//
//    @Test
//    public void testCachePutNull() throws CacheException_Exception,
//            NoSuchCacheException_Exception, IllegalStateException_Exception {
//
//        Element element = new Element();
//        element.setKey("1");
//        cacheService.put("sampleCache1", element);
//
//        element = getElementFromCache();
//        boolean equals = Arrays.equals(null, element.getValue());
//        assertTrue(equals);
//    }


    @Test
    public void testAddCache() throws Exception {

        //add a cache that does not exist
        HttpURLConnection urlConnection = HttpUtil.put("http://localhost:8080/ehcache/rest/newcache1");
        assertEquals(201, urlConnection.getResponseCode());

        if (urlConnection.getHeaderField("Server").matches("(.*)Glassfish(.*)")) {
            //others do not set it because the response body is empty
            assertTrue(urlConnection.getContentType().matches("text/plain(.*)"));
        }
        String location = urlConnection.getHeaderField("Location");
        assertEquals("http://localhost:8080/ehcache/rest/newcache1", location);
        String responseBody = HttpUtil.inputStreamToText(urlConnection.getInputStream());
        assertEquals("", responseBody);
        assertEquals(0, urlConnection.getContentLength());

        //attempt to add an existing cache
        urlConnection = HttpUtil.put("http://localhost:8080/ehcache/rest/newcache1");
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
        HttpURLConnection urlConnection = HttpUtil.put("http://localhost:8080/ehcache/rest/newcache1");

        //remove cache
        urlConnection = HttpUtil.delete("http://localhost:8080/ehcache/rest/newcache1");
        assertEquals(200, urlConnection.getResponseCode());


        if (urlConnection.getHeaderField("Server").matches("(.*)Glassfish(.*)")) {
            //others do not set it because the response body is empty
            assertTrue(urlConnection.getContentType().matches("text/plain(.*)"));
        }
        String responseBody = HttpUtil.inputStreamToText(urlConnection.getInputStream());
        assertEquals("", responseBody);

        urlConnection = HttpUtil.delete("http://localhost:8080/ehcache/rest/newcache1");
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

        HttpURLConnection result = HttpUtil.get("http://localhost:8080/ehcache/rest/sampleCache1");
        assertEquals(200, result.getResponseCode());
        assertEquals("application/xml", result.getContentType());

        JAXBContext jaxbContext = new JAXBContextResolver().getContext(Caches.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        Cache cache = (Cache) unmarshaller.unmarshal(result.getInputStream());

        assertEquals("sampleCache1", cache.getName());
        assertEquals("http://localhost:8080/ehcache/rest/sampleCache1", cache.getUri());
        assertNotNull("http://localhost:8080/ehcache/rest/sampleCache1", cache.getDescription());

//        Status status = cacheService.getStatus("sampleCache1");
//        assertTrue(status == Status.STATUS_ALIVE);
    }

//
//    private Element getElementFromCache() throws CacheException_Exception, IllegalStateException_Exception, NoSuchCacheException_Exception {
//        Element element;
//        element = cacheService.get("sampleCache1", "1");
//        return element;
//    }
//
//    /**
//     * Tests get, getQuiet and put, putQuiet
//     */
//    @Test
//    public void testCacheGetPut() throws CacheException_Exception,
//            NoSuchCacheException_Exception, IllegalStateException_Exception, IOException, IllegalArgumentException_Exception, InterruptedException {
//
//        Element element = new Element();
//        element.setKey("2");
//        byte[] bytes1 = new byte[]{1, 2, 3, 4, 5, 6};
//        element.setValue(bytes1);
//
//        cacheService.put("sampleCache1", element);
//        element = cacheService.get("sampleCache1", "2");
//        byte[] bytes2 = element.getValue();
//        assertTrue(Arrays.equals(bytes1, bytes2));
//        cacheService.remove("sampleCache1", "2");
//
//        cacheService.putQuiet("sampleCache1", element);
//        element = cacheService.get("sampleCache1", "2");
//        bytes2 = element.getValue();
//        assertTrue(Arrays.equals(bytes1, bytes2));
//        cacheService.remove("sampleCache1", "2");
//
//        cacheService.put("sampleCache1", element);
//        element = cacheService.getQuiet("sampleCache1", "2");
//        bytes2 = element.getValue();
//        assertTrue(Arrays.equals(bytes1, bytes2));
//        cacheService.remove("sampleCache1", "2");
//
//        //ttl override
//        Element expiryOverrideElement = new Element();
//        expiryOverrideElement.setKey("abc");
//        expiryOverrideElement.setValue("value".getBytes());
//        expiryOverrideElement.setTimeToLiveSeconds(1);
//        cacheService.put("sampleCache1", expiryOverrideElement);
//        Thread.sleep(1010);
//        element = cacheService.get("sampleCache1", "abc");
//        assertEquals(null, element);
//
//
//    }
//
//    @Test
//    public void testDefaultExpiry() throws NoSuchCacheException_Exception, CacheException_Exception, IllegalStateException_Exception, InterruptedException {
//        Element element2 = new Element();
//        element2.setKey("2");
//        element2.setValue(new byte[]{1, 2, 3, 4, 5, 6});
//        cacheService.put("sampleCache3", element2);
//        assertNotNull(cacheService.get("sampleCache3", "2"));
//        Thread.sleep(1010);
//        assertEquals(null, cacheService.get("sampleCache3", "2"));
//
//    }
//
//    @Test
//    public void testOverrideEternal() throws NoSuchCacheException_Exception, CacheException_Exception, IllegalStateException_Exception, InterruptedException {
//        Element element = new Element();
//        element.setKey("2");
//        element.setValue(new byte[]{1, 2, 3, 4, 5, 6});
//        element.setEternal(true);
//        cacheService.put("sampleCache3", element);
//        assertNotNull(cacheService.get("sampleCache3", "2"));
//        Thread.sleep(1010);
//        //should not expire
//        assertNotNull(cacheService.get("sampleCache3", "2"));
//    }
//
//
//    @Test
//    public void testOverrideTTI() throws NoSuchCacheException_Exception, CacheException_Exception, IllegalStateException_Exception, InterruptedException {
//        Element element = new Element();
//        element.setKey("2");
//        element.setValue(new byte[]{1, 2, 3, 4, 5, 6});
//        element.setTimeToIdleSeconds(1);
//        cacheService.put("sampleCache3", element);
//        assertNotNull(cacheService.get("sampleCache3", "2"));
//        Thread.sleep(1010);
//        //should expire
//        assertNull(cacheService.get("sampleCache3", "2"));
//    }
//
//    /**
//     * Test getKeys() and its veriants
//     */
//    @Test
//    public void testGetKeys() throws NoSuchCacheException_Exception,
//            CacheException_Exception, IllegalStateException_Exception {
//
//        for (int i = 0; i < 1000; i++) {
//            Element element = new Element();
//            element.setKey(i);
//            element.setValue(("value" + i).getBytes());
//
//            cacheService.put("sampleCache1", element);
//        }
//
//        List keys = cacheService.getKeys("sampleCache1");
//        assertEquals(1000, keys.size());
//
//        keys = cacheService.getKeysWithExpiryCheck("sampleCache1");
//        assertEquals(1000, keys.size());
//
//        keys = cacheService.getKeysNoDuplicateCheck("sampleCache1");
//        assertEquals(1000, keys.size());
//
//    }
//
//
//    @Test
//    public void testRemove() throws NoSuchCacheException_Exception,
//            CacheException_Exception, IllegalStateException_Exception {
//
//        putElementIntoCache();
//
//        assertEquals(1, cacheService.getSize("sampleCache1"));
//    }
//
//    private void putElementIntoCache() throws CacheException_Exception, NoSuchCacheException_Exception {
//        Element element = new Element();
//        element.setKey("1");
//        element.setValue(("value1").getBytes());
//        cacheService.put("sampleCache1", element);
//    }
//
//
//
//
//    /**
//     * No loader configured. smoke test only
//     */
//    @Test
//    public void testLoad() throws NoSuchCacheException_Exception,
//            CacheException_Exception, IllegalStateException_Exception {
//        cacheService.load("sampleCache1", "2");
//    }
//
//    /**
//     * No loader configured. smoke test only
//     */
//    @Test
//    public void testLoadAll() throws NoSuchCacheException_Exception,
//            CacheException_Exception, IllegalStateException_Exception {
//        List keys = new ArrayList();
//        for (int i = 0; i < 5; i++) {
//            keys.add("" + i);
//        }
//        cacheService.loadAll("sampleCache1", keys);
//    }
//
//
//    /**
//     * No loader configured. smoke test only
//     */
//    @Test
//    public void testGetWithLoad() throws NoSuchCacheException_Exception,
//            CacheException_Exception, IllegalStateException_Exception {
//        cacheService.getWithLoader("sampleCache1", "2");
//    }
//
//    /**
//     * No loader configured. smoke test only
//     */
//    @Test
//    public void testGetAllWithLoader() throws NoSuchCacheException_Exception,
//            CacheException_Exception, IllegalStateException_Exception {
//        List keys = new ArrayList();
//        for (int i = 0; i < 5; i++) {
//            keys.add("" + i);
//        }
//        cacheService.getAllWithLoader("sampleCache1", keys);
//    }


}
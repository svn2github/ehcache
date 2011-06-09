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

import net.sf.ehcache.Status;
import net.sf.ehcache.server.util.HttpUtil;
import net.sf.ehcache.server.util.Header;
import net.sf.ehcache.server.jaxb.Cache;
import net.sf.ehcache.server.jaxb.Caches;
import net.sf.ehcache.server.jaxb.JAXBContextResolver;
import net.sf.ehcache.server.soap.jaxws.CacheException_Exception;
import net.sf.ehcache.server.soap.jaxws.EhcacheWebServiceEndpoint;
import net.sf.ehcache.server.soap.jaxws.EhcacheWebServiceEndpointService;
import net.sf.ehcache.server.soap.jaxws.IllegalStateException_Exception;
import net.sf.ehcache.util.MemoryEfficientByteArrayOutputStream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Map;



/**
 * Tests the REST web resource using the lightweight http container
 * <p/>
 *
 * @author Greg Luck
 */
public class ElementResourceTest extends AbstractRestTest {

    private static final Logger LOG = LoggerFactory.getLogger(ElementResourceTest.class);

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
        cacheService.removeAll("sampleCache2");
        cacheService.removeCache("newcache1");
    }

    @Test
    public void testHead() throws Exception {

        String originalString = "Some string";
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(originalString.getBytes());
        int status = HttpUtil.put("http://localhost:9090/ehcache/rest/sampleCache1/1", "text/plain", byteArrayInputStream);
        assertEquals(201, status);

        HttpURLConnection urlConnection = HttpUtil.head("http://localhost:9090/ehcache/rest/sampleCache1/1");
        assertEquals(200, urlConnection.getResponseCode());

        if (urlConnection.getHeaderField("Server").matches("(.*)Glassfish(.*)")) {
            //others do not set it because the response body is empty
            assertTrue(urlConnection.getContentType().matches("text/plain(.*)"));
        }

        LOG.info("Result of HEAD: " + urlConnection);
        byte[] bytes = HttpUtil.inputStreamToBytes(urlConnection.getInputStream());
        assertEquals(0, bytes.length);
        //head should set content-length
        assertEquals(11, urlConnection.getContentLength());
        Map headers = urlConnection.getHeaderFields();
        assertNotNull(headers);
    }


    @Test
    public void testHeadElementDoesNotExist() throws Exception {

        HttpURLConnection result = HttpUtil.head("http://localhost:9090/ehcache/rest/sampleCache3/doesnotexist");
        assertEquals(404, result.getResponseCode());
        //0 for Jetty. Stack trace for Glassfish
        assertTrue(result.getContentLength() >= 0);
        if (result.getHeaderField("Server").matches("(.*)Jetty(.*)")) {
            assertEquals("text/html; charset=iso-8859-1", result.getContentType());
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

        HttpURLConnection result = HttpUtil.options("http://localhost:9090/ehcache/rest/doesnotexist/1");
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
        Thread.sleep(10);
        String originalString = "The rain in Spain falls mainly on the plain";
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(originalString.getBytes());

        assertEquals(404, HttpUtil.get("http://localhost:9090/ehcache/rest/sampleCache2/1").getResponseCode());
        int status = HttpUtil.put("http://localhost:9090/ehcache/rest/sampleCache2/1", "text/plain", byteArrayInputStream);
        assertEquals(201, status);

        HttpURLConnection urlConnection = HttpUtil.get("http://localhost:9090/ehcache/rest/sampleCache2/1");
        assertEquals(200, urlConnection.getResponseCode());
        assertTrue(urlConnection.getContentType().matches("text/plain"));
        byte[] bytes = HttpUtil.inputStreamToBytes(urlConnection.getInputStream());
        urlConnection.disconnect();
        String plainText = new String(bytes);
        assertEquals(originalString, plainText);
        LOG.info("beforeCreated: " + beforeCreated);
        LOG.info("lastModified: " + new Date(urlConnection.getLastModified()));
        LOG.info("now: " + new Date(System.currentTimeMillis()));
        //The HTTP protocol Last-Modified only goes down to seconds, therefore we need to take a second off to make sure
        //  the time is grated than a ms
        //accurate beforeCreated time. This was a little messy to find out.
        //We use the Element version + Last-Modified
        assertNotNull(urlConnection.getHeaderField("ETag"));
        //As of 1.7 Element now ceilings up, so can be up to a second out
        assertTrue("", urlConnection.getLastModified() > (beforeCreated - 1000));
        assertTrue("last Modified not before now", urlConnection.getLastModified() < (System.currentTimeMillis() + 1000));
    }


    /**
     * Tests specifying the ehcacheTimeToLiveSeconds request header
     */
    @Test
    public void testPutOverrideTTL() throws Exception {
        long beforeCreated = System.currentTimeMillis();
        Thread.sleep(10);
        String originalString = "The rain in Spain falls mainly on the plain";
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(originalString.getBytes());

        assertEquals(404, HttpUtil.get("http://localhost:9090/ehcache/rest/sampleCache2/1").getResponseCode());
        Header header = new Header("ehcacheTimeToLiveSeconds", "10");
        int status = HttpUtil.put("http://localhost:9090/ehcache/rest/sampleCache2/1", "text/plain", byteArrayInputStream, header);
        assertEquals(201, status);

        HttpURLConnection urlConnection = HttpUtil.get("http://localhost:9090/ehcache/rest/sampleCache2/1");
        assertEquals(200, urlConnection.getResponseCode());

        Thread.sleep(15000);
        urlConnection = HttpUtil.get("http://localhost:9090/ehcache/rest/sampleCache2/1");
        assertEquals(404, urlConnection.getResponseCode());

        header = new Header("ehcacheTimeToLiveSeconds", "garbage");
        status = HttpUtil.put("http://localhost:9090/ehcache/rest/sampleCache2/1", "text/plain", byteArrayInputStream, header);
        assertEquals(201, status);

        //Should have not parsed
        Thread.sleep(15000);
        urlConnection = HttpUtil.get("http://localhost:9090/ehcache/rest/sampleCache2/1");
        assertEquals(200, urlConnection.getResponseCode());


        header = new Header("ehcacheTimeToLiveSeconds", null);
        status = HttpUtil.put("http://localhost:9090/ehcache/rest/sampleCache2/1", "text/plain", byteArrayInputStream, header);
        assertEquals(201, status);

        //Should have not parsed
        Thread.sleep(15000);
        urlConnection = HttpUtil.get("http://localhost:9090/ehcache/rest/sampleCache2/1");
        assertEquals(200, urlConnection.getResponseCode());

        //the header is case insensitive
        header = new Header("EhcachetImeToLiveSeconds", "1");
        status = HttpUtil.put("http://localhost:9090/ehcache/rest/sampleCache2/1", "text/plain", byteArrayInputStream, header);
        assertEquals(201, status);

        Thread.sleep(5000);
        urlConnection = HttpUtil.get("http://localhost:9090/ehcache/rest/sampleCache2/1");
        assertEquals(404, urlConnection.getResponseCode());


    }


    /**
     * @throws java.io.IOException
     * @throws javax.xml.parsers.ParserConfigurationException
     *
     * @throws org.xml.sax.SAXException
     */
    @Test
    public void testDeleteElement() throws Exception {
        long beforeCreated = System.currentTimeMillis();
        Thread.sleep(10);
        String originalString = "The rain in Spain falls mainly on the plain";
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(originalString.getBytes());

        int status = HttpUtil.put("http://localhost:9090/ehcache/rest/sampleCache2/1", "text/plain", byteArrayInputStream);
        assertEquals(201, status);
        HttpURLConnection urlConnection = HttpUtil.get("http://localhost:9090/ehcache/rest/sampleCache2/1");
        assertEquals(200, urlConnection.getResponseCode());

        urlConnection = HttpUtil.delete("http://localhost:9090/ehcache/rest/sampleCache2/1");
        assertEquals(204, urlConnection.getResponseCode());

        assertEquals(404, HttpUtil.get("http://localhost:9090/ehcache/rest/sampleCache2/1").getResponseCode());
    }


    /**
     * @throws java.io.IOException
     * @throws javax.xml.parsers.ParserConfigurationException
     *
     * @throws org.xml.sax.SAXException
     */
    @Test
    public void testDeleteAllElement() throws Exception {
        long beforeCreated = System.currentTimeMillis();
        Thread.sleep(10);
        String originalString = "The rain in Spain falls mainly on the plain";
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(originalString.getBytes());

        HttpUtil.put("http://localhost:9090/ehcache/rest/sampleCache2/1", "text/plain", byteArrayInputStream);
        HttpUtil.put("http://localhost:9090/ehcache/rest/sampleCache2/2", "text/plain", byteArrayInputStream);
        HttpUtil.put("http://localhost:9090/ehcache/rest/sampleCache2/3", "text/plain", byteArrayInputStream);


        HttpURLConnection urlConnection = HttpUtil.get("http://localhost:9090/ehcache/rest/sampleCache2/1");
        assertEquals(200, urlConnection.getResponseCode());

        urlConnection = HttpUtil.delete("http://localhost:9090/ehcache/rest/sampleCache2/*");
        assertEquals(404, HttpUtil.get("http://localhost:9090/ehcache/rest/sampleCache2/1").getResponseCode());
        assertEquals(404, HttpUtil.get("http://localhost:9090/ehcache/rest/sampleCache2/2").getResponseCode());
        assertEquals(404, HttpUtil.get("http://localhost:9090/ehcache/rest/sampleCache2/3").getResponseCode());
    }

    /**
     * Stick in a java object with mime type of application/x-java-serialized-object and make sure it comes back.
     */
    @Test
    public void testPutGetElementJava() throws Exception {

        Status somethingThatIsSerializable = Status.STATUS_ALIVE;
        byte[] serializedForm = MemoryEfficientByteArrayOutputStream.serialize(somethingThatIsSerializable).getBytes();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(serializedForm);

        assertEquals(404, HttpUtil.get("http://localhost:9090/ehcache/rest/sampleCache2/1").getResponseCode());
        int status = HttpUtil.put("http://localhost:9090/ehcache/rest/sampleCache2/1", "application/x-java-serialized-object",
                byteArrayInputStream);
        assertEquals(201, status);

        HttpURLConnection urlConnection = HttpUtil.get("http://localhost:9090/ehcache/rest/sampleCache2/1");
        assertEquals(200, urlConnection.getResponseCode());
        assertTrue(urlConnection.getContentType().matches("application/x-java-serialized-object"));
        byte[] bytes = HttpUtil.inputStreamToBytes(urlConnection.getInputStream());
        urlConnection.disconnect();

        final ByteArrayInputStream instr = new ByteArrayInputStream(bytes);
        final ObjectInputStream objectInputStream = new ObjectInputStream(instr);
        Status somethingThatIsSerializable2 = (Status) objectInputStream.readObject();

        assertEquals(somethingThatIsSerializable, somethingThatIsSerializable2);
    }

    /**
     * Get a java object which was put in some way other than the RESTful API.
     */
    @Test
    public void testGetElementJava() throws Exception {

        Status somethingThatIsSerializable = Status.STATUS_ALIVE;
        byte[] serializedForm = MemoryEfficientByteArrayOutputStream.serialize(somethingThatIsSerializable).getBytes();

        assertEquals(404, HttpUtil.get("http://localhost:9090/ehcache/rest/sampleCache2/1").getResponseCode());

        net.sf.ehcache.server.soap.jaxws.Element element = new net.sf.ehcache.server.soap.jaxws.Element();
        element.setKey("1");
        element.setValue(serializedForm);
        cacheService.put("sampleCache2", element);

        long begin = System.currentTimeMillis();
        HttpURLConnection urlConnection = HttpUtil.get("http://localhost:9090/ehcache/rest/sampleCache2/1");
        long end = System.currentTimeMillis();
        LOG.info("Get time: " + (end - begin));
        assertEquals(200, urlConnection.getResponseCode());


        //MimeTYpe not set, so the server sets application/octet-stream
        assertTrue(urlConnection.getContentType().matches("application/octet-stream"));
        byte[] bytes = HttpUtil.inputStreamToBytes(urlConnection.getInputStream());
        urlConnection.disconnect();

        //We should still be able to deserialize because we know it a java object
        final ByteArrayInputStream instr = new ByteArrayInputStream(bytes);
        final ObjectInputStream objectInputStream = new ObjectInputStream(instr);
        Status somethingThatIsSerializable2 = (Status) objectInputStream.readObject();

        assertEquals(somethingThatIsSerializable, somethingThatIsSerializable2);
    }


    /**
     * Get a java object which was put in some way other than the RESTful API.
     */
    @Test
    public void testGetElementJavaPerfTest() throws Exception {

        Status somethingThatIsSerializable = Status.STATUS_ALIVE;
        byte[] serializedForm = MemoryEfficientByteArrayOutputStream.serialize(somethingThatIsSerializable).getBytes();

        assertEquals(404, HttpUtil.get("http://localhost:9090/ehcache/rest/sampleCache2/1").getResponseCode());

        net.sf.ehcache.server.soap.jaxws.Element element = new net.sf.ehcache.server.soap.jaxws.Element();
        element.setKey("1");
        element.setValue(serializedForm);
        cacheService.put("sampleCache2", element);

        long begin = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            HttpURLConnection urlConnection = HttpUtil.get("http://localhost:9090/ehcache/rest/sampleCache2/1");
            assertEquals(200, urlConnection.getResponseCode());
        }
        long end = System.currentTimeMillis();
        LOG.info("Get time: " + (end - begin) / 100f);
    }


    /**
     * Stick in a java object without mime type of application/x-java-serialized-object.
     * Server does not accept the content and responds with a 400
     * <p/>
     * Note: it this test causes a java.lang.IllegalArgumentException: Error parsing media type '' in the server log.
     */
    @Test
    public void testPutEmptyMimeType() throws Exception {

        Status somethingThatIsSerializable = Status.STATUS_ALIVE;
        byte[] serializedForm = MemoryEfficientByteArrayOutputStream.serialize(somethingThatIsSerializable).getBytes();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(serializedForm);

        assertEquals(404, HttpUtil.get("http://localhost:9090/ehcache/rest/sampleCache2/1").getResponseCode());
        int status = HttpUtil.put("http://localhost:9090/ehcache/rest/sampleCache2/1", null, byteArrayInputStream);
        //GF does 400 which is better
        //GFV3 does 201
        //Jetty does 500
        assertTrue(status == 400 || status == 500 | status == 201);

    }

    /**
     * Note: The server does not return Elements. It returns values, with meta data in the headers.
     */
    @Test
    public void testPutGetElementXML() throws Exception {

        String xmlDocument = "<?xml version=\"1.0\"?>\n" +
                "<oldjoke>\n" +
                "<burns>Say <quote>goodnight</quote>,\n" +
                "Gracie.</burns>\n" +
                "<allen><quote>Goodnight, \n" +
                "Gracie.</quote></allen>\n" +
                "<applause/>\n" +
                "</oldjoke>";

        HttpUtil.put("http://localhost:9090/ehcache/rest/sampleCache2/2", "application/xml",
                new ByteArrayInputStream(xmlDocument.getBytes()));
        Thread.sleep(100);
        HttpURLConnection urlConnection = HttpUtil.get("http://localhost:9090/ehcache/rest/sampleCache2/2");
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
        URL u = new URL("http://localhost:9090/ehcache/rest/sampleCache2/2");
        urlConnection = (HttpURLConnection) u.openConnection();
        urlConnection.setRequestMethod("GET");
        assertEquals(200, urlConnection.getResponseCode());
        assertTrue(urlConnection.getContentType().matches("application/xml"));
        assertEquals(eTag, urlConnection.getHeaderField("Etag"));
        assertEquals(lastModified, urlConnection.getHeaderField("Last-Modified"));
        urlConnection.disconnect();

        Thread.sleep(1100);
        //Need a thread sleep because Element ceilings up
        //Check ETag and Last-Modified are different after the element was updated.
        HttpUtil.put("http://localhost:9090/ehcache/rest/sampleCache2/2", "application/xml",
                new ByteArrayInputStream(xmlDocument.getBytes()));
        Thread.sleep(100);
        u = new URL("http://localhost:9090/ehcache/rest/sampleCache2/2");
        urlConnection = (HttpURLConnection) u.openConnection();
        urlConnection.setRequestMethod("GET");
        assertEquals(200, urlConnection.getResponseCode());
        assertTrue(urlConnection.getContentType().matches("application/xml"));
        String eTagInResponse = urlConnection.getHeaderField("Etag");
        LOG.info("eTag in response: " + urlConnection.getHeaderField("Etag"));
        assertTrue(!eTag.equals(eTagInResponse));
        String content = HttpUtil.inputStreamToText(urlConnection.getInputStream());
        LOG.info("lastModified: " + urlConnection.getHeaderField("Last-Modified"));
        //HttpURLConnection weirdness. It works from wget.
        //assertTrue(!lastModified.equals(urlConnection.getHeaderField("Last-Modified")));

        //Check Caching Behaviour
        u = new URL("http://localhost:9090/ehcache/rest/sampleCache2/2");
        urlConnection = (HttpURLConnection) u.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.setIfModifiedSince(System.currentTimeMillis() + 1000);
        urlConnection.setRequestProperty("If-None-Match", eTagInResponse);

        assertEquals(304, urlConnection.getResponseCode());

    }


    @Test
    public void testGetElement() throws Exception {

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
    public void testEncoding() throws URISyntaxException, MalformedURLException, UnsupportedEncodingException {

        //Uses RFC 2396 - i.e. does not encode the *
        String encoded = URLEncoder.encode("*,", "UTF-8");

        //legal to use * and ,
        URI uri = new URI("http://localhost/ehcache/sampleCache1/*,");
        URI url2 = uri.resolve("http://localhost/ehcache/sampleCache1/*,");

    }




/*
Manual Testing Procedure

HttpURLConnection is a little unwieldy. Also we need to make sure that the implementation works with widely
used HTTP client tools.

wget
====



1. wget -d --timestamping "http://localhost:9090/ehcache/rest/sampleCache2/2"

Expected behaviour: Will check the Last-Modified timestamp against the last modified time on the local filesystem.
This is how mirroring is implemented in wget. See http://www.gnu.org/software/wget/manual/wget.html#HTTP-Time_002dStamping-Internals


Here are some manual tests that can be done with wget and curl to verify correctness of the service.

This test requires data in samplecache2/2.

wget -d --timestamping "http://localhost:9090/ehcache/rest/sampleCache2/2" results in the following interaction:

wget -d --timestamping "http://localhost:9090/ehcache/rest/sampleCache2/2"
Setting --timestamping (timestamping) to 1
DEBUG output created by Wget 1.10.2 on darwin8.8.0.

--18:02:19--  http://localhost:9090/ehcache/rest/sampleCache2/2
           => `2'
Resolving localhost... 127.0.0.1, ::1
Caching localhost => 127.0.0.1 ::1
Connecting to localhost|127.0.0.1|:9090... connected.
Created socket 3.
Releasing 0x004022d0 (new refcount 1).

---request begin---
HEAD /ehcache/rest/sampleCache2/2 HTTP/1.0
User-Agent: Wget/1.10.2
Accept: *\/*
Host: localhost:9090
Connection: Keep-Alive

---request end---
HTTP request sent, awaiting response...
---response begin---
HTTP/1.1 200 OK
X-Powered-By: Servlet/2.5
Server: GlassFish/v3
Last-Modified: Sun, 27 Jul 2008 08:00:05 GMT
ETag: "1217145605632"
Content-Type: text/plain; charset=iso-8859-1
Content-Length: 157
Date: Sun, 27 Jul 2008 08:02:19 GMT
Connection: Keep-Alive

---response end---
200 OK
Registered socket 3 for persistent reuse.
Length: 157 [text/plain]
Server file no newer than local file `2' -- not retrieving.




curl
====

1. OPTIONS test

curl --request OPTIONS http://localhost:9090/ehcache/rest/sampleCache2/2


<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<application xmlns="http://research.sun.com/wadl/2006/10">
<resources base="http://localhost:9090/ehcache/rest/">
<resource path="sampleCache2/2">
<method name="HEAD"><response><representation mediaType="
    ...
</resource>
</resources>
</application>


2. HEAD test

curl --head  http://localhost:9090/ehcache/rest/sampleCache2/2

HTTP/1.1 200 OK
X-Powered-By: Servlet/2.5
Server: GlassFish/v3
Last-Modified: Sun, 27 Jul 2008 08:08:49 GMT
ETag: "1217146129490"
Content-Type: text/plain; charset=iso-8859-1
Content-Length: 157
Date: Sun, 27 Jul 2008 08:17:09 GMT


*/

}

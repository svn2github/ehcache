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
import net.sf.ehcache.server.jaxb.JAXBContextResolver;
import net.sf.ehcache.server.jaxb.Caches;
import net.sf.ehcache.server.soap.jaxws.CacheException_Exception;
import net.sf.ehcache.server.soap.jaxws.NoSuchCacheException_Exception;
import net.sf.ehcache.server.soap.jaxws.Cache;
import net.sf.ehcache.server.soap.jaxws.IllegalStateException_Exception;
import net.sf.ehcache.server.soap.jaxws.ObjectExistsException_Exception;
import net.sf.ehcache.server.soap.jaxws.EhcacheWebServiceEndpointService;
import net.sf.ehcache.server.soap.jaxws.EhcacheWebServiceEndpoint;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.Before;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.ws.soap.SOAPFaultException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.logging.Logger;
import java.util.List;
import java.net.HttpURLConnection;

import com.sun.xml.ws.util.Pool;


/**
 * Tests the REST web resource using the lightweight http container
 * <p/>
 * @author Greg Luck
 * @version $Id$
 */
public class CachesResourceTest {

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
    }



    /**
     * Returns the WADL for the CacheManager operations. Should just list GET
     * @throws Exception
     */
    @Test
    public void testOptions() throws Exception {

        HttpURLConnection result = HttpUtil.options("http://localhost:8080/ehcache/rest/");
        assertEquals(200, result.getResponseCode());
        assertEquals("application/vnd.sun.wadl+xml", result.getContentType());

        String responseBody;
        try {
            responseBody = HttpUtil.inputStreamToText(result.getInputStream());
            assertNotNull(responseBody);
            assertTrue(responseBody.matches("(.*)GET(.*)"));
        } catch (IOException e) {
            //expected
        }
    }


    /**
     * Gets the cache names
     */
    @Test
    public void testCacheNames() throws Exception, IOException, ParserConfigurationException, SAXException {

        HttpURLConnection result = HttpUtil.get("http://localhost:8080/ehcache/rest/");
        assertEquals(200, result.getResponseCode());
        JAXBContext jaxbContext = new JAXBContextResolver().getContext(Caches.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        Caches caches = (Caches) unmarshaller.unmarshal(result.getInputStream());
        List<net.sf.ehcache.server.jaxb.Cache> cacheList = caches.getCaches();
        for (net.sf.ehcache.server.jaxb.Cache cache : cacheList) {
            assertNotNull(cache.getName());
        }

    }



    @Test
    public void testGetCaches() throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        HttpURLConnection result = HttpUtil.get("http://localhost:8080/ehcache/rest/");
        assertEquals(200, result.getResponseCode());

        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = documentBuilder.parse(result.getInputStream());

        XPath xpath = XPathFactory.newInstance().newXPath();
        String cacheCount = xpath.evaluate("count(//caches)", document);
        //some other tests create more
        assertTrue(Integer.parseInt(cacheCount) >= 6);
    }

    @Test
    public void testGetCachesJaxb() throws Exception, SAXException, XPathExpressionException, JAXBException {
        HttpURLConnection result = HttpUtil.get("http://localhost:8080/ehcache/rest/");
        assertEquals(200, result.getResponseCode());
        JAXBContext jaxbContext = new JAXBContextResolver().getContext(Caches.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        Caches caches = (Caches) unmarshaller.unmarshal(result.getInputStream());
        assertTrue(caches.getCaches().size() >= 6);
    }




}
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
import net.sf.ehcache.server.soap.jaxws.EhcacheWebServiceEndpoint;
import net.sf.ehcache.server.soap.jaxws.CacheException_Exception;
import net.sf.ehcache.server.soap.jaxws.IllegalStateException_Exception;
import net.sf.ehcache.server.soap.jaxws.EhcacheWebServiceEndpointService;
import net.sf.ehcache.server.jaxb.JAXBContextResolver;
import net.sf.ehcache.server.jaxb.Caches;
import net.sf.ehcache.server.jaxb.Cache;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.Before;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.ws.soap.SOAPFaultException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.logging.Logger;
import java.util.Map;
import java.net.HttpURLConnection;

import com.sun.jersey.api.NotFoundException;


/**
 * Tests the REST web resource using the lightweight http container
 * <p/>
 *
 * @author Greg Luck
 * @version $Id$
 */
public class CacheResourceTest {

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


    @Test
    public void testGetCache() throws Exception {

        HttpURLConnection result = HttpUtil.get("http://localhost:8080/ehcache/rest/sampleCache1");
        assertEquals(200, result.getResponseCode());
        JAXBContext jaxbContext = new JAXBContextResolver().getContext(Caches.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        Cache cache = (Cache) unmarshaller.unmarshal(result.getInputStream());

        assertEquals("sampleCache1", cache.getName());
        assertEquals("http://localhost:8080/ehcache/rest/sampleCache1", cache.getUri());
        assertNotNull("http://localhost:8080/ehcache/rest/sampleCache1", cache.getDescription());
    }

//    @Test
//    public void testAddCache() throws CacheException_Exception, NoSuchCacheException_Exception, IllegalStateException_Exception, ObjectExistsException_Exception {
//
//        cacheService.addCache("newcache1");
//        Cache cache = cacheService.getCache("newcache1");
//        assertNotNull(cache);
//
//        try {
//            cacheService.addCache("newcache1");
//        } catch (SOAPFaultException e) {
//            //expected
//            assertTrue(e.getCause().getMessage().indexOf("Cache newcache1 already exists") != -1);
//        }
//    }
//
//    @Test
//    public void testRemoveCache() throws CacheException_Exception, NoSuchCacheException_Exception, IllegalStateException_Exception, ObjectExistsException_Exception {
//
//        cacheService.addCache("newcache2");
//        Cache cache = cacheService.getCache("newcache2");
//        assertNotNull(cache);
//
//        cacheService.removeCache("newcache2");
//        cache = cacheService.getCache("newcache2");
//        assertNull(cache);
//
//        //should not throw an exception
//        cacheService.removeCache("newcache2");
//        cache = cacheService.getCache("newcache2");
//        assertNull(cache);
//    }


    @Test
    public void testGet() throws IOException, ParserConfigurationException, SAXException {
        HttpURLConnection result = HttpUtil.get("http://localhost:8080/ehcache/rest/");
        assertEquals(200, result.getResponseCode());
    }


    @Test
    public void testPut() throws IOException, ParserConfigurationException, SAXException {
        HttpUtil.put("http://localhost:8080/ehcache/rest/testCache");
        HttpURLConnection result = HttpUtil.get("http://localhost:8080/ehcache/rest/testCache");
        assertEquals(200, result.getResponseCode());
    }


    @Test
    public void testHead() throws IOException, ParserConfigurationException, SAXException {
        HttpUtil.put("http://localhost:8080/ehcache/rest/testCache");
        HttpURLConnection result = HttpUtil.head("http://localhost:8080/ehcache/rest/testCache");
        LOG.info("Result of HEAD: " + result);
        byte[] bytes = HttpUtil.inputStreamToBytes(result.getInputStream());
        assertEquals(0, bytes.length);
        Map headers = result.getHeaderFields();
        assertNotNull(headers);
    }


}
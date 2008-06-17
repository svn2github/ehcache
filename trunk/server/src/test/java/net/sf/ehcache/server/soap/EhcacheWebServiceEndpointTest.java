/**
 * Tests for the Ehcache WebService
 * @author Greg Luck
 * @version $Id$
 */
package net.sf.ehcache.server.soap;

import net.sf.ehcache.server.soap.jaxws.*;
import net.sf.ehcache.server.soap.jaxws.EhcacheWebServiceEndpoint;
import net.sf.ehcache.server.soap.jaxws.Status;
import org.junit.Test;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

import javax.xml.ws.soap.SOAPFaultException;
import java.util.List;
import java.util.Arrays;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import sun.misc.BASE64Decoder;

public class EhcacheWebServiceEndpointTest {
    private static EhcacheWebServiceEndpoint serviceEndpoint;

    @BeforeClass
    public static void setup() {
        serviceEndpoint = new EhcacheWebServiceEndpointService().getEhcacheWebServiceEndpointPort();
    }

    @Test
    public void testPing() {
        //invoke business method
        String result = serviceEndpoint.ping();
        assertEquals("pong", result);
    }

    @Test
    public void testGetCache() throws CacheException_Exception, NoSuchCacheException_Exception {
        Cache cache = serviceEndpoint.getCache("doesnotexist");
        assertNull(cache);

        cache = serviceEndpoint.getCache("sampleCache1");
        assertEquals("sampleCache1", cache.getName());
        assertEquals("rest/sampleCache1", cache.getUri());
        assertTrue(cache.getDescription().indexOf("sampleCache1") != -1);
    }

    @Test
    public void testAddCache() throws CacheException_Exception, NoSuchCacheException_Exception, IllegalStateException_Exception, ObjectExistsException_Exception {

        serviceEndpoint.addCache("newcache1");
        Cache cache = serviceEndpoint.getCache("newcache1");
        assertNotNull(cache);

        try {
            serviceEndpoint.addCache("newcache1");
        } catch (SOAPFaultException e) {
            //expected
            assertTrue(e.getCause().getMessage().indexOf("Cache newcache1 already exists") != -1);
        }
    }

    @Test
    public void testRemoveCache() throws CacheException_Exception, NoSuchCacheException_Exception, IllegalStateException_Exception, ObjectExistsException_Exception {

        serviceEndpoint.addCache("newcache2");
        Cache cache = serviceEndpoint.getCache("newcache2");
        assertNotNull(cache);

        serviceEndpoint.removeCache("newcache2");
        cache = serviceEndpoint.getCache("newcache2");
        assertNull(cache);

        //should not throw an exception
        serviceEndpoint.removeCache("newcache2");
        cache = serviceEndpoint.getCache("newcache2");
        assertNull(cache);
    }

    /**
     * Gets the cache names
     */
    @Test
    public void testCacheNames() throws IllegalStateException_Exception {
        List cacheNames = serviceEndpoint.cacheNames();
        //Other tests add caches to the CacheManager
        assertTrue(cacheNames.size() >= 6);
    }

    @Test
    public void testCacheStatus() throws CacheException_Exception, NoSuchCacheException_Exception {
        Status status = serviceEndpoint.getStatus("sampleCache1");
        assertTrue(status == Status.STATUS_ALIVE);
    }


    @Test
    public void testCachePutNull() throws CacheException_Exception, NoSuchCacheException_Exception, IllegalStateException_Exception {

        Element element = new Element();
        element.setKey("1");
        serviceEndpoint.put("sampleCache1", element);

        element = serviceEndpoint.get("sampleCache1", "1");
        boolean equals = Arrays.equals(null, element.getValue());
        assertTrue(equals);
    }


    @Test
    public void testCachePut() throws CacheException_Exception, NoSuchCacheException_Exception, IllegalStateException_Exception, IOException {

        Element element = new Element();
        element.setKey("2");
        byte[] bytes1 = new byte[]{1,2,3,4,5,6};
        element.setValue(bytes1);
        serviceEndpoint.put("sampleCache1", element);

        element = serviceEndpoint.get("sampleCache1", "2");
        byte[] bytes2 = element.getValue();
        assertTrue(Arrays.equals(bytes1, bytes2));
    }




}
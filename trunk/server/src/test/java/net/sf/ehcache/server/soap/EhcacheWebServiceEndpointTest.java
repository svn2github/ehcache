/**
 * Tests for the Ehcache WebService
 * @author Greg Luck
 * @version $Id$
 */
package net.sf.ehcache.server.soap;

import net.sf.ehcache.server.soap.jaxws.*;
import net.sf.ehcache.server.soap.jaxws.EhcacheWebServiceEndpoint;
import org.junit.Test;
import org.junit.Assert;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

import java.util.List;

public class EhcacheWebServiceEndpointTest {
    private static EhcacheWebServiceEndpoint endpoint;

    @BeforeClass
    public static void setup() {
        endpoint = new EhcacheWebServiceEndpointService().getEhcacheWebServiceEndpointPort();
    }

    @Test
    public void testPing() {
        //invoke business method
        String result = endpoint.ping();
        assertEquals("pong", result);
    }

    /**
     * Gets the cache names
     * @throws IllegalStateException_Exception
     */
    @Test
    public void testCacheNames() throws IllegalStateException_Exception {
        //invoke business method
        List cacheNames = endpoint.cacheNames();
        //Other tests add caches to the CacheManager
        assertTrue(cacheNames.size() >= 6);
    }

    /**
     * This will throw an exception
     *
     * @throws IllegalStateException_Exception
     *
     */
    @Test
    public void testCacheDoesNotExist() throws CacheException_Exception, NoSuchCacheException_Exception {
        //invoke business method
        Cache cache = endpoint.getCache("doesnotexist");
        assertNull(cache);

        cache = endpoint.getCache("sampleCache1");
        assertEquals("sampleCache1", cache.getName());
        //todo what should this be
        assertEquals("rest/sampleCache1", cache.getUri());
        assertTrue(cache.getDescription().indexOf("sampleCache1") != -1);


    }


}
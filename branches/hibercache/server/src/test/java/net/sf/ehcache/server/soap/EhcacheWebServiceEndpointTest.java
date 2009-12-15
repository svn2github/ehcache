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


package net.sf.ehcache.server.soap;

import net.sf.ehcache.server.soap.jaxws.Cache;
import net.sf.ehcache.server.soap.jaxws.CacheException_Exception;
import net.sf.ehcache.server.soap.jaxws.EhcacheWebServiceEndpoint;
import net.sf.ehcache.server.soap.jaxws.EhcacheWebServiceEndpointService;
import net.sf.ehcache.server.soap.jaxws.Element;
import net.sf.ehcache.server.soap.jaxws.IllegalStateException_Exception;
import net.sf.ehcache.server.soap.jaxws.NoSuchCacheException_Exception;
import net.sf.ehcache.server.soap.jaxws.Statistics;
import net.sf.ehcache.server.soap.jaxws.StatisticsAccuracy;
import net.sf.ehcache.server.soap.jaxws.Status;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.ws.soap.SOAPFaultException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests for the Ehcache WebService
 * @author Greg Luck
 * @version $Id$
 */
public class EhcacheWebServiceEndpointTest {
    private static EhcacheWebServiceEndpoint cacheService;
    private String cacheName = "sampleCache1";

    @BeforeClass
    public static void beforeClass() {
        cacheService = new EhcacheWebServiceEndpointService().getEhcacheWebServiceEndpointPort();

        //add security credentials
        // Uncomment and Enable XWSS config files to run tests with security
//        ((BindingProvider)cacheService).getRequestContext().put(BindingProvider.USERNAME_PROPERTY, "Ron");
//        ((BindingProvider)cacheService).getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, "noR");

    }

    @Before
    public void zeroOutCache() throws CacheException_Exception, IllegalStateException_Exception {
        cacheService.removeAll(cacheName);
        cacheService.removeAll("sampleCache3");
    }


    @Test
    public void testPing() {
        //invoke business method
        String result = cacheService.ping();
        assertEquals("pong", result);
    }

    @Test
    public void testGetCache() throws CacheException_Exception, NoSuchCacheException_Exception {
        Cache cache = cacheService.getCache("doesnotexist");
        assertNull(cache);

        cache = cacheService.getCache("sampleCache1");
        assertEquals("sampleCache1", cache.getName());
        assertEquals("rest/sampleCache1", cache.getUri());
        assertTrue(cache.getDescription().indexOf("sampleCache1") != -1);
    }

    @Test
    public void testAddCache() throws Exception {

        cacheService.addCache("newcache1");
        Cache cache = cacheService.getCache("newcache1");
        assertNotNull(cache);

        try {
            cacheService.addCache("newcache1");
        } catch (SOAPFaultException e) {
            //expected
            assertTrue(e.getCause().getMessage().indexOf("Cache newcache1 already exists") != -1);
        }
    }

    @Test
    public void testRemoveCache() throws Exception {

        cacheService.addCache("newcache2");
        Cache cache = cacheService.getCache("newcache2");
        assertNotNull(cache);

        cacheService.removeCache("newcache2");
        cache = cacheService.getCache("newcache2");
        assertNull(cache);

        //should not throw an exception
        cacheService.removeCache("newcache2");
        cache = cacheService.getCache("newcache2");
        assertNull(cache);
    }

    /**
     * Gets the cache names
     */
    @Test
    public void testCacheNames() throws IllegalStateException_Exception {
        List cacheNames = cacheService.cacheNames();
        //Other tests add caches to the CacheManager
        assertTrue(cacheNames.size() >= 6);
    }


    @Test
    public void testCacheStatus() throws CacheException_Exception, NoSuchCacheException_Exception {
        Status status = cacheService.getStatus("sampleCache1");
        assertTrue(status == Status.STATUS_ALIVE);
    }


    @Test
    public void testCachePutNull() throws Exception,
            NoSuchCacheException_Exception, IllegalStateException_Exception {

        Element element = new Element();
        element.setKey("1");
        cacheService.put("sampleCache1", element);

        element = getElementFromCache();
        boolean equals = Arrays.equals(null, element.getValue());
        assertTrue(equals);
    }

    private Element getElementFromCache() throws Exception {
        Element element;
        element = cacheService.get("sampleCache1", "1");
        return element;
    }

    /**
     * Tests get, getQuiet and put, putQuiet
     */
    @Test
    public void testCacheGetPut() throws CacheException_Exception,
            NoSuchCacheException_Exception, Exception {

        Element element = new Element();
        element.setKey("2");
        byte[] bytes1 = new byte[]{1, 2, 3, 4, 5, 6};
        element.setValue(bytes1);

        cacheService.put("sampleCache1", element);
        element = cacheService.get("sampleCache1", "2");
        byte[] bytes2 = element.getValue();
        assertTrue(Arrays.equals(bytes1, bytes2));
        cacheService.remove("sampleCache1", "2");

        cacheService.putQuiet("sampleCache1", element);
        element = cacheService.get("sampleCache1", "2");
        bytes2 = element.getValue();
        assertTrue(Arrays.equals(bytes1, bytes2));
        cacheService.remove("sampleCache1", "2");

        cacheService.put("sampleCache1", element);
        element = cacheService.getQuiet("sampleCache1", "2");
        bytes2 = element.getValue();
        assertTrue(Arrays.equals(bytes1, bytes2));
        cacheService.remove("sampleCache1", "2");

        //ttl override
        Element expiryOverrideElement = new Element();
        expiryOverrideElement.setKey("abc");
        expiryOverrideElement.setValue("value".getBytes());
        expiryOverrideElement.setTimeToLiveSeconds(1);
        cacheService.put("sampleCache1", expiryOverrideElement);
        Thread.sleep(2010);
        element = cacheService.get("sampleCache1", "abc");
        assertEquals(null, element);


    }

    @Test
    public void testDefaultExpiry() throws Exception {
        Element element2 = new Element();
        element2.setKey("2");
        element2.setValue(new byte[]{1, 2, 3, 4, 5, 6});
        cacheService.put("sampleCache3", element2);
        assertNotNull(cacheService.get("sampleCache3", "2"));
        Thread.sleep(2010);
        assertEquals(null, cacheService.get("sampleCache3", "2"));

    }

    @Test
    public void testOverrideEternal() throws Exception {
        Element element = new Element();
        element.setKey("2");
        element.setValue(new byte[]{1, 2, 3, 4, 5, 6});
        element.setEternal(true);
        cacheService.put("sampleCache3", element);
        assertNotNull(cacheService.get("sampleCache3", "2"));
        Thread.sleep(1010);
        //should not expire
        assertNotNull(cacheService.get("sampleCache3", "2"));
    }


    @Test
    public void testOverrideTTI() throws Exception {
        Element element = new Element();
        element.setKey("2");
        element.setValue(new byte[]{1, 2, 3, 4, 5, 6});
        element.setTimeToIdleSeconds(1);
        cacheService.put("sampleCache3", element);
        assertNotNull(cacheService.get("sampleCache3", "2"));
        Thread.sleep(2010);
        //should expire
        assertNull(cacheService.get("sampleCache3", "2"));
    }

    /**
     * Test getKeys() and its veriants
     */
    @Test
    public void testGetKeys() throws NoSuchCacheException_Exception,
            CacheException_Exception, IllegalStateException_Exception {

        for (int i = 0; i < 1000; i++) {
            Element element = new Element();
            element.setKey(i);
            element.setValue(("value" + i).getBytes());

            cacheService.put("sampleCache1", element);
        }

        List keys = cacheService.getKeys("sampleCache1");
        assertEquals(1000, keys.size());

        keys = cacheService.getKeysWithExpiryCheck("sampleCache1");
        assertEquals(1000, keys.size());

        keys = cacheService.getKeysNoDuplicateCheck("sampleCache1");
        assertEquals(1000, keys.size());

    }


    @Test
    public void testRemove() throws NoSuchCacheException_Exception,
            CacheException_Exception, IllegalStateException_Exception {

        putElementIntoCache();

        assertEquals(1, cacheService.getSize("sampleCache1"));
    }

    private void putElementIntoCache() throws CacheException_Exception, NoSuchCacheException_Exception {
        Element element = new Element();
        element.setKey("1");
        element.setValue(("value1").getBytes());
        cacheService.put("sampleCache1", element);
    }


    @Test
    public void testGetStatistics() throws Exception {
        cacheService.clearStatistics("sampleCache1");

        Statistics statistics = cacheService.getStatistics("sampleCache1");
        assertEquals(0L, statistics.getCacheHits());

        putElementIntoCache();
        getElementFromCache();

        statistics = cacheService.getStatistics("sampleCache1");
        assertEquals(1L, statistics.getCacheHits());
        assertTrue(statistics.getAverageGetTime() >= 0);
        assertEquals(0L, statistics.getEvictionCount());
        assertEquals(1L, statistics.getInMemoryHits());
        assertEquals(0L, statistics.getOnDiskHits());
        assertEquals(StatisticsAccuracy.STATISTICS_ACCURACY_BEST_EFFORT, statistics.getStatisticsAccuracy());
    }

    @Test
    public void testGetStatisticsAccuracy() throws NoSuchCacheException_Exception,
            CacheException_Exception, IllegalStateException_Exception {
        assertEquals(StatisticsAccuracy.STATISTICS_ACCURACY_BEST_EFFORT,
                cacheService.getStatisticsAccuracy("sampleCache1"));
    }

    @Test
    public void testClearStatistics() throws Exception {
        putElementIntoCache();
        getElementFromCache();

        cacheService.clearStatistics("sampleCache1");
        Statistics statistics = cacheService.getStatistics("sampleCache1");
        assertEquals(0L, statistics.getCacheHits());
    }


    /**
     * No loader configured. smoke test only
     */
    @Test
    public void testLoad() throws NoSuchCacheException_Exception,
            CacheException_Exception, IllegalStateException_Exception {
        cacheService.load("sampleCache1", "2");
    }

    /**
     * No loader configured. smoke test only
     */
    @Test
    public void testLoadAll() throws NoSuchCacheException_Exception,
            CacheException_Exception, IllegalStateException_Exception {
        List keys = new ArrayList();
        for (int i = 0; i < 5; i++) {
            keys.add("" + i);
        }
        cacheService.loadAll("sampleCache1", keys);
    }


    /**
     * No loader configured. smoke test only
     */
    @Test
    public void testGetWithLoad() throws NoSuchCacheException_Exception,
            CacheException_Exception, IllegalStateException_Exception {
        cacheService.getWithLoader("sampleCache1", "2");
    }

    /**
     * No loader configured. smoke test only
     */
    @Test
    public void testGetAllWithLoader() throws NoSuchCacheException_Exception,
            CacheException_Exception, IllegalStateException_Exception {
        List keys = new ArrayList();
        for (int i = 0; i < 5; i++) {
            keys.add("" + i);
        }
        cacheService.getAllWithLoader("sampleCache1", keys);
    }

}

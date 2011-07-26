/**
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
 *
 * @author Pierre Monestie (pmonestie__REMOVE__THIS__@gmail.com)
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */

package net.sf.ehcache.distribution.jgroups;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.distribution.CacheManagerPeerProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;



/**
 * Test JGroups replication
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 */
public class JGroupsReplicationTest {
    private static final long MAX_WAIT_TIME = 60000;

    
    private static final String SAMPLE_CACHE_NOREP = "sampleCacheNorep";

    private static final int NBR_ELEMENTS = 100;
    
    private static final String SAMPLE_CACHE1 = "sampleCacheAsync";
    private static final String SAMPLE_CACHE2 = "sampleCacheAsync2";
    private static final Logger LOG = LoggerFactory.getLogger(JGroupsReplicationTest.class.getName());
    
    /**
     * Used for getting the current test's name
     */
    @Rule public TestName name = new TestName();

    private CacheManager manager1;
    private CacheManager manager2;
    private CacheManager manager3;
    private CacheManager manager4;

    private String cacheName;
    
    @Before
    public void setUp() throws Exception {
        CacheTestUtilities.startTest(name.getMethodName());
        LOG.info("SETUP");
        
        manager1 = new CacheManager(CacheTestUtilities.ASYNC_CONFIG_URL1);
        CacheTestUtilities.waitForBootstrap(manager1, MAX_WAIT_TIME);
        
        manager2 = new CacheManager(CacheTestUtilities.ASYNC_CONFIG_URL2);
        CacheTestUtilities.waitForBootstrap(manager2, MAX_WAIT_TIME);
        
        manager3 = new CacheManager(CacheTestUtilities.ASYNC_CONFIG_URL3);
        CacheTestUtilities.waitForBootstrap(manager3, MAX_WAIT_TIME);
        
        manager4 = new CacheManager(CacheTestUtilities.ASYNC_CONFIG_URL4);
        CacheTestUtilities.waitForBootstrap(manager4, MAX_WAIT_TIME);
        
        cacheName = SAMPLE_CACHE1;
    }

    @After
    public void tearDown() throws Exception {
        LOG.debug("Tearing down cm1");
        manager1.shutdown();
        LOG.debug("Tearing down cm2");
        manager2.shutdown();
        LOG.debug("Tearing down cm3");
        manager3.shutdown();
        LOG.debug("Tearing down cm4");
        manager4.shutdown();
        
        LOG.info("TEARDOWN");
        CacheTestUtilities.endTest();
    }

    @Test
    public void testBasicReplication() throws Exception {
        LOG.info("START TEST");

        final Ehcache cache1 = manager1.getEhcache(cacheName);
        final Ehcache cache2 = manager2.getEhcache(cacheName);
        final Ehcache cache3 = manager3.getEhcache(cacheName);
        final Ehcache cache4 = manager4.getEhcache(cacheName);
        
        for (int i = 0; i < NBR_ELEMENTS; i++) {
            cache1.put(new Element(i, "testdat"));
        }
        
        //Wait up to 3 seconds for the caches to become coherent
        CacheTestUtilities.waitForReplication(NBR_ELEMENTS, MAX_WAIT_TIME, cache2, cache3, cache4);
        
        assertEquals(NBR_ELEMENTS, cache1.getKeys().size());
        assertEquals(NBR_ELEMENTS, cache2.getKeys().size());
        assertEquals(NBR_ELEMENTS, cache3.getKeys().size());
        assertEquals(NBR_ELEMENTS, cache4.getKeys().size());

        cache1.removeAll();
        
        //Wait up to 3 seconds for the caches to become coherent
        CacheTestUtilities.waitForReplication(0, MAX_WAIT_TIME, cache2, cache3, cache4);
        
        assertEquals(0, cache1.getKeys().size());
        assertEquals(0, cache2.getKeys().size());
        assertEquals(0, cache3.getKeys().size());
        assertEquals(0, cache4.getKeys().size());

        LOG.info("END TEST");
    }

    @Test
    public void testCASOperationsNotSupported() throws Exception {
        LOG.info("START TEST");

        final Ehcache cache1 = manager1.getEhcache(cacheName);
        final Ehcache cache2 = manager2.getEhcache(cacheName);
        final Ehcache cache3 = manager3.getEhcache(cacheName);
        final Ehcache cache4 = manager4.getEhcache(cacheName);
        
        try {
            cache1.putIfAbsent(new Element("foo", "poo"));
            throw new AssertionError("CAS operation should have failed.");
        } catch (CacheException ce) {
            assertEquals(true, ce.getMessage().contains("CAS"));
        }

        try {
            cache2.removeElement(new Element("foo", "poo"));
            throw new AssertionError("CAS operation should have failed.");
        } catch (CacheException ce) {
            assertEquals(true, ce.getMessage().contains("CAS"));
        }

        try {
            cache3.replace(new Element("foo", "poo"));
            throw new AssertionError("CAS operation should have failed.");
        } catch (CacheException ce) {
            assertEquals(true, ce.getMessage().contains("CAS"));
        }

        try {
            cache4.replace(new Element("foo", "poo"), new Element("foo", "poo2"));
            throw new AssertionError("CAS operation should have failed.");
        } catch (CacheException ce) {
            assertEquals(true, ce.getMessage().contains("CAS"));
        }

        LOG.info("END TEST");
    }

    @Test
    public void testShutdownManager() throws Exception {
        LOG.info("START TEST");
        
        cacheName = SAMPLE_CACHE1;
        final Ehcache cache1 = manager1.getEhcache(cacheName);
        final Ehcache cache2 = manager2.getEhcache(cacheName);
        
        cache1.removeAll();
        
        //Wait up to 2 seconds for the caches to become coherent
        CacheTestUtilities.waitForReplication(0, MAX_WAIT_TIME, cache2);

        CacheManagerPeerProvider provider = manager1.getCacheManagerPeerProvider(JGroupsCacheManagerPeerProvider.SCHEME_NAME);
        JGroupsCacheManagerPeerProvider jg = (JGroupsCacheManagerPeerProvider) provider;
        assertEquals(Status.STATUS_ALIVE, jg.getStatus());

        manager1.shutdown();
        assertEquals(Status.STATUS_UNINITIALISED, jg.getStatus());

        //Lets see if the other still replicate
        
        cache2.put(new Element(1, new Date()));
        
        final Ehcache cache3 = manager3.getEhcache(cacheName);
        final Ehcache cache4 = manager4.getEhcache(cacheName);
        
        //Wait up to 2 seconds for the caches to become coherent
        CacheTestUtilities.waitForReplication(1, MAX_WAIT_TIME, cache3, cache4);

        try {
            cache1.getKeys();
            fail("");
        } catch (IllegalStateException e) {
            //expected
        }
        assertEquals(1, cache2.getKeys().size());
        assertEquals(1, cache3.getKeys().size());
        assertEquals(1, cache4.getKeys().size());
        
        LOG.info("END TEST");
    }

    @Test
    public void testAddManager() throws Exception {
        LOG.info("START TEST");
        
        cacheName = "sampleCacheSyncBootstrap";
        if (manager1.getStatus() != Status.STATUS_SHUTDOWN) {
            manager1.shutdown();
        }
        
        manager1 = new CacheManager(CacheTestUtilities.ASYNC_CONFIG_URL1);
        CacheTestUtilities.waitForBootstrap(manager1, MAX_WAIT_TIME);
        
        final Ehcache cache1 = manager1.getEhcache(cacheName);
        final Ehcache cache2 = manager2.getEhcache(cacheName);
        final Ehcache cache3 = manager3.getEhcache(cacheName);
        final Ehcache cache4 = manager4.getEhcache(cacheName);
        
        //Wait up to 2 seconds for the caches to become coherent
        CacheTestUtilities.waitForReplication(0, MAX_WAIT_TIME, cache1, cache2, cache3, cache4);

        cache1.put(new Element(1, new Date()));
        cache2.put(new Element(2, new Date()));

        //Wait up to 2 seconds for the caches to become coherent
        CacheTestUtilities.waitForReplication(2, MAX_WAIT_TIME, cache1, cache2, cache3, cache4);
        
        assertEquals(2, cache1.getKeys().size());
        assertEquals(2, cache2.getKeys().size());
        assertEquals(2, cache3.getKeys().size());
        assertEquals(2, cache4.getKeys().size());
        
        LOG.info("END TEST");
    }


    @Test
    public void testConfig() throws InterruptedException {
        LOG.info("START TEST");
        
        cacheName = SAMPLE_CACHE_NOREP;
        Ehcache cache1 = manager1.getEhcache(cacheName);
        Ehcache cache2 = manager2.getEhcache(cacheName);
        Element element = new Element(1, new Date());
        cache2.put(element);
        
        //Wait up 2 seconds to see if replication happens (it shouldn't)
        Thread.sleep(2000);

        assertEquals(0, cache1.getKeys().size());
        assertEquals(1, cache2.getKeys().size());
        
        LOG.info("END TEST");
    }

    /**
     * What happens when two cache instances replicate to each other and a change is initiated
     * @throws InterruptedException -
     */
    @Test
    public void testVariousPuts() throws InterruptedException {
        LOG.info("START TEST");
        
        cacheName = SAMPLE_CACHE1;
        Ehcache cache1 = manager1.getEhcache(cacheName);
        Ehcache cache2 = manager2.getEhcache(cacheName);

        Serializable key = "1";
        Serializable value = new Date();
        Element element = new Element(key, value);

        //Put
        cache1.put(element);

        //Wait up to 2 seconds for the caches to become coherent
        CacheTestUtilities.waitForReplication(1, MAX_WAIT_TIME, cache2);

        //Should have been replicated to cache2.
        Element element2 = cache2.get(key);
        assertEquals(element, element2);

        //Remove
        cache1.remove(key);
        assertNull(cache1.get(key));

        //Wait up to 2 seconds for the caches to become coherent
        CacheTestUtilities.waitForReplication(0, MAX_WAIT_TIME, cache2);

        element2 = cache2.get(key);
        assertNull(element2);

        //Put into 2
        Element element3 = new Element("3", "ddsfds");
        cache2.put(element3);

        //Wait up to 2 seconds for the caches to become coherent
        CacheTestUtilities.waitForReplication(1, MAX_WAIT_TIME, cache2);

        Element element4 = cache2.get("3");
        assertEquals(element3, element4);

        manager1.clearAll();

        LOG.info("END TEST");
    }

    @Test
    public void testSimultaneousPutRemove() throws InterruptedException {
        LOG.info("START TEST");
        
        //Synced one
        cacheName = SAMPLE_CACHE2;
        Ehcache cache1 = manager1.getEhcache(cacheName);
        Ehcache cache2 = manager2.getEhcache(cacheName);


        Serializable key = "1";
        Serializable value = new Date();
        Element element = new Element(key, value);

        //Put
        cache1.put(element);

        //Wait up to 2 seconds for the caches to become coherent
        CacheTestUtilities.waitForReplication(1, MAX_WAIT_TIME, cache2);

        cache2.remove(element.getKey());
        
        //Wait up to 2 seconds for the caches to become coherent
        CacheTestUtilities.waitForReplication(0, MAX_WAIT_TIME, cache1);

        assertNull(cache1.get(element.getKey()));
        manager1.clearAll();

        //Wait up to 2 seconds for the caches to become coherent
        CacheTestUtilities.waitForReplication(0, MAX_WAIT_TIME, cache2);

        cache2.put(element);
        cache2.remove(element.getKey());

        //Wait up to 2 seconds for the caches to become coherent
        CacheTestUtilities.waitForReplication(0, MAX_WAIT_TIME, cache2);

        cache1.put(element);
        
        //Wait up to 2 seconds for the caches to become coherent
        CacheTestUtilities.waitForReplication(1, MAX_WAIT_TIME, cache2);

        assertNotNull(cache2.get(element.getKey()));

        manager1.clearAll();
        
        LOG.info("END TEST");
    }

}

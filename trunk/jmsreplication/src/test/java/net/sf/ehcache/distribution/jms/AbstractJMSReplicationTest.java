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

package net.sf.ehcache.distribution.jms;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.util.Date;
import java.util.logging.Logger;

import com.sun.messaging.ConnectionConfiguration;

import javax.jms.Connection;

public abstract class AbstractJMSReplicationTest {

    private static final int NBR_ELEMENTS = 100;

    private static final String SAMPLE_CACHE_ASYNC = "sampleCacheAsync";
    private static final String SAMPLE_CACHE_SYNC = "sampleCacheSync";
    private static final String SAMPLE_CACHE_NOREP = "sampleCacheNorep";

    String cacheName;

    private static final Logger LOG = Logger.getLogger(AbstractJMSReplicationTest.class.getName());

    protected CacheManager manager1, manager2, manager3, manager4, manager5;

    protected abstract String getConfigurationFile();

    @Before
    public void setUp() throws Exception {

        manager1 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + getConfigurationFile());
        manager2 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + getConfigurationFile());
        manager3 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + getConfigurationFile());
        manager4 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + getConfigurationFile());
        cacheName = SAMPLE_CACHE_ASYNC;
        Thread.sleep(200);
    }

    @After
    public void tearDown() throws Exception {
        if (manager1 != null) {
            manager1.shutdown();
        }
        if (manager2 != null) {
            manager2.shutdown();
        }
        if (manager3 != null) {
            manager3.shutdown();
        }
        if (manager4 != null) {
            manager4.shutdown();
        }
    }


    @Test
    public void testBasicReplicationAsynchronous() throws Exception {
        cacheName = SAMPLE_CACHE_ASYNC;
        basicReplicationTest();
    }

    @Test
    public void testBasicReplicationSynchronous() throws Exception {
        cacheName = SAMPLE_CACHE_SYNC;
        basicReplicationTest();
    }

    @Test
    public void testStartupAndShutdown() {
        //noop
    }


    public void basicReplicationTest() throws Exception {

        //put
        for (int i = 0; i < NBR_ELEMENTS; i++) {
            manager1.getCache(cacheName).put(new Element(i, "testdat"));
        }
        Thread.sleep(3000);

        LOG.info(manager1.getCache(cacheName).getKeys().size() + "  " + manager2.getCache(cacheName).getKeys().size()
                + " " + manager3.getCache(cacheName).getKeys().size()
                + " " + manager4.getCache(cacheName).getKeys().size());

        assertTrue(manager1.getCache(cacheName).getKeys().size() == manager2.getCache(cacheName).getKeys().size() &&
                manager1.getCache(cacheName).getKeys().size() == manager3.getCache(cacheName).getKeys().size() &&
                manager1.getCache(cacheName).getKeys().size() == manager4.getCache(cacheName).getKeys().size() &&
                manager1.getCache(cacheName).getKeys().size() == NBR_ELEMENTS);

        //update via copy
        for (int i = 0; i < NBR_ELEMENTS; i++) {
            manager1.getCache(cacheName).put(new Element(i, "testdat"));
        }
        Thread.sleep(3000);

        LOG.info(manager1.getCache(cacheName).getKeys().size() + "  " + manager2.getCache(cacheName).getKeys().size()
                + " " + manager3.getCache(cacheName).getKeys().size()
                + " " + manager4.getCache(cacheName).getKeys().size());

        assertTrue(manager1.getCache(cacheName).getKeys().size() == manager2.getCache(cacheName).getKeys().size() &&
                manager1.getCache(cacheName).getKeys().size() == manager3.getCache(cacheName).getKeys().size() &&
                manager1.getCache(cacheName).getKeys().size() == manager4.getCache(cacheName).getKeys().size() &&
                manager1.getCache(cacheName).getKeys().size() == NBR_ELEMENTS);


        //remove
        manager1.getCache(cacheName).remove(0);
        Thread.sleep(1010);

        LOG.info(manager1.getCache(cacheName).getKeys().size() + "  " + manager2.getCache(cacheName).getKeys().size()
                + " " + manager3.getCache(cacheName).getKeys().size()
                + " " + manager4.getCache(cacheName).getKeys().size());

        assertTrue(manager1.getCache(cacheName).getKeys().size() == manager2.getCache(cacheName).getKeys().size() &&
                manager1.getCache(cacheName).getKeys().size() == manager3.getCache(cacheName).getKeys().size() &&
                manager1.getCache(cacheName).getKeys().size() == manager4.getCache(cacheName).getKeys().size() &&
                manager1.getCache(cacheName).getKeys().size() == NBR_ELEMENTS - 1);

        //removeall
        manager1.getCache(cacheName).removeAll();
        Thread.sleep(1010);

        LOG.info(manager1.getCache(cacheName).getKeys().size() + "  " + manager2.getCache(cacheName).getKeys().size()
                + " " + manager3.getCache(cacheName).getKeys().size()
                + " " + manager4.getCache(cacheName).getKeys().size());

        assertTrue(manager1.getCache(cacheName).getKeys().size() == manager2.getCache(cacheName).getKeys().size() &&
                manager1.getCache(cacheName).getKeys().size() == manager3.getCache(cacheName).getKeys().size() &&
                manager1.getCache(cacheName).getKeys().size() == manager4.getCache(cacheName).getKeys().size() &&
                manager1.getCache(cacheName).getKeys().size() == 0);

    }


//    @Test
//    public void testShutdownManager() throws Exception {
//        cacheName = SAMPLE_CACHE_ASYNC;
//        manager1.getCache(cacheName).removeAll();
//        Thread.currentThread().sleep(1000);
//
//        CacheManagerPeerProvider provider = manager1.getCachePeerProvider();
//        JGroupManager jg = (JGroupManager) provider;
//        assertEquals(Status.STATUS_ALIVE, jg.getStatus());
//        manager1.shutdown();
//        assertEquals(Status.STATUS_SHUTDOWN, jg.getStatus());
//        //Lets see if the other still replicate
//        manager2.getCache(cacheName).put(new Element(new Integer(1), new Date()));
//        Thread.currentThread().sleep(2000);
//
//
//        assertTrue(manager2.getCache(cacheName).getKeys().size() == manager3.getCache(cacheName).getKeys().size() &&
//                manager2.getCache(cacheName).getKeys().size() == manager4.getCache(cacheName).getKeys().size() &&
//                manager2.getCache(cacheName).getKeys().size() == 1);
//
//
//    }

    @Test
    public void testAddManager() throws Exception {
        cacheName = SAMPLE_CACHE_ASYNC;
        if (manager1.getStatus() != Status.STATUS_SHUTDOWN)
            manager1.shutdown();


        Thread.sleep(1000);
        manager1 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + getConfigurationFile());
        Thread.sleep(3000);
        manager2.clearAll();

        Thread.sleep(1000);

        manager2.getCache(cacheName).put(new Element(2, new Date()));
        manager1.getCache(cacheName).put(new Element(3, new Date()));
        Thread.sleep(2000);

        assertTrue(manager1.getCache(cacheName).getKeys().size() == manager2.getCache(cacheName).getKeys().size() &&
                manager1.getCache(cacheName).getKeys().size() == manager3.getCache(cacheName).getKeys().size() &&
                manager1.getCache(cacheName).getKeys().size() == manager4.getCache(cacheName).getKeys().size() &&
                manager1.getCache(cacheName).getKeys().size() == 2);

    }


    @Test
    public void testNoreplication() throws InterruptedException {
        cacheName = SAMPLE_CACHE_NOREP;
        Ehcache cache1 = manager1.getCache(cacheName);
        Ehcache cache2 = manager2.getCache(cacheName);
        Element element = new Element(1, new Date());

        //put
        cache2.put(element);
        Thread.sleep(1000);
        assertTrue(cache1.getKeys().size() == 0 && cache2.getKeys().size() == 1);

        //update
        cache2.put(element);
        Thread.sleep(1000);
        assertTrue(cache1.getKeys().size() == 0 && cache2.getKeys().size() == 1);

        //remove
        cache1.put(element);
        cache1.remove(1);
        Thread.sleep(1000);
        assertTrue(cache1.getKeys().size() == 0 && cache2.getKeys().size() == 1);

        //removeAll
        cache1.removeAll();
        Thread.sleep(1000);
        assertTrue(cache1.getKeys().size() == 0 && cache2.getKeys().size() == 1);

    }

    /**
     * What happens when two cache instances replicate to each other and a change is initiated
     *
     * @throws InterruptedException -
     */
    @Test
    public void testVariousPuts() throws InterruptedException {
        cacheName = SAMPLE_CACHE_ASYNC;
        Ehcache cache1 = manager1.getCache(cacheName);
        Ehcache cache2 = manager2.getCache(cacheName);

        Serializable key = "1";
        Serializable value = new Date();
        Element element = new Element(key, value);

        //Put
        cache1.put(element);
        Thread.sleep(1000);

        //Should have been replicated to cache2.
        Element element2 = cache2.get(key);
        assertEquals(element, element2);


        //Remove
        cache1.remove(key);
        assertNull(cache1.get(key));

        //Should have been replicated to cache2.
        Thread.sleep(1000);
        element2 = cache2.get(key);
        assertNull(element2);

        //Put into 2
        Element element3 = new Element("3", "ddsfds");
        cache2.put(element3);
        Thread.sleep(1000);
        Element element4 = cache2.get("3");
        assertEquals(element3, element4);

        manager1.clearAll();
        Thread.sleep(1000);

    }


    /**
     * What happens when two cache instances replicate to each other and a change is initiated
     *
     * @throws InterruptedException -
     */
    @Test
    public void testPutAndRemove() throws InterruptedException {
        cacheName = SAMPLE_CACHE_SYNC;
        Ehcache cache1 = manager1.getCache(cacheName);
        Ehcache cache2 = manager2.getCache(cacheName);

        Serializable key = "1";
        Serializable value = new Date();
        Element element = new Element(key, value);

        //Put
        cache1.put(element);
        long version = element.getVersion();
        Thread.sleep(1050);
        //make sure we are not getting our own circular update back
        assertEquals(version, cache1.get(key).getVersion());

        //Should have been replicated to cache2.
        Element element2 = cache2.get(key);
        assertEquals(element, element2);


        //Remove
        cache1.remove(key);
        assertNull(cache1.get(key));

        //Should have been replicated to cache2.
        Thread.sleep(1050);
        element2 = cache2.get(key);
        assertNull(element2);

    }


    /**
     * Uses the JMSCacheLoader.
     * <p/>
     * We put an item in cache1, which does not replicate.
     * <p/>
     * We then do a get on cache2, which has a JMSCacheLoader which should ask the cluster for the answer.
     * If a cache does not have an element it should leave the message on the queue for the next node to process.
     */
    @Test
    public void testGet() throws InterruptedException {
        cacheName = SAMPLE_CACHE_SYNC;
        Ehcache cache1 = manager1.getCache("sampleCacheNorep");
        Ehcache cache2 = manager2.getCache("sampleCacheNorep");

        Serializable key = "1";
        Serializable value = new Date();
        Element element = new Element(key, value);

        //Put
        cache1.put(element);
        long version = element.getVersion();
        Thread.sleep(1050);


        //Should not have been replicated to cache2.
        Element element2 = cache2.get(key);
        assertEquals(null, element2);

        //Should load from cache1
        element2 = cache2.getWithLoader(key, null, null);
        assertEquals(value, element2.getValue());
    }


    @Test
    public void testSimultaneousPutRemove() throws InterruptedException {
        cacheName = SAMPLE_CACHE_SYNC; //Synced one
        Ehcache cache1 = manager1.getCache(cacheName);
        Ehcache cache2 = manager2.getCache(cacheName);


        Serializable key = "1";
        Serializable value = new Date();
        Element element = new Element(key, value);

        //Put
        cache1.put(element);
        Thread.sleep(1000);
        cache2.remove(element.getKey());
        Thread.sleep(1000);


        assertNull(cache1.get(element.getKey()));
        manager1.clearAll();
        Thread.sleep(1000);

        cache2.put(element);
        cache2.remove(element.getKey());
        Thread.sleep(1000);
        cache1.put(element);
        Thread.sleep(1000);
        assertNotNull(cache2.get(element.getKey()));

        manager1.clearAll();
        Thread.sleep(1000);

    }


}

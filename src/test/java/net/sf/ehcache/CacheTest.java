/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 - 2004 Greg Luck.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by Greg Luck
 *       (http://sourceforge.net/users/gregluck) and contributors.
 *       See http://sourceforge.net/project/memberlist.php?group_id=93232
 *       for a list of contributors"
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "EHCache" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For written
 *    permission, please contact Greg Luck (gregluck at users.sourceforge.net).
 *
 * 5. Products derived from this software may not be called "EHCache"
 *    nor may "EHCache" appear in their names without prior written
 *    permission of Greg Luck.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL GREG LUCK OR OTHER
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by contributors
 * individuals on behalf of the EHCache project.  For more
 * information on EHCache, please see <http://ehcache.sourceforge.net/>.
 *
 */


package net.sf.ehcache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Date;

/**
 * Tests for a Cache
 *
 * @author Greg Luck, Claus Ibsen
 * @version $Id: CacheTest.java,v 1.2 2006/03/14 08:32:43 gregluck Exp $
 */
public class CacheTest extends AbstractCacheTest {
    private static final Log LOG = LogFactory.getLog(CacheTest.class.getName());

    /**
     * Checks we cannot use a cache after shutdown
     */
    public void testUseCacheAfterManagerShutdown() throws CacheException {
        Cache cache = manager.getCache("sampleCache1");
        manager.shutdown();
        Element element = new Element("key", "value");
        try {
            cache.getSize();
            fail();
        } catch (IllegalStateException e) {
            assertEquals("The sampleCache1 Cache is not alive.", e.getMessage());
        }
        try {
            cache.put(element);
            fail();
        } catch (IllegalStateException e) {
            assertEquals("The sampleCache1 Cache is not alive.", e.getMessage());
        }
        try {
            cache.get("key");
            fail();
        } catch (IllegalStateException e) {
            assertEquals("The sampleCache1 Cache is not alive.", e.getMessage());
        }
        try {
            cache.getHitCount();
            fail();
        } catch (IllegalStateException e) {
            assertEquals("The sampleCache1 Cache is not alive.", e.getMessage());
        }
        try {
            cache.getMemoryStoreHitCount();
            fail();
        } catch (IllegalStateException e) {
            assertEquals("The sampleCache1 Cache is not alive.", e.getMessage());
        }
        try {
            cache.getDiskStoreHitCount();
            fail();
        } catch (IllegalStateException e) {
            assertEquals("The sampleCache1 Cache is not alive.", e.getMessage());
        }
        try {
            cache.getMissCountExpired();
            fail();
        } catch (IllegalStateException e) {
            assertEquals("The sampleCache1 Cache is not alive.", e.getMessage());
        }
        try {
            cache.getMissCountNotFound();
            fail();
        } catch (IllegalStateException e) {
            assertEquals("The sampleCache1 Cache is not alive.", e.getMessage());
        }
    }

    /**
     * Checks we cannot use a cache outside the manager
     */
    public void testUseCacheOutsideManager() throws CacheException {
        //Not put into manager.
        Cache cache = new Cache("testCache", 1, true, false, 5, 2);
        Element element = new Element("key", "value");
        try {
            cache.getSize();
            fail();
        } catch (IllegalStateException e) {
            assertEquals("The testCache Cache is not alive.", e.getMessage());
        }
        try {
            cache.put(element);
            fail();
        } catch (IllegalStateException e) {
            assertEquals("The testCache Cache is not alive.", e.getMessage());
        }
        try {
            cache.get("key");
            fail();
        } catch (IllegalStateException e) {
            assertEquals("The testCache Cache is not alive.", e.getMessage());
        }
        try {
            cache.getHitCount();
            fail();
        } catch (IllegalStateException e) {
            assertEquals("The testCache Cache is not alive.", e.getMessage());
        }
        try {
            cache.getMemoryStoreHitCount();
            fail();
        } catch (IllegalStateException e) {
            assertEquals("The testCache Cache is not alive.", e.getMessage());
        }
        try {
            cache.getDiskStoreHitCount();
            fail();
        } catch (IllegalStateException e) {
            assertEquals("The testCache Cache is not alive.", e.getMessage());
        }
        try {
            cache.getMissCountExpired();
            fail();
        } catch (IllegalStateException e) {
            assertEquals("The testCache Cache is not alive.", e.getMessage());
        }
        try {
            cache.getMissCountNotFound();
            fail();
        } catch (IllegalStateException e) {
            assertEquals("The testCache Cache is not alive.", e.getMessage());
        }
    }


    /**
     * Test using a cache which has been removed and replaced.
     */
    public void testStaleCacheReference() throws CacheException {
        manager.addCache("test");
        Cache cache = manager.getCache("test");
        assertNotNull(cache);
        cache.put(new Element("key1", "value1"));

        assertEquals("value1", cache.get("key1").getValue());
        manager.removeCache("test");
        manager.addCache("test");

        try {
            cache.get("key1");
            fail();
        } catch (IllegalStateException e) {
            assertEquals("The test Cache is not alive.", e.getMessage());
        }
    }

    /**
     * Tests getting the cache name
     *
     * @throws Exception
     */
    public void testCacheName() throws Exception {
        manager.addCache("test");
        Cache cache = manager.getCache("test");
        assertEquals("test", cache.getName());
        assertEquals(Status.STATUS_ALIVE, cache.getStatus());
    }

    /**
     * Tests getting the cache name
     *
     * @throws Exception
     */
    public void testCacheWithNoIdle() throws Exception {
        Cache cache = manager.getCache("sampleCacheNoIdle");
        assertEquals("sampleCacheNoIdle", cache.getName());
        assertEquals(Status.STATUS_ALIVE, cache.getStatus());
        assertEquals(0, cache.getTimeToIdleSeconds());
    }

    /**
     * Test expiry based on time to live
     * <cache name="sampleCacheNoIdle"
     * maxElementsInMemory="1000"
     * eternal="false"
     * timeToLiveSeconds="5"
     * overflowToDisk="false"
     * />
     */
    public void testExpiryBasedOnTimeToLiveWhenNoIdle() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = manager.getCache("sampleCacheNoIdle");
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));

        //Test time to idle. Should not idle out because not specified
        Thread.sleep(2000);
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));

        //Test time to live.
        Thread.sleep(5001);
        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));
    }


    /**
     * Test expiry based on time to live. Even though eternal is false, because there are no
     * expiry or idle times, it is eternal.
     * <cache name="sampleCacheNotEternalButNoIdleOrExpiry"
     * maxElementsInMemory="1000"
     * eternal="false"
     * overflowToDisk="false"
     * />
     */
    public void testExpirySampleCacheNotEternalButNoIdleOrExpiry() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = manager.getCache("sampleCacheNotEternalButNoIdleOrExpiry");
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));

        //Test time to idle. Should not idle out because not specified
        Thread.sleep(2000);
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));

        //Test time to live.
        Thread.sleep(5001);
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));
    }


    /**
     * Test overflow to disk = false
     */
    public void testNoOverflowToDisk() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1, false, true, 5, 2);
        manager.addCache(cache);
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));
        assertNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));
    }


    /**
     * Performance tests for a range of Memory Store - Disk Store combinations.
     * <p/>
     * This demonstrates that a memory only store is approximately an order of magnitude
     * faster than a disk only store.
     * <p/>
     * It also shows that double the performance of a Disk Only store can be obtained
     * with a maximum memory size of only 1. Accordingly a Cache created without a
     * maximum memory size of less than 1 will issue a warning.
     * <p/>
     * Threading changes were made in v1.41 of DiskStore. The before and after numbers are shown.
     */
    public void testProportionMemoryAndDiskPerformance() throws Exception {
        StopWatch stopWatch = new StopWatch();
        long time = 0;

        //Memory only Typical 192ms
        Cache memoryOnlyCache = new Cache("testMemoryOnly", 5000, false, false, 5, 2);
        manager.addCache(memoryOnlyCache);
        time = stopWatch.getElapsedTime();
        for (int i = 0; i < 5000; i++) {
            Integer key = new Integer(i);
            memoryOnlyCache.put(new Element(new Integer(i), "value"));
            memoryOnlyCache.get(key);
        }
        time = stopWatch.getElapsedTime();
        LOG.info("Time for MemoryStore: " + time);
        assertTrue("Time to put and get 5000 entries into MemoryStore", time < 300);

        //Set size so that all elements overflow to disk.
        // 1245 ms v1.38 DiskStore
        // 273 ms v1.42 DiskStore
        Cache diskOnlyCache = new Cache("testDiskOnly", 0, true, false, 5, 2);
        manager.addCache(diskOnlyCache);
        time = stopWatch.getElapsedTime();
        for (int i = 0; i < 5000; i++) {
            Integer key = new Integer(i);
            diskOnlyCache.put(new Element(key, "value"));
            diskOnlyCache.get(key);
        }
        time = stopWatch.getElapsedTime();
        LOG.info("Time for DiskStore: " + time);
        assertTrue("Time to put and get 5000 entries into DiskStore was less than 2 sec", time < 2000);

        // 1 Memory, 999 Disk
        // 591 ms v1.38 DiskStore
        // 56 ms v1.42 DiskStore
        Cache m1d999Cache = new Cache("m1d999Cache", 1, true, false, 5, 2);
        manager.addCache(m1d999Cache);
        time = stopWatch.getElapsedTime();
        for (int i = 0; i < 5000; i++) {
            Integer key = new Integer(i);
            m1d999Cache.put(new Element(key, "value"));
            m1d999Cache.get(key);
        }
        time = stopWatch.getElapsedTime();
        LOG.info("Time for m1d999Cache: " + time);
        assertTrue("Time to put and get 5000 entries into m1d999Cache", time < 2000);

        // 500 Memory, 500 Disk
        // 669 ms v1.38 DiskStore
        // 47 ms v1.42 DiskStore
        Cache m500d500Cache = new Cache("m500d500Cache", 500, true, false, 5, 2);
        manager.addCache(m500d500Cache);
        time = stopWatch.getElapsedTime();
        for (int i = 0; i < 5000; i++) {
            Integer key = new Integer(i);
            m500d500Cache.put(new Element(key, "value"));
            m500d500Cache.get(key);
        }
        time = stopWatch.getElapsedTime();
        LOG.info("Time for m500d500Cache: " + time);
        assertTrue("Time to put and get 5000 entries into m500d500Cache", time < 2000);

    }

    /**
     * todo uncomment
     * Test Caches with persistent stores dispose properly. Tests:
     * <ol>
     * <li>No exceptions are thrown
     * </ol>
     */
    public void testDispose() throws CacheException {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test2", 1, true, true, 0, 0, true, 120);
        manager.addCache(cache);
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));
        int sizeFromGetSize = cache.getSize();
        int sizeFromKeys = cache.getKeys().size();
        assertEquals(sizeFromGetSize, sizeFromKeys);
        assertEquals(2, cache.getSize());

        //cache.dispose(); //package protected method, only available to tests. Called by teardown

    }

    /**
     * Test expiry based on time to live
     */
    public void testExpiryBasedOnTimeToLive() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1, true, false, 5, 2);
        manager.addCache(cache);
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));

        //Test time to live
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));
        Thread.sleep(5010);
        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));
    }


    /**
     * Tests that a cache created from defaults will expire as per
     * the default expiry policy.
     *
     * @throws Exception
     */
    public void testExpiryBasedOnTimeToLiveForDefault() throws Exception {
        String name = "ThisIsACacheWhichIsNotConfiguredAndWillThereforeUseDefaults";
        Cache cache = null;
        CacheManager manager = CacheManager.getInstance();
        cache = manager.getCache(name);
        if (cache == null) {
            LOG.warn("Could not find configuration for " + name
                    + ". Configuring using the defaultCache settings.");
            manager.addCache(name);
            cache = manager.getCache(name);
        }

        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));

        //Test time to live
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));
        Thread.sleep(10010);
        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));


    }


    /**
     * Test expiry based on time to live.
     * <p/>
     * Elements are put quietly back into the cache after being cloned.
     * The elements should expire as if the putQuiet had not happened.
     */
    public void testExpiryBasedOnTimeToLiveAfterPutQuiet() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1, true, false, 5, 2);
        manager.addCache(cache);
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));

        Element element1 = cache.get("key1");
        Element element2 = cache.get("key2");
        assertNotNull(element1);
        assertNotNull(element2);

        //Test time to live
        Thread.sleep(2010);
        //Should not affect age
        cache.putQuiet((Element) element2.clone());
        cache.putQuiet((Element) element2.clone());
        Thread.sleep(3010);
        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));
    }

    /**
     * Test expiry based on time to live
     */
    public void testNoIdleOrExpiryBasedOnTimeToLiveForEternal() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1, true, true, 5, 2);
        manager.addCache(cache);
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));

        //Test time to live
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));

        //Check that we did not idle out
        Thread.sleep(2010);
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));

        //Check that we did not expire out
        Thread.sleep(3010);
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));
    }

    /**
     * Test expiry based on time to idle.
     */
    public void testExpiryBasedOnTimeToIdle() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1, true, false, 5, 2);
        manager.addCache(cache);
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));

        //Test time to idle
        Element element1 = cache.get("key1");
        Element element2 = cache.get("key2");
        assertNotNull(element1);
        assertNotNull(element2);
        Thread.sleep(2010);
        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));
    }


    /**
     * Test expiry based on time to idle.
     */
    public void testExpiryBasedOnTimeToIdleAfterPutQuiet() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1, true, false, 5, 3);
        manager.addCache(cache);
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));

        //Test time to idle
        Element element1 = cache.get("key1");
        Element element2 = cache.get("key2");
        assertNotNull(element1);
        assertNotNull(element2);

        //Now, getQuiet and check still times out 2 seconds after last get
        Thread.sleep(1010);
        element1 = cache.getQuiet("key1");
        element2 = cache.getQuiet("key2");
        Thread.sleep(2010);
        assertNull(cache.getQuiet("key1"));
        assertNull(cache.getQuiet("key2"));

        //Now put back in with putQuiet. Should be immediately expired
        cache.putQuiet((Element) element1.clone());
        cache.putQuiet((Element) element2.clone());
        assertNull(cache.get("key1"));
        element2 = cache.get("key2");
        assertNull(element2);
    }

    /**
     * Test statistics
     */
    public void testStatistics() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1, true, false, 5, 2);
        manager.addCache(cache);
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));

        //key1 should be in the Disk Store
        cache.get("key1");
        assertEquals(1, cache.getHitCount());
        assertEquals(1, cache.getDiskStoreHitCount());
        assertEquals(0, cache.getMemoryStoreHitCount());
        assertEquals(0, cache.getMissCountExpired());
        assertEquals(0, cache.getMissCountNotFound());

        //key 1 should now be in the LruMemoryStore
        cache.get("key1");
        assertEquals(2, cache.getHitCount());
        assertEquals(1, cache.getDiskStoreHitCount());
        assertEquals(1, cache.getMemoryStoreHitCount());
        assertEquals(0, cache.getMissCountExpired());
        assertEquals(0, cache.getMissCountNotFound());

        //Let the idle expire
        Thread.sleep(5010);

        //key 1 should now be expired
        cache.get("key1");
        assertEquals(2, cache.getHitCount());
        assertEquals(1, cache.getDiskStoreHitCount());
        assertEquals(1, cache.getMemoryStoreHitCount());
        assertEquals(1, cache.getMissCountExpired());
        assertEquals(1, cache.getMissCountNotFound());
    }

    /**
     * Test element statistics, including get and getQuiet
     * eternal="false"
     * timeToIdleSeconds="5"
     * timeToLiveSeconds="10"
     * overflowToDisk="true"
     */
    public void testElementStatistics() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1, true, false, 5, 2);
        manager.addCache(cache);
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));

        Element element1 = cache.get("key1");
        assertEquals("Should be one", 1, element1.getHitCount());
        element1 = cache.getQuiet("key1");
        assertEquals("Should be one", 1, element1.getHitCount());
        element1 = cache.get("key1");
        assertEquals("Should be two", 2, element1.getHitCount());
    }

    /**
     * Checks that getQuiet works how we expect it to
     *
     * @throws Exception
     */
    public void testGetQuietAndPutQuiet() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1, true, false, 5, 2);
        manager.addCache(cache);
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));

        Element element1 = cache.get("key1");
        long lastAccessedElement1 = element1.getLastAccessTime();
        long hitCountElement1 = element1.getHitCount();
        assertEquals("Should be two", 1, element1.getHitCount());

        element1 = cache.getQuiet("key1");
        element1 = cache.getQuiet("key1");
        Element clonedElement1 = (Element) element1.clone();
        cache.putQuiet(clonedElement1);
        element1 = cache.getQuiet("key1");
        assertEquals("last access time should be unchanged",
                lastAccessedElement1, element1.getLastAccessTime());
        assertEquals("hit count should be unchanged",
                hitCountElement1, element1.getHitCount());
        element1 = cache.get("key1");
        assertEquals("Should be two", 2, element1.getHitCount());
    }

    /**
     * Test size with put and remove.
     * <p/>
     * It checks that size makes sense, and also that getKeys.size() matches getSize()
     */
    public void testSizeWithPutAndRemove() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test2", 1, true, true, 0, 0);
        manager.addCache(cache);
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));
        int sizeFromGetSize = cache.getSize();
        int sizeFromKeys = cache.getKeys().size();
        assertEquals(sizeFromGetSize, sizeFromKeys);
        assertEquals(2, cache.getSize());
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key1", "value1"));

        //key1 should be in the Disk Store
        assertEquals(cache.getSize(), cache.getKeys().size());
        assertEquals(2, cache.getSize());
        //there were two of these, so size will now be one
        cache.remove("key1");
        assertEquals(cache.getSize(), cache.getKeys().size());
        assertEquals(1, cache.getSize());
        cache.remove("key2");
        assertEquals(cache.getSize(), cache.getKeys().size());
        assertEquals(0, cache.getSize());
    }

    /**
     * Test getKeys after expiry
     * <p/>
     * Makes sure that if an element is expired, its key should also be expired
     */
    public void testGetKeysAfterExpiry() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test2", 1, true, false, 1, 0);
        manager.addCache(cache);
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));
        //getSize uses getKeys().size(), so these should be the same
        assertEquals(cache.getSize(), cache.getKeys().size());
        //getKeys does not do an expiry check, so the expired elements are counted
        assertEquals(2, cache.getSize());
        Thread.sleep(1010);
        assertEquals(2, cache.getKeys().size());
        //getKeysWithExpiryCheck does check and gives the correct answer of 0
        assertEquals(0, cache.getKeysWithExpiryCheck().size());
    }

    /**
     * Test size after multiple calls, with put and remove
     */
    public void testSizeMultipleCallsWithPutAndRemove() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test3", 1, true, true, 0, 0);
        manager.addCache(cache);
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));

        //key1 should be in the Disk Store
        assertEquals(2, cache.getSize());
        assertEquals(2, cache.getSize());
        assertEquals(2, cache.getSize());
        assertEquals(2, cache.getSize());
        assertEquals(2, cache.getSize());
        cache.remove("key1");
        assertEquals(1, cache.getSize());
        assertEquals(1, cache.getSize());
        assertEquals(1, cache.getSize());
        assertEquals(1, cache.getSize());
        assertEquals(1, cache.getSize());
        cache.remove("key2");
        assertEquals(0, cache.getSize());
        assertEquals(0, cache.getSize());
        assertEquals(0, cache.getSize());
        assertEquals(0, cache.getSize());
        assertEquals(0, cache.getSize());
    }

    /**
     * Checks the expense of checking for duplicates
     * Typical Results Duplicate Check: 8ms versus 3ms for No Duplicate Check
     * <p/>
     * 66ms for 1000, 6ms for no duplicate/expiry
     * 187565 for 100000, where 500 is the in-memory size. 964ms without checking expiry. 134ms for getKeysNoDuplicateCheckTime
     * 18795 for 100000, where 50000 is in-memory size. 873ms without checking expiry. 158ms for getKeysNoDuplicateCheckTime
     */
    public void testGetKeysPerformance() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test4", 1000, true, true, 0, 0);
        manager.addCache(cache);

        for (int i = 0; i < 2000; i++) {
            cache.put(new Element("key" + i, "value"));
        }
        //let the notifiers cool down
        Thread.sleep(1000);
        StopWatch stopWatch = new StopWatch();
        List keys = cache.getKeys();
        assertTrue("Should be 2000 keys. ", keys.size() == 2000);
        long getKeysTime = stopWatch.getElapsedTime();
        cache.getKeysNoDuplicateCheck();
        long getKeysNoDuplicateCheckTime = stopWatch.getElapsedTime();
        LOG.info("Time to get 1000 keys: With Duplicate Check: " + getKeysTime
                + " Without Duplicate Check: " + getKeysNoDuplicateCheckTime);
        assertTrue("Getting keys took more than 150ms", getKeysTime < 100);
    }

    /**
     * Checks the expense of checking in-memory size
     * 3467890 bytes in 1601ms for JDK1.4.2
     */
    public void testCalculateInMemorySizePerformanceAndReasonableness() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test4", 1000, true, true, 0, 0);
        manager.addCache(cache);

        //Set up object graphs
        for (int i = 0; i < 1000; i++) {
            HashMap map = new HashMap(100);
            for (int j = 0; j < 100; j++) {
                map.put("key" + j, new String[]{"adfdafs", "asdfdsafa", "sdfasdf"});
            }
            cache.put(new Element("key" + i, map));
        }

        StopWatch stopWatch = new StopWatch();
        long size = cache.calculateInMemorySize();
        assertTrue("Size is " + size + ". Check it for reasonableness.", size > 100000 && size < 5000000);
        long elapsed = stopWatch.getElapsedTime();
        LOG.info("In-memory size in bytes: " + size
                + " time to calculate in ms: " + elapsed);
        assertTrue("Calculate memory size takes less than 3.5 seconds", elapsed < 3500);
    }


    /**
     * Expire elements and verify size is correct.
     */
    public void testGetSizeAfterExpiry() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1, true, false, 5, 2);
        manager.addCache(cache);
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));

        //Let the idle expire
        Thread.sleep(5010);
        assertEquals(null, cache.get("key1"));
        assertEquals(null, cache.get("key2"));

        assertEquals(0, cache.getSize());
    }

    /**
     * Test create and access times
     */
    public void testAccessTimes() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 5, true, false, 5, 2);
        assertEquals(Status.STATUS_UNINITIALISED, cache.getStatus());
        manager.addCache(cache);
        Element newElement = new Element("key1", "value1");
        long creationTime = newElement.getCreationTime();
        assertTrue(newElement.getCreationTime() > (System.currentTimeMillis() - 500));
        assertTrue(newElement.getHitCount() == 0);
        assertTrue(newElement.getLastAccessTime() == 0);

        cache.put(newElement);

        Element element = cache.get("key1");
        assertNotNull(element);
        assertEquals(creationTime, element.getCreationTime());
        assertTrue(element.getLastAccessTime() != 0);
        assertTrue(element.getHitCount() == 1);

        //Check that access statistics were reset but not creation time
        cache.put(element);
        element = cache.get("key1");
        assertEquals(creationTime, element.getCreationTime());
        assertTrue(element.getLastAccessTime() != 0);
        assertTrue(element.getHitCount() == 1);
    }

    /**
     * Tests initialisation failures
     */
    public void testInitialiseFailures() {
        try {
            Cache cache = new Cache("testInitialiseFailures2", 1, false, false, 5, 1);
            cache.initialise();

            cache.initialise();
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalStateException e) {
            //noop
        }
    }

    /**
     * Tests putting nulls throws correct exception
     *
     * @throws Exception
     */
    public void testPutFailures() throws Exception {
        Cache cache = new Cache("testPutFailures", 1, false, false, 5, 1);
        manager.addCache(cache);

        try {
            cache.put(null);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            //noop
        }

        try {
            cache.putQuiet(null);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            //noop
        }
    }

    /**
     * Tests memory store size
     *
     * @throws Exception
     */
    public void testGetMemoryStoreSize() throws Exception {
        Cache cache = new Cache("testGetMemoryStoreSize", 10, false, false, 100, 200);
        manager.addCache(cache);

        assertEquals(0, cache.getMemoryStoreSize());

        cache.put(new Element("key1", "value1"));
        assertEquals(1, cache.getMemoryStoreSize());

        cache.put(new Element("key2", "value2"));
        assertEquals(2, cache.getMemoryStoreSize());

        cache.put(new Element("key3", "value3"));
        cache.put(new Element("key4", "value4"));
        assertEquals(4, cache.getMemoryStoreSize());

        cache.remove("key4");
        cache.remove("key3");
        assertEquals(2, cache.getMemoryStoreSize());

        cache.removeAll();
        assertEquals(0, cache.getMemoryStoreSize());
    }

    /**
     * Tests flushing the cache
     *
     * @throws Exception
     */
    public void testFlushWhenOverflowToDisk() throws Exception {
        Cache cache = new Cache("testGetMemoryStoreSize", 50, true, false, 100, 200);
        manager.addCache(cache);

        assertEquals(0, cache.getMemoryStoreSize());

        for (int i = 0; i < 100; i++) {
            cache.put(new Element("" + i, new Date()));
        }
        assertEquals(50, cache.getMemoryStoreSize());
        assertEquals(50, cache.getDiskStoreSize());

        cache.flush();
        assertEquals(0, cache.getMemoryStoreSize());
        assertEquals(100, cache.getDiskStoreSize());

    }

    /**
     * Tests using elements with null values. They should work as normal.
     *
     * @throws Exception
     */
    public void testElementWithNullValue() throws Exception {
        Cache cache = new Cache("testElementWithNullValue", 10, false, false, 100, 200);
        manager.addCache(cache);

        Element element = new Element("key1", null);
        cache.put(element);
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.getQuiet("key1"));
        assertSame(element, cache.get("key1"));
        assertSame(element, cache.getQuiet("key1"));
        assertNull(cache.get("key1").getValue());
        assertNull(cache.getQuiet("key1").getValue());

        assertEquals(false, cache.isExpired(element));
    }

    /**
     * Tests disk store and memory store size
     *
     * @throws Exception
     */
    public void testGetDiskStoreSize() throws Exception {
        Cache cache = new Cache("testGetDiskStoreSize", 1, true, false, 100, 200);
        manager.addCache(cache);
        assertEquals(0, cache.getDiskStoreSize());

        cache.put(new Element("key1", "value1"));
        assertEquals(0, cache.getDiskStoreSize());
        assertEquals(1, cache.getSize());

        cache.put(new Element("key2", "value2"));
        assertEquals(2, cache.getSize());
        assertEquals(1, cache.getDiskStoreSize());
        assertEquals(1, cache.getMemoryStoreSize());

        cache.put(new Element("key3", "value3"));
        cache.put(new Element("key4", "value4"));
        assertEquals(4, cache.getSize());
        assertEquals(3, cache.getDiskStoreSize());
        assertEquals(1, cache.getMemoryStoreSize());

        // remove last element inserted (is in memory store)
        assertNotNull(cache.getMemoryStore().get("key4"));
        cache.remove("key4");
        assertEquals(3, cache.getSize());
        assertEquals(3, cache.getDiskStoreSize());
        assertEquals(0, cache.getMemoryStoreSize());

        // remove key1 element
        assertNotNull(cache.getDiskStore().get("key1"));
        cache.remove("key1");
        assertEquals(2, cache.getSize());
        assertEquals(2, cache.getDiskStoreSize());
        assertEquals(0, cache.getMemoryStoreSize());

        // add another
        cache.put(new Element("key5", "value5"));
        assertEquals(3, cache.getSize());
        assertEquals(2, cache.getDiskStoreSize());
        assertEquals(1, cache.getMemoryStoreSize());

        // remove all
        cache.removeAll();
        assertEquals(0, cache.getSize());
        assertEquals(0, cache.getDiskStoreSize());
        assertEquals(0, cache.getMemoryStoreSize());
    }

    /**
     * Tests that attempting to clone a cache fails with the right exception.
     *
     * @throws Exception
     */
    public void testCloneFailures() throws Exception {
        Cache cache = new Cache("testGetMemoryStore", 10, false, false, 100, 200);
        manager.addCache(cache);
        try {
            cache.clone();
            fail("Should have thrown CloneNotSupportedException");
        } catch (CloneNotSupportedException e) {
            //noop
        }
    }


    /**
     * Tests that the toString() method works.
     */
    public void testToString() {
        Cache cache = new Cache("testGetMemoryStore", 10, false, false, 100, 200);
        assertTrue(cache.toString().indexOf("testGetMemoryStore") > -1);
        assertEquals(367, cache.toString().length());
    }


    /**
     * When does equals mean the same thing as == ?
     * @throws CacheException
     * @throws InterruptedException
     */
    public void testEquals() throws CacheException, InterruptedException {
        Cache cache = new Cache("cache", 1, true, false, 100, 200, false, 1);
        manager.addCache(cache);

        Element element1 = new Element("1", new Date());
        Element element2 = new Element("2", new Date());
        cache.put(element1);
        cache.put(element2);

        //Test equals and == from an Element retrieved from the MemoryStore
        Element elementFromStore = cache.get("2");
        assertEquals(element2, elementFromStore);
        assertTrue(element2 == elementFromStore);

        //Give the spool a chance to make sure it really got serialized to Disk
        Thread.sleep(300);

        //Test equals and == from an Element retrieved from the MemoryStore
        Element elementFromDiskStore = cache.get("1");
        assertEquals(element1, elementFromDiskStore);
        assertTrue(element1 != elementFromDiskStore);
    }

    /**
     * Tests the uniqueness of the GUID
     */
    public void testGuid() {
        Cache cache1 = new Cache("testGetMemoryStore", 10, false, false, 100, 200);
        Cache cache2 = new Cache("testGetMemoryStore", 10, false, false, 100, 200);
        String guid1 = cache1.getGuid();
        String guid2 = cache2.getGuid();
        assertEquals(cache1.getName(), cache2.getName());
        assertTrue(!guid1.equals(guid2));


    }

}

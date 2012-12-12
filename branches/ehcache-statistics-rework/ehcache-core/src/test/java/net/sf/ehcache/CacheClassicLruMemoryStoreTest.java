package net.sf.ehcache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import net.sf.ehcache.store.LegacyStoreWrapper;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Does the cache tests using the classic LRUMemoryStore implementation.
 *
 * @author Greg Luck
 */
public class CacheClassicLruMemoryStoreTest extends CacheTest {

    @BeforeClass
    public static void enableHeapDump() {
        setHeapDumpOnOutOfMemoryError(true);
    }

    @AfterClass
    public static void disableHeapDump() {
        setHeapDumpOnOutOfMemoryError(false);
    }

    private static final Logger LOG = LoggerFactory.getLogger(CacheClassicLruMemoryStoreTest.class.getName());

    @BeforeClass
    public static void beforeClass() throws Exception {
        System.setProperty(Cache.NET_SF_EHCACHE_USE_CLASSIC_LRU, "true");
    }


    @AfterClass
    public static void afterClass() throws Exception {
        System.setProperty(Cache.NET_SF_EHCACHE_USE_CLASSIC_LRU, "false");
    }

    /**
     * Tests flushing the cache, with the default, which is to clear
     * <p/>
     * Has different numbers because LRU works slightly differently
     *
     * @throws Exception
     */
    @Override
    @Test
    public void testFlushWhenOverflowToDisk() throws Exception {
        if (manager.getCache("testFlushWhenOverflowToDisk") == null) {
            manager.addCache(new Cache("testFlushWhenOverflowToDisk", 50, true, false, 100, 200, true, 120));
        }
        Cache cache = manager.getCache("testFlushWhenOverflowToDisk");
        cache.removeAll();

        assertEquals(0, cache.getMemoryStoreSize());
        assertEquals(0, cache.getDiskStoreSize());


        for (int i = 0; i < 100; i++) {
            cache.put(new Element("" + i, new Date()));
            //hit
            cache.get("" + i);
        }
        assertEquals(50, cache.getMemoryStoreSize());
        assertEquals(50, cache.getDiskStoreSize());


        cache.put(new Element("key", new Object()));
        cache.put(new Element("key2", new Object()));
        Object key = new Object();
        cache.put(new Element(key, "value"));

        //get it and make sure it is mru
        Thread.sleep(15);
        cache.get(key);

        assertEquals(103, cache.getSize());
        assertEquals(50, cache.getMemoryStoreSize());
        assertEquals(53, cache.getDiskStoreSize());


        //these "null" Elements are ignored and do not get put in
        cache.put(new Element(null, null));
        cache.put(new Element(null, null));

        assertEquals(103, cache.getSize());
        assertEquals(50, cache.getMemoryStoreSize());
        assertEquals(53, cache.getDiskStoreSize());

        //this one does
        cache.put(new Element("nullValue", null));

        LOG.info("Size: " + cache.getDiskStoreSize());

        assertEquals(50, cache.getMemoryStoreSize());
        assertEquals(54, cache.getDiskStoreSize());

        cache.flush();
        assertEquals(0, cache.getMemoryStoreSize());
        //Non Serializable Elements get discarded
        assertEquals(101, cache.getDiskStoreSize());

        cache.removeAll();

    }

    @Override
    @Test
    public void testFlushWithoutClear() throws InterruptedException {

        CacheManager cacheManager = CacheManager.create(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache.xml");
        Cache cache = cacheManager.getCache("SimplePageCachingFilter");
        cache.removeAll();
        for (int i = 0; i < 100; i++) {
            cache.put(new Element("" + i, new Date()));
            //hit
            cache.get("" + i);
        }
        assertEquals(10, cache.getMemoryStoreSize());
        assertEquals(90, cache.getDiskStoreSize());

        cache.flush();
        Thread.sleep(1000);

        assertEquals(10, cache.getMemoryStoreSize());
        assertEquals(100, cache.getDiskStoreSize());
        cacheManager.shutdown();

    }

    @Override
    @Test
    public void testFlushWithClear() throws InterruptedException {

        CacheManager cacheManager = CacheManager.create(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache.xml");
        Cache cache = cacheManager.getCache("SimplePageFragmentCachingFilter");
        cache.removeAll();
        for (int i = 0; i < 100; i++) {
            cache.put(new Element("" + i, new Date()));
            //hit
            cache.get("" + i);
        }
        assertEquals(10, cache.getMemoryStoreSize());
        assertEquals(90, cache.getDiskStoreSize());

        cache.flush();
        Thread.sleep(1000);

        assertEquals(0, cache.getMemoryStoreSize());
        assertEquals(100, cache.getDiskStoreSize());
        cacheManager.shutdown();

    }

    /**
     * Tests disk store and memory store size
     * <p/>
     * This is overridden because the classic LRU store uses different classes
     */
    @Test
    @Override
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

        assertTrue(((LegacyStoreWrapper) cache.getStore()).getMemoryStore().containsKey("key4"));
        cache.remove("key4");
        assertEquals(3, cache.getSize());
        assertEquals(3, cache.getDiskStoreSize());
        assertEquals(0, cache.getMemoryStoreSize());

        // remove key1 element
        assertFalse(((LegacyStoreWrapper) cache.getStore()).getMemoryStore().containsKey("key1"));
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

        //Check behaviour of NonSerializable objects
        cache.put(new Element(new Object(), new Object()));
        cache.put(new Element(new Object(), new Object()));
        cache.put(new Element(new Object(), new Object()));
        assertEquals(1, cache.getSize());
        assertEquals(0, cache.getDiskStoreSize());
        assertEquals(1, cache.getMemoryStoreSize());
    }

    /**
     * Tests cache, memory store and disk store sizes from config
     */
    @Override
    @Test
    public void testSizes() throws Exception {
        Ehcache cache = getSampleCache1();

        assertEquals(0, cache.getStatistics().getMemoryStoreSize());
        assertEquals(0, cache.getStatistics().getDiskStoreSize());

        for (int i = 0; i < 10010; i++) {
            cache.put(new Element("key" + i, "value1"));
        }

        Thread.sleep(1000);

        assertEquals(10010, cache.getSize());
        assertEquals(10000, cache.getStatistics().getMemoryStoreSize());
        assertEquals(10, cache.getStatistics().getDiskStoreSize());

        //NonSerializable
        Thread.sleep(15);
        cache.put(new Element(new Object(), Object.class));

        Thread.sleep(1000);

        assertEquals(10011, cache.getSize());
        assertEquals(11, cache.getStatistics().getDiskStoreSize());
        assertEquals(10000, cache.getStatistics().getMemoryStoreSize());
        assertEquals(10000, cache.getStatistics().getMemoryStoreSize());
        assertEquals(10000, cache.getStatistics().getMemoryStoreSize());
        assertEquals(10000, cache.getStatistics().getMemoryStoreSize());


        cache.remove("key4");
        cache.remove("key3");

        assertEquals(10009, cache.getSize());
        //cannot make any guarantees as no elements have been getted, and all are equally likely to be evicted.
        //assertEquals(10000, cache.getMemoryStoreSize());
        //assertEquals(9, cache.getDiskStoreSize());


        Thread.sleep(1000);

        cache.removeAll();
        assertEquals(0, cache.getSize());
        assertEquals(0, cache.getStatistics().getMemoryStoreSize());
        assertEquals(0, cache.getStatistics().getDiskStoreSize());

    }

    /**
     * Tests that the toString() method works.
     */
    @Override
    @Test
    public void testToString() {
        Ehcache cache = new Cache("testGetMemoryStore", 10, false, false, 100, 200);
        assertTrue(cache.toString().indexOf("testGetMemoryStore") > -1);
    }

    /**
     * Test expiry based on time to idle.
     */
    @Override
    @Test
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
        Thread.sleep(1050);
        element1 = cache.getQuiet("key1");
        assertNotNull(element1);
        element2 = cache.getQuiet("key2");
        assertNotNull(element2);
        Thread.sleep(2949);
        assertNull(cache.getQuiet("key1"));
        assertNull(cache.getQuiet("key2"));

        //Now put back in with putQuiet. Should be immediately expired
        cache.putQuiet((Element) element1.clone());
        cache.putQuiet((Element) element2.clone());
        assertNull(cache.get("key1"));
        element2 = cache.get("key2");
        assertNull(element2);
    }

    @Override
    @Test
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

        //try null values
        cache.removeAll();
        Object object1 = new Object();
        Object object2 = new Object();
        cache.put(new Element(object1, null));
        cache.put(new Element(object2, null));
        // wait until the disk store flushed to disk
        Thread.sleep(500);
        //Cannot overflow therefore just one
        try {
            assertEquals(1, cache.getSize());
        } catch (AssertionError e) {
            //eviction failure
            System.err.println(e + " - likely eviction failure: checking memory store");
            assertEquals(2, cache.getMemoryStoreSize());
        }
        Element nullValueElement = cache.get(object2);
        assertNull(nullValueElement.getValue());
        assertNull(nullValueElement.getObjectValue());
    }

}

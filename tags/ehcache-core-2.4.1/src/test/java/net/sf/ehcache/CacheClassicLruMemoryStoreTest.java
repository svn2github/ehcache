package net.sf.ehcache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
}

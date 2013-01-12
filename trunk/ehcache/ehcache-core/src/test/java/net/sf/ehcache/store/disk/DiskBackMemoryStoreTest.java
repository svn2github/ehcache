package net.sf.ehcache.store.disk;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.Assert;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.pool.impl.UnboundedPool;
import net.sf.ehcache.store.NotifyingMemoryStore;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.util.RetryAssert;

import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Alex Snaps
 * @author Ludovic Orban
 */
public class DiskBackMemoryStoreTest {

    private static final String KEY = "KEY";

    private Store xaStore;

    private CacheManager cacheManager;
    private Cache cache;
    private Cache xaCache;

    @Before
    public void init() {
        cacheManager = new CacheManager(new Configuration().diskStore(new DiskStoreConfiguration().path("java.io.tmpdir/DiskBackMemoryStoreTest")));
        cache = new Cache(new CacheConfiguration("SomeCache", 1000).overflowToDisk(true).diskPersistent(true));
        cacheManager.addCache(cache);
        xaCache = new Cache(new CacheConfiguration("SomeXaCache", 1000).transactionalMode("xa_strict"));
        xaStore = NotifyingMemoryStore.createNotifyingStore(xaCache, new UnboundedPool());
    }

    @After
    public void tearDown() {
        cacheManager.shutdown();
    }

    @Test
    public void testDiskStoreSize() throws Exception {
        CacheManager cm = new CacheManager(
            new Configuration()
                .cache(new CacheConfiguration("aCache", 10000)
                    .overflowToDisk(true)
                    .eternal(false)
                    .timeToLiveSeconds(1000)
                    .timeToLiveSeconds(360)
                )
                .name("testDiskStoreSize")
                .diskStore(new DiskStoreConfiguration().path("java.io.tmpdir/testDiskStoreSize"))
        );
        final Cache cache = cm.getCache("aCache");


        cache.put(new Element(-1, -1));
        assertEquals(-1, cache.get(-1).getValue());
        cache.remove(-1);
        assertEquals(null, cache.get(-1));

        cache.put(new Element(-2, -2));
        assertEquals(-2, cache.get(-2).getValue());
        cache.remove(-2);
        assertEquals(null, cache.get(-2));

        assertEquals(0, cache.getDiskStoreSize());

        for (int i = 0; i < 10010; i++) {
            cache.put(new Element(i, i));
        }

        Thread.sleep(3000);

        RetryAssert.assertBy(1, SECONDS, new Callable<Integer>() {
                public Integer call() throws Exception {
                    return cache.getDiskStoreSize();
                }
            }, Is.is(10010));

        cm.shutdown();
    }

    @Test
    public void testSupportsCopyOnRead() {
        Element element = new Element(KEY, "Some String", 1);
        xaStore.put(element);
        Element copy = xaStore.get(KEY);
        Assert.assertNotNull(copy);
        assertNotSame(copy, xaStore.get(KEY));
        Assert.assertEquals("Some String", copy.getValue());
        Assert.assertEquals(copy.getValue(), xaStore.get(KEY).getValue());
        assertNotSame(copy.getValue(), xaStore.get(KEY).getValue());
    }

    @Test
    public void testSupportsCopyOnWrite() {

        AtomicLong atomicLong = new AtomicLong(0);

        Element element = new Element(KEY, atomicLong, 1);
        atomicLong.getAndIncrement();
        xaStore.put(element);

        atomicLong.getAndIncrement();
        element.setVersion(2);

        Assert.assertEquals(1, ((AtomicLong)xaStore.get(KEY).getValue()).get());
        Assert.assertEquals(1, xaStore.get(KEY).getVersion());

        xaStore.put(new Element(KEY, atomicLong, 1));
        Assert.assertEquals(2, ((AtomicLong)xaStore.get(KEY).getValue()).get());
        atomicLong.getAndIncrement();

        Assert.assertEquals(2, ((AtomicLong)xaStore.get(KEY).getValue()).get());
        Assert.assertEquals(1, xaStore.get(KEY).getVersion());
    }

    @Test
    public void testThrowsExceptionOnNonSerializableValue() {
        try {
            xaStore.put(new Element(KEY, new Object()));
            fail("Should have thrown an Exception");
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue("Expected " + CacheException.class.getName() + ", but was " + e.getClass().getName(), e instanceof CacheException);
        }
        assertNull(xaStore.get(KEY));
    }

}

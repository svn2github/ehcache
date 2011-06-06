package net.sf.ehcache.store.disk;

import junit.framework.Assert;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.pool.impl.UnboundedPool;
import net.sf.ehcache.store.DiskBackedMemoryStore;
import net.sf.ehcache.store.MemoryOnlyStore;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.store.disk.DiskStore;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Alex Snaps
 * @author Ludovic Orban
 */
public class DiskPersistentStoreTest {

    private static final String KEY = "KEY";

    private Store store;
    private Store xaStore;

    private Cache cache;
    private Cache xaCache;

    @Before
    public void init() {
        cache = new Cache(new CacheConfiguration("SomeCache", 1000).overflowToDisk(true).diskPersistent(true));
        store = DiskBackedMemoryStore.create(cache, System.getProperty("java.io.tmpdir"), new UnboundedPool(), new UnboundedPool());
        xaCache = new Cache(new CacheConfiguration("SomeXaCache", 1000).transactionalMode("xa_strict"));
        xaStore = MemoryOnlyStore.create(xaCache, new UnboundedPool());
    }

    @Test
    public void testPersistenceWithPinnedElements() {
        final Element[] lastEvicted = new Element[1];

        cache.getCacheEventNotificationService().registerListener(new CacheEventListener() {
            @Override
            public Object clone() throws CloneNotSupportedException {
                return super.clone();
            }
            public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
            }
            public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
            }
            public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
            }
            public void notifyElementExpired(Ehcache cache, Element element) {
            }
            public void notifyElementEvicted(Ehcache cache, Element element) {
                lastEvicted[0] = element;
            }
            public void notifyRemoveAll(Ehcache cache) {
            }
            public void dispose() {
            }
        });

        store.put(new Element(1, "one"));
        Element element2 = new Element(2, new Object());
        element2.setPinned(true);
        store.put(element2);
        store.dispose();

        assertNull("element should not have been evicted: " + lastEvicted[0], lastEvicted[0]);

        store = DiskStore.create(new Cache(new CacheConfiguration("SomeCache", 1000).overflowToDisk(true).diskPersistent(true)), System.getProperty("java.io.tmpdir"), new UnboundedPool(), new UnboundedPool());
        assertEquals("one", store.get(1).getObjectValue());
        assertNull(store.get(2));
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

        Assert.assertEquals(1, ((AtomicLong) xaStore.get(KEY).getValue()).get());
        Assert.assertEquals(1, xaStore.get(KEY).getVersion());

        xaStore.put(new Element(KEY, atomicLong, 1));
        Assert.assertEquals(2, ((AtomicLong) xaStore.get(KEY).getValue()).get());
        atomicLong.getAndIncrement();

        Assert.assertEquals(2, ((AtomicLong) xaStore.get(KEY).getValue()).get());
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

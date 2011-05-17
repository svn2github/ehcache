package net.sf.ehcache.store.compound.impl;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.pool.impl.DiskPersistentPoolableStore;
import net.sf.ehcache.pool.impl.UnboundedPool;
import net.sf.ehcache.store.compound.CompoundStore;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Alex Snaps
 */
public class DiskPersistentStoreTest extends CompoundStoreTest {

    @Before
    public void init() {
        store = DiskPersistentPoolableStore.create(new Cache(new CacheConfiguration("SomeCache", 1000)), System.getProperty("java.io.tmpdir"), new UnboundedPool(), new UnboundedPool());
        xaStore = DiskPersistentPoolableStore.create(new Cache(new CacheConfiguration("SomeXaCache", 1000).transactionalMode("xa_strict")), System.getProperty("java.io.tmpdir"), new UnboundedPool(), new UnboundedPool());
    }

    @Test
    public void testPersistenceWithPinnedElements() {
        final Element[] lastEvicted = new Element[1];
        store.addInternalEventListener(new CompoundStore.InternalEventListener() {
            public void onFault(Object key, Object from, Object to) {
            }

            public void onEvict(Object key, Element evicted) {
                lastEvicted[0] = evicted;
            }

            public void onUpdate(Object removed, Element newElement) {
            }

            public void onRemove(Object removed, Element removedElement) {
            }
        });
        store.put(new Element(1, "one"));
        Element element2 = new Element(2, new Object());
        element2.setPinned(true);
        store.put(element2);
        store.dispose();

        assertNull("element should not have been evicted: " + lastEvicted[0], lastEvicted[0]);

        store = DiskPersistentPoolableStore.create(new Cache(new CacheConfiguration("SomeCache", 1000)), System.getProperty("java.io.tmpdir"), new UnboundedPool(), new UnboundedPool());
        assertEquals("one", store.get(1).getObjectValue());
        assertNull(store.get(2));
    }
}

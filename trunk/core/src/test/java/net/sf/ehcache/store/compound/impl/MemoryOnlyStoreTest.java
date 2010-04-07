package net.sf.ehcache.store.compound.impl;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Alex Snaps
 */
public class MemoryOnlyStoreTest {

    private static final String KEY = "KEY";

    private MemoryOnlyStore xaMemoryStore;

    @Before
    public void init() {
        xaMemoryStore = MemoryOnlyStore.create(new Cache(new CacheConfiguration("SomeCache", 1000).transactionalMode("XA")), null);
    }

    @Test
    public void testSupportsCopyOnRead() {
        Element element = new Element(KEY, "Some String", 1);
        xaMemoryStore.put(element);
        Element copy = xaMemoryStore.get(KEY);
        assertNotNull(copy);
        assertNotSame(copy, xaMemoryStore.get(KEY));
        assertEquals("Some String", copy.getValue());
        assertEquals(copy.getValue(), xaMemoryStore.get(KEY).getValue());
        assertNotSame(copy.getValue(), xaMemoryStore.get(KEY).getValue());
    }
    
    @Test
    public void testSupportsCopyOnWrite() {

        AtomicLong atomicLong = new AtomicLong(0);

        Element element = new Element(KEY, atomicLong, 1);
        atomicLong.getAndIncrement();
        xaMemoryStore.put(element);

        atomicLong.getAndIncrement();
        element.setVersion(2);

        assertEquals(1, ((AtomicLong)xaMemoryStore.get(KEY).getValue()).get());
        assertEquals(1, xaMemoryStore.get(KEY).getVersion());

        xaMemoryStore.put(new Element(KEY, atomicLong, 1));
        assertEquals(2, ((AtomicLong)xaMemoryStore.get(KEY).getValue()).get());
        atomicLong.getAndIncrement();
        
        assertEquals(2, ((AtomicLong)xaMemoryStore.get(KEY).getValue()).get());
        assertEquals(1, xaMemoryStore.get(KEY).getVersion());
    }

    @Test
    public void testThrowsExceptionOnNonSerializableValue() {
        try {
            xaMemoryStore.put(new Element(KEY, new Object()));
            fail("Should have thrown an Exception");
        } catch (Exception e) {
            assertTrue("Expected org.hibernate.cache.CacheException, but was " + e.getClass().getName(), e instanceof CacheException);
        }
        assertNull(xaMemoryStore.get(KEY));
    }
}

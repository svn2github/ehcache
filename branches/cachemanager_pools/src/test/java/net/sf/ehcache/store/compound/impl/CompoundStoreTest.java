package net.sf.ehcache.store.compound.impl;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.store.Store;
import org.junit.Test;

import java.io.Serializable;
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
public abstract class CompoundStoreTest {

    private static final String KEY = "KEY";

    protected Store store;
    protected Store xaStore;

    @Test
    public void testSupportsCopyOnRead() {
        Element element = new Element(KEY, "Some String", 1);
        xaStore.put(element);
        Element copy = xaStore.get(KEY);
        assertNotNull(copy);
        assertNotSame(copy, xaStore.get(KEY));
        assertEquals("Some String", copy.getValue());
        assertEquals(copy.getValue(), xaStore.get(KEY).getValue());
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

        assertEquals(1, ((AtomicLong) xaStore.get(KEY).getValue()).get());
        assertEquals(1, xaStore.get(KEY).getVersion());

        xaStore.put(new Element(KEY, atomicLong, 1));
        assertEquals(2, ((AtomicLong) xaStore.get(KEY).getValue()).get());
        atomicLong.getAndIncrement();

        assertEquals(2, ((AtomicLong) xaStore.get(KEY).getValue()).get());
        assertEquals(1, xaStore.get(KEY).getVersion());
    }

    @Test
    public void testThrowsExceptionOnNonSerializableValue() {
        try {
            xaStore.put(new Element(KEY, new Object()));
            fail("Should have thrown an Exception");
        } catch (Exception e) {
            assertTrue("Expected " + CacheException.class.getName() + ", but was " + e.getClass().getName(), e instanceof CacheException);
        }
        assertNull(xaStore.get(KEY));
    }

    public static class SomeKey implements Serializable {

        private final int value;

        public SomeKey(final int value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }
}

package net.sf.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.pool.Pool;
import net.sf.ehcache.pool.impl.ConstantSizeOfEngine;
import net.sf.ehcache.pool.impl.FromLargestCacheOnHeapPoolEvictor;
import net.sf.ehcache.pool.impl.StrictlyBoundedPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Ludovic Orban
 */
public class MemoryStorePinningTest {

    private volatile Cache cache;
    private volatile MemoryStore memoryStore;

    @Before
    public void setUp() {
        cache = new Cache(new CacheConfiguration("myCache1", 0).eternal(true));

        Pool onHeapPool = new StrictlyBoundedPool(
                16384 * 2, // == 2 elements
                new FromLargestCacheOnHeapPoolEvictor(),
                new ConstantSizeOfEngine(
                        1536,  /* 1.5 KB*/
                        14336, /* 14 KB */
                        512    /* 0.5 KB */
                )
        );

        memoryStore = NotifyingMemoryStore.create(cache, onHeapPool);
    }

    @After
    public void tearDown() {
        cache.dispose();
        memoryStore.dispose();
    }


    @Test
    public void testPutIfAbsent() throws Exception {
        final Element ELEMENT = new Element(1, "one");

        memoryStore.setPinned(1, true);
        assertEquals(0, memoryStore.getSize());
        assertNull(memoryStore.putIfAbsent(ELEMENT));
        assertSame(ELEMENT, memoryStore.get(1));
        assertEquals(1, memoryStore.getSize());

        memoryStore.removeAll();
        assertTrue(memoryStore.isPinned(1));
        assertEquals(0, memoryStore.getSize());

        assertNull(memoryStore.putIfAbsent(ELEMENT));
        memoryStore.setPinned(1, false);
        assertSame(ELEMENT, memoryStore.get(1));
        assertEquals(1, memoryStore.getSize());
    }


}

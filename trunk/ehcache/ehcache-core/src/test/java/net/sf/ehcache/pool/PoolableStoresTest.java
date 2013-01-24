package net.sf.ehcache.pool;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Ludovic Orban
 */
public class PoolableStoresTest {

    public static final String DEFAULT_CACHE_MANAGER_SIZE_OF_ENGINE_PROP = "net.sf.ehcache.sizeofengine.default";
    private CacheManager cacheManager;

    @Before
    public void setUp() throws Exception {
        System.getProperties().setProperty(DEFAULT_CACHE_MANAGER_SIZE_OF_ENGINE_PROP,
                "net.sf.ehcache.pool.impl.ConstantSizeOfEngine");
    }

    @After
    public void tearDown() throws Exception {
        if(cacheManager != null && cacheManager.getStatus() == Status.STATUS_ALIVE) {
            cacheManager.shutdown();
        }
        System.getProperties().remove(DEFAULT_CACHE_MANAGER_SIZE_OF_ENGINE_PROP);
        assertThat(System.getProperties().getProperty(DEFAULT_CACHE_MANAGER_SIZE_OF_ENGINE_PROP), nullValue());
    }

    @Test
    public void test() throws Exception {
        cacheManager = new CacheManager(PoolableStoresTest.class.getResourceAsStream("/pool/ehcache-heap-disk.xml"));

        Cache memoryOnlyCache = cacheManager.getCache("memoryOnly");
        Cache overflowToDiskCache = cacheManager.getCache("overflowToDisk");

        for (int i = 0; i < 100; i++) {
            memoryOnlyCache.put(new Element(i, "" + i));
        }

        assertEquals(64, memoryOnlyCache.getSize());

        for (int i = 0; i < 100; i++) {
            overflowToDiskCache.put(new Element(i, "" + i));
        }

        // because of the ConstantSizeOfEngine an entry always occupies 16Kb of memory, no matter if it is
        // in the memory store of the memoryOnly cache, in the memory tier of the overflowToDisk cache or
        // in the disk tier of the overflowToDisk, flushed to disk or not.
        //
        // The reason is that this specific sizeof engine returns a constant size for the element's value no matter
        // how big or small it is. In all those cases, the element's value always is either the actual value or
        // a about-to-be-flushed-to-disk container or a flushed-to-disk marker.
        //
        // This means that since the two caches are sharing an on-heap pool of 1MB, up to 64 16KB elements can fit
        // not matter where they are in those two caches. Since the two tiers of the disk cache consume heap,
        // you can actually cache less with the disk cache than with the heap cache because of that synthetic sizeof engine.

        //System.out.println(memoryOnlyCache.getSize());
        //System.out.println(overflowToDiskCache.getMemoryStoreSize());
        //System.out.println(overflowToDiskCache.getDiskStoreSize());

        assertThat(memoryOnlyCache.getSize() + overflowToDiskCache.getMemoryStoreSize() +
                overflowToDiskCache.getDiskStoreSize(), lessThanOrEqualTo(64L));
    }

}

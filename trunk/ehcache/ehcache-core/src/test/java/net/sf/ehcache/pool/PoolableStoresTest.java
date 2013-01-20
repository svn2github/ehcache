package net.sf.ehcache.pool;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.store.disk.DiskStoreHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.greaterThan;
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
    @Ignore
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

        System.out.println(memoryOnlyCache.getSize());
        System.out.println(overflowToDiskCache.getSize());
        System.out.println(memoryOnlyCache.getSize() + overflowToDiskCache.getSize());

        assertThat(memoryOnlyCache.getSize(), greaterThan(0));
        assertThat(overflowToDiskCache.getSize(), greaterThan(0));
        assertThat(memoryOnlyCache.getSize() + overflowToDiskCache.getSize(), lessThanOrEqualTo(64));
    }

}

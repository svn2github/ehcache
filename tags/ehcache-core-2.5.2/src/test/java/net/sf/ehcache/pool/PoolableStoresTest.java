package net.sf.ehcache.pool;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Ludovic Orban
 */
public class PoolableStoresTest {

    public static final String DEFAULT_CACHE_MANAGER_SIZE_OF_ENGINE_PROP = "net.sf.ehcache.sizeofengine.default";

    @Before
    public void setUp() throws Exception {
        System.getProperties().setProperty(DEFAULT_CACHE_MANAGER_SIZE_OF_ENGINE_PROP,
                "net.sf.ehcache.pool.impl.ConstantSizeOfEngine");
    }

    @After
    public void tearDown() throws Exception {
        System.getProperties().remove(DEFAULT_CACHE_MANAGER_SIZE_OF_ENGINE_PROP);
        assertThat(System.getProperties().getProperty(DEFAULT_CACHE_MANAGER_SIZE_OF_ENGINE_PROP), nullValue());
    }

    @Test
    public void test() throws Exception {
        CacheManager cm = new CacheManager(PoolableStoresTest.class.getResourceAsStream("/pool/ehcache-heap-disk.xml"));

        Cache memoryOnlyCache = cm.getCache("memoryOnly");
        Cache overflowToDiskCache = cm.getCache("overflowToDisk");

        for (int i = 0; i < 100; i++) {
            memoryOnlyCache.put(new Element(i, "" + i));
        }

        assertEquals(64, memoryOnlyCache.getSize());

        for (int i = 0; i < 100; i++) {
            overflowToDiskCache.put(new Element(i, "" + i));
        }

        assertEquals(64, memoryOnlyCache.getSize() + overflowToDiskCache.getSize());


        cm.shutdown();
    }

}

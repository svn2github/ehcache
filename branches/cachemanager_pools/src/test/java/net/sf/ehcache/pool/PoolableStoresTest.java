package net.sf.ehcache.pool;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Ludovic Orban
 */
public class PoolableStoresTest {

    @Test
    public void testMemoryOnly() throws Exception {
        CacheManager cm = new CacheManager(PoolableStoresTest.class.getResourceAsStream("/pool/ehcache-heap-disk.xml"));

        Cache memoryOnlyCache = cm.getCache("memoryOnly");
        Cache diskPersistentCache = cm.getCache("diskPersistent");

        for (int i=0; i<100 ;i++) {
            memoryOnlyCache.put(new Element(i, ""+i));
        }

        assertEquals(64, memoryOnlyCache.getSize());

        for (int i=0; i<100 ;i++) {
            diskPersistentCache.put(new Element(i, ""+i));
        }

        assertEquals(64, memoryOnlyCache.getSize() + diskPersistentCache.getSize());


        cm.shutdown();
    }

}

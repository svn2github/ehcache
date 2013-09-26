package net.sf.ehcache.store.disk;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Alex Snaps
 * @author Ludovic Orban
 */
public class DiskBackMemoryStoreTest {

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
        try {
            final Cache cache = cm.getCache("aCache");


            cache.put(new Element(-1, -1));
            assertEquals(-1, cache.get(-1).getValue());
            cache.remove(-1);
            assertEquals(null, cache.get(-1));

            cache.put(new Element(-2, -2));
            assertEquals(-2, cache.get(-2).getValue());
            cache.remove(-2);
            assertEquals(null, cache.get(-2));

            DiskStoreHelper.flushAllEntriesToDisk(cache).get();
            assertEquals(0, cache.getStatistics().getLocalDiskSize());

            for (int i = 0; i < 10010; i++) {
                cache.put(new Element(i, i));
            }

            DiskStoreHelper.flushAllEntriesToDisk(cache).get();
            assertEquals(10010, cache.getStatistics().getLocalDiskSize());
        } finally {
            cm.shutdown();
        }
    }
}

package net.sf.ehcache.store;

import junit.framework.Assert;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import org.junit.Test;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

/**
 * @author Ludovic Orban
 */
public class DiskBackedMemoryStoreTest {

    @Test
    public void testElementPinning() throws Exception {
        CacheManager cm = new CacheManager();
        Cache cache = new Cache(new CacheConfiguration("myCache", 20).overflowToDisk(true).maxElementsOnDisk(20));
        cm.addCache(cache);

        for (int i = 0; i < 200; i++) {
            Element element = new Element("Ku-" + i, "" + i);
            cache.put(element);
        }

        Thread.sleep(1000);

        Assert.assertEquals(20, cache.getSize());

        for (int i = 0; i < 200; i++) {
            Element element = new Element("Kp-" + i, new Object());
            element.setTimeToIdle(1);
            element.setTimeToLive(1);
            element.setPinned(true);
            cache.put(element);
        }

        for (int i = 0; i < 200; i++) {
            assertNotNull(cache.get("Kp-" + i));
        }

        // wait until all pinned elements expire
        Thread.sleep(1100);

        for (int i = 0; i < 200; i++) {
            assertNull(cache.get("Kp-" + i));
        }

        Assert.assertEquals(20, cache.getSize());

        cm.shutdown();
    }
}

package net.sf.ehcache.store;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import junit.framework.Assert;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;

import org.junit.Test;

/**
 * @author Ludovic Orban
 */
public class DiskBackedMemoryStoreTest {

    @Test
    public void testElementPinning() throws Exception {
        int unpinCount = 200;
        int pinCount = 400;
        int maxElementsOnDisk = 300;
        CacheManager cm = new CacheManager();
        Cache cache = new Cache(new CacheConfiguration("myCache", 0).overflowToDisk(true).maxElementsOnDisk(maxElementsOnDisk));
        cm.addCache(cache);

        for (int i = 0; i < unpinCount; i++) {
            Element element = new Element("Ku-" + i, "" + i);
            cache.put(element);
        }

        Thread.sleep(1000);

        Assert.assertEquals(unpinCount, cache.getSize());

        for (int i = 0; i < pinCount; i++) {
            Element element = new Element("Kp-" + i, new Object());
            element.setTimeToIdle(2);
            element.setTimeToLive(2);
            cache.setPinned(element.getObjectKey(), true);
            cache.put(element);
        }
        Assert.assertEquals(pinCount+unpinCount, cache.getSize());

        for (int i = 0; i < pinCount; i++) {
            assertNotNull(cache.get("Kp-" + i));
        }

        // wait until all pinned elements expire
        Thread.sleep(2200);

        for (int i = 0; i < pinCount; i++) {
            assertNull(cache.get("Kp-" + i));
        }
        Assert.assertEquals(unpinCount, cache.getSize());
        cm.shutdown();
    }
}

package net.sf.ehcache.constructs.refreshahead;

import junit.framework.Assert;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.loader.CacheLoader;

import org.junit.Test;

public class RefreshAheadCacheTest {

    private static CacheLoader stringifyCacheLoader = new StringifyCacheLoaderFactory().createCacheLoader(null, null);

    private static void sleepySeconds(int secs) {
        sleepy(secs * 1000);
    }

    private static void sleepy(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {

        }
    }

    // OK. we want to create an ehcache, then programmitaclly decorate it with
    // locks.
    @Test
    public void testSimpleCaseProgrammatic() {

        CacheManager manager = new CacheManager();
        manager.removalAll();

        manager.addCache(new Cache(new CacheConfiguration().name("test").timeToLiveSeconds(10).overflowToDisk(false)
                .maxElementsInMemory(100)));
        Ehcache cache = manager.getEhcache("test");

        RefreshAheadCacheConfiguration refreshConfig = new RefreshAheadCacheConfiguration().timeToRefreshSeconds(7).numberOfThreads(4)
                .maximumRefreshBacklogItems(100)
                .build();
        RefreshAheadCache decorator = new RefreshAheadCache(cache, refreshConfig);

        cache.registerCacheLoader(stringifyCacheLoader);

        decorator.put(new Element(new Integer(1), new String("1")));
        decorator.put(new Element(new Integer(2), new String("2")));
        decorator.put(new Element(new Integer(3), new String("3")));
        decorator.put(new Element(new Integer(4), new String("4")));

        // get the first one
        Element got = decorator.get(new Integer(1));
        long creationTime = got.getCreationTime();
        Assert.assertNotNull(got);
        Assert.assertEquals(0, decorator.getRefreshSuccessCount().get());

        sleepySeconds(1);
        // now, you should get the same one, no refresh ahead
        got = decorator.get(new Integer(1));
        Assert.assertNotNull(got);
        Assert.assertEquals(0, decorator.getRefreshSuccessCount().get());
        Assert.assertEquals(got.getCreationTime(), creationTime);

        // wait long enough for refresh ahead to trigger. 7+1=8 seconds
        // you'll get the original back right away, but a call done soon should get a new one.
        sleepySeconds(7);
        got = decorator.get(new Integer(1));
        Assert.assertNotNull(got);

        // processing time,
        sleepySeconds(1);
        // better be a new object this time, faulted in via refresh ahead
        // better not have the same creation time as originally
        got = decorator.get(new Integer(1));
        Assert.assertEquals(1, decorator.getRefreshSuccessCount().get());
        Assert.assertFalse(creationTime == got.getCreationTime());

        manager.removalAll();
        manager.shutdown();
    }

}

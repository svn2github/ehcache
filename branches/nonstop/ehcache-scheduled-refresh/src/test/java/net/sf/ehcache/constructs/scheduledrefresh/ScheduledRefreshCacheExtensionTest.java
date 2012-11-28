package net.sf.ehcache.constructs.scheduledrefresh;

import java.util.Calendar;
import java.util.GregorianCalendar;

import junit.framework.Assert;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;

import org.junit.Test;

public class ScheduledRefreshCacheExtensionTest {

    private static OddCacheLoader stupidCacheLoaderOdds = new OddCacheLoader();
    private static EvenCacheLoader stupidCacheLoaderEvens = new EvenCacheLoader();

    private static void sleepySeconds(int secs) {
        sleepy(secs * 1000);
    }

    private static void sleepy(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {

        }
    }

    // OK. we want to create an ehcache, then programmitically decorate it with
    // locks.
    @Test
    public void testSimpleCaseProgrammatic() {

        CacheManager manager = new CacheManager();
        manager.removalAll();

        manager.addCache(new Cache(new CacheConfiguration().name("test").eternal(true).maxEntriesLocalHeap(5000)));
        Ehcache cache = manager.getEhcache("test");
        cache.registerCacheLoader(stupidCacheLoaderEvens);
        cache.registerCacheLoader(stupidCacheLoaderOdds);

        int second = (new GregorianCalendar().get(Calendar.SECOND) + 5) % 60;
        ScheduledRefreshConfiguration config = new ScheduledRefreshConfiguration().batchSize(100).quartzThreadCount
                (4).cronExpression(second + "/5 * * * * ?").build();
        ScheduledRefreshCacheExtension cacheExtension = new ScheduledRefreshCacheExtension(config, cache);
        cache.registerCacheExtension(cacheExtension);
        cacheExtension.init();

        for (int i = 0; i < 10; i++) {
            cache.put(new Element(new Integer(i), i + ""));
        }

        sleepySeconds(8);

        for (Object key : cache.getKeys()) {
            Element val = cache.get(key);
            // System.out.println("["+key+", "+cache.get(key).getObjectValue()+"]");
            int iVal = ((Number) key).intValue();
            if ((iVal & 0x01) == 0) {
                // even
                Assert.assertEquals(iVal + 20000, Long.parseLong((String) val.getObjectValue()));
            } else {
                Assert.assertEquals(iVal + 10000, Long.parseLong((String) val.getObjectValue()));
                // odd
            }

        }
        //cacheExtension.dispose();
        manager.removalAll();
        manager.shutdown();
    }

    // OK. we want to create an ehcache, then programmitaclly decorate it with
    // locks.
    @Test
    public void testSimpleCaseXML() {

        CacheManager manager = CacheManager.create("src/test/resources/ehcache-scheduled-refresh.xml");

        Cache cache = manager.getCache("sr-test");

        int second = (new GregorianCalendar().get(Calendar.SECOND) + 5) % 60;

        for (int i = 0; i < 10; i++) {
            cache.put(new Element(new Integer(i), i + ""));
        }

        second = Math.max(8, 60 - second + 3);
        System.out.println("Scheduled delay is :: " + second);
        sleepySeconds(second);

        for (Object key : cache.getKeys()) {
            Element val = cache.get(key);
            // System.out.println("["+key+", "+cache.get(key).getObjectValue()+"]");
            int iVal = ((Number) key).intValue();
            if ((iVal & 0x01) == 0) {
                // even
                Assert.assertEquals(iVal + 20000, Long.parseLong((String) val.getObjectValue()));
            } else {
                Assert.assertEquals(iVal + 10000, Long.parseLong((String) val.getObjectValue()));
                // odd
            }

        }
        manager.removalAll();

        manager.shutdown();
    }
    
    // OK. we want to create an ehcache, then programmitically decorate it with
    // locks.
    @Test
    public void testPolling() {

        CacheManager manager = new CacheManager();
        manager.removalAll();

        manager.addCache(new Cache(new CacheConfiguration().name("tt").eternal(true).maxEntriesLocalHeap(5000).overflowToDisk(false)));
        Ehcache cache = manager.getEhcache("tt");
        stupidCacheLoaderEvens.setMsDelay(100);
        cache.registerCacheLoader(stupidCacheLoaderEvens);
        cache.registerCacheLoader(stupidCacheLoaderOdds);

        int second = (new GregorianCalendar().get(Calendar.SECOND) + 5) % 60;
        ScheduledRefreshConfiguration config = new ScheduledRefreshConfiguration().batchSize(2).quartzThreadCount
                (2).pollTimeMs(100).cronExpression(second + "/1 * * * * ?").build();
        ScheduledRefreshCacheExtension cacheExtension = new ScheduledRefreshCacheExtension(config, cache);
        cache.registerCacheExtension(cacheExtension);
        cacheExtension.init();

        final int ELEMENT_COUNT=50;
        long[] orig=new long[ELEMENT_COUNT];
        for (int i = 0; i < ELEMENT_COUNT; i++) {
            Element elem = new  Element(new Integer(i), i + "");
            orig[i]=elem.getCreationTime();
            cache.put(elem);
        }

        sleepySeconds(20);

        //cacheExtension.dispose();
        manager.removalAll();
        manager.shutdown();
    }

}
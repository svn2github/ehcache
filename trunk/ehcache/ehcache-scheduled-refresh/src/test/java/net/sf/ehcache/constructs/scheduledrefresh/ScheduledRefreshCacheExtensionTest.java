package net.sf.ehcache.constructs.scheduledrefresh;

import java.util.Calendar;
import java.util.GregorianCalendar;

import junit.framework.Assert;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.loader.CacheLoader;

import org.junit.Test;

public class ScheduledRefreshCacheExtensionTest {

	private static CacheLoader stupidCacheLoaderOdds = new OddClassLoader();
	private static CacheLoader stupidCacheLoaderEvens = new EvenClassLoader();

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

		manager.addCache(new Cache(new CacheConfiguration().name("test")
				.eternal(true).maxEntriesLocalHeap(5000)));
		Ehcache cache = manager.getEhcache("test");
		cache.registerCacheLoader(stupidCacheLoaderEvens);
		cache.registerCacheLoader(stupidCacheLoaderOdds);

		int second = (new GregorianCalendar().get(Calendar.SECOND) + 5) % 60;
		ScheduledRefreshConfiguration config = new ScheduledRefreshConfiguration()
				.batchSize(100).quartzThreadCount(4)
				.cronExpression(second + "/5 * * * * ?").build();
		ScheduledRefreshCacheExtension cacheExtension = new ScheduledRefreshCacheExtension(
				config, cache);
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
				Assert.assertEquals(iVal + 20000,
						Long.parseLong((String) val.getObjectValue()));
			} else {
				Assert.assertEquals(iVal + 10000,
						Long.parseLong((String) val.getObjectValue()));
				// odd
			}

		}
		manager.removalAll();
		manager.shutdown();
	}
	
	   // OK. we want to create an ehcache, then programmitaclly decorate it with
    // locks.
    @Test
    public void testSimpleCaseXML() {

        CacheManager manager = CacheManager.create("src/test/resources/ehcache-scheduled-refresh.xml");

        Cache cache=manager.getCache("sr-test");

        int second = (new GregorianCalendar().get(Calendar.SECOND) + 5) % 60;

        for (int i = 0; i < 10; i++) {
            cache.put(new Element(new Integer(i), i + ""));
        }

        second=Math.max(8,60-second+3);
        System.out.println(":: "+second);
        sleepySeconds(second);

        for (Object key : cache.getKeys()) {
            Element val = cache.get(key);
            // System.out.println("["+key+", "+cache.get(key).getObjectValue()+"]");
            int iVal = ((Number) key).intValue();
            if ((iVal & 0x01) == 0) {
                // even
                Assert.assertEquals(iVal + 20000,
                        Long.parseLong((String) val.getObjectValue()));
            } else {
                Assert.assertEquals(iVal + 10000,
                        Long.parseLong((String) val.getObjectValue()));
                // odd
            }

        }
        manager.removalAll();

        manager.shutdown();
    }
}
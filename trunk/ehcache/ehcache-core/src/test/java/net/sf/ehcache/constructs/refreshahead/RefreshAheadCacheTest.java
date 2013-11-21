package net.sf.ehcache.constructs.refreshahead;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.loader.CacheLoader;
import net.sf.ehcache.statistics.extended.ExtendedStatistics;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.terracotta.test.categories.CheckShorts;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static net.sf.ehcache.util.RetryAssert.assertBy;
import static net.sf.ehcache.util.RetryAssert.sleepFor;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

@Category(CheckShorts.class)
public class RefreshAheadCacheTest {

    private static CacheLoader stringifyCacheLoader = new StringifyCacheLoaderFactory().createCacheLoader(null, null);

    @Test
    public void testSimpleCaseProgrammatic() {

        CacheManager manager = new CacheManager(new Configuration().name("programmatic"));

        try {
            manager.addCache(new Cache(new CacheConfiguration().name("test").timeToLiveSeconds(10).overflowToDisk(false)
                    .maxElementsInMemory(100)));
            Ehcache cache = manager.getEhcache("test");

            RefreshAheadCacheConfiguration refreshConfig = new RefreshAheadCacheConfiguration().timeToRefreshSeconds(3).numberOfThreads(4)
                    .maximumRefreshBacklogItems(100)
                    .build();
            final RefreshAheadCache decorator = new RefreshAheadCache(cache, refreshConfig);

            cache.registerCacheLoader(stringifyCacheLoader);

            Set<ExtendedStatistics.Statistic<Number>> offerStat = RefreshAheadCache.findOfferStatistic(decorator);
            Set<ExtendedStatistics.Statistic<Number>> processedStat = RefreshAheadCache.findProcessedStatistic(decorator);
            Set<ExtendedStatistics.Statistic<Number>> successStat = RefreshAheadCache.findRefreshedStatistic(decorator);
            Set<ExtendedStatistics.Statistic<Number>> droppedStat = RefreshAheadCache.findDroppedStatistic(decorator);
            Set<ExtendedStatistics.Statistic<Number>> backlogStat = RefreshAheadCache.findBacklogStatistic(decorator);
            assertFalse(offerStat.isEmpty());
            assertFalse(processedStat.isEmpty());
            assertFalse(droppedStat.isEmpty());
            assertFalse(successStat.isEmpty());
            assertFalse(backlogStat.isEmpty());

            final Integer key1 = 1;
            decorator.put(new Element(key1, "1"));
            decorator.put(new Element(2, "2"));
            decorator.put(new Element(3, "3"));
            decorator.put(new Element(4, "4"));

            // get the first one
            Element got = decorator.get(key1);
            long creationTime = got.getCreationTime();
            assertNotNull(got);
            assertEquals(0, decorator.getRefreshSuccessCount());

            sleepFor(1, TimeUnit.SECONDS);
            // now, you should get the same one, no refresh ahead
            got = decorator.get(key1);
            assertNotNull(got);
            assertEquals(0, decorator.getRefreshSuccessCount());
            assertEquals(0, successStat.iterator().next().value().longValue());
            assertEquals(got.getCreationTime(), creationTime);

            // wait long enough for refresh ahead to trigger.
            assertBy(5, TimeUnit.SECONDS, new Callable<Element>() {
                @Override
                public Element call() throws Exception {
                    return decorator.get(key1);
                }
            }, not(sameInstance(got)));
            got = decorator.get(key1);
            assertEquals(1, decorator.getRefreshSuccessCount());
            assertEquals(1, successStat.iterator().next().value().longValue());
            assertEquals(1, processedStat.iterator().next().value().longValue());
            assertEquals(0, droppedStat.iterator().next().value().longValue());
            assertEquals(0, backlogStat.iterator().next().value().longValue());
            assertFalse(creationTime == got.getCreationTime());
        } finally {
            manager.shutdown();
        }
    }

    @Test
    public void testSimpleCaseXML() {

        CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream
                ("/ehcache-refresh-ahead-simple.xml"));

        try {
            final Ehcache decorator = cacheManager.getEhcache("testRefreshAhead1");

            final Integer key1 = 1;
            decorator.put(new Element(key1, "1"));
            decorator.put(new Element(2, "2"));
            decorator.put(new Element(3, "3"));
            decorator.put(new Element(4, "4"));

            // get the first one
            Element got = decorator.get(key1);
            long creationTime = got.getCreationTime();
            assertNotNull(got);

            sleepFor(1, TimeUnit.SECONDS);
            // now, you should get the same one, no refresh ahead
            got = decorator.get(key1);
            assertNotNull(got);
            assertEquals(got.getCreationTime(), creationTime);

            assertBy(5, TimeUnit.SECONDS, new Callable<Element>() {
                @Override
                public Element call() throws Exception {
                    return decorator.get(key1);
                }
            }, not(sameInstance(got)));
            // better not have the same creation time as originally
            got = decorator.get(key1);
            assertFalse(creationTime == got.getCreationTime());

        } finally {
            cacheManager.shutdown();
        }
    }

    @Test
    public void testSimpleCaseXMLNullEvicts() {

        CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream
                ("/ehcache-refresh-ahead-simple.xml"));

        try {
            final Ehcache decorator=cacheManager.getEhcache("testRefreshAhead2");


            final Integer key1 = 1;
            decorator.put(new Element(key1, "1"));
            decorator.put(new Element(2, "2"));
            decorator.put(new Element(3, "3"));
            decorator.put(new Element(4, "4"));

            // get the first one
            Element got = decorator.get(key1);
            long creationTime = got.getCreationTime();
            assertNotNull(got);

            sleepFor(1, TimeUnit.SECONDS);
            // now, you should get the same one, no refresh ahead
            got = decorator.get(key1);
            assertNotNull(got);
            assertEquals(got.getCreationTime(), creationTime);

            assertBy(5, TimeUnit.SECONDS, new Callable<Element>() {
                @Override
                public Element call() throws Exception {
                    return decorator.get(key1);
                }
            }, nullValue());
        } finally {
            cacheManager.shutdown();
        }
    }
}
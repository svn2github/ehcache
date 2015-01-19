package net.sf.ehcache.store;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.statistics.StatisticsGateway;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * MemoryLimitedCacheLoaderTest
 */
public class MemoryLimitedCacheLoaderTest {

    private Ehcache cache;
    private CacheConfiguration configuration;
    private TestCacheLoader loader;
    private StatisticsGateway statisticsGateway;

    @Before
    public void setUp() {
        cache = mock(Ehcache.class);
        configuration = new CacheConfiguration();
        when(cache.getCacheConfiguration()).thenReturn(configuration);
        statisticsGateway = mock(StatisticsGateway.class);
        when(cache.getStatistics()).thenReturn(statisticsGateway);
        loader = new TestCacheLoader();

    }

    @Test
    public void testCountBasedLimitSetToNoLimit() {
        configuration.setOverflowToOffHeap(false);
        configuration.setMaxEntriesLocalHeap(0);

        assertThat(loader.isInMemoryLimitReached(cache, 0), is(false));
        assertThat(loader.isInMemoryLimitReached(cache, Integer.MAX_VALUE), is(true));
    }

    @Test
    public void testCountBasedLimitSetToSomeLimit() {
        configuration.setOverflowToOffHeap(false);
        int maxEntriesLocalHeap = 10;
        configuration.setMaxEntriesLocalHeap(maxEntriesLocalHeap);

        assertThat(loader.isInMemoryLimitReached(cache, maxEntriesLocalHeap - 2), is(false));
        assertThat(loader.isInMemoryLimitReached(cache, maxEntriesLocalHeap), is(true));
        assertThat(loader.isInMemoryLimitReached(cache, maxEntriesLocalHeap + 2), is(true));
    }

    @Test
    public void testLocalHeapSizeBased() {
        configuration.setOverflowToOffHeap(false);
        configuration.setMaxBytesLocalHeap(1024L);

        when(statisticsGateway.getLocalHeapSize()).thenReturn(0L, 1L, 4L);
        when(statisticsGateway.getLocalHeapSizeInBytes()).thenReturn(250L, 1000L);

        assertThat(loader.isInMemoryLimitReached(cache, 0), is(false));
        assertThat(loader.isInMemoryLimitReached(cache, 1), is(false));
        assertThat(loader.isInMemoryLimitReached(cache, 4), is(true));
    }

    @Test
    public void testOffHeapSizeBased() {
        configuration.setOverflowToOffHeap(true);
        configuration.setMaxBytesLocalHeap(1024L);
        configuration.setMaxBytesLocalOffHeap(2048L);

        when(statisticsGateway.getLocalOffHeapSize()).thenReturn(0L, 1L, 4L);
        when(statisticsGateway.getLocalOffHeapSizeInBytes()).thenReturn(500L, 2000L);

        assertThat(loader.isInMemoryLimitReached(cache, 0), is(false));
        assertThat(loader.isInMemoryLimitReached(cache, 1), is(false));
        assertThat(loader.isInMemoryLimitReached(cache, 4), is(true));
    }

    @Test
    public void testPooledOffHeapSizeBased() {
        // Sizing info is in CacheManager, never consulted by MemoryLimitedCacheLoader
        configuration.setOverflowToOffHeap(true);

        assertThat(loader.isInMemoryLimitReached(cache, 0), is(false));
        assertThat(loader.isInMemoryLimitReached(cache, Integer.MAX_VALUE), is(true));
    }

    @Test
    public void testPooledHeapSizeBased() {
        // Sizing info is in CacheManager, never consulted by MemoryLimitedCacheLoader

        assertThat(loader.isInMemoryLimitReached(cache, 0), is(false));
        assertThat(loader.isInMemoryLimitReached(cache, Integer.MAX_VALUE), is(true));
    }

    private static class TestCacheLoader extends MemoryLimitedCacheLoader {

        @Override
        public void load(Ehcache cache) throws CacheException {
            // no-op
        }

        @Override
        public boolean isAsynchronous() {
            return true; // not used
        }
    }
}

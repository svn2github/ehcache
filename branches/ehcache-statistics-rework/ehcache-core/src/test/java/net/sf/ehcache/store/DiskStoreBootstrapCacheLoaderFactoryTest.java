package net.sf.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.store.disk.DiskStoreHelper;

import org.junit.After;
import org.junit.Test;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import static net.sf.ehcache.config.MemoryUnit.KILOBYTES;
import static net.sf.ehcache.config.MemoryUnit.MEGABYTES;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;

/**
 * @author Alex Snaps
 */
public class DiskStoreBootstrapCacheLoaderFactoryTest {

    private static final int ELEMENTS_ON_DISK = 500;
    private CacheManager manager;
    private Cache cacheElementCountBound;
    private Cache cacheSizeBound;
    private TestDiskStoreBootstrapCacheLoader cacheElementCountBoundBootstrapCacheLoader;
    private TestDiskStoreBootstrapCacheLoader cacheSizeBoundBootstrapCacheLoader;

    public void setUp(CacheUT cut) {
        initCacheManager(cut);
        switch (cut) {
            case elementBased:
                cacheElementCountBound.removeAll();
                populateCache(cacheElementCountBound);
                break;
            case sizeBased:
                cacheSizeBound.removeAll();
                populateCache(cacheSizeBound);
                break;
        }
    }

    private void populateCache(final Cache cache) {
        for (int i = 0; i < ELEMENTS_ON_DISK; i++) {
            cache.put(new Element(i, "Some value for key " + i));
        }
    }

    @Test
    public void testLoadsFromDiskWithMaxElementsInMemorySet() throws Exception {
        setUp(CacheUT.elementBased);
        DiskStoreHelper.flushAllEntriesToDisk(cacheElementCountBound).get();
        int onDiskElements = cacheElementCountBound.getDiskStoreSize();
        cacheElementCountBoundBootstrapCacheLoader.triggerLoad();
        assertThat(cacheElementCountBound.getMemoryStoreSize(), is(100L));
        manager.shutdown();
        initCacheManager(CacheUT.elementBased);
        assertThat(cacheElementCountBound.getDiskStoreSize(), is(onDiskElements));
        assertThat(cacheElementCountBound.getMemoryStoreSize(), is(0L));
        cacheElementCountBoundBootstrapCacheLoader.triggerLoad();
        assertThat(cacheElementCountBound.getDiskStoreSize(), is(onDiskElements));
        assertThat(cacheElementCountBound.getMemoryStoreSize(), is(100L));
    }

    @Test
    public void testLoadsFromDiskWithMaxBytesOnHeapSet() throws Exception {
        setUp(CacheUT.sizeBased);
        DiskStoreHelper.flushAllEntriesToDisk(cacheSizeBound).get();
        cacheSizeBoundBootstrapCacheLoader.triggerLoad();
        int onDiskSize = cacheSizeBound.getDiskStoreSize();
        assertThat(cacheSizeBound.getMemoryStoreSize(), greaterThan(0L));
        assertThat(cacheSizeBound.getStatistics().getCore().getLocalHeapSizeInBytes(), lessThanOrEqualTo(KILOBYTES.toBytes(220L)));
        assertThat(cacheSizeBound.getDiskStoreSize(), is(onDiskSize));
        manager.shutdown();
        initCacheManager(CacheUT.sizeBased);
        assertThat(cacheSizeBound.getDiskStoreSize(), is(onDiskSize));
        assertThat(cacheSizeBound.getMemoryStoreSize(), is(0L));
        cacheSizeBoundBootstrapCacheLoader.triggerLoad();
        assertThat(cacheSizeBound.getMemoryStoreSize(), greaterThan(0L));
        assertThat(cacheSizeBound.getLiveCacheStatistics().getLocalHeapSizeInBytes(), lessThanOrEqualTo(KILOBYTES.toBytes(220L)));
    }

    private void initCacheManager(CacheUT cut) {
        switch (cut) {
            case elementBased:
                manager = new CacheManager(new Configuration().diskStore(new DiskStoreConfiguration().path("java.io.tmpdir/DiskPersistent")));
                cacheElementCountBoundBootstrapCacheLoader = new TestDiskStoreBootstrapCacheLoader();
                cacheElementCountBound = new Cache(new CacheConfiguration("maxElementsInMemory", 100)
                    .eternal(true)
                    .diskPersistent(true)
                    .overflowToDisk(true)
                    .maxEntriesLocalDisk(ELEMENTS_ON_DISK), null, cacheElementCountBoundBootstrapCacheLoader);
                manager.addCache(cacheElementCountBound);
                break;
            case sizeBased:
                manager = new CacheManager(new Configuration().diskStore(new DiskStoreConfiguration().path("java.io.tmpdir/DiskPersistentSize")));
                cacheSizeBoundBootstrapCacheLoader = new TestDiskStoreBootstrapCacheLoader();
                cacheSizeBound = new Cache(new CacheConfiguration("maxOnHeap", 0)
                    .eternal(true)
                    .diskPersistent(true)
                    .overflowToDisk(true)
                    .maxBytesLocalHeap(220, KILOBYTES)
                    .maxBytesLocalDisk(300, MEGABYTES), null, cacheSizeBoundBootstrapCacheLoader);
                manager.addCache(cacheSizeBound);
                cacheSizeBound.getStatistics().setSampledStatisticsEnabled(true);
                break;
        }
    }

    @After
    public void shutdown() {
        if (manager != null && manager.getStatus() == Status.STATUS_ALIVE) {
            manager.shutdown();
        }
    }

    private static enum CacheUT {
        elementBased, sizeBased
    }

    static class TestDiskStoreBootstrapCacheLoader extends DiskStoreBootstrapCacheLoader {

        private final CyclicBarrier before = new CyclicBarrier(2);
        private final CyclicBarrier after = new CyclicBarrier(2);

        public TestDiskStoreBootstrapCacheLoader() {
            super(true);
        }

        @Override
        protected void doLoad(Ehcache cache) {
            try {
                before.await();
                try {
                    super.doLoad(cache);
                } finally {
                    after.await();
                }
            } catch (InterruptedException e) {
                throw new AssertionError(e);
            } catch (BrokenBarrierException e) {
                throw new AssertionError(e);
            }
        }

        public void triggerLoad() {
            try {
                before.await();
                after.await();
            } catch (InterruptedException e) {
                throw new AssertionError(e);
            } catch (BrokenBarrierException e) {
                throw new AssertionError(e);
            }
        }
    }
}

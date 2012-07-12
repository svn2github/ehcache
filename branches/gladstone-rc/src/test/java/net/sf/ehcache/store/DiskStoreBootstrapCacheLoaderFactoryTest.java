package net.sf.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.config.MemoryUnit;
import net.sf.ehcache.util.RetryAssert;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Test;

import java.util.concurrent.Callable;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Alex Snaps
 */
public class DiskStoreBootstrapCacheLoaderFactoryTest {

    private static final int ELEMENTS_ON_DISK = 500;
    private static final int LOADER_DELAY = 500;
    private CacheManager manager;
    private Cache cacheElementCountBound;
    private Cache cacheSizeBound;
    private DiskStoreBootstrapCacheLoader cacheElementCountBoundBootstrapCacheLoader;
    private DiskStoreBootstrapCacheLoader cacheSizeBoundBootstrapCacheLoader;

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
    public void testLoadsFromDiskWithMaxElementsInMemorySet() throws InterruptedException {
        setUp(CacheUT.elementBased);
        int waitCycles = 0;
        while (cacheElementCountBound.getDiskStoreSize() != ELEMENTS_ON_DISK && waitCycles < 15) {
            System.err.println("Not all entries have been spooled to disk, waiting a bit ... ");
            Thread.sleep(250);
            waitCycles++;
        }
        waitForBootstrapLoader(cacheElementCountBoundBootstrapCacheLoader);
        RetryAssert.assertBy(10, SECONDS, new Callable<Integer>() {
                public Integer call() throws Exception {
                    return cacheElementCountBound.getDiskStoreSize();
                }
            }, Is.is(ELEMENTS_ON_DISK));
        assertThat(cacheElementCountBound.getMemoryStoreSize(), is(100L));
        manager.shutdown();
        initCacheManager(CacheUT.elementBased);
        RetryAssert.assertBy(10, SECONDS, new Callable<Integer>() {
                public Integer call() throws Exception {
                    return cacheElementCountBound.getDiskStoreSize();
                }
            }, Is.is(ELEMENTS_ON_DISK));
        assertThat(cacheElementCountBound.getMemoryStoreSize(), is(0L));
        waitForBootstrapLoader(cacheElementCountBoundBootstrapCacheLoader);
        RetryAssert.assertBy(10, SECONDS, new Callable<Integer>() {
                public Integer call() throws Exception {
                    return cacheElementCountBound.getDiskStoreSize();
                }
            }, Is.is(ELEMENTS_ON_DISK));
        assertThat(cacheElementCountBound.getMemoryStoreSize(), is(100L));
        assertThat(cacheElementCountBoundBootstrapCacheLoader.getLoadedElements(), is(100));
    }

    @Test
    public void testLoadsFromDiskWithMaxBytesOnHeapSet() throws InterruptedException {
        setUp(CacheUT.sizeBased);
        int waitCycles = 0;
        while (cacheSizeBound.getDiskStoreSize() != ELEMENTS_ON_DISK && waitCycles < 15) {
            System.err.println("Not all entries have been spooled to disk, waiting a bit ... " + cacheSizeBound.getDiskStoreSize() + " in");
            Thread.sleep(250);
            waitCycles++;
        }
        waitForBootstrapLoader(cacheSizeBoundBootstrapCacheLoader);
        RetryAssert.assertBy(10, SECONDS, new Callable<Integer>() {
                public Integer call() throws Exception {
                    return cacheSizeBound.getDiskStoreSize();
                }
            }, Is.is(ELEMENTS_ON_DISK));
        assertThat(cacheSizeBound.getLiveCacheStatistics().getLocalHeapSizeInBytes() <= MemoryUnit.KILOBYTES.toBytes(220), is(true));
        manager.shutdown();
        initCacheManager(CacheUT.sizeBased);
        RetryAssert.assertBy(10, SECONDS, new Callable<Integer>() {
                public Integer call() throws Exception {
                    return cacheSizeBound.getDiskStoreSize();
                }
            }, Is.is(ELEMENTS_ON_DISK));
        assertThat(cacheSizeBound.getMemoryStoreSize(), is(0L));
        waitForBootstrapLoader(cacheSizeBoundBootstrapCacheLoader);
        RetryAssert.assertBy(10, SECONDS, new Callable<Integer>() {
                public Integer call() throws Exception {
                    return cacheSizeBound.getDiskStoreSize();
                }
            }, Is.is(ELEMENTS_ON_DISK));
        assertThat(cacheSizeBound.getLiveCacheStatistics().getLocalHeapSizeInBytes() <= MemoryUnit.KILOBYTES.toBytes(220), is(true));
    }

    private int waitForBootstrapLoader(DiskStoreBootstrapCacheLoader bootstrapCacheLoader) throws InterruptedException {
        while (!bootstrapCacheLoader.isDoneLoading()) {
            System.err.println("Waiting for the loader to be done... Loaded " + bootstrapCacheLoader.getLoadedElements()
                               + " elements so far");
            Thread.sleep(LOADER_DELAY);
        }
        return bootstrapCacheLoader.getLoadedElements();
    }

    private void initCacheManager(CacheUT cut) {
        switch (cut) {
            case elementBased:
                manager = new CacheManager(new Configuration().diskStore(new DiskStoreConfiguration().path("java.io.tmpdir/DiskPersistent")));
                cacheElementCountBoundBootstrapCacheLoader = new DiskStoreBootstrapCacheLoader(LOADER_DELAY);
                cacheElementCountBound = new Cache(new CacheConfiguration("maxElementsInMemory", 100)
                    .eternal(true)
                    .diskPersistent(true)
                    .overflowToDisk(true)
                    .maxEntriesLocalDisk(ELEMENTS_ON_DISK), null, cacheElementCountBoundBootstrapCacheLoader);
                manager.addCache(cacheElementCountBound);
                break;
            case sizeBased:
                manager = new CacheManager(new Configuration().diskStore(new DiskStoreConfiguration().path("java.io.tmpdir/DiskPersistentSize")));
                cacheSizeBoundBootstrapCacheLoader = new DiskStoreBootstrapCacheLoader(LOADER_DELAY);
                cacheSizeBound = new Cache(new CacheConfiguration("maxOnHeap", 0)
                    .eternal(true)
                    .diskPersistent(true)
                    .overflowToDisk(true)
                    .maxBytesLocalHeap(220, MemoryUnit.KILOBYTES)
                    .maxBytesLocalDisk(300, MemoryUnit.MEGABYTES), null, cacheSizeBoundBootstrapCacheLoader);
                manager.addCache(cacheSizeBound);
                cacheSizeBound.setSampledStatisticsEnabled(true);
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
}

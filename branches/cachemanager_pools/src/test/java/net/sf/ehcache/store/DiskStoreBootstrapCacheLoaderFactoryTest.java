package net.sf.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.MemoryUnit;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * @author Alex Snaps
 */
public class DiskStoreBootstrapCacheLoaderFactoryTest {

    private static final int ELEMENTS_ON_DISK = 1000;
    private static final int LOADER_DELAY = 500;
    private CacheManager manager;
    private Cache cacheElementCountBound;
    private Cache cacheSizeBound;
    private DiskStoreBootstrapCacheLoader cacheElementCountBoundBootstrapCacheLoader;
    private DiskStoreBootstrapCacheLoader cacheSizeBoundBootstrapCacheLoader;

    @Before
    public void setUp() {
        initCacheManager();
        cacheElementCountBound.removeAll();
        populateCache(cacheElementCountBound);
        cacheSizeBound.removeAll();
        populateCache(cacheSizeBound);
    }

    private void populateCache(final Cache cache) {
        for(int i = 0; i < ELEMENTS_ON_DISK; i++) {
            cache.put(new Element(i, "Some value for key " + i));
        }
    }

    @Test
    public void testLoadsFromDiskWithMaxElementsInMemorySet() throws InterruptedException {
        int waitCycles = 0;
        while(cacheElementCountBound.getDiskStoreSize() != ELEMENTS_ON_DISK && waitCycles < 15) {
            System.err.println("Not all entries have been spooled to disk, waiting a bit ... ");
            Thread.sleep(250);
            waitCycles++;
        }
        waitForBootstrapLoader(cacheElementCountBoundBootstrapCacheLoader);
        assertThat(cacheElementCountBound.getDiskStoreSize(), is(ELEMENTS_ON_DISK));
        assertThat(cacheElementCountBound.getMemoryStoreSize(), is(100L));
        manager.shutdown();
        initCacheManager();
        assertThat(cacheElementCountBound.getDiskStoreSize(), is(ELEMENTS_ON_DISK));
        assertThat(cacheElementCountBound.getMemoryStoreSize(), is(0L));
        waitForBootstrapLoader(cacheElementCountBoundBootstrapCacheLoader);
        assertThat(cacheElementCountBound.getDiskStoreSize(), is(ELEMENTS_ON_DISK));
        assertThat(cacheElementCountBound.getMemoryStoreSize(), is(100L));
        assertThat(cacheElementCountBoundBootstrapCacheLoader.getLoadedElements(), is(100));
    }

    @Test
    public void testLoadsFromDiskWithMaxBytesOnHeapSet() throws InterruptedException {
        int waitCycles = 0;
        while(cacheSizeBound.getDiskStoreSize() != ELEMENTS_ON_DISK && waitCycles < 15) {
            System.err.println("Not all entries have been spooled to disk, waiting a bit ... " + cacheSizeBound.getDiskStoreSize() + " in");
            Thread.sleep(250);
            waitCycles++;
        }
        waitForBootstrapLoader(cacheSizeBoundBootstrapCacheLoader);
        assertThat(cacheSizeBound.getDiskStoreSize(), is(ELEMENTS_ON_DISK));
        assertThat(cacheSizeBound.getMemoryStoreSize(), is(100L));
        manager.shutdown();
        initCacheManager();
        assertThat(cacheSizeBound.getDiskStoreSize(), is(ELEMENTS_ON_DISK));
        assertThat(cacheSizeBound.getMemoryStoreSize(), is(0L));
        waitForBootstrapLoader(cacheSizeBoundBootstrapCacheLoader);
        assertThat(cacheSizeBound.getDiskStoreSize(), is(ELEMENTS_ON_DISK));
        assertThat(cacheSizeBound.getMemoryStoreSize(), is(100L));
        assertThat(cacheSizeBoundBootstrapCacheLoader.getLoadedElements(), is(100));
    }

    private void waitForBootstrapLoader(DiskStoreBootstrapCacheLoader bootstrapCacheLoader) throws InterruptedException {
        while(!bootstrapCacheLoader.isDoneLoading()) {
            System.err.println("Waiting for the loader to be done... Loaded " + bootstrapCacheLoader.getLoadedElements()
                               + " elements so far" );
            Thread.sleep(LOADER_DELAY);
        }
    }

    private void initCacheManager() {
        manager = new CacheManager(new Configuration());
        cacheElementCountBoundBootstrapCacheLoader = new DiskStoreBootstrapCacheLoader(LOADER_DELAY);
        cacheSizeBoundBootstrapCacheLoader = new DiskStoreBootstrapCacheLoader(LOADER_DELAY);
        cacheElementCountBound = new Cache(new CacheConfiguration("maxElementsInMemory", 100)
            .diskPersistent(true)
            .overflowToDisk(true)
            .diskStorePath("caches/DiskPersistent")
            .maxElementsOnDisk(2000), null, cacheElementCountBoundBootstrapCacheLoader);
        cacheSizeBound = new Cache(new CacheConfiguration("maxOnHeap", 0)
            .diskPersistent(true)
            .overflowToDisk(true)
            .maxOnHeap(220, MemoryUnit.KILOBYTES)
            .maxOnDisk(300, MemoryUnit.MEGABYTES)
            .diskStorePath("caches/DiskPersistentSize")
            .maxElementsOnDisk(1000), null, cacheSizeBoundBootstrapCacheLoader);
        manager.addCache(cacheElementCountBound);
        manager.addCache(cacheSizeBound);
    }
}

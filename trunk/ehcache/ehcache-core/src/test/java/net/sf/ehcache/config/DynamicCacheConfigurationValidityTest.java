package net.sf.ehcache.config;

import junit.framework.Assert;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Raghvendra Singh
 */
public class DynamicCacheConfigurationValidityTest {

    private CacheManager cacheManager;
    private final String TEST_NAME = "tempCache";

    @Before
    public void setup() {
        this.cacheManager = CacheManager.getInstance();
        cacheManager.addCache(new Cache(new CacheConfiguration(TEST_NAME, 0)));
    }

    @After
    public void cleanup() {
      this.cacheManager.removalAll();
    }

    @Test
    public void testMaxEntriesLocalHeap() {

        CacheConfiguration cacheConfiguration;
        try {
            cacheConfiguration = new CacheConfiguration(TEST_NAME, -1);
            Assert.fail("should not be able to create config with negative maxEntriesLocalHeap");
        } catch (IllegalArgumentException e) {
            // expected exception;
            Assert.assertEquals("Illegal value -1 for maxEntriesLocalHeap: has to be larger than or equal to 0", e.getMessage());
        }

        Cache cache = cacheManager.getCache(TEST_NAME);
        Assert.assertEquals(0, cache.getCacheConfiguration().getMaxEntriesLocalHeap());
        try {
            cache.getCacheConfiguration().setMaxEntriesLocalHeap(-1234L);
            Assert.fail("should not be able to set negative maxEntriesLocalHeap");
        } catch (IllegalArgumentException e) {
            // expected exception;
            Assert.assertEquals("Illegal value -1234 for maxEntriesLocalHeap: has to be larger than or equal to 0", e.getMessage());
        }

        Assert.assertEquals(0, cache.getCacheConfiguration().getMaxEntriesLocalHeap());
        cache.getCacheConfiguration().setMaxEntriesLocalHeap(1234L);
        Assert.assertEquals(1234, cache.getCacheConfiguration().getMaxEntriesLocalHeap());

        cache.getCacheConfiguration().setMaxEntriesLocalHeap(0);
        Assert.assertEquals(0, cache.getCacheConfiguration().getMaxEntriesLocalHeap());
    }

    @Test
    public void testMaxBytesLocalOffHeap() {
        Cache cache = cacheManager.getCache(TEST_NAME);

        Assert.assertEquals(0, cache.getCacheConfiguration().getMaxBytesLocalOffHeap());
        try {
            cache.getCacheConfiguration().setMaxBytesLocalOffHeap(-1234L);
            Assert.fail("should not be able to set maxEntriesLocalHeap dynamically");
        } catch (IllegalStateException e) {
            // expected exception;
            Assert.assertEquals(e.getMessage(), "OffHeap can't be set dynamically!");
        }

        try {
            cache.getCacheConfiguration().setMaxBytesLocalOffHeap(1234L);
            Assert.fail("should not be able to set maxEntriesLocalHeap dynamically");
        } catch (IllegalStateException e) {
            // expected exception;
            Assert.assertEquals(e.getMessage(), "OffHeap can't be set dynamically!");
        }

        Assert.assertEquals(0, cache.getCacheConfiguration().getMaxBytesLocalOffHeap());
    }

    @Test
    public void testMaxEntriesLocalDisk() {
        Cache cache = cacheManager.getCache(TEST_NAME);
        Assert.assertEquals(0, cache.getCacheConfiguration().getMaxEntriesLocalDisk());
        try {
            cache.getCacheConfiguration().setMaxEntriesLocalDisk(-1234L);
            Assert.fail("should not be able to set negative maxEntriesLocalDisk");
        } catch (IllegalArgumentException e) {
            // expected exception;
            Assert.assertEquals("Illegal value -1234 for maxEntriesLocalDisk: has to be larger than or equal to 0", e.getMessage());
        }

        Assert.assertEquals(0, cache.getCacheConfiguration().getMaxEntriesLocalDisk());
        cache.getCacheConfiguration().setMaxEntriesLocalHeap(1234L);
        Assert.assertEquals(1234, cache.getCacheConfiguration().getMaxEntriesLocalHeap());

        cache.getCacheConfiguration().setMaxEntriesLocalHeap(0);
        Assert.assertEquals(0, cache.getCacheConfiguration().getMaxEntriesLocalHeap());
    }

    @Test
    public void testMaxElementsOnDisk() {
        Cache cache = cacheManager.getCache(TEST_NAME);
        Assert.assertEquals(0, cache.getCacheConfiguration().getMaxElementsOnDisk());
        try {
            cache.getCacheConfiguration().setMaxElementsOnDisk(-1234);
            Assert.fail("should not be able to set negative maxElementsOnDisk");
        } catch (IllegalArgumentException e) {
            // expected exception;
            Assert.assertEquals("Illegal value -1234 for maxElementsOnDisk: has to be larger than or equal to 0", e.getMessage());
        }

        Assert.assertEquals(0, cache.getCacheConfiguration().getMaxElementsOnDisk());
        cache.getCacheConfiguration().setMaxElementsOnDisk(1234);
        Assert.assertEquals(1234, cache.getCacheConfiguration().getMaxElementsOnDisk());
    }

    @Test
    public void testTTL() {
        Cache cache = cacheManager.getCache(TEST_NAME);
        Assert.assertEquals(false, cache.getCacheConfiguration().isEternal());
        Assert.assertEquals(0, cache.getCacheConfiguration().getTimeToLiveSeconds());
        try {
            cache.getCacheConfiguration().setTimeToLiveSeconds(-1234L);
            Assert.fail("should not be able to set negative TTL");
        } catch (IllegalArgumentException e) {
            // expected exception;
            Assert.assertEquals("Illegal value -1234 for timeToLiveSeconds: has to be larger than or equal to 0", e.getMessage());
        }

        Assert.assertEquals(0, cache.getCacheConfiguration().getTimeToLiveSeconds());
        cache.getCacheConfiguration().setTimeToLiveSeconds(1234L);
        Assert.assertEquals(1234, cache.getCacheConfiguration().getTimeToLiveSeconds());

        cache.getCacheConfiguration().setEternal(true);
        Assert.assertEquals(0, cache.getCacheConfiguration().getTimeToLiveSeconds());
        cache.getCacheConfiguration().setTimeToLiveSeconds(1234L);
        Assert.assertEquals(0, cache.getCacheConfiguration().getTimeToLiveSeconds());

        cache.getCacheConfiguration().setEternal(false);
        cache.getCacheConfiguration().setTimeToLiveSeconds(1234L);
        Assert.assertEquals(1234, cache.getCacheConfiguration().getTimeToLiveSeconds());
    }

    @Test
    public void testTTI() {
        Cache cache = cacheManager.getCache(TEST_NAME);
        Assert.assertEquals(false, cache.getCacheConfiguration().isEternal());
        Assert.assertEquals(0, cache.getCacheConfiguration().getTimeToIdleSeconds());
        try {
            cache.getCacheConfiguration().setTimeToIdleSeconds(-1234L);
            Assert.fail("should not be able to set negative TTI");
        } catch (IllegalArgumentException e) {
            // expected exception;
            Assert.assertEquals("Illegal value -1234 for timeToIdleSeconds: has to be larger than or equal to 0", e.getMessage());
        }

        Assert.assertEquals(0, cache.getCacheConfiguration().getTimeToIdleSeconds());
        cache.getCacheConfiguration().setTimeToIdleSeconds(1234L);
        Assert.assertEquals(1234, cache.getCacheConfiguration().getTimeToIdleSeconds());

        cache.getCacheConfiguration().setEternal(true);
        Assert.assertEquals(0, cache.getCacheConfiguration().getTimeToIdleSeconds());
        cache.getCacheConfiguration().setTimeToIdleSeconds(1234L);
        Assert.assertEquals(0, cache.getCacheConfiguration().getTimeToIdleSeconds());
    }
}

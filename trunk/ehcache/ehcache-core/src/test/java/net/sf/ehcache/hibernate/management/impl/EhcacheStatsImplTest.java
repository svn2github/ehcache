package net.sf.ehcache.hibernate.management.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import net.sf.ehcache.CacheManager;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Alex Snaps
 */
public class EhcacheStatsImplTest {

    private static EhcacheStatsImpl stats;

    @BeforeClass
    public static void createCache() throws Exception {
        CacheManager manager = CacheManager.getInstance();
        stats = new EhcacheStatsImpl(manager);
    }

    @AfterClass
    public static void destroyCache() throws Exception {
      CacheManager manager = CacheManager.getInstance();
      manager.shutdown();
    }

    @Test
    public void testIsRegionCacheOrphanEvictionEnabled() {
        assertThat(stats.isRegionCacheOrphanEvictionEnabled("sampleCache1"), is(false));
    }

    @Test
    public void testGetRegionCacheOrphanEvictionPeriod() {
        assertThat(stats.getRegionCacheOrphanEvictionPeriod("sampleCache1"), is(-1));
    }
}

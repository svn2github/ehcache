package net.sf.ehcache.config;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.PersistenceConfiguration.Strategy;

import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Alex Snaps
 */
public class CacheConfigurationTest {

    private CacheManager cacheManager;

    @Before
    public void setup() {
        Configuration configTestCM = new Configuration().name("configTestCM")
                        .diskStore(new DiskStoreConfiguration().path("java.io.tmpdir"));
        this.cacheManager = CacheManager.newInstance(configTestCM);
    }

    @Test
    public void testTransactionalMode() {
        CacheConfiguration configuration = new CacheConfiguration();
        assertEquals(CacheConfiguration.TransactionalMode.OFF, configuration.getTransactionalMode());
        try {
            configuration.setTransactionalMode(null);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }

        configuration.setTransactionalMode("local");
        assertEquals(CacheConfiguration.TransactionalMode.LOCAL, configuration.getTransactionalMode());
        try {
            configuration.transactionalMode(CacheConfiguration.TransactionalMode.OFF);
            fail("expected InvalidConfigurationException");
        } catch (InvalidConfigurationException e) {
            // expected
        }
        try {
            configuration.setTransactionalMode(null);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }

        CacheConfiguration clone = configuration.clone();
        assertEquals(CacheConfiguration.TransactionalMode.LOCAL, clone.getTransactionalMode());
        try {
            clone.transactionalMode(CacheConfiguration.TransactionalMode.XA);
            fail("expected InvalidConfigurationException");
        } catch (InvalidConfigurationException e) {
            // expected
        }
    }

    @Test
    public void testReadPercentageProperly() {
        CacheConfiguration configuration = new CacheConfiguration();
        assertThat(configuration.getMaxBytesLocalOffHeapPercentage(), nullValue());
        configuration.setMaxBytesLocalOffHeap("12%");
        assertThat(configuration.getMaxBytesLocalOffHeapPercentage(), equalTo(12));
        configuration.setMaxBytesLocalOffHeap("99%");
        assertThat(configuration.getMaxBytesLocalOffHeapPercentage(), equalTo(99));
        configuration.setMaxBytesLocalOffHeap("100%");
        assertThat(configuration.getMaxBytesLocalOffHeapPercentage(), equalTo(100));
        configuration.setMaxBytesLocalOffHeap("0%");
        assertThat(configuration.getMaxBytesLocalOffHeapPercentage(), equalTo(0));
        try {
            configuration.setMaxBytesLocalOffHeap("101%");
            fail("This should throw an IllegalArgumentException, 101% is above 100%");
        } catch (IllegalArgumentException e) {
            // Expected
        }
        try {
            configuration.setMaxBytesLocalOffHeap("-10%");
            fail("This should throw an IllegalArgumentException, -10% is below 0%");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testCanSetBothMaxWhenCacheNotRunning() {
        CacheConfiguration configuration = new CacheConfiguration();
        try {
            configuration.setMaxEntriesLocalHeap(10);
            configuration.maxBytesLocalHeap(10, MemoryUnit.MEGABYTES);
            configuration.setMaxEntriesLocalDisk(10);
            configuration.maxBytesLocalDisk(10, MemoryUnit.MEGABYTES);
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
            fail("Shouldn't have thrown an exception");
        }
    }

    @Test
    public void testMaxEntriesLocalDiskAndMaxElementsOnDiskAlias() {
        CacheConfiguration configuration = new CacheConfiguration().maxElementsOnDisk(10);
        assertThat(configuration.getMaxEntriesLocalDisk(), is(10L));
        assertThat(configuration.getMaxElementsOnDisk(), is(10));
        configuration.maxEntriesLocalDisk(20);
        assertThat(configuration.getMaxEntriesLocalDisk(), is(20L));
        assertThat(configuration.getMaxElementsOnDisk(), is(20));
    }

    @Test
    public void testCantSetMaxEntriesLocalDiskWhenClustered() {
        CacheConfiguration configuration = new CacheConfiguration("Test", 10)
            .maxEntriesLocalDisk(10).terracotta(new TerracottaConfiguration());
        try {
            cacheManager.addCache(new Cache(configuration));
            fail("This should throw InvalidConfigurationException");
        } catch (CacheException e) {
            assertThat(e.getMessage().contains("use maxElementsOnDisk instead"), is(true));
        }
    }

    @Test
    public void testSynchronousWritesPersistenceConfiguration() {
        CacheConfiguration configuration = new CacheConfiguration("Test", 10).persistence(new PersistenceConfiguration()
                .strategy(Strategy.LOCALTEMPSWAP).synchronousWrites(true));
        try {
            cacheManager.addCache(new Cache(configuration));
            fail("Expected InvalidConfigurationException");
        } catch (InvalidConfigurationException e) {
            assertThat(e.getMessage(), containsString("synchronousWrites"));
        } finally {
            cacheManager.removeCache("Test");
        }

        configuration = new CacheConfiguration("Test", 10).persistence(new PersistenceConfiguration()
                .strategy(Strategy.NONE).synchronousWrites(true));
        try {
            cacheManager.addCache(new Cache(configuration));
            fail("Expected InvalidConfigurationException");
        } catch (InvalidConfigurationException e) {
            assertThat(e.getMessage(), containsString("synchronousWrites"));
        } finally {
            cacheManager.removeCache("Test");
        }

        configuration = new CacheConfiguration("Test", 10).persistence(new PersistenceConfiguration()
                .strategy(Strategy.LOCALRESTARTABLE).synchronousWrites(true));
        try {
            cacheManager.addCache(new Cache(configuration));
            fail("Expected CacheException");
        } catch (CacheException e) {
            assertThat(e.getMessage(), containsString("enterprise"));
        } finally {
            cacheManager.removeCache("Test");
        }
    }

    @Test
    public void testPersistenceConfigMixing() {
        CacheConfiguration persistence = new CacheConfiguration().persistence(new PersistenceConfiguration().strategy(Strategy.LOCALTEMPSWAP));
        try {
            persistence.diskPersistent(true);
        } catch (InvalidConfigurationException e) {
            Assert.assertThat(e.getMessage(), StringContains.containsString("<persistence ...> and diskPersistent"));
        }
        try {
            persistence.overflowToDisk(true);
        } catch (InvalidConfigurationException e) {
            Assert.assertThat(e.getMessage(), StringContains.containsString("<persistence ...> and overflowToDisk"));
        }

        CacheConfiguration diskPersistent = new CacheConfiguration().diskPersistent(true);
        try {
            diskPersistent.persistence(new PersistenceConfiguration().strategy(Strategy.LOCALTEMPSWAP));
        } catch (InvalidConfigurationException e) {
            Assert.assertThat(e.getMessage(), StringContains.containsString("<persistence ...> and diskPersistent"));
        }

        CacheConfiguration overflowToDisk = new CacheConfiguration().overflowToDisk(true);
        try {
            overflowToDisk.persistence(new PersistenceConfiguration().strategy(Strategy.LOCALTEMPSWAP));
        } catch (InvalidConfigurationException e) {
            Assert.assertThat(e.getMessage(), StringContains.containsString("<persistence ...> and overflowToDisk"));
        }
    }

    @Test
    public void testNoPersistenceStrategySet() {
        CacheConfiguration config = new CacheConfiguration().name("foo").persistence(new PersistenceConfiguration()).maxBytesLocalHeap(1, MemoryUnit.MEGABYTES);
        try {
            cacheManager.addCache(new Cache(config));
        } catch (InvalidConfigurationException e) {
            Assert.assertThat(e.getMessage(), StringContains.containsString("Persistence configuration found with no strategy set."));
        }
    }
}

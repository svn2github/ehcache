/**
 *  Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;
import net.sf.ehcache.bootstrap.BootstrapCacheLoader;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.config.InvalidConfigurationException;
import net.sf.ehcache.config.MemoryUnit;
import net.sf.ehcache.constructs.blocking.BlockingCache;
import net.sf.ehcache.constructs.blocking.CountingCacheEntryFactory;
import net.sf.ehcache.constructs.blocking.SelfPopulatingCache;
import net.sf.ehcache.distribution.AbstractRMITest;
import net.sf.ehcache.distribution.JVMUtil;
import net.sf.ehcache.distribution.RMIAsynchronousCacheReplicator;
import net.sf.ehcache.distribution.RMIBootstrapCacheLoader;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.statistics.LiveCacheStatisticsData;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.store.disk.DiskStore;
import net.sf.ehcache.util.MemorySizeParser;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for CacheManager
 *
 * @author Greg Luck
 * @version $Id$
 */
public class CacheManagerTest {

    private static final Logger LOG = LoggerFactory.getLogger(CacheManagerTest.class.getName());
    private static final int CACHES_IN_EHCACHE_XML = 15;

    /**
     * the CacheManager Singleton instance
     */
    protected CacheManager singletonManager;

    /**
     * a CacheManager which is created as an instance
     */
    protected CacheManager instanceManager;

    @BeforeClass
    public static void installRMISocketFactory() {
        AbstractRMITest.installRMISocketFactory();
    }

    /**
     * Shutdown managers. Check that the manager is removed from
     * CacheManager.ALL_CACHE_MANAGERS
     */
    @After
    public void tearDown() throws Exception {
        if (singletonManager != null) {
            if (singletonManager.getStatus().equals(Status.STATUS_ALIVE)) {
                assertTrue(CacheManager.ALL_CACHE_MANAGERS.contains(singletonManager));
            }
            singletonManager.shutdown();
            assertFalse(CacheManager.ALL_CACHE_MANAGERS.contains(singletonManager));
        }
        if (instanceManager != null) {
            if (instanceManager.getStatus().equals(Status.STATUS_ALIVE)) {
                assertTrue(CacheManager.ALL_CACHE_MANAGERS.contains(instanceManager));
            }
            instanceManager.shutdown();
            assertFalse(CacheManager.ALL_CACHE_MANAGERS.contains(instanceManager));
        }
    }

    @Test(expected = InvalidConfigurationException.class)
    public void testCantMixCountAndSizeBasedTunings() {
        Configuration configuration = new Configuration().maxBytesLocalHeap(16, MemoryUnit.MEGABYTES);
        CacheManager cacheManager = new CacheManager(configuration);
        try {
            cacheManager.addCache(new Cache(new CacheConfiguration("zero", 10)));
        } finally {
            cacheManager.shutdown();
        }
    }

    @Test
    public void testReduceCacheManagerPoolBelowReservedUsage() {
        Configuration configuration = new Configuration()
            .maxBytesLocalHeap(16, MemoryUnit.MEGABYTES);
        CacheManager cacheManager = new CacheManager(configuration);
        cacheManager.addCache(new Cache(new CacheConfiguration("zero", 0)));
        cacheManager.addCache(new Cache(new CacheConfiguration("one", 0).maxBytesLocalHeap(2, MemoryUnit.MEGABYTES)));
        cacheManager.addCache(new Cache(new CacheConfiguration("two", 0).maxBytesLocalHeap(2, MemoryUnit.MEGABYTES)));
        assertThat(cacheManager.getOnHeapPool().getMaxSize(), is(MemoryUnit.MEGABYTES.toBytes(12)));
        cacheManager.getConfiguration().maxBytesLocalHeap(8, MemoryUnit.MEGABYTES);
        assertThat(cacheManager.getOnHeapPool().getMaxSize(), is(MemoryUnit.MEGABYTES.toBytes(4)));
        cacheManager.getConfiguration().maxBytesLocalHeap(4, MemoryUnit.MEGABYTES);
        assertThat(cacheManager.getOnHeapPool().getMaxSize(), is(MemoryUnit.MEGABYTES.toBytes(0)));
        try {
            cacheManager.getConfiguration().maxBytesLocalHeap(3, MemoryUnit.MEGABYTES);
            fail();
        } catch (InvalidConfigurationException e) {
            assertThat(e.getMessage().contains("one"), is(true));
            assertThat(e.getMessage().contains("two"), is(true));
            assertThat(e.getMessage().contains("zero"), is(false));
        }
        cacheManager.removeCache("one");
        assertThat(cacheManager.getOnHeapPool().getMaxSize(), is(MemoryUnit.MEGABYTES.toBytes(2)));
        CacheConfiguration two = cacheManager.getCache("two").getCacheConfiguration();
        two.maxBytesLocalHeap(1, MemoryUnit.MEGABYTES);
        assertThat(cacheManager.getOnHeapPool().getMaxSize(), is(MemoryUnit.MEGABYTES.toBytes(3)));
        cacheManager.removeCache("two");
        assertThat(cacheManager.getOnHeapPool().getMaxSize(), is(MemoryUnit.MEGABYTES.toBytes(4)));
        two.maxBytesLocalHeap(2, MemoryUnit.MEGABYTES);
        assertThat(cacheManager.getOnHeapPool().getMaxSize(), is(MemoryUnit.MEGABYTES.toBytes(4)));

        cacheManager.shutdown();
    }

    @Test
    public void testCantAddCacheWhenOverAllocatingCacheManagerPool() {
        Configuration configuration = new Configuration()
            .maxBytesLocalHeap(5, MemoryUnit.MEGABYTES);
        CacheManager cacheManager = new CacheManager(configuration);
        cacheManager.addCache(new Cache(new CacheConfiguration("one", 0).maxBytesLocalHeap(2, MemoryUnit.MEGABYTES)));
        assertThat(cacheManager.getOnHeapPool().getMaxSize(), is(MemoryUnit.MEGABYTES.toBytes(3)));
        cacheManager.addCache(new Cache(new CacheConfiguration("two", 0).maxBytesLocalHeap(2, MemoryUnit.MEGABYTES)));
        assertThat(cacheManager.getOnHeapPool().getMaxSize(), is(MemoryUnit.MEGABYTES.toBytes(1)));
        try {
            cacheManager.addCache(new Cache(new CacheConfiguration("three", 0).maxBytesLocalHeap(2, MemoryUnit.MEGABYTES)));
        } catch (InvalidConfigurationException e) {
            assertThat(e.getMessage().contains("'three'"), is(true));
            assertThat(e.getMessage().contains("over-allocate"), is(true));
        }
        assertThat(cacheManager.getCache("three"), nullValue());
        assertThat(cacheManager.getConfiguration().getCacheConfigurations().get("three"), nullValue());
        assertThat(cacheManager.getConfiguration().getCacheConfigurations().size(), is(2));
        assertThat(cacheManager.getCacheNames().length, is(2));
        cacheManager.shutdown();
    }

    // todo This should be addressed at some point: we're cloning things around too much...
    @Ignore
    @Test
    public void testCacheConfigurationAreInSync() {
        Configuration configuration = new Configuration().cache(new CacheConfiguration("one", 0));
        CacheManager cacheManager = new CacheManager(configuration);
        assertThat(cacheManager.getCache("one").getCacheConfiguration(),
                is(cacheManager.getConfiguration().getCacheConfigurations().get("one")));
    }

    @Test
    public void testMaxBytesOnCacheDynamicChangesReflectOnPercentBasedCaches() throws Exception {
        CacheConfiguration configuration1 = new CacheConfiguration("one", 0);
        CacheConfiguration configuration2 = new CacheConfiguration("two", 0);
        Configuration configuration = new Configuration()
            .maxBytesLocalHeap(5, MemoryUnit.MEGABYTES)
            .cache(configuration1)
            .cache(configuration2)
            .cache(new CacheConfiguration("three", 0));
        configuration1.setMaxBytesLocalHeap("20%");
        configuration2.setMaxBytesLocalHeap("20%");

        CacheManager cacheManager = new CacheManager(configuration);
        assertThat(cacheManager.getCache("one").getCacheConfiguration().getMaxBytesLocalHeap(), equalTo(MemoryUnit.MEGABYTES.toBytes(1)));
        assertThat(cacheManager.getCache("two").getCacheConfiguration().getMaxBytesLocalHeap(), equalTo(MemoryUnit.MEGABYTES.toBytes(1)));
        assertThat(cacheManager.getCache("three").getCacheConfiguration().getMaxBytesLocalHeap(), equalTo(0L));

        configuration.maxBytesLocalHeap(10, MemoryUnit.MEGABYTES);

        assertThat(cacheManager.getCache("one").getCacheConfiguration().getMaxBytesLocalHeap(), equalTo(MemoryUnit.MEGABYTES.toBytes(2)));
        assertThat(cacheManager.getCache("two").getCacheConfiguration().getMaxBytesLocalHeap(), equalTo(MemoryUnit.MEGABYTES.toBytes(2)));
        assertThat(cacheManager.getCache("three").getCacheConfiguration().getMaxBytesLocalHeap(), equalTo(0L));

        cacheManager.shutdown();
    }

    @Test
    public void testMaxBytesOnCacheConfiguration() throws Exception {
        CacheConfiguration configuration1 = new CacheConfiguration("one", 0);
        CacheConfiguration configuration2 = new CacheConfiguration("two", 0);
        Configuration configuration = new Configuration()
            .maxBytesLocalHeap(5, MemoryUnit.MEGABYTES)
            .cache(configuration1)
            .cache(configuration2)
            .cache(new CacheConfiguration("three", 0));
        configuration1.setMaxBytesLocalHeap("20%");
        configuration2.setMaxBytesLocalHeap("20%");

        CacheManager cacheManager = new CacheManager(configuration);
        assertThat(cacheManager.getCache("one").getCacheConfiguration().getMaxBytesLocalHeap(), equalTo(MemoryUnit.MEGABYTES.toBytes(1)));
        assertThat(cacheManager.getCache("two").getCacheConfiguration().getMaxBytesLocalHeap(), equalTo(MemoryUnit.MEGABYTES.toBytes(1)));
        assertThat(cacheManager.getCache("three").getCacheConfiguration().getMaxBytesLocalHeap(), equalTo(0L));

        configuration1 = new CacheConfiguration("one", 0);
        configuration2 = new CacheConfiguration("two", 0);
        configuration = new Configuration()
            .cache(configuration1)
            .cache(configuration2);
        configuration1.setMaxBytesLocalHeap("2048");
        configuration2.setMaxBytesLocalHeap("20%");
        cacheManager.shutdown();

        try {
            new CacheManager(configuration);
            fail("This should have thrown an InvalidConfigurationException");
        } catch (InvalidConfigurationException e) {
            assertThat(e.getMessage().contains("percentage maxBytesOnHeap"), is(true));
            assertThat(e.getMessage().contains("no CacheManager wide value"), is(true));
            assertThat(e.getMessage().contains("two"), is(true));
        }
    }

    @Test
    public void testDynamicallyAddedCacheConfiguration() {
        Configuration configuration = new Configuration();
        configuration.addCache(new CacheConfiguration("before1",100));
        configuration.addCache(new CacheConfiguration("before2",100));
        CacheManager manager = new CacheManager(configuration);
        assertThat(manager.getCache("before1"), notNullValue());
        assertThat(manager.getCache("before2"), notNullValue());
        try {
            configuration.addCache(new CacheConfiguration("after", 100));
            fail("Should have had a IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(manager.getCache("after"), nullValue());
        }
        manager.shutdown();
    }

    @Test
    public void testSupportsNameChanges() {
        Configuration configuration = new Configuration().name("firstName");
        CacheManager manager = new CacheManager(configuration);
        assertThat(manager.getName(), equalTo("firstName"));

        manager.setName("newerName");
        assertThat(configuration.getName(), equalTo("newerName"));
        assertThat(manager.getName(), equalTo("newerName"));

        configuration.setName("evenNewerName");
        assertThat(configuration.getName(), equalTo("evenNewerName"));
        assertThat(manager.getName(), equalTo("evenNewerName"));
        manager.shutdown();
    }

    @Test
    public void testMaxBytesOverAllocated() {

        Configuration configuration = new Configuration()
            .maxBytesLocalHeap(50, MemoryUnit.KILOBYTES)
            .cache(new CacheConfiguration("one", 0).maxBytesLocalHeap(30, MemoryUnit.KILOBYTES))
            .cache(new CacheConfiguration("two", 0).maxBytesLocalHeap(30, MemoryUnit.KILOBYTES));

        try {
            new CacheManager(configuration);
            fail("This should have thrown an InvalidConfigurationException");
        } catch (InvalidConfigurationException e) {
            assertThat(e.getMessage().contains("over-allocate"), is(true));
        }

        CacheConfiguration configuration1 = new CacheConfiguration("one", 0);
        CacheConfiguration configuration2 = new CacheConfiguration("two", 0);
        CacheConfiguration configuration3 = new CacheConfiguration("three", 0);
        CacheConfiguration configuration4 = new CacheConfiguration("four", 0);
        configuration = new Configuration()
            .maxBytesLocalHeap(30, MemoryUnit.KILOBYTES)
            .cache(configuration1)
            .cache(configuration2)
            .cache(configuration3)
            .cache(configuration4);

        configuration1.setMaxBytesLocalHeap("30%");
        configuration2.setMaxBytesLocalHeap("30%");
        configuration3.setMaxBytesLocalHeap("30%");
        configuration4.setMaxBytesLocalHeap("30%");

        try {
            new CacheManager(configuration);
            fail("This should have thrown an InvalidConfigurationException");
        } catch (InvalidConfigurationException e) {
            assertThat(e.getMessage().contains("over-allocate"), is(true));
        }

        configuration = new Configuration()
            .maxBytesLocalHeap(4096, MemoryUnit.GIGABYTES)
            .cache(new CacheConfiguration("one", 0).maxBytesLocalHeap(30, MemoryUnit.KILOBYTES))
            .cache(new CacheConfiguration("two", 0).maxBytesLocalHeap(30, MemoryUnit.KILOBYTES));

        try {
            new CacheManager(configuration);
            fail("This should have thrown an InvalidConfigurationException");
        } catch (InvalidConfigurationException e) {
            assertThat(e.getMessage().contains("-Xmx"), is(true));
        }

        configuration = new Configuration()
            .cache(new CacheConfiguration("one", 0).maxBytesLocalHeap(2048, MemoryUnit.GIGABYTES))
            .cache(new CacheConfiguration("two", 0).maxBytesLocalHeap(2048, MemoryUnit.GIGABYTES));

        try {
            new CacheManager(configuration);
            fail("This should have thrown an InvalidConfigurationException");
        } catch (InvalidConfigurationException e) {
            assertThat(e.getMessage().contains("-Xmx"), is(true));
        }

        configuration1 = new CacheConfiguration("one", 0);
        configuration2 = new CacheConfiguration("two", 0);
        configuration3 = new CacheConfiguration("three", 0);
        configuration4 = new CacheConfiguration("four", 0);
        configuration = new Configuration()
            .maxBytesLocalHeap(30, MemoryUnit.KILOBYTES)
            .cache(configuration1)
            .cache(configuration2)
            .cache(configuration3);

        configuration1.setMaxBytesLocalHeap("30%");
        configuration2.setMaxBytesLocalHeap("30%");
        configuration3.setMaxBytesLocalHeap("30%");
        configuration4.setMaxBytesLocalHeap("30%");

        CacheManager cacheManager = new CacheManager(configuration);
        try {
            cacheManager.addCache(new Cache(configuration4));
            fail("This should have thrown an InvalidConfigurationException");
        } catch (InvalidConfigurationException e) {
            assertThat(e.getMessage().contains("over-allocate"), is(true));
            assertThat(e.getMessage().contains("'four'"), is(true));
        }
        cacheManager.shutdown();
    }

    @Test
    public void testPoolSize() throws Exception {
        Configuration configuration = new Configuration()
            .diskStore(new DiskStoreConfiguration().path("java.io.tmpdir/tmp"))
            .maxBytesLocalHeap(50, MemoryUnit.MEGABYTES)
            .maxBytesLocalDisk(500, MemoryUnit.MEGABYTES)
            .cache(new CacheConfiguration("one", 0).maxBytesLocalHeap(10, MemoryUnit.MEGABYTES))
            .cache(new CacheConfiguration("two", 0).maxBytesLocalHeap(10, MemoryUnit.MEGABYTES))
            .cache(new CacheConfiguration("three", 0).maxBytesLocalDisk(100, MemoryUnit.MEGABYTES));

        CacheManager cacheManager = new CacheManager(configuration);
        assertEquals(MemorySizeParser.parse("30M"), cacheManager.getOnHeapPool().getMaxSize());
        assertEquals(MemorySizeParser.parse("400M"), cacheManager.getOnDiskPool().getMaxSize());

        cacheManager.addCache(new Cache(new CacheConfiguration("four", 0)
                .maxBytesLocalHeap(10, MemoryUnit.MEGABYTES)
                .maxBytesLocalDisk(150, MemoryUnit.MEGABYTES)));
        assertEquals(MemorySizeParser.parse("20M"), cacheManager.getOnHeapPool().getMaxSize());
        assertEquals(MemorySizeParser.parse("250M"), cacheManager.getOnDiskPool().getMaxSize());

        cacheManager.removeCache("one");
        assertEquals(MemorySizeParser.parse("30M"), cacheManager.getOnHeapPool().getMaxSize());
        assertEquals(MemorySizeParser.parse("250M"), cacheManager.getOnDiskPool().getMaxSize());

        cacheManager.removeCache("three");
        assertEquals(MemorySizeParser.parse("30M"), cacheManager.getOnHeapPool().getMaxSize());
        assertEquals(MemorySizeParser.parse("350M"), cacheManager.getOnDiskPool().getMaxSize());
    }

    @Test
    public void testCacheReferenceLookUps() {
        singletonManager = CacheManager.create();
        String cacheName = "randomNewCache";
        singletonManager.addCache(new Cache(new CacheConfiguration().name(cacheName)));

        // Default state by name
        Cache cache = singletonManager.getCache(cacheName);
        assertNotNull(cache);
        assertNotNull(singletonManager.getEhcache(cacheName));
        assertTrue(singletonManager.getEhcache(cacheName) instanceof Cache);
        assertTrue(cache == singletonManager.getEhcache(cacheName));

        // replace cache
        BlockingCache decoratedCache = new BlockingCache(cache);
        singletonManager.replaceCacheWithDecoratedCache(cache, decoratedCache);
        assertNull(singletonManager.getCache(cacheName));
        assertNotNull(singletonManager.getEhcache(cacheName));
        assertTrue(singletonManager.getEhcache(cacheName) == decoratedCache);
    }

    @Test
    public void testProgrammaticConfigurationFailsProperlyWhenNoDefaultCacheConfigured() {
        Configuration mgrConfig = new Configuration();
        mgrConfig.setUpdateCheck(false);
        CacheManager cacheManager = null;
        try {
            cacheManager = new CacheManager(mgrConfig);
            Assert.assertNotNull(cacheManager);
        } catch (Exception e) {
            fail("Creating cache manager having no default cache config shouldn't fail!");
        }
        cacheManager.shutdown();
    }

    /**
     * Tests that the CacheManager was successfully created
     */
    @Test
    public void testCreateCacheManager() throws CacheException {
        singletonManager = CacheManager.create();
        singletonManager.getEhcache("");
        assertNotNull(singletonManager);
        assertEquals(CACHES_IN_EHCACHE_XML, singletonManager.getCacheNames().length);
    }

    /**
     * Tests that the CacheManager was successfully created
     */
    @Test
    public void testCreateCacheManagerFromFile() throws CacheException {
        singletonManager = CacheManager.create(AbstractCacheTest.SRC_CONFIG_DIR + "ehcache.xml");
        assertNotNull(singletonManager);
        assertEquals(6, singletonManager.getCacheNames().length);
    }

    /**
     * Tests that the CacheManager was successfully created from a Configuration
     */
    @Test
    public void testCreateCacheManagerFromConfiguration() throws CacheException {
        File file = new File(AbstractCacheTest.SRC_CONFIG_DIR + "ehcache.xml");
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        CacheManager manager = new CacheManager(configuration);
        assertNotNull(manager);
        assertEquals(6, manager.getCacheNames().length);
        manager.shutdown();
    }

    /**
     * Tests that the CacheManager was successfully created
     */
    @Test
    public void testCreateCacheManagerFromInputStream() throws Exception {
        InputStream fis = new FileInputStream(new File(
                AbstractCacheTest.SRC_CONFIG_DIR + "ehcache.xml")
                .getAbsolutePath());
        try {
            singletonManager = CacheManager.create(fis);
        } finally {
            fis.close();
        }
        assertNotNull(singletonManager);
        assertEquals(6, singletonManager.getCacheNames().length);
    }

    /**
     * Tests that creating a second cache manager with the same disk path will
     * fail.
     */
    @Test
    public void testCreateTwoCacheManagersWithSamePath() throws CacheException {
        URL configUrl = this.getClass().getResource("/ehcache-2.xml");

        CacheManager managerOne = CacheManager.create(configUrl);
        assertFalse(managerOne.getDiskStorePathManager().isAutoCreated());
        try {
            Configuration secondCacheConfiguration = ConfigurationFactory.parseConfiguration(configUrl).name("some-name");
            CacheManager managerTwo = new CacheManager(secondCacheConfiguration);
            try {
                assertTrue(managerTwo.getDiskStorePathManager().isAutoCreated());
            } finally {
                managerTwo.shutdown();
            }
        } finally {
	    managerOne.shutdown();
	}
    }

    /**
     * Tests that two CacheManagers were successfully created
     */
    @Test
    public void testTwoCacheManagers() throws CacheException {
        Element element1 = new Element(1 + "", new Date());
        Element element2 = new Element(2 + "", new Date());

        CacheManager.getInstance().getCache("sampleCache1").put(element1);

        // Check can start second one with a different disk path
        URL configUrl = this.getClass().getResource(
                "/ehcache-2.xml");
        Configuration secondCacheConfiguration = ConfigurationFactory.parseConfiguration(configUrl).name("cm-2");
        instanceManager = new CacheManager(secondCacheConfiguration);
        instanceManager.getCache("sampleCache1").put(element2);

        assertEquals(element1, CacheManager.getInstance().getCache(
                "sampleCache1").get(1 + ""));
        assertEquals(element2, instanceManager.getCache("sampleCache1").get(
                2 + ""));

        // shutting down instance should leave singleton unaffected
        instanceManager.shutdown();
        assertEquals(element1, CacheManager.getInstance().getCache(
                "sampleCache1").get(1 + ""));

        // Try shutting and recreating a new instance cache manager
        instanceManager = new CacheManager(secondCacheConfiguration);
        instanceManager.getCache("sampleCache1").put(element2);
        CacheManager.getInstance().shutdown();
        assertEquals(element2, instanceManager.getCache("sampleCache1").get(
                2 + ""));

        // Try shutting and recreating the singleton cache manager
        CacheManager.getInstance().getCache("sampleCache1").put(element2);
        assertNull(CacheManager.getInstance().getCache("sampleCache1").get(
                1 + ""));
        assertEquals(element2, CacheManager.getInstance().getCache(
                "sampleCache1").get(2 + ""));

        CacheManager.getInstance().shutdown();
        instanceManager.shutdown();
    }

    /**
     * Tests that two CacheManagers were successfully created
     */
    @Test
    public void testTwoCacheManagersWithSameConfiguration()
            throws CacheException {
        Element element1 = new Element(1 + "", new Date());
        Element element2 = new Element(2 + "", new Date());

        String fileName = AbstractCacheTest.TEST_CONFIG_DIR + "ehcache.xml";
        CacheManager.create(fileName).getCache("sampleCache1").put(element1);

        Configuration secondConfig = ConfigurationFactory.parseConfiguration(new File(fileName)).name("cm-2");
        // Check can start second one with the same config
        instanceManager = new CacheManager(secondConfig);
        instanceManager.getCache("sampleCache1").put(element2);

        assertEquals(element1, CacheManager.getInstance().getCache(
                "sampleCache1").get(1 + ""));
        assertEquals(element2, instanceManager.getCache("sampleCache1").get(
                2 + ""));

        // shutting down instance should leave singleton unaffected
        instanceManager.shutdown();
        assertEquals(element1, CacheManager.getInstance().getCache(
                "sampleCache1").get(1 + ""));

        // Try shutting and recreating a new instance cache manager
        instanceManager = new CacheManager(secondConfig);
        instanceManager.getCache("sampleCache1").put(element2);
        CacheManager.getInstance().shutdown();
        assertEquals(element2, instanceManager.getCache("sampleCache1").get(
                2 + ""));

        // Try shutting and recreating the singleton cache manager
        CacheManager.getInstance().getCache("sampleCache1").put(element2);
        assertNull(CacheManager.getInstance().getCache("sampleCache1").get(
                1 + ""));
        assertEquals(element2, CacheManager.getInstance().getCache(
                "sampleCache1").get(2 + ""));

        CacheManager.getInstance().shutdown();
        instanceManager.shutdown();
    }

    /**
     * Create and destory cache managers and see what happens with threads. Each
     * Cache creates at least two threads. These should all be killed when the
     * Cache disposes. Doing that 800 times as in that test gives the
     * reassurance.
     */
    @Test
    public void testForCacheManagerThreadLeak() throws CacheException,
            InterruptedException {
        // Check can start second one with a different disk path
        int startingThreadCount = countThreads();

        URL secondCacheConfiguration = this.getClass().getResource(
                "/ehcache-2.xml");
        for (int i = 0; i < 100; i++) {
            instanceManager = new CacheManager(secondCacheConfiguration);
            instanceManager.shutdown();
        }
        int endingThreadCount;
        int tries = 0;
        // Give the spools a chance to exit
        do {
            Thread.sleep(500);
            endingThreadCount = countThreads();
        } while (tries++ < 5 || endingThreadCount >= startingThreadCount + 2);

        // Allow a bit of variation.
        assertTrue(endingThreadCount < startingThreadCount + 2);

    }

    /**
     * The expiry threads and spool threads share are now combined. This should
     * save some.
     * <p/>
     * ehcache-big.xml has 70 caches that overflow to disk. Check that the
     * DiskStore is not using more than 1 thread per DiskStore.
     * <p/>
     * ehcache-1.2.3 had 126 threads for this test. ehcache-1.2.4 has 71. 70 for
     * the DiskStore thread and one shutdown hook
     * <p/>
     * ehcache-1.7 has 1 additional thread per cache for
     * SampledCacheUsageStatistics. 70 Caches means 140 threads plus 1 for
     * shutdown totalling to 141. Plus Junit thread totals 142.
     */
    @Test
    public void testCacheManagerThreads() throws CacheException,
            InterruptedException {
        singletonManager = CacheManager
                .create(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-big.xml");
        int threads = countThreads();
        assertTrue("More than 145 threads: " + threads, countThreads() <= 145);
    }

    /**
     * It should be possible to create a new CacheManager instance with the same
     * disk configuration, provided the first was shutdown. Note that any
     * persistent disk stores will be available to the second cache manager.
     */
    @Test
    public void testInstanceCreateShutdownCreate() throws CacheException {
        singletonManager = CacheManager.create();

        URL configUrl = this.getClass().getResource(
                "/ehcache-2.xml");
        Configuration secondCacheConfiguration = ConfigurationFactory.parseConfiguration(configUrl).name("cm-2");
        instanceManager = new CacheManager(secondCacheConfiguration);
        instanceManager.shutdown();

        // shutting down instance should leave singleton ok
        assertEquals(CACHES_IN_EHCACHE_XML, singletonManager.getCacheNames().length);

        instanceManager = new CacheManager(secondCacheConfiguration);
        assertNotNull(instanceManager);
        assertEquals(8, instanceManager.getCacheNames().length);
        instanceManager.shutdown();
    }

    /**
     * Tests programmatic creation of CacheManager with a programmatic
     * Configuration.
     * <p/>
     * Tests:
     * <ol>
     * <li>adding a cache by name, which will use default cache
     * <li>adding a Cache object
     * <li>setting the DiskStore directory path
     * </ol>
     *
     * @throws CacheException
     */
    @Test
    public void testCreateCacheManagersProgrammatically() throws CacheException {

        Configuration configuration = new Configuration()
                .defaultCache(new CacheConfiguration("defaultCache", 10))
                .diskStore(new DiskStoreConfiguration().path("java.io.tmpdir"));
        assertNotNull(configuration);

        instanceManager = new CacheManager(configuration);
        assertNotNull(instanceManager);
        assertEquals(0, instanceManager.getCacheNames().length);

        instanceManager.addCache("toBeDerivedFromDefaultCache");
        Cache cache = new Cache("testCache", 1, true, false, 5, 2);
        instanceManager.addCache(cache);

        assertEquals(2, instanceManager.getCacheNames().length);

    }

    /**
     * Checks we can get a cache
     */
    @Test
    public void testGetCache() throws CacheException {
        instanceManager = CacheManager.create();
        Ehcache cache = instanceManager.getCache("sampleCache1");
        assertNotNull(cache);
    }

    /**
     * Does the cache hang on to its instance?
     */
    @Test
    public void testCacheManagerReferenceInstance() {
        instanceManager = new CacheManager();
        instanceManager.addCache("test");
        Ehcache cache = instanceManager.getCache("test");
        assertEquals("test", cache.getName());
        assertEquals(Status.STATUS_ALIVE, cache.getStatus());
        CacheManager reference = cache.getCacheManager();
        assertTrue(reference == instanceManager);
    }

    /**
     * Does a cache with a reference to a singleton hang on to it?
     */
    @Test
    public void testCacheManagerReferenceSingleton() {
        singletonManager = CacheManager.create();
        singletonManager.addCache("test");
        Ehcache cache = singletonManager.getCache("test");
        assertEquals("test", cache.getName());
        assertEquals(Status.STATUS_ALIVE, cache.getStatus());
        CacheManager reference = cache.getCacheManager();
        assertTrue(reference == singletonManager);
    }

    /**
     * Checks we can disable ehcache using a system property
     */
    @Test
    public void testDisableEhcache() throws CacheException,
            InterruptedException {
        System.setProperty(Cache.NET_SF_EHCACHE_DISABLED, "true");
        Thread.sleep(1000);
        instanceManager = CacheManager.create();
        Ehcache cache = instanceManager.getCache("sampleCache1");
        assertNotNull(cache);
        cache.put(new Element("key123", "value"));
        Element element = cache.get("key123");
        assertNull(
                "When the disabled property is set all puts should be discarded",
                element);

        cache.putQuiet(new Element("key1234", "value"));
        assertNull(
                "When the disabled property is set all puts should be discarded",
                cache.get("key1234"));

        System.setProperty(Cache.NET_SF_EHCACHE_DISABLED, "false");

    }

    /**
     * Tests shutdown after shutdown.
     */
    @Test
    public void testShutdownAfterShutdown() throws CacheException {
        instanceManager = CacheManager.create();
        assertEquals(Status.STATUS_ALIVE, instanceManager.getStatus());
        instanceManager.shutdown();
        assertEquals(Status.STATUS_SHUTDOWN, instanceManager.getStatus());
        instanceManager.shutdown();
        assertEquals(Status.STATUS_SHUTDOWN, instanceManager.getStatus());
    }

    /**
     * Tests create, shutdown, create
     */
    @Test
    public void testCreateShutdownCreate() throws CacheException {
        singletonManager = CacheManager.create();
        assertEquals(Status.STATUS_ALIVE, singletonManager.getStatus());
        singletonManager.shutdown();

        // check we can recreate the CacheManager on demand.
        singletonManager = CacheManager.create();
        assertNotNull(singletonManager);
        assertEquals(CACHES_IN_EHCACHE_XML, singletonManager.getCacheNames().length);
        assertEquals(Status.STATUS_ALIVE, singletonManager.getStatus());

        singletonManager.shutdown();
        assertEquals(Status.STATUS_SHUTDOWN, singletonManager.getStatus());
    }

    /**
     * Tests removing a cache
     */
    @Test
    public void testRemoveCache() throws CacheException {
        singletonManager = CacheManager.create();
        assertEquals(15, singletonManager.getConfiguration().getCacheConfigurations().size());
        Ehcache cache = singletonManager.getCache("sampleCache1");
        assertNotNull(cache);
        singletonManager.removeCache("sampleCache1");
        cache = singletonManager.getCache("sampleCache1");
        assertNull(cache);

        assertEquals(14, singletonManager.getConfiguration().getCacheConfigurations().size());

        // NPE tests
        singletonManager.removeCache(null);
        singletonManager.removeCache("");
        assertEquals(14, singletonManager.getConfiguration().getCacheConfigurations().size());
    }

    @Test
    public void testAddRemoveCache() throws CacheException {
        String config = "<ehcache><defaultCache maxEntriesLocalHeap=\"0\"/></ehcache>";
        CacheManager manager = new CacheManager(new ByteArrayInputStream(config.getBytes()));
        assertEquals(0, manager.getConfiguration().getCacheConfigurations().size());
        manager.addCache("test1");
        assertEquals(1, manager.getConfiguration().getCacheConfigurations().size());
        manager.removalAll();
        assertEquals(0, manager.getConfiguration().getCacheConfigurations().size());
        manager.shutdown();
    }

    /**
     * Tests adding a new cache with default config
     */
    @Test
    public void testAddCache() throws CacheException {
        singletonManager = CacheManager.create();
        assertEquals(15, singletonManager.getConfiguration().getCacheConfigurations().size());
        singletonManager.addCache("test");
        singletonManager.addCache("test2");
        assertEquals(17, singletonManager.getConfiguration().getCacheConfigurations().size());
        Ehcache cache = singletonManager.getCache("test");
        assertNotNull(cache);
        assertEquals("test", cache.getName());
        String[] cacheNames = singletonManager.getCacheNames();
        boolean match = false;
        for (String cacheName : cacheNames) {
            if (cacheName.equals("test")) {
                match = true;
            }
        }
        assertTrue(match);

        // NPE tests
        singletonManager.addCache("");
        assertEquals(17, singletonManager.getConfiguration().getCacheConfigurations().size());
    }

    @Test
    public void testAddCacheIfAbsent() {
        singletonManager = CacheManager.create();
        singletonManager.addCache("present");
        assertTrue(singletonManager.getCache("present")
                == singletonManager.addCacheIfAbsent(new Cache(new CacheConfiguration("present", 1000))));

        Cache theCache = new Cache(new CacheConfiguration("absent", 1000));
        Ehcache cache = singletonManager.addCacheIfAbsent(theCache);
        assertNotNull(cache);
        assertTrue(theCache == cache);
        assertEquals("absent", cache.getName());

        Cache other = new Cache(new CacheConfiguration(cache.getName(), 1000));
        Ehcache actualCacheRegisteredWithManager = singletonManager.addCacheIfAbsent(other);
        assertNotNull(actualCacheRegisteredWithManager);
        assertFalse(other == actualCacheRegisteredWithManager);
        assertTrue(cache == actualCacheRegisteredWithManager);

        Cache newCache = new Cache(new CacheConfiguration(cache.getName(), 1000));
        singletonManager.removeCache(actualCacheRegisteredWithManager.getName());
        actualCacheRegisteredWithManager = singletonManager.addCacheIfAbsent(newCache);
        assertNotNull(actualCacheRegisteredWithManager);
        assertFalse(cache == actualCacheRegisteredWithManager);
        assertTrue(newCache == actualCacheRegisteredWithManager);

        assertTrue(singletonManager.addCacheIfAbsent(new Cache(new CacheConfiguration(actualCacheRegisteredWithManager.getName(), 1000)))
                == actualCacheRegisteredWithManager);

        assertNull(singletonManager.addCacheIfAbsent((Ehcache) null));
    }

    @Test
    public void testAddNamedCacheIfAbsent() {
        singletonManager = CacheManager.create();
        String presentCacheName = "present";
        singletonManager.addCache(presentCacheName);
        Cache alreadyPresent = singletonManager.getCache(presentCacheName);
        Ehcache cache = singletonManager.addCacheIfAbsent(presentCacheName);
        assertNotNull(cache);
        assertTrue(alreadyPresent == cache);
        assertEquals(presentCacheName, cache.getName());

        Ehcache actualCacheRegisteredWithManager = singletonManager.addCacheIfAbsent("absent");
        assertNotNull(actualCacheRegisteredWithManager);
        assertTrue(singletonManager.getCache(actualCacheRegisteredWithManager.getName()) == actualCacheRegisteredWithManager);
        assertEquals("absent", actualCacheRegisteredWithManager.getName());
        assertTrue(singletonManager.addCacheIfAbsent(actualCacheRegisteredWithManager.getName()) == actualCacheRegisteredWithManager);

        assertTrue(singletonManager.addCacheIfAbsent(new Cache(new CacheConfiguration(actualCacheRegisteredWithManager.getName(), 1000)))
                == actualCacheRegisteredWithManager);
        assertNull(singletonManager.addCacheIfAbsent((String) null));
        assertNull(singletonManager.addCacheIfAbsent(""));
    }

    /**
     * Tests we can add caches from the default where the default has listeners.
     * Since 1.7, a CacheUsageStatisticsData is also registered.
     */
    @Test
    public void testAddCacheFromDefaultWithListeners() throws CacheException {
        singletonManager = CacheManager
                .create(AbstractCacheTest.TEST_CONFIG_DIR + File.separator
                        + "distribution" + File.separator
                        + "ehcache-distributed1.xml");
        singletonManager.addCache("test");
        Ehcache cache = singletonManager.getCache("test");
        assertNotNull(cache);
        assertEquals("test", cache.getName());

        Set listeners = cache.getCacheEventNotificationService()
                .getCacheEventListeners();
        assertEquals(2, listeners.size());
        for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
            CacheEventListener cacheEventListener = (CacheEventListener) iterator
                    .next();
            assertTrue(cacheEventListener instanceof RMIAsynchronousCacheReplicator
                    || cacheEventListener instanceof LiveCacheStatisticsData);
        }
    }

    /**
     * Bug 1457268. Instance of RegisteredEventListeners shared between caches
     * created from default cache. The issue also results in sharing of all
     * references. This test makes sure each cache has its own.
     */
    @Test
    public void testCachesCreatedFromDefaultDoNotShareListenerReferences() {
        singletonManager = CacheManager.create();
        singletonManager.addCache("newfromdefault1");
        Cache cache1 = singletonManager.getCache("newfromdefault1");
        singletonManager.addCache("newfromdefault2");
        Cache cache2 = singletonManager.getCache("newfromdefault2");

        RegisteredEventListeners listeners1 = cache1
                .getCacheEventNotificationService();
        RegisteredEventListeners listeners2 = cache2
                .getCacheEventNotificationService();
        assertTrue(listeners1 != listeners2);

        Store store1 = cache1.getStore();
        Store store2 = cache2.getStore();
        assertTrue(store1 != store2);

    }

    /**
     * Do bootstrap cache loaders work ok when created from the default cache?
     */
    @Test
    public void testCachesCreatedFromDefaultWithBootstrapSet() {
        singletonManager = CacheManager
                .create(AbstractCacheTest.TEST_CONFIG_DIR
                        + "distribution/ehcache-distributed1.xml");
        singletonManager.addCache("newfromdefault1");
        Cache newfromdefault1 = singletonManager.getCache("newfromdefault1");
        singletonManager.addCache("newfromdefault2");
        Cache newfromdefault2 = singletonManager.getCache("newfromdefault2");

        assertTrue(newfromdefault1 != newfromdefault2);

        BootstrapCacheLoader bootstrapCacheLoader1 = (newfromdefault1)
                .getBootstrapCacheLoader();
        BootstrapCacheLoader bootstrapCacheLoader2 = (newfromdefault2)
                .getBootstrapCacheLoader();

        assertTrue(bootstrapCacheLoader1 != bootstrapCacheLoader2);

        assertNotNull(bootstrapCacheLoader1);
        assertEquals(RMIBootstrapCacheLoader.class, bootstrapCacheLoader1
                .getClass());
        assertEquals(true, bootstrapCacheLoader1.isAsynchronous());
        assertEquals(5000000, ((RMIBootstrapCacheLoader) bootstrapCacheLoader1)
                .getMaximumChunkSizeBytes());

    }

    /**
     * Does clone work ok?
     */
    @Test
    public void testCachesCreatedFromDefaultDoNotInteract() {
        singletonManager = CacheManager
                .create(AbstractCacheTest.TEST_CONFIG_DIR
                        + "distribution/ehcache-distributed1.xml");
        singletonManager.addCache("newfromdefault1");
        Cache newfromdefault1 = singletonManager.getCache("newfromdefault1");
        singletonManager.addCache("newfromdefault2");
        Cache newfromdefault2 = singletonManager.getCache("newfromdefault2");

        assertTrue(newfromdefault1 != newfromdefault2);
        assertFalse(newfromdefault1.getName().equals(newfromdefault2.getName()));
        // status is an enum style class, so it ok for them to point to the same
        // instance if they are the same
        assertTrue(newfromdefault1.getStatus() == newfromdefault2.getStatus());
        assertFalse(newfromdefault1.getGuid() == newfromdefault2.getGuid());
    }

    /**
     * Test using a cache which has been removed and replaced.
     */
    @Test
    public void testStaleCacheReference() throws CacheException {
        singletonManager = CacheManager.create();
        singletonManager.addCache("test");
        Ehcache cache = singletonManager.getCache("test");
        assertNotNull(cache);
        cache.put(new Element("key1", "value1"));

        assertEquals("value1", cache.get("key1").getObjectValue());
        singletonManager.removeCache("test");
        singletonManager.addCache("test");

        try {
            cache.get("key1");
            fail();
        } catch (IllegalStateException e) {
            assertEquals("The test Cache is not alive (STATUS_SHUTDOWN)", e.getMessage());
        }
    }

    private int countThreads() {
        return JVMUtil.enumerateThreads().size();
    }

    /**
     * Shows that a decorated cache can be substituted
     */
    @Test
    public void testDecoratorRequiresDecoratedCache() {

        singletonManager = CacheManager.create();
        Ehcache cache = singletonManager.getEhcache("sampleCache1");
        // decorate and substitute
        BlockingCache newBlockingCache = new BlockingCache(cache);
        singletonManager
                .replaceCacheWithDecoratedCache(cache, newBlockingCache);
        Ehcache blockingCache = singletonManager.getEhcache("sampleCache1");
        assertNull(singletonManager.getCache("sampleCache1"));
        blockingCache.get("unknownkey");
        assertTrue(singletonManager.getEhcache("sampleCache1") == newBlockingCache);
    }

    /**
     * Shows that a decorated cache can be substituted
     */
    @Test
    public void testDecoratorFailsIfUnderlyingCacheNotSame() {

        singletonManager = CacheManager.create();
        Ehcache cache = singletonManager.getEhcache("sampleCache1");
        Ehcache cache2 = singletonManager.getEhcache("sampleCache2");
        // decorate and substitute
        BlockingCache newBlockingCache = new BlockingCache(cache2);
        try {
            singletonManager.replaceCacheWithDecoratedCache(cache,
                    newBlockingCache);
        } catch (CacheException e) {
            // expected
        }
        assertNotNull(singletonManager.getCache("sampleCache1"));
    }

    @Test
    public void testDecoratorFailsIfUnderlyingCacheHasChanged() {

        singletonManager = CacheManager.create();
        Ehcache cache = singletonManager.getEhcache("sampleCache1");
        singletonManager.removeCache("sampleCache1");
        singletonManager.addCache("sampleCache1");
        // decorate and substitute
        BlockingCache newBlockingCache = new BlockingCache(cache);
        try {
            singletonManager.replaceCacheWithDecoratedCache(cache,
                    newBlockingCache);
            fail("This should throw an exception!");
        } catch (CacheException e) {
            // expected
        }
        assertFalse(singletonManager.getEhcache("sampleCache1") instanceof BlockingCache);
    }

    @Test
    public void testDecoratorFailsIfUnderlyingCacheIsNotPresent() {

        singletonManager = CacheManager.create();
        Ehcache cache = singletonManager.getEhcache("sampleCache1");
        singletonManager.removeCache("sampleCache1");
        // decorate and substitute
        BlockingCache newBlockingCache = new BlockingCache(cache);
        try {
            singletonManager.replaceCacheWithDecoratedCache(cache,
                    newBlockingCache);
            fail("This should throw an exception!");
        } catch (CacheException e) {
            // expected
        }
        assertFalse(singletonManager.getEhcache("sampleCache1") instanceof BlockingCache);
    }

    /**
     * Shows that a decorated cache has decorated behaviour for methods that
     * override Cache methods, without requiring a cast.
     */
    @Test
    public void testDecoratorOverridesDefaultBehaviour() {

        singletonManager = CacheManager.create();
        Ehcache cache = singletonManager.getEhcache("sampleCache1");
        Element element = cache.get("key");
        // default behaviour for a missing key
        assertNull(element);

        // decorate and substitute
        SelfPopulatingCache selfPopulatingCache = new SelfPopulatingCache(
                cache, new CountingCacheEntryFactory("value"));
        selfPopulatingCache.get("key");
        singletonManager.replaceCacheWithDecoratedCache(cache,
                selfPopulatingCache);

        Ehcache decoratedCache = singletonManager.getEhcache("sampleCache1");
        assertNull(singletonManager.getCache("sampleCache1"));
        Element element2 = cache.get("key");
        assertEquals("value", element2.getObjectValue());
    }

    /**
     * Test added after bug with multiple cachemanagers and programmatic cache
     * creation
     */
    @Test
    public void testMultipleCacheManagers() {
        CacheManager[] managers = new CacheManager[2];
        managers[0] = new CacheManager(makeCacheManagerConfig("cm1"));
        managers[1] = new CacheManager(makeCacheManagerConfig("cm2"));

        managers[0].shutdown();
        managers[1].shutdown();

    }

    private static Configuration makeCacheManagerConfig(String cmName) {
        Configuration config = new Configuration().name(cmName);
        CacheConfiguration defaults = new CacheConfiguration("cacheName", 10)
                .eternal(true);
        config.setDefaultCacheConfiguration(defaults);
        return config;
    }

    /**
     * Make sure we can manipulate tmpdir. This is so continous integration
     * builds can get the disk path zapped each run.
     */
    @Test
    public void testTmpDir() {
        String tmp = System.getProperty("java.io.tmpdir");
        System.setProperty("java.io.tmpdir", "greg");
        assertEquals("greg", System.getProperty("java.io.tmpdir"));
        System.setProperty("java.io.tmpdir", tmp);
        assertEquals(tmp, System.getProperty("java.io.tmpdir"));

    }

    /**
     * I have suggested that people can rely on the thread names to change
     * priorities etc. The names should stay fixed.
     */
    @Test
    public void testThreadNamingAndManipulation() {

        singletonManager = CacheManager.create();

        List threads = JVMUtil.enumerateThreads();

        for (int i = 0; i < threads.size(); i++) {
            Thread thread = (Thread) threads.get(i);
            String name = thread.getName();
            LOG.info(name);
        }
    }

    /**
     * Tests that the CacheManager implements clearAll():void and clearAllStartingWith(String):void properly
     */
    @Test
    public void testClearCacheManager() throws CacheException {
        singletonManager = CacheManager.create();
        assertNotNull(singletonManager);
        assertEquals(CACHES_IN_EHCACHE_XML, singletonManager.getCacheNames().length);
        singletonManager.getEhcache("sampleCache1").put(new Element("key1", "value"));
        assertEquals(1, singletonManager.getEhcache("sampleCache1").getSize());
        singletonManager.getEhcache("sampleCache2").put(new Element("key2", "value"));
        assertEquals(1, singletonManager.getEhcache("sampleCache2").getSize());
        singletonManager.getEhcache("CachedLogin").put(new Element("key3", "value"));
        assertEquals(1, singletonManager.getEhcache("CachedLogin").getSize());
        singletonManager.clearAllStartingWith("");
        assertEquals(1, singletonManager.getEhcache("sampleCache1").getSize());
        assertEquals(1, singletonManager.getEhcache("sampleCache2").getSize());
        assertEquals(1, singletonManager.getEhcache("CachedLogin").getSize());
        singletonManager.clearAllStartingWith("sample");
        assertEquals(0, singletonManager.getEhcache("sampleCache1").getSize());
        assertEquals(0, singletonManager.getEhcache("sampleCache2").getSize());
        assertEquals(1, singletonManager.getEhcache("CachedLogin").getSize());
        singletonManager.clearAll();
        assertEquals(0, singletonManager.getEhcache("sampleCache1").getSize());
        assertEquals(0, singletonManager.getEhcache("sampleCache2").getSize());
        assertEquals(0, singletonManager.getEhcache("CachedLogin").getSize());
    }

}

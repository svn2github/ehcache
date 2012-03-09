/**
 *  Copyright 2003-2010 Terracotta, Inc.
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

import static net.sf.ehcache.util.RetryAssert.assertBy;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.lessThanOrEqualTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

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
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

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
import net.sf.ehcache.util.RetryAssert;
import org.hamcrest.collection.IsCollectionWithSize;
import org.hamcrest.collection.IsEmptyCollection;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
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

    @BeforeClass
    public static void installRMISocketFactory() {
        AbstractRMITest.installRMISocketFactory();
    }

    @BeforeClass
    public static void shutdownRunningCacheManagers() {
        if (!CacheManager.ALL_CACHE_MANAGERS.isEmpty()) {
          LOG.warn("Expected NO CacheManagers on test startup " + CacheManager.ALL_CACHE_MANAGERS);
          for (CacheManager manager : CacheManager.ALL_CACHE_MANAGERS) {
            manager.shutdown();
          }
        }
        Assert.assertThat(CacheManager.ALL_CACHE_MANAGERS, IsEmptyCollection.<CacheManager>empty());
    }
    
    @Before
    public void noCacheManagersBefore() {
        Assert.assertThat(CacheManager.ALL_CACHE_MANAGERS, IsEmptyCollection.<CacheManager>empty());
    }
    
    @After
    public void noCacheManagersAfter() {
        Assert.assertThat(CacheManager.ALL_CACHE_MANAGERS, IsEmptyCollection.<CacheManager>empty());
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
        try {
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
        } finally {
            cacheManager.shutdown();
        }
    }

    @Test
    public void testCantAddCacheWhenOverAllocatingCacheManagerPool() {
        Configuration configuration = new Configuration()
            .maxBytesLocalHeap(5, MemoryUnit.MEGABYTES);
        CacheManager cacheManager = new CacheManager(configuration);
        try {
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
        } finally {
            cacheManager.shutdown();
        }
    }

    // todo This should be addressed at some point: we're cloning things around too much...
    @Ignore
    @Test
    public void testCacheConfigurationAreInSync() {
        Configuration configuration = new Configuration().cache(new CacheConfiguration("one", 0));
        CacheManager cacheManager = new CacheManager(configuration);
        try {
            assertThat(cacheManager.getCache("one").getCacheConfiguration(),
                    is(cacheManager.getConfiguration().getCacheConfigurations().get("one")));
        } finally {
            cacheManager.shutdown();
        }
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
        try {
            assertThat(cacheManager.getCache("one").getCacheConfiguration().getMaxBytesLocalHeap(), equalTo(MemoryUnit.MEGABYTES.toBytes(1)));
            assertThat(cacheManager.getCache("two").getCacheConfiguration().getMaxBytesLocalHeap(), equalTo(MemoryUnit.MEGABYTES.toBytes(1)));
            assertThat(cacheManager.getCache("three").getCacheConfiguration().getMaxBytesLocalHeap(), equalTo(0L));

            configuration.maxBytesLocalHeap(10, MemoryUnit.MEGABYTES);

            assertThat(cacheManager.getCache("one").getCacheConfiguration().getMaxBytesLocalHeap(), equalTo(MemoryUnit.MEGABYTES.toBytes(2)));
            assertThat(cacheManager.getCache("two").getCacheConfiguration().getMaxBytesLocalHeap(), equalTo(MemoryUnit.MEGABYTES.toBytes(2)));
            assertThat(cacheManager.getCache("three").getCacheConfiguration().getMaxBytesLocalHeap(), equalTo(0L));
        } finally {
            cacheManager.shutdown();
        }
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
        try {
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
        } finally {
            cacheManager.shutdown();
        }

        try {
            new CacheManager(configuration).shutdown();
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
        try {
            assertThat(manager.getCache("before1"), notNullValue());
            assertThat(manager.getCache("before2"), notNullValue());
            try {
                configuration.addCache(new CacheConfiguration("after", 100));
                fail("Should have had a IllegalStateException");
            } catch (IllegalStateException e) {
                assertThat(manager.getCache("after"), nullValue());
            }
        } finally {
            manager.shutdown();
        }
    }

    @Test
    public void testSupportsNameChanges() {
        Configuration configuration = new Configuration().name("firstName");
        CacheManager manager = new CacheManager(configuration);
        try {
            assertThat(manager.getName(), equalTo("firstName"));

            manager.setName("newerName");
            assertThat(configuration.getName(), equalTo("newerName"));
            assertThat(manager.getName(), equalTo("newerName"));

            configuration.setName("evenNewerName");
            assertThat(configuration.getName(), equalTo("evenNewerName"));
            assertThat(manager.getName(), equalTo("evenNewerName"));
        } finally {
            manager.shutdown();
        }
    }

    @Test
    public void testMaxBytesOverAllocated() {

        Configuration configuration = new Configuration()
            .maxBytesLocalHeap(50, MemoryUnit.KILOBYTES)
            .cache(new CacheConfiguration("one", 0).maxBytesLocalHeap(30, MemoryUnit.KILOBYTES))
            .cache(new CacheConfiguration("two", 0).maxBytesLocalHeap(30, MemoryUnit.KILOBYTES));

        try {
            new CacheManager(configuration).shutdown();
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
            new CacheManager(configuration).shutdown();
            fail("This should have thrown an InvalidConfigurationException");
        } catch (InvalidConfigurationException e) {
            assertThat(e.getMessage().contains("over-allocate"), is(true));
        }

        configuration = new Configuration()
            .maxBytesLocalHeap(4096, MemoryUnit.GIGABYTES)
            .cache(new CacheConfiguration("one", 0).maxBytesLocalHeap(30, MemoryUnit.KILOBYTES))
            .cache(new CacheConfiguration("two", 0).maxBytesLocalHeap(30, MemoryUnit.KILOBYTES));

        try {
            new CacheManager(configuration).shutdown();
            fail("This should have thrown an InvalidConfigurationException");
        } catch (InvalidConfigurationException e) {
            assertThat(e.getMessage().contains("-Xmx"), is(true));
        }

        configuration = new Configuration()
            .cache(new CacheConfiguration("one", 0).maxBytesLocalHeap(2048, MemoryUnit.GIGABYTES))
            .cache(new CacheConfiguration("two", 0).maxBytesLocalHeap(2048, MemoryUnit.GIGABYTES));

        try {
            new CacheManager(configuration).shutdown();
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
        } finally {
            cacheManager.shutdown();
        }
    }

    @Test
    public void testPoolSize() throws Exception {
        Configuration configuration = new Configuration()
            .diskStore(new DiskStoreConfiguration().path("./tmp"))
            .maxBytesLocalHeap(50, MemoryUnit.MEGABYTES)
            .maxBytesLocalDisk(500, MemoryUnit.MEGABYTES)
            .cache(new CacheConfiguration("one", 0).maxBytesLocalHeap(10, MemoryUnit.MEGABYTES))
            .cache(new CacheConfiguration("two", 0).maxBytesLocalHeap(10, MemoryUnit.MEGABYTES))
            .cache(new CacheConfiguration("three", 0).maxBytesLocalDisk(100, MemoryUnit.MEGABYTES));

        CacheManager cacheManager = new CacheManager(configuration);
        try {
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
        } finally {
            cacheManager.shutdown();
        }
    }

    @Test
    public void testCacheReferenceLookUps() {
        CacheManager manager = CacheManager.create();
        try {
            String cacheName = "randomNewCache";
            manager.addCache(new Cache(new CacheConfiguration().name(cacheName).maxEntriesLocalHeap(1000)));

            // Default state by name
            Cache cache = manager.getCache(cacheName);
            assertNotNull(cache);
            assertNotNull(manager.getEhcache(cacheName));
            assertTrue(manager.getEhcache(cacheName) instanceof Cache);
            assertTrue(cache == manager.getEhcache(cacheName));

            // replace cache
            BlockingCache decoratedCache = new BlockingCache(cache);
            manager.replaceCacheWithDecoratedCache(cache, decoratedCache);
            assertNull(manager.getCache(cacheName));
            assertNotNull(manager.getEhcache(cacheName));
            assertTrue(manager.getEhcache(cacheName) == decoratedCache);
        } finally {
            manager.shutdown();
        }
    }

    @Test
    public void testProgrammaticConfigurationFailsProperlyWhenNoDefaultCacheConfigured() {
        Configuration mgrConfig = new Configuration();
        mgrConfig.setUpdateCheck(false);
        new CacheManager(mgrConfig).shutdown();
    }

    /**
     * Tests that the CacheManager was successfully created
     */
    @Test
    public void testCreateCacheManager() throws CacheException {
        CacheManager manager = CacheManager.create();
        try {
            manager.getEhcache("");
            assertNotNull(manager);
            assertEquals(CACHES_IN_EHCACHE_XML, manager.getCacheNames().length);
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Tests that the CacheManager was successfully created
     */
    @Test
    public void testCreateCacheManagerFromFile() throws CacheException {
        CacheManager manager = CacheManager.create(AbstractCacheTest.SRC_CONFIG_DIR + "ehcache.xml");
        try {
            assertNotNull(manager);
            assertEquals(6, manager.getCacheNames().length);
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Tests that the CacheManager was successfully created from a Configuration
     */
    @Test
    public void testCreateCacheManagerFromConfiguration() throws CacheException {
        File file = new File(AbstractCacheTest.SRC_CONFIG_DIR + "ehcache.xml");
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        CacheManager manager = new CacheManager(configuration);
        try {
            assertNotNull(manager);
            assertEquals(6, manager.getCacheNames().length);
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Tests that the CacheManager was successfully created
     */
    @Test
    public void testCreateCacheManagerFromInputStream() throws Exception {
        InputStream fis = new FileInputStream(new File(AbstractCacheTest.SRC_CONFIG_DIR, "ehcache.xml"));
        try {
            CacheManager manager = CacheManager.create(fis);
            try {
                assertNotNull(manager);
                assertEquals(6, manager.getCacheNames().length);
            } finally {
                manager.shutdown();
            }
        } finally {
            fis.close();
        }
    }

    /**
     * Tests that creating a second cache manager with the same disk path will
     * fail.
     */
    @Test
    public void testCreateTwoCacheManagersWithSamePath() throws CacheException {
        URL configUrl = this.getClass().getResource("/ehcache-2.xml");

        CacheManager managerOne = CacheManager.create(configUrl);
        try {
            Configuration secondCacheConfiguration = ConfigurationFactory.parseConfiguration(configUrl).name("some-name");
            CacheManager managerTwo = new CacheManager(secondCacheConfiguration);
            try {
                String intialDiskStorePath = System.getProperty("java.io.tmpdir")
                        + File.separator + "second";

                File diskStorePathDir = new File(intialDiskStorePath);
                File[] files = diskStorePathDir.listFiles();
                File newDiskStorePath = null;
                boolean newDiskStorePathFound = false;
                for (File file : files) {
                    if (file.isDirectory()) {
                        if (file.getName().indexOf(
                                DiskStore.AUTO_DISK_PATH_DIRECTORY_PREFIX) != -1) {
                            newDiskStorePathFound = true;
                            newDiskStorePath = file;
                            break;
                        }
                    }
                }
                assertTrue(newDiskStorePathFound);
                newDiskStorePath.delete();
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
        try {
            // Check can start second one with a different disk path
            URL configUrl = this.getClass().getResource(
                    "/ehcache-2.xml");
            Configuration secondCacheConfiguration = ConfigurationFactory.parseConfiguration(configUrl).name("cm-2");
            CacheManager manager = new CacheManager(secondCacheConfiguration);
            try {
                manager.getCache("sampleCache1").put(element2);

                assertEquals(element1, CacheManager.getInstance().getCache(
                        "sampleCache1").get(1 + ""));
                assertEquals(element2, manager.getCache("sampleCache1").get(
                        2 + ""));
            } finally {
                // shutting down instance should leave singleton unaffected
                manager.shutdown();
            }

            assertEquals(element1, CacheManager.getInstance().getCache(
                    "sampleCache1").get(1 + ""));

            // Try shutting and recreating a new instance cache manager
            manager = new CacheManager(secondCacheConfiguration);
            try {
                manager.getCache("sampleCache1").put(element2);
                CacheManager.getInstance().shutdown();
                assertEquals(element2, manager.getCache("sampleCache1").get(
                        2 + ""));

                // Try shutting and recreating the singleton cache manager
                CacheManager.getInstance().getCache("sampleCache1").put(element2);
                assertNull(CacheManager.getInstance().getCache("sampleCache1").get(
                        1 + ""));
                assertEquals(element2, CacheManager.getInstance().getCache(
                        "sampleCache1").get(2 + ""));
            } finally {
                manager.shutdown();
            }
        } finally {
            CacheManager.getInstance().shutdown();
        }
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
        try {
            Configuration secondConfig = ConfigurationFactory.parseConfiguration(new File(fileName)).name("cm-2");
            // Check can start second one with the same config
            CacheManager manager = new CacheManager(secondConfig);
            try {
                manager.getCache("sampleCache1").put(element2);

                assertEquals(element1, CacheManager.getInstance().getCache("sampleCache1").get(Integer.toString(1)));
                assertEquals(element2, manager.getCache("sampleCache1").get(Integer.toString(2)));

            } finally {
                // shutting down instance should leave singleton unaffected
                manager.shutdown();
            }
            assertEquals(element1, CacheManager.getInstance().getCache("sampleCache1").get(Integer.toString(1)));

            // Try shutting and recreating a new instance cache manager
            manager = new CacheManager(secondConfig);
            try {
                manager.getCache("sampleCache1").put(element2);
                CacheManager.getInstance().shutdown();
                assertEquals(element2, manager.getCache("sampleCache1").get(Integer.toString(2)));

                // Try shutting and recreating the singleton cache manager
                CacheManager.getInstance().getCache("sampleCache1").put(element2);
                assertNull(CacheManager.getInstance().getCache("sampleCache1").get(Integer.toString(1)));
                assertEquals(element2, CacheManager.getInstance().getCache("sampleCache1").get(Integer.toString(2)));
            } finally {
                manager.shutdown();
            }
        } finally {
            CacheManager.getInstance().shutdown();
        }
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
        int startingThreadCount = JVMUtil.enumerateThreads().size();

        URL configuration = this.getClass().getResource("/ehcache-2.xml");
        for (int i = 0; i < 100; i++) {
            new CacheManager(configuration).shutdown();
        }
        
        // Give the spools a chance to exit
        RetryAssert.assertBy(3, TimeUnit.SECONDS, new Callable<Collection<Thread>>() {
            public Collection<Thread> call() {
                return JVMUtil.enumerateThreads();
            }
        // Allow a bit of variation - one extra thread.
        }, hasSize(lessThanOrEqualTo(startingThreadCount + 1)));
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
     * ehcache-2.5 afaict one thread per Cache, made the test really test for exactly that, and make no other assumptions
     */
    @Test
    public void testCacheManagerThreads() throws CacheException,
            InterruptedException {
        final Collection<Thread> initialThreads = Collections.unmodifiableCollection(JVMUtil.enumerateThreads());
        CacheManager manager = CacheManager.create(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-big.xml");
        try {
            Collection<Thread> spawnedThreads = JVMUtil.enumerateThreads();
            spawnedThreads.removeAll(initialThreads);
            assertThat("Spawned Threads", spawnedThreads, hasSize(both(greaterThan(0)).and(lessThanOrEqualTo(manager.getCacheNames().length))));
        } finally {
            manager.shutdown();
        }
        
        Collection<Thread> deadThreads = new ArrayList<Thread>(initialThreads);
        deadThreads.removeAll(JVMUtil.enumerateThreads());
        assertThat("Stopped Threads", deadThreads, IsEmptyCollection.<Thread>empty());

        /*
         * The 'termination' of a ThreadPoolExecutor does not guarantee that all
         * if it's worker threads have terminated.  There is a race between
         * the worker threads terminating and evaluation this assertion.  We
         * give the worker threads 10 seconds to terminate.
         */
        assertBy(10, TimeUnit.SECONDS, new Callable<Collection<Thread>>() {
            @Override
            public Collection<Thread> call() throws Exception {
                Collection<Thread> newThreads = new ArrayList<Thread>(JVMUtil.enumerateThreads());
                newThreads.removeAll(initialThreads);
                return newThreads;
            }
        }, IsEmptyCollection.<Thread>empty());
    }

    /**
     * It should be possible to create a new CacheManager instance with the same
     * disk configuration, provided the first was shutdown. Note that any
     * persistent disk stores will be available to the second cache manager.
     */
    @Test
    public void testInstanceCreateShutdownCreate() throws CacheException {
        CacheManager manager = CacheManager.create();
        try {
            URL configUrl = this.getClass().getResource(
                    "/ehcache-2.xml");
            Configuration secondCacheConfiguration = ConfigurationFactory.parseConfiguration(configUrl).name("cm-2");
            new CacheManager(secondCacheConfiguration).shutdown();

            // shutting down instance should leave singleton ok
            assertEquals(CACHES_IN_EHCACHE_XML, manager.getCacheNames().length);

            CacheManager managerTwo = new CacheManager(secondCacheConfiguration);
            try {
                assertNotNull(managerTwo);
                assertEquals(8, managerTwo.getCacheNames().length);
            } finally {
                managerTwo.shutdown();
            }
        } finally {
            manager.shutdown();
        }
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

        CacheManager manager = new CacheManager(configuration);
        try {
            assertNotNull(manager);
            assertEquals(0, manager.getCacheNames().length);

            manager.addCache("toBeDerivedFromDefaultCache");
            Cache cache = new Cache("testCache", 1, true, false, 5, 2);
            manager.addCache(cache);

            assertEquals(2, manager.getCacheNames().length);
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Checks we can get a cache
     */
    @Test
    public void testGetCache() throws CacheException {
        CacheManager manager = CacheManager.create();
        try {
            Ehcache cache = manager.getCache("sampleCache1");
            assertNotNull(cache);
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Does the cache hang on to its instance?
     */
    @Test
    public void testCacheManagerReferenceInstance() {
        CacheManager manager = new CacheManager();
        try {
            manager.addCache("test");
            Ehcache cache = manager.getCache("test");
            assertEquals("test", cache.getName());
            assertEquals(Status.STATUS_ALIVE, cache.getStatus());
            CacheManager reference = cache.getCacheManager();
            assertTrue(reference == manager);
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Does a cache with a reference to a singleton hang on to it?
     */
    @Test
    public void testCacheManagerReferenceSingleton() {
        CacheManager manager = CacheManager.create();
        try {
            manager.addCache("test");
            Ehcache cache = manager.getCache("test");
            assertEquals("test", cache.getName());
            assertEquals(Status.STATUS_ALIVE, cache.getStatus());
            CacheManager reference = cache.getCacheManager();
            assertTrue(reference == manager);
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Checks we can disable ehcache using a system property
     */
    @Test
    public void testDisableEhcache() throws CacheException,
            InterruptedException {
        System.setProperty(Cache.NET_SF_EHCACHE_DISABLED, "true");
        try {
            CacheManager manager = CacheManager.create();
            try {
                Ehcache cache = manager.getCache("sampleCache1");
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

            } finally {
                manager.shutdown();
            }
        } finally {
            System.clearProperty(Cache.NET_SF_EHCACHE_DISABLED);
        }
    }

    /**
     * Tests shutdown after shutdown.
     */
    @Test
    public void testShutdownAfterShutdown() throws CacheException {
        CacheManager manager = CacheManager.create();
        try {
            assertEquals(Status.STATUS_ALIVE, manager.getStatus());
            manager.shutdown();
            assertEquals(Status.STATUS_SHUTDOWN, manager.getStatus());
            manager.shutdown();
            assertEquals(Status.STATUS_SHUTDOWN, manager.getStatus());
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Tests create, shutdown, create
     */
    @Test
    public void testCreateShutdownCreate() throws CacheException {
        CacheManager manager = CacheManager.create();
        try {
            assertEquals(Status.STATUS_ALIVE, manager.getStatus());
        } finally {
            manager.shutdown();
        }

        // check we can recreate the CacheManager on demand.
        manager = CacheManager.create();
        try {
            assertNotNull(manager);
            assertEquals(CACHES_IN_EHCACHE_XML, manager.getCacheNames().length);
            assertEquals(Status.STATUS_ALIVE, manager.getStatus());
        } finally {
            manager.shutdown();
        }
        assertEquals(Status.STATUS_SHUTDOWN, manager.getStatus());
    }

    /**
     * Tests removing a cache
     */
    @Test
    public void testRemoveCache() throws CacheException {
        CacheManager manager = CacheManager.create();
        try {
            assertEquals(15, manager.getConfiguration().getCacheConfigurations().size());
            Ehcache cache = manager.getCache("sampleCache1");
            assertNotNull(cache);
            manager.removeCache("sampleCache1");
            cache = manager.getCache("sampleCache1");
            assertNull(cache);

            assertEquals(14, manager.getConfiguration().getCacheConfigurations().size());

            // NPE tests
            manager.removeCache(null);
            manager.removeCache("");
            assertEquals(14, manager.getConfiguration().getCacheConfigurations().size());
        } finally {
            manager.shutdown();
        }
    }

    @Test
    public void testAddRemoveCache() throws CacheException {
        String config = "<ehcache><defaultCache maxEntriesLocalHeap=\"0\"/></ehcache>";
        CacheManager manager = new CacheManager(new ByteArrayInputStream(config.getBytes()));
        try {
            assertEquals(0, manager.getConfiguration().getCacheConfigurations().size());
            manager.addCache("test1");
            assertEquals(1, manager.getConfiguration().getCacheConfigurations().size());
            manager.removalAll();
            assertEquals(0, manager.getConfiguration().getCacheConfigurations().size());
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Tests adding a new cache with default config
     */
    @Test
    public void testAddCache() throws CacheException {
        CacheManager manager = CacheManager.create();
        try {
            assertEquals(15, manager.getConfiguration().getCacheConfigurations().size());
            manager.addCache("test");
            manager.addCache("test2");
            assertEquals(17, manager.getConfiguration().getCacheConfigurations().size());
            Ehcache cache = manager.getCache("test");
            assertNotNull(cache);
            assertEquals("test", cache.getName());
            String[] cacheNames = manager.getCacheNames();
            boolean match = false;
            for (String cacheName : cacheNames) {
                if (cacheName.equals("test")) {
                    match = true;
                }
            }
            assertTrue(match);

            // NPE tests
            manager.addCache("");
            assertEquals(17, manager.getConfiguration().getCacheConfigurations().size());
        } finally {
            manager.shutdown();
        }
    }

    @Test
    public void testAddCacheIfAbsent() {
        CacheManager manager = CacheManager.create();
        try {
            manager.addCache("present");
            assertTrue(manager.getCache("present")
                    == manager.addCacheIfAbsent(new Cache(new CacheConfiguration("present", 1000))));

            Cache theCache = new Cache(new CacheConfiguration("absent", 1000));
            Ehcache cache = manager.addCacheIfAbsent(theCache);
            assertNotNull(cache);
            assertTrue(theCache == cache);
            assertEquals("absent", cache.getName());

            Cache other = new Cache(new CacheConfiguration(cache.getName(), 1000));
            Ehcache actualCacheRegisteredWithManager = manager.addCacheIfAbsent(other);
            assertNotNull(actualCacheRegisteredWithManager);
            assertFalse(other == actualCacheRegisteredWithManager);
            assertTrue(cache == actualCacheRegisteredWithManager);

            Cache newCache = new Cache(new CacheConfiguration(cache.getName(), 1000));
            manager.removeCache(actualCacheRegisteredWithManager.getName());
            actualCacheRegisteredWithManager = manager.addCacheIfAbsent(newCache);
            assertNotNull(actualCacheRegisteredWithManager);
            assertFalse(cache == actualCacheRegisteredWithManager);
            assertTrue(newCache == actualCacheRegisteredWithManager);

            assertTrue(manager.addCacheIfAbsent(new Cache(new CacheConfiguration(actualCacheRegisteredWithManager.getName(), 1000)))
                    == actualCacheRegisteredWithManager);

            assertNull(manager.addCacheIfAbsent((Ehcache) null));
        } finally {
            manager.shutdown();
        }
    }

    @Test
    public void testAddNamedCacheIfAbsent() {
        CacheManager manager = CacheManager.create();
        try {
            String presentCacheName = "present";
            manager.addCache(presentCacheName);
            Cache alreadyPresent = manager.getCache(presentCacheName);
            Ehcache cache = manager.addCacheIfAbsent(presentCacheName);
            assertNotNull(cache);
            assertTrue(alreadyPresent == cache);
            assertEquals(presentCacheName, cache.getName());

            Ehcache actualCacheRegisteredWithManager = manager.addCacheIfAbsent("absent");
            assertNotNull(actualCacheRegisteredWithManager);
            assertTrue(manager.getCache(actualCacheRegisteredWithManager.getName()) == actualCacheRegisteredWithManager);
            assertEquals("absent", actualCacheRegisteredWithManager.getName());
            assertTrue(manager.addCacheIfAbsent(actualCacheRegisteredWithManager.getName()) == actualCacheRegisteredWithManager);

            assertTrue(manager.addCacheIfAbsent(new Cache(new CacheConfiguration(actualCacheRegisteredWithManager.getName(), 1000)))
                    == actualCacheRegisteredWithManager);
            assertNull(manager.addCacheIfAbsent((String) null));
            assertNull(manager.addCacheIfAbsent(""));
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Tests we can add caches from the default where the default has listeners.
     * Since 1.7, a CacheUsageStatisticsData is also registered.
     */
    @Test
    public void testAddCacheFromDefaultWithListeners() throws CacheException {
        CacheManager manager = CacheManager
                .create(AbstractCacheTest.TEST_CONFIG_DIR + File.separator
                        + "distribution" + File.separator
                        + "ehcache-distributed1.xml");
        try {
            manager.addCache("test");
            Ehcache cache = manager.getCache("test");
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
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Bug 1457268. Instance of RegisteredEventListeners shared between caches
     * created from default cache. The issue also results in sharing of all
     * references. This test makes sure each cache has its own.
     */
    @Test
    public void testCachesCreatedFromDefaultDoNotShareListenerReferences() {
        CacheManager manager = CacheManager.create();
        try {
            manager.addCache("newfromdefault1");
            Cache cache1 = manager.getCache("newfromdefault1");
            manager.addCache("newfromdefault2");
            Cache cache2 = manager.getCache("newfromdefault2");

            RegisteredEventListeners listeners1 = cache1
                    .getCacheEventNotificationService();
            RegisteredEventListeners listeners2 = cache2
                    .getCacheEventNotificationService();
            assertTrue(listeners1 != listeners2);

            Store store1 = cache1.getStore();
            Store store2 = cache2.getStore();
            assertTrue(store1 != store2);
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Do bootstrap cache loaders work ok when created from the default cache?
     */
    @Test
    public void testCachesCreatedFromDefaultWithBootstrapSet() {
        CacheManager manager = CacheManager
                .create(AbstractCacheTest.TEST_CONFIG_DIR
                        + "distribution/ehcache-distributed1.xml");
        try {
            manager.addCache("newfromdefault1");
            Cache newfromdefault1 = manager.getCache("newfromdefault1");
            manager.addCache("newfromdefault2");
            Cache newfromdefault2 = manager.getCache("newfromdefault2");

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
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Does clone work ok?
     */
    @Test
    public void testCachesCreatedFromDefaultDoNotInteract() {
        CacheManager manager = CacheManager
                .create(AbstractCacheTest.TEST_CONFIG_DIR
                        + "distribution/ehcache-distributed1.xml");
        try {
            manager.addCache("newfromdefault1");
            Cache newfromdefault1 = manager.getCache("newfromdefault1");
            manager.addCache("newfromdefault2");
            Cache newfromdefault2 = manager.getCache("newfromdefault2");

            assertTrue(newfromdefault1 != newfromdefault2);
            assertFalse(newfromdefault1.getName().equals(newfromdefault2.getName()));
            // status is an enum style class, so it ok for them to point to the same
            // instance if they are the same
            assertTrue(newfromdefault1.getStatus() == newfromdefault2.getStatus());
            assertFalse(newfromdefault1.getGuid() == newfromdefault2.getGuid());
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Test using a cache which has been removed and replaced.
     */
    @Test
    public void testStaleCacheReference() throws CacheException {
        CacheManager manager = CacheManager.create();
        try {
            manager.addCache("test");
            Ehcache cache = manager.getCache("test");
            assertNotNull(cache);
            cache.put(new Element("key1", "value1"));

            assertEquals("value1", cache.get("key1").getObjectValue());
            manager.removeCache("test");
            manager.addCache("test");

            try {
                cache.get("key1");
                fail();
            } catch (IllegalStateException e) {
                assertEquals("The test Cache is not alive (STATUS_SHUTDOWN)", e.getMessage());
            }
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Shows that a decorated cache can be substituted
     */
    @Test
    public void testDecoratorRequiresDecoratedCache() {

        CacheManager manager = CacheManager.create();
        try {
            Ehcache cache = manager.getEhcache("sampleCache1");
            // decorate and substitute
            BlockingCache newBlockingCache = new BlockingCache(cache);
            manager
                    .replaceCacheWithDecoratedCache(cache, newBlockingCache);
            Ehcache blockingCache = manager.getEhcache("sampleCache1");
            assertNull(manager.getCache("sampleCache1"));
            blockingCache.get("unknownkey");
            assertTrue(manager.getEhcache("sampleCache1") == newBlockingCache);
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Shows that a decorated cache can be substituted
     */
    @Test
    public void testDecoratorFailsIfUnderlyingCacheNotSame() {

        CacheManager manager = CacheManager.create();
        try {
            Ehcache cache = manager.getEhcache("sampleCache1");
            Ehcache cache2 = manager.getEhcache("sampleCache2");
            // decorate and substitute
            BlockingCache newBlockingCache = new BlockingCache(cache2);
            try {
                manager.replaceCacheWithDecoratedCache(cache,
                        newBlockingCache);
                fail();
            } catch (CacheException e) {
                // expected
            }
            assertNotNull(manager.getCache("sampleCache1"));
        } finally {
            manager.shutdown();
        }
    }

    @Test
    public void testDecoratorFailsIfUnderlyingCacheHasChanged() {

        CacheManager manager = CacheManager.create();
        try {
            Ehcache cache = manager.getEhcache("sampleCache1");
            manager.removeCache("sampleCache1");
            manager.addCache("sampleCache1");
            // decorate and substitute
            BlockingCache newBlockingCache = new BlockingCache(cache);
            try {
                manager.replaceCacheWithDecoratedCache(cache,
                        newBlockingCache);
                fail("This should throw an exception!");
            } catch (CacheException e) {
                // expected
            }
            assertFalse(manager.getEhcache("sampleCache1") instanceof BlockingCache);
        } finally {
            manager.shutdown();
        }
    }

    @Test
    public void testDecoratorFailsIfUnderlyingCacheIsNotPresent() {

        CacheManager manager = CacheManager.create();
        try {
            Ehcache cache = manager.getEhcache("sampleCache1");
            manager.removeCache("sampleCache1");
            // decorate and substitute
            BlockingCache newBlockingCache = new BlockingCache(cache);
            try {
                manager.replaceCacheWithDecoratedCache(cache,
                        newBlockingCache);
                fail("This should throw an exception!");
            } catch (CacheException e) {
                // expected
            }
            assertFalse(manager.getEhcache("sampleCache1") instanceof BlockingCache);
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Shows that a decorated cache has decorated behaviour for methods that
     * override Cache methods, without requiring a cast.
     */
    @Test
    public void testDecoratorOverridesDefaultBehaviour() {

        CacheManager manager = CacheManager.create();
        try {
            Ehcache cache = manager.getEhcache("sampleCache1");
            Element element = cache.get("key");
            // default behaviour for a missing key
            assertNull(element);

            // decorate and substitute
            SelfPopulatingCache selfPopulatingCache = new SelfPopulatingCache(
                    cache, new CountingCacheEntryFactory("value"));
            selfPopulatingCache.get("key");
            manager.replaceCacheWithDecoratedCache(cache,
                    selfPopulatingCache);

            Ehcache decoratedCache = manager.getEhcache("sampleCache1");
            assertNull(manager.getCache("sampleCache1"));
            Element element2 = cache.get("key");
            assertEquals("value", element2.getObjectValue());
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Test added after bug with multiple cachemanagers and programmatic cache
     * creation
     */
    @Test
    public void testMultipleCacheManagers() {
        CacheManager[] managers = new CacheManager[2];
        managers[0] = new CacheManager(makeCacheManagerConfig("cm1"));
        try {
            managers[1] = new CacheManager(makeCacheManagerConfig("cm2"));
            managers[1].shutdown();
        } finally {
            managers[0].shutdown();
        }
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
     * Ehcache 1.5 allows the diskStore element to be optional. Check that is is null
     * Add different cache constructors to make sure none inadvertently create a disk store
     */
    @Test
    public void testCacheManagerWithNoDiskCachesFromConfiguration() throws CacheException, InterruptedException {
        LOG.info(System.getProperty("java.io.tmpdir"));
        CacheManager manager = CacheManager.create(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-nodisk.xml");
        try {
            manager.addCache("jsecurity-activeSessionCache");
            Cache cacheA = manager.getCache("jsecurity-activeSessionCache");
            Cache cacheB = new Cache("1", 10, false, false, 2, 2);
            manager.addCache(cacheB);
            Cache cacheC = new Cache("2", 10, false, false, 2, 2, false, 100);
            manager.addCache(cacheC);
            for (int i = 0; i < 100; i++) {
                cacheA.put(new Element(i + "", "dog"));
                cacheB.put(new Element(i + "", "dog"));
                cacheC.put(new Element(i + "", "dog"));
            }
            Cache diskCache = new Cache("disk", 10, true, false, 2, 2);
            try {
                manager.addCache(diskCache);
                throw new AssertionError("Expected that adding a disk cache to a cache manager" +
                        " with no configured disk store path would throw CacheException");
            } catch (CacheException e) {
                LOG.info("Caught expected exception", e);
            }
        } finally {
            manager.shutdown();
        }
        assertEquals(null, manager.getDiskStorePath());
    }

    @Test
    public void testUnnamedCacheManagerDefaultName() {
        CacheManager cacheManager1 = new CacheManager();
        try {
            Assert.assertNotNull(CacheManager.getCacheManager(cacheManager1.getName()));
        } finally {
            cacheManager1.shutdown();
        }
    }

    /**
     * Tests that the CacheManager implements clearAll():void and clearAllStartingWith(String):void properly
     */
    @Test
    public void testClearCacheManager() throws CacheException {
        CacheManager manager = CacheManager.create();
        try {
            assertNotNull(manager);
            assertEquals(CACHES_IN_EHCACHE_XML, manager.getCacheNames().length);
            manager.getEhcache("sampleCache1").put(new Element("key1", "value"));
            assertEquals(1, manager.getEhcache("sampleCache1").getSize());
            manager.getEhcache("sampleCache2").put(new Element("key2", "value"));
            assertEquals(1, manager.getEhcache("sampleCache2").getSize());
            manager.getEhcache("CachedLogin").put(new Element("key3", "value"));
            assertEquals(1, manager.getEhcache("CachedLogin").getSize());
            manager.clearAllStartingWith("");
            assertEquals(1, manager.getEhcache("sampleCache1").getSize());
            assertEquals(1, manager.getEhcache("sampleCache2").getSize());
            assertEquals(1, manager.getEhcache("CachedLogin").getSize());
            manager.clearAllStartingWith("sample");
            assertEquals(0, manager.getEhcache("sampleCache1").getSize());
            assertEquals(0, manager.getEhcache("sampleCache2").getSize());
            assertEquals(1, manager.getEhcache("CachedLogin").getSize());
            manager.clearAll();
            assertEquals(0, manager.getEhcache("sampleCache1").getSize());
            assertEquals(0, manager.getEhcache("sampleCache2").getSize());
            assertEquals(0, manager.getEhcache("CachedLogin").getSize());
        } finally {
            manager.shutdown();
        }
    }

}

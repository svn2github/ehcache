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

import static net.sf.ehcache.util.RetryAssert.assertBy;
import static org.hamcrest.collection.IsArrayContainingInOrder.arrayContaining;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.sf.ehcache.bootstrap.BootstrapCacheLoader;
import net.sf.ehcache.bootstrap.BootstrapCacheLoaderFactory;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheConfiguration.BootstrapCacheLoaderFactoryConfiguration;
import net.sf.ehcache.config.CacheConfiguration.CacheEventListenerFactoryConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import net.sf.ehcache.config.ConfigurationHelper;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.config.InvalidConfigurationException;
import net.sf.ehcache.config.MemoryUnit;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration.Strategy;
import net.sf.ehcache.config.generator.ConfigurationUtil;
import net.sf.ehcache.constructs.blocking.BlockingCache;
import net.sf.ehcache.distribution.JVMUtil;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.CountingCacheEventListener;
import net.sf.ehcache.event.CountingCacheEventListenerFactory;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.terracotta.TerracottaClient;
import net.sf.ehcache.util.MemorySizeParser;

import org.hamcrest.collection.IsCollectionWithSize;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.CombinableMatcher;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.test.categories.CheckShorts;

/**
 * Tests for CacheManager
 *
 * @author Greg Luck
 * @version $Id$
 */
@Category(CheckShorts.class)
public class CacheManagerTest {

    private static final Logger LOG = LoggerFactory.getLogger(CacheManagerTest.class.getName());
    private static final int CACHES_IN_EHCACHE_XML = 15;

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
            .diskStore(new DiskStoreConfiguration().path("${java.io.tmpdir}/tmp"))
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
    public void testCacheManagerPoolResizing() {
        Configuration configuration = new Configuration()
                .diskStore(new DiskStoreConfiguration().path("${java.io.tmpdir}/tmp"))
                .maxBytesLocalHeap(50, MemoryUnit.MEGABYTES)
                .maxBytesLocalDisk(500, MemoryUnit.MEGABYTES)
                .cache(new CacheConfiguration("one", 0).maxBytesLocalHeap(10, MemoryUnit.MEGABYTES)
                        .maxBytesLocalDisk(100, MemoryUnit.MEGABYTES))
                .cache(new CacheConfiguration("two", 0));

        CacheManager cacheManager = new CacheManager(configuration);
        try {
            assertEquals(MemorySizeParser.parse("40M"), cacheManager.getOnHeapPool().getMaxSize());
            assertEquals(MemorySizeParser.parse("400M"), cacheManager.getOnDiskPool().getMaxSize());
            cacheManager.getConfiguration().maxBytesLocalHeap(20, MemoryUnit.MEGABYTES);
            cacheManager.getConfiguration().maxBytesLocalDisk(200, MemoryUnit.MEGABYTES);
            assertEquals(MemorySizeParser.parse("10M"), cacheManager.getOnHeapPool().getMaxSize());
            assertEquals(MemorySizeParser.parse("100M"), cacheManager.getOnDiskPool().getMaxSize());
        } finally {
            cacheManager.shutdown();
        }
    }

    @Test
    public void testCacheReferenceLookUps() {
        CacheManager manager = new CacheManager(new Configuration());
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
    public void testProgrammaticConfigurationWorksProperlyWhenNoDefaultCacheConfigured() {
        Configuration mgrConfig = new Configuration();
        mgrConfig.setUpdateCheck(false);
        new CacheManager(mgrConfig).shutdown();
    }

    /**
     * Tests that the CacheManager was successfully created
     */
    @Test
    public void testCreateCacheManager() throws IOException {
        Configuration config = new Configuration().cache(new CacheConfiguration("foo", 100));
        String configXml = ConfigurationUtil.generateCacheManagerConfigurationText(config);
        final File configFile = File.createTempFile("CacheManagerTest.testCreateCacheManager", ".xml");
        FileWriter writer = new FileWriter(configFile);
        try {
            writer.write(configXml);
        } finally {
            writer.close();
        }

        Thread.currentThread().setContextClassLoader(new ClassLoader() {

            @Override
            public URL getResource(String name) {
                if ("/ehcache.xml".equals(name)) {
                    try {
                        return configFile.toURI().toURL();
                    } catch (MalformedURLException e) {
                        throw new AssertionError(e);
                    }
                } else {
                    return super.getResource(name);
                }
            }

        });
        try {
            CacheManager manager = CacheManager.create();
            try {
                assertThat(manager.getCacheNames(), arrayContaining("foo"));
            } finally {
                manager.shutdown();
            }
        } finally {
            Thread.currentThread().setContextClassLoader(null);
        }
    }

    /**
     * Tests that the CacheManager was successfully created
     * @throws IOException
     */
    @Test
    public void testCreateCacheManagerFromFile() throws IOException {
        Configuration config = new Configuration().cache(new CacheConfiguration("foo", 100));
        String configXml = ConfigurationUtil.generateCacheManagerConfigurationText(config);
        File configFile = File.createTempFile("CacheManagerTest.testCreateCacheManagerFromFile", ".xml");
        FileWriter writer = new FileWriter(configFile);
        try {
            writer.write(configXml);
        } finally {
            writer.close();
        }

        CacheManager manager = CacheManager.create(configFile.getAbsolutePath());
        try {
            assertThat(manager.getCacheNames(), arrayContaining("foo"));
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Tests that the CacheManager was successfully created from a Configuration
     */
    @Test
    public void testCreateCacheManagerFromConfiguration() throws CacheException {
        Configuration configuration = new Configuration();
        configuration.cache(new CacheConfiguration("foo", 100));
        configuration.cache(new CacheConfiguration("bar", 100));
        CacheManager manager = new CacheManager(configuration);
        try {
            assertThat(manager.getCacheNames(), arrayContaining("foo", "bar"));
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Tests that the CacheManager was successfully created
     */
    @Test
    public void testCreateCacheManagerFromInputStream() throws Exception {
        Configuration config = new Configuration().cache(new CacheConfiguration("foo", 100));
        String configXml = ConfigurationUtil.generateCacheManagerConfigurationText(config);
        InputStream fis = new ByteArrayInputStream(configXml.getBytes("UTF-8"));
        try {
            CacheManager manager = CacheManager.create(fis);
            try {
                assertThat(manager.getCacheNames(), arrayContaining("foo"));
            } finally {
                manager.shutdown();
            }
        } finally {
            fis.close();
        }
    }

    @Test
    public void testSingletonAndNonSingletonAreIndependent() {
        CacheManager singleton = CacheManager.create(new Configuration());
        try {
            CacheManager other = new CacheManager(new Configuration().name("other"));
            try {
                Assert.assertThat(other, not(sameInstance(singleton)));
                Assert.assertThat(other.getName(), not(singleton.getName()));
            } finally {
                other.shutdown();
            }
        } finally {
            singleton.shutdown();
        }
    }

    /**
     * Tests that creating a second cache manager with the same disk path will
     * fail.
     */
    @Test
    public void testCreateTwoCacheManagersWithSamePath() throws CacheException {
        Configuration configOne = new Configuration().name("one")
                .diskStore(new DiskStoreConfiguration().path("target/CacheManagerTest/testCreateTwoCacheManagersWithSamePath"))
                .cache(new CacheConfiguration("foo", 100).persistence(new PersistenceConfiguration().strategy(Strategy.LOCALTEMPSWAP)));

        Configuration configTwo = new Configuration().name("two")
                .diskStore(new DiskStoreConfiguration().path("target/CacheManagerTest/testCreateTwoCacheManagersWithSamePath"))
                .cache(new CacheConfiguration("foo", 100).persistence(new PersistenceConfiguration().strategy(Strategy.LOCALTEMPSWAP)));

        CacheManager managerOne = new CacheManager(configOne);
        assertFalse(managerOne.getDiskStorePathManager().isAutoCreated());
        try {
            CacheManager managerTwo = new CacheManager(configTwo);
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
        Element element1 = new Element(Integer.toString(1), new Date());
        Element element2 = new Element(Integer.toString(2), new Date());

        Configuration configOne = new Configuration().name("one");
        configOne.diskStore(new DiskStoreConfiguration().path("${java.io.tmpdir}/CacheManagerTest/testTwoCacheManagers/one"));
        configOne.addCache(new CacheConfiguration("test", 100).persistence(new PersistenceConfiguration().strategy(Strategy.LOCALTEMPSWAP)));
        CacheManager managerOne = new CacheManager(configOne);
        try {
            Cache cacheOne = managerOne.getCache("test");
            cacheOne.put(element1);

            // Check can start second one with a different disk path
            Configuration configTwo = new Configuration().name("two");
            configTwo.diskStore(new DiskStoreConfiguration().path("${java.io.tmpdir}/CacheManagerTest/testTwoCacheManagers/two"));
            configTwo.addCache(new CacheConfiguration("test", 100).persistence(new PersistenceConfiguration().strategy(Strategy.LOCALTEMPSWAP)));
            CacheManager managerTwo = new CacheManager(configTwo);
            try {
                Cache cacheTwo = managerTwo.getCache("test");
                cacheTwo.put(element2);

                assertEquals(element1, cacheOne.get(Integer.toString(1)));
                assertEquals(element2, cacheTwo.get(Integer.toString(2)));
            } finally {
                managerTwo.shutdown();
            }

            assertEquals(element1, cacheOne.get(Integer.toString(1)));

            managerTwo = new CacheManager(configTwo);
            try {
                Cache cacheTwo = managerTwo.getCache("test");
                cacheTwo.put(element2);
                managerOne.shutdown();
                try {
                    assertEquals(element2, cacheTwo.get(Integer.toString(2)));

                    // Try shutting and recreating the singleton cache manager
                } finally {
                    managerOne = new CacheManager(configOne);
                }
                cacheOne = managerOne.getCache("test");
                cacheOne.put(element2);
                assertNull(cacheOne.get(Integer.toString(1)));
                assertEquals(element2, cacheOne.get(Integer.toString(2)));
            } finally {
                managerTwo.shutdown();
            }
        } finally {
            managerOne.shutdown();
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
        final Set<Thread> initialThreads = Collections.unmodifiableSet(Thread.getAllStackTraces().keySet());

        Configuration config = new Configuration();
        config.diskStore(new DiskStoreConfiguration().path("${java.io.tmpdir}/CacheManagerTest/testForCacheManagerThreadLeak"));
        config.cache(new CacheConfiguration("heap", 100));
        config.cache(new CacheConfiguration("disk", 100).maxEntriesLocalDisk(1000).persistence(new PersistenceConfiguration().strategy(Strategy.LOCALTEMPSWAP)));
        config.cache(new CacheConfiguration("persistent", 100).maxEntriesLocalDisk(1000).overflowToDisk(true).diskPersistent(true));
        for (int i = 0; i < 100; i++) {
            new CacheManager(config).shutdown();
        }

        /*
        * The 'termination' of a ThreadPoolExecutor does not guarantee that all
        * if it's worker threads have terminated.  There is a race between
        * the worker threads terminating and evaluation this assertion.  We
        * give the worker threads 10 seconds to terminate.
        */
        assertBy(10, TimeUnit.SECONDS, new Callable<Map<Thread, List<StackTraceElement>>>() {
            @Override
            public Map<Thread, List<StackTraceElement>> call() throws Exception {
                Map<Thread, StackTraceElement[]> newThreads = Thread.getAllStackTraces();
                newThreads.keySet().removeAll(initialThreads);
                Map<Thread, List<StackTraceElement>> newThreadsListStack = new HashMap<Thread, List<StackTraceElement>>();
                for (Entry<Thread, StackTraceElement[]> e : newThreads.entrySet()) {
                    newThreadsListStack.put(e.getKey(), Arrays.asList(e.getValue()));
                }
                return newThreadsListStack;
            }
        }, is(Collections.<Thread, List<StackTraceElement>>emptyMap()));
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
        Configuration config = new Configuration().diskStore(new DiskStoreConfiguration().path("${java.io.tmpdir}/CacheManagerTest/testCacheManagerThreads"));
        for (int i = 0; i < 70; i++) {
            config.cache(new CacheConfiguration().name(Integer.toString(i)).maxEntriesLocalHeap(100).maxEntriesLocalDisk(1000)
                    .persistence(new PersistenceConfiguration().strategy(Strategy.LOCALTEMPSWAP)));
        }
        int managerThreads = 0;
        managerThreads += Runtime.getRuntime().availableProcessors(); //statistics executor
        managerThreads += 1; //cache manager timer thread
        managerThreads += 1; //local transactions recovery thread
        CacheManager manager = new CacheManager(config);

        try {
            Collection<Thread> spawnedThreads = JVMUtil.enumerateThreads();
            spawnedThreads.removeAll(initialThreads);
            assertThat("Spawned Threads", spawnedThreads, hasSize(CombinableMatcher.<Integer>both(greaterThan(0)).and(lessThanOrEqualTo(manager.getCacheNames().length + managerThreads))));
        } finally {
            manager.shutdown();
        }

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
        URL configUrl = this.getClass().getResource(
                "/ehcache-2.xml");
        Configuration secondCacheConfiguration = ConfigurationFactory.parseConfiguration(configUrl).name("cm-2");
        new CacheManager(secondCacheConfiguration).shutdown();

        CacheManager managerTwo = new CacheManager(secondCacheConfiguration);
        try {
            assertNotNull(managerTwo);
            assertEquals(8, managerTwo.getCacheNames().length);
        } finally {
            managerTwo.shutdown();
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
        CacheManager manager = new CacheManager(new Configuration().cache(new CacheConfiguration("foo", 100)));
        try {
            assertNotNull(manager.getCache("foo"));
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Does the cache hang on to its instance?
     */
    @Test
    public void testCacheManagerReferenceInstance() {
        Configuration config = new Configuration().defaultCache(new CacheConfiguration().maxEntriesLocalHeap(10));
        CacheManager manager = new CacheManager(config);
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
        Configuration config = new Configuration().defaultCache(new CacheConfiguration().maxEntriesLocalHeap(10));
        CacheManager manager = CacheManager.create(config);
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
            Configuration config = new Configuration().cache(new CacheConfiguration("heap", 100));
            CacheManager manager = new CacheManager(config);
            try {
                Ehcache cache = manager.getCache("heap");
                cache.put(new Element("key123", "value"));
                Element element = cache.get("key123");
                assertNull("When the disabled property is set all puts should be discarded", element);

                cache.putQuiet(new Element("key1234", "value"));
                assertNull("When the disabled property is set all puts should be discarded", cache.get("key1234"));
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
        CacheManager manager = new CacheManager(new Configuration());
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
        Configuration config = new Configuration();
        CacheManager manager = CacheManager.create(config);
        try {
            assertEquals(Status.STATUS_ALIVE, manager.getStatus());
        } finally {
            manager.shutdown();
        }

        // check we can recreate the CacheManager on demand.
        manager = CacheManager.create(config);
        try {
            assertNotNull(manager);
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
        Configuration config = new Configuration().cache(new CacheConfiguration("foo", 100));
        CacheManager manager = new CacheManager(config);
        try {
            assertEquals(1, manager.getConfiguration().getCacheConfigurations().size());
            assertNotNull(manager.getCache("foo"));
            manager.removeCache("foo");
            assertNull(manager.getCache("foo"));

            assertEquals(0, manager.getConfiguration().getCacheConfigurations().size());

            // NPE tests
            manager.removeCache(null);
            manager.removeCache("");
            assertEquals(0, manager.getConfiguration().getCacheConfigurations().size());
        } finally {
            manager.shutdown();
        }
    }

    @Test
    public void testAddRemoveCache() throws CacheException {
        Configuration config = new Configuration().defaultCache(new CacheConfiguration().maxEntriesLocalHeap(0));
        CacheManager manager = new CacheManager(config);
        try {
            assertEquals(0, manager.getConfiguration().getCacheConfigurations().size());
            manager.addCache("test1");
            assertEquals(1, manager.getConfiguration().getCacheConfigurations().size());
            manager.removeAllCaches();
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
        Configuration config = new Configuration().defaultCache(new CacheConfiguration().maxEntriesLocalHeap(0));
        CacheManager manager = new CacheManager(config);
        try {
            assertEquals(0, manager.getConfiguration().getCacheConfigurations().size());
            manager.addCache("test");
            manager.addCache("test2");
            assertEquals(2, manager.getConfiguration().getCacheConfigurations().size());
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
            assertEquals(2, manager.getConfiguration().getCacheConfigurations().size());
        } finally {
            manager.shutdown();
        }
    }

    @Test
    public void testAddCacheIfAbsent() {
        Configuration config = new Configuration().defaultCache(new CacheConfiguration().maxEntriesLocalHeap(100));
        CacheManager manager = new CacheManager(config);
        try {
            manager.addCache("present");
            assertThat(manager.addCacheIfAbsent(new Cache(new CacheConfiguration("present", 1000))), sameInstance(manager.getEhcache("present")));

            Ehcache theCache = new Cache(new CacheConfiguration("absent", 1000));
            Ehcache cache = manager.addCacheIfAbsent(theCache);
            assertNotNull(cache);
            assertThat(cache, sameInstance(theCache));
            assertThat(cache.getName(), is("absent"));

            Ehcache other = new Cache(new CacheConfiguration(cache.getName(), 1000));
            Ehcache actual = manager.addCacheIfAbsent(other);
            assertThat(actual, notNullValue());
            assertThat(actual, not(sameInstance(other)));
            assertThat(actual, sameInstance(cache));

            Ehcache newCache = new Cache(new CacheConfiguration(cache.getName(), 1000));
            manager.removeCache(actual.getName());
            actual = manager.addCacheIfAbsent(newCache);
            assertThat(actual, notNullValue());
            assertThat(actual, not(sameInstance(cache)));
            assertThat(actual, sameInstance(newCache));

            assertThat(manager.addCacheIfAbsent(new Cache(new CacheConfiguration(actual.getName(), 1000))), sameInstance(actual));
            assertThat(manager.addCacheIfAbsent((Ehcache) null), nullValue());
        } finally {
            manager.shutdown();
        }
    }

    @Test
    public void testMultiThreadedAddCacheIfAbsent() throws InterruptedException, ExecutionException {
        final CacheManager manager = new CacheManager(new Configuration().name("testMultiThreadedAddCacheIfAbsent"));
        try {
            int parallelism = Runtime.getRuntime().availableProcessors();

            ExecutorService executor = Executors.newFixedThreadPool(parallelism);
            try {
                List<Future<Ehcache>> results = executor.invokeAll(Collections.nCopies(parallelism, new Callable<Ehcache>() {

                    @Override
                    public Ehcache call() throws Exception {
                        return manager.addCacheIfAbsent(new Cache(new CacheConfiguration().name("present").maxElementsInMemory(1000)));
                    }
                }));

                for (Future<Ehcache> result : results) {
                    Ehcache cache = result.get();
                    assertThat(cache, notNullValue());
                    assertThat(cache, sameInstance(results.get(0).get()));
                }
            } finally {
                executor.shutdown();
            }
        } finally {
            manager.shutdown();
        }
    }

    @Test
    public void testAddNamedCacheIfAbsent() {
        Configuration config = new Configuration().defaultCache(new CacheConfiguration().maxEntriesLocalHeap(100));
        CacheManager manager = new CacheManager(config);
        try {
            manager.addCache("present");
            Ehcache present = manager.getCache("present");
            Ehcache cache = manager.addCacheIfAbsent("present");
            assertThat(cache, notNullValue());
            assertThat(cache, sameInstance(present));
            assertThat(cache.getName(), is("present"));

            Ehcache actual = manager.addCacheIfAbsent("absent");
            assertThat(actual, notNullValue());
            assertThat(actual, sameInstance(manager.getEhcache(actual.getName())));
            assertThat(actual.getName(), is("absent"));
            assertThat(manager.addCacheIfAbsent(actual.getName()), sameInstance(actual));

            assertThat(manager.addCacheIfAbsent(new Cache(new CacheConfiguration(actual.getName(), 1000))), sameInstance(actual));
            assertThat(manager.addCacheIfAbsent((String) null), nullValue());
            assertThat(manager.addCacheIfAbsent(""), nullValue());
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
        Configuration config = new Configuration();
        config.defaultCache(new CacheConfiguration().maxEntriesLocalHeap(100)
                .cacheEventListenerFactory(new CacheEventListenerFactoryConfiguration().className(CountingCacheEventListenerFactory.class.getName())));
        CacheManager manager = new CacheManager(config);
        try {
            manager.addCache("test");
            Ehcache cache = manager.getCache("test");
            assertNotNull(cache);
            assertEquals("test", cache.getName());

            Set listeners = cache.getCacheEventNotificationService()
                    .getCacheEventListeners();
            assertEquals(1, listeners.size());
            for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
                CacheEventListener cacheEventListener = (CacheEventListener) iterator.next();
                assertTrue(cacheEventListener instanceof CountingCacheEventListener);
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
        Configuration config = new Configuration();
        config.defaultCache(new CacheConfiguration().maxEntriesLocalHeap(100)
                .cacheEventListenerFactory(new CacheEventListenerFactoryConfiguration().className(CountingCacheEventListenerFactory.class.getName())));
        CacheManager manager = new CacheManager(config);
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
        Configuration config = new Configuration();
        config.addDefaultCache(new CacheConfiguration().maxEntriesLocalHeap(10)
                .bootstrapCacheLoaderFactory(new BootstrapCacheLoaderFactoryConfiguration()
                .className(DummyBootstrapCacheLoaderFactory.class.getName())));
        CacheManager manager = new CacheManager(config);
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

            assertThat(bootstrapCacheLoader1, instanceOf(DummyBootstrapCacheLoader.class));
            assertThat(bootstrapCacheLoader2, instanceOf(DummyBootstrapCacheLoader.class));
        } finally {
            manager.shutdown();
        }
    }

    public static class DummyBootstrapCacheLoaderFactory extends BootstrapCacheLoaderFactory<BootstrapCacheLoader> {

        @Override
        public BootstrapCacheLoader createBootstrapCacheLoader(Properties properties) {
            return new DummyBootstrapCacheLoader();
        }

    }

    static class DummyBootstrapCacheLoader implements BootstrapCacheLoader {

        @Override
        public void load(Ehcache cache) throws CacheException {
            //no-op
        }

        @Override
        public boolean isAsynchronous() {
            return false;
        }

        @Override
        public DummyBootstrapCacheLoader clone() {
            return new DummyBootstrapCacheLoader();
        }
    }

    /**
     * Does clone work ok?
     */
    @Test
    public void testCachesCreatedFromDefaultDoNotInteract() {
        Configuration config = new Configuration();
        config.defaultCache(new CacheConfiguration().maxEntriesLocalHeap(100));
        CacheManager manager = new CacheManager(config);
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
        Configuration config = new Configuration();
        config.defaultCache(new CacheConfiguration().maxEntriesLocalHeap(100));
        CacheManager manager = new CacheManager(config);
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
        Configuration config = new Configuration();
        config.cache(new CacheConfiguration("test", 10));
        CacheManager manager = new CacheManager(config);
        try {
            Ehcache cache = manager.getEhcache("test");
            // decorate and substitute
            Ehcache blockingCache = new BlockingCache(cache);
            manager.replaceCacheWithDecoratedCache(cache, blockingCache);
            assertThat(manager.getCache("test"), nullValue());
            assertThat(manager.getEhcache("test"), sameInstance(blockingCache));
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Shows that a decorated cache can be substituted
     */
    @Test
    public void testDecoratorFailsIfUnderlyingCacheNotSame() {
        Configuration config = new Configuration();
        config.cache(new CacheConfiguration("test1", 10));
        config.cache(new CacheConfiguration("test2", 10));
        CacheManager manager = new CacheManager(config);
        try {
            Ehcache cache1 = manager.getEhcache("test1");
            Ehcache cache2 = manager.getEhcache("test2");
            // decorate and substitute
            BlockingCache blockingCache = new BlockingCache(cache2);
            try {
                manager.replaceCacheWithDecoratedCache(cache1, blockingCache);
                fail();
            } catch (CacheException e) {
                // expected
            }
            assertThat(manager.getCache("test1"), sameInstance(cache1));
        } finally {
            manager.shutdown();
        }
    }

    @Test
    public void testDecoratorFailsIfUnderlyingCacheHasChanged() {
        Configuration config = new Configuration();
        config.defaultCache(new CacheConfiguration().maxEntriesLocalHeap(10));
        config.cache(new CacheConfiguration("test", 10));
        CacheManager manager = new CacheManager(config);
        try {
            Ehcache cache = manager.getEhcache("test");
            manager.removeCache("test");
            manager.addCache("test");
            // decorate and substitute
            BlockingCache blockingCache = new BlockingCache(cache);
            try {
                manager.replaceCacheWithDecoratedCache(cache, blockingCache);
                fail("Expected CacheException");
            } catch (CacheException e) {
                // expected
            }
            assertFalse(manager.getEhcache("test") instanceof BlockingCache);
        } finally {
            manager.shutdown();
        }
    }

    @Test
    public void testDecoratorFailsIfUnderlyingCacheIsNotPresent() {
        Configuration config = new Configuration();
        config.cache(new CacheConfiguration("test", 10));
        CacheManager manager = new CacheManager(config);
        try {
            Ehcache cache = manager.getEhcache("test");
            manager.removeCache("test");
            // decorate and substitute
            try {
                manager.replaceCacheWithDecoratedCache(cache, new BlockingCache(cache));
                fail("This should throw an exception!");
            } catch (CacheException e) {
                // expected
            }
            assertThat(manager.getEhcache("test"), nullValue());
            assertThat(manager.getCache("test"), nullValue());
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
        CacheManager managerOne = new CacheManager(makeCacheManagerConfig("cm1"));
        try {
            new CacheManager(makeCacheManagerConfig("cm2")).shutdown();
        } finally {
            managerOne.shutdown();
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
     * I have suggested that people can rely on the thread names to change
     * priorities etc. The names should stay fixed.
     */
    @Test
    public void testUnnamedCacheManagerDefaultName() {
        Configuration config = new Configuration();
        CacheManager manager = new CacheManager(config);
        try {
            Assert.assertNotNull(CacheManager.getCacheManager(manager.getName()));
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Tests that the CacheManager implements clearAll():void and clearAllStartingWith(String):void properly
     */
    @Test
    public void testClearCacheManager() throws CacheException {
        Configuration config = new Configuration();
        config.cache(new CacheConfiguration("foo", 10));
        config.cache(new CacheConfiguration("foobar", 10));
        config.cache(new CacheConfiguration("bar", 10));

        CacheManager manager = new CacheManager(config);
        try {
            Ehcache foo = manager.getCache("foo");
            Ehcache foobar = manager.getCache("foobar");
            Ehcache bar = manager.getCache("bar");

            foo.put(new Element("key1", "value"));
            foobar.put(new Element("key2", "value"));
            bar.put(new Element("key3", "value"));

            assertThat(foo.getSize(), is(1));
            assertThat(foobar.getSize(), is(1));
            assertThat(bar.getSize(), is(1));

            manager.clearAllStartingWith("");

            assertThat(foo.getSize(), is(1));
            assertThat(foobar.getSize(), is(1));
            assertThat(bar.getSize(), is(1));

            manager.clearAllStartingWith("foo");

            assertThat(foo.getSize(), is(0));
            assertThat(foobar.getSize(), is(0));
            assertThat(bar.getSize(), is(1));

            manager.clearAll();

            assertThat(foo.getSize(), is(0));
            assertThat(foobar.getSize(), is(0));
            assertThat(bar.getSize(), is(0));
        } finally {
            manager.shutdown();
        }
    }

    @Test
    public void testStrategyNoneDoesntRequireDiskPath() {
       Configuration config = new Configuration();
       config.addCache(new CacheConfiguration().name("foo")
          .maxEntriesLocalHeap(1000)
          .persistence(new PersistenceConfiguration().strategy(Strategy.NONE)));

       CacheManager manager = new CacheManager(config);
       try {
            ConfigurationHelper helper = new ConfigurationHelper(manager, config);
            Assert.assertThat(helper.numberOfCachesThatUseDiskStorage(), is(0));
        } finally {
            manager.shutdown();
        }
    }


   public static final String STATISTIC_THREAD_PROPERTY  = "net.sf.ehcache.CacheManager.statisticsExecutor.poolSize";
   public static final String STATISTIC_THREAD_NAME      = "Statistics Thread";

   @Test
   public void testOverrideStatisticsThreadCount() throws InterruptedException {
      _testArbitraryStatThreadCount(3,5,null);
      _testArbitraryStatThreadCount(3,5,"2");
      _testArbitraryStatThreadCount(3,5,"4");
      _testArbitraryStatThreadCount(3,5,"6");
      _testArbitraryStatThreadCount(3,4,"7");
      System.getProperties().remove(STATISTIC_THREAD_PROPERTY);
   }

   private void _testArbitraryStatThreadCount(int cmCount, int cCount, String setting) throws InterruptedException {

      if(setting==null) {
         System.getProperties().remove(STATISTIC_THREAD_PROPERTY);
      } else {
         System.getProperties().put(STATISTIC_THREAD_PROPERTY, setting);
      }

      for (Thread t : getStatisticThreads()) {
        t.join();
      }

      CacheManager[] managers=new CacheManager[cmCount];
      for(int i=0;i< cmCount;i++) {
         Configuration config = new Configuration();
         config.setName("tcm"+i);
         for(int n=0;n<cCount;n++) {
            CacheConfiguration conf=new CacheConfiguration().name("foo"+n)
               .maxEntriesLocalHeap(1000)
               .persistence(new PersistenceConfiguration().strategy(Strategy.NONE));
            config.addCache(conf);
         }
         CacheManager manager = managers[i] = new CacheManager(config);
         for(String s:manager.getCacheNames()) {
            if(s.startsWith("foo")) {
               Ehcache c=manager.getCache(s);
               // turn stats on to get some activity in the pool
               // this gets the threads allocated
               c.getStatistics().getExtended().setAlwaysOn(true);
            }
         }
      }
      Set<Thread> postThreads = getStatisticThreads();
      if(setting == null) {
         Assert.assertThat(postThreads, IsCollectionWithSize.hasSize(cmCount));
      } else {
         Assert.assertThat(postThreads, IsCollectionWithSize.hasSize(cmCount * Integer.parseInt(setting)));
      }

      for(CacheManager cm:managers) {
         cm.shutdown();
      }
      for (Thread t : postThreads) {
         t.join();
      }
   }

    @Test
    public void testClusteredInstanceFactoryAccessor() throws Exception {
        /*
        This test makes sure that the ClusteredInstanceFactoryAccessor class
        used by the management agent doesn't break since it relies on this reflection code.
        If you break this test, make sure you also check ClusteredInstanceFactoryAccessorTest.
         */
        CacheManager manager = new CacheManager(new Configuration());
        try {
            Field field = CacheManager.class.getDeclaredField("terracottaClient");
            field.setAccessible(true);
            TerracottaClient terracottaClient = (TerracottaClient)field.get(manager);
            terracottaClient.getClusteredInstanceFactory();
        } finally {
            manager.shutdown();
        }
    }

    private static Set<Thread> getStatisticThreads() {
      Set<Thread> threads = new HashSet<Thread>();
      for (Thread thread : Thread.getAllStackTraces().keySet()) {
         if (thread.getName().startsWith(STATISTIC_THREAD_NAME)) {
            threads.add(thread);
         }
      }
      return threads;
   }

    @Test
    public void testRuntimeConfigRemovalDuringShutdown() throws Exception {
        Configuration configuration =  new Configuration();
        CacheManager cacheManager = new CacheManager(configuration);
        cacheManager.shutdown();
        Configuration.RuntimeCfg runtimeCfg = configuration.setupFor(cacheManager, "expecting CacheManager name in RuntimeCfg to be this value");
        assertTrue(runtimeCfg.getCacheManagerName().equals("expecting CacheManager name in RuntimeCfg to be this value"));
    }
}

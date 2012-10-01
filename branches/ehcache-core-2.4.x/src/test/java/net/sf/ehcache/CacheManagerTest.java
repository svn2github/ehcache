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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.lessThanOrEqualTo;
import static org.hamcrest.collection.IsArrayContainingInOrder.arrayContaining;
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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.config.generator.ConfigurationUtil;
import net.sf.ehcache.constructs.blocking.BlockingCache;
import net.sf.ehcache.distribution.JVMUtil;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.CountingCacheEventListener;
import net.sf.ehcache.event.CountingCacheEventListenerFactory;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.statistics.LiveCacheStatisticsData;
import net.sf.ehcache.store.DiskStore;
import net.sf.ehcache.store.Store;

import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.CombinableMatcher;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
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

    @Test
    public void testCacheReferenceLookUps() {
        CacheManager manager = new CacheManager(new Configuration());
        try {
            String cacheName = "randomNewCache";
            manager.addCache(new Cache(new CacheConfiguration().name(cacheName).maxElementsInMemory(1000)));

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
                .cache(new CacheConfiguration("foo", 100).overflowToDisk(true));

        Configuration configTwo = new Configuration().name("two")
                .diskStore(new DiskStoreConfiguration().path("target/CacheManagerTest/testCreateTwoCacheManagersWithSamePath"))
                .cache(new CacheConfiguration("foo", 100).overflowToDisk(true));

        CacheManager managerOne = new CacheManager(configOne);
        try {
            CacheManager managerTwo = new CacheManager(configTwo);
            try {
                String intialDiskStorePath = "target/CacheManagerTest/testCreateTwoCacheManagersWithSamePath";

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
        Element element1 = new Element(Integer.toString(1), new Date());
        Element element2 = new Element(Integer.toString(2), new Date());

        Configuration configOne = new Configuration().name("one");
        configOne.diskStore(new DiskStoreConfiguration().path("java.io.tmpdir/CacheManagerTest/testTwoCacheManagers/one"));
        configOne.addCache(new CacheConfiguration("test", 100).overflowToDisk(true));
        CacheManager managerOne = new CacheManager(configOne);
        try {
            Cache cacheOne = managerOne.getCache("test");
            cacheOne.put(element1);

            // Check can start second one with a different disk path
            Configuration configTwo = new Configuration().name("two");
            configTwo.diskStore(new DiskStoreConfiguration().path("java.io.tmpdir/CacheManagerTest/testTwoCacheManagers/two"));
            configTwo.addCache(new CacheConfiguration("test", 100).overflowToDisk(true));
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
        config.diskStore(new DiskStoreConfiguration().path("java.io.tmpdir/CacheManagerTest/testForCacheManagerThreadLeak"));
        config.cache(new CacheConfiguration("heap", 100));
        config.cache(new CacheConfiguration("disk", 100).maxElementsOnDisk(1000).overflowToDisk(true));
        config.cache(new CacheConfiguration("persistent", 100).maxElementsOnDisk(1000).overflowToDisk(true).diskPersistent(true));
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
     */
    @Test
    public void testCacheManagerThreads() throws CacheException,
            InterruptedException {
        final Collection<Thread> initialThreads = Collections.unmodifiableCollection(JVMUtil.enumerateThreads());
        Configuration config = new Configuration().diskStore(new DiskStoreConfiguration().path("java.io.tmpdir/CacheManagerTest/testCacheManagerThreads"));
        for (int i = 0; i < 70; i++) {
            config.cache(new CacheConfiguration().name(Integer.toString(i)).maxElementsInMemory(100).maxElementsOnDisk(1000)
                    .overflowToDisk(true));
        }
        CacheManager manager = new CacheManager(config);
        try {
            Collection<Thread> spawnedThreads = JVMUtil.enumerateThreads();
            spawnedThreads.removeAll(initialThreads);
            assertThat("Spawned Threads", spawnedThreads, hasSize(CombinableMatcher.<Integer>both(greaterThan(0)).and(lessThanOrEqualTo(manager.getCacheNames().length + 2))));
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
        Configuration config = new Configuration().defaultCache(new CacheConfiguration().maxElementsInMemory(10));
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
        Configuration config = new Configuration().defaultCache(new CacheConfiguration().maxElementsInMemory(10));
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
        Configuration config = new Configuration().defaultCache(new CacheConfiguration().maxElementsInMemory(0));
        CacheManager manager = new CacheManager(config);
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
        Configuration config = new Configuration().defaultCache(new CacheConfiguration().maxElementsInMemory(0));
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
        Configuration config = new Configuration().defaultCache(new CacheConfiguration().maxElementsInMemory(100));
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
        Configuration config = new Configuration().defaultCache(new CacheConfiguration().maxElementsInMemory(100));
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
        config.defaultCache(new CacheConfiguration().maxElementsInMemory(100)
                .cacheEventListenerFactory(new CacheEventListenerFactoryConfiguration().className(CountingCacheEventListenerFactory.class.getName())));
        CacheManager manager = new CacheManager(config);
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
                assertTrue(cacheEventListener instanceof CountingCacheEventListener
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
        Configuration config = new Configuration();
        config.defaultCache(new CacheConfiguration().maxElementsInMemory(100)
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
        config.addDefaultCache(new CacheConfiguration().maxElementsInMemory(10)
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

    public static class DummyBootstrapCacheLoaderFactory extends BootstrapCacheLoaderFactory {

        @Override
        public BootstrapCacheLoader createBootstrapCacheLoader(Properties properties) {
            return new DummyBootstrapCacheLoader();
        }

    }

    static class DummyBootstrapCacheLoader implements BootstrapCacheLoader {

        public void load(Ehcache cache) throws CacheException {
            //no-op
        }

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
        config.defaultCache(new CacheConfiguration().maxElementsInMemory(100));
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
        config.defaultCache(new CacheConfiguration().maxElementsInMemory(100));
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
                assertEquals("The test Cache is not alive.", e.getMessage());
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
        config.defaultCache(new CacheConfiguration().maxElementsInMemory(10));
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
        CacheManager managerOne = new CacheManager(makeCacheManagerConfig());
        try {
            new CacheManager(makeCacheManagerConfig()).shutdown();
        } finally {
            managerOne.shutdown();
        }
    }

    private static Configuration makeCacheManagerConfig() {
        Configuration config = new Configuration();
        CacheConfiguration defaults = new CacheConfiguration("cacheName", 10)
                .eternal(true);
        config.setDefaultCacheConfiguration(defaults);
        return config;
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

}

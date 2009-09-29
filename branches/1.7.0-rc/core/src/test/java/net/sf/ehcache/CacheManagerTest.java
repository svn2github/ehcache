/**
 *  Copyright 2003-2009 Terracotta, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.ehcache.bootstrap.BootstrapCacheLoader;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.constructs.blocking.BlockingCache;
import net.sf.ehcache.constructs.blocking.CountingCacheEntryFactory;
import net.sf.ehcache.constructs.blocking.SelfPopulatingCache;
import net.sf.ehcache.distribution.JVMUtil;
import net.sf.ehcache.distribution.RMIAsynchronousCacheReplicator;
import net.sf.ehcache.distribution.RMIBootstrapCacheLoader;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.statistics.LiveCacheStatisticsData;
import net.sf.ehcache.store.DiskStore;
import net.sf.ehcache.store.Store;

import org.junit.After;
import org.junit.Test;

/**
 * Tests for CacheManager
 * 
 * @author Greg Luck
 * @version $Id$
 */
public class CacheManagerTest {

    private static final Logger LOG = Logger.getLogger(CacheManagerTest.class
            .getName());

    /**
     * the CacheManager Singleton instance
     */
    protected CacheManager singletonManager;

    /**
     * a CacheManager which is created as an instance
     */
    protected CacheManager instanceManager;

    /**
     * Shutdown managers. Check that the manager is removed from
     * CacheManager.ALL_CACHE_MANAGERS
     */
    @After
    public void tearDown() throws Exception {
        if (singletonManager != null) {
            if (singletonManager.getStatus().equals(Status.STATUS_ALIVE)) {
                assertTrue(CacheManager.ALL_CACHE_MANAGERS
                        .contains(singletonManager));
            }
            singletonManager.shutdown();
            assertFalse(CacheManager.ALL_CACHE_MANAGERS
                    .contains(singletonManager));
        }
        if (instanceManager != null) {
            if (instanceManager.getStatus().equals(Status.STATUS_ALIVE)) {
                assertTrue(CacheManager.ALL_CACHE_MANAGERS
                        .contains(instanceManager));
            }
            instanceManager.shutdown();
            assertFalse(CacheManager.ALL_CACHE_MANAGERS
                    .contains(instanceManager));
        }
    }

    /**
     * Tests that the CacheManager was successfully created
     */
    @Test
    public void testCreateCacheManager() throws CacheException {
        singletonManager = CacheManager.create();
        assertNotNull(singletonManager);
        assertEquals(13, singletonManager.getCacheNames().length);
    }

    /**
     * Tests that the CacheManager was successfully created
     */
    @Test
    public void testCreateCacheManagerFromFile() throws CacheException {
        singletonManager = CacheManager.create(AbstractCacheTest.SRC_CONFIG_DIR
                + "ehcache.xml");
        assertNotNull(singletonManager);
        assertEquals(6, singletonManager.getCacheNames().length);
    }

    /**
     * Tests that the CacheManager was successfully created from a Configuration
     */
    @Test
    public void testCreateCacheManagerFromConfiguration() throws CacheException {
        File file = new File(AbstractCacheTest.SRC_CONFIG_DIR + "ehcache.xml");
        Configuration configuration = ConfigurationFactory
                .parseConfiguration(file);
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
        URL secondCacheConfiguration = this.getClass().getResource(
                "/ehcache-2.xml");

        singletonManager = CacheManager.create(secondCacheConfiguration);
        instanceManager = new CacheManager(secondCacheConfiguration);

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
        URL secondCacheConfiguration = this.getClass().getResource(
                "/ehcache-2.xml");
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

        // Check can start second one with the same config
        instanceManager = new CacheManager(fileName);
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
        instanceManager = new CacheManager(fileName);
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
     * <p />
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

        URL secondCacheConfiguration = this.getClass().getResource(
                "/ehcache-2.xml");
        instanceManager = new CacheManager(secondCacheConfiguration);
        instanceManager.shutdown();

        // shutting down instance should leave singleton ok
        assertEquals(13, singletonManager.getCacheNames().length);

        instanceManager = new CacheManager(secondCacheConfiguration);
        assertNotNull(instanceManager);
        assertEquals(8, instanceManager.getCacheNames().length);

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

        Configuration configuration = new Configuration();
        assertNotNull(configuration);

        CacheConfiguration defaultCacheConfiguration = new CacheConfiguration();
        defaultCacheConfiguration.setEternal(false);
        defaultCacheConfiguration.setName("defaultCache");
        configuration.addDefaultCache(defaultCacheConfiguration);

        DiskStoreConfiguration diskStoreConfiguration = new DiskStoreConfiguration();
        diskStoreConfiguration.setPath("java.io.tmpdir");
        configuration.addDiskStore(diskStoreConfiguration);

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
        assertEquals(13, singletonManager.getCacheNames().length);
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
        Ehcache cache = singletonManager.getCache("sampleCache1");
        assertNotNull(cache);
        singletonManager.removeCache("sampleCache1");
        cache = singletonManager.getCache("sampleCache1");
        assertNull(cache);

        // NPE tests
        singletonManager.removeCache(null);
        singletonManager.removeCache("");
    }

    /**
     * Tests adding a new cache with default config
     */
    @Test
    public void testAddCache() throws CacheException {
        singletonManager = CacheManager.create();
        singletonManager.addCache("test");
        singletonManager.addCache("test2");
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

        Store diskStore1 = cache1.getDiskStore();
        Store diskStore2 = cache2.getDiskStore();
        assertTrue(diskStore1 != diskStore2);

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
            assertEquals("The test Cache is not alive.", e.getMessage());
        }
    }

    /**
     * Tests that we can run 69 caches, most with disk stores, with no ill
     * effects
     * <p/>
     * Check that this is fast.
     */
    @Test
    public void testCreateCacheManagerWithManyCaches() throws CacheException,
            InterruptedException {
        singletonManager = CacheManager
                .create(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-big.xml");
        assertNotNull(singletonManager);
        assertEquals(69, singletonManager.getCacheNames().length);

        String[] names = singletonManager.getCacheNames();
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            Ehcache cache = singletonManager.getCache(name);
            for (int j = 0; i < 100; i++) {
                cache.put(new Element(new Integer(j), "value"));
            }
        }
        StopWatch stopWatch = new StopWatch();
        for (int repeats = 0; repeats < 5000; repeats++) {
            for (int i = 0; i < names.length; i++) {
                String name = names[i];
                Ehcache cache = singletonManager.getCache(name);
                for (int j = 0; i < 100; i++) {
                    Element element = cache.get(name + j);
                    if ((element == null)) {
                        cache.put(new Element(new Integer(j), "value"));
                    }
                }
            }
        }
        long elapsedTime = stopWatch.getElapsedTime();
        LOG.log(Level.INFO, "Time taken was: " + elapsedTime);
        assertTrue("Time taken was: " + elapsedTime, elapsedTime < 5000);
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
        blockingCache.get("unknownkey");
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
        managers[0] = new CacheManager(makeCacheManagerConfig());
        managers[1] = new CacheManager(makeCacheManagerConfig());

        managers[0].shutdown();
        managers[1].shutdown();

    }

    private static Configuration makeCacheManagerConfig() {
        Configuration config = new Configuration();
        CacheConfiguration defaults = new CacheConfiguration();
        defaults.setEternal(true);
        defaults.setDiskPersistent(false);
        defaults.setOverflowToDisk(false);
        defaults.setMaxElementsInMemory(10);
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
     * Ehcache 1.5 allows the diskStore element to be optional. Check that is is
     * null
     */
    @Test
    public void testCacheManagerWithNoDiskCachesFromConfiguration()
            throws CacheException, InterruptedException {
        singletonManager = CacheManager
                .create(AbstractCacheTest.TEST_CONFIG_DIR
                        + "ehcache-nodisk.xml");
        assertEquals(null, singletonManager.getDiskStorePath());
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
            LOG.log(Level.INFO, name);
        }
    }

}

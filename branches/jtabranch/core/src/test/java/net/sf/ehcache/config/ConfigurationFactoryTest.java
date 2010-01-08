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


package net.sf.ehcache.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Matcher;

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.bootstrap.BootstrapCacheLoader;
import net.sf.ehcache.distribution.CacheManagerPeerListener;
import net.sf.ehcache.distribution.CacheManagerPeerProvider;
import net.sf.ehcache.distribution.MulticastRMICacheManagerPeerProvider;
import net.sf.ehcache.distribution.RMIAsynchronousCacheReplicator;
import net.sf.ehcache.distribution.RMIBootstrapCacheLoader;
import net.sf.ehcache.distribution.RMICacheManagerPeerListener;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.CacheManagerEventListener;
import net.sf.ehcache.event.CountingCacheEventListener;
import net.sf.ehcache.event.CountingCacheManagerEventListener;
import net.sf.ehcache.exceptionhandler.CacheExceptionHandler;
import net.sf.ehcache.exceptionhandler.CountingExceptionHandler;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for Store Configuration
 * <p/>
 * Make sure ant compile has been executed before running these tests, as they rely on the test ehcache.xml being
 * in the classpath.
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public class ConfigurationFactoryTest extends AbstractCacheTest {
    private static final int CACHES_IN_TEST_EHCACHE = 13;

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationFactoryTest.class.getName());


    /**
     * setup test
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        manager.removalAll();
    }

    /**
     * Tests that the loader successfully loads from ehcache.xml.
     * ehcache.xml should be found in the classpath. In our ant configuration
     * this should be from build/test-classes/ehcache.xml
     * <p/>
     * <defaultCache
     * maxElementsInMemory="10000"
     * eternal="false"
     * timeToIdleSeconds="3600"
     * timeToLiveSeconds="10"
     * overflowToDisk="true"
     * />
     */
    @Test
    public void testLoadConfigurationFromClasspath() throws Exception {

        Configuration configuration = ConfigurationFactory.parseConfiguration();
        ConfigurationHelper configurationHelper = new ConfigurationHelper(manager, configuration);

        //Check core attributes
        assertEquals(null, configurationHelper.getConfigurationBean().getName());
        assertEquals(true, configurationHelper.getConfigurationBean().getUpdateCheck());
        assertEquals(Configuration.Monitoring.AUTODETECT, configurationHelper.getConfigurationBean().getMonitoring());

        //Check disk store
        assertEquals(System.getProperty("java.io.tmpdir"), configurationHelper.getDiskStorePath());


        //Check CacheManagerPeerProvider
        Map<String, CacheManagerPeerProvider> peerProviders = configurationHelper.createCachePeerProviders();
        CacheManagerPeerProvider peerProvider = peerProviders.get("RMI");


        //Check TTL
        assertTrue(peerProvider instanceof MulticastRMICacheManagerPeerProvider);
        assertEquals(Integer.valueOf(0), ((MulticastRMICacheManagerPeerProvider) peerProvider).getHeartBeatSender().getTimeToLive());

        //Check CacheManagerEventListener
        assertEquals(null, configurationHelper.createCacheManagerEventListener());

        //Check default cache
        Ehcache defaultCache = configurationHelper.createDefaultCache();
        assertEquals("default", defaultCache.getName());
        assertEquals(false, defaultCache.getCacheConfiguration().isEternal());
        assertEquals(5, defaultCache.getCacheConfiguration().getTimeToIdleSeconds());
        assertEquals(10, defaultCache.getCacheConfiguration().getTimeToLiveSeconds());
        assertEquals(true, defaultCache.getCacheConfiguration().isOverflowToDisk());

        //Check caches
        assertEquals(CACHES_IN_TEST_EHCACHE, configurationHelper.createCaches().size());

        /*
        <cache name="sampleCache1"
        maxElementsInMemory="10000"
        eternal="false"
        timeToIdleSeconds="360"
        timeToLiveSeconds="1000"
        overflowToDisk="true"
        />
        */
        Ehcache sampleCache1 = configurationHelper.createCacheFromName("sampleCache1");
        assertEquals("sampleCache1", sampleCache1.getName());
        assertEquals(false, sampleCache1.getCacheConfiguration().isEternal());
        assertEquals(360, sampleCache1.getCacheConfiguration().getTimeToIdleSeconds());
        assertEquals(1000, sampleCache1.getCacheConfiguration().getTimeToLiveSeconds());
        assertEquals(true, sampleCache1.getCacheConfiguration().isOverflowToDisk());
        assertEquals(1000, sampleCache1.getCacheConfiguration().getMaxElementsOnDisk());

        /** A cache which overflows to disk. The disk store is persistent
         between cache and VM restarts. The disk expiry thread interval is set to 10 minutes, overriding
         the default of 2 minutes.
         <cache name="persistentLongExpiryIntervalCache"
         maxElementsInMemory="500"
         eternal="false"
         timeToIdleSeconds="300"
         timeToLiveSeconds="600"
         overflowToDisk="true"
         diskPersistent="true"
         diskExpiryThreadIntervalSeconds="600"
         /> */
        Ehcache persistentLongExpiryIntervalCache = configurationHelper.createCacheFromName("persistentLongExpiryIntervalCache");
        assertEquals("persistentLongExpiryIntervalCache", persistentLongExpiryIntervalCache.getName());
        assertEquals(false, persistentLongExpiryIntervalCache.getCacheConfiguration().isEternal());
        assertEquals(300, persistentLongExpiryIntervalCache.getCacheConfiguration().getTimeToIdleSeconds());
        assertEquals(600, persistentLongExpiryIntervalCache.getCacheConfiguration().getTimeToLiveSeconds());
        assertEquals(true, persistentLongExpiryIntervalCache.getCacheConfiguration().isOverflowToDisk());
        assertEquals(true, persistentLongExpiryIntervalCache.getCacheConfiguration().isDiskPersistent());
        assertEquals(600, persistentLongExpiryIntervalCache.getCacheConfiguration().getDiskExpiryThreadIntervalSeconds());

        /*
           <!--
            A cache which has a CacheExtension
            -->
            <cache name="testCacheExtensionCache"
                   maxElementsInMemory="10"
                   eternal="false"
                   timeToIdleSeconds="100"
                   timeToLiveSeconds="100"
                   overflowToDisk="false">
                <cacheExtensionFactory
                        class="net.sf.ehcache.extension.TestCacheExtensionFactory"
                        properties="propertyA=valueA"/>
            </cache>CacheExtension cache
        */
        Ehcache exceptionHandlingCache = configurationHelper.createCacheFromName("exceptionHandlingCache");
        assertEquals("exceptionHandlingCache", exceptionHandlingCache.getName());
        assertTrue(exceptionHandlingCache.getCacheExceptionHandler() != null);
        assertTrue(exceptionHandlingCache.getCacheExceptionHandler() instanceof CountingExceptionHandler);
        assertTrue(exceptionHandlingCache.getCacheExceptionHandler() instanceof CacheExceptionHandler);
    }


    /**
     * Tests that the loader successfully loads from ehcache.xml
     * given as a {@link File}
     * <p/>
     * <defaultCache
     * maxElementsInMemory="10000"
     * eternal="false"
     * timeToIdleSeconds="120"
     * timeToLiveSeconds="120"
     * overflowToDisk="true"
     * />
     */
    @Test
    public void testLoadConfigurationFromFile() throws Exception {

        File file = new File(SRC_CONFIG_DIR + "ehcache.xml");
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        ConfigurationHelper configurationHelper = new ConfigurationHelper(manager, configuration);

        assertEquals(null, configurationHelper.getConfigurationBean().getName());
        assertEquals(true, configurationHelper.getConfigurationBean().getUpdateCheck());
        assertEquals(Configuration.Monitoring.AUTODETECT, configurationHelper.getConfigurationBean().getMonitoring());

        //Check disk store  <diskStore path="/tmp"/>
        assertEquals(System.getProperty("java.io.tmpdir"), configurationHelper.getDiskStorePath());

        //Check CacheManagerPeerProvider
        Map<String, CacheManagerPeerProvider> peerProviders = configurationHelper.createCachePeerProviders();
        CacheManagerPeerProvider peerProvider = peerProviders.get("RMI");


        //Check TTL
        assertTrue(peerProvider instanceof MulticastRMICacheManagerPeerProvider);
        assertEquals(Integer.valueOf(1), ((MulticastRMICacheManagerPeerProvider) peerProvider).getHeartBeatSender().getTimeToLive());


        //Check default cache
        Ehcache defaultCache = configurationHelper.createDefaultCache();
        assertEquals("default", defaultCache.getName());
        assertEquals(false, defaultCache.getCacheConfiguration().isEternal());
        assertEquals(120, defaultCache.getCacheConfiguration().getTimeToIdleSeconds());
        assertEquals(120, defaultCache.getCacheConfiguration().getTimeToLiveSeconds());
        assertEquals(true, defaultCache.getCacheConfiguration().isOverflowToDisk());
        assertEquals(10000, defaultCache.getCacheConfiguration().getMaxElementsInMemory());
        assertEquals(10000000, defaultCache.getCacheConfiguration().getMaxElementsOnDisk());

        //Check caches
        assertEquals(6, configurationHelper.createCaches().size());

        //check config
        CacheConfiguration sampleCache1Config = (CacheConfiguration) configuration.getCacheConfigurations().get("sampleCache1");
        assertEquals("sampleCache1", sampleCache1Config.getName());
        assertEquals(false, sampleCache1Config.isEternal());
        assertEquals(300, sampleCache1Config.getTimeToIdleSeconds());
        assertEquals(600, sampleCache1Config.getTimeToLiveSeconds());
        assertEquals(true, sampleCache1Config.isOverflowToDisk());
        assertEquals(20, sampleCache1Config.getDiskSpoolBufferSizeMB());

        //  <cache name="sampleCache1"
        //  maxElementsInMemory="10000"
        //  eternal="false"
        //  timeToIdleSeconds="300"
        //  timeToLiveSeconds="600"
        //  overflowToDisk="true"
        //  />
        //Check created cache
        Ehcache sampleCache1 = configurationHelper.createCacheFromName("sampleCache1");
        assertEquals("sampleCache1", sampleCache1.getName());
        assertEquals(false, sampleCache1.getCacheConfiguration().isEternal());
        assertEquals(300, sampleCache1.getCacheConfiguration().getTimeToIdleSeconds());
        assertEquals(1000, sampleCache1.getCacheConfiguration().getMaxElementsOnDisk());
        assertEquals(600, sampleCache1.getCacheConfiguration().getTimeToLiveSeconds());
        assertEquals(true, sampleCache1.getCacheConfiguration().isOverflowToDisk());
    }

    /**
     * Can we read from a UTF8 encoded file which uses Japanese characters
     */
    @Test
    public void testLoadUTF8ConfigurationFromFile() throws Exception {

        File file = new File(TEST_CONFIG_DIR + "ehcacheUTF8.xml");
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        ConfigurationHelper configurationHelper = new ConfigurationHelper(manager, configuration);
    }


    /**
     * Tests that the loader successfully loads from ehcache-1.1.xml
     * given as a {@link File}. This is a backward compatibility test.
     * <p/>
     * <defaultCache
     * maxElementsInMemory="10000"
     * eternal="false"
     * timeToIdleSeconds="120"
     * timeToLiveSeconds="120"
     * overflowToDisk="true"
     * />
     */
    @Test
    public void testLoadConfigurationFromEhcache11File() throws Exception {

        File file = new File(TEST_CONFIG_DIR + "ehcache-1_1.xml");
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        ConfigurationHelper configurationHelper = new ConfigurationHelper(manager, configuration);

        assertEquals(null, configurationHelper.getConfigurationBean().getName());
        assertEquals(true, configurationHelper.getConfigurationBean().getUpdateCheck());
        assertEquals(Configuration.Monitoring.AUTODETECT, configurationHelper.getConfigurationBean().getMonitoring());

        //Check disk path  <diskStore path="/tmp"/>
        assertEquals(System.getProperty("java.io.tmpdir"), configurationHelper.getDiskStorePath());

        //Check default cache
        Ehcache defaultCache = configurationHelper.createDefaultCache();
        assertEquals("default", defaultCache.getName());
        assertEquals(false, defaultCache.getCacheConfiguration().isEternal());
        assertEquals(5, defaultCache.getCacheConfiguration().getTimeToIdleSeconds());
        assertEquals(10, defaultCache.getCacheConfiguration().getTimeToLiveSeconds());
        assertEquals(true, defaultCache.getCacheConfiguration().isOverflowToDisk());
        assertEquals(10, defaultCache.getCacheConfiguration().getMaxElementsInMemory());
        assertEquals(0, defaultCache.getCacheConfiguration().getMaxElementsOnDisk());

        //Check caches
        assertEquals(8, configurationHelper.createCaches().size());

        //  <cache name="sampleCache1"
        //  maxElementsInMemory="10000"
        //  eternal="false"
        //  timeToIdleSeconds="300"
        //  timeToLiveSeconds="600"
        //  overflowToDisk="true"
        //  />
        Ehcache sampleCache1 = configurationHelper.createCacheFromName("sampleCache1");
        assertEquals("sampleCache1", sampleCache1.getName());
        assertEquals(false, sampleCache1.getCacheConfiguration().isEternal());
        assertEquals(360, sampleCache1.getCacheConfiguration().getTimeToIdleSeconds());
        assertEquals(1000, sampleCache1.getCacheConfiguration().getTimeToLiveSeconds());
        assertEquals(true, sampleCache1.getCacheConfiguration().isOverflowToDisk());
    }

    /**
     * Tests that the CacheManagerEventListener is null when
     * no CacheManagerEventListener class is specified.
     */
    @Test
    public void testLoadConfigurationFromFileNoCacheManagerListenerDefault() throws Exception {
        File file = new File(TEST_CONFIG_DIR + "ehcache-nolisteners.xml");
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        ConfigurationHelper configurationHelper = new ConfigurationHelper(manager, configuration);

        //Check CacheManagerEventListener
        CacheManagerEventListener listener = configurationHelper.createCacheManagerEventListener();
        assertEquals(null, listener);

        //Check caches. Configuration should have completed
        assertEquals(10, configurationHelper.createCaches().size());
    }

    /**
     * Tests that the CacheManagerEventListener class is set as the CacheManagerEventListener
     * when the class is unloadable.
     */
    @Test
    public void testLoadConfigurationFromFileUnloadableCacheManagerListenerDefault() throws Exception {
        File file = new File(TEST_CONFIG_DIR + "ehcache-unloadablecachemanagerlistenerclass.xml");
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        ConfigurationHelper configurationHelper = new ConfigurationHelper(manager, configuration);

        //Check CacheManagerEventListener
        CacheManagerEventListener listener = null;
        try {
            listener = configurationHelper.createCacheManagerEventListener();
            fail();
        } catch (CacheException e) {
            //expected
        }
    }

    /**
     * Positive and negative Tests for setting a list of CacheEventListeners in the configuration
     */
    @Test
    public void testLoadConfigurationFromFileCountingCacheListener() throws Exception {
        File file = new File(TEST_CONFIG_DIR + "ehcache-countinglisteners.xml");
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        ConfigurationHelper configurationHelper = new ConfigurationHelper(manager, configuration);

        //Check CacheManagerEventListener
        Class actualClass = configurationHelper.createCacheManagerEventListener().getClass();
        assertEquals(CountingCacheManagerEventListener.class, actualClass);

        //Check caches. Configuration should have completed
        assertEquals(10, configurationHelper.createCaches().size());

        //Should have null and counting
        Ehcache sampleCache1 = configurationHelper.createCacheFromName("sampleCache1");
        Set registeredListeners = sampleCache1.getCacheEventNotificationService().getCacheEventListeners();
        assertEquals(2, registeredListeners.size());

        //Should have null and counting
        Ehcache sampleCache2 = configurationHelper.createCacheFromName("sampleCache2");
        registeredListeners = sampleCache2.getCacheEventNotificationService().getCacheEventListeners();
        assertEquals(1, registeredListeners.size());

        //Should have null and counting
        Ehcache sampleCache3 = configurationHelper.createCacheFromName("sampleCache3");
        registeredListeners = sampleCache3.getCacheEventNotificationService().getCacheEventListeners();
        assertEquals(1, registeredListeners.size());

        //Should have none. None set.
        Ehcache footerPageCache = configurationHelper.createCacheFromName("FooterPageCache");
        registeredListeners = footerPageCache.getCacheEventNotificationService().getCacheEventListeners();
        assertEquals(0, registeredListeners.size());

        //Should have one. null listener set.
        Ehcache persistentLongExpiryIntervalCache = configurationHelper.createCacheFromName("persistentLongExpiryIntervalCache");
        registeredListeners = persistentLongExpiryIntervalCache.getCacheEventNotificationService()
                .getCacheEventListeners();
        assertEquals(1, registeredListeners.size());
    }

    /**
     * Tests for Distributed Cache config
     */
    @Test
    public void testLoadConfigurationFromFileDistribution() throws Exception {
        File file = new File(TEST_CONFIG_DIR + "distribution/ehcache-distributed1.xml");
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        ConfigurationHelper configurationHelper = new ConfigurationHelper(manager, configuration);

        //Check CacheManagerPeerProvider
        Map<String, CacheManagerPeerProvider> peerProviders = configurationHelper.createCachePeerProviders();
        CacheManagerPeerProvider peerProvider = peerProviders.get("RMI");


        //Check TTL
        assertTrue(peerProvider instanceof MulticastRMICacheManagerPeerProvider);
        assertEquals(Integer.valueOf(0), ((MulticastRMICacheManagerPeerProvider) peerProvider).getHeartBeatSender().getTimeToLive());


        //check CacheManagerPeerListener
        Map<String, CacheManagerPeerListener> peerListeners = configurationHelper.createCachePeerListeners();

        //should be one in this config
        for (CacheManagerPeerListener peerListener : peerListeners.values()) {
            assertTrue(peerListener instanceof RMICacheManagerPeerListener);
        }

        //Check caches. Configuration should have completed
        assertEquals(61, configurationHelper.createCaches().size());

        Ehcache sampleCache1 = configurationHelper.createCacheFromName("sampleCache1");
        Set listeners = sampleCache1.getCacheEventNotificationService().getCacheEventListeners();
        assertEquals(2, listeners.size());
        for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
            CacheEventListener cacheEventListener = (CacheEventListener) iterator.next();
            assertTrue(cacheEventListener instanceof RMIAsynchronousCacheReplicator || cacheEventListener
                    instanceof CountingCacheEventListener);
        }

        BootstrapCacheLoader bootstrapCacheLoader = sampleCache1.getBootstrapCacheLoader();
        assertNotNull(bootstrapCacheLoader);
        assertEquals(RMIBootstrapCacheLoader.class, bootstrapCacheLoader.getClass());
        assertEquals(true, bootstrapCacheLoader.isAsynchronous());
        assertEquals(5000000, ((RMIBootstrapCacheLoader) bootstrapCacheLoader).getMaximumChunkSizeBytes());

    }

    /**
     * The following should give defaults of true and 5000000
     * <bootstrapCacheLoaderFactory class="net.sf.ehcache.distribution.RMIBootstrapCacheLoaderFactory" />
     */
    @Test
    public void testLoadConfigurationFromFileNoBootstrapPropertiesSet() throws Exception {
        File file = new File(TEST_CONFIG_DIR + "distribution/ehcache-distributed1.xml");
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        ConfigurationHelper configurationHelper = new ConfigurationHelper(manager, configuration);
        Ehcache sampleCache3 = configurationHelper.createCacheFromName("sampleCache3");

        BootstrapCacheLoader bootstrapCacheLoader = ((Cache) sampleCache3).getBootstrapCacheLoader();
        assertEquals(true, bootstrapCacheLoader.isAsynchronous());
        assertEquals(5000000, ((RMIBootstrapCacheLoader) bootstrapCacheLoader).getMaximumChunkSizeBytes());
    }

    /**
     * The following should give defaults of true and 5000000
     * <bootstrapCacheLoaderFactory class="net.sf.ehcache.distribution.RMIBootstrapCacheLoaderFactory"
     * properties="bootstrapAsynchronously=false, maximumChunkSizeBytes=10000"/>
     */
    @Test
    public void testLoadConfigurationFromFileWithSpecificPropertiesSet() throws Exception {
        File file = new File(TEST_CONFIG_DIR + "distribution/ehcache-distributed1.xml");
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        ConfigurationHelper configurationHelper = new ConfigurationHelper(manager, configuration);
        Ehcache sampleCache4 = configurationHelper.createCacheFromName("sampleCache4");

        BootstrapCacheLoader bootstrapCacheLoader = ((Cache) sampleCache4).getBootstrapCacheLoader();
        assertEquals(false, bootstrapCacheLoader.isAsynchronous());
        assertEquals(10000, ((RMIBootstrapCacheLoader) bootstrapCacheLoader).getMaximumChunkSizeBytes());
    }

    /**
     * Tests that the loader successfully loads from ehcache-nodefault.xml
     * given as a {@link File}
     * <p/>
     * <defaultCache
     * maxElementsInMemory="10000"
     * eternal="false"
     * timeToIdleSeconds="120"
     * timeToLiveSeconds="120"
     * overflowToDisk="true"
     * />
     */
    @Test
    public void testLoadConfigurationFromFileNoDefault() throws Exception {
        File file = new File(TEST_CONFIG_DIR + "ehcache-nodefault.xml");
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        ConfigurationHelper configurationHelper = new ConfigurationHelper(manager, configuration);

        assertEquals(null, configurationHelper.getConfigurationBean().getName());
        assertEquals(true, configurationHelper.getConfigurationBean().getUpdateCheck());
        assertEquals(Configuration.Monitoring.AUTODETECT, configurationHelper.getConfigurationBean().getMonitoring());

        //Check disk path  <diskStore path="/tmp"/>
        assertEquals(System.getProperty("java.io.tmpdir"), configurationHelper.getDiskStorePath());

        //Check default cache
        try {
            configurationHelper.createDefaultCache();
            fail();
        } catch (CacheException e) {
            //noop
        }

        //Check caches
        assertEquals(4, configurationHelper.createCaches().size());

        //  <cache name="sampleCache1"
        //  maxElementsInMemory="10000"
        //  eternal="false"
        //  timeToIdleSeconds="300"
        //  timeToLiveSeconds="600"
        //  overflowToDisk="true"
        //  />
        Ehcache sampleCache1 = configurationHelper.createCacheFromName("sampleCache1");
        Ehcache sampleCache4 = configurationHelper.createCacheFromName("sampleCache4");
        assertEquals("net.sf.ehcache.transaction.manager.DefaultTransactionManagerLookup", configuration.getTransactionManagerLookupClass());
        assertEquals("sampleCache1", sampleCache1.getName());
        assertEquals(false, sampleCache1.getCacheConfiguration().isEternal());
        assertEquals(300, sampleCache1.getCacheConfiguration().getTimeToIdleSeconds());
        assertEquals(600, sampleCache1.getCacheConfiguration().getTimeToLiveSeconds());
        assertEquals(true, sampleCache1.getCacheConfiguration().isOverflowToDisk());
        assertEquals(CacheConfiguration.TransactionalMode.OFF, sampleCache1.getCacheConfiguration().getTransactionalMode());
        assertEquals(false, sampleCache1.getCacheConfiguration().isTransactional());
        assertEquals("sampleCache4", sampleCache4.getName());
        assertEquals(CacheConfiguration.TransactionalMode.XA, sampleCache4.getCacheConfiguration().getTransactionalMode());
        assertEquals(true, sampleCache4.getCacheConfiguration().isTransactional());
    }

    /**
     * Tests that the loader successfully loads from ehcache-nodefault.xml
     * given as a {@link File}
     * <p/>
     * /**
     * Tests that the loader successfully loads from ehcache-nodefault.xml
     * given as a {@link File}
     * <p/>
     * <cache name="sampleCacheNoOptionalAttributes"
     * maxElementsInMemory="1000"
     * eternal="true"
     * overflowToDisk="false"
     * />
     */
    @Test
    public void testDefaultValues() throws Exception {
        File file = new File(TEST_CONFIG_DIR + "ehcache-nodefault.xml");
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        ConfigurationHelper configurationHelper = new ConfigurationHelper(manager, configuration);

        Ehcache sampleCacheNoOptionalAttributes = configurationHelper.createCacheFromName("sampleCacheNoOptionalAttributes");
        assertEquals("sampleCacheNoOptionalAttributes", sampleCacheNoOptionalAttributes.getName());
        assertEquals(1000, sampleCacheNoOptionalAttributes.getCacheConfiguration().getMaxElementsInMemory());
        assertEquals(true, sampleCacheNoOptionalAttributes.getCacheConfiguration().isEternal());
        assertEquals(false, sampleCacheNoOptionalAttributes.getCacheConfiguration().isOverflowToDisk());
        assertEquals(0, sampleCacheNoOptionalAttributes.getCacheConfiguration().getTimeToIdleSeconds());
        assertEquals(0, sampleCacheNoOptionalAttributes.getCacheConfiguration().getTimeToLiveSeconds());
        assertEquals(false, sampleCacheNoOptionalAttributes.getCacheConfiguration().isDiskPersistent());
        assertEquals(120, sampleCacheNoOptionalAttributes.getCacheConfiguration().getDiskExpiryThreadIntervalSeconds());
    }


    /**
     * Tests that the loader successfully loads from ehcache-nodisk.xml
     * given as a {@link File}
     * <p/>
     * <defaultCache
     * maxElementsInMemory="10000"
     * eternal="false"
     * timeToIdleSeconds="120"
     * timeToLiveSeconds="120"
     * overflowToDisk="false"
     * <p/>
     */
    @Test
    public void testLoadConfigurationFromFileNoDisk() throws Exception {
        File file = new File(TEST_CONFIG_DIR + "ehcache-nodisk.xml");
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        ConfigurationHelper configurationHelper = new ConfigurationHelper(manager, configuration);

        assertEquals(null, configurationHelper.getConfigurationBean().getName());
        assertEquals(true, configurationHelper.getConfigurationBean().getUpdateCheck());
        assertEquals(Configuration.Monitoring.AUTODETECT, configurationHelper.getConfigurationBean().getMonitoring());

        //Check disk path  <diskStore path="/tmp"/>
        assertEquals(null, configurationHelper.getDiskStorePath());

        //Check default cache
        Ehcache defaultCache = configurationHelper.createDefaultCache();
        assertEquals("default", defaultCache.getName());
        assertEquals(false, defaultCache.getCacheConfiguration().isEternal());
        assertEquals(5L, defaultCache.getCacheConfiguration().getTimeToIdleSeconds());
        assertEquals(10, defaultCache.getCacheConfiguration().getTimeToLiveSeconds());
        assertEquals(false, defaultCache.getCacheConfiguration().isOverflowToDisk());

        //Check caches
        assertEquals(2, configurationHelper.createCaches().size());

        //  <cache name="sampleCache1"
        //  maxElementsInMemory="10000"
        //  eternal="false"
        //  timeToIdleSeconds="300"
        //  timeToLiveSeconds="600"
        //  overflowToDisk="true"
        //  />
        Ehcache sampleCache1 = configurationHelper.createCacheFromName("sampleCache1");
        assertEquals("sampleCache1", sampleCache1.getName());
        assertEquals(false, sampleCache1.getCacheConfiguration().isEternal());
        assertEquals(360, sampleCache1.getCacheConfiguration().getTimeToIdleSeconds());
        assertEquals(1000, sampleCache1.getCacheConfiguration().getTimeToLiveSeconds());
        assertEquals(false, sampleCache1.getCacheConfiguration().isOverflowToDisk());
    }

    /**
     * Tests the default values for optional attributes
     * <p/>
     * <!-- Sample cache. Optional attributes are removed -->
     * <cache name="sampleRequiredAttributesOnly"
     * maxElementsInMemory="1000"
     * eternal="true"
     * overflowToDisk="false"
     * />
     * <p/>
     * No disk store path specified as disk store not being used
     * />
     */
    @Test
    public void testOptionalAttributeDefaultValues() throws Exception {
        File file = new File(TEST_CONFIG_DIR + "ehcache-nodisk.xml");
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        ConfigurationHelper configurationHelper = new ConfigurationHelper(manager, configuration);

        assertEquals(null, configurationHelper.getDiskStorePath());


        //  <cache name="sampleCache1"
        //  maxElementsInMemory="10000"
        //  eternal="false"
        //  timeToIdleSeconds="300"
        //  timeToLiveSeconds="600"
        //  overflowToDisk="true"
        //  />
        Ehcache sampleCache1 = configurationHelper.createCacheFromName("sampleCache1");
        assertEquals("sampleCache1", sampleCache1.getName());
        assertEquals(false, sampleCache1.getCacheConfiguration().isEternal());
        assertEquals(360, sampleCache1.getCacheConfiguration().getTimeToIdleSeconds());
        assertEquals(1000, sampleCache1.getCacheConfiguration().getTimeToLiveSeconds());
        assertEquals(false, sampleCache1.getCacheConfiguration().isOverflowToDisk());
    }

    /**
     * Regression test for bug 1432074 - NullPointer on RMICacheManagerPeerProviderFactory
     * If manual peer provider configuration is selected then an info message should be
     * logged if there is no list.
     */
    @Test
    public void testEmptyPeerListManualDistributedConfiguration() {
        CacheManager cacheManager = new CacheManager(TEST_CONFIG_DIR + "distribution/ehcache-manual-distributed3.xml");
        assertEquals(0, cacheManager.getCacheManagerPeerProvider("RMI")
                .listRemoteCachePeers(cacheManager.getCache("sampleCache1")).size());

    }


    /**
     * Tests that the loader successfully loads from ehcache.xml
     * given as an {@link URL}.
     * <p/>
     * is found first
     * <p/>
     * <defaultCache
     * maxElementsInMemory="10"
     * eternal="false"
     * timeToIdleSeconds="5"
     * timeToLiveSeconds="10"
     * overflowToDisk="true"
     * />
     */
    @Test
    public void testLoadConfigurationFromURL() throws Exception {
        URL url = getClass().getResource("/ehcache.xml");
        testDefaultConfiguration(url);
    }

    /**
     * Exposes a bug where the default configuration could not be loaded from a Jar URL
     * (a common scenario when ehcache is deployed, and always used for failsafe config).
     *
     * @throws Exception When the test fails.
     */
    @Test
    public void testLoadConfigurationFromJarURL() throws Exception {

        // first, create the jar
        File tempJar = createTempConfigJar();

        // convert it to a URL
        URL tempUrl = tempJar.toURI().toURL();

        // create a jar url that points to the configuration file
        String entry = "jar:" + tempUrl + "!/ehcache.xml";

        // create a URL object from the string, going through the URI class so it's encoded
        URL entryUrl = new URI(entry).toURL();

        testDefaultConfiguration(entryUrl);
    }

    /**
     * Given a URL, parse the configuration and test that the config read corresponds
     * to that which exists in the ehcache.xml file.
     *
     * @param url The URL to load.
     */
    private void testDefaultConfiguration(URL url) {
        Configuration configuration = ConfigurationFactory.parseConfiguration(url);
        ConfigurationHelper configurationHelper = new ConfigurationHelper(manager, configuration);

        //Check disk path missing in test ehcache.xml"/>
        assertEquals(System.getProperty("java.io.tmpdir"), configurationHelper.getDiskStorePath());

        //Check default cache
        Ehcache defaultCache = configurationHelper.createDefaultCache();
        assertEquals("default", defaultCache.getName());
        assertEquals(false, defaultCache.getCacheConfiguration().isEternal());
        assertEquals(5L, defaultCache.getCacheConfiguration().getTimeToIdleSeconds());
        assertEquals(10, defaultCache.getCacheConfiguration().getTimeToLiveSeconds());
        assertEquals(true, defaultCache.getCacheConfiguration().isOverflowToDisk());

        //Check caches
        assertEquals(CACHES_IN_TEST_EHCACHE, configurationHelper.createCaches().size());

        //  <cache name="sampleCache1"
        //  maxElementsInMemory="10000"
        //  eternal="false"
        //  timeToIdleSeconds="300"
        //  timeToLiveSeconds="600"
        //  overflowToDisk="true"
        //  />
        Ehcache sampleCache1 = configurationHelper.createCacheFromName("sampleCache1");
        assertEquals("sampleCache1", sampleCache1.getName());
        assertEquals(false, sampleCache1.getCacheConfiguration().isEternal());
        assertEquals(360, sampleCache1.getCacheConfiguration().getTimeToIdleSeconds());
        assertEquals(1000, sampleCache1.getCacheConfiguration().getTimeToLiveSeconds());
        assertEquals(true, sampleCache1.getCacheConfiguration().isOverflowToDisk());
    }

    /**
     * Creates a jar file that contains only ehcache.xml (a supplied configuration file).
     *
     * @return The jar file created with the configuration file as its only entry.
     * @throws IOException If the jar could not be created.
     */
    private File createTempConfigJar() throws IOException, FileNotFoundException {
        File tempJar = File.createTempFile("config_", ".jar");
        tempJar.deleteOnExit();

        // write the default config to the jar
        JarOutputStream jos = null;
        try {
            jos = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(tempJar)));

            jos.putNextEntry(new JarEntry("ehcache.xml"));

            InputStream defaultCfg = null;
            try {
                defaultCfg = new BufferedInputStream(getClass().getResource("/ehcache.xml").openStream());
                byte[] buf = new byte[1024];
                int read = 0;
                while ((read = defaultCfg.read(buf)) > 0) {
                    jos.write(buf, 0, read);
                }
            } finally {
                try {
                    if (defaultCfg != null) {
                        defaultCfg.close();
                    }
                } catch (IOException ioEx) {
                    // swallow this exception
                }
            }

        } finally {
            try {
                if (jos != null) {
                    jos.closeEntry();

                    jos.flush();
                    jos.close();
                }
            } catch (IOException ioEx) {
                // swallow this exception
            }
        }

        return tempJar;
    }

    /**
     * Tests that the loader successfully loads from ehcache.xml
     * given as a {@link InputStream}
     * <p/>
     * <defaultCache
     * maxElementsInMemory="10000"
     * eternal="false"
     * timeToIdleSeconds="120"
     * timeToLiveSeconds="120"
     * overflowToDisk="true"
     * />
     */
    @Test
    public void testLoadConfigurationFromInputStream() throws Exception {
        InputStream fis = new FileInputStream(new File(SRC_CONFIG_DIR + "ehcache.xml").getAbsolutePath());
        ConfigurationHelper configurationHelper;
        try {
            Configuration configuration = ConfigurationFactory.parseConfiguration(fis);
            configurationHelper = new ConfigurationHelper(manager, configuration);
        } finally {
            fis.close();
        }

        assertEquals(null, configurationHelper.getConfigurationBean().getName());
        assertEquals(true, configurationHelper.getConfigurationBean().getUpdateCheck());
        assertEquals(Configuration.Monitoring.AUTODETECT, configurationHelper.getConfigurationBean().getMonitoring());

        //Check disk path  <diskStore path="/tmp"/>
        assertEquals(System.getProperty("java.io.tmpdir"), configurationHelper.getDiskStorePath());

        //Check default cache
        Ehcache defaultCache = configurationHelper.createDefaultCache();
        assertEquals("default", defaultCache.getName());
        assertEquals(false, defaultCache.getCacheConfiguration().isEternal());
        assertEquals(120, defaultCache.getCacheConfiguration().getTimeToIdleSeconds());
        assertEquals(120, defaultCache.getCacheConfiguration().getTimeToLiveSeconds());
        assertEquals(true, defaultCache.getCacheConfiguration().isOverflowToDisk());

        //Check caches
        assertEquals(6, configurationHelper.createCaches().size());

        //  <cache name="sampleCache1"
        //  maxElementsInMemory="10000"
        //  eternal="false"
        //  timeToIdleSeconds="300"
        //  timeToLiveSeconds="600"
        //  overflowToDisk="true"
        //  />
        Ehcache sampleCache1 = configurationHelper.createCacheFromName("sampleCache1");
        assertEquals("sampleCache1", sampleCache1.getName());
        assertEquals(false, sampleCache1.getCacheConfiguration().isEternal());
        assertEquals(300, sampleCache1.getCacheConfiguration().getTimeToIdleSeconds());
        assertEquals(600, sampleCache1.getCacheConfiguration().getTimeToLiveSeconds());
        assertEquals(true, sampleCache1.getCacheConfiguration().isOverflowToDisk());
    }

    /**
     * Tests that the loader successfully loads from ehcache-failsafe.xml
     * found in the classpath.
     * ehcache.xml should be found in the classpath. In our ant configuration
     * this should be from build/classes/ehcache-failsafe.xml
     * <p/>
     * We delete ehcache.xml from build/test-classes/ first, as failsafe only
     * kicks in when ehcache.xml is not in the classpath.
     * <p/>
     * <defaultCache
     * maxElementsInMemory="10000"
     * eternal="false"
     * timeToIdleSeconds="120"
     * timeToLiveSeconds="120"
     * overflowToDisk="true"
     * />
     */
    @Test
    public void testLoadConfigurationFromFailsafe() throws Exception {
        try {
            File file = new File(AbstractCacheTest.TEST_CLASSES_DIR + "ehcache.xml");
            file.renameTo(new File(AbstractCacheTest.TEST_CLASSES_DIR + "hideehcache.xml"));
            Configuration configuration = ConfigurationFactory.parseConfiguration();
            ConfigurationHelper configurationHelper = new ConfigurationHelper(manager, configuration);

            assertEquals(null, configurationHelper.getConfigurationBean().getName());
            assertEquals(true, configurationHelper.getConfigurationBean().getUpdateCheck());
            assertEquals(Configuration.Monitoring.AUTODETECT, configurationHelper.getConfigurationBean().getMonitoring());

            //Check disk path  <diskStore path="/tmp"/>
            assertEquals(System.getProperty("java.io.tmpdir"), configurationHelper.getDiskStorePath());

            //Check default cache
            Ehcache defaultCache = configurationHelper.createDefaultCache();
            assertEquals("default", defaultCache.getName());
            assertEquals(false, defaultCache.getCacheConfiguration().isEternal());
            assertEquals(120, defaultCache.getCacheConfiguration().getTimeToIdleSeconds());
            assertEquals(120, defaultCache.getCacheConfiguration().getTimeToLiveSeconds());
            assertEquals(true, defaultCache.getCacheConfiguration().isOverflowToDisk());

            //Check caches
            assertEquals(0, configurationHelper.createCaches().size());
        } finally {
            //Put ehcache.xml back
            File hiddenFile = new File(AbstractCacheTest.TEST_CLASSES_DIR + "hideehcache.xml");
            hiddenFile.renameTo(new File(AbstractCacheTest.TEST_CLASSES_DIR + "ehcache.xml"));
        }

    }

    /**
     * Make sure that the empty Configuration constructor remains public for those wishing to create CacheManagers
     * purely programmatically.
     */
    @Test
    public void testCreateEmptyConfiguration() {
        Configuration configuration = new Configuration();
        configuration.setSource("programmatic");
    }


    /**
     * Tests that you cannot use the name default for a cache.
     */
    @Test
    public void testLoadConfigurationFromInvalidXMLFileWithDefaultCacheNameUsed() throws Exception {
        File file = new File(TEST_CONFIG_DIR + "ehcache-withdefaultset.xml");
        try {
            Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        } catch (CacheException e) {
            assertTrue(e.getMessage().contains("The Default Cache has already been configured"));
        }

    }


    /**
     * Tests replacement in the config file.
     */
    @Test
    public void testLoadConfigurationWithReplacement() throws Exception {
        System.setProperty("multicastGroupPort", "4446");
        File file = new File(TEST_CONFIG_DIR + "ehcache-replacement.xml");
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        ConfigurationHelper configurationHelper = new ConfigurationHelper(manager, configuration);


        //Check disk path  <diskStore path="/tmp"/>
        assertNotSame(System.getProperty("java.io.tmpdir"), configurationHelper.getDiskStorePath());
        assertTrue(configuration.getCacheManagerPeerProviderFactoryConfiguration().get(0)
                .getProperties().indexOf("multicastGroupPort=4446") != -1);


    }


    /**
     * Fun with replaceAll which clobbers \\ by default!
     */
    @Test
    public void testPathExpansionAndReplacement() throws Exception {

        String configuration = "This is my ${basedir}.";
        String trimmedToken = "basedir";
        String property = "D:\\sonatype\\workspace\\nexus-aggregator\\nexus\\nexus-app";
        LOG.info("Property: " + property);
        LOG.info("configuration is: " + configuration);
        String propertyWithQuotesProtected = Matcher.quoteReplacement(property);
        configuration = configuration.replaceAll("\\$\\{" + trimmedToken + "\\}", propertyWithQuotesProtected);
        assertTrue(configuration.contains(property));
        LOG.info("configuration is: " + configuration);


    }


    /**
     * Tests the property token extraction logic
     */
    @Test
    public void testMatchPropertyTokensProperlyFormed() {
        String example = "<cacheManagerPeerProviderFactory class=\"net.sf.ehcache.distribution.RMICacheManagerPeerProviderFactory\"" +
                "properties=\"peerDiscovery=automatic, " +
                "multicastGroupAddress=${multicastAddress}, " +
                "multicastGroupPort=4446, timeToLive=1\"/>";
        Set propertyTokens = ConfigurationFactory.extractPropertyTokens(example);
        assertEquals(1, propertyTokens.size());
        String firstPropertyToken = (String) (propertyTokens.toArray())[0];
        assertEquals("${multicastAddress}", firstPropertyToken);
    }

    /**
     * Tests the property token extraction logic
     */
    @Test
    public void testMatchPropertyTokensProperlyFormedTwo() {
        String example = "<cacheManagerPeerProviderFactory class=\"net.sf.ehcache.distribution.RMICacheManagerPeerProviderFactory\"" +
                "properties=\"peerDiscovery=automatic, " +
                "multicastGroupAddress=${multicastAddress}\n, " +
                "multicastGroupPort=4446, timeToLive=${multicastAddress}\"/>";
        Set propertyTokens = ConfigurationFactory.extractPropertyTokens(example);
        assertEquals(1, propertyTokens.size());
        String firstPropertyToken = (String) (propertyTokens.toArray())[0];
        assertEquals("${multicastAddress}", firstPropertyToken);
    }


    /**
     * Tests the property token extraction logic
     */
    @Test
    public void testMatchPropertyTokensProperlyFormedTwoUnique() {
        String example = "<cacheManagerPeerProviderFactory class=\"net.sf.ehcache.distribution.RMICacheManagerPeerProviderFactory\"" +
                "properties=\"peerDiscovery=automatic, " +
                "multicastGroupAddress=${multicastAddress}\n, " +
                "multicastGroupPort=4446, timeToLive=${multicastAddress1}\"/>";
        Set propertyTokens = ConfigurationFactory.extractPropertyTokens(example);
        assertEquals(2, propertyTokens.size());
    }

    /**
     * If you leave off the } then no match.
     */
    @Test
    public void testMatchPropertyTokensNotClosed() {
        String example = "<cacheManagerPeerProviderFactory class=\"net.sf.ehcache.distribution.RMICacheManagerPeerProviderFactory\"" +
                "properties=\"peerDiscovery=automatic, " +
                "multicastGroupAddress=${multicastAddress\n, " +
                "multicastGroupPort=4446, timeToLive=${multicastAddress\"/>";
        Set propertyTokens = ConfigurationFactory.extractPropertyTokens(example);
        assertEquals(0, propertyTokens.size());
    }

    /**
     * Test named cachemanager, terracotta config, clustered caches
     */
    @Test
    public void testTerracottaConfiguration() {
      File file = new File(TEST_CONFIG_DIR + "terracotta/ehcache-terracotta.xml");
      Configuration configuration = ConfigurationFactory.parseConfiguration(file);
      ConfigurationHelper configurationHelper = new ConfigurationHelper(manager, configuration);

      assertEquals("tc", configurationHelper.getConfigurationBean().getName());
      assertEquals(false, configurationHelper.getConfigurationBean().getUpdateCheck());
      assertEquals(Configuration.Monitoring.AUTODETECT, configurationHelper.getConfigurationBean().getMonitoring());

      //Check default cache
      Ehcache defaultCache = configurationHelper.createDefaultCache();
      assertEquals("default", defaultCache.getName());
      assertEquals(false, defaultCache.getCacheConfiguration().isEternal());
      assertEquals(5, defaultCache.getCacheConfiguration().getTimeToIdleSeconds());
      assertEquals(10, defaultCache.getCacheConfiguration().getTimeToLiveSeconds());
      assertEquals(false, defaultCache.getCacheConfiguration().isOverflowToDisk());
      assertEquals(10, defaultCache.getCacheConfiguration().getMaxElementsInMemory());
      assertEquals(0, defaultCache.getCacheConfiguration().getMaxElementsOnDisk());
      assertEquals(true, defaultCache.getCacheConfiguration().isTerracottaClustered());
      assertEquals(true, defaultCache.getCacheConfiguration().getTerracottaConfiguration().getCoherentReads());

      //Check caches
      assertEquals(10, configurationHelper.createCaches().size());

      //  <cache name="clustered-1"
      //   maxElementsInMemory="1000"
      //   memoryStoreEvictionPolicy="LFU">
      //   <terracotta/>
      //  </cache>
      Ehcache sampleCache1 = configurationHelper.createCacheFromName("clustered-1");
      assertEquals("clustered-1", sampleCache1.getName());
      assertEquals(true, sampleCache1.getCacheConfiguration().isTerracottaClustered());
      assertEquals(TerracottaConfiguration.ValueMode.SERIALIZATION,
                  sampleCache1.getCacheConfiguration().getTerracottaConfiguration().getValueMode());

      //  <cache name="clustered-2"
      //      maxElementsInMemory="1000"
      //            memoryStoreEvictionPolicy="LFU">
      //          <terracotta clustered="false"/>
      //   </cache>
      Ehcache sampleCache2 = configurationHelper.createCacheFromName("clustered-2");
      assertEquals("clustered-2", sampleCache2.getName());
      assertEquals(false, sampleCache2.getCacheConfiguration().isTerracottaClustered());
      assertEquals(TerracottaConfiguration.ValueMode.SERIALIZATION,
              sampleCache2.getCacheConfiguration().getTerracottaConfiguration().getValueMode());

      //  <cache name="clustered-3"
      //   maxElementsInMemory="1000"
      //   memoryStoreEvictionPolicy="LFU">
      //   <terracotta valueMode="serialization"/>
      //  </cache>
      Ehcache sampleCache3 = configurationHelper.createCacheFromName("clustered-3");
      assertEquals("clustered-3", sampleCache3.getName());
      assertEquals(true, sampleCache3.getCacheConfiguration().isTerracottaClustered());
      assertEquals(TerracottaConfiguration.ValueMode.SERIALIZATION,
              sampleCache3.getCacheConfiguration().getTerracottaConfiguration().getValueMode());

      //  <cache name="clustered-4"
      //   maxElementsInMemory="1000"
      //   memoryStoreEvictionPolicy="LFU">
      //   <terracotta valueMode="identity"/>
      //  </cache>
      Ehcache sampleCache4 = configurationHelper.createCacheFromName("clustered-4");
      assertEquals("clustered-4", sampleCache4.getName());
      assertEquals(true, sampleCache4.getCacheConfiguration().isTerracottaClustered());
      assertEquals(TerracottaConfiguration.ValueMode.IDENTITY,
              sampleCache4.getCacheConfiguration().getTerracottaConfiguration().getValueMode());

      //  <cache name="clustered-5"
      //   maxElementsInMemory="1000"
      //   memoryStoreEvictionPolicy="LFU">
      //   <terracotta coherentReads="false"/>
      //  </cache>
      Ehcache sampleCache5 = configurationHelper.createCacheFromName("clustered-5");
      assertEquals("clustered-5", sampleCache5.getName());
      assertEquals(true, sampleCache5.getCacheConfiguration().isTerracottaClustered());
      assertEquals(false,
              sampleCache5.getCacheConfiguration().getTerracottaConfiguration().getCoherentReads());

      //  <cache name="clustered-6"
      //   maxElementsInMemory="1000"
      //   memoryStoreEvictionPolicy="LFU">
      //   <terracotta orphanEviction="false"/>
      //  </cache>
      Ehcache sampleCache6 = configurationHelper.createCacheFromName("clustered-6");
      assertEquals("clustered-6", sampleCache6.getName());
      assertEquals(true, sampleCache6.getCacheConfiguration().isTerracottaClustered());
      assertEquals(false,
              sampleCache6.getCacheConfiguration().getTerracottaConfiguration().getOrphanEviction());

      //  <cache name="clustered-7"
      //   maxElementsInMemory="1000"
      //   memoryStoreEvictionPolicy="LFU">
      //   <terracotta orphanEvictionPeriod="42"/>
      //  </cache>
      Ehcache sampleCache7 = configurationHelper.createCacheFromName("clustered-7");
      assertEquals("clustered-7", sampleCache7.getName());
      assertEquals(true, sampleCache7.getCacheConfiguration().isTerracottaClustered());
      assertEquals(42,
              sampleCache7.getCacheConfiguration().getTerracottaConfiguration().getOrphanEvictionPeriod());

      //  <cache name="clustered-8"
      //   maxElementsInMemory="1000"
      //   memoryStoreEvictionPolicy="LFU">
      //   <terracotta localKeyCache="true"/>
      //  </cache>
      Ehcache sampleCache8 = configurationHelper.createCacheFromName("clustered-8");
      assertEquals("clustered-8", sampleCache8.getName());
      assertEquals(true, sampleCache8.getCacheConfiguration().isTerracottaClustered());
      assertEquals(true,
              sampleCache8.getCacheConfiguration().getTerracottaConfiguration().getLocalKeyCache());

      //  <cache name="clustered-9"
      //   maxElementsInMemory="1000"
      //   memoryStoreEvictionPolicy="LFU">
      //   <terracotta localKeyCache="true"/>
      //  </cache>
      Ehcache sampleCache9 = configurationHelper.createCacheFromName("clustered-9");
      assertEquals("clustered-9", sampleCache9.getName());
      assertEquals(true, sampleCache9.getCacheConfiguration().isTerracottaClustered());
      assertEquals(42,
              sampleCache9.getCacheConfiguration().getTerracottaConfiguration().getLocalKeyCacheSize());

      // <terracottaConfig>
      //  <url>localhost:9510</url>
      // </terracottaConfig>
      TerracottaConfigConfiguration tcConfig = configuration.getTerracottaConfiguration();
      assertNotNull(tcConfig);
      assertEquals("localhost:9510", tcConfig.getUrl());
    }


    /**
     * Test tc-config embedded in ehcache.xml
     */
    @Test
    public void testTerracottaEmbeddedConfig() {
      File file = new File(TEST_CONFIG_DIR + "terracotta/ehcache-tc-embedded.xml");
      Configuration configuration = ConfigurationFactory.parseConfiguration(file);
      ConfigurationHelper configurationHelper = new ConfigurationHelper(manager, configuration);

      assertEquals("tc", configurationHelper.getConfigurationBean().getName());
      assertEquals(false, configurationHelper.getConfigurationBean().getUpdateCheck());
      assertEquals(Configuration.Monitoring.AUTODETECT, configurationHelper.getConfigurationBean().getMonitoring());

      //Check default cache
      Ehcache defaultCache = configurationHelper.createDefaultCache();
      assertEquals("default", defaultCache.getName());
      assertEquals(false, defaultCache.getCacheConfiguration().isEternal());
      assertEquals(5, defaultCache.getCacheConfiguration().getTimeToIdleSeconds());
      assertEquals(10, defaultCache.getCacheConfiguration().getTimeToLiveSeconds());
      assertEquals(false, defaultCache.getCacheConfiguration().isOverflowToDisk());
      assertEquals(10, defaultCache.getCacheConfiguration().getMaxElementsInMemory());
      assertEquals(0, defaultCache.getCacheConfiguration().getMaxElementsOnDisk());
      assertEquals(true, defaultCache.getCacheConfiguration().isTerracottaClustered());

      //Check caches
      assertEquals(1, configurationHelper.createCaches().size());

      //  <cache name="clustered-1"
      //   maxElementsInMemory="1000"
      //   memoryStoreEvictionPolicy="LFU">
      //   <terracotta/>
      //  </cache>
      Ehcache sampleCache1 = configurationHelper.createCacheFromName("clustered-1");
      assertEquals("clustered-1", sampleCache1.getName());
      assertEquals(true, sampleCache1.getCacheConfiguration().isTerracottaClustered());
      assertEquals(TerracottaConfiguration.ValueMode.SERIALIZATION,
                  sampleCache1.getCacheConfiguration().getTerracottaConfiguration().getValueMode());

      // <terracottaConfig>
      //  <tc-config> ... </tc-config>
      // </terracottaConfig>
      TerracottaConfigConfiguration tcConfig = configuration.getTerracottaConfiguration();
      assertNotNull(tcConfig);
      assertEquals(null, tcConfig.getUrl());
      String embeddedConfig = tcConfig.getEmbeddedConfig();
      assertEquals("<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\"> " +
              "<servers> <server host=\"server1\" name=\"s1\"></server> " +
              "<server host=\"server2\" name=\"s2\"></server> </servers> " +
              "<clients> <logs>app/logs-%i</logs> </clients> </tc:tc-config>",
              removeLotsOfWhitespace(tcConfig.getEmbeddedConfig()));
    }

    @Test
    public void testTerracottaEmbeddedXsdConfig() {
        File file = new File(TEST_CONFIG_DIR
                + "terracotta/ehcache-tc-embedded-xsd.xml");
        Configuration configuration = ConfigurationFactory
                .parseConfiguration(file);
        ConfigurationHelper configurationHelper = new ConfigurationHelper(
                manager, configuration);

        assertEquals("tc", configurationHelper.getConfigurationBean().getName());
        assertEquals(false, configurationHelper.getConfigurationBean()
                .getUpdateCheck());
        assertEquals(Configuration.Monitoring.AUTODETECT, configurationHelper
                .getConfigurationBean().getMonitoring());

        // <terracottaConfig>
        // <tc-config> ... </tc-config>
        // </terracottaConfig>
        TerracottaConfigConfiguration tcConfig = configuration
                .getTerracottaConfiguration();
        assertNotNull(tcConfig);
        assertEquals(null, tcConfig.getUrl());
        String embeddedConfig = tcConfig.getEmbeddedConfig();
        assertEquals(
                "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\"> <servers> "
                        + "<server host=\"server1\" name=\"s1\"></server> "
                        + "<server host=\"server2\" name=\"s2\"></server> </servers> "
                        + "<clients> <logs>app/logs-%i</logs> </clients> </tc:tc-config>",
                removeLotsOfWhitespace(tcConfig.getEmbeddedConfig()));
    }

    /**
     * Test invalid combination of overflow to disk and terracotta ehcache.xml
     */
    @Test
    public void testTerracottaInvalidConfig1() {
      File file = new File(TEST_CONFIG_DIR + "terracotta/ehcache-terracotta-invalid1.xml");
      try {
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        fail("expecting exception to be thrown");
      } catch (CacheException e) {
        assertTrue(e.getMessage().contains("overflowToDisk isn't supported for a clustered Terracotta cache"));
      }
    }

    /**
     * Test invalid combination of disk persistent and terracotta ehcache.xml
     */
    @Test
    public void testTerracottaInvalidConfig2() {
      File file = new File(TEST_CONFIG_DIR + "terracotta/ehcache-terracotta-invalid2.xml");
      try {
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        fail("expecting exception to be thrown");
      } catch (CacheException e) {
        assertTrue(e.getMessage().contains("diskPersistent isn't supported for a clustered Terracotta cache"));
      }
    }

    /**
     * Test invalid combination of replicated and terracotta ehcache.xml
     */
    @Test
    public void testTerracottaInvalidConfig3() {
      File file = new File(TEST_CONFIG_DIR + "terracotta/ehcache-terracotta-invalid3.xml");
      try {
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        fail("expecting exception to be thrown");
      } catch (CacheException e) {
        assertTrue(e.getMessage().contains("cache replication isn't supported for a clustered Terracotta cache"));
      }
    }

    /**
     * Test invalid combination of replicated and terracotta ehcache.xml
     */
    @Test
    public void testTerracottaInvalidConfig4() {
      File file = new File(TEST_CONFIG_DIR + "terracotta/ehcache-terracotta-invalid4.xml");
      try {
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        fail("expecting exception to be thrown");
      } catch (CacheException e) {
        assertTrue(e.getMessage().contains("cache replication isn't supported for a clustered Terracotta cache"));
      }
    }

    /**
     * Test invalid combination of replicated and terracotta ehcache.xml
     */
    @Test
    public void testTerracottaInvalidConfig5() {
      File file = new File(TEST_CONFIG_DIR + "terracotta/ehcache-terracotta-invalid5.xml");
      try {
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        fail("expecting exception to be thrown");
      } catch (CacheException e) {
        assertTrue(e.getMessage().contains("cache replication isn't supported for a clustered Terracotta cache"));
      }
    }

    private String removeLotsOfWhitespace(String str) {
        return str.replace("\t", "").replace("\r", "").replace("\n", "").replaceAll("\\s+", " ");
    }

    @Test
    public void testMonitoringOn() {
      File file = new File(TEST_CONFIG_DIR + "ehcache-monitoring-on.xml");
      Configuration configuration = ConfigurationFactory.parseConfiguration(file);
      ConfigurationHelper configurationHelper = new ConfigurationHelper(manager, configuration);

      assertEquals(Configuration.Monitoring.ON, configurationHelper.getConfigurationBean().getMonitoring());
    }

    @Test
    public void testMonitoringOff() {
      File file = new File(TEST_CONFIG_DIR + "ehcache-monitoring-off.xml");
      Configuration configuration = ConfigurationFactory.parseConfiguration(file);
      ConfigurationHelper configurationHelper = new ConfigurationHelper(manager, configuration);

      assertEquals(Configuration.Monitoring.OFF, configurationHelper.getConfigurationBean().getMonitoring());
    }

    @Test
    public void testMonitoringAutodetect() {
      File file = new File(TEST_CONFIG_DIR + "ehcache-monitoring-autodetect.xml");
      Configuration configuration = ConfigurationFactory.parseConfiguration(file);
      ConfigurationHelper configurationHelper = new ConfigurationHelper(manager, configuration);

      assertEquals(Configuration.Monitoring.AUTODETECT, configurationHelper.getConfigurationBean().getMonitoring());
    }
}

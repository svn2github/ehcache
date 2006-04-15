/**
 *  Copyright 2003-2006 Greg Luck
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

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.distribution.CacheManagerPeerListener;
import net.sf.ehcache.distribution.CacheManagerPeerProvider;
import net.sf.ehcache.distribution.MulticastRMICacheManagerPeerProvider;
import net.sf.ehcache.distribution.RMIAsynchronousCacheReplicator;
import net.sf.ehcache.distribution.RMICacheManagerPeerListener;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.CacheManagerEventListener;
import net.sf.ehcache.event.CountingCacheEventListener;
import net.sf.ehcache.event.CountingCacheManagerEventListener;

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
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Tests for Store Configuration
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public class ConfigurationFactoryTest extends AbstractCacheTest {


    /**
     * setup test
     */
    protected void setUp() throws Exception {
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
    public void testLoadConfigurationFromClasspath() throws Exception {

        Configuration configuration = ConfigurationFactory.parseConfiguration();
        ConfigurationHelper configurationHelper = new ConfigurationHelper(manager, configuration);

        //Check disk path  <diskStore path="/tmp"/>
        assertEquals(System.getProperty("java.io.tmpdir"), configurationHelper.getDiskStorePath());

        //Check CacheManagerEventListener
        assertEquals(null, configurationHelper.createCacheManagerEventListener());

        //Check default cache
        Cache defaultCache = configurationHelper.createDefaultCache();
        assertEquals("default", defaultCache.getName());
        assertEquals(false, defaultCache.isEternal());
        assertEquals(5, defaultCache.getTimeToIdleSeconds());
        assertEquals(10, defaultCache.getTimeToLiveSeconds());
        assertEquals(true, defaultCache.isOverflowToDisk());

        //Check caches
        assertEquals(8, configurationHelper.createCaches().size());

        /*
        <cache name="sampleCache1"
        maxElementsInMemory="10000"
        eternal="false"
        timeToIdleSeconds="360"
        timeToLiveSeconds="1000"
        overflowToDisk="true"
        />
        */
        Cache sampleCache1 = configurationHelper.createCacheFromName("sampleCache1");
        assertEquals("sampleCache1", sampleCache1.getName());
        assertEquals(false, sampleCache1.isEternal());
        assertEquals(360, sampleCache1.getTimeToIdleSeconds());
        assertEquals(1000, sampleCache1.getTimeToLiveSeconds());
        assertEquals(true, sampleCache1.isOverflowToDisk());

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
        Cache persistentLongExpiryIntervalCache = configurationHelper.createCacheFromName("persistentLongExpiryIntervalCache");
        assertEquals("persistentLongExpiryIntervalCache", persistentLongExpiryIntervalCache.getName());
        assertEquals(false, persistentLongExpiryIntervalCache.isEternal());
        assertEquals(300, persistentLongExpiryIntervalCache.getTimeToIdleSeconds());
        assertEquals(600, persistentLongExpiryIntervalCache.getTimeToLiveSeconds());
        assertEquals(true, persistentLongExpiryIntervalCache.isOverflowToDisk());
        assertEquals(true, persistentLongExpiryIntervalCache.isDiskPersistent());
        assertEquals(600, persistentLongExpiryIntervalCache.getDiskExpiryThreadIntervalSeconds());
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
    public void testLoadConfigurationFromFile() throws Exception {

        File file = new File(SRC_CONFIG_DIR + "ehcache.xml");
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        ConfigurationHelper configurationHelper = new ConfigurationHelper(manager, configuration);

        //Check disk path  <diskStore path="/tmp"/>
        assertEquals(System.getProperty("java.io.tmpdir"), configurationHelper.getDiskStorePath());

        //Check default cache
        Cache defaultCache = configurationHelper.createDefaultCache();
        assertEquals("default", defaultCache.getName());
        assertEquals(false, defaultCache.isEternal());
        assertEquals(120, defaultCache.getTimeToIdleSeconds());
        assertEquals(120, defaultCache.getTimeToLiveSeconds());
        assertEquals(true, defaultCache.isOverflowToDisk());

        //Check caches
        assertEquals(5, configurationHelper.createCaches().size());

        //  <cache name="sampleCache1"
        //  maxElementsInMemory="10000"
        //  eternal="false"
        //  timeToIdleSeconds="300"
        //  timeToLiveSeconds="600"
        //  overflowToDisk="true"
        //  />
        Cache sampleCache1 = configurationHelper.createCacheFromName("sampleCache1");
        assertEquals("sampleCache1", sampleCache1.getName());
        assertEquals(false, sampleCache1.isEternal());
        assertEquals(300, sampleCache1.getTimeToIdleSeconds());
        assertEquals(600, sampleCache1.getTimeToLiveSeconds());
        assertEquals(true, sampleCache1.isOverflowToDisk());
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
    public void testLoadConfigurationFromEhcache11File() throws Exception {

        File file = new File(TEST_CONFIG_DIR + "ehcache-1_1.xml");
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        ConfigurationHelper configurationHelper = new ConfigurationHelper(manager, configuration);

        //Check disk path  <diskStore path="/tmp"/>
        assertEquals(System.getProperty("java.io.tmpdir"), configurationHelper.getDiskStorePath());

        //Check default cache
        Cache defaultCache = configurationHelper.createDefaultCache();
        assertEquals("default", defaultCache.getName());
        assertEquals(false, defaultCache.isEternal());
        assertEquals(5, defaultCache.getTimeToIdleSeconds());
        assertEquals(10, defaultCache.getTimeToLiveSeconds());
        assertEquals(true, defaultCache.isOverflowToDisk());

        //Check caches
        assertEquals(8, configurationHelper.createCaches().size());

        //  <cache name="sampleCache1"
        //  maxElementsInMemory="10000"
        //  eternal="false"
        //  timeToIdleSeconds="300"
        //  timeToLiveSeconds="600"
        //  overflowToDisk="true"
        //  />
        Cache sampleCache1 = configurationHelper.createCacheFromName("sampleCache1");
        assertEquals("sampleCache1", sampleCache1.getName());
        assertEquals(false, sampleCache1.isEternal());
        assertEquals(360, sampleCache1.getTimeToIdleSeconds());
        assertEquals(1000, sampleCache1.getTimeToLiveSeconds());
        assertEquals(true, sampleCache1.isOverflowToDisk());
    }

    /**
     * Tests that the CacheManagerEventListener is null when
     * no CacheManagerEventListener class is specified.
     */
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
        Cache sampleCache1 = configurationHelper.createCacheFromName("sampleCache1");
        Set registeredListeners = sampleCache1.getCacheEventNotificationService().getCacheEventListeners();
        assertEquals(2, registeredListeners.size());

        //Should have null and counting
        Cache sampleCache2 = configurationHelper.createCacheFromName("sampleCache2");
        registeredListeners = sampleCache2.getCacheEventNotificationService().getCacheEventListeners();
        assertEquals(1, registeredListeners.size());

        //Should have null and counting
        Cache sampleCache3 = configurationHelper.createCacheFromName("sampleCache3");
        registeredListeners = sampleCache3.getCacheEventNotificationService().getCacheEventListeners();
        assertEquals(1, registeredListeners.size());

        //Should have none. None set.
        Cache footerPageCache = configurationHelper.createCacheFromName("FooterPageCache");
        registeredListeners = footerPageCache.getCacheEventNotificationService().getCacheEventListeners();
        assertEquals(0, registeredListeners.size());

        //Should have one. null listener set.
        Cache persistentLongExpiryIntervalCache = configurationHelper.createCacheFromName("persistentLongExpiryIntervalCache");
        registeredListeners = persistentLongExpiryIntervalCache.getCacheEventNotificationService()
                .getCacheEventListeners();
        assertEquals(1, registeredListeners.size());
    }

    /**
     * Tests for Distributed Cache config
     */
    public void testLoadConfigurationFromFileDistribution() throws Exception {
        File file = new File(TEST_CONFIG_DIR + "distribution/ehcache-distributed1.xml");
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        ConfigurationHelper configurationHelper = new ConfigurationHelper(manager, configuration);

        //Check CacheManagerPeerProvider
        CacheManagerPeerProvider peerProvider = configurationHelper.createCachePeerProvider();
        assertTrue(peerProvider instanceof MulticastRMICacheManagerPeerProvider);

        //check CacheManagerPeerListener
        CacheManagerPeerListener peerListener = configurationHelper.createCachePeerListener();
        assertTrue(peerListener instanceof RMICacheManagerPeerListener);

        //Check caches. Configuration should have completed
        assertEquals(61, configurationHelper.createCaches().size());

        Cache sampleCache1 = configurationHelper.createCacheFromName("sampleCache1");
        Set listeners = sampleCache1.getCacheEventNotificationService().getCacheEventListeners();
        assertEquals(2, listeners.size());
        for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
            CacheEventListener cacheEventListener = (CacheEventListener) iterator.next();
            assertTrue(cacheEventListener instanceof RMIAsynchronousCacheReplicator || cacheEventListener
                    instanceof CountingCacheEventListener);
        }


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
    public void testLoadConfigurationFromFileNoDefault() throws Exception {
        File file = new File(TEST_CONFIG_DIR + "ehcache-nodefault.xml");
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        ConfigurationHelper configurationHelper = new ConfigurationHelper(manager, configuration);

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
        assertEquals(3, configurationHelper.createCaches().size());

        //  <cache name="sampleCache1"
        //  maxElementsInMemory="10000"
        //  eternal="false"
        //  timeToIdleSeconds="300"
        //  timeToLiveSeconds="600"
        //  overflowToDisk="true"
        //  />
        Cache sampleCache1 = configurationHelper.createCacheFromName("sampleCache1");
        assertEquals("sampleCache1", sampleCache1.getName());
        assertEquals(false, sampleCache1.isEternal());
        assertEquals(300, sampleCache1.getTimeToIdleSeconds());
        assertEquals(600, sampleCache1.getTimeToLiveSeconds());
        assertEquals(true, sampleCache1.isOverflowToDisk());
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
    public void testDefaultValues() throws Exception {
        File file = new File(TEST_CONFIG_DIR + "ehcache-nodefault.xml");
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        ConfigurationHelper configurationHelper = new ConfigurationHelper(manager, configuration);

        Cache sampleCacheNoOptionalAttributes = configurationHelper.createCacheFromName("sampleCacheNoOptionalAttributes");
        assertEquals("sampleCacheNoOptionalAttributes", sampleCacheNoOptionalAttributes.getName());
        assertEquals(1000, sampleCacheNoOptionalAttributes.getMaxElementsInMemory());
        assertEquals(true, sampleCacheNoOptionalAttributes.isEternal());
        assertEquals(false, sampleCacheNoOptionalAttributes.isOverflowToDisk());
        assertEquals(0, sampleCacheNoOptionalAttributes.getTimeToIdleSeconds());
        assertEquals(0, sampleCacheNoOptionalAttributes.getTimeToLiveSeconds());
        assertEquals(false, sampleCacheNoOptionalAttributes.isDiskPersistent());
        assertEquals(120, sampleCacheNoOptionalAttributes.getDiskExpiryThreadIntervalSeconds());
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
     * No disk store path specified as disk store not being used
     * />
     */
    public void testLoadConfigurationFromFileNoDisk() throws Exception {
        File file = new File(TEST_CONFIG_DIR + "ehcache-nodisk.xml");
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        ConfigurationHelper configurationHelper = new ConfigurationHelper(manager, configuration);

        //Check disk path  <diskStore path="/tmp"/>
        assertEquals(null, configurationHelper.getDiskStorePath());

        //Check default cache
        Cache defaultCache = configurationHelper.createDefaultCache();
        assertEquals("default", defaultCache.getName());
        assertEquals(false, defaultCache.isEternal());
        assertEquals(5, defaultCache.getTimeToIdleSeconds());
        assertEquals(10, defaultCache.getTimeToLiveSeconds());
        assertEquals(false, defaultCache.isOverflowToDisk());

        //Check caches
        assertEquals(2, configurationHelper.createCaches().size());

        //  <cache name="sampleCache1"
        //  maxElementsInMemory="10000"
        //  eternal="false"
        //  timeToIdleSeconds="300"
        //  timeToLiveSeconds="600"
        //  overflowToDisk="true"
        //  />
        Cache sampleCache1 = configurationHelper.createCacheFromName("sampleCache1");
        assertEquals("sampleCache1", sampleCache1.getName());
        assertEquals(false, sampleCache1.isEternal());
        assertEquals(360, sampleCache1.getTimeToIdleSeconds());
        assertEquals(1000, sampleCache1.getTimeToLiveSeconds());
        assertEquals(false, sampleCache1.isOverflowToDisk());
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
    public void testOptionalAttributeDefaultValues() throws Exception {
        File file = new File(TEST_CONFIG_DIR + "ehcache-nodisk.xml");
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        ConfigurationHelper configurationHelper = new ConfigurationHelper(manager, configuration);

        //  <cache name="sampleCache1"
        //  maxElementsInMemory="10000"
        //  eternal="false"
        //  timeToIdleSeconds="300"
        //  timeToLiveSeconds="600"
        //  overflowToDisk="true"
        //  />
        Cache sampleCache1 = configurationHelper.createCacheFromName("sampleCache1");
        assertEquals("sampleCache1", sampleCache1.getName());
        assertEquals(false, sampleCache1.isEternal());
        assertEquals(360, sampleCache1.getTimeToIdleSeconds());
        assertEquals(1000, sampleCache1.getTimeToLiveSeconds());
        assertEquals(false, sampleCache1.isOverflowToDisk());
    }

    /**
     * Regression test for bug 1432074 - NullPointer on RMICacheManagerPeerProviderFactory
     * If manual peer provider configuration is selected then a CacheException should be
     * thrown if there is no list.
     */
    public void testBadManualDistributedConfiguration() {
        try {
            new CacheManager(TEST_CONFIG_DIR + "distribution/ehcache-bad-manual-distributed.xml");
            fail();
        } catch (CacheException e) {
            assertEquals("rmiUrls must be specified when peerDiscovery is manual", e.getMessage());
        }
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

        //Check disk path  <diskStore path="/tmp"/>
        assertEquals(System.getProperty("java.io.tmpdir"), configurationHelper.getDiskStorePath());

        //Check default cache
        Cache defaultCache = configurationHelper.createDefaultCache();
        assertEquals("default", defaultCache.getName());
        assertEquals(false, defaultCache.isEternal());
        assertEquals(5, defaultCache.getTimeToIdleSeconds());
        assertEquals(10, defaultCache.getTimeToLiveSeconds());
        assertEquals(true, defaultCache.isOverflowToDisk());

        //Check caches
        assertEquals(8, configurationHelper.createCaches().size());

        //  <cache name="sampleCache1"
        //  maxElementsInMemory="10000"
        //  eternal="false"
        //  timeToIdleSeconds="300"
        //  timeToLiveSeconds="600"
        //  overflowToDisk="true"
        //  />
        Cache sampleCache1 = configurationHelper.createCacheFromName("sampleCache1");
        assertEquals("sampleCache1", sampleCache1.getName());
        assertEquals(false, sampleCache1.isEternal());
        assertEquals(360, sampleCache1.getTimeToIdleSeconds());
        assertEquals(1000, sampleCache1.getTimeToLiveSeconds());
        assertEquals(true, sampleCache1.isOverflowToDisk());
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
    public void testLoadConfigurationFromInputStream() throws Exception {
        InputStream fis = new FileInputStream(new File(SRC_CONFIG_DIR + "ehcache.xml").getAbsolutePath());
        ConfigurationHelper configurationHelper;
        try {
            Configuration configuration = ConfigurationFactory.parseConfiguration(fis);
            configurationHelper = new ConfigurationHelper(manager, configuration);
        } finally {
            fis.close();
        }

        //Check disk path  <diskStore path="/tmp"/>
        assertEquals(System.getProperty("java.io.tmpdir"), configurationHelper.getDiskStorePath());

        //Check default cache
        Cache defaultCache = configurationHelper.createDefaultCache();
        assertEquals("default", defaultCache.getName());
        assertEquals(false, defaultCache.isEternal());
        assertEquals(120, defaultCache.getTimeToIdleSeconds());
        assertEquals(120, defaultCache.getTimeToLiveSeconds());
        assertEquals(true, defaultCache.isOverflowToDisk());

        //Check caches
        assertEquals(5, configurationHelper.createCaches().size());

        //  <cache name="sampleCache1"
        //  maxElementsInMemory="10000"
        //  eternal="false"
        //  timeToIdleSeconds="300"
        //  timeToLiveSeconds="600"
        //  overflowToDisk="true"
        //  />
        Cache sampleCache1 = configurationHelper.createCacheFromName("sampleCache1");
        assertEquals("sampleCache1", sampleCache1.getName());
        assertEquals(false, sampleCache1.isEternal());
        assertEquals(300, sampleCache1.getTimeToIdleSeconds());
        assertEquals(600, sampleCache1.getTimeToLiveSeconds());
        assertEquals(true, sampleCache1.isOverflowToDisk());
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
    public void testLoadConfigurationFromFailsafe() throws Exception {
        try {
            File file = new File(AbstractCacheTest.TEST_CLASSES_DIR + "ehcache.xml");
            file.renameTo(new File(AbstractCacheTest.TEST_CLASSES_DIR + "hideehcache.xml"));
            Configuration configuration = ConfigurationFactory.parseConfiguration();
            ConfigurationHelper configurationHelper = new ConfigurationHelper(manager, configuration);

            //Check disk path  <diskStore path="/tmp"/>
            assertEquals(System.getProperty("java.io.tmpdir"), configurationHelper.getDiskStorePath());

            //Check default cache
            Cache defaultCache = configurationHelper.createDefaultCache();
            assertEquals("default", defaultCache.getName());
            assertEquals(false, defaultCache.isEternal());
            assertEquals(120, defaultCache.getTimeToIdleSeconds());
            assertEquals(120, defaultCache.getTimeToLiveSeconds());
            assertEquals(true, defaultCache.isOverflowToDisk());

            //Check caches
            assertEquals(0, configurationHelper.createCaches().size());
        } finally {
            //Put ehcache.xml back
            File hiddenFile = new File(AbstractCacheTest.TEST_CLASSES_DIR + "hideehcache.xml");
            hiddenFile.renameTo(new File(AbstractCacheTest.TEST_CLASSES_DIR + "ehcache.xml"));
        }

    }
}

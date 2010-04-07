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

package net.sf.ehcache.management.sampled;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for testing the mbean registration provider
 * 
 * @author Abhishek Sanoujam
 * @version $Id: MBeanRegistrationProviderTest.java 1178 2009-09-23 23:50:15Z
 *          asingh
 *          $
 */
public class MBeanRegistrationProviderTest extends AbstractCacheTest {

    private static final Logger LOG = LoggerFactory.getLogger(MBeanRegistrationProviderTest.class.getName());

    private MBeanServer mbeanServer;

    private CacheManager cacheManager;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        mbeanServer = createMBeanServer();
        cleanUpExistingMbeans();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        if (cacheManager != null) {
            cacheManager.shutdown();
        }
        super.tearDown();
        assertEquals(0, mbeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null).size());
        cacheManager = null;
        cleanUpExistingMbeans();
    }

    private void cleanUpExistingMbeans() throws Exception {
        Set<ObjectName> queryNames = mbeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null);
        for (ObjectName name : queryNames) {
            mbeanServer.unregisterMBean(name);
        }
    }

    @Test
    public void testMonitoringOn() throws Exception {
        File file = new File(TEST_CONFIG_DIR + "ehcache-monitoring-on.xml");
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        cacheManager = new CacheManager(configuration);
        assertSampledMBeansGroupRegistered(3);
        assertCacheManagerMBeansRegistered("cacheManagerOn", 1);
    }

    private void assertSampledMBeansGroupRegistered(int size) throws Exception {
        Set queryNames = mbeanServer.queryNames(new ObjectName(SampledEhcacheMBeans.GROUP_ID + ":*"), null);
        assertEquals(size, queryNames.size());
    }

    private void assertCacheManagerMBeansRegistered(String cacheManagerName, int size) throws Exception {
        Set queryNames = mbeanServer.queryNames(SampledEhcacheMBeans.getCacheManagerObjectName(null, cacheManagerName), null);
        assertEquals(size, queryNames.size());
    }

    private void assertCacheManagerMBeansRegistered(int size) throws Exception {
        Set queryNames = mbeanServer.queryNames(SampledEhcacheMBeans.getQueryCacheManagersObjectName(null), null);
        assertEquals(size, queryNames.size());
    }

    @Test
    public void testMonitoringOff() throws Exception {
        File file = new File(TEST_CONFIG_DIR + "ehcache-monitoring-off.xml");
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        cacheManager = new CacheManager(configuration);
        assertSampledMBeansGroupRegistered(0);
        assertCacheManagerMBeansRegistered("cacheManagerOff", 0);
    }

    @Test
    public void testMonitoringAutodetect() throws Exception {
        System.setProperty("tc.active", "false");
        doTestMonitoringAutodetect(false);

        System.setProperty("tc.active", "true");
        doTestMonitoringAutodetect(true);

        // reset property
        System.setProperty("tc.active", "false");
    }

    public void doTestMonitoringAutodetect(boolean dsoActive) throws Exception {
        File file = new File(TEST_CONFIG_DIR + "ehcache-monitoring-autodetect.xml");
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        cacheManager = new CacheManager(configuration);
        if (dsoActive) {
            assertSampledMBeansGroupRegistered(3);
            assertCacheManagerMBeansRegistered("cacheManagerAutoDetect", 1);
        } else {
            assertSampledMBeansGroupRegistered(0);
            assertCacheManagerMBeansRegistered("cacheManagerAutoDetect", 0);
        }
    }

    @Test
    public void testMultipleCacheManagerDifferentNames() throws Exception {
        System.setProperty("tc.active", "true");
        File file = new File(TEST_CONFIG_DIR + "ehcache-monitoring-autodetect.xml");
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        cacheManager = new CacheManager(configuration);
        assertSampledMBeansGroupRegistered(3);
        assertCacheManagerMBeansRegistered("cacheManagerAutoDetect", 1);

        file = new File(TEST_CONFIG_DIR + "ehcache-monitoring-on.xml");
        configuration = ConfigurationFactory.parseConfiguration(file);
        CacheManager otherCacheManager = new CacheManager(configuration);
        assertSampledMBeansGroupRegistered(3 + 3);
        assertCacheManagerMBeansRegistered(2);

        cacheManager.shutdown();
        otherCacheManager.shutdown();
        // reset property
        System.setProperty("tc.active", "false");
    }

    @Test
    public void testMultipleCacheManagerSameNames() throws Exception {
        int count = 8;
        CacheManager[] managers = new CacheManager[count];
        for (int i = 0; i < count; i++) {
            File file = new File(TEST_CONFIG_DIR + "ehcache-monitoring-on.xml");
            Configuration configuration = ConfigurationFactory.parseConfiguration(file);
            managers[i] = new CacheManager(configuration);
            assertSampledMBeansGroupRegistered(3 * (i + 1));
            assertCacheManagerMBeansRegistered(i + 1);
        }

        CacheManager[] duplicates = new CacheManager[count];
        for (int i = 0; i < count; i++) {
            File file = new File(TEST_CONFIG_DIR + "ehcache-monitoring-on.xml");
            Configuration configuration = ConfigurationFactory.parseConfiguration(file);

            duplicates[i] = new CacheManager(configuration);
            assertSampledMBeansGroupRegistered(3 * (i + 1) + count * 3);
            assertCacheManagerMBeansRegistered((i + 1) + count);
        }
        // shutting down the cacheManager should clean up the mbeans
        for (CacheManager mgr : managers) {
            mgr.shutdown();
        }
        for (CacheManager mgr : duplicates) {
            mgr.shutdown();
        }
        assertSampledMBeansGroupRegistered(0);
        assertCacheManagerMBeansRegistered(0);
    }
}

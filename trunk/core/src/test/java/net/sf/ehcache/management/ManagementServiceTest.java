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

package net.sf.ehcache.management;

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;

import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import java.io.File;
import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * These tests use the JDK1.5 platform mbean server
 * To interactively examine behaviour, add a Thread.sleep(...) and add -Dcom.sun.management.jmxremote to the java
 * invocation.
 * <p/>
 * To see ehcache specific types in the JMX client add the ehcache.jar to the classpath.
 * e.g. to avoid the "Unavailable" message in jconsole caused by ClassNotFound add:
 * jconsole -J-Djava.class.path=core/target/classes
 *
 * @author Greg Luck
 * @version $Id$
 */
public class ManagementServiceTest extends AbstractCacheTest {

    private static final Logger LOG = LoggerFactory.getLogger(ManagementServiceTest.class.getName());
    private static final int OBJECTS_IN_TEST_EHCACHE = 46;
    private MBeanServer mBeanServer;


    /**
     * setup test
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        mBeanServer = createMBeanServer();
    }

    private MBeanServer create14MBeanServer() {
        return MBeanServerFactory.createMBeanServer("SimpleAgent");
    }

    /**
     * teardown
     */
    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        //Ensure the CacheManager shutdown clears all ObjectNames from the MBeanServer
        assertEquals(0, mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null).size());
    }


    /**
     * Integration test for the registration service
     */
    @Test
    public void testRegistrationServiceFourTrue() throws Exception {
        ManagementService.registerMBeans(manager, mBeanServer, true, true, true, true);
        assertEquals(OBJECTS_IN_TEST_EHCACHE, mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null).size());
    }

    /**
     * Integration test for the registration service
     */
    @Test
    public void testRegistrationServiceFourTrueUsing14MBeanServer() throws Exception {
        mBeanServer = create14MBeanServer();
        ManagementService.registerMBeans(manager, mBeanServer, true, true, true, true);
        assertEquals(OBJECTS_IN_TEST_EHCACHE, mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null).size());
    }


    /**
     * Integration test for the registration service using a contructed ManagementService as would be done
     * by an IoC container.
     */
    @Test
    public void testRegistrationServiceFourTrueUsing14MBeanServerWithConstructorInjection() throws Exception {
        mBeanServer = create14MBeanServer();
        ManagementService managementService = new ManagementService(manager, mBeanServer, true, true, true, true);
        managementService.init();
        assertEquals(OBJECTS_IN_TEST_EHCACHE, mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null).size());
    }

    /**
     * Integration test for the registration service
     */
    @Test
    public void testRegistrationServiceListensForCacheChanges() throws Exception {
        ManagementService.registerMBeans(manager, mBeanServer, true, true, true, true);
        assertEquals(OBJECTS_IN_TEST_EHCACHE, mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null).size());
        manager.addCache("new cache");
        assertEquals(OBJECTS_IN_TEST_EHCACHE + 3, mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null).size());
        manager.removeCache("sampleCache1");
        assertEquals(OBJECTS_IN_TEST_EHCACHE, mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null).size());
//        Thread.sleep(1000000);
    }

    /**
     * Integration test for the registration service
     */
    @Test
    public void testMultipleCacheManagers() throws Exception {
        ManagementService.registerMBeans(manager, mBeanServer, true, true, true, true);
        assertEquals(OBJECTS_IN_TEST_EHCACHE, mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null).size());
        File file = new File(AbstractCacheTest.SRC_CONFIG_DIR + "ehcache.xml");
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        net.sf.ehcache.CacheManager secondCacheManager = new net.sf.ehcache.CacheManager(configuration);
        ManagementService.registerMBeans(secondCacheManager, mBeanServer, true, true, true, true);
        assertEquals(OBJECTS_IN_TEST_EHCACHE + 19, mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null).size());
        secondCacheManager.shutdown();
        assertEquals(OBJECTS_IN_TEST_EHCACHE, mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null).size());

//        Thread.sleep(1000000);

    }

    /**
     * Checks that Statistics updates
     */
    @Test
    public void testStatisticsMBeanUpdatesAsStatsChange() throws Exception {
        ManagementService.registerMBeans(manager, mBeanServer, false, false, false, true);
        Ehcache cache = manager.getCache("sampleCache1");
        ObjectName name = CacheStatistics.createObjectName(manager.getName(), cache.getName());
        assertEquals(Long.valueOf(0), mBeanServer.getAttribute(name, "ObjectCount"));
        cache.put(new Element("1", "value"));
        cache.get("1");
        Thread.sleep(20);
        assertEquals(Long.valueOf(1), mBeanServer.getAttribute(name, "ObjectCount"));
        assertEquals(Long.valueOf(1), mBeanServer.getAttribute(name, "MemoryStoreObjectCount"));
        assertEquals(Long.valueOf(0), mBeanServer.getAttribute(name, "DiskStoreObjectCount"));

//        Thread.sleep(1000000);


    }

    /**
     * Integration test for the registration service
     */
    @Test
    public void testRegistrationServiceThreeTrue() throws Exception {
        ManagementService.registerMBeans(manager, mBeanServer, true, true, true, false);
        assertEquals(31, mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null).size());

    }

    /**
     * Integration test for the registration service
     */
    @Test
    public void testRegistrationServiceTwoTrue() throws Exception {
        ManagementService.registerMBeans(manager, mBeanServer, true, true, false, false);
        assertEquals(16, mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null).size());

    }

    /**
     * Integration test for the registration service
     */
    @Test
    public void testRegistrationServiceOneTrue() throws Exception {
        ManagementService.registerMBeans(manager, mBeanServer, true, false, false, false);
        assertEquals(1, mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null).size());

    }

    /**
     * Integration test for the registration service
     */
    @Test
    public void testRegistrationServiceNoneTrue() throws Exception {
        ManagementService.registerMBeans(manager, mBeanServer, false, false, false, false);
        assertEquals(0, mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null).size());

    }

    /**
     * Can we register the CacheManager MBean?
     */
    @Test
    public void testRegisterCacheManager() throws Exception {
        //Set size so the second element overflows to disk.
        Ehcache ehcache = new net.sf.ehcache.Cache("testNoOverflowToDisk", 1, false, false, 500, 200);
        manager.addCache(ehcache);

        ehcache.put(new Element("key1", "value1"));
        ehcache.put(new Element("key2", "value1"));
        assertNull(ehcache.get("key1"));
        assertNotNull(ehcache.get("key2"));


        ObjectName name = new ObjectName("net.sf.ehcache:type=CacheManager,name=1");
        CacheManager cacheManager = new CacheManager(manager);
        mBeanServer.registerMBean(cacheManager, name);
        mBeanServer.unregisterMBean(name);

        name = new ObjectName("net.sf.ehcache:type=CacheManager.Cache,CacheManager=1,name=testOverflowToDisk");
        mBeanServer.registerMBean(new Cache(ehcache), name);
        mBeanServer.unregisterMBean(name);

        name = new ObjectName("net.sf.ehcache:type=CacheManager.Cache,CacheManager=1,name=sampleCache1");
        mBeanServer.registerMBean(new Cache(manager.getCache("sampleCache1")), name);
        mBeanServer.unregisterMBean(name);

    }


    /**
     * Can we register the CacheManager MBean?
     */
    @Test
    public void testListCachesFromManager() throws Exception {
        ManagementService.registerMBeans(manager, mBeanServer, true, false, false, false);

        Ehcache ehcache = manager.getCache("sampleCache1");

        ehcache.put(new Element("key1", "value1"));
        ehcache.put(new Element("key2", "value1"));
        assertNotNull(ehcache.get("key1"));
        assertNotNull(ehcache.get("key2"));

        ObjectName name = CacheManager.createObjectName(manager);

        Object object = mBeanServer.getAttribute(name, "Status");
        LOG.info(object.toString());

        List caches = (List) mBeanServer.getAttribute(name, "Caches");
        assertEquals(15, caches.size());

        for (int i = 0; i < caches.size(); i++) {
            Cache cache = (Cache) caches.get(i);
            String cacheName = cache.getName();
            CacheStatistics cacheStatistics = cache.getStatistics();
            CacheConfiguration cacheConfiguration = cache.getCacheConfiguration();
            LOG.info(cacheName + " " + cacheStatistics + " " + cacheConfiguration);
        }
    }

    /**
     * Shows that all MBeans are fully traversable locally
     *
     * @throws JMException
     */
    @Test
    public void testTraversalUsingMBeanServer() throws JMException {
        //Test CacheManager
        //not all attributes are accessible due to serializability constraints
        traverseMBeanAttributesUsingMBeanServer("CacheManager");

        //Test Cache
        //not all attributes are accessible due to serializability constraints
        traverseMBeanAttributesUsingMBeanServer("Cache");

        //Test CacheStatistics
        traverseMBeanAttributesUsingMBeanServer("CacheStatistics");

        //Test CacheConfiguration
        traverseMBeanAttributesUsingMBeanServer("CacheConfiguration");

    }


    /**
     * Creates an RMI JMXConnectorServer, connects to it and demonstrates what attributes are traversable.
     * The answer is not all.
     * <p/>
     * Note that this test creates a Registry which will keep running until the JVM Exists. There
     * is no way to stop it but it should do no harm.
     */
    @Test
    public void testJMXConnectorServer() throws Exception {

        ManagementService.registerMBeans(manager, mBeanServer, true, true, true, true);

        LocateRegistry.createRegistry(55000);
        String serverUrl = "service:jmx:rmi:///jndi/rmi://localhost:55000/server";
        JMXServiceURL url = new JMXServiceURL(serverUrl);
        JMXConnectorServer cs = JMXConnectorServerFactory.newJMXConnectorServer(url, null, mBeanServer);
        cs.start();
        JMXConnector connector = cs.toJMXConnector(null);
        connector.connect(null);
        MBeanServerConnection connection = connector.getMBeanServerConnection();
        assertEquals(OBJECTS_IN_TEST_EHCACHE, connection.queryNames(new ObjectName("net.sf.ehcache:*"), null).size());


        Ehcache ehcache = manager.getCache("sampleCache1");

        ehcache.put(new Element("key1", "value1"));
        ehcache.put(new Element("key2", "value1"));
        assertNotNull(ehcache.get("key1"));
        assertNotNull(ehcache.get("key2"));

        //Test CacheManager
        //not all attributes are accessible due to serializability constraints
        //traverseMBeanAttributes(connection, "CacheManager");

        //Test Cache
        //not all attributes are accessible due to serializability constraints
        //traverseMBeanAttributes(connection, "Cache");

        //Test CacheStatistics
        traverseMBeanAttributes(connection, "CacheStatistics");

        //Test CacheConfiguration
        traverseMBeanAttributes(connection, "CacheConfiguration");

        cs.stop();
    }

    private void traverseMBeanAttributes(MBeanServerConnection connection, String type) throws JMException, IOException {
        Set objectNames = connection.queryNames(new ObjectName("net.sf.ehcache:type=" + type + ",*"), null);
        for (Iterator iterator = objectNames.iterator(); iterator.hasNext();) {
            ObjectName objectName = (ObjectName) iterator.next();
            MBeanInfo mBeanInfo = connection.getMBeanInfo(objectName);
            MBeanAttributeInfo[] attributes = mBeanInfo.getAttributes();
            for (MBeanAttributeInfo attribute : attributes) {
                LOG.info(attribute.getName() + " " + connection.getAttribute(objectName, attribute.getName()));
            }
        }
    }

    private void traverseMBeanAttributesUsingMBeanServer(String type) throws JMException {
        Set objectNames = mBeanServer.queryNames(new ObjectName("net.sf.ehcache:type=" + type + ",*"), null);
        for (Iterator iterator = objectNames.iterator(); iterator.hasNext();) {
            ObjectName objectName = (ObjectName) iterator.next();
            MBeanInfo mBeanInfo = mBeanServer.getMBeanInfo(objectName);
            MBeanAttributeInfo[] attributes = mBeanInfo.getAttributes();
            for (MBeanAttributeInfo attribute : attributes) {
                LOG.info(attribute.getName() + " " + mBeanServer.getAttribute(objectName, attribute.getName()));
            }
        }
    }


}

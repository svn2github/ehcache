/**
 *  Copyright 2003-2007 Greg Luck
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

/**
 * These tests use the JDK1.5 platform mbean server
 * To interactively examine behaviour, add a Thread.sleep(...) and add -Dcom.sun.management.jmxremote to the java
 * invocation.
 *
 * On Mac OS X, add -Dcom.sun.management.jmxremote to the java command line for JConsole to see the test
 *
 * @author Greg Luck
 * @version $Id$
 */
public class ManagementServiceTest extends AbstractCacheTest {

    private static final Log LOG = LogFactory.getLog(ManagementServiceTest.class.getName());
    private MBeanServer mBeanServer;


    /**
     * setup test
     */
    protected void setUp() throws Exception {
        super.setUp();
        mBeanServer = createMBeanServer();
    }

    private MBeanServer create14MBeanServer() {
        return MBeanServerFactory.createMBeanServer("SimpleAgent");
    }

    /**
     * teardown
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        //Ensure the CacheManager shutdown clears all ObjectNames from the MBeanServer
        assertEquals(0, mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null).size());
    }


    /**
     * Integration test for the registration service
     */
    public void testRegistrationServiceFourTrue() throws Exception {
        ManagementService.registerMBeans(manager, mBeanServer, true, true, true, true);
        assertEquals(37, mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null).size());
    }

    /**
     * Integration test for the registration service
     */
    public void testRegistrationServiceFourTrueUsing14MBeanServer() throws Exception {
        mBeanServer = create14MBeanServer();
        ManagementService.registerMBeans(manager, mBeanServer, true, true, true, true);
        assertEquals(37, mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null).size());
    }

    /**
     * Integration test for the registration service
     */
    public void testRegistrationServiceListensForCacheChanges() throws Exception {
        ManagementService.registerMBeans(manager, mBeanServer, true, true, true, true);
        assertEquals(37, mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null).size());
        manager.addCache("new cache");
        assertEquals(40, mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null).size());
        manager.removeCache("sampleCache1");
        assertEquals(37, mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null).size());
        //Thread.sleep(1000000);
    }

    /**
     * Integration test for the registration service
     */
    public void testMultipleCacheManagers() throws Exception {
        ManagementService.registerMBeans(manager, mBeanServer, true, true, true, true);
        assertEquals(37, mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null).size());
        File file = new File(AbstractCacheTest.SRC_CONFIG_DIR + "ehcache.xml");
        Configuration configuration = ConfigurationFactory.parseConfiguration(file);
        net.sf.ehcache.CacheManager secondCacheManager = new net.sf.ehcache.CacheManager(configuration);
        ManagementService.registerMBeans(secondCacheManager, mBeanServer, true, true, true, true);
        assertEquals(56, mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null).size());
        secondCacheManager.shutdown();
        assertEquals(37, mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null).size());

    }

    /**
     * Checks that Statistics updates
     */
    public void testStatisticsMBeanUpdatesAsStatsChange() throws Exception {
        ManagementService.registerMBeans(manager, mBeanServer, false, false, false, true);
        Ehcache cache = manager.getCache("sampleCache1");
        ObjectName name = CacheStatistics.createObjectName(manager.getName(), cache.getName());
        assertEquals(new Long(0), mBeanServer.getAttribute(name, "ObjectCount"));
        cache.put(new Element("1", "value"));
        cache.get("1");
        Thread.sleep(20);
        assertEquals(new Long(1), mBeanServer.getAttribute(name, "ObjectCount"));

//        Thread.sleep(1000000);


    }

    /**
     * Integration test for the registration service
     */
    public void testRegistrationServiceThreeTrue() throws Exception {
        ManagementService.registerMBeans(manager, mBeanServer, true, true, true, false);
        assertEquals(25, mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null).size());

    }

    /**
     * Integration test for the registration service
     */
    public void testRegistrationServiceTwoTrue() throws Exception {
        ManagementService.registerMBeans(manager, mBeanServer, true, true, false, false);
        assertEquals(13, mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null).size());

    }

    /**
     * Integration test for the registration service
     */
    public void testRegistrationServiceOneTrue() throws Exception {
        ManagementService.registerMBeans(manager, mBeanServer, true, false, false, false);
        assertEquals(1, mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null).size());

    }

    /**
     * Integration test for the registration service
     */
    public void testRegistrationServiceNoneTrue() throws Exception {
        ManagementService.registerMBeans(manager, mBeanServer, false, false, false, false);
        assertEquals(0, mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null).size());

    }

    /**
     * Can we register the CacheManager MBean?
     */
    public void testRegisterCacheManager() throws Exception {
        //Set size so the second element overflows to disk.
        Ehcache ehcache = new net.sf.ehcache.Cache("testNoOverflowToDisk", 1, false, true, 500, 200);
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
    public void testListCachesFromManager() throws Exception {
        ManagementService.registerMBeans(manager, mBeanServer, true, false, false, false);

        Ehcache ehcache = manager.getCache("sampleCache1");

        ehcache.put(new Element("key1", "value1"));
        ehcache.put(new Element("key2", "value1"));
        assertNotNull(ehcache.get("key1"));
        assertNotNull(ehcache.get("key2"));

        ObjectName name = CacheManager.createObjectName(manager);

        Object object = mBeanServer.getAttribute(name, "Status");
        LOG.info(object);

        List caches = (List) mBeanServer.getAttribute(name, "Caches");
        assertEquals(12, caches.size());

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
     * @throws JMException
     */
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
     *
     * Note that this test creates a Registry which will keep running until the JVM Exists. There
     * is no way to stop it but it should do no harm.
     *
     *
     */
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
        assertEquals(37, connection.queryNames(new ObjectName("net.sf.ehcache:*"), null).size());


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
            for (int i = 0; i < attributes.length; i++) {
                MBeanAttributeInfo attribute = attributes[i];
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
            for (int i = 0; i < attributes.length; i++) {
                MBeanAttributeInfo attribute = attributes[i];
                LOG.info(attribute.getName() + " " + mBeanServer.getAttribute(objectName, attribute.getName()));
            }
        }
    }


}
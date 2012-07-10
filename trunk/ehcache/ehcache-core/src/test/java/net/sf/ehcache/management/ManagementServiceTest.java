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

package net.sf.ehcache.management;

import static org.junit.Assert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.ExportException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

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

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import net.sf.ehcache.constructs.blocking.BlockingCache;
import net.sf.ehcache.store.disk.DiskStoreHelper;

import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.Is;
import org.hibernate.type.CollectionType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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
        assertThat(mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null), IsEmptyCollection.<ObjectName>empty());
    }


    /**
     * Integration test for the registration service
     */
    @Test
    public void testRegistrationServiceFourTrue() throws Exception {
        ManagementService.registerMBeans(manager, mBeanServer, true, true, true, true, true);
        assertThat(mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null), hasSize(OBJECTS_IN_TEST_EHCACHE));
    }

    /**
     * Integration test for the registration service
     */
    @Test
    public void testRegistrationServiceFourTrueUsing14MBeanServer() throws Exception {
        mBeanServer = create14MBeanServer();
        ManagementService.registerMBeans(manager, mBeanServer, true, true, true, true, true);
        assertThat(mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null), hasSize(OBJECTS_IN_TEST_EHCACHE));
    }


    /**
     * Integration test for the registration service using a contructed ManagementService as would be done
     * by an IoC container.
     */
    @Test
    public void testRegistrationServiceFourTrueUsing14MBeanServerWithConstructorInjection() throws Exception {
        mBeanServer = create14MBeanServer();
        ManagementService managementService = new ManagementService(manager, mBeanServer, true, true, true, true, true);
        managementService.init();
        assertThat(mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null), hasSize(OBJECTS_IN_TEST_EHCACHE));
    }

    /**
     * Integration test for the registration service
     */
    @Test
    public void testRegistrationServiceListensForCacheChanges() throws Exception {
        ManagementService.registerMBeans(manager, mBeanServer, true, true, true, true, true);
        assertThat(mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null), hasSize(OBJECTS_IN_TEST_EHCACHE));
        manager.addCache("new cache");
        assertThat(mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null), hasSize(OBJECTS_IN_TEST_EHCACHE + 3));
        manager.removeCache("sampleCache1");
        assertThat(mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null), hasSize(OBJECTS_IN_TEST_EHCACHE));
    }

    /**
     * Integration test for the registration service
     */
    @Test
    public void testMultipleCacheManagers() throws Exception {
        ManagementService.registerMBeans(manager, mBeanServer, true, true, true, true, true);
        assertThat(mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null), hasSize(OBJECTS_IN_TEST_EHCACHE));
        File file = new File(AbstractCacheTest.SRC_CONFIG_DIR + "ehcache.xml");
        Configuration configuration = ConfigurationFactory.parseConfiguration(file).name("cm-2");
        net.sf.ehcache.CacheManager secondCacheManager = new net.sf.ehcache.CacheManager(configuration);
        ManagementService.registerMBeans(secondCacheManager, mBeanServer, true, true, true, true, true);
        assertThat(mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null), hasSize(OBJECTS_IN_TEST_EHCACHE + 19));
        secondCacheManager.shutdown();
        assertThat(mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null), hasSize(OBJECTS_IN_TEST_EHCACHE));
    }

    /**
     * Checks that Statistics updates
     */
    @Test
    public void testStatisticsMBeanUpdatesAsStatsChange() throws Exception {
        ManagementService.registerMBeans(manager, mBeanServer, false, false, false, true, false);
        Ehcache cache = manager.getCache("sampleCache1");
        ObjectName name = CacheStatistics.createObjectName(manager.getName(), cache.getName());
        assertThat(mBeanServer.getAttribute(name, "ObjectCount"), Is.<Object>is(Long.valueOf(0L)));
        cache.put(new Element("1", "value"));
        cache.get("1");
        DiskStoreHelper.flushAllEntriesToDisk((net.sf.ehcache.Cache)cache).get();
        assertThat(mBeanServer.getAttribute(name, "ObjectCount"), Is.<Object>is(Long.valueOf(1L)));
        assertThat(mBeanServer.getAttribute(name, "MemoryStoreObjectCount"), Is.<Object>is(Long.valueOf(1L)));
        assertThat(mBeanServer.getAttribute(name, "DiskStoreObjectCount"), Is.<Object>is(Long.valueOf(1L)));
    }

    /**
     * Integration test for the registration service
     */
    @Test
    public void testRegistrationServiceThreeTrue() throws Exception {
        ManagementService.registerMBeans(manager, mBeanServer, true, true, true, false, false);
        assertThat(mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null), hasSize(31));

    }

    /**
     * Integration test for the registration service
     */
    @Test
    public void testRegistrationServiceTwoTrue() throws Exception {
        ManagementService.registerMBeans(manager, mBeanServer, true, true, false, false, false);
        assertThat(mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null), hasSize(16));
    }

    /**
     * Integration test for the registration service
     */
    @Test
    public void testRegistrationServiceOneTrue() throws Exception {
        ManagementService.registerMBeans(manager, mBeanServer, true, false, false, false, false);
        assertThat(mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null), hasSize(1));
    }

    /**
     * Integration test for the registration service
     */
    @Test
    public void testRegistrationServiceNoneTrue() throws Exception {
        ManagementService.registerMBeans(manager, mBeanServer, false, false, false, false, false);
        assertThat(mBeanServer.queryNames(new ObjectName("net.sf.ehcache:*"), null), IsEmptyCollection.<ObjectName>empty());
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
        assertThat(ehcache.get("key1"), nullValue());
        assertThat(ehcache.get("key2"), notNullValue());


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
        ManagementService.registerMBeans(manager, mBeanServer, true, false, false, false, false);

        Ehcache ehcache = manager.getCache("sampleCache1");

        ehcache.put(new Element("key1", "value1"));
        ehcache.put(new Element("key2", "value1"));
        assertThat(ehcache.get("key1"), notNullValue());
        assertThat(ehcache.get("key2"), notNullValue());

        ObjectName name = CacheManager.createObjectName(manager);

        Object object = mBeanServer.getAttribute(name, "Status");
        LOG.info(object.toString());

        List<?> caches = (List<?>) mBeanServer.getAttribute(name, "Caches");
        assertThat(caches, hasSize(15));

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

        ManagementService.registerMBeans(manager, mBeanServer, true, true, true, true, true);

        int registryPort = startRegistry(50000, 60000, 100);
        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:" + registryPort + "/ManagementServiceTest/testJMXConnectorServer");
        JMXConnectorServer cs = JMXConnectorServerFactory.newJMXConnectorServer(url, null, mBeanServer);
        cs.start();
        JMXConnector connector = cs.toJMXConnector(null);
        connector.connect(null);
        MBeanServerConnection connection = connector.getMBeanServerConnection();
        assertThat(connection.queryNames(new ObjectName("net.sf.ehcache:*"), null), hasSize(OBJECTS_IN_TEST_EHCACHE));


        Ehcache ehcache = manager.getCache("sampleCache1");

        ehcache.put(new Element("key1", "value1"));
        ehcache.put(new Element("key2", "value1"));
        assertThat(ehcache.get("key1"), notNullValue());
        assertThat(ehcache.get("key2"), notNullValue());

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

    private int startRegistry(int portRangeStart, int portRangeEnd, int attempts) throws RemoteException {
        Random rndm = new Random();
        for (int i = 0; ; i++) {
            int candidatePort = rndm.nextInt(portRangeEnd - portRangeStart) + portRangeStart;
            LOG.info("Attempting to start RMI registry on port " + candidatePort);
            try {
                LocateRegistry.createRegistry(candidatePort);
                return candidatePort;
            } catch (ExportException e) {
                if (e.getCause() instanceof BindException) {
                    LOG.warn("Failed to bind to port " + candidatePort);
                }
                if (i >= attempts) {
                    throw e;
                }
            }
        }
    }

    @Test
    public void testSupportsDecoratedCaches() {
        ManagementService.registerMBeans(manager, mBeanServer, true, true, true, true, true);

        net.sf.ehcache.Cache cache = new net.sf.ehcache.Cache(new net.sf.ehcache.config.CacheConfiguration("decoratedCache", 1000));
        BlockingCache blockingCache = new BlockingCache(cache);

        manager.addCacheIfAbsent(blockingCache);
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

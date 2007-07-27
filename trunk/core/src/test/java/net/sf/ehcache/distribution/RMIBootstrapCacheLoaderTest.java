/**
 *  Copyright 2003-2007 Luck Consulting Pty Ltd
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

package net.sf.ehcache.distribution;

import junit.framework.TestCase;
import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CountingCacheEventListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Date;

/**
 * @author Greg Luck
 * @version $Id$
 */
public class RMIBootstrapCacheLoaderTest extends TestCase {


    /**
     * A value to represent replicate asynchronously
     */
    protected static final boolean ASYNCHRONOUS = true;

    /**
     * A value to represent replicate synchronously
     */
    protected static final boolean SYNCHRONOUS = false;

    private static final Log LOG = LogFactory.getLog(RMICacheReplicatorTest.class.getName());

    /**
     * CacheManager 1 in the cluster
     */
    protected CacheManager manager1;
    /**
     * CacheManager 2 in the cluster
     */
    protected CacheManager manager2;
    /**
     * CacheManager 3 in the cluster
     */
    protected CacheManager manager3;
    /**
     * CacheManager 4 in the cluster
     */
    protected CacheManager manager4;
    /**
     * CacheManager 5 in the cluster
     */
    protected CacheManager manager5;
    /**
     * CacheManager 6 in the cluster
     */
    protected CacheManager manager6;

    /**
     * The name of the cache under test
     */
    protected String cacheName = "sampleCache1";

    /**
     * {@inheritDoc}
     * Sets up two caches: cache1 is local. cache2 is to be receive updates
     *
     * @throws Exception
     */
    protected void setUp() throws Exception {
        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        MulticastKeepaliveHeartbeatSender.setHeartBeatInterval(1000);

        CountingCacheEventListener.resetCounters();
        manager1 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed1.xml");
        manager2 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed2.xml");

        //manager6 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed-jndi6.xml");


        //allow cluster to be established
        Thread.sleep(3000);
    }

    /**
     * Force the VM to grow to its full size. This stops SoftReferences from being reclaimed in favour of
     * Heap growth. Only an issue when a VM is cold.
     */
    protected void forceVMGrowth() {
        byte[] forceVMGrowth = new byte[50000000];
    }


    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    protected void tearDown() throws Exception {

        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        if (manager1 != null) {
            manager1.shutdown();
        }
        if (manager2 != null) {
            manager2.shutdown();
        }
        if (manager3 != null) {
            manager3.shutdown();
        }
        if (manager4 != null) {
            manager4.shutdown();
        }
        if (manager5 != null) {
            manager5.shutdown();
        }
        if (manager6 != null) {
            manager6.shutdown();
        }
    }

    /**
     * Tests loading from bootstrap
     */
    public void testBootstrapFromClusterWithAsyncLoader() throws CacheException, InterruptedException {

        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        forceVMGrowth();

        //Give everything a chance to startup
        Integer index = null;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 1000; j++) {
                index = new Integer(((1000 * i) + j));
                manager1.getCache("sampleCache1").put(new Element(index,
                        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            }

        }
        assertEquals(2000, manager1.getCache("sampleCache1").getSize());

        Thread.sleep(8000);
        assertEquals(2000, manager2.getCache("sampleCache1").getSize());

        manager3 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed3.xml");
        Thread.sleep(5000);
        assertEquals(2000, manager3.getCache("sampleCache1").getSize());


    }

    /**
     * Tests loading from bootstrap
     */
    public void testBootstrapFromClusterWithSyncLoader() throws CacheException, InterruptedException {

        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        forceVMGrowth();

        //Give everything a chance to startup
        Integer index = null;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 1000; j++) {
                index = new Integer(((1000 * i) + j));
                manager1.getCache("sampleCache2").put(new Element(index,
                        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            }

        }

        assertEquals(2000, manager1.getCache("sampleCache2").getSize());

        Thread.sleep(8000);
        assertEquals(2000, manager2.getCache("sampleCache2").getSize());

        manager3 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed3.xml");
        //Should not need to wait because the load is synchronous
        //Thread.sleep(10000);
        assertEquals(2000, manager3.getCache("sampleCache2").getSize());


    }


    /**
     * Create the same named cache in two CacheManagers. Populate the first one. Check that the second one gets the
     * entries.
     */
    public void testAddCacheAndBootstrapOccurs() throws InterruptedException {

        manager1.addCache("testBootstrap1");
        Cache testBootstrap1 = manager1.getCache("testBootstrap1");
        for (int i = 0; i < 1000; i++) {
            testBootstrap1.put(new Element("key" + i, new Date()));
        }

        manager2.addCache("testBootstrap1");
        Cache testBootstrap2 = manager2.getCache("testBootstrap1");
        //wait for async bootstrap
        Thread.sleep(3000);
        assertEquals(1000, testBootstrap2.getSize());


    }


}

/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
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

package net.sf.ehcache.distribution.jgroups;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Cache;
import net.sf.ehcache.distribution.MulticastKeepaliveHeartbeatSender;

import java.util.logging.Logger;
import java.util.Date;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Greg Luck
 */
public class JGroupsBootstrapCacheLoaderTest {

    /**
     * A value to represent replicate asynchronously
     */
    protected static final boolean ASYNCHRONOUS = true;

    /**
     * A value to represent replicate synchronously
     */
    protected static final boolean SYNCHRONOUS = false;

    private static final Logger LOG = Logger.getLogger(JGroupsBootstrapCacheLoaderTest.class.getName());

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
     * The name of the cache under test
     */
    protected String cacheName = "sampleCacheAsync";

    /**
     * {@inheritDoc}
     * Sets up two caches: cache1 is local. cache2 is to be receive updates
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {

        MulticastKeepaliveHeartbeatSender.setHeartBeatInterval(1000);

        manager1 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/jgroups/ehcache-distributed-jgroups.xml");
        manager2 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/jgroups/ehcache-distributed-jgroups.xml");

        //allow cluster to be established
        Thread.sleep(10000);
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
    @After
    public void tearDown() throws Exception {

        if (manager1 != null) {
            manager1.shutdown();
        }
        if (manager2 != null) {
            manager2.shutdown();
        }
        if (manager3 != null) {
            manager3.shutdown();
        }
    }

    /**
     * Tests loading from bootstrap
     */
    @Test
    public void testBootstrapFromClusterWithAsyncLoader() throws CacheException, InterruptedException {

        forceVMGrowth();

        //Give everything a chance to startup
        Integer index = null;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 1000; j++) {
                index = new Integer(((1000 * i) + j));
                manager2.getCache("sampleCacheAsync").put(new Element(index,
                        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            }

        }
        assertEquals(2000, manager2.getCache("sampleCacheAsync").getSize());

        Thread.sleep(8000);
        assertEquals(2000, manager1.getCache("sampleCacheAsync").getSize());

        manager3 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/jgroups/ehcache-distributed-jgroups.xml");
        Thread.sleep(8000);
        assertEquals(2000, manager3.getCache("sampleCacheAsync").getSize());


    }

    /**
     * Tests loading from bootstrap
     */
    @Test
    public void testBootstrapFromClusterWithSyncLoader() throws CacheException, InterruptedException {

        forceVMGrowth();

        //Give everything a chance to startup
        Integer index = null;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 1000; j++) {
                index = new Integer(((1000 * i) + j));
                manager2.getCache("sampleCacheAsync2").put(new Element(index,
                        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            }

        }

        assertEquals(2000, manager2.getCache("sampleCacheAsync2").getSize());

        Thread.sleep(8000);
        //normal replication
        assertEquals(2000, manager1.getCache("sampleCacheAsync2").getSize());

        manager3 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/jgroups/ehcache-distributed-jgroups.xml");

        //Should not need to wait because the load is synchronous
        Thread.sleep(10000);
        assertEquals(2000, manager3.getCache("sampleCacheAsync2").getSize());


    }


    /**
     * Create the same named cache in two CacheManagers. Populate the first one. Check that the second one gets the
     * entries.
     */
    @Test
    public void testAddCacheAndBootstrapOccurs() throws InterruptedException {

        manager1.addCache("testBootstrap1");
        Cache testBootstrap1 = manager1.getCache("testBootstrap1");
        for (int i = 0; i < 1000; i++) {
            testBootstrap1.put(new Element("key" + i, new Date()));
        }



        manager2.addCache("testBootstrap1");
        Cache testBootstrap2 = manager2.getCache("testBootstrap1");
        //wait for async bootstrap

        Thread.sleep(8000);
        assertEquals(1000, testBootstrap2.getSize());


    }

    /**
     * Not supported.
     */
    @Test
    public void testSynchronousBootstrap() {

        try {
            manager1 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/jgroups/ehcache-distributed-jgroups-sync.xml");
            fail();
        } catch (UnsupportedOperationException e) {
            //expected
        }

    }


}


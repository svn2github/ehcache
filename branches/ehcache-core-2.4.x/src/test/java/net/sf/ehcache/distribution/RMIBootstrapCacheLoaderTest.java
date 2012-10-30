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

package net.sf.ehcache.distribution;


import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.util.RetryAssert;

import org.hamcrest.collection.IsEmptyCollection;
import org.junit.After;

import static org.junit.Assert.assertEquals;
import static org.hamcrest.core.IsEqual.equalTo;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Greg Luck
 * @version $Id$
 */
public class RMIBootstrapCacheLoaderTest extends AbstractRMITest {


    /**
     * A value to represent replicate asynchronously
     */
    protected static final boolean ASYNCHRONOUS = true;

    /**
     * A value to represent replicate synchronously
     */
    protected static final boolean SYNCHRONOUS = false;

    private static final Logger LOG = LoggerFactory.getLogger(RMIBootstrapCacheLoaderTest.class.getName());

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
     * {@inheritDoc}
     * Sets up two caches: cache1 is local. cache2 is to be receive updates
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        List<Configuration> configurations = new ArrayList<Configuration>();
        configurations.add(getConfiguration(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed1.xml").name("cm1"));
        configurations.add(getConfiguration(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed2.xml").name("cm2"));

        List<CacheManager> managers = startupManagers(configurations);
        manager1 = managers.get(0);
        manager2 = managers.get(1);

        //allow cluster to be established
        waitForClusterMembership(10, TimeUnit.SECONDS, Arrays.asList("sampleCache1", "sampleCache2"), manager1, manager2);
    }

    /**
     * Force the VM to grow to its full size. This stops SoftReferences from being reclaimed in favour of
     * Heap growth. Only an issue when a VM is cold.
     */
    protected void forceVMGrowth() {
        Object[] arrays = new Object[40];
        for (int i = 0; i < arrays.length; i++) {
            arrays[i] = new byte[1024 * 1024];
        }
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

        RetryAssert.assertBy(30, TimeUnit.SECONDS, new Callable<Set<Thread>>() {
            public Set<Thread> call() throws Exception {
                return getActiveReplicationThreads();
            }
        }, IsEmptyCollection.<Thread>empty());
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
                index = Integer.valueOf(((1000 * i) + j));
                manager2.getCache("sampleCache1").put(new Element(index,
                        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            }

        }
        assertEquals(2000, manager2.getCache("sampleCache1").getSize());

        RetryAssert.assertBy(16, TimeUnit.SECONDS, RetryAssert.sizeOf(manager1.getCache("sampleCache1")), equalTo(2000));

        manager3 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed3.xml");
        RetryAssert.assertBy(10, TimeUnit.SECONDS, RetryAssert.sizeOf(manager3.getCache("sampleCache1")), equalTo(2000));
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
                index = Integer.valueOf(((1000 * i) + j));
                manager2.getCache("sampleCache2").put(new Element(index,
                        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            }

        }

        assertEquals(2000, manager2.getCache("sampleCache2").getSize());

        RetryAssert.assertBy(16, TimeUnit.SECONDS, RetryAssert.sizeOf(manager1.getCache("sampleCache2")), equalTo(2000));

        manager3 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed3.xml");
        //Should not need to wait because the load is synchronous
        //Thread.sleep(10000);
        assertEquals(2000, manager3.getCache("sampleCache2").getSize());


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
        RetryAssert.assertBy(6, TimeUnit.SECONDS, RetryAssert.sizeOf(testBootstrap2), equalTo(1000));


    }


}

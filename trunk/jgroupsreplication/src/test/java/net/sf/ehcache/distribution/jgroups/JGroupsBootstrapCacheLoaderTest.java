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

import static org.junit.Assert.assertEquals;

import java.util.Date;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Greg Luck
 */
public class JGroupsBootstrapCacheLoaderTest {
    private static final long MAX_WAIT_TIME = 60000;


    
    private static final Logger LOG = LoggerFactory.getLogger(JGroupsBootstrapCacheLoaderTest.class.getName());
    
    /**
     * Used for getting the current test's name
     */
    @Rule public TestName name = new TestName();
    
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
        CacheTestUtilities.startTest(name.getMethodName());
        LOG.info("SETUP");

        manager1 = new CacheManager(CacheTestUtilities.ASYNC_CONFIG_URL1);
        CacheTestUtilities.waitForBootstrap(manager1, MAX_WAIT_TIME);
        
        manager2 = new CacheManager(CacheTestUtilities.ASYNC_CONFIG_URL2);
        CacheTestUtilities.waitForBootstrap(manager2, MAX_WAIT_TIME);
    }

    /**
     * Force the VM to grow to its full size. This stops SoftReferences from being reclaimed in favor of
     * Heap growth. Only an issue when a VM is cold.
     */
    protected byte[] forceVMGrowth() {
        return new byte[50000000];
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
        
        LOG.info("TEARDOWN");
        CacheTestUtilities.endTest();
    }

    /**
     * Tests loading from bootstrap
     */
    @Test
    public void testBootstrapFromClusterWithAsyncLoader() throws CacheException, InterruptedException {
        LOG.info("START TEST");
        
        forceVMGrowth();
        
        final Cache cache1 = manager1.getCache("sampleCacheAsync");
        final Cache cache2 = manager2.getCache("sampleCacheAsync");
        
        final int testElementCount = 2000;

        //Give everything a chance to startup
        for (int i = 0; i < testElementCount; i++) {
            final int value = i + 1000;
            cache2.put(new Element(value,
                      value + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        }
        assertEquals(testElementCount, cache2.getSize());

        //Wait for normal replication to complete
        CacheTestUtilities.waitForReplication(testElementCount, MAX_WAIT_TIME, cache1);
        
        //Verify normal replication worked
        assertEquals(testElementCount, cache1.getSize());

        manager3 = new CacheManager(CacheTestUtilities.ASYNC_CONFIG_URL3);
        //Wait for bootstrap to complete
        CacheTestUtilities.waitForBootstrap(manager3, MAX_WAIT_TIME);
        
        final Cache cache3 = manager3.getCache("sampleCacheAsync");
        assertEquals(testElementCount, cache3.getSize());

        LOG.info("END TEST");
    }

    /**
     * Tests loading from bootstrap
     */
    @Test
    public void testBootstrapFromClusterWithSyncLoader() throws CacheException, InterruptedException {
        LOG.info("START TEST");
        
        forceVMGrowth();
        
        final Cache cache1 = manager1.getCache("sampleCacheAsync2");
        final Cache cache2 = manager2.getCache("sampleCacheAsync2");
        
        final int testElementCount = 2000;

        //Give everything a chance to startup
        for (int i = 0; i < testElementCount; i++) {
            final int value = i + 1000;
            cache2.put(new Element(value,
                      value + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        }
        assertEquals(testElementCount, cache2.getSize());

        //Wait for normal replication to complete
        CacheTestUtilities.waitForReplication(testElementCount, MAX_WAIT_TIME, cache1);
        
        //Verify normal replication worked
        assertEquals(testElementCount, cache1.getSize());

        manager3 = new CacheManager(CacheTestUtilities.ASYNC_CONFIG_URL3);
        //Wait for bootstrap to complete
        CacheTestUtilities.waitForBootstrap(manager3, MAX_WAIT_TIME);
        
        final Cache cache3 = manager3.getCache("sampleCacheAsync2");
        assertEquals(testElementCount, cache3.getSize());

        LOG.info("END TEST");
    }


    /**
     * Create the same named cache in two CacheManagers. Populate the first one. Check that the second one gets the
     * entries.
     */
    @Test
    public void testAddCacheAndBootstrapOccurs() throws InterruptedException {
        LOG.info("START TEST");
        
        final int expectedSize = 1000;

        manager1.addCache("testBootstrap1");
        Cache testBootstrap1 = manager1.getCache("testBootstrap1");
        for (int i = 0; i < expectedSize; i++) {
            testBootstrap1.put(new Element("key" + (i + 1000), new Date()));
        }
        CacheTestUtilities.waitForBootstrap(manager1, MAX_WAIT_TIME);


        manager2.addCache("testBootstrap1");
        Cache testBootstrap2 = manager2.getCache("testBootstrap1");
        //wait for async bootstrap
        CacheTestUtilities.waitForBootstrap(manager2, MAX_WAIT_TIME);

        //Wait for normal replication to complete
        CacheTestUtilities.waitForReplication(expectedSize, MAX_WAIT_TIME, testBootstrap2);
        
        assertEquals(expectedSize, testBootstrap2.getSize());

        LOG.info("END TEST");
    }
}


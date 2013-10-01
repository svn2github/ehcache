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

package net.sf.ehcache.distribution;


import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.util.RetryAssert;

import org.hamcrest.collection.IsEmptyCollection;
import org.junit.After;

import static org.junit.Assert.assertEquals;
import static org.hamcrest.core.IsEqual.equalTo;

import org.junit.Test;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import net.sf.ehcache.config.CacheConfiguration;

import org.hamcrest.core.Is;
import org.junit.Assert;

import static net.sf.ehcache.distribution.AbstractRMITest.createAsynchronousCache;
import static net.sf.ehcache.distribution.AbstractRMITest.createRMICacheManagerConfiguration;

/**
 * @author Greg Luck
 * @version $Id$
 */
public class RMIBootstrapCacheLoaderTest extends AbstractRMITest {

    /**
     * Force the VM to grow to its full size. This stops SoftReferences from being reclaimed in favour of
     * Heap growth. Only an issue when a VM is cold.
     */
    protected void forceVMGrowth() {
        try {
            Object[] arrays = new Object[40];
            for (int i = 0; i < arrays.length; i++) {
                arrays[i] = new byte[1024 * 1024];
            }
        } catch (OutOfMemoryError e) {
            // ignore
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
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
        CacheManager manager = new CacheManager(createRMICacheManagerConfiguration()
                .cache(createAsynchronousCache().name("asynchronousCache"))
                .name("testBootstrapFromClusterWithAsyncLoader-1"));
        try {
            forceVMGrowth();

            //Give everything a chance to startup
            for (int i = 0; i < 2000; i++) {
                manager.getCache("asynchronousCache").put(new Element(i,
                        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            }
            assertEquals(2000, manager.getCache("asynchronousCache").getSize());

            CacheManager bootstrapManager = new CacheManager(createRMICacheManagerConfiguration()
                    .cache(createAsynchronousCache()
                    .bootstrapCacheLoaderFactory(new CacheConfiguration.BootstrapCacheLoaderFactoryConfiguration()
                    .className("net.sf.ehcache.distribution.RMIBootstrapCacheLoaderFactory")
                    .properties("bootstrapAsynchronously=true,maximumChunkSizeBytes=5000000"))
                    .name("asynchronousCache"))
                    .name("testBootstrapFromClusterWithAsyncLoader-2"));
            try {
                RetryAssert.assertBy(10, TimeUnit.SECONDS, RetryAssert.sizeOf(bootstrapManager.getCache("asynchronousCache")), equalTo(2000));
            } finally {
                bootstrapManager.shutdown();
            }
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Tests loading from bootstrap
     */
    @Test
    public void testBootstrapFromClusterWithSyncLoader() throws CacheException, InterruptedException {
        CacheManager manager = new CacheManager(createRMICacheManagerConfiguration()
                .cache(createAsynchronousCache().name("asynchronousCache"))
                .name("testBootstrapFromClusterWithSyncLoader-1"));
        try {
            forceVMGrowth();

            //Give everything a chance to startup
            for (int i = 0; i < 2000; i++) {
                manager.getCache("asynchronousCache").put(new Element(i,
                        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            }
            assertEquals(2000, manager.getCache("asynchronousCache").getSize());

            CacheManager bootstrapManager = new CacheManager(createRMICacheManagerConfiguration()
                    .cache(createAsynchronousCache()
                    .bootstrapCacheLoaderFactory(new CacheConfiguration.BootstrapCacheLoaderFactoryConfiguration()
                    .className("net.sf.ehcache.distribution.RMIBootstrapCacheLoaderFactory")
                    .properties("bootstrapAsynchronously=false,maximumChunkSizeBytes=5000000"))
                    .name("asynchronousCache"))
                    .name("testBootstrapFromClusterWithSyncLoader-2"));
            try {
                Assert.assertThat(bootstrapManager.getCache("asynchronousCache").getSize(), Is.is(2000));
            } finally {
                bootstrapManager.shutdown();
            }
        } finally {
            manager.shutdown();
        }



    }


    /**
     * Create the same named cache in two CacheManagers. Populate the first one. Check that the second one gets the
     * entries.
     */
    @Test
    public void testAddCacheAndBootstrapOccurs() throws InterruptedException {
        CacheManager manager = new CacheManager(createRMICacheManagerConfiguration()
                .defaultCache(createAsynchronousCache())
                .name("testAddCacheAndBootstrapOccurs-1"));
        try {
            manager.addCache("testBootstrap");
            Cache test = manager.getCache("testBootstrap");
            for (int i = 0; i < 1000; i++) {
                test.put(new Element(i, new Date()));
            }

            CacheManager bootstrapManager = new CacheManager(createRMICacheManagerConfiguration()
                    .defaultCache(createAsynchronousCache()
                    .bootstrapCacheLoaderFactory(new CacheConfiguration.BootstrapCacheLoaderFactoryConfiguration()
                    .className("net.sf.ehcache.distribution.RMIBootstrapCacheLoaderFactory")
                    .properties("bootstrapAsynchronously=true,maximumChunkSizeBytes=5000000")))
                    .name("testBootstrapFromClusterWithSyncLoader-2"));
            try {
                bootstrapManager.addCache("testBootstrap");
                Cache testBootstrap = bootstrapManager.getCache("testBootstrap");
                //wait for async bootstrap
                RetryAssert.assertBy(60, TimeUnit.SECONDS, RetryAssert.sizeOf(testBootstrap), equalTo(1000));
            } finally {
                bootstrapManager.shutdown();
            }
        } finally {
            manager.shutdown();
        }
    }
}

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

package net.sf.ehcache.distribution.jgroups;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.util.ClassLoaderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author Eric Dalquist
 * @version $Revision$
 */
public final class CacheTestUtilities {
    /**
     * config file location. We have three thats less us test, with a manual change, different ways of configuring
     * JGroups
     */
    public static final URL ASYNC_CONFIG_URL1 =
            ClassLoaderUtil.getStandardClassLoader().getResource("distribution/jgroups/ehcache-distributed-jgroups-file-manager1.xml");
    /**
     * Config file 2
     */
    public static final URL ASYNC_CONFIG_URL2 =
            ClassLoaderUtil.getStandardClassLoader().getResource("distribution/jgroups/ehcache-distributed-jgroups-file-manager2.xml");
    /**
     * Config file 3
     */
    public static final URL ASYNC_CONFIG_URL3 =
            ClassLoaderUtil.getStandardClassLoader().getResource("distribution/jgroups/ehcache-distributed-jgroups-file-manager3.xml");
    /**
     * Config file 4
     */
    public static final URL ASYNC_CONFIG_URL4 =
            ClassLoaderUtil.getStandardClassLoader().getResource("distribution/jgroups/ehcache-distributed-jgroups-file-manager4.xml");

    private static final ThreadLocal<String> THREAD_NAME = new ThreadLocal<String>();

    private static final Logger LOG = LoggerFactory.getLogger(CacheTestUtilities.class.getName());
    private static final long WAIT_FOR_REPLICATION_TIME = 10;

    
    private CacheTestUtilities() {
    }
    
    /**
     * Update the thread name with the test's name
     */
    public static void startTest(String name) {
        final Thread currentThread = Thread.currentThread();
        final String oldName = currentThread.getName();
        THREAD_NAME.set(oldName);
        currentThread.setName(oldName + " - " + name);
    }
    
    /**
     * Reset the thread name
     */
    public static void endTest() {
        final Thread currentThread = Thread.currentThread();
        final String oldName = THREAD_NAME.get();
        currentThread.setName(oldName);
        THREAD_NAME.remove();
    }
    
    /**
     * Wait for the specified cache manager to complete bootstrapping operations. Useful for waiting for async bootstrap requests
     * to complete.
     */
    public static void waitForBootstrap(CacheManager cacheManager, long duration) {
        final JGroupsCacheManagerPeerProvider cachePeerProvider = JGroupsCacheManagerPeerProvider.getCachePeerProvider(cacheManager);
        final JGroupsBootstrapManager bootstrapManager = cachePeerProvider.getBootstrapManager();
        
        LOG.debug("Waiting for bootstrap of {} to complete", cacheManager.getName());
        if (!bootstrapManager.waitForCompleteBootstrap(duration)) {
            LOG.warn("Failed to wait for bootstrap of {} to complete", cacheManager.getName());
        } else {
            LOG.debug("Bootstrap of {} complete", cacheManager.getName());
        }
    }
    
    /**
     * Waits for cache(s) to reach a specific size.
     */
    public static void waitForReplication(int expectedSize, long maxWait, Ehcache... caches) throws InterruptedException {
        if (maxWait <= 0) {
            return;
        }

        final List<Ehcache> cacheList = new ArrayList<Ehcache>(Arrays.asList(caches));
        
        final long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < maxWait && !cacheList.isEmpty()) {
            for (final Iterator<Ehcache> cacheItr = cacheList.iterator(); cacheItr.hasNext();) {
                final Ehcache cache = cacheItr.next();
                if (expectedSize != cache.getSize()) {
                    Thread.sleep(WAIT_FOR_REPLICATION_TIME);
                } else {
                    cacheItr.remove();
                }
            }
        }
        final long waited = System.currentTimeMillis() - start;
        
        for (final Ehcache cache : caches) {
            final int size = cache.getSize();
            if (expectedSize != size) {
                LOG.warn("Cache {} failed to reach expected size {}, size is {}", new Object[] {cache.getName(), expectedSize, size});
            }
        }
        
        LOG.debug("Waited {}ms out of {}ms alloted.", waited, maxWait);
    }
}

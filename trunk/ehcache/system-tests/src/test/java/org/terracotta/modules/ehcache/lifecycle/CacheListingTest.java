/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terracotta.modules.ehcache.lifecycle;

import net.sf.ehcache.Cache;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.toolkit.Toolkit;

import com.tc.test.config.model.TestConfig;
import com.terracotta.entity.ClusteredEntityManager;
import com.terracotta.entity.ehcache.ClusteredCache;
import com.terracotta.entity.ehcache.ClusteredCacheManager;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * CacheListingTest
 */
public class CacheListingTest extends AbstractCacheTestBase {
    public CacheListingTest(TestConfig testConfig) {
        super("lifecycle/cache-listing.xml", testConfig, CacheManagerCreateClient.class, ClusteredEntityClient.class);
    }

    public static class CacheManagerCreateClient extends ClientBase {
        public CacheManagerCreateClient(String[] args) {
            super(args);
        }

        @Override
        protected void runTest(Cache cache, Toolkit myToolkit) throws Throwable {
            // Client setup already created CacheManager and caches
            // Signalling for other client to check it can be listed
            getBarrierForAllClients().await(10, TimeUnit.SECONDS);

            // Waiting for other client to finish listing assertions
            getBarrierForAllClients().await(1, TimeUnit.MINUTES);
        }
    }

    public static class ClusteredEntityClient extends ClientBase {
        public ClusteredEntityClient(String[] args) {
            super(args);
        }

        @Override
        protected void setupCacheManager() {
            // Do nothing here
        }

        @Override
        protected Cache getCache() {
            // Do nothing here
            return null;
        }

        @Override
        protected void runTest(Cache cache, Toolkit myToolkit) throws Throwable {
            // Waiting for CM to be created
            getBarrierForAllClients().await(1, TimeUnit.MINUTES);

            ClusteredEntityManager clusteredEntityManager = new ClusteredEntityManager(myToolkit);
            Configuration configuration = ConfigurationFactory.parseConfiguration(getEhcacheXmlAsStream());
            Map<String, ClusteredCacheManager> cacheManagers = clusteredEntityManager.getRootEntities(ClusteredCacheManager.class);

            ClusteredCacheManager clusteredCacheManager = cacheManagers.get(configuration.getName());
            Map<String,ClusteredCache> caches = clusteredCacheManager.getCaches();

            assertTrue(caches.size() == 2);
            assertNotNull(caches.get("cache1"));
            assertNotNull(caches.get("cache2"));

            getBarrierForAllClients().await(10, TimeUnit.SECONDS);
        }
    }
}

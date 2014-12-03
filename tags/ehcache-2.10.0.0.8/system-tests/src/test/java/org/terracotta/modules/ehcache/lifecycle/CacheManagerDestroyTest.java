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
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import net.sf.ehcache.config.TerracottaConfiguration;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.toolkit.Toolkit;

import com.tc.test.config.model.TestConfig;
import com.terracotta.entity.ClusteredEntityManager;
import com.terracotta.entity.ehcache.ClusteredCacheManager;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * CacheDestroyTest
 */
public class CacheManagerDestroyTest extends AbstractCacheTestBase {

    private static final String CACHE_NAME = "cache1";

    public CacheManagerDestroyTest(TestConfig testConfig) {
        super("lifecycle/cache-destroy.xml", testConfig, CacheManagerCreateClient.class, ClusteredEntityClient.class);
    }

    public static class CacheManagerCreateClient extends ClientBase {
        public CacheManagerCreateClient(String[] args) {
            super(args);
        }

        @Override
        protected void runTest(Cache cache, Toolkit myToolkit) throws Throwable {

            CacheConfiguration cacheConfig = new CacheConfiguration(CACHE_NAME, 100).terracotta(new TerracottaConfiguration());
            cacheManager.addCache(new Cache(cacheConfig));

            // Signalling client to try destroy while cache manager in use
            getBarrierForAllClients().await(10, TimeUnit.SECONDS);

            // Waiting for other client to finish trying to destroy
            getBarrierForAllClients().await(1, TimeUnit.MINUTES);

            cacheManager.shutdown();

            // Signalling other client to destroy
            getBarrierForAllClients().await(10, TimeUnit.SECONDS);

            // Waiting for other client to signal destroy done
            getBarrierForAllClients().await(1, TimeUnit.MINUTES);

            setupCacheManager();
            assertTrue(cacheManager.getStatus() == Status.STATUS_ALIVE);
            assertNull(cacheManager.getCache(CACHE_NAME));
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

            try {
                clusteredEntityManager.destroyRootEntity(configuration.getName(), ClusteredCacheManager.class, clusteredCacheManager);
                fail("cache manager is in use, destroy must fail");
            } catch (IllegalStateException isex) {
                assertTrue(isex.getMessage().contains("destruction"));
            }

            // Signalling for cache manager shutdown
            getBarrierForAllClients().await(10, TimeUnit.SECONDS);

            // Wait for CM to be shutdown
            getBarrierForAllClients().await(1, TimeUnit.MINUTES);

            clusteredEntityManager.destroyRootEntity(configuration.getName(), ClusteredCacheManager.class, clusteredCacheManager);
            assertNull(clusteredEntityManager.getRootEntity(configuration.getName(), ClusteredCacheManager.class));

            // Signalling other client destroy performed
            getBarrierForAllClients().await(10, TimeUnit.SECONDS);
        }
    }
}

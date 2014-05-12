package org.terracotta.modules.ehcache.lifecycle;

import net.sf.ehcache.Cache;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.toolkit.Toolkit;

import com.tc.test.config.model.TestConfig;
import com.terracotta.entity.ClusteredEntityManager;
import com.terracotta.entity.ehcache.ClusteredCacheManager;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * CacheManagerListingTest
 */
public class CacheManagerListingTest extends AbstractCacheTestBase {
    public CacheManagerListingTest(TestConfig testConfig) {
        super("lifecycle/cache-manager-minimal.xml", testConfig, CacheManagerCreateClient.class, ClusteredEntityClient.class);
    }

    public static class CacheManagerCreateClient extends ClientBase {
        public CacheManagerCreateClient(String[] args) {
            super(args);
        }

        @Override
        protected void runTest(Cache cache, Toolkit myToolkit) throws Throwable {
            // Client setup already created CacheManager
            // Signalling for other client to check it can be listed
            getBarrierForAllClients().await(10, TimeUnit.SECONDS);

            // Waiting for other client to finish listing asserts
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
            Map<String,ClusteredCacheManager> cacheManagers = clusteredEntityManager.getRootEntities(ClusteredCacheManager.class);

            ClusteredCacheManager clusteredCacheManager = cacheManagers.get(configuration.getName());
            assertNotNull(clusteredCacheManager);

            getBarrierForAllClients().await(10, TimeUnit.SECONDS);
        }
    }
}

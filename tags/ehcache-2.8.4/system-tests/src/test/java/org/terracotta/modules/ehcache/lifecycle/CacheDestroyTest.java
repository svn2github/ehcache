package org.terracotta.modules.ehcache.lifecycle;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import net.sf.ehcache.config.MemoryUnit;
import net.sf.ehcache.config.TerracottaConfiguration;

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
 * CacheDestroyTest
 */
public class CacheDestroyTest extends AbstractCacheTestBase {

    private static final String CACHE_1_NAME = "cache1";
    private static final String CACHE_2_NAME = "cache2";
    public CacheDestroyTest(TestConfig testConfig) {
        super("lifecycle/cache-destroy.xml", testConfig, CacheCreateClient.class, ClusteredEntityClient.class);
    }

    public static class CacheCreateClient extends ClientBase {
        public CacheCreateClient(String[] args) {
            super(args);
        }

        @Override
        protected void runTest(Cache cache, Toolkit myToolkit) throws Throwable {


            CacheConfiguration cacheConfig = new CacheConfiguration(CACHE_1_NAME, 100).terracotta(new TerracottaConfiguration());
            cacheManager.addCache(new Cache(cacheConfig));

            // Create different cache config to ensure all can be deleted
            CacheConfiguration arcHeapCacheConfig = new CacheConfiguration().name(CACHE_2_NAME)
                    .maxBytesLocalHeap(100, MemoryUnit.MEGABYTES)
                    .terracotta(new TerracottaConfiguration());
            cacheManager.addCache(new Cache(arcHeapCacheConfig));

            cache = cacheManager.getCache(CACHE_1_NAME);

            String key = "key";

            cache.put(new Element(key, "value", true));

            // Cache created with some content
            // Signalling client to try destroy while cache in use
            getBarrierForAllClients().await(10, TimeUnit.SECONDS);

            // Waiting for other client to finish trying to destroy
            getBarrierForAllClients().await(1, TimeUnit.MINUTES);

            cacheManager.removeAllCaches();

            // Signalling other client to destroy
            getBarrierForAllClients().await(10, TimeUnit.SECONDS);

            // Waiting for other client to signal destroy done
            getBarrierForAllClients().await(1, TimeUnit.MINUTES);

            // Making sure adding back cache does not resurrect old data structures
            cacheManager.addCache(new Cache(cacheConfig));
            cache = cacheManager.getCache(CACHE_1_NAME);
            assertTrue(cache.get(key) == null);
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

            try {
                clusteredCacheManager.destroyCache(caches.get(CACHE_1_NAME));
                fail("cache is in use, destroy must fail");
            } catch (IllegalStateException isex) {
                assertTrue(isex.getMessage().contains("destruction"));
            }

            // Signalling for cache disposal
            getBarrierForAllClients().await(10, TimeUnit.SECONDS);

            getBarrierForAllClients().await(1, TimeUnit.MINUTES);

            destroyCache(CACHE_1_NAME, clusteredCacheManager, caches);

            destroyCache(CACHE_2_NAME, clusteredCacheManager, caches);

            // Signalling other client destroy performed
            getBarrierForAllClients().await(10, TimeUnit.SECONDS);
        }

        private void destroyCache(String cacheName, ClusteredCacheManager clusteredCacheManager, Map<String, ClusteredCache> caches) {
            clusteredCacheManager.destroyCache(caches.get(cacheName));
            assertNull(clusteredCacheManager.getCache(cacheName));
        }
    }
}

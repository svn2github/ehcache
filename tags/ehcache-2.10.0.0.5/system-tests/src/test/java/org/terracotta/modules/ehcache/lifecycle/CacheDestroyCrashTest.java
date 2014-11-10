/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.lifecycle;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class CacheDestroyCrashTest extends AbstractCacheTestBase {
    private static final String CACHE_NAME = "cache1";

    public CacheDestroyCrashTest(TestConfig testConfig) {
        super("lifecycle/cache-destroy.xml", testConfig, CacheCreateClient.class,
                CacheEntityDestroyCrashClient.class);
    }

    public static class CacheCreateClient extends ClientBase {

        public CacheCreateClient(String[] mainArgs) {
            super(mainArgs);
        }

        @Override
        protected void runTest(Cache cache, Toolkit myToolkit) throws Throwable {

            CacheConfiguration cacheConfig = new CacheConfiguration(CACHE_NAME, 100)
                    .terracotta(new TerracottaConfiguration());
            cacheManager.addCache(new Cache(cacheConfig));
            cache = cacheManager.getCache(CACHE_NAME);
            cache.put(new Element("key", "value", true));

            cacheManager.removeCache(CACHE_NAME);

            // Notify client to destroy
            getBarrierForAllClients().await(10, TimeUnit.SECONDS); // hit 1

            // Waiting for other client to signal destroy done
            getBarrierForAllClients().await(1, TimeUnit.MINUTES); // hit 2

            // Making sure adding back cache does not resurrect old data structures
            cacheManager.addCache(new Cache(cacheConfig));
            cache = cacheManager.getCache(CACHE_NAME);
            assertNull(cache.get("key"));
        }

        @Override
        protected Cache getCache() {
            return null;
        }

    }

    public static class CacheEntityDestroyCrashClient extends ClientBase {

        public CacheEntityDestroyCrashClient(String[] mainArgs) {
            super(mainArgs);
        }

        @Override
        protected void runTest(Cache cache, Toolkit myToolkit) throws Throwable {
            // Waiting for CM to be created

            waitForAllClients(); // hit 1

            Toolkit spiedToolkit = spy(getClusteringToolkit());

            ClusteredEntityManager clusteredEntityManager1 = new ClusteredEntityManager(spiedToolkit);
            Configuration configuration = ConfigurationFactory.parseConfiguration(getEhcacheXmlAsStream());
            String cmName = configuration.getName();

            Map<String, ClusteredCacheManager> cacheManagers = clusteredEntityManager1
                    .getRootEntities(ClusteredCacheManager.class);

            ClusteredCacheManager clusteredCacheManager = cacheManagers.get(cmName);
            Map<String, ClusteredCache> caches = clusteredCacheManager.getCaches();

            ClusteredCache clusteredCache = caches.get(CACHE_NAME);

            while(clusteredCacheManager.isCacheUsed(clusteredCache)) {
                TimeUnit.MILLISECONDS.sleep(200);
            }


            doThrow(new TestDestroyCrashException("Crashing destroy"))
                    .when(spiedToolkit)
                    .getCache(any(String.class), any(org.terracotta.toolkit.config.Configuration.class), any(Class.class));
            try {
                clusteredCacheManager.destroyCache(clusteredCache);
                fail("Destroy should have thrown an exception");
            } catch(TestDestroyCrashException e) {
                // Expected as we want destroy to crash
                e.printStackTrace();
            }
            reset(spiedToolkit);

            clusteredCacheManager.getCache(CACHE_NAME); // Will clean up
            // Shows inline clean up performed
            verify(spiedToolkit).getCache(any(String.class), any(org.terracotta.toolkit.config.Configuration.class), any(Class.class));

            reset(spiedToolkit);

            ClusteredEntityManager clusteredEntityManager2 = new ClusteredEntityManager(spiedToolkit);
            clusteredCacheManager = clusteredEntityManager2.getRootEntity(cmName, ClusteredCacheManager.class);

            assertNull(clusteredCacheManager.getCache(CACHE_NAME));
            verify(spiedToolkit, never()).getCache(any(String.class), any(org.terracotta.toolkit.config.Configuration.class), any(Class.class));

            getBarrierForAllClients().await(10, TimeUnit.SECONDS); // hit 2
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
    }

    public static class TestDestroyCrashException extends RuntimeException {
        public TestDestroyCrashException(String msg) {
            super(msg);
        }
    }
}

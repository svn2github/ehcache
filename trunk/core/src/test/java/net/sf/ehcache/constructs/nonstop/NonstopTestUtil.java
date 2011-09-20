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

package net.sf.ehcache.constructs.nonstop;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.CacheManagerMockHelper;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Statistics;
import net.sf.ehcache.cluster.ClusterScheme;
import net.sf.ehcache.cluster.NoopCacheCluster;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.config.SearchAttribute;
import net.sf.ehcache.config.Searchable;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.StorageStrategy;
import net.sf.ehcache.store.Store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class
 *
 * @author Abhishek Sanoujam
 */
public class NonstopTestUtil extends TestCase {

    private static final Logger LOG = LoggerFactory.getLogger(NonstopTestUtil.class);

    private CacheManager cacheManager;

    public static String getMethodSignature(Method method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getName()).append('(');

        Class[] parameterTypes = method.getParameterTypes();
        for (int j = 0; j < parameterTypes.length; j++) {
            sb.append(parameterTypes[j].getName());
            if (j < (parameterTypes.length - 1)) {
                sb.append(',');
            }
        }
        sb.append(')');
        return sb.toString();
    }

    public static Object[] getMethodArguments(Method m) throws Exception {
        Class<?>[] parameterTypes = m.getParameterTypes();
        Object[] parameters = new Object[parameterTypes.length];
        for (int i = 0; i < parameters.length; i++) {
            try {
                parameters[i] = getArgumentInstanceFor(m, parameterTypes[i]);
            } catch (InstantiationException e) {
                throw e;
            } catch (IllegalAccessException e) {
                throw e;
            }
        }
        return parameters;
    }

    private static Object getArgumentInstanceFor(Method method, Class<?> parameterClass) throws Exception {
        if (parameterClass.equals(Object.class) || parameterClass.equals(Serializable.class)) {
            // isKeyInCache doesn't touch tc layer for null keys in LocalReadsBehavior
            return "SomeKey";
        } else if (parameterClass.equals(String.class) && method.getName().equals("getSearchAttribute")) {
            // isKeyInCache doesn't touch tc layer for null keys in LocalReadsBehavior
            return "searchAttributeKey";
        } else if (parameterClass.equals(Element.class)) {
            // put calls with null does not touch terracotta layer... use non-null arg
            return new Element("someKey", "someValue");
        } else if (parameterClass.equals(Boolean.TYPE)) {
            return Boolean.FALSE;
        } else if ((method.getName().equals("getSizeBasedOnAccuracy") || method.getName().equals("setStatisticsAccuracy"))
                && parameterClass.equals(Integer.TYPE)) {
            return Statistics.STATISTICS_ACCURACY_BEST_EFFORT;
        } else if (parameterClass.equals(Collection.class)) {
          return Collections.emptySet();
        } else {
            String msg = "Unhandled parameter type for method: " + getMethodSignature(method) + ", type: " + parameterClass.getName();
            throw new Exception(msg);
        }
    }

    /**
     * @return all methods in Ehcache that potentially touches Terracotta layer
     * @throws Exception
     */
    public static List<Method> getEhcacheMethodsTouchingStore() {
        final List<Method> rv = new ArrayList<Method>();

        final MockStoreWithFlag mockStore = new MockStoreWithFlag();
        Cache cache = getMockTerracottaStore(mockStore, null);

        final Set<String> skipMethods = new HashSet<String>();
        skipMethods.add("setCacheManager");
        skipMethods.add("setDiskStorePath");
        skipMethods.add("setTransactionManagerLookup");
        skipMethods.add("initialise");
        skipMethods.add("registerCacheUsageListener");
        skipMethods.add("removeCacheUsageListener");
        skipMethods.add("setBootstrapCacheLoader");
        skipMethods.add("registerCacheExtension");
        skipMethods.add("unregisterCacheExtension");
        skipMethods.add("registerCacheLoader");
        skipMethods.add("unregisterCacheLoader");
        skipMethods.add("registerCacheWriter");
        skipMethods.add("unregisterCacheWriter");
        skipMethods.add("setDisabled");
        skipMethods.add("getWithLoader");
        skipMethods.add("getAllWithLoader");
        skipMethods.add("loadAll");
        skipMethods.add("clone");
        skipMethods.add("setName");
        skipMethods.add("dispose");
        skipMethods.add("setCacheExceptionHandler");
        skipMethods.add("addPropertyChangeListener");
        skipMethods.add("removePropertyChangeListener");
        skipMethods.add("waitUntilClusterCoherent");
        skipMethods.add("waitUntilClusterBulkLoadComplete");
        skipMethods.add("calculateOnDiskSize");
        skipMethods.add("calculateInMemorySize");
        skipMethods.add("hasAbortedSizeOf");
        // off-heap methods don't reach terracotta layer.. yet
        skipMethods.add("calculateOffHeapSize");
        skipMethods.add("getOffHeapStoreSize");
        // methods that are no-op for ClusteredStore
        skipMethods.add("bufferFull");
        skipMethods.add("getInternalContext");
        skipMethods.add("isElementOnDisk");
        skipMethods.add("acquireReadLockOnKey");
        skipMethods.add("acquireWriteLockOnKey");
        skipMethods.add("releaseReadLockOnKey");
        skipMethods.add("releaseWriteLockOnKey");
        skipMethods.add("tryReadLockOnKey");
        skipMethods.add("tryWriteLockOnKey");
        skipMethods.add("isReadLockedByCurrentThread");
        skipMethods.add("isWriteLockedByCurrentThread");
        skipMethods.add("evictExpiredElements");

        new EhcacheMethodsInvoker() {

            @Override
            protected void invokeOne(Ehcache ehcache, Method m) {
                mockStore.clearAccessFlag();
                if (skipMethods.contains(m.getName())) {
                    LOG.info(" Skipped: " + getMethodSignature(m));
                    return;
                }
                super.invokeOne(ehcache, m);
                if (mockStore.isAccessFlagMarked()) {
                    rv.add(m);
                }
            }

        }.invokeAll(cache);

        return rv;
    }

    public static Cache getMockTerracottaStore(Store mockTerracottaStore, NonstopConfiguration nonstopConfiguration) {
        CacheManager cacheManager = mock(CacheManager.class);
        CacheConfiguration cacheConfiguration = new CacheConfiguration("someName", 10000);
        TerracottaConfiguration terracottaConfiguration = new TerracottaConfiguration().clustered(true).storageStrategy(
                StorageStrategy.CLASSIC);
        terracottaConfiguration.addNonstop(nonstopConfiguration);
        cacheConfiguration.addTerracotta(terracottaConfiguration);
        Searchable searchable = new Searchable();
        SearchAttribute searchAttribute = new SearchAttribute();
        searchAttribute.setName("searchAttributeKey");
        searchable.addSearchAttribute(searchAttribute);
        cacheConfiguration.addSearchable(searchable);

        Cache cache = new Cache(cacheConfiguration);

        when(cacheManager.getConfiguration()).thenReturn(new Configuration());
        when(cacheManager.createTerracottaStore(cache)).thenReturn(mockTerracottaStore);
        when(cacheManager.getCluster((ClusterScheme) any())).thenReturn(new NoopCacheCluster());

        CacheManagerMockHelper.mockGetNonstopExecutorService(cacheManager);

        cacheManager.addCache(cache);
        cache.setCacheManager(cacheManager);
        cache.initialise();
        cache.registerCacheWriter(new MockCacheWriterFactory().createCacheWriter(cache, null));
        return cache;
    }

    public static class EhcacheMethodsInvoker {

        public void invokeAll(Ehcache ehcache) {
            Method[] methods = Ehcache.class.getMethods();
            for (Method m : methods) {
                invokeOne(ehcache, m);
            }
        }

        protected void invokeOne(Ehcache ehcache, Method m) {
            try {
                Object[] args = getMethodArguments(m);
                m.invoke(ehcache, args);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

}

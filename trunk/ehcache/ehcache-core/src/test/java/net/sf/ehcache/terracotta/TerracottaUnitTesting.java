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

package net.sf.ehcache.terracotta;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Map;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.store.TerracottaStore;
import net.sf.ehcache.terracotta.TerracottaClusteredInstanceHelper.TerracottaRuntimeType;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * @author Abhishek Sanoujam
 */
public class TerracottaUnitTesting {

    public static void setupTerracottaTesting(ClusteredInstanceFactory mockFactory) throws Exception {
        setupTerracottaTesting(mockFactory, null, TerracottaRuntimeType.EnterpriseExpress);
    }

    public static void setupTerracottaTesting(ClusteredInstanceFactory mockFactory, Runnable onNewClusteredInstanceFactory)
            throws Exception {
        setupTerracottaTesting(mockFactory, onNewClusteredInstanceFactory, TerracottaRuntimeType.EnterpriseExpress);
    }

    public static void setupTerracottaTesting(ClusteredInstanceFactory mockFactory, TerracottaRuntimeType terracottaRuntimeType) throws Exception {
        setupTerracottaTesting(mockFactory, null, terracottaRuntimeType);
    }

    public static void setupTerracottaTesting(final ClusteredInstanceFactory mockFactory, final Runnable onNewClusteredInstanceFactory,
                                              TerracottaRuntimeType terracottaRuntimeType)
            throws Exception {
        TerracottaStore terracottaStore = Mockito.mock(TerracottaStore.class);
        CacheCluster mockCacheCluster = Mockito.mock(CacheCluster.class);
        when(mockFactory.createStore((Ehcache) any())).thenReturn(terracottaStore);
        when(mockFactory.getTopology()).thenReturn(mockCacheCluster);
        CacheLockProvider mockCacheLockProvider = Mockito.mock(CacheLockProvider.class);
        when(terracottaStore.getInternalContext()).thenReturn(mockCacheLockProvider);
        when(mockFactory.createNonStopStore((TerracottaStore) any(),(NonstopConfiguration) any())).thenReturn(terracottaStore);

        TerracottaClusteredInstanceHelper mockHelper = Mockito.mock(TerracottaClusteredInstanceHelper.class);
        when(mockHelper.newClusteredInstanceFactory((Map<String, CacheConfiguration>) any(), (TerracottaClientConfiguration) any()))
                .thenAnswer(new Answer<ClusteredInstanceFactory>() {
                    public ClusteredInstanceFactory answer(InvocationOnMock invocation) throws Throwable {
                        if (onNewClusteredInstanceFactory != null) {
                            onNewClusteredInstanceFactory.run();
                        }
                        return mockFactory;
                    }
                });
        when(mockHelper.getTerracottaRuntimeTypeOrNull()).thenReturn(terracottaRuntimeType);

        Method method = TerracottaClient.class.getDeclaredMethod("setTestMode", TerracottaClusteredInstanceHelper.class);
        method.setAccessible(true);
        method.invoke(null, mockHelper);
    }
}

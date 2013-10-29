/**
 * Copyright Terracotta, Inc. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.events.ToolkitNotifier;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.ToolkitProperties;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test that asserts quickSize is not called when {@link Ehcache} sizing methods are called.
 *
 * @author Ludovic Orban
 */
public class ClusteredStoreTest {

  private ToolkitInstanceFactory toolkitInstanceFactory = mock(ToolkitInstanceFactory.class);
  private Ehcache cache = mock(Ehcache.class);
  private CacheCluster cacheCluster = mock(CacheCluster.class);
  private CacheConfiguration cacheConfiguration = new CacheConfiguration().terracotta(new TerracottaConfiguration().clustered(true));
  private CacheManager cacheManager = mock(CacheManager.class);
  private ToolkitMap configMap = mock(ToolkitMap.class);
  private ToolkitInternal toolkitInternal = mock(ToolkitInternal.class);
  private ToolkitProperties toolkitProperties = mock(ToolkitProperties.class);
  private ToolkitCacheInternal toolkitCacheInternal = mock(ToolkitCacheInternal.class);
  private org.terracotta.toolkit.config.Configuration toolkitCacheConfiguration = mock(org.terracotta.toolkit.config.Configuration.class);
  private ToolkitNotifier toolkitNotifier = mock(ToolkitNotifier.class);
  private ClusteredStore clusteredStore;


  @Before
  public void setUpClusteredStore() {
    when(cache.getCacheConfiguration()).thenReturn(cacheConfiguration);
    when(cache.getCacheManager()).thenReturn(cacheManager);
    when(cache.getName()).thenReturn("ClusteredStoreTest-cache");
    when(cacheManager.getName()).thenReturn("ClusteredStoreTest-cm");
    when(toolkitInstanceFactory.getOrCreateClusteredStoreConfigMap(eq("ClusteredStoreTest-cm"), eq("ClusteredStoreTest-cache"))).thenReturn(configMap);
    when(toolkitInstanceFactory.getToolkit()).thenReturn(toolkitInternal);
    when(toolkitInternal.getProperties()).thenReturn(toolkitProperties);
    when(toolkitProperties.getBoolean(anyString())).thenReturn(false);
    when(toolkitInstanceFactory.getOrCreateToolkitCache(eq(cache))).thenReturn(toolkitCacheInternal);
    when(toolkitCacheInternal.getConfiguration()).thenReturn(toolkitCacheConfiguration);
    when(toolkitCacheConfiguration.getInt(anyString())).thenReturn(1);
    when(toolkitInstanceFactory.getOrCreateConfigChangeNotifier(eq(cache))).thenReturn(toolkitNotifier);
    clusteredStore = new ClusteredStore(toolkitInstanceFactory, cache, cacheCluster) {
      @Override
      void setUpWanConfig() {
        // Do Nothing
      }
    };
    
  }

  @Test
  public void clusteredStore_getSize_calls_size_not_quickSize() throws Exception {
    clusteredStore.getSize();
    verify(toolkitCacheInternal, times(1)).size();
    verify(toolkitCacheInternal, times(0)).quickSize();
  }

  @Test
  public void clusteredStore_getTerracottaClusteredSize_calls_size_not_quickSize() throws Exception {
    clusteredStore.getTerracottaClusteredSize();
    verify(toolkitCacheInternal, times(1)).size();
    verify(toolkitCacheInternal, times(0)).quickSize();
  }
}

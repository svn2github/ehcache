/**
 * Copyright Terracotta, Inc. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */
package org.terracotta.modules.ehcache.store;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.store.DefaultElementValueComparator;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.events.ToolkitNotifier;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.ToolkitProperties;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;

/**
 * Test that asserts quickSize is not called when {@link Ehcache} sizing methods are called.
 *
 * @author Ludovic Orban
 */
public class ClusteredStoreTest {

  private final ToolkitInstanceFactory toolkitInstanceFactory = mock(ToolkitInstanceFactory.class);
  private final Ehcache cache = mock(Ehcache.class);
  private final CacheCluster cacheCluster = mock(CacheCluster.class);
  private final CacheConfiguration cacheConfiguration = new CacheConfiguration().terracotta(new TerracottaConfiguration().clustered(true).consistency(TerracottaConfiguration.Consistency.EVENTUAL));
  private final CacheManager cacheManager = mock(CacheManager.class);
  private final ToolkitMap configMap = mock(ToolkitMap.class);
  private final ToolkitInternal toolkitInternal = mock(ToolkitInternal.class);
  private final ToolkitProperties toolkitProperties = mock(ToolkitProperties.class);
  private final ToolkitCacheInternal toolkitCacheInternal = mock(ToolkitCacheInternal.class);
  private final org.terracotta.toolkit.config.Configuration toolkitCacheConfiguration = mock(org.terracotta.toolkit.config.Configuration.class);
  private final ToolkitNotifier toolkitNotifier = mock(ToolkitNotifier.class);
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
    when(toolkitInstanceFactory.getOrCreateToolkitCache(eq(cache), eq(false))).thenReturn(toolkitCacheInternal);
    when(toolkitCacheInternal.getConfiguration()).thenReturn(toolkitCacheConfiguration);
    when(toolkitCacheConfiguration.getInt(anyString())).thenReturn(1);
    when(toolkitInstanceFactory.getOrCreateConfigChangeNotifier(eq(cache))).thenReturn(toolkitNotifier);
    clusteredStore = new ClusteredStore(toolkitInstanceFactory, cache, cacheCluster) {
      @Override
      void setUpWanConfig() {
        // Do Nothing
      }

      @Override
      boolean isWANEnabled() {
        return false;
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

  @Test(expected = UnsupportedOperationException.class)
  public void clusteredStore_putIfAbsent_throw_in_eventual_consistency() {
    clusteredStore.putIfAbsent(new Element("key", "value"));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void clusteredStore_replace_1_arg_throw_in_eventual_consistency() {
    clusteredStore.replace(new Element("key", "value"));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void clusteredStore_replace_2_args_throw_in_eventual_consistency() {
    clusteredStore.replace(new Element("key", "value"), new Element("key", "other"), new DefaultElementValueComparator(cacheConfiguration));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void clusteredStore_removeElement_throw_in_eventual_consistency() {
    clusteredStore.removeElement(new Element("key", "value"), new DefaultElementValueComparator(cacheConfiguration));
  }
}

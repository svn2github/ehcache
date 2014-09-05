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
import net.sf.ehcache.CacheStoreHelper;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.NonEternalElementData;
import net.sf.ehcache.Status;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.cluster.ClusterNode;
import net.sf.ehcache.cluster.ClusterTopologyListener;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.CacheEventListenerAdapter;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.store.DefaultElementValueComparator;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.toolkit.cache.ToolkitCacheListener;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.events.ToolkitNotifier;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.ToolkitProperties;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.internal.cache.ToolkitValueComparator;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test that asserts quickSize is not called when {@link Ehcache} sizing methods are called.
 *
 * @author Ludovic Orban
 */
public class ClusteredStoreTest {
  
  private Ehcache cache = mock(Ehcache.class);
  private CacheConfiguration cacheConfiguration = new CacheConfiguration().terracotta(new TerracottaConfiguration().clustered(true).consistency(TerracottaConfiguration.Consistency.EVENTUAL));
  private Configuration configuration = new Configuration().name("ClusteredStoreTest-cm").terracotta(new TerracottaClientConfiguration());
  private ToolkitCacheInternal toolkitCacheInternal = mock(ToolkitCacheInternal.class);
  private ClusteredStore clusteredStore;
  private ToolkitInstanceFactory toolkitInstanceFactory = mock(ToolkitInstanceFactory.class);
  private CacheCluster cacheCluster = mockCacheCluster("abc");
  private CacheManager cacheManager = when(mock(CacheManager.class).getConfiguration()).thenReturn(configuration).getMock();
  private ToolkitMap configMap = mock(ToolkitMap.class);
  private ToolkitInternal toolkitInternal = mock(ToolkitInternal.class);
  private ToolkitProperties toolkitProperties = mock(ToolkitProperties.class);
  private org.terracotta.toolkit.config.Configuration toolkitCacheConfiguration = mock(org.terracotta.toolkit.config.Configuration.class);
  private ToolkitNotifier toolkitNotifier = mock(ToolkitNotifier.class);
  private CacheStoreHelper cacheStoreHelper = mock(CacheStoreHelper.class);
  private ToolkitLock toolkitLock = mock(ToolkitLock.class);

  @Before
  public void setUpClusteredStore() {
    when(cache.getCacheConfiguration()).thenReturn(cacheConfiguration);
    when(cache.getCacheManager()).thenReturn(cacheManager);
    when(cache.getName()).thenReturn("ClusteredStoreTest-cache");
    when(cache.getStatus()).thenReturn(Status.STATUS_ALIVE);
    when(cache.getCacheEventNotificationService()).thenReturn(new RegisteredEventListeners(cache, cacheStoreHelper));
    when(cacheManager.getName()).thenReturn("ClusteredStoreTest-cm");
    when(toolkitInstanceFactory.getOrCreateClusteredStoreConfigMap(eq("ClusteredStoreTest-cm"), eq("ClusteredStoreTest-cache"))).thenReturn(configMap);
    when(toolkitInstanceFactory.getToolkit()).thenReturn(toolkitInternal);
    when(toolkitInstanceFactory.getLockForCache(any(Ehcache.class), anyString())).thenReturn(toolkitLock);
    when(toolkitInternal.getProperties()).thenReturn(toolkitProperties);
    when(toolkitProperties.getBoolean(anyString())).thenReturn(false);
    when(toolkitInstanceFactory.getOrCreateToolkitCache(cache)).thenReturn(toolkitCacheInternal);
    when(toolkitCacheInternal.getConfiguration()).thenReturn(toolkitCacheConfiguration);
    when(toolkitCacheConfiguration.getInt(anyString())).thenReturn(1);
    when(toolkitInstanceFactory.getOrCreateConfigChangeNotifier(eq(cache))).thenReturn(toolkitNotifier);
    clusteredStore = new ClusteredStore(toolkitInstanceFactory, cache, cacheCluster) {
      @Override
      void setUpWanConfig() {
        // Do Nothing
      }
    };
    when(cacheStoreHelper.getStore()).thenReturn(clusteredStore);
  }

  private static CacheCluster mockCacheCluster(String thisNode) {
    CacheCluster cacheCluster = mock(CacheCluster.class);
    ClusterNode node = when(mock(ClusterNode.class).getId()).thenReturn(thisNode).getMock();
    when(cacheCluster.getCurrentNode()).thenReturn(node);
    return cacheCluster;
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

  @Test
  public void clusteredStore_putIfAbsent_enabled_in_eventual_consistency() {
    clusteredStore.putIfAbsent(new Element("key", "value"));
    verify(toolkitCacheInternal).putIfAbsent(eq("key"), any(NonEternalElementData.class));
  }

  @Test
  public void clusteredStore_replace_1_arg_enabled_in_eventual_consistency() {
    clusteredStore.replace(new Element("key", "value"));
    verify(toolkitCacheInternal).replace(any(), any());
  }

  @Test
  public void clusteredStore_replace_2_args_enabled_in_eventual_consistency() {
    clusteredStore.replace(new Element("key", "value"), new Element("key", "other"), new DefaultElementValueComparator(cacheConfiguration));
    verify(toolkitCacheInternal).replace(any(), any(), any(), any(ToolkitValueComparator.class));
  }

  @Test
  public void clusteredStore_removeElement_enabled_in_eventual_consistency() {
    clusteredStore.removeElement(new Element("key", "value"), new DefaultElementValueComparator(cacheConfiguration));
    verify(toolkitCacheInternal).remove(any(), any(), any(ToolkitValueComparator.class));
  }
  
  @Test
  public void testDispose() throws Exception {
    clusteredStore.dispose();
    verify(toolkitCacheInternal).disposeLocally();
    verify(cacheCluster).removeTopologyListener(any(ClusterTopologyListener.class));
    verify(toolkitCacheInternal).removeListener(any(ToolkitCacheListener.class));
  }

  @Test
  public void testRegisterToolkitCacheEventListener() throws Exception {
    verify(toolkitCacheInternal, never()).addListener(any(ToolkitCacheListener.class));
    cache.getCacheEventNotificationService().registerListener(new CacheEventListenerAdapter());
    cache.getCacheEventNotificationService().registerListener(new CacheEventListenerAdapter());
    verify(toolkitCacheInternal, times(1)).addListener(any(ToolkitCacheListener.class));
  }

  @Test
  public void testUnregisterToolkitCacheEventListener() throws Exception {
    String thisNodeId = cacheCluster.getCurrentNode().getId();
    when(configMap.get(ClusteredStore.LEADER_NODE_ID)).thenReturn(thisNodeId); // make this node the leader
    verify(toolkitCacheInternal, never()).addListener(any(ToolkitCacheListener.class));
    CacheEventListener listener = new CacheEventListenerAdapter();
    cache.getCacheEventNotificationService().registerListener(listener);
    cache.getCacheEventNotificationService().registerListener(listener);
    cache.getCacheEventNotificationService().unregisterListener(listener);
    cache.getCacheEventNotificationService().unregisterListener(listener);
    verify(toolkitCacheInternal, times(1)).removeListener(any(ToolkitCacheListener.class));
    verify(configMap).remove(ClusteredStore.LEADER_NODE_ID); // make sure we drop leader status
  }
}

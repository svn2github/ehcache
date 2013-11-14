/**
 * Copyright Terracotta, Inc. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.cluster.ClusterTopologyListener;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.CacheEventListenerAdapter;
import net.sf.ehcache.store.DefaultElementValueComparator;
import org.junit.Test;
import org.terracotta.toolkit.cache.ToolkitCacheListener;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test that asserts quickSize is not called when {@link Ehcache} sizing methods are called.
 *
 * @author Ludovic Orban
 */
public class ClusteredStoreTest extends AbstractClusteredStoreTest {
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

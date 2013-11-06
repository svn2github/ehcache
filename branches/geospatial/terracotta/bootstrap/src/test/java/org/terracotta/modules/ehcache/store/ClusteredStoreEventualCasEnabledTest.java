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
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.NonEternalElementData;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.store.DefaultElementValueComparator;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.internal.AssumptionViolatedException;
import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.events.ToolkitNotifier;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.ToolkitProperties;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test that asserts quickSize is not called when {@link net.sf.ehcache.Ehcache} sizing methods are called.
 *
 * @author Ludovic Orban
 */
public class ClusteredStoreEventualCasEnabledTest {

  private static boolean previousValue;
  private static Field eventual_cas_enabled;

  private ToolkitInstanceFactory toolkitInstanceFactory = mock(ToolkitInstanceFactory.class);
  private Ehcache cache = mock(Ehcache.class);
  private CacheCluster cacheCluster = mock(CacheCluster.class);
  private CacheConfiguration cacheConfiguration = new CacheConfiguration().terracotta(new TerracottaConfiguration().clustered(true)
      .consistency(TerracottaConfiguration.Consistency.EVENTUAL));
  private CacheManager cacheManager = mock(CacheManager.class);
  private ToolkitMap configMap = mock(ToolkitMap.class);
  private ToolkitInternal toolkitInternal = mock(ToolkitInternal.class);
  private ToolkitProperties toolkitProperties = mock(ToolkitProperties.class);
  private ToolkitCacheInternal toolkitCacheInternal = mock(ToolkitCacheInternal.class);
  private org.terracotta.toolkit.config.Configuration toolkitCacheConfiguration = mock(org.terracotta.toolkit.config.Configuration.class);
  private ToolkitNotifier toolkitNotifier = mock(ToolkitNotifier.class);
  private ClusteredStore clusteredStore;
  private ToolkitReadWriteLock tkRWLock = mock(ToolkitReadWriteLock.class);

  @BeforeClass
  public static void setUpClass() {
    try {
      eventual_cas_enabled = ClusteredStore.class.getDeclaredField("EVENTUAL_CAS_ENABLED");
      eventual_cas_enabled.setAccessible(true);

      Field modifiers = Field.class.getDeclaredField("modifiers");
      modifiers.setAccessible(true);
      modifiers.setInt(eventual_cas_enabled, eventual_cas_enabled.getModifiers() & ~Modifier.FINAL);

      previousValue = (Boolean) eventual_cas_enabled.get(null);
      eventual_cas_enabled.set(null, true);
    } catch (Exception e) {
      throw new AssumptionViolatedException("Unable to modify field", e);
    }
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    try {
      eventual_cas_enabled.set(null, previousValue);
    } catch (Exception e) {
      // Ignore
    }
  }

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
    when(toolkitCacheInternal.createLockForKey(any())).thenReturn(tkRWLock);
    when(tkRWLock.writeLock()).thenReturn(mock(ToolkitLock.class));
    clusteredStore = new ClusteredStore(toolkitInstanceFactory, cache, cacheCluster) {
      @Override
      void setUpWanConfig() {
        // Do Nothing
      }
    };
  }

  @Test
  public void clusteredStore_putIfAbsent_enabled_in_eventual_consistency() {
    clusteredStore.putIfAbsent(new Element("key", "value"));
    verify(toolkitCacheInternal).putIfAbsent(eq("key"), any(NonEternalElementData.class));
  }

  @Test
  public void clusteredStore_replace_1_arg_enabled_in_eventual_consistency() {
    clusteredStore.replace(new Element("key", "value"));
    verify(toolkitCacheInternal).getQuiet("key");
  }

  @Test
  public void clusteredStore_replace_2_args_enabled_in_eventual_consistency() {
    clusteredStore.replace(new Element("key", "value"), new Element("key", "other"), new DefaultElementValueComparator(cacheConfiguration));
    verify(toolkitCacheInternal).getQuiet("key");
  }

  @Test
  public void clusteredStore_removeElement_enabled_in_eventual_consistency() {
    clusteredStore.removeElement(new Element("key", "value"), new DefaultElementValueComparator(cacheConfiguration));
    verify(toolkitCacheInternal).getQuiet("key");
  }
}

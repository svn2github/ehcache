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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import org.terracotta.toolkit.internal.cache.ToolkitValueComparator;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Test that asserts quickSize is not called when {@link net.sf.ehcache.Ehcache} sizing methods are called.
 *
 * @author Ludovic Orban
 */
public class ClusteredStoreEventualCasEnabledTest extends AbstractClusteredStoreTest {

  private static boolean previousValue;
  private static Field eventual_cas_enabled;

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
}

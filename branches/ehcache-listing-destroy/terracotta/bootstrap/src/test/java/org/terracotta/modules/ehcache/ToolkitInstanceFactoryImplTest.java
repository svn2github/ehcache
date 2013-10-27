/**
 * Copyright Terracotta, Inc. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */

package org.terracotta.modules.ehcache;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.terracotta.modules.ehcache.wan.WANUtil;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitFeatureType;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.feature.NonStopFeature;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.nonstop.NonStopConfigurationRegistry;
import org.terracotta.toolkit.store.ToolkitConfigFields;

import com.terracotta.entity.ClusteredEntityManager;
import com.terracotta.entity.ehcache.ClusteredCache;
import com.terracotta.entity.ehcache.ClusteredCacheManager;
import com.terracotta.entity.ehcache.ClusteredCacheManagerConfiguration;

import java.io.Serializable;

import junit.framework.Assert;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * ToolkitInstanceFactoryImplTest
 */
public class ToolkitInstanceFactoryImplTest {

  private static final String                        CACHE_MANAGER_NAME = "CACHE_MANAGER_NAME";
  private static final String                        CACHE_NAME         = "CACHE_NAME";

  @Mock private Toolkit                                    toolkit;
  @Mock private WANUtil                                    wanUtil;
  @Mock private Ehcache                                    ehcache;
  @Mock private CacheManager                               cacheManager;

  private ToolkitInstanceFactoryImpl                 factory;
  private ToolkitCacheInternal<String, Serializable> resultantCache;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    toolkit = mock(Toolkit.class);
    when(toolkit.getMap(anyString(), any(Class.class), any(Class.class))).thenReturn(mock(ToolkitMap.class));
    ToolkitReadWriteLock rwLock = mock(ToolkitReadWriteLock.class);
    ToolkitLock lock = mock(ToolkitLock.class);
    when(lock.tryLock()).thenReturn(true);
    when(rwLock.readLock()).thenReturn(lock);
    when(rwLock.writeLock()).thenReturn(lock);
    when(toolkit.getReadWriteLock(any(String.class))).thenReturn(rwLock);
    makeToolkitReturnNonStopConfigurationRegistry();

    wanUtil = mock(WANUtil.class);
    ehcache = mock(Ehcache.class);
    when(cacheManager.isNamed()).thenReturn(true);
    when(cacheManager.getName()).thenReturn(CACHE_MANAGER_NAME);
    when(ehcache.getCacheManager()).thenReturn(cacheManager);
    when(ehcache.getName()).thenReturn(CACHE_NAME);
    CacheConfiguration configuration = new CacheConfiguration().terracotta(new TerracottaConfiguration());
    when(ehcache.getCacheConfiguration()).thenReturn(configuration);

    ClusteredEntityManager clusteredEntityManager = mock(ClusteredEntityManager.class);
    ClusteredCacheManager clusteredCacheManager = new ClusteredCacheManager("aName", new ClusteredCacheManagerConfiguration(defaultCMConfig), toolkit);
    when(clusteredEntityManager.getRootEntity(any(String.class), any(Class.class))).thenReturn(clusteredCacheManager);
    factory = new ToolkitInstanceFactoryImpl(toolkit, clusteredEntityManager);
    factory.setWANUtil(wanUtil);
    factory.linkClusteredCacheManager(CACHE_MANAGER_NAME, null);
  }

  @Test
  public void testGetOrCreateToolkitCacheForWanEnabled() throws Exception {
    whenCacheIsWanEnabled().callGetOrCreateToolkitCache().assertInstanceOfWanAwareToolkitCache(true);
  }

  @Test
  public void testGetOrCreateToolkitCacheForWanDisabled() throws Exception {
    whenCacheIsWanDisabled().callGetOrCreateToolkitCache().assertInstanceOfWanAwareToolkitCache(false);
  }

  @Test
  public void testMaxEntriesInCacheToMaxTotalCountTransformation() {
    CacheConfiguration configuration = new CacheConfiguration().terracotta(new TerracottaConfiguration()).maxEntriesInCache(10);
    forEhcacheConfig(configuration).callGetOrCreateToolkitCache().validateMaxTotalCountForToolkitCacheIs(10);
  }

  private void validateMaxTotalCountForToolkitCacheIs(int maxTotalCount) {
    ArgumentCaptor<Configuration> captor = ArgumentCaptor.forClass(Configuration.class);
    verify(toolkit).getCache(anyString(), captor.capture(), eq(Serializable.class));
    assertThat(captor.getValue().getInt(ToolkitConfigFields.MAX_TOTAL_COUNT_FIELD_NAME), is(10));
  }

  private ToolkitInstanceFactoryImplTest forEhcacheConfig(CacheConfiguration configuration) {
    when(ehcache.getCacheConfiguration()).thenReturn(configuration);
    return this;
  }


  private void makeToolkitReturnNonStopConfigurationRegistry() {
    NonStopFeature feature = mock(NonStopFeature.class);
    when(toolkit.getFeature(any(ToolkitFeatureType.class))).thenReturn(feature);
    when(feature.getNonStopConfigurationRegistry()).thenReturn(mock(NonStopConfigurationRegistry.class));
  }

  /**
   * This test case was added while fixing DEV-9223. From now on, we assume that the default value for maxTotalCount in
   * Toolkit (-1), and the default value for maxEntriesInCache in EhCache (0) will be aligned. That is, they both will
   * mean the same thing. Currently they mean no-limit cache. If someone changes the default value of one of those, then
   * this test case will fail and we would need to handle it.
   */
  @Test
  public void testToolkitAndEhCacheDefaultsAreAligned() {
    Assert.assertEquals(0, CacheConfiguration.DEFAULT_MAX_ENTRIES_IN_CACHE);
    Assert.assertEquals(-1, ToolkitConfigFields.DEFAULT_MAX_TOTAL_COUNT);
  }


  private void assertInstanceOfWanAwareToolkitCache(boolean expectedResult) {
    Assert.assertEquals(expectedResult, (resultantCache instanceof WanAwareToolkitCache));
  }

  private ToolkitInstanceFactoryImplTest callGetOrCreateToolkitCache() {
    resultantCache = factory.getOrCreateToolkitCache(ehcache);
    return this;
  }

  private ToolkitInstanceFactoryImplTest whenCacheIsWanEnabled() {
    when(wanUtil.isWanEnabledCache(CACHE_MANAGER_NAME, CACHE_NAME)).thenReturn(true);
    return this;
  }

  private ToolkitInstanceFactoryImplTest whenCacheIsWanDisabled() {
    when(wanUtil.isWanEnabledCache(CACHE_MANAGER_NAME, CACHE_NAME)).thenReturn(false);
    return this;
  }
  
  @Test
  public void testAddCacheEntityInfo() {
    factory.addCacheEntityInfo(CACHE_NAME, new CacheConfiguration(), "testTKCacheName");
  }

  @Test
  public void testRetrievingExistingClusteredCacheManagerEntity() {
    String name = "existing";
    net.sf.ehcache.config.Configuration configuration = new net.sf.ehcache.config.Configuration();

    Toolkit toolkit = mock(Toolkit.class);
    when(toolkit.getMap(anyString(), eq(String.class), eq(ClusteredCache.class))).thenReturn(mock(ToolkitMap.class));

    ClusteredEntityManager clusteredEntityManager = mock(ClusteredEntityManager.class);
    when(clusteredEntityManager.getRootEntity(name, ClusteredCacheManager.class))
        .thenReturn(new ClusteredCacheManager(name, new ClusteredCacheManagerConfiguration("test"), toolkit));

    ToolkitInstanceFactoryImpl toolkitInstanceFactory = new ToolkitInstanceFactoryImpl(mock(Toolkit.class),
                                                                                       clusteredEntityManager);

    toolkitInstanceFactory.linkClusteredCacheManager(name, configuration);
    verify(clusteredEntityManager).getRootEntity(name, ClusteredCacheManager.class);
    verifyNoMoreInteractions(clusteredEntityManager);
  }

  @Test
  public void testCreatingNewClusteredCacheManagerEntity() {
    String name = "newCM";

    net.sf.ehcache.config.Configuration configuration = new net.sf.ehcache.config.Configuration();

    ClusteredEntityManager clusteredEntityManager = mock(ClusteredEntityManager.class);

    ToolkitInstanceFactoryImpl toolkitInstanceFactory = new ToolkitInstanceFactoryImpl(mock(Toolkit.class),
                                                                                       clusteredEntityManager);

    toolkitInstanceFactory.linkClusteredCacheManager(name, configuration);
    verify(clusteredEntityManager).getRootEntity(name, ClusteredCacheManager.class);
    verify(clusteredEntityManager).addRootEntity(eq(name), any(ClusteredCacheManager.class));
    verifyNoMoreInteractions(clusteredEntityManager);
  }

  @Test
  public void testTryingToCreateNewClusteredCacheManagerEntityButLooseRace() {
    String name = "newCM";

    net.sf.ehcache.config.Configuration configuration = new net.sf.ehcache.config.Configuration();

    ClusteredEntityManager clusteredEntityManager = mock(ClusteredEntityManager.class);
    doThrow(new IllegalStateException("exists")).when(clusteredEntityManager)
        .addRootEntity(eq(name), any(ClusteredCacheManager.class));

    ToolkitInstanceFactoryImpl toolkitInstanceFactory = new ToolkitInstanceFactoryImpl(mock(Toolkit.class),
                                                                                       clusteredEntityManager);

    toolkitInstanceFactory.linkClusteredCacheManager(name, configuration);
    InOrder inOrder = inOrder(clusteredEntityManager);
    inOrder.verify(clusteredEntityManager).getRootEntity(name, ClusteredCacheManager.class);
    inOrder.verify(clusteredEntityManager).addRootEntity(eq(name), any(ClusteredCacheManager.class));
    inOrder.verify(clusteredEntityManager).getRootEntity(name, ClusteredCacheManager.class);
    verifyNoMoreInteractions(clusteredEntityManager);
  }

  @Test
  public void testConfigurationSavedContainsNoCachesAnymore() {
    String name = "newCM";

    net.sf.ehcache.config.Configuration configuration = new net.sf.ehcache.config.Configuration();
    configuration.addCache(new CacheConfiguration("test", 1));

    ClusteredEntityManager clusteredEntityManager = mock(ClusteredEntityManager.class);

    ToolkitInstanceFactoryImpl toolkitInstanceFactory = new ToolkitInstanceFactoryImpl(mock(Toolkit.class),
                                                                                       clusteredEntityManager);

    toolkitInstanceFactory.linkClusteredCacheManager(name, configuration);
    ArgumentCaptor<ClusteredCacheManager> captor = ArgumentCaptor.forClass(ClusteredCacheManager.class);
    verify(clusteredEntityManager).addRootEntity(eq(name), captor.capture());

    assertThat(captor.getValue().getConfiguration().getConfigurationAsText(), not(containsString("<cache")));
  }

  @Test
  public void testConfigurationSavedContainsCacheManagerName() {
    String name = "newCM";

    net.sf.ehcache.config.Configuration configuration = new net.sf.ehcache.config.Configuration();
    configuration.addCache(new CacheConfiguration("test", 1));

    ClusteredEntityManager clusteredEntityManager = mock(ClusteredEntityManager.class);

    ToolkitInstanceFactoryImpl toolkitInstanceFactory = new ToolkitInstanceFactoryImpl(mock(Toolkit.class),
                                                                                       clusteredEntityManager);

    toolkitInstanceFactory.linkClusteredCacheManager(name, configuration);
    ArgumentCaptor<ClusteredCacheManager> captor = ArgumentCaptor.forClass(ClusteredCacheManager.class);
    verify(clusteredEntityManager).addRootEntity(eq(name), captor.capture());

    assertThat(captor.getValue().getConfiguration().getConfigurationAsText(), containsString("name=\"" + name));
  }

  private final String defaultCMConfig = "<ehcache name=\"test-lifecycle\">" +
                                         "  <terracottaConfig url=\"localhost:PORT\"/>" +
                                         "  <defaultCache" +
                                         "      maxElementsInMemory=\"10\"" +
                                         "      eternal=\"true\"/>" +
                                         "</ehcache>";

}

/**
 * Copyright Terracotta, Inc. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */

package org.terracotta.modules.ehcache;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.terracotta.modules.ehcache.wan.WANUtil;
import org.terracotta.modules.ehcache.wan.Watchdog;
import org.terracotta.test.categories.CheckShorts;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitFeatureType;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.feature.NonStopFeature;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.ToolkitLogger;
import org.terracotta.toolkit.internal.cache.BufferingToolkitCache;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.nonstop.NonStopConfigurationRegistry;
import org.terracotta.toolkit.store.ToolkitConfigFields;

import com.terracotta.entity.ClusteredEntityManager;
import com.terracotta.entity.EntityLockHandler;
import com.terracotta.entity.ehcache.ClusteredCache;
import com.terracotta.entity.ehcache.ClusteredCacheManager;
import com.terracotta.entity.ehcache.ClusteredCacheManagerConfiguration;
import com.terracotta.entity.ehcache.EhcacheEntitiesNaming;
import com.terracotta.entity.ehcache.ToolkitBackedClusteredCacheManager;

import java.io.Serializable;

import junit.framework.Assert;

/**
 * ToolkitInstanceFactoryImplTest
 */
@Category(CheckShorts.class)
public class ToolkitInstanceFactoryImplTest {

  private static final String                        CACHE_MANAGER_NAME = "CACHE_MANAGER_NAME";
  private static final String                        CACHE_NAME         = "CACHE_NAME";

  @Mock private Toolkit                                    toolkit;
  @Mock private WANUtil                                    wanUtil;
  @Mock private Ehcache                                    ehcache;
  @Mock private CacheManager                               cacheManager;

  private ToolkitInstanceFactoryImpl                       factory;
  private ToolkitCacheInternal<String, Serializable>       resultantCache;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    toolkit = getMockToolkit();
    final ToolkitMap toolkitMap = mockMap();
    when(toolkit.getMap(anyString(), any(Class.class), any(Class.class))).thenReturn(toolkitMap);
    when(toolkit.getCache(anyString(), any(Configuration.class), any(Class.class)))
        .thenReturn(mock(BufferingToolkitCache.class));
    ToolkitReadWriteLock rwLock = mockReadWriteLock();
    when(toolkit.getReadWriteLock(any(String.class))).thenReturn(rwLock);
    makeToolkitReturnNonStopConfigurationRegistry();

    wanUtil = mock(WANUtil.class);
    final Watchdog wanWatchdog = mock(Watchdog.class);
    ehcache = mock(Ehcache.class);
    when(cacheManager.isNamed()).thenReturn(true);
    when(cacheManager.getName()).thenReturn(CACHE_MANAGER_NAME);
    when(ehcache.getCacheManager()).thenReturn(cacheManager);
    when(ehcache.getName()).thenReturn(CACHE_NAME);
    CacheConfiguration configuration = new CacheConfiguration().terracotta(new TerracottaConfiguration());
    when(ehcache.getCacheConfiguration()).thenReturn(configuration);

    ClusteredEntityManager clusteredEntityManager = mock(ClusteredEntityManager.class);
    ToolkitBackedClusteredCacheManager clusteredCacheManager = new ToolkitBackedClusteredCacheManager("aName", new ClusteredCacheManagerConfiguration(defaultCMConfig));
    clusteredCacheManager.setToolkit(toolkit);
    clusteredCacheManager.setEntityLockHandler(mock(EntityLockHandler.class));
    when(clusteredEntityManager.getRootEntity(any(String.class), any(Class.class))).thenReturn(clusteredCacheManager);
    when(clusteredEntityManager.getEntityLock(anyString())).thenReturn(mock(ToolkitReadWriteLock.class));
    factory = new ToolkitInstanceFactoryImpl(toolkit, clusteredEntityManager, wanUtil, wanWatchdog);
    factory.linkClusteredCacheManager(CACHE_MANAGER_NAME, null);
  }

  private ToolkitMap mockMap() {
    ToolkitMap map = mock(ToolkitMap.class);
    ToolkitReadWriteLock readWriteLock = mockReadWriteLock();
    when(map.getReadWriteLock()).thenReturn(readWriteLock);
    return map;
  }

  private ToolkitReadWriteLock mockReadWriteLock() {
    ToolkitReadWriteLock readWriteLock = mock(ToolkitReadWriteLock.class);
    ToolkitLock lock = mockLock();
    when(readWriteLock.readLock()).thenReturn(lock);
    when(readWriteLock.writeLock()).thenReturn(lock);
    return readWriteLock;
  }

  private ToolkitLock mockLock() {
    return when(mock(ToolkitLock.class).tryLock()).thenReturn(true).getMock();
  }

  @Test
  public void testGetOrCreateToolkitCacheForWanEnabled() throws Exception {
    whenCacheIsWanEnabled().callGetOrCreateToolkitCache().assertInstanceOfWanAwareToolkitCache(true);
  }

  @Test
  public void testGetOrCreateToolkitCacheForWanDisabled() throws Exception {
    whenCacheIsWanDisabled().callGetOrCreateToolkitCache().assertInstanceOfWanAwareToolkitCache(false);
  }

  @Test(expected = CacheException.class)
  public void testWanAwareToolkitCacheDoesNotSupportDynamicConfigChange() throws Exception {
    whenCacheIsWanEnabled().callGetOrCreateToolkitCache().assertInstanceOfWanAwareToolkitCache(true).updateTimeToLive();
  }

  @Test
  public void testMaxEntriesInCacheToMaxTotalCountTransformation() {
    CacheConfiguration configuration = new CacheConfiguration().terracotta(new TerracottaConfiguration()).maxEntriesInCache(10);
    forEhcacheConfig(configuration).callGetOrCreateToolkitCache().validateMaxTotalCountForToolkitCacheIs(10);
  }

  private void updateTimeToLive() {
    ehcache.getCacheConfiguration().setTimeToLiveSeconds(10);
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


  private ToolkitInstanceFactoryImplTest assertInstanceOfWanAwareToolkitCache(boolean expectedResult) {
    Assert.assertEquals(expectedResult, (resultantCache instanceof WanAwareToolkitCache));
    return this;
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

    Toolkit toolkit = getMockToolkit();
    when(toolkit.getMap(anyString(), eq(String.class), eq(ClusteredCache.class))).thenReturn(mock(ToolkitMap.class));

    ClusteredEntityManager clusteredEntityManager = mock(ClusteredEntityManager.class);
    ToolkitBackedClusteredCacheManager cacheManagerEntity = new ToolkitBackedClusteredCacheManager(name, new ClusteredCacheManagerConfiguration("test"));
    cacheManagerEntity.setEntityLockHandler(mock(EntityLockHandler.class));
    when(clusteredEntityManager.getRootEntity(name, ClusteredCacheManager.class))
        .thenReturn(cacheManagerEntity);
    when(clusteredEntityManager.getEntityLock(anyString())).thenReturn(mock(ToolkitReadWriteLock.class));
    ToolkitInstanceFactoryImpl toolkitInstanceFactory = new ToolkitInstanceFactoryImpl(getMockToolkit(),
                                                                                       clusteredEntityManager);

    toolkitInstanceFactory.linkClusteredCacheManager(name, configuration);
    verify(clusteredEntityManager).getRootEntity(name, ClusteredCacheManager.class);
    verify(clusteredEntityManager).getEntityLock(anyString());
    verifyNoMoreInteractions(clusteredEntityManager);
  }

  @Test
  public void testCreatingNewClusteredCacheManagerEntity() {
    String name = "newCM";

    net.sf.ehcache.config.Configuration configuration = new net.sf.ehcache.config.Configuration();

    ClusteredEntityManager clusteredEntityManager = mock(ClusteredEntityManager.class);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        Object[] arguments = invocationOnMock.getArguments();
        ((ToolkitBackedClusteredCacheManager)arguments[2]).setEntityLockHandler(mock(EntityLockHandler.class));
        return null;
      }
    }).when(clusteredEntityManager).addRootEntityIfAbsent(eq(name), eq(ClusteredCacheManager.class), any(ClusteredCacheManager.class));
    ToolkitReadWriteLock rwLock = mock(ToolkitReadWriteLock.class);
    ToolkitLock writeLock = mock(ToolkitLock.class);
    when(writeLock.tryLock()).thenReturn(true);
    when(rwLock.writeLock()).thenReturn(writeLock);
    when(clusteredEntityManager.getEntityLock(any(String.class))).thenReturn(rwLock);

    ToolkitInstanceFactoryImpl toolkitInstanceFactory = new ToolkitInstanceFactoryImpl(getMockToolkit(),
                                                                                       clusteredEntityManager);

    toolkitInstanceFactory.linkClusteredCacheManager(name, configuration);
    verify(clusteredEntityManager).getRootEntity(name, ClusteredCacheManager.class);
    verify(clusteredEntityManager).getEntityLock(EhcacheEntitiesNaming.getCacheManagerLockNameFor(name));
    verify(clusteredEntityManager).addRootEntityIfAbsent(eq(name), any(Class.class), any(ClusteredCacheManager.class));
    verifyNoMoreInteractions(clusteredEntityManager);
  }

  @Test
  public void testTryingToCreateNewClusteredCacheManagerEntityButLooseRace() {
    String name = "newCM";

    net.sf.ehcache.config.Configuration configuration = new net.sf.ehcache.config.Configuration();

    ClusteredEntityManager clusteredEntityManager = mock(ClusteredEntityManager.class);
    when(clusteredEntityManager.addRootEntityIfAbsent(eq(name), any(Class.class), any(ClusteredCacheManager.class))).thenReturn(mock(ClusteredCacheManager.class));
    when(clusteredEntityManager.getRootEntity(name, ClusteredCacheManager.class)).thenReturn(null, mock(ClusteredCacheManager.class));
    ToolkitReadWriteLock rwLock = mock(ToolkitReadWriteLock.class);
    ToolkitLock writeLock = mock(ToolkitLock.class);
    when(writeLock.tryLock()).thenReturn(true);
    when(rwLock.writeLock()).thenReturn(writeLock);
    when(clusteredEntityManager.getEntityLock(any(String.class))).thenReturn(rwLock);

    ToolkitInstanceFactoryImpl toolkitInstanceFactory = new ToolkitInstanceFactoryImpl(getMockToolkit(),
                                                                                       clusteredEntityManager);

    toolkitInstanceFactory.linkClusteredCacheManager(name, configuration);
    InOrder inOrder = inOrder(clusteredEntityManager);
    inOrder.verify(clusteredEntityManager).getRootEntity(name, ClusteredCacheManager.class);
    inOrder.verify(clusteredEntityManager).getEntityLock(EhcacheEntitiesNaming.getCacheManagerLockNameFor(name));
    inOrder.verify(clusteredEntityManager).addRootEntityIfAbsent(eq(name), any(Class.class), any(ClusteredCacheManager.class));
    verifyNoMoreInteractions(clusteredEntityManager);
  }

  @Test
  public void testConfigurationSavedContainsNoCachesAnymore() {
    String name = "newCM";

    net.sf.ehcache.config.Configuration configuration = new net.sf.ehcache.config.Configuration();
    configuration.addCache(new CacheConfiguration("test", 1));

    ClusteredEntityManager clusteredEntityManager = mock(ClusteredEntityManager.class);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        Object[] arguments = invocationOnMock.getArguments();
        ((ToolkitBackedClusteredCacheManager)arguments[2]).setEntityLockHandler(mock(EntityLockHandler.class));
        return null;
      }
    }).when(clusteredEntityManager).addRootEntityIfAbsent(eq(name), eq(ClusteredCacheManager.class), any(ClusteredCacheManager.class));
    ToolkitReadWriteLock rwLock = mock(ToolkitReadWriteLock.class);
    ToolkitLock writeLock = mock(ToolkitLock.class);
    when(writeLock.tryLock()).thenReturn(true);
    when(rwLock.writeLock()).thenReturn(writeLock);
    when(clusteredEntityManager.getEntityLock(any(String.class))).thenReturn(rwLock);

    ToolkitInstanceFactoryImpl toolkitInstanceFactory = new ToolkitInstanceFactoryImpl(getMockToolkit(),
                                                                                       clusteredEntityManager);

    toolkitInstanceFactory.linkClusteredCacheManager(name, configuration);
    ArgumentCaptor<ClusteredCacheManager> captor = ArgumentCaptor.forClass(ClusteredCacheManager.class);
    verify(clusteredEntityManager).addRootEntityIfAbsent(eq(name), any(Class.class), captor.capture());

    assertThat(captor.getValue().getConfiguration().getConfigurationAsText(), not(containsString("<cache")));
  }

  @Test
  public void testConfigurationSavedContainsCacheManagerName() {
    String name = "newCM";

    net.sf.ehcache.config.Configuration configuration = new net.sf.ehcache.config.Configuration();
    configuration.addCache(new CacheConfiguration("test", 1));

    Toolkit toolkit = getMockToolkit();
    ClusteredEntityManager clusteredEntityManager = mock(ClusteredEntityManager.class);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        Object[] arguments = invocationOnMock.getArguments();
        ((ToolkitBackedClusteredCacheManager)arguments[2]).setEntityLockHandler(mock(EntityLockHandler.class));
        return null;
      }
    }).when(clusteredEntityManager).addRootEntityIfAbsent(eq(name), eq(ClusteredCacheManager.class), any(ClusteredCacheManager.class));
    ToolkitReadWriteLock rwLock = mock(ToolkitReadWriteLock.class);
    ToolkitLock writeLock = mock(ToolkitLock.class);
    when(writeLock.tryLock()).thenReturn(true);
    when(rwLock.writeLock()).thenReturn(writeLock);
    when(clusteredEntityManager.getEntityLock(any(String.class))).thenReturn(rwLock);

    ToolkitInstanceFactoryImpl toolkitInstanceFactory = new ToolkitInstanceFactoryImpl(toolkit,
                                                                                       clusteredEntityManager);

    toolkitInstanceFactory.linkClusteredCacheManager(name, configuration);
    ArgumentCaptor<ClusteredCacheManager> captor = ArgumentCaptor.forClass(ClusteredCacheManager.class);
    verify(clusteredEntityManager).addRootEntityIfAbsent(eq(name), any(Class.class), captor.capture());

    assertThat(captor.getValue().getConfiguration().getConfigurationAsText(), containsString("name=\"" + name));
  }

  @Test
  public void testClusterRejoinedCacheManagerDestroyed() {
    Toolkit toolkit = getMockToolkit();
    ClusteredEntityManager entityManager = mock(ClusteredEntityManager.class);
    ToolkitInstanceFactoryImpl toolkitInstanceFactory = new ToolkitInstanceFactoryImpl(toolkit, entityManager);

    toolkitInstanceFactory.clusterRejoined();

    verify(entityManager).dispose();
    verify(toolkit).shutdown();
  }

  @Test
  public void testClusterRejoinedCMDestroyedBetweenGetAndLock() {
    Toolkit toolkit = getMockToolkit();
    ClusteredEntityManager entityManager = mock(ClusteredEntityManager.class);
    ClusteredCacheManager cmEntity = mock(ClusteredCacheManager.class);
    ClusteredCache cEntity = mock(ClusteredCache.class);
    when(cmEntity.getCache(CACHE_NAME)).thenReturn(cEntity);

    when(entityManager.getRootEntity(CACHE_MANAGER_NAME, ClusteredCacheManager.class)).thenReturn(cmEntity, cmEntity, null);
    when(entityManager.getEntityLock(any(String.class))).thenReturn(mock(ToolkitReadWriteLock.class));

    ToolkitInstanceFactoryImpl toolkitInstanceFactory = new ToolkitInstanceFactoryImpl(toolkit, entityManager);
    toolkitInstanceFactory.linkClusteredCacheManager(CACHE_MANAGER_NAME, null);
    toolkitInstanceFactory.addCacheEntityInfo(CACHE_NAME, null, EhcacheEntitiesNaming.getToolkitCacheNameFor(CACHE_MANAGER_NAME, CACHE_NAME));

    toolkitInstanceFactory.clusterRejoined();

    verify(cmEntity, times(2)).markInUse();
    verify(entityManager).dispose();
    verify(toolkit).shutdown();
  }

  private Toolkit getMockToolkit() {
    ToolkitInternal toolkitInternal = mock(ToolkitInternal.class);
    when(toolkitInternal.getLogger(anyString())).thenReturn(mock(ToolkitLogger.class));

    return toolkitInternal;
  }

  @Test
  public void testClusterRejoined() {
    Toolkit toolkit = getMockToolkit();
    ClusteredEntityManager entityManager = mock(ClusteredEntityManager.class);
    ClusteredCacheManager cmEntity = mock(ClusteredCacheManager.class);
    ClusteredCache cEntity = mock(ClusteredCache.class);
    when(cmEntity.getCache(CACHE_NAME)).thenReturn(cEntity);

    when(entityManager.getRootEntity(CACHE_MANAGER_NAME, ClusteredCacheManager.class)).thenReturn(cmEntity);
    when(entityManager.getEntityLock(any(String.class))).thenReturn(mock(ToolkitReadWriteLock.class));

    ToolkitInstanceFactoryImpl toolkitInstanceFactory = new ToolkitInstanceFactoryImpl(toolkit, entityManager);
    toolkitInstanceFactory.linkClusteredCacheManager(CACHE_MANAGER_NAME, null);
    toolkitInstanceFactory.addCacheEntityInfo(CACHE_NAME, null, EhcacheEntitiesNaming.getToolkitCacheNameFor(CACHE_MANAGER_NAME, CACHE_NAME));

    toolkitInstanceFactory.clusterRejoined();

    verify(cmEntity, times(2)).markInUse();
    verify(cmEntity, times(2)).markCacheInUse(cEntity);
  }

  private final String defaultCMConfig = "<ehcache name=\"test-lifecycle\">" +
                                         "  <terracottaConfig url=\"localhost:PORT\"/>" +
                                         "  <defaultCache" +
                                         "      maxElementsInMemory=\"10\"" +
                                         "      eternal=\"true\"/>" +
                                         "</ehcache>";

}

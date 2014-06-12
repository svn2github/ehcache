/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache.management.service.impl;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.ClusteredInstanceFactoryAccessor;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ManagementRESTServiceConfiguration;
import net.sf.ehcache.terracotta.ClusteredInstanceFactory;
import net.sf.ehcache.terracotta.TerracottaClient;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Ludovic Orban
 */
public class DfltSamplerRepositoryServiceV2Test {

  private DfltSamplerRepositoryServiceV2 repositoryService;
  private ClusteredInstanceFactory clusteredInstanceFactory;

  @Before
  public void setUp() throws Exception {
    ManagementRESTServiceConfiguration managementRESTServiceConfiguration = new ManagementRESTServiceConfiguration();
    managementRESTServiceConfiguration.setEnabled(true);
    CacheManagerPushEvents cacheManagerPushEvents = new CacheManagerPushEvents();
    repositoryService = new DfltSamplerRepositoryServiceV2("123", managementRESTServiceConfiguration, null,
        cacheManagerPushEvents);

    Configuration configuration = new Configuration();
    configuration.setName("testCacheManager");
    CacheConfiguration cacheConfiguration = new CacheConfiguration("testCache1", 12);
    configuration.addCache(cacheConfiguration);
    CacheManager cacheManager = new CacheManager(configuration);
    // Cache ehcache = new Cache(cacheConfiguration);
    TerracottaClient terracottaClient = mock(TerracottaClient.class);
    clusteredInstanceFactory = mock(ClusteredInstanceFactory.class);

    ClusteredInstanceFactoryAccessor.setTerracottaClient(cacheManager, terracottaClient);
    when(terracottaClient.getClusteredInstanceFactory()).thenReturn(clusteredInstanceFactory);

    repositoryService.register(cacheManager);
  }

  @Test
  public void testCreateCacheEntitiesDisablesNonStop() throws Exception {
    repositoryService.createCacheEntities(Collections.singleton("testCacheManager"),
        Collections.singleton("testCache1"),
        Collections.singleton("Size"));

    verify(clusteredInstanceFactory, times(2)).enableNonStopForCurrentThread(anyBoolean());
  }

  @Test
  public void testCreateCacheStatisticSampleEntityDisablesNonStop() throws Exception {
    repositoryService.createCacheStatisticSampleEntity(Collections.singleton("testCacheManager"),
        Collections.singleton("testCache1"),
        Collections.singleton("Size"));

    verify(clusteredInstanceFactory, times(2)).enableNonStopForCurrentThread(anyBoolean());
  }

  @Test
  public void testClearCacheDisablesNonStop() throws Exception {
    repositoryService.clearCache("testCacheManager", "testCache1");

    verify(clusteredInstanceFactory, times(2)).enableNonStopForCurrentThread(anyBoolean());
  }

  @After
  public void tearDown() {
    CacheManager.getCacheManager("testCacheManager").shutdown();
  }

}

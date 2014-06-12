/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache.management.service.impl;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.ClusteredInstanceFactoryAccessor;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.ManagementRESTServiceConfiguration;
import net.sf.ehcache.terracotta.ClusteredInstanceFactory;
import net.sf.ehcache.terracotta.TerracottaClient;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Ludovic Orban
 */
public class DfltSamplerRepositoryServiceTest {

  private DfltSamplerRepositoryService repositoryService;
  private ClusteredInstanceFactory clusteredInstanceFactory;

  @Before
  public void setUp() throws Exception {
    ManagementRESTServiceConfiguration managementRESTServiceConfiguration = new ManagementRESTServiceConfiguration();
    managementRESTServiceConfiguration.setEnabled(true);
    repositoryService = new DfltSamplerRepositoryService("123", managementRESTServiceConfiguration);

    CacheManager cacheManager = mock(CacheManager.class);
    Ehcache ehcache = mock(Ehcache.class);
    TerracottaClient terracottaClient = mock(TerracottaClient.class);
    clusteredInstanceFactory = mock(ClusteredInstanceFactory.class);

    when(cacheManager.getCacheNames()).thenReturn(new String[] {"testCache1"});
    when(cacheManager.getName()).thenReturn("testCacheManager");
    when(cacheManager.getEhcache(anyString())).thenReturn(ehcache);
    when(ehcache.getCacheConfiguration()).thenReturn(new CacheConfiguration());
    when(ehcache.getName()).thenReturn("testCache1");
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

}

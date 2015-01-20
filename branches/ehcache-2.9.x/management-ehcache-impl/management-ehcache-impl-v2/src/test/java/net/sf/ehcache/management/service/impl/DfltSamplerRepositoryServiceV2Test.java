/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache.management.service.impl;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.ClusteredInstanceFactoryAccessor;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ManagementRESTServiceConfiguration;
import net.sf.ehcache.constructs.blocking.BlockingCache;
import net.sf.ehcache.management.resource.CacheManagerEntityV2;
import net.sf.ehcache.terracotta.ClusteredInstanceFactory;
import net.sf.ehcache.terracotta.TerracottaClient;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.terracotta.management.resource.ResponseEntityV2;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.spy;

/**
 * @author Ludovic Orban
 */
public class DfltSamplerRepositoryServiceV2Test {

  private DfltSamplerRepositoryServiceV2 repositoryService;
  private ClusteredInstanceFactory clusteredInstanceFactory;
  private CacheManager cacheManager;
  private RemoteAgentEndpointImpl remoteAgentEndpoint;

  @Before
  public void setUp() throws Exception {
    ManagementRESTServiceConfiguration managementRESTServiceConfiguration = new ManagementRESTServiceConfiguration();
    managementRESTServiceConfiguration.setEnabled(true);
    remoteAgentEndpoint = Mockito.mock(RemoteAgentEndpointImpl.class);
    repositoryService = new DfltSamplerRepositoryServiceV2(managementRESTServiceConfiguration, remoteAgentEndpoint);
    Configuration configuration = new Configuration();
    configuration.setName("testCacheManager");
    CacheConfiguration cacheConfiguration = new CacheConfiguration("testCache1", 12);
    configuration.addCache(cacheConfiguration);
    cacheManager = new CacheManager(configuration);
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

    verify(clusteredInstanceFactory, times(4)).enableNonStopForCurrentThread(anyBoolean());
  }

  @Test
  public void testCreateCacheStatisticSampleEntityDisablesNonStop() throws Exception {
    repositoryService.createCacheStatisticSampleEntity(Collections.singleton("testCacheManager"),
        Collections.singleton("testCache1"),
        Collections.singleton("Size"));

    verify(clusteredInstanceFactory, times(4)).enableNonStopForCurrentThread(anyBoolean());
  }

  @Test
  public void testClearCacheDisablesNonStop() throws Exception {
    repositoryService.clearCache("testCacheManager", "testCache1");

    verify(clusteredInstanceFactory, times(4)).enableNonStopForCurrentThread(anyBoolean());
  }

  @Test
  public void testCanAddDecoratedCache() {
    Cache underlyingCache = new Cache(new CacheConfiguration("decoratedTestCache", 10));
    cacheManager.addCache(new BlockingCache(underlyingCache));
    // not using cacheEventListeners for management anymore : TAB-5077
    assertThat(underlyingCache.getCacheEventNotificationService().hasCacheEventListeners(), is(false));
  }

  @Test
  public void testPropertyListenerRemoval() throws Exception {
    CacheManager cacheManagerSpy = spy(cacheManager);
    repositoryService.unregister(cacheManagerSpy);
    verify(cacheManagerSpy).getConfiguration();
    repositoryService.register(cacheManager);
  }

  @Test
  public void testCreateCacheManagerEntitiesMatchingClientIDWithRemote() throws Exception{
    CacheManager cacheManagerSpy = spy(cacheManager);
    Set<String> dummyClientUUids = new HashSet<String>(1);
    dummyClientUUids.add("dummyid");
    when(remoteAgentEndpoint.getClientUUIDsListFromRemote()).thenReturn(dummyClientUUids);
    when(cacheManagerSpy.getClusterUUID()).thenReturn("dummyid");
    repositoryService.register(cacheManager);
    Set<String> cacheManagerNames = null;
    Set<String> attributes = new HashSet<String>();
    attributes.add(cacheManagerSpy.getClusterUUID());
    ResponseEntityV2<CacheManagerEntityV2> responseEntityV2 =  repositoryService.createCacheManagerEntities(cacheManagerNames, attributes);
    verify(cacheManagerSpy,times(2)).getClusterUUID();
    assertNotNull(responseEntityV2);
    assertEquals(responseEntityV2.getEntities().size(),1);
    CacheManagerEntityV2 cacheManagerEntityV2 = responseEntityV2.getEntities().iterator().next();
    assertEquals(cacheManagerEntityV2.getName(),cacheManagerSpy.getName());
  }


  @Test
  public void testCreateCacheManagerEntitiesMisMatchedClientIDWithRemote() throws Exception{
    CacheManager cacheManagerSpy = spy(cacheManager);
    Set<String> dummyClientUUids = new HashSet<String>(1);
    dummyClientUUids.add("dummyid");
    when(remoteAgentEndpoint.getClientUUIDsListFromRemote()).thenReturn(dummyClientUUids);
    when(cacheManagerSpy.getClusterUUID()).thenReturn("dummyidanother");
    repositoryService.register(cacheManager);
    ResponseEntityV2<CacheManagerEntityV2> responseEntityV2 =  repositoryService.createCacheManagerEntities(null, null);
    verify(cacheManagerSpy,times(1)).getClusterUUID();
    assertNotNull(responseEntityV2);
    assertEquals(responseEntityV2.getEntities().size(),0);
  }

  @Test
  public void testCreateCacheManagerEntitiesWithRemoteNotSendingAnyClientIDs() throws Exception{
    CacheManager cacheManagerSpy = spy(cacheManager);
    when(remoteAgentEndpoint.getClientUUIDsListFromRemote()).thenReturn(null);
    when(cacheManagerSpy.getClusterUUID()).thenReturn("dummyidanother");
    repositoryService.register(cacheManager);
    ResponseEntityV2<CacheManagerEntityV2> responseEntityV2 =  repositoryService.createCacheManagerEntities(null, null);
    verify(cacheManagerSpy,times(1)).getClusterUUID();
    assertNotNull(responseEntityV2);
    assertEquals(responseEntityV2.getEntities().size(),1);
    CacheManagerEntityV2 cacheManagerEntityV2 = responseEntityV2.getEntities().iterator().next();
    assertEquals(cacheManagerEntityV2.getName(),cacheManagerSpy.getName());
  }

  @After
  public void tearDown() {
    CacheManager.getCacheManager("testCacheManager").shutdown();
  }

}

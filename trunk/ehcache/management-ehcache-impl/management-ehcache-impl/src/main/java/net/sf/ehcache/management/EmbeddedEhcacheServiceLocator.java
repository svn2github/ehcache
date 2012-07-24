/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package net.sf.ehcache.management;

import net.sf.ehcache.config.ManagementRESTServiceConfiguration;
import net.sf.ehcache.management.service.AgentService;
import net.sf.ehcache.management.service.CacheManagerService;
import net.sf.ehcache.management.service.CacheService;
import net.sf.ehcache.management.service.EntityResourceFactory;
import net.sf.ehcache.management.service.SamplerRepositoryService;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.services.validator.RequestValidator;

/**
 * @author brandony
 */
public class EmbeddedEhcacheServiceLocator extends ServiceLocator
    implements CacheManagerService.Locator, CacheService.Locator, EntityResourceFactory.Locator,
    SamplerRepositoryService.Locator, AgentService.Locator {
  protected final boolean licensedLocator;

  private final CacheManagerService cacheMgrSvc;

  private final CacheService cacheSvc;

  private final EntityResourceFactory entityRsrcFactory;

  private final SamplerRepositoryService samplerRepoSvc;

  private final AgentService agentService;

  public static EmbeddedEhcacheServiceLocator locator() {
    return (EmbeddedEhcacheServiceLocator) ServiceLocator.locator();
  }

  public EmbeddedEhcacheServiceLocator(boolean licensedLocator,
                                       RequestValidator requestValidator,
                                       CacheManagerService cacheMgrSvc,
                                       CacheService cacheSvc,
                                       EntityResourceFactory entityRsrcFactory,
                                       SamplerRepositoryService samplerRepoSvc,
                                       AgentService agentService) {
    super(requestValidator);
    this.licensedLocator = licensedLocator;
    this.cacheMgrSvc = cacheMgrSvc;
    this.cacheSvc = cacheSvc;
    this.entityRsrcFactory = entityRsrcFactory;
    this.samplerRepoSvc = samplerRepoSvc;
    this.agentService = agentService;
  }

  @Override
  public EntityResourceFactory locateEntityResourceFactory() {
    return entityRsrcFactory;
  }

  @Override
  public CacheService locateCacheService() {
    return cacheSvc;
  }

  @Override
  public SamplerRepositoryService locateSamplerRepositoryService() {
    return samplerRepoSvc;
  }

  public boolean isLicensedLocator() {
    return licensedLocator;
  }

  @Override
  public CacheManagerService locateCacheManagerService() {
    return cacheMgrSvc;
  }

  @Override
  public AgentService locateAgentService() {
    return agentService;
  }
}

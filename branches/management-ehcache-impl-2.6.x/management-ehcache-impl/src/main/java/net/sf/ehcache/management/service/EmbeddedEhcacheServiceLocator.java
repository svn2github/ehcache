/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package net.sf.ehcache.management.service;

import org.terracotta.management.resource.services.validator.RequestValidator;
import org.terracotta.management.service.ServiceLocator;

/**
 * @author brandony
 */
public final class EmbeddedEhcacheServiceLocator extends ServiceLocator
    implements CacheService.Locator, EntityResourceFactory.Locator, SamplerRepositoryService.Locator {
  private final CacheService cacheSvc;

  private final EntityResourceFactory entityRsrcFactory;

  private final SamplerRepositoryService samplerRepoSvc;

  public EmbeddedEhcacheServiceLocator(RequestValidator requestValidator,
                                       CacheService cacheSvc,
                                       EntityResourceFactory entityRsrcFactory,
                                       SamplerRepositoryService samplerRepoSvc) {
    super(requestValidator);
    this.cacheSvc = cacheSvc;
    this.entityRsrcFactory = entityRsrcFactory;
    this.samplerRepoSvc = samplerRepoSvc;
  }

  public static EmbeddedEhcacheServiceLocator locator() {
    return (EmbeddedEhcacheServiceLocator)ServiceLocator.locator();
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
}

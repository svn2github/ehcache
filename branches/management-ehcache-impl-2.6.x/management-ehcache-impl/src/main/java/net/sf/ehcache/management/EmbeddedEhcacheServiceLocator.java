/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package net.sf.ehcache.management;

import net.sf.ehcache.management.service.CacheService;
import net.sf.ehcache.management.service.EntityResourceFactory;
import net.sf.ehcache.management.service.SamplerRepositoryService;
import org.terracotta.management.resource.services.validator.RequestValidator;
import org.terracotta.management.ServiceLocator;

/**
 * @author brandony
 */
public class EmbeddedEhcacheServiceLocator extends ServiceLocator
    implements CacheService.Locator, EntityResourceFactory.Locator, SamplerRepositoryService.Locator {
  private final CacheService cacheSvc;

  private final EntityResourceFactory entityRsrcFactory;

  private final SamplerRepositoryService samplerRepoSvc;

  public static EmbeddedEhcacheServiceLocator locator() {
    return (EmbeddedEhcacheServiceLocator)ServiceLocator.locator();
  }

  public EmbeddedEhcacheServiceLocator(RequestValidator requestValidator,
                                       CacheService cacheSvc,
                                       EntityResourceFactory entityRsrcFactory,
                                       SamplerRepositoryService samplerRepoSvc) {
    super(requestValidator);
    this.cacheSvc = cacheSvc;
    this.entityRsrcFactory = entityRsrcFactory;
    this.samplerRepoSvc = samplerRepoSvc;
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

/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package net.sf.ehcache.management;

import net.sf.ehcache.config.ManagementRESTServiceConfiguration;
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
  protected final boolean licensedLocator;

  private final CacheService cacheSvc;

  private final EntityResourceFactory entityRsrcFactory;

  private final SamplerRepositoryService samplerRepoSvc;

  private final ManagementRESTServiceConfiguration mgmtRESTSvcConfig;

  public static EmbeddedEhcacheServiceLocator locator() {
    return (EmbeddedEhcacheServiceLocator)ServiceLocator.locator();
  }

  public EmbeddedEhcacheServiceLocator(boolean licensedLocator,
                                       RequestValidator requestValidator,
                                       CacheService cacheSvc,
                                       EntityResourceFactory entityRsrcFactory,
                                       SamplerRepositoryService samplerRepoSvc,
                                       ManagementRESTServiceConfiguration mgmtRESTSvcConfig) {
    super(requestValidator);
    this.licensedLocator = licensedLocator;
    this.cacheSvc = cacheSvc;
    this.entityRsrcFactory = entityRsrcFactory;
    this.samplerRepoSvc = samplerRepoSvc;
    this.mgmtRESTSvcConfig = mgmtRESTSvcConfig;
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

  public ManagementRESTServiceConfiguration locateRESTConfiguration() {
    return mgmtRESTSvcConfig;
  }

  public boolean isLicensedLocator() {
    return licensedLocator;
  }
}

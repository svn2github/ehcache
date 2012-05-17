/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package net.sf.ehcache.management.resource.services;

import net.sf.ehcache.management.service.CacheService;
import net.sf.ehcache.management.service.EmbeddedEhcacheServiceLocator;
import org.terracotta.management.resource.services.validator.RequestValidator;

import javax.ws.rs.Path;
import javax.ws.rs.core.UriInfo;

/**
 * @author brandony
 */
@Path("/agents/cacheManagers/caches/elements")
public final class ElementsResourceServiceImpl implements ElementsResourceService {
  private CacheService cacheSvc;
  private RequestValidator validator;

  public ElementsResourceServiceImpl() {
    this.cacheSvc = EmbeddedEhcacheServiceLocator.locator().locateCacheService();
    this.validator = EmbeddedEhcacheServiceLocator.locator().locateRequestValidator();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteElements(UriInfo info) {
    validator.validate(info);
    String cacheManagerName = info.getPathSegments().get(1).getMatrixParameters().getFirst("names");
    String cacheName = info.getPathSegments().get(2).getMatrixParameters().getFirst("names");

    cacheSvc.clearCache(cacheManagerName, cacheName);
  }
}

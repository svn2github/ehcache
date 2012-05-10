/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package net.sf.ehcache.management.resource.services;

import com.sun.jersey.api.core.InjectParam;
import net.sf.ehcache.management.services.CacheService;
import net.sf.ehcache.management.validators.impl.CacheRequestValidator;

import javax.ws.rs.Path;
import javax.ws.rs.core.UriInfo;

/**
 * @author brandony
 */
@Path("/agents/cacheManagers/caches/elements")
public final class ElementsResourceServiceImpl implements ElementsResourceService {
  private CacheService cacheSvc;
  private CacheRequestValidator validator;

  public ElementsResourceServiceImpl(@InjectParam CacheService cacheSvc,
                                     @InjectParam CacheRequestValidator validator) {
    this.cacheSvc = cacheSvc;
    this.validator = validator;
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

/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package net.sf.ehcache.management.resource.services;

import net.sf.ehcache.management.service.CacheServiceV2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;
import org.terracotta.management.resource.services.validator.RequestValidator;

import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * A resource service interface for implementations interacting with cache elements.
 * 
 * @author brandony
 */
@Path("/v2/agents/cacheManagers/caches/elements")
public final class ElementsResourceServiceImplV2 {
  private static final Logger LOG = LoggerFactory.getLogger(ElementsResourceServiceImplV2.class);

  private final CacheServiceV2 cacheSvc;

  private final RequestValidator validator;

  public ElementsResourceServiceImplV2() {
    this.validator = ServiceLocator.locate(RequestValidator.class);
    this.cacheSvc = ServiceLocator.locate(CacheServiceV2.class);
  }

  /**
   * Remove elements from the cache.
   *
   * @param info
   *          for this resource request
   */
  @DELETE
  public void deleteElements(@Context UriInfo info) {
    LOG.debug(String.format("Invoking ElementsResourceServiceImpl.deleteElements: %s", info.getRequestUri()));

    validator.validate(info);
    String cacheManagerName = info.getPathSegments().get(2).getMatrixParameters().getFirst("names");
    String cacheName = info.getPathSegments().get(3).getMatrixParameters().getFirst("names");

    try {
      cacheSvc.clearCache(cacheManagerName, cacheName);
    } catch (ServiceExecutionException e) {
      throw new ResourceRuntimeException("Failed to delete element", e, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }
}

/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package net.sf.ehcache.management.resource.services;

import net.sf.ehcache.management.service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;
import org.terracotta.management.resource.services.validator.RequestValidator;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author brandony
 */
@Path("/agents/cacheManagers/caches/elements")
public final class ElementsResourceServiceImpl implements ElementsResourceService {
  private static final Logger LOG = LoggerFactory.getLogger(ElementsResourceServiceImpl.class);

  private final CacheService cacheSvc;

  private final RequestValidator validator;

  public ElementsResourceServiceImpl() {
    this.validator = ServiceLocator.locate(RequestValidator.class);
    this.cacheSvc = ServiceLocator.locate(CacheService.class);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteElements(UriInfo info) {
    LOG.debug(String.format("Invoking ElementsResourceServiceImpl.deleteElements: %s", info.getRequestUri()));

    validator.validate(info);
    String cacheManagerName = info.getPathSegments().get(1).getMatrixParameters().getFirst("names");
    String cacheName = info.getPathSegments().get(2).getMatrixParameters().getFirst("names");

    try {
      cacheSvc.clearCache(cacheManagerName, cacheName);
    } catch (ServiceExecutionException e) {
      throw new ResourceRuntimeException("Failed to delete element", e, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }
}

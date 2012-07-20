/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package net.sf.ehcache.management.resource.services;

import net.sf.ehcache.management.EmbeddedEhcacheServiceLocator;
import net.sf.ehcache.management.service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.resource.services.validator.RequestValidator;

import javax.ws.rs.Path;
import javax.ws.rs.core.UriInfo;

/**
 * @author brandony
 */
@Path("/agents/cacheManagers/caches/elements")
public final class ElementsResourceServiceImpl implements ElementsResourceService {
  private static final Logger LOG = LoggerFactory.getLogger(ElementsResourceServiceImpl.class);

  private CacheService cacheSvc;

  private RequestValidator validator;

  public ElementsResourceServiceImpl() {
    CacheService.Locator cacheSvcLocator = EmbeddedEhcacheServiceLocator.locator();
    this.cacheSvc = cacheSvcLocator.locateCacheService();
    RequestValidator.Locator reqValidatorLocator = EmbeddedEhcacheServiceLocator.locator();
    this.validator = reqValidatorLocator.locateRequestValidator();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteElements(UriInfo info) {
    LOG.info(String.format("Invoking ElementsResourceServiceImpl.deleteElements: %s", info.getRequestUri()));

    validator.validate(info);
    String cacheManagerName = info.getPathSegments().get(1).getMatrixParameters().getFirst("names");
    String cacheName = info.getPathSegments().get(2).getMatrixParameters().getFirst("names");

    cacheSvc.clearCache(cacheManagerName, cacheName);
  }
}

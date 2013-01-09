package net.sf.ehcache.management.resource.services;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import net.sf.ehcache.management.resource.CacheEntity;
import net.sf.ehcache.management.service.CacheService;
import net.sf.ehcache.management.service.EntityResourceFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.services.validator.RequestValidator;

/**
 * @author brandony
 */
@Path("/agents/cacheManagers/caches")
public final class CachesResourceServiceImpl implements CachesResourceService {
  private final static Logger LOG = LoggerFactory.getLogger(CachesResourceServiceImpl.class);

  private final EntityResourceFactory entityResourceFactory;

  private final CacheService cacheSvc;

  private final RequestValidator validator;

  public CachesResourceServiceImpl() {
    this.entityResourceFactory = ServiceLocator.locate(EntityResourceFactory.class);
    this.validator = ServiceLocator.locate(RequestValidator.class);
    this.cacheSvc = ServiceLocator.locate(CacheService.class);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<CacheEntity> getCaches(final UriInfo info) {
    LOG.info(String.format("Invoking CachesResourceServiceImpl.getCaches: %s", info.getRequestUri()));

    validator.validateSafe(info);

    String cacheManagerNames = info.getPathSegments().get(1).getMatrixParameters().getFirst("names");
    Set<String> cmNames = cacheManagerNames == null ? null : new HashSet<String>(
        Arrays.asList(cacheManagerNames.split(",")));

    String cacheNames = info.getPathSegments().get(2).getMatrixParameters().getFirst("names");
    Set<String> cNames = cacheNames == null ? null : new HashSet<String>(Arrays.asList(cacheNames.split(",")));

    MultivaluedMap<String, String> qParams = info.getQueryParameters();
    List<String> attrs = qParams.get(ATTR_QUERY_KEY);
    Set<String> cAttrs = attrs == null || attrs.isEmpty() ? null : new HashSet<String>(attrs);

    try {
      return entityResourceFactory.createCacheEntities(cmNames, cNames, cAttrs);
    } catch (ServiceExecutionException e) {
      Throwable exceptionToLog = e.getCause() != null ? e.getCause() :  e;
      LOG.error("Failed to get caches.", exceptionToLog);
      throw new WebApplicationException(
          Response.status(Response.Status.BAD_REQUEST).entity(exceptionToLog.getMessage()).build());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void createOrUpdateCache(final UriInfo info,
                                  CacheEntity resource) {
    LOG.info(String.format("Invoking CachesResourceServiceImpl.createOrUpdateCache: %s", info.getRequestUri()));

    validator.validate(info);

    String cacheManagerName = info.getPathSegments().get(1).getMatrixParameters().getFirst("names");

    String cacheName = info.getPathSegments().get(2).getMatrixParameters().getFirst("names");

    try {
      cacheSvc.createOrUpdateCache(cacheManagerName, cacheName, resource);
    } catch (ServiceExecutionException e) {
      LOG.error("Failed to create or update cache.", e.getCause());
      throw new WebApplicationException(e.getCause(),
        Response.status(Response.Status.BAD_REQUEST)
          .entity(e.getCause().getMessage())
          .type("text/plain")
          .build());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteCache(final UriInfo info) {
    LOG.info(String.format("Invoking CachesResourceServiceImpl.deleteCache: %s", info.getRequestUri()));

    //todo: implement
    throw new WebApplicationException(
        Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("Not yet implemented").build());
  }

}

package net.sf.ehcache.management.resource.services;

import net.sf.ehcache.management.EmbeddedEhcacheServiceLocator;
import net.sf.ehcache.management.resource.CacheEntity;
import net.sf.ehcache.management.service.CacheService;
import net.sf.ehcache.management.service.EntityResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.resource.services.validator.RequestValidator;

import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    EntityResourceFactory.Locator entityRsrcFactoryLocator = EmbeddedEhcacheServiceLocator.locator();
    this.entityResourceFactory = entityRsrcFactoryLocator.locateEntityResourceFactory();
    CacheService.Locator cacheSvcLocator = EmbeddedEhcacheServiceLocator.locator();
    this.cacheSvc = cacheSvcLocator.locateCacheService();
    RequestValidator.Locator reqValidatorLocator = EmbeddedEhcacheServiceLocator.locator();
    this.validator = reqValidatorLocator.locateRequestValidator();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<CacheEntity> getCaches(final UriInfo info) {
    validator.validateSafe(info);

    String cacheManagerNames = info.getPathSegments().get(1).getMatrixParameters().getFirst("names");
    Set<String> cmNames = cacheManagerNames == null ? null : new HashSet<String>(
        Arrays.asList(cacheManagerNames.split(",")));

    String cacheNames = info.getPathSegments().get(2).getMatrixParameters().getFirst("names");
    Set<String> cNames = cacheNames == null ? null : new HashSet<String>(Arrays.asList(cacheNames.split(",")));

    MultivaluedMap<String, String> qParams = info.getQueryParameters();
    List<String> attrs = qParams.get(ATTR_QUERY_KEY);
    Set<String> cAttrs = attrs == null || attrs.isEmpty() ? null : new HashSet<String>(attrs);

    return entityResourceFactory.createCacheEntities(cmNames, cNames, cAttrs);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void createOrUpdateCache(final UriInfo info,
                                  CacheEntity resource) {
    validator.validate(info);

    String cacheManagerName = info.getPathSegments().get(1).getMatrixParameters().getFirst("names");

    String cacheName = info.getPathSegments().get(2).getMatrixParameters().getFirst("names");

    try {
      cacheSvc.createOrUpdateCache(cacheManagerName, cacheName, resource);
    } catch (Exception e) {
      LOG.error("Failed to create or update cache.", e.getCause());
      throw new WebApplicationException(
          Response.status(Response.Status.BAD_REQUEST).entity(e.getCause().getMessage()).build());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteCache(final UriInfo info) {
    //todo: implement
    throw new WebApplicationException(
        Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("Not yet implemented").build());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void wipeStatistics(final UriInfo info) {
    validator.validate(info);
    String cacheManagerName = info.getPathSegments().get(1).getMatrixParameters().getFirst("names");
    String cacheName = info.getPathSegments().get(2).getMatrixParameters().getFirst("names");

    cacheSvc.clearCacheStats(cacheManagerName, cacheName);
  }

}

package net.sf.ehcache.management.resource.services;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import net.sf.ehcache.management.resource.CacheEntityV2;
import net.sf.ehcache.management.service.CacheServiceV2;
import net.sf.ehcache.management.service.EntityResourceFactoryV2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.ResponseEntityV2;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;
import org.terracotta.management.resource.services.validator.RequestValidator;

/**
 * 
 * <p>
 * A resource service for interacting with ehcache caches via the {@link CacheEntityV2}.
 * </p>
 * 
 * @author brandony
 */
@Path("/v2/agents/cacheManagers/caches")
public final class CachesResourceServiceImplV2 {

  public static final String ATTR_QUERY_KEY = "show";

  private final static Logger LOG = LoggerFactory.getLogger(CachesResourceServiceImplV2.class);

  private final EntityResourceFactoryV2 entityResourceFactory;

  private final CacheServiceV2 cacheSvc;

  private final RequestValidator validator;

  public CachesResourceServiceImplV2() {
    this.entityResourceFactory = ServiceLocator.locate(EntityResourceFactoryV2.class);
    this.validator = ServiceLocator.locate(RequestValidator.class);
    this.cacheSvc = ServiceLocator.locate(CacheServiceV2.class);
  }

  /**
   * <p>
   * Get a {@code Collection} of {@link CacheEntityV2} objects representing the cache information provided by the
   * associated monitorable entity's agent given the request path.
   * </p>
   * 
   * @param info
   *          {@link UriInfo} for this resource request
   * @return a collection of {@link CacheEntityV2} objects when successful.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntityV2 getCaches(@Context UriInfo info) {
    LOG.debug(String.format("Invoking CachesResourceServiceImpl.getCaches: %s", info.getRequestUri()));

    validator.validateSafe(info);

    String cacheManagerNames = info.getPathSegments().get(2).getMatrixParameters().getFirst("names");
    Set<String> cmNames = cacheManagerNames == null ? null : new HashSet<String>(
        Arrays.asList(cacheManagerNames.split(",")));

    String cacheNames = info.getPathSegments().get(3).getMatrixParameters().getFirst("names");
    Set<String> cNames = cacheNames == null ? null : new HashSet<String>(Arrays.asList(cacheNames.split(",")));

    MultivaluedMap<String, String> qParams = info.getQueryParameters();
    List<String> attrs = qParams.get(ATTR_QUERY_KEY);
    Set<String> cAttrs = attrs == null || attrs.isEmpty() ? null : new HashSet<String>(attrs);

    try {
      return entityResourceFactory.createCacheEntities(cmNames, cNames, cAttrs);
    } catch (ServiceExecutionException e) {
      throw new ResourceRuntimeException("Failed to get caches", e, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  /**
   * <p>
   * Create or update a cache with the name specified in the request path, for a specific agent and cache manager. The
   * request path that does not identify a unique cache resource for creation or identifies a cache that already exists
   * will constitute a bad request and will be denied, resulting in a response with a 400 and 409 respectively.
   * </p>
   * 
   * @param info
   *          {@link UriInfo} for this resource request
   * @param resource
   *          {@code CacheEntity} resource for update or creation
   */
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  public void createOrUpdateCache(@Context UriInfo info, CacheEntityV2 resource) {
    LOG.debug(String.format("Invoking CachesResourceServiceImpl.createOrUpdateCache: %s", info.getRequestUri()));

    validator.validate(info);

    String cacheManagerName = info.getPathSegments().get(2).getMatrixParameters().getFirst("names");

    String cacheName = info.getPathSegments().get(3).getMatrixParameters().getFirst("names");

    try {
      cacheSvc.createOrUpdateCache(cacheManagerName, cacheName, resource);
    } catch (ServiceExecutionException e) {
      throw new ResourceRuntimeException("Failed to create or update cache", e, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }
}

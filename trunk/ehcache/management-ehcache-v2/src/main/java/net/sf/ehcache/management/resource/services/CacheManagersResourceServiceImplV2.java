package net.sf.ehcache.management.resource.services;

import net.sf.ehcache.management.resource.CacheManagerEntityV2;
import net.sf.ehcache.management.service.CacheManagerServiceV2;
import net.sf.ehcache.management.service.EntityResourceFactoryV2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.ResponseEntityV2;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;
import org.terracotta.management.resource.services.validator.RequestValidator;

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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>An implementation of {@link CacheManagersResourceService}.</p>
 *
 * @author brandony
 */
@Path("/v2/agents/cacheManagers")
public final class CacheManagersResourceServiceImplV2 {
  public final static String ATTR_QUERY_KEY = "show";

  private static final Logger LOG = LoggerFactory.getLogger(CacheManagersResourceServiceImplV2.class);

  private final EntityResourceFactoryV2 entityResourceFactory;

  private final RequestValidator validator;

  private final CacheManagerServiceV2 cacheMgrSvc;

  public CacheManagersResourceServiceImplV2() {
    this.entityResourceFactory = ServiceLocator.locate(EntityResourceFactoryV2.class);
    this.validator = ServiceLocator.locate(RequestValidator.class);
    this.cacheMgrSvc = ServiceLocator.locate(CacheManagerServiceV2.class);
  }

  /**
   * <p>
   * Get a {@code Collection} of {@link CacheManagerEntityV2} objects representing the cache manager information provided
   * by the associated monitorable entity's agent given the request path.
   * </p>
   * 
   * @param {@link UriInfo} for this resource request
   * @return a collection of {@link CacheManagerEntityV2} objects when successful.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntityV2 getCacheManagers(@Context UriInfo info) {
    LOG.debug(String.format("Invoking getCacheManagers: %s", info.getRequestUri()));

    validator.validateSafe(info);

    String names = info.getPathSegments().get(1).getMatrixParameters().getFirst("names");
    Set<String> cmNames = names == null ? null : new HashSet<String>(Arrays.asList(names.split(",")));

    MultivaluedMap<String, String> qParams = info.getQueryParameters();
    List<String> attrs = qParams.get(ATTR_QUERY_KEY);
    Set<String> cmAttrs = attrs == null || attrs.isEmpty() ? null : new HashSet<String>(attrs);

    try {
      return entityResourceFactory.createCacheManagerEntities(cmNames, cmAttrs);
    } catch (ServiceExecutionException e) {
      throw new ResourceRuntimeException("Failed to get cache managers", e, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  /**
   * Update a cache manager with the name specified in the request path, for a specific agent. The request path that
   * does not identify a unique cache manager resource for update will constitute a bad request and will be denied,
   * resulting in a response with a 400.
   *
   * @param info
   *          {@link UriInfo} for this resource request
   * @param resource
   *          {@code CacheEntity} resource for update or creation
   */
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  public void updateCacheManager(@Context UriInfo info,
      CacheManagerEntityV2 resource) {
    LOG.debug(String.format("Invoking updateCacheManager: %s", info.getRequestUri()));

    validator.validate(info);

    String cacheManagerName = info.getPathSegments().get(1).getMatrixParameters().getFirst("names");

    try {
      cacheMgrSvc.updateCacheManager(cacheManagerName, resource);
    } catch (ServiceExecutionException e) {
      throw new ResourceRuntimeException("Failed to update cache manager", e, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }
}

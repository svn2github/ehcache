package net.sf.ehcache.management.resource.services;

import net.sf.ehcache.management.service.EntityResourceFactoryV2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.ResponseEntityV2;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;
import org.terracotta.management.resource.services.validator.RequestValidator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author brandony
 */
@Path("/v2/agents/cacheManagers/caches/configs")
public final class CacheConfigsResourceServiceImplV2 {
  private static final Logger LOG = LoggerFactory.getLogger(CacheConfigsResourceServiceImplV2.class);

  private final EntityResourceFactoryV2 entityResourceFactory;

  private final RequestValidator validator;

  public CacheConfigsResourceServiceImplV2() {
    this.entityResourceFactory = ServiceLocator.locate(EntityResourceFactoryV2.class);
    this.validator = ServiceLocator.locate(RequestValidator.class);
  }

  /**
   * Get a {@code Collection} of {@link net.sf.ehcache.management.resource.CacheConfigEntityV2} objects representing the
   * cache manager configuration information provided by the associated monitorable entity's agent given the request
   * path.
   *
   *
   * @param {@link UriInfo} for this resource request
   * @return a collection of CacheConfigEntity objects. {@link net.sf.ehcache.management.resource.CacheConfigEntityV2}
   *         objects
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntityV2 getCacheConfigs(@Context UriInfo info) {
    LOG.debug(String.format("Invoking CacheConfigsResourceServiceImpl.getCacheConfigs: %s", info.getRequestUri()));

    validator.validateSafe(info);

    String cacheManagerNames = info.getPathSegments().get(1).getMatrixParameters().getFirst("names");
    Set<String> cmNames = cacheManagerNames == null ? null : new HashSet<String>(
        Arrays.asList(cacheManagerNames.split(",")));

    String cacheNames = info.getPathSegments().get(2).getMatrixParameters().getFirst("names");
    Set<String> cNames = cacheNames == null ? null : new HashSet<String>(Arrays.asList(cacheNames.split(",")));

    try {
      return entityResourceFactory.createCacheConfigEntities(cmNames, cNames);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get cache configs", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }
}

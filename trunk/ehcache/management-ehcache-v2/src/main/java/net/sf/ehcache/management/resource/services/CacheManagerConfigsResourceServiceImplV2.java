package net.sf.ehcache.management.resource.services;

import net.sf.ehcache.management.resource.CacheManagerConfigEntityV2;
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
@Path("/v2/agents/cacheManagers/configs")
public final class CacheManagerConfigsResourceServiceImplV2 {
  private static final Logger LOG = LoggerFactory.getLogger(CacheManagerConfigsResourceServiceImplV2.class);

  private final EntityResourceFactoryV2 entityResourceFactory;

  private final RequestValidator validator;

  public CacheManagerConfigsResourceServiceImplV2() {
    this.entityResourceFactory = ServiceLocator.locate(EntityResourceFactoryV2.class);
    this.validator = ServiceLocator.locate(RequestValidator.class);
  }

  /**
   * Get a {@code Collection} of {@link CacheManagerConfigEntityV2} objects representing the cache manager configuration
   * information provided by the associated monitorable entity's agent given the request path.
   *
   *
   * @param {@link UriInfo} for this resource request
   * @return a collection of {@link CacheManagerConfigEntityV2} objects
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntityV2 getCacheManagerConfigs(@Context UriInfo info) {
    LOG.debug(String
        .format("Invoking CacheManagerConfigsResourceServiceImpl.getXMLCacheManagerConfigs: %s", info.getRequestUri()));

    validator.validateSafe(info);

    String names = info.getPathSegments().get(2).getMatrixParameters().getFirst("names");
    Set<String> cmNames = names == null ? null : new HashSet<String>(Arrays.asList(names.split(",")));

    try {
      return entityResourceFactory.createCacheManagerConfigEntities(cmNames);
    } catch (ServiceExecutionException e) {
      throw new ResourceRuntimeException("Failed to get xml cache manager configs", e,
          Response.Status.BAD_REQUEST.getStatusCode());
    }
  }
}

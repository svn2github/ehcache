package net.sf.ehcache.management.resource.services;

import net.sf.ehcache.management.resource.CacheManagerConfigEntity;
import net.sf.ehcache.management.service.EntityResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;
import org.terracotta.management.resource.services.validator.RequestValidator;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author brandony
 */
@Path("/agents/cacheManagers/configs")
public final class CacheManagerConfigsResourceServiceImpl implements CacheManagerConfigsResourceService {
  private static final Logger LOG = LoggerFactory.getLogger(CacheManagerConfigsResourceService.class);

  private final EntityResourceFactory entityResourceFactory;

  private final RequestValidator validator;

  public CacheManagerConfigsResourceServiceImpl() {
    this.entityResourceFactory = ServiceLocator.locate(EntityResourceFactory.class);
    this.validator = ServiceLocator.locate(RequestValidator.class);
  }

  @Override
  public Collection<CacheManagerConfigEntity> getCacheManagerConfigs(UriInfo info) {
    LOG.debug(String
        .format("Invoking CacheManagerConfigsResourceServiceImpl.getXMLCacheManagerConfigs: %s", info.getRequestUri()));

    validator.validateSafe(info);

    String names = info.getPathSegments().get(1).getMatrixParameters().getFirst("names");
    Set<String> cmNames = names == null ? null : new HashSet<String>(Arrays.asList(names.split(",")));

    try {
      return entityResourceFactory.createCacheManagerConfigEntities(cmNames);
    } catch (ServiceExecutionException e) {
      throw new ResourceRuntimeException("Failed to get xml cache manager configs", e,
          Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  @Override
  public Collection<CacheManagerConfigEntity> getXMLCacheManagerConfigs(UriInfo info) {
    return getCacheManagerConfigs(info);
  }
}

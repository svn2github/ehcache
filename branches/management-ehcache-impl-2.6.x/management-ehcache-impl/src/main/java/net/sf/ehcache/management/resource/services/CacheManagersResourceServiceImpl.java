package net.sf.ehcache.management.resource.services;

import net.sf.ehcache.management.resource.CacheManagerEntity;
import net.sf.ehcache.management.service.CacheManagerService;
import net.sf.ehcache.management.service.EntityResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.ServiceExecutionException;
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
 * <p>An implementation of {@link CacheManagersResourceService}.</p>
 *
 * @author brandony
 */
@Path("/agents/cacheManagers")
public final class CacheManagersResourceServiceImpl implements CacheManagersResourceService {
  private static final Logger LOG = LoggerFactory.getLogger(CacheManagersResourceServiceImpl.class);

  private final EntityResourceFactory entityResourceFactory;

  private final RequestValidator validator;

  private final CacheManagerService cacheMgrSvc;

  public CacheManagersResourceServiceImpl() {
    this.entityResourceFactory = ServiceLocator.locate(EntityResourceFactory.class);
    this.validator = ServiceLocator.locate(RequestValidator.class);
    this.cacheMgrSvc = ServiceLocator.locate(CacheManagerService.class);
  }

  /**
   * {@inheritDoc}
   */
  public Collection<CacheManagerEntity> getCacheManagers(UriInfo info) {
    LOG.info(String.format("Invoking getCacheManagers: %s", info.getRequestUri()));

    validator.validateSafe(info);

    String names = info.getPathSegments().get(1).getMatrixParameters().getFirst("names");
    Set<String> cmNames = names == null ? null : new HashSet<String>(Arrays.asList(names.split(",")));

    MultivaluedMap<String, String> qParams = info.getQueryParameters();
    List<String> attrs = qParams.get(ATTR_QUERY_KEY);
    Set<String> cmAttrs = attrs == null || attrs.isEmpty() ? null : new HashSet<String>(attrs);

    return entityResourceFactory.createCacheManagerEntities(cmNames, cmAttrs);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateCacheManager(UriInfo info,
                                 CacheManagerEntity resource) {
    LOG.info(String.format("Invoking updateCacheManager: %s", info.getRequestUri()));

    validator.validate(info);

    String cacheManagerName = info.getPathSegments().get(1).getMatrixParameters().getFirst("names");

    try {
      cacheMgrSvc.updateCacheManager(cacheManagerName, resource);
    } catch (ServiceExecutionException e) {
      LOG.error("Failed to update cache manager.", e.getCause());
      throw new WebApplicationException(
          Response.status(Response.Status.BAD_REQUEST).entity(e.getCause().getMessage()).build());
    }
  }
}

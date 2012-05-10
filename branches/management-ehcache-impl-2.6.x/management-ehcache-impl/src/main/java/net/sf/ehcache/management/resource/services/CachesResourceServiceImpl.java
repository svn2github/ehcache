package net.sf.ehcache.management.resource.services;

import com.sun.jersey.api.core.InjectParam;
import net.sf.ehcache.management.resource.CacheEntity;
import net.sf.ehcache.management.services.CacheService;
import net.sf.ehcache.management.services.EntityResourceFactory;
import net.sf.ehcache.management.validators.impl.CacheRequestValidator;
import org.terracotta.management.resource.services.Utils;

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

  private final EntityResourceFactory entityResourceFactory;

  private final CacheService cacheSvc;

  private final CacheRequestValidator validator;

  public CachesResourceServiceImpl(@InjectParam EntityResourceFactory entityResourceFactory,
                                   @InjectParam CacheService cacheSvc,
                                   @InjectParam CacheRequestValidator validator) {
    this.entityResourceFactory = entityResourceFactory;
    this.cacheSvc = cacheSvc;
    this.validator = validator;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Response getCaches(final UriInfo info) {
    validator.validateSafe(info);

    String cacheManagerNames = info.getPathSegments().get(1).getMatrixParameters().getFirst("names");
    Set<String> cmNames = cacheManagerNames == null ? null : new HashSet<String>(
        Arrays.asList(cacheManagerNames.split(",")));

    String cacheNames = info.getPathSegments().get(2).getMatrixParameters().getFirst("names");
    Set<String> cNames = cacheNames == null ? null : new HashSet<String>(Arrays.asList(cacheNames.split(",")));

    MultivaluedMap<String, String> qParams = info.getQueryParameters();
    List<String> attrs = qParams.get(ATTR_QUERY_KEY);
    Set<String> cAttrs = attrs == null || attrs.isEmpty() ? null : new HashSet<String>(attrs);

    Collection<CacheEntity> entities = entityResourceFactory.createCacheEntities(cmNames, cNames, cAttrs);

    return Utils.buildNoCacheResponse(entities);
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

    cacheSvc.createOrUpdateCache(cacheManagerName, cacheName, resource);
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

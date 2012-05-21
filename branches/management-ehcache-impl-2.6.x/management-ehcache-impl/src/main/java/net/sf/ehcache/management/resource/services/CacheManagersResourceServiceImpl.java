package net.sf.ehcache.management.resource.services;

import net.sf.ehcache.management.resource.CacheManagerEntity;
import net.sf.ehcache.management.service.EmbeddedEhcacheServiceLocator;
import net.sf.ehcache.management.service.EntityResourceFactory;
import org.terracotta.management.resource.services.Utils;
import org.terracotta.management.resource.services.validator.RequestValidator;

import javax.ws.rs.Path;
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

  private final EntityResourceFactory entityResourceFactory;

  private final RequestValidator validator;

  public CacheManagersResourceServiceImpl() {
    this.entityResourceFactory = EmbeddedEhcacheServiceLocator.locator().locateEntityResourceFactory();
    this.validator = EmbeddedEhcacheServiceLocator.locator().locateRequestValidator();
  }

  /**
   * {@inheritDoc}
   */
  public Collection<CacheManagerEntity> getCacheManagers(UriInfo info) {
    validator.validateSafe(info);

    String names = info.getPathSegments().get(1).getMatrixParameters().getFirst("names");
    Set<String> cmNames = names == null ? null : new HashSet<String>(Arrays.asList(names.split(",")));

    MultivaluedMap<String, String> qParams = info.getQueryParameters();
    List<String> attrs = qParams.get(ATTR_QUERY_KEY);
    Set<String> cmAttrs = attrs == null || attrs.isEmpty() ? null : new HashSet<String>(attrs);

    return entityResourceFactory.createCacheManagerEntities(cmNames, cmAttrs);
  }
}

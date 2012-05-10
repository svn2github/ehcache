/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache.management.resource.services;

import com.sun.jersey.api.core.InjectParam;
import net.sf.ehcache.management.resource.CacheStatisticSampleEntity;
import net.sf.ehcache.management.services.EntityResourceFactory;
import net.sf.ehcache.management.validators.impl.CacheRequestValidator;
import org.terracotta.management.resource.services.Utils;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author brandony
 */
@Path("/agents/cacheManagers/caches/statistics/samples")
public final class CacheStatisticSamplesResourceServiceImpl implements CacheStatisticSamplesResourceService {
  private final EntityResourceFactory entityResourceFactory;

  private final CacheRequestValidator validator;

  public CacheStatisticSamplesResourceServiceImpl(@InjectParam EntityResourceFactory entityResourceFactory,
                                                  @InjectParam CacheRequestValidator validator) {
    this.entityResourceFactory = entityResourceFactory;
    this.validator = validator;
  }

  @Override
  public Response getCacheStatisticSamples(UriInfo info) {
    validator.validateSafe(info);

    String cacheManagerNames = info.getPathSegments().get(1).getMatrixParameters().getFirst("names");
    Set<String> cmNames = cacheManagerNames == null ? null : new HashSet<String>(
        Arrays.asList(cacheManagerNames.split(",")));

    String cacheNames = info.getPathSegments().get(2).getMatrixParameters().getFirst("names");
    Set<String> cNames = cacheNames == null ? null : new HashSet<String>(Arrays.asList(cacheNames.split(",")));

    String sampleNames = info.getPathSegments().get(4).getMatrixParameters().getFirst("names");
    Set<String> sNames = sampleNames == null ? null : new HashSet<String>(Arrays.asList(sampleNames.split(",")));

    Collection<CacheStatisticSampleEntity> entities = entityResourceFactory
        .createCacheStatisticSampleEntity(cmNames, cNames, sNames);

    return Utils.buildNoCacheResponse(entities);
  }
}

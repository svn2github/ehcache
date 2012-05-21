/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache.management.resource.services;

import net.sf.ehcache.management.resource.CacheStatisticSampleEntity;
import net.sf.ehcache.management.service.EmbeddedEhcacheServiceLocator;
import net.sf.ehcache.management.service.EntityResourceFactory;
import org.terracotta.management.resource.services.Utils;
import org.terracotta.management.resource.services.validator.RequestValidator;

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

  private final RequestValidator validator;

  public CacheStatisticSamplesResourceServiceImpl() {
    this.entityResourceFactory = EmbeddedEhcacheServiceLocator.locator().locateEntityResourceFactory();
    this.validator = EmbeddedEhcacheServiceLocator.locator().locateRequestValidator();
  }

  @Override
  public Collection<CacheStatisticSampleEntity> getCacheStatisticSamples(UriInfo info) {
    validator.validateSafe(info);

    String cacheManagerNames = info.getPathSegments().get(1).getMatrixParameters().getFirst("names");
    Set<String> cmNames = cacheManagerNames == null ? null : new HashSet<String>(
        Arrays.asList(cacheManagerNames.split(",")));

    String cacheNames = info.getPathSegments().get(2).getMatrixParameters().getFirst("names");
    Set<String> cNames = cacheNames == null ? null : new HashSet<String>(Arrays.asList(cacheNames.split(",")));

    String sampleNames = info.getPathSegments().get(4).getMatrixParameters().getFirst("names");
    Set<String> sNames = sampleNames == null ? null : new HashSet<String>(Arrays.asList(sampleNames.split(",")));

    return entityResourceFactory.createCacheStatisticSampleEntity(cmNames, cNames, sNames);
  }
}

/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache.management.resource.services;

import net.sf.ehcache.management.resource.CacheStatisticSampleEntity;
import net.sf.ehcache.management.service.EntityResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;
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
  private static final Logger LOG = LoggerFactory.getLogger(CacheStatisticSamplesResourceServiceImpl.class);
  private final EntityResourceFactory entityResourceFactory;

  private final RequestValidator validator;

  public CacheStatisticSamplesResourceServiceImpl() {
    this.entityResourceFactory = ServiceLocator.locate(EntityResourceFactory.class);
    this.validator = ServiceLocator.locate(RequestValidator.class);
  }

  @Override
  public Collection<CacheStatisticSampleEntity> getCacheStatisticSamples(UriInfo info) {
    LOG.debug(String.format("Invoking CacheStatisticSamplesResourceServiceImpl.getCacheStatisticSamples: %s",
        info.getRequestUri()));

    validator.validateSafe(info);

    String cacheManagerNames = info.getPathSegments().get(1).getMatrixParameters().getFirst("names");
    Set<String> cmNames = cacheManagerNames == null ? null : new HashSet<String>(
        Arrays.asList(cacheManagerNames.split(",")));

    String cacheNames = info.getPathSegments().get(2).getMatrixParameters().getFirst("names");
    Set<String> cNames = cacheNames == null ? null : new HashSet<String>(Arrays.asList(cacheNames.split(",")));

    String sampleNames = info.getPathSegments().get(4).getMatrixParameters().getFirst("names");
    Set<String> sNames = sampleNames == null ? null : new HashSet<String>(Arrays.asList(sampleNames.split(",")));

    try {
      return entityResourceFactory.createCacheStatisticSampleEntity(cmNames, cNames, sNames);
    } catch (ServiceExecutionException e) {
      throw new ResourceRuntimeException("Failed to get cache statistics sample", e,
          Response.Status.BAD_REQUEST.getStatusCode());
    }
  }
}

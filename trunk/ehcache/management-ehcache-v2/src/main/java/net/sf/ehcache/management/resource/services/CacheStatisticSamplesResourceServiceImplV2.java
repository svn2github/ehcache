/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache.management.resource.services;

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

import net.sf.ehcache.management.service.EntityResourceFactoryV2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.ResponseEntityV2;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;
import org.terracotta.management.resource.services.validator.RequestValidator;

/**
 * @author brandony
 */
@Path("/v2/agents/cacheManagers/caches/statistics/samples")
public final class CacheStatisticSamplesResourceServiceImplV2 {
  private static final Logger LOG = LoggerFactory.getLogger(CacheStatisticSamplesResourceServiceImplV2.class);
  private final EntityResourceFactoryV2 entityResourceFactory;

  private final RequestValidator validator;

  public CacheStatisticSamplesResourceServiceImplV2() {
    this.entityResourceFactory = ServiceLocator.locate(EntityResourceFactoryV2.class);
    this.validator = ServiceLocator.locate(RequestValidator.class);
  }

  /**
   *
   * @param info
   * @return
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ResponseEntityV2 getCacheStatisticSamples(@Context UriInfo info) {
    LOG.debug(String.format("Invoking CacheStatisticSamplesResourceServiceImpl.getCacheStatisticSamples: %s",
        info.getRequestUri()));

    validator.validateSafe(info);

    String cacheManagerNames = info.getPathSegments().get(2).getMatrixParameters().getFirst("names");
    Set<String> cmNames = cacheManagerNames == null ? null : new HashSet<String>(
        Arrays.asList(cacheManagerNames.split(",")));

    String cacheNames = info.getPathSegments().get(3).getMatrixParameters().getFirst("names");
    Set<String> cNames = cacheNames == null ? null : new HashSet<String>(Arrays.asList(cacheNames.split(",")));

    String sampleNames = info.getPathSegments().get(5).getMatrixParameters().getFirst("names");
    Set<String> sNames = sampleNames == null ? null : new HashSet<String>(Arrays.asList(sampleNames.split(",")));

    try {
      return entityResourceFactory.createCacheStatisticSampleEntity(cmNames, cNames, sNames);
    } catch (ServiceExecutionException e) {
      throw new ResourceRuntimeException("Failed to get cache statistics sample", e,
          Response.Status.BAD_REQUEST.getStatusCode());
    }
  }
}

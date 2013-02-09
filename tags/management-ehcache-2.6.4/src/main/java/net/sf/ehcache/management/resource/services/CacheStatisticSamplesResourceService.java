/* All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.*/
package net.sf.ehcache.management.resource.services;

import net.sf.ehcache.management.resource.CacheStatisticSampleEntity;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;

/**
 * @author brandony
 */
public interface CacheStatisticSamplesResourceService {
  /**
   *
   * @param info
   * @return
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  Collection<CacheStatisticSampleEntity> getCacheStatisticSamples(@Context UriInfo info);
}

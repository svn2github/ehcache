/* All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.*/
package net.sf.ehcache.management.resource.services;

import net.sf.ehcache.management.resource.CacheManagerEntity;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;

/**
 * <p>
 * A resource service for interacting with ehcache cache managers via the {@link CacheManagerEntity}.
 * </p>
 * 
 * @author brandony
 * 
 */
public interface CacheManagersResourceService {
  public final static String ATTR_QUERY_KEY = "show";

  /**
   * <p>
   * Get a {@code Collection} of {@link CacheManagerEntity} objects representing the cache manager information provided
   * by the associated monitorable entity's agent given the request path.
   * </p>
   * 
   * @param {@link UriInfo} for this resource request
   * @return a collection of {@link CacheManagerEntity} objects when successful.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  Collection<CacheManagerEntity> getCacheManagers(@Context UriInfo info);
}

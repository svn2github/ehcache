/* All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.*/
package net.sf.ehcache.management.resource.services;

import java.util.Collection;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import net.sf.ehcache.management.resource.CacheEntity;

/**
 * <p>
 * A resource service for interacting with ehcache caches via the {@link CacheEntity}.
 * </p>
 * 
 * @author brandony
 * 
 */
public interface CachesResourceService {
  public static final String ATTR_QUERY_KEY = "show";

  /**
   * <p>
   * Get a {@code Collection} of {@link CacheEntity} objects representing the cache information provided by the
   * associated monitorable entity's agent given the request path.
   * </p>
   * 
   * @param info {@link UriInfo} for this resource request
   * @return a collection of {@link CacheEntity} objects when successful.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  Collection<CacheEntity> getCaches(@Context UriInfo info);

  /**
   * <p>
   * Create or update a cache with the name specified in the request path, for a specific agent and cache manager. The request
   * path that does not identify a unique cache resource for creation or identifies a cache that already exists will
   * constitute a bad request and will be denied, resulting in a response with a 400 and 409 respectively.
   * </p>
   * 
   * @param info {@link UriInfo} for this resource request
   * @param resource {@code CacheEntity} resource for update or creation
   */
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  void createOrUpdateCache(@Context UriInfo info, CacheEntity resource);

  /**
   * <p>
   * Delete a cache with the name specified in the request path, for a specific agent and cache manager. The request
   * path that does not identify a unique cache resource for deletion will constitute a bad request and will be denied, 
   * resulting in a response with a 400 status.
   * </p>
   * 
   * @param info {@link UriInfo} for this resource request
   */
  @DELETE
  void deleteCache(@Context UriInfo info);

}

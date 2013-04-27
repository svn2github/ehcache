/* All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.*/
package net.sf.ehcache.management.resource.services;

import net.sf.ehcache.management.resource.CacheManagerConfigEntity;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;
import java.util.List;

/**
 * A resource service for interacting with and providing cache manager configurations.
 *
 * @author brandony
 */
public interface CacheManagerConfigsResourceService {

  /**
   * Get a {@code Collection} of {@link CacheManagerConfigEntity} objects representing the cache manager configuration
   * information provided by the associated monitorable entity's agent given the request path.
   *
   *
   * @param {@link UriInfo} for this resource request
   * @return a {@link Response} whose content includes a collection of {@link CacheManagerConfigEntity} objects
   */
  @GET
  @Produces(MediaType.APPLICATION_XML)
  public Collection<CacheManagerConfigEntity> getXMLCacheManagerConfigs(@Context UriInfo info);
}

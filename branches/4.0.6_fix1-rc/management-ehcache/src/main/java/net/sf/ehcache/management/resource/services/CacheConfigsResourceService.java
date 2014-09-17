/* All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.*/
package net.sf.ehcache.management.resource.services;

import net.sf.ehcache.management.resource.CacheConfigEntity;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;
import java.util.List;

/**
 * @author brandony
 */
public interface CacheConfigsResourceService {
  /**
   * Get a {@code Collection} of {@link net.sf.ehcache.management.resource.CacheConfigEntity} objects representing the
   * cache manager configuration information provided by the associated monitorable entity's agent given the request path.
   *
   *
   * @param {@link UriInfo} for this resource request
   * @return a collection of
   * {@link net.sf.ehcache.management.resource.CacheConfigEntity} objects
   */
  @GET
  @Produces(MediaType.APPLICATION_XML)
  public Collection<CacheConfigEntity> getXMLCacheConfigs(@Context UriInfo info);
}

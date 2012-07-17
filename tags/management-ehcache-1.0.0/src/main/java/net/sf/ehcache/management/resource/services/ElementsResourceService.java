/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package net.sf.ehcache.management.resource.services;

import javax.ws.rs.DELETE;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

/**
 * A resource service interface for implementations interacting with cache elements.
 *
 * @author brandony
 */
public interface ElementsResourceService {

  /**
   * Remove elements from the cache.
   *
   *  @param info for this resource request
   */
  @DELETE
  void deleteElements(@Context UriInfo info);
}

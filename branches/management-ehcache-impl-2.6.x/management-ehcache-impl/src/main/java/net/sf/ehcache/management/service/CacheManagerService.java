/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package net.sf.ehcache.management.service;

import net.sf.ehcache.management.resource.CacheManagerEntity;
import org.terracotta.management.ServiceExecutionException;

/**
 * An interface for service implementations providing operations on {@link CacheManager} objects.
 *
 * @author brandony
 */
public interface CacheManagerService {

  /**
   * Update a cache manager represented by the submitted entity.
   *
   * @param cacheManagerName the name of the {@link CacheManager} to be updated
   * @param resource         the representation of the resource informing this update
   * @throws ServiceExecutionException if the update fails
   */
  void updateCacheManager(String cacheManagerName,
                           CacheManagerEntity resource) throws ServiceExecutionException;
}

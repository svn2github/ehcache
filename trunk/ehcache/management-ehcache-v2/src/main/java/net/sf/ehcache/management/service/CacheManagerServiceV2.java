/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package net.sf.ehcache.management.service;

import java.util.Collection;

import net.sf.ehcache.management.resource.CacheManagerEntityV2;
import net.sf.ehcache.management.resource.QueryResultsEntityV2;

import org.terracotta.management.ServiceExecutionException;

/**
 * An interface for service implementations providing operations on {@link CacheManager} objects.
 *
 * @author brandony
 */
public interface CacheManagerServiceV2 {

  /**
   * Update a cache manager represented by the submitted entity.
   *
   * @param cacheManagerName the name of the {@link CacheManager} to be updated
   * @param resource         the representation of the resource informing this update
   * @throws ServiceExecutionException if the update fails
   */
  void updateCacheManager(String cacheManagerName,
                           CacheManagerEntityV2 resource) throws ServiceExecutionException;
  
  /**
   * Execute query against cache manager and returns results array
   * 
   * @param queryString
   * @return
   * @throws ServiceExecutionException
   */
  Collection<QueryResultsEntityV2> executeQuery(String cacheManagerName, String queryString) throws ServiceExecutionException;
}

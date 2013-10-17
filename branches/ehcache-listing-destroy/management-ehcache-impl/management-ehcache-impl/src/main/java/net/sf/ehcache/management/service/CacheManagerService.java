/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package net.sf.ehcache.management.service;

import java.util.Collection;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.management.resource.CacheManagerEntity;
import net.sf.ehcache.management.resource.QueryResultsEntity;

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
  
  /**
   * Execute query against cache manager and returns results array
   * 
   * @param queryString
   * @return
   * @throws ServiceExecutionException
   */
  Collection<QueryResultsEntity> executeQuery(String cacheManagerName, String queryString) throws ServiceExecutionException;
}

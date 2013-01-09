package net.sf.ehcache.management.service;

import net.sf.ehcache.management.resource.CacheEntity;

import org.terracotta.management.ServiceExecutionException;

/**
 * An interface for service implementations providing operations on {@link Cache} objects.
 *
 * @author brandony
 */
public interface CacheService {

  /**
   * Create or update a cache represented by the submitted entity.
   *
   * @param cacheManagerName the name of the {@link CacheManager} managing the {@link Cache} to be updated
   * @param cacheName        the name of the {@link Cache} to be updated
   * @param resource         the representation of the resource informing this update
   * @throws ServiceExecutionException if the update fails
   */
  void createOrUpdateCache(String cacheManagerName,
                           String cacheName,
                           CacheEntity resource) throws ServiceExecutionException;

  /**
   * Clears all the elements in the cache.
   *
   * @param cacheManagerName the name of the {@link CacheManager} managing the {@link Cache} to be cleared
   * @param cacheName        the name of the {@link Cache} to be cleared
   */
  void clearCache(String cacheManagerName,
                  String cacheName) throws ServiceExecutionException;
}

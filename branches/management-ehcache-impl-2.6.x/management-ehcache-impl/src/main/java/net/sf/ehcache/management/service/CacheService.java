package net.sf.ehcache.management.service;

import net.sf.ehcache.management.resource.CacheEntity;

/**
 * @author brandony
 */
public interface CacheService {
  interface Locator {
    CacheService locateCacheService();
  }

  /**
   * Clears the stats for the specified cache.
   *
   * @param cacheManagerName the name of a registered {@link net.sf.ehcache.CacheManager}
   * @param cacheName        the name of a registered {@link net.sf.ehcache.Cache}
   */
  void clearCacheStats(String cacheManagerName,
                       String cacheName);

  /**
   * Create or update a cache represented by the submitted entity.
   *
   * @param cacheManagerName the name of the cache manager for the cache to update
   * @param cacheName the name of the cache to update
   * @param resource the representation of the resource informing this update
   */
  void createOrUpdateCache(String cacheManagerName,
                           String cacheName,
                           CacheEntity resource);

  /**
   * Clears all the elements in the cache.
   *
   * @param cacheManagerName
   * @param cacheName
   */
  void clearCache(String cacheManagerName,
                  String cacheName);
}

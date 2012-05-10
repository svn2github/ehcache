package net.sf.ehcache.management.services;

import net.sf.ehcache.CacheManager;

/**
 * A service for registering {@link CacheManager}s for sampling.
 *
 * @author brandony
 */
public interface SamplerRepositoryService {
  /**
   * Register a {@link CacheManager} for sampling.
   *
   * @param cacheManager to register
   */
  void register(CacheManager cacheManager);

  void unregister(CacheManager cacheManager);

  boolean hasRegistered();
}

package net.sf.ehcache.management.service;

import net.sf.ehcache.CacheManager;

/**
 * A interface for services registering {@link CacheManager} objects for sampling.
 *
 * @author brandony
 */
public interface SamplerRepositoryService {

  /**
   * Register this service as an MBean, if not done already.
   *
   * @param clientUUID the UUID to identify this service
   */
  void registerMBean(String clientUUID);

  /**
   * Register a {@link CacheManager} for sampling.
   *
   * @param cacheManager to register
   */
  void register(CacheManager cacheManager);

  /**
   * Unregister a {@link CacheManager} for sampling.
   *
   * @param cacheManager to register
   */
  void unregister(CacheManager cacheManager);

  /**
   * An indicator as to whether or not any {@link CacheManager} objects have been registered.
   *
   * @return {@code true} if an object has been registered, {@code false} otherwise
   */
  boolean hasRegistered();

  /**
   * Dispose of the repository service mbean
   */
  void dispose();
}

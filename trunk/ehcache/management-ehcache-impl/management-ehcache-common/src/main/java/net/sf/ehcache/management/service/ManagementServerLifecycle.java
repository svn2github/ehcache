/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache.management.service;

import net.sf.ehcache.CacheManager;

/**
 * A interface for services registering {@link CacheManager} objects for sampling.
 *
 * @author brandony
 */
public interface ManagementServerLifecycle {

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

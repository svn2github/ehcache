/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Status;
import net.sf.ehcache.event.CacheManagerEventListener;
import net.sf.ehcache.terracotta.InternalEhcache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LocalVMResources {

  private static final Logger LOGGER = LoggerFactory.getLogger(LocalVMResources.class);

  public static LocalVMResources getInstance() {
    return LocalVMResourcesHolder.INSTANCE;
  }

  private static final class LocalVMResourcesHolder {
    private static final LocalVMResources INSTANCE = new LocalVMResources();
  }

  private final ConcurrentMap<String, Object> resources = new ConcurrentHashMap<String, Object>();

  private LocalVMResources() {
    // private constructor
  }

  /**
   * Registers a cacheManager and returns previous object mapped to uuid or null
   */
  public Object registerCacheManager(String uuid, CacheManager cacheManager) {
    LOGGER.debug("Registering CacheManager with uuid: " + uuid + ", cacheManager: " + cacheManager);
    CacheManagerDisposalListener cacheManagerEventListener = new CacheManagerDisposalListener(uuid);
    cacheManager.getCacheManagerEventListenerRegistry().registerListener(cacheManagerEventListener);
    resources.put(CacheManagerDisposalListener.class.getName() + "_" + uuid, cacheManagerEventListener);
    return resources.put(uuid, cacheManager);
  }

  /**
   * Return the cacheManager registered with specified param otherwise throws exception if not present
   */
  public CacheManager getRegisteredCacheManager(String cacheManagerClusterUUID) {
    Object object = resources.get(cacheManagerClusterUUID);
    if (object instanceof CacheManager) {
      return (CacheManager) object;
    } else {
      throw new CacheException("Expected a cacheManager to be registered with uuid: " + cacheManagerClusterUUID
                               + ", but was mapped to (className: "
                               + (object == null ? "null" : object.getClass().getName()) + "): " + object);
    }
  }

  /**
   * Registers a cache and returns previous object mapped to its name if present or null
   */
  public Object registerCache(Ehcache cache) {
    LOGGER.debug("Registering Cache with name: " + cache.getName());
    return resources.put(getFQN(cache.getCacheManager(), cache.getName()), cache);
  }

  private String getFQN(CacheManager cacheManager, String cacheName) {
    String cmName = cacheManager.isNamed() ? cacheManager.getName() : "__DEFAULT_CM_PREFIX__";
    return cmName + "_" + cacheName;
  }

  /**
   * Unregisters a cache
   */
  public void unregisterCache(Ehcache cache) {
    LOGGER.debug("Unregistering Cache with name: " + cache.getName());
    if (getRegisteredCache(cache.getCacheManager(), cache.getName()) != null) {
      resources.remove(getFQN(cache.getCacheManager(), cache.getName()));
    }
  }

  /**
   * Unregisters a cacheManager
   */
  public void unregisterCacheManager(String cacheManagerClusterUUID) {
    LOGGER.debug("Unregistering CacheManager with uuid: " + cacheManagerClusterUUID);
    CacheManager cacheManager = (CacheManager) resources.remove(cacheManagerClusterUUID);
    CacheManagerDisposalListener listener = (CacheManagerDisposalListener) resources
        .remove(CacheManagerDisposalListener.class.getName() + "_" + cacheManagerClusterUUID);
    if (cacheManager != null) {
      cacheManager.getCacheManagerEventListenerRegistry().unregisterListener(listener);
    }
  }

  /**
   * Return the cache registered with specified param otherwise returns null. If a different resource other than a cache
   * is registered with the name, an exception will be thrown
   */
  public InternalEhcache getRegisteredCache(CacheManager cacheManager, String cacheName) {
    String cacheFQN = getFQN(cacheManager, cacheName);
    Object object = resources.get(cacheFQN);
    if (object == null) {
      return null;
    } else {
      if (object instanceof InternalEhcache) {
        return (InternalEhcache) object;
      } else {
        throw new CacheException("Expected a cache to be registered with uuid: " + cacheFQN
                                 + ", but was mapped to (className: " + object.getClass().getName() + "): " + object);
      }
    }
  }

  /**
   * Used in tests
   */
  ConcurrentMap<String, Object> getRegisteredResources() {
    return resources;
  }

  private class CacheManagerDisposalListener implements CacheManagerEventListener {
    private final String uuid;

    private CacheManagerDisposalListener(final String uuid) {
      this.uuid = uuid;
    }

    public void init() throws CacheException {
      // nothing to initialize
    }

    public Status getStatus() {
      return null;
    }

    public void dispose() throws CacheException {
      unregisterCacheManager(uuid);
    }

    public void notifyCacheAdded(String cacheName) {
      // nothing to do
    }

    public void notifyCacheRemoved(String cacheName) {
      // nothing to do
    }

    @Override
    public boolean equals(Object o) {
      if (o == null) { return false; }
      if (o.getClass() != getClass()) { return false; }
      return uuid.equals(((CacheManagerDisposalListener) o).uuid);
    }
  }
}

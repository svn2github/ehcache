/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.wan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;

import java.io.Serializable;
import java.util.concurrent.ConcurrentMap;

public class WANUtil {
  private static final Logger          LOGGER                  = LoggerFactory.getLogger(WANUtil.class);
  private static final String          WAN_PREFIX              = "__WAN__";
  private static final String          LOCK_PREFIX             = WAN_PREFIX + "LOCK";
  private static final String          WAN_CONFIG_MAP_NAME     = WAN_PREFIX + "CONFIG_MAP";
  private static final String          WAN_ENABLED_CACHE_ENTRY = WAN_PREFIX + "ENABLED_CACHE";

  private final ToolkitInstanceFactory factory;

  public WANUtil(ToolkitInstanceFactory factory) {
    this.factory = factory;
  }

  /**
   * This method marks the Orchestrator running for the given cacheManager
   * 
   * @param cacheManagerName name of the CacheManager
   */
  public void markOrchestratorRunning(String cacheManagerName) {
    getWanConfigMap().put(cacheManagerName, Boolean.TRUE);
    notifyClients(cacheManagerName);
  }

  /**
   * This method is used to check whether the Orchestrator has started or not.
   * 
   * @param cacheManagerName
   * @return <code>true</code> if Orchestrator has started else <code>false</code>.
   */
  public boolean isOrchestratorRunning(String cacheManagerName) {
    Boolean value = getWanConfigMap().get(cacheManagerName);
    return (value == null) ? false : value;
  }

  /**
   * This method is used to wait until the Orchestrator is running.
   * 
   * @param cacheManagerName
   */
  public void waitForOrchestrator(String cacheManagerName) {
    if (!isOrchestratorRunning(cacheManagerName)) {
      ToolkitLock toolkitLock = factory.getToolkit().getLock(LOCK_PREFIX + cacheManagerName);
      toolkitLock.lock();
      try {
        while (!isOrchestratorRunning(cacheManagerName)) {
          LOGGER.info("Waiting for the Orchestrator...");
          try {
            toolkitLock.getCondition().await();
          } catch (InterruptedException e) {
            LOGGER.warn("Interrupted while waiting for the Orchestrator to be running.", e);
          }
        }
      } finally {
        toolkitLock.unlock();
      }
    }

    LOGGER.info("Orchestrator is available for the CacheManager '{}'", cacheManagerName);
  }

  /**
   * This method is used by Orchestrator to mark the cache as wan-enabled.
   * 
   * @param cacheManagerName
   * @param cacheName
   * @throws IllegalConfigurationException if the cache is already marked as wan disabled
   */
  public void markCacheWanEnabled(String cacheManagerName, String cacheName) {
    ConcurrentMap<String, Serializable> cacheConfigMap = getCacheConfigMap(cacheManagerName, cacheName);
    Boolean existingValue = (Boolean) cacheConfigMap.putIfAbsent(WAN_ENABLED_CACHE_ENTRY, Boolean.TRUE);
    if ((existingValue != null) && (existingValue.equals(Boolean.FALSE))) {
      throw new IllegalConfigurationException("Cache '" + cacheName + "' is already marked as disabled for WAN");
    }
    LOGGER.info("Marked the cache '{}' wan enabled for CacheManager '{}'", cacheName, cacheManagerName);
  }

  /**
   * This method is used by Client to mark the cache as wan-disabled.
   * 
   * @param cacheName
   * @throws IllegalConfigurationException if the cache is already marked as wan enabled
   */
  public void markCacheWanDisabled(String cacheManagerName, String cacheName) {
    ConcurrentMap<String, Serializable> cacheConfigMap = getCacheConfigMap(cacheManagerName, cacheName);
    Boolean existingValue = (Boolean) cacheConfigMap.putIfAbsent(WAN_ENABLED_CACHE_ENTRY, Boolean.FALSE);
    if ((existingValue != null) && (existingValue.equals(Boolean.TRUE))) {
      throw new IllegalConfigurationException("Cache '" + cacheName + "' is already marked as enabled for WAN");
    }
    LOGGER.info("Marked the cache '{}' wan disabled for CacheManager '{}'", cacheName, cacheManagerName);
  }

  /**
   * This method returns true if the cache is wan-enabled else false. This method should only be used by Client and not
   * by the Orchestrator.
   * 
   * @param cacheManagerName name of the CacheManager
   * @param cacheName name of the Cache
   */
  public boolean isWanEnabledCache(String cacheManagerName, String cacheName) {
    if (cacheName == null || cacheManagerName == null) {
      throw new IllegalArgumentException("Invalid arguments: CacheManagerName- " + cacheManagerName
                                         + " and CacheName- " + cacheName);
    }

    Boolean value = (Boolean) getCacheConfigMap(cacheManagerName, cacheName).get(WAN_ENABLED_CACHE_ENTRY);
    return (value == null) ? false : value;
  }

  void notifyClients(String cacheManagerName) {
    ToolkitLock toolkitLock = factory.getToolkit().getLock(LOCK_PREFIX + cacheManagerName);
    toolkitLock.lock();
    try {
      toolkitLock.getCondition().signalAll();
    } finally {
      toolkitLock.unlock();
    }
  }

  ConcurrentMap<String, Serializable> getCacheConfigMap(String cacheManagerName, String cacheName) {
    return factory.getOrCreateClusteredStoreConfigMap(cacheManagerName, cacheName);
  }

  ConcurrentMap<String, Boolean> getWanConfigMap() {
    return factory.getToolkit().getMap(WAN_CONFIG_MAP_NAME, String.class, Boolean.class);
  }

}

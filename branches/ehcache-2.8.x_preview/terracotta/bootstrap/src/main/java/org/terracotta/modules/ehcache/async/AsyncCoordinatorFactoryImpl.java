/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.async;

import net.sf.ehcache.Ehcache;

import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.modules.ehcache.async.AsyncCoordinatorImpl.Callback;
import org.terracotta.toolkit.collections.ToolkitMap;

import java.util.HashMap;
import java.util.Map;

public class AsyncCoordinatorFactoryImpl implements AsyncCoordinatorFactory {
  private static final String DELIMITER = "|";
  private final ToolkitInstanceFactory                          toolkitInstanceFactory;
  private final Map<String, AsyncCoordinatorImpl> localMap;

  public AsyncCoordinatorFactoryImpl(ToolkitInstanceFactory toolkitInstanceFactory) {
    this.toolkitInstanceFactory = toolkitInstanceFactory;
    this.localMap = new HashMap<String, AsyncCoordinatorImpl>();
  }

  private static String getFullAsyncName(String cacheManagerName, String cacheName) {
    return cacheManagerName + DELIMITER + cacheName;
  }

  @Override
  public synchronized AsyncCoordinator getOrCreateAsyncCoordinator(final Ehcache cache, final AsyncConfig config) {
    return getOrCreateAsyncCoordinator(cache.getCacheManager().getName(), cache.getName(), config);
  }

  private synchronized AsyncCoordinator getOrCreateAsyncCoordinator(String cacheManagerName, String cacheName, AsyncConfig config) {
    final String fullAsyncName = getFullAsyncName(cacheManagerName, cacheName); // contains CM name and Cache name
    final ToolkitMap<String, AsyncConfig> configMap = toolkitInstanceFactory.getOrCreateAsyncConfigMap();
    AsyncConfig oldConfig = configMap.putIfAbsent(fullAsyncName, config);
    if (oldConfig != null && !oldConfig.equals(config)) { throw new IllegalArgumentException(
        "can not get AsyncCoordinator "
        + fullAsyncName
        + " for same name but different configs.\nExisting config\n"
        + oldConfig
        + "\nNew Config\n"
        + config); }
    AsyncCoordinatorImpl async = localMap.get(fullAsyncName);
    if (async != null) {
      if (oldConfig == null) { throw new IllegalArgumentException(
          "AsyncCoordinator "
          + fullAsyncName
          + " created for this node but entry not present in configMap"); }
    } else {
      Callback stopCallable = new Callback() {
        @Override
        public void callback() {
          synchronized (AsyncCoordinatorFactoryImpl.this) {
            localMap.remove(fullAsyncName);
          }
        }
      };
      async = new AsyncCoordinatorImpl(fullAsyncName, config, toolkitInstanceFactory, stopCallable);
      localMap.put(fullAsyncName, async);
    }
    return async;
  }

  public boolean destroy(final String cacheManagerName, final String cacheName) {
    AsyncConfig config = toolkitInstanceFactory.getOrCreateAsyncConfigMap().get(getFullAsyncName(cacheManagerName, cacheName));
    if (config != null) {
      getOrCreateAsyncCoordinator(cacheManagerName, cacheName, config).destroy();
      toolkitInstanceFactory.getOrCreateAsyncConfigMap().remove(getFullAsyncName(cacheManagerName, cacheName));
      return true;
    } else {
      return false;
    }
  }
}

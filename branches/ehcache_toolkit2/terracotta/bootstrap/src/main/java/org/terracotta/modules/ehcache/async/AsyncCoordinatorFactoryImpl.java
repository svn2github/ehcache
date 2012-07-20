/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.async;

import net.sf.ehcache.Ehcache;

import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.modules.ehcache.async.AsyncCoordinatorImpl.Callback;
import org.terracotta.toolkit.cache.ToolkitCache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AsyncCoordinatorFactoryImpl implements AsyncCoordinatorFactory {
  private final ToolkitInstanceFactory                          toolkitInstanceFactory;
  private final ConcurrentHashMap<String, AsyncCoordinatorImpl> localMap;

  public AsyncCoordinatorFactoryImpl(ToolkitInstanceFactory toolkitInstanceFactory) {
    this.toolkitInstanceFactory = toolkitInstanceFactory;
    this.localMap = new ConcurrentHashMap<String, AsyncCoordinatorImpl>();
  }

  @Override
  public AsyncCoordinator getOrCreateAsyncCoordinator(final Ehcache cache, final AsyncConfig config) {
    final String fullAsyncName = toolkitInstanceFactory.getFullAsyncName(cache);
    final ToolkitCache<String, AsyncConfig> configMap = toolkitInstanceFactory.getOrCreateAsyncConfigMap();
    Lock localMapLock = new ReentrantLock();
    localMapLock.lock();
    try {
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
            localMap.remove(fullAsyncName);
          }
        };
        async = new AsyncCoordinatorImpl(fullAsyncName, config, toolkitInstanceFactory, stopCallable);
        localMap.put(fullAsyncName, async);
      }
      return async;
    } finally {
      localMapLock.unlock();
    }
  }
}

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.async;

import net.sf.ehcache.Ehcache;

import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.modules.ehcache.async.AsyncCoordinatorImpl.StopCallable;
import org.terracotta.toolkit.collections.ToolkitCache;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;

import java.util.concurrent.ConcurrentHashMap;

public class AsyncCoordinatorFactoryImpl implements AsyncCoordinatorFactory {
  private final ToolkitInstanceFactory                      toolkitInstanceFactory;
  private final ConcurrentHashMap<String, AsyncCoordinatorImpl> localMap;

  public AsyncCoordinatorFactoryImpl(ToolkitInstanceFactory toolkitInstanceFactory) {
    this.toolkitInstanceFactory = toolkitInstanceFactory;
    this.localMap = new ConcurrentHashMap<String, AsyncCoordinatorImpl>();
  }

  @Override
  public AsyncCoordinator getOrCreateAsyncCoordinator(final Ehcache cache,
                                                      final AsyncConfig config) {
    final String fullAsyncName = toolkitInstanceFactory.getFullAsyncName(cache);
    final ToolkitCache<String, AsyncConfig> configMap = toolkitInstanceFactory.getOrCreateAsyncConfigMap();
    String nodeId = getCurrentNodeId();
    String asyncNameWithNodeId = toolkitInstanceFactory.getAsyncNode(fullAsyncName, nodeId);
    ToolkitLock toolkitLock = toolkitInstanceFactory.getAsyncWriteLock();
    toolkitLock.lock();
    try {
      AsyncCoordinatorImpl async = localMap.get(fullAsyncName);
      AsyncConfig oldConfig = configMap.putIfAbsent(fullAsyncName, config);
      if (oldConfig != null && !oldConfig.equals(config)) { throw new IllegalArgumentException(
                                                                                               "can not get AsyncCoordinator "
                                                                                                   + fullAsyncName
                                                                                                   + " for same name but different configs.\nExisting config\n"
                                                                                                   + oldConfig
                                                                                                   + "\nNew Config\n"
                                                                                                   + config); }
      if (async != null) {
        if (oldConfig == null) { throw new IllegalArgumentException("AsyncCoordinator " + fullAsyncName
                                                                    + " created for node " + nodeId
                                                                    + " but entry not present in configMap"); }
      } else {
        async = new AsyncCoordinatorImpl(fullAsyncName, asyncNameWithNodeId, config,
                                                                  toolkitInstanceFactory);
        localMap.put(fullAsyncName, async);

        async.registerStopCallable(new StopCallable() {
          @Override
          public void stop() {
            localMap.remove(fullAsyncName);
          }
        });
      }
      return async;
    } finally {
      toolkitLock.unlock();
    }
  }

  private String getCurrentNodeId() {
    return toolkitInstanceFactory.getToolkit().getClusterInfo().getCurrentNode().getId();
  }

}

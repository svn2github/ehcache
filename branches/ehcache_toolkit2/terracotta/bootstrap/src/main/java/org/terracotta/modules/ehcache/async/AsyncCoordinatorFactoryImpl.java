/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.async;

import net.sf.ehcache.Ehcache;

import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;

import java.util.concurrent.ConcurrentHashMap;

public class AsyncCoordinatorFactoryImpl implements AsyncCoordinatorFactory {
  private final ToolkitInstanceFactory                      toolkitInstanceFactory;
  private final ConcurrentHashMap<String, AsyncCoordinator> localMap;

  public AsyncCoordinatorFactoryImpl(ToolkitInstanceFactory toolkitInstanceFactory) {
    this.toolkitInstanceFactory = toolkitInstanceFactory;
    this.localMap = new ConcurrentHashMap<String, AsyncCoordinator>();
  }

  public AsyncCoordinator getOrCreateAsyncCoordinator(final String asyncName, final Ehcache cache,
                                                      final AsyncConfig config) {
    final String fullAsyncName = toolkitInstanceFactory.getFullAsyncName(cache, asyncName);
    final ToolkitMap<String, AsyncConfig> configMap = toolkitInstanceFactory.getOrCreateAsyncConfigMap();
    String nodeId = getCurrentNodeId();
    String asyncNode = toolkitInstanceFactory.getAsyncNode(fullAsyncName, nodeId);
    ToolkitLock toolkitLock = toolkitInstanceFactory.getAsyncWriteLock();
    AsyncCoordinator async = localMap.get(fullAsyncName);
    toolkitLock.lock();
    try {
      if (async != null) {
        AsyncConfig oldConfig = configMap.putIfAbsent(fullAsyncName, config);
        if (oldConfig != null && !oldConfig.equals(config)) { throw new IllegalArgumentException(
                                                                                                 "can not get AsyncCoordinator for same name but different configs.\nExisting config\n"
                                                                                                     + oldConfig
                                                                                                     + "\nNew Config\n"
                                                                                                     + config); }
        async = new AsyncCoordinatorImpl(fullAsyncName, asyncNode, config, toolkitInstanceFactory);
        localMap.put(fullAsyncName, async);
      }
      return async;
    } finally {
      toolkitLock.unlock();
    }
  }

  private String getCurrentNodeId() {
    return toolkitInstanceFactory.getToolkit().getClusterInfo().waitUntilNodeJoinsCluster().getId();
  }

}

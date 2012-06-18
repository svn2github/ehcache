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

  @Override
  public AsyncCoordinator getOrCreateAsyncCoordinator(final String asyncName, final Ehcache cache,
                                                      final AsyncConfig config) {
    final String fullAsyncName = toolkitInstanceFactory.getFullAsyncName(cache, asyncName);
    final ToolkitMap<String, AsyncConfig> configMap = toolkitInstanceFactory.getOrCreateAsyncConfigMap();
    String nodeId = getCurrentNodeId();
    String asyncNameWithNodeId = toolkitInstanceFactory.getAsyncNode(fullAsyncName, nodeId);
    ToolkitLock toolkitLock = toolkitInstanceFactory.getAsyncWriteLock();
    AsyncCoordinator async = localMap.get(fullAsyncName);
    toolkitLock.lock();
    try {
      AsyncConfig oldConfig = configMap.putIfAbsent(fullAsyncName, config);
      if (oldConfig != null && !oldConfig.equals(config)) { throw new IllegalArgumentException(
                                                                                               "can not get AsyncCoordinator "
                                                                                                   + asyncName
                                                                                                   + " for same name but different configs.\nExisting config\n"
                                                                                                   + oldConfig
                                                                                                   + "\nNew Config\n"
                                                                                                   + config); }
      if (async != null) {
        if (oldConfig == null) { throw new IllegalArgumentException("AsyncCoordinator " + asyncName
                                                                    + " created for node " + nodeId
                                                                    + " but entry not present in configMap"); }
      } else {
        async = new AsyncCoordinatorImpl(fullAsyncName, asyncNameWithNodeId, config, toolkitInstanceFactory);
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

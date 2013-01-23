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
  private final ToolkitInstanceFactory                          toolkitInstanceFactory;
  private final Map<String, AsyncCoordinatorImpl> localMap;

  public AsyncCoordinatorFactoryImpl(ToolkitInstanceFactory toolkitInstanceFactory) {
    this.toolkitInstanceFactory = toolkitInstanceFactory;
    this.localMap = new HashMap<String, AsyncCoordinatorImpl>();
  }

  @Override
  public synchronized AsyncCoordinator getOrCreateAsyncCoordinator(final Ehcache cache, final AsyncConfig config) {
    final String fullAsyncName = toolkitInstanceFactory.getFullAsyncName(cache); // contains CM name and Cache name
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
}

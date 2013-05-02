/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store.bulkload;

import net.sf.ehcache.constructs.nonstop.NonStopCacheException;

import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.cluster.ClusterInfo;
import org.terracotta.toolkit.internal.ToolkitInternal;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Abhishek Sanoujam
 */
public class BulkLoadShutdownHook implements Runnable {
  private final Set<BulkLoadToolkitCache> registeredCaches = Collections.newSetFromMap(new ConcurrentHashMap());
  private final ClusterInfo                    terracottaClusterInfo;
  private final ToolkitInternal           toolkit;
  private boolean                         init             = false;

  public BulkLoadShutdownHook(ToolkitInternal toolkit) {
    this.toolkit = toolkit;
    terracottaClusterInfo = toolkit.getClusterInfo();
  }

  public synchronized void init() {
    if (init) { return; }

    toolkit.registerBeforeShutdownHook(new Runnable() {
      @Override
      public void run() {
        shutdownRegisteredCaches();
      }
    });
    init = true;
  }

  @Override
  public void run() {
    shutdownRegisteredCaches();
  }

  private synchronized void shutdownRegisteredCaches() {
    if (registeredCaches.size() != 0 && terracottaClusterInfo.areOperationsEnabled()) {
      for (BulkLoadToolkitCache cache : registeredCaches) {
        try {
          if (cache.isBulkLoadEnabledInCurrentNode()) {
            cache.setBulkLoadEnabledInCurrentNode(false);
          }
        } catch (NonStopCacheException e) {
          //
        }
      }
    }
  }

  /**
   * Registers the cache for shutdown hook. When the node shuts down, if the cache is in incoherent mode, it will set
   * the cache back to coherent mode. Setting the node back to coherent mode will flush any changes that were made while
   * in incoherent mode.
   */
  public synchronized void registerCache(BulkLoadToolkitCache cache) {
    registeredCaches.add(cache);
  }

  /**
   * Unregisters the cache from shutdown hook.
   */
  public synchronized void unregisterCache(ToolkitCache cache) {
    registeredCaches.remove(cache);
  }

}

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store.bulkload;

import net.sf.ehcache.constructs.nonstop.NonStopCacheException;

import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.cluster.ClusterInfo;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.ToolkitLogger;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Abhishek Sanoujam
 */
public class BulkLoadShutdownHook implements Runnable {

  private final ToolkitLogger                  logger;
  private final Set<BulkLoadToolkitCache>      registeredCaches = new HashSet<BulkLoadToolkitCache>();
  private final ClusterInfo                    terracottaClusterInfo;
  private final static boolean                 DEBUG            = false;

  public BulkLoadShutdownHook(ToolkitInternal toolkit) {
    terracottaClusterInfo = toolkit.getClusterInfo();
    logger = toolkit.getLogger(BulkLoadShutdownHook.class.getName());
    toolkit.registerBeforeShutdownHook(new Runnable() {
      @Override
      public void run() {
        shutdownRegisteredCaches();
      }
    });
  }

  @Override
  public void run() {
    shutdownRegisteredCaches();
  }

  private synchronized void shutdownRegisteredCaches() {
    debug("Shutting down registered ehcaches...");
    if (terracottaClusterInfo.areOperationsEnabled()) {
      for (BulkLoadToolkitCache cache : registeredCaches) {
        try {
          if (cache.isBulkLoadEnabledInCurrentNode()) {
            debug("Turning Off Bulk Load: " + cache.getName());
            cache.setBulkLoadEnabledInCurrentNode(false);
          }
        } catch (NonStopCacheException e) {
          debug("NonStopCacheException ignored, probably L2 is not reachable anymore - " + e.getMessage());
        }
      }
    }
    debug("Completed shutting down ehcaches.");
  }

  /**
   * Registers the cache for shutdown hook. When the node shuts down, if the cache is in incoherent mode, it will set
   * the cache back to coherent mode. Setting the node back to coherent mode will flush any changes that were made while
   * in incoherent mode.
   */
  public synchronized void registerCache(BulkLoadToolkitCache cache) {
    if (registeredCaches.add(cache)) {
      debug("Registered cache for shutdown hook: " + cache.getName());
    }
  }

  /**
   * Unregisters the cache from shutdown hook.
   */
  public synchronized void unregisterCache(ToolkitCache cache) {
    if (registeredCaches.remove(cache)) {
      debug("Unregistered cache from shutdown hook: " + cache.getName());
    }
  }

  private void debug(String msg) {
    if (DEBUG) logger.info(msg);
  }

}

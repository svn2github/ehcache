/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.coherence;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.constructs.nonstop.NonStopCacheException;

import org.terracotta.api.Terracotta;
import org.terracotta.cluster.ClusterLogger;
import org.terracotta.cluster.TerracottaClusterInfo;
import org.terracotta.cluster.TerracottaLogger;
import org.terracotta.cluster.TerracottaProperties;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Abhishek Sanoujam
 */
public class CacheShutdownHook {

  private static final ClusterLogger    LOGGER           = new TerracottaLogger(CacheShutdownHook.class.getName());
  private static final boolean          DEBUG            = new TerracottaProperties()
                                                             .getBoolean(CacheCoherence.LOGGING_ENABLED_PROPERTY, false);
  public static final CacheShutdownHook INSTANCE         = new CacheShutdownHook();

  private final Set<Ehcache>            registeredCaches = new HashSet<Ehcache>();
  private boolean                       shutdown         = false;
  private TerracottaClusterInfo         terracottaClusterInfo;

  // private constructor
  private CacheShutdownHook() {
    //
  }

  public synchronized void init() {
    Terracotta.registerBeforeShutdownHook(new Runnable() {
      public void run() {
        shutdownRegisteredCaches();
      }
    });
    if (terracottaClusterInfo == null) {
      terracottaClusterInfo = new TerracottaClusterInfo();
    }
  }

  private synchronized void shutdownRegisteredCaches() {
    if (!shutdown) {
      shutdown = true;
      debug("Shutting down registered ehcaches...");
      if (terracottaClusterInfo.areOperationsEnabled()) {
        for (Ehcache cache : registeredCaches) {
          try {
            if (!cache.isNodeCoherent()) {
              debug("Setting cache coherent: " + cache.getName());
              cache.setNodeCoherent(true);
            }
          } catch (NonStopCacheException e) {
            debug("NonStopCacheException ignored, probably L2 is not reachable anymore - " + e.getMessage());
          }
        }
      }
      debug("Completed shutting down ehcaches.");
    } else {
      debug("CacheShutdownHook has already shut down. Ignoring subsequent request.");
    }
  }

  public void shutdown() {
    shutdownRegisteredCaches();
  }

  /**
   * Registers the cache for shutdown hook. When the node shuts down, if the cache is in incoherent mode, it will set
   * the cache back to coherent mode. Setting the node back to coherent mode will flush any changes that were made while
   * in incoherent mode.
   */
  public synchronized void registerCache(Ehcache cache) {
    if (registeredCaches.add(cache)) {
      debug("Registered cache for shutdown hook: " + cache.getName());
    }
  }

  /**
   * Unregisters the cache from shutdown hook.
   */
  public synchronized void unregisterCache(Ehcache cache) {
    if (registeredCaches.remove(cache)) {
      debug("Unregistered cache from shutdown hook: " + cache.getName());
    }
  }

  private void debug(String msg) {
    if (DEBUG) LOGGER.info(msg);
  }

}

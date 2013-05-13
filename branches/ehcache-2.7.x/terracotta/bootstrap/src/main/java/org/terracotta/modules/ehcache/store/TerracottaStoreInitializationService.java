/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.config.NonstopConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.toolkit.cluster.ClusterInfo;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class TerracottaStoreInitializationService {
  private static final Logger   LOGGER = LoggerFactory.getLogger(TerracottaStoreInitializationService.class);
  private final ExecutorService threadPool;
  private final ClusterInfo     clusterInfo;


  public TerracottaStoreInitializationService(ClusterInfo clusterInfo) {
    this.clusterInfo = clusterInfo;
    this.threadPool = getThreadPool();
  }

  /**
   * This method should be called when the associated CacheManager is shutdown. Once shutdowm, the initialization
   * service cannot be started again.
   */
  public void shutdown() {
    threadPool.shutdownNow();
  }

  public void initialize(Runnable runnable, NonstopConfiguration nonStopConfiguration) {

    // Submit the Thread for execution
    Future<?> future = threadPool.submit(runnable);

    // wait for initialization to complete (until operations are enabled)
    waitForInitialization(future, nonStopConfiguration.getTimeoutMillis());
  }

  /**
   * This method holds the calling thread until the given Future returns the result or the Cluster operations get/are
   * disabled. In case the Cluster operations get disabled while waiting, the method call returns honoring the nonstop
   * timeout.
   * 
   */
  private void waitForInitialization(Future<?> future, long nonStopTimeOutInMillis) {
    boolean interrupted = false;
    boolean initializationCompleted = false;
    try {
      do {
        try {
          future.get(nonStopTimeOutInMillis, TimeUnit.MILLISECONDS);
          initializationCompleted = true;
        } catch (InterruptedException e) {
          interrupted = true;
        } catch (ExecutionException e) {
          throw new RuntimeException(e.getCause());
        } catch (TimeoutException e) {
          // Retry if operations are enabled
        }
      } while (!initializationCompleted && areOperationsEnabled());

    } finally {
      if (interrupted) Thread.currentThread().interrupt();
    }

    if (!initializationCompleted) {
      LOGGER.debug("Returning without completing TerracottaStore initialization. Operations Enabled = {}",
                   areOperationsEnabled());
    }
  }

  private boolean areOperationsEnabled() {
    return clusterInfo.areOperationsEnabled();
  }

  private ExecutorService getThreadPool() {
    ThreadFactory daemonThreadFactory = new ThreadFactory() {
      private final AtomicInteger threadID = new AtomicInteger();

      @Override
      public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable, "TerracottaStoreInitializationThread_" + threadID.incrementAndGet());
        thread.setDaemon(true);
        return thread;
      }
    };

    return Executors.newCachedThreadPool(daemonThreadFactory);
  }

}

package org.terracotta.modules.ehcache.wan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.modules.ehcache.WanAwareToolkitCache;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * @author Eugene Shelestovich
 */
public class Watchdog {

  public static final String WATCHDOG_LOCK_NAME = "__WAN__WATCHDOG__LOCK";

  private static final Logger LOGGER = LoggerFactory.getLogger(WANUtil.class);
  private static final long WATCHDOG_INTERVAL = 5000L;
  private static final ThreadFactory DEFAULT_THREAD_FACTORY = new ThreadFactory() {
    @Override
    public Thread newThread(Runnable runnable) {
      Thread thread = new Thread(runnable);
      thread.setName("wan-watchdog");
      thread.setDaemon(true);
      return thread;
    }
  };

  private final Toolkit toolkit;
  private final Set<WanAwareToolkitCache> registry = new CopyOnWriteArraySet<WanAwareToolkitCache>();
  private final ScheduledExecutorService scheduler;

  public static Watchdog create(final Toolkit toolkit) {
    return create(toolkit, Executors.newSingleThreadScheduledExecutor(DEFAULT_THREAD_FACTORY));
  }

  static Watchdog create(final Toolkit toolkit, final ScheduledExecutorService scheduler) {
    final Watchdog dog = new Watchdog(toolkit, scheduler);
    dog.init();
    return dog;
  }

  private Watchdog(final Toolkit toolkit, final ScheduledExecutorService scheduler) {
    this.toolkit = toolkit;
    this.scheduler = scheduler;
  }

  public void watchFor(final WanAwareToolkitCache cache) {
    registry.add(cache);
    LOGGER.debug("Cache '{}' registered with WAN watchdog", cache.getName());
  }

  public void init() {
    scheduler.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        final ToolkitLock lock = toolkit.getLock(WATCHDOG_LOCK_NAME);
        if (lock.tryLock()) {
          try {
            for (final WanAwareToolkitCache cache : registry) {
              if (cache.deactivate()) {
                LOGGER.error("WAN orchestrator is not running. '{}' cache has been deactivated", cache.getName());
              }
            }
          } finally {
            lock.unlock();
          }
        }
      }
    }, 0L, WATCHDOG_INTERVAL, TimeUnit.MILLISECONDS);

    LOGGER.debug("WAN watchdog started");
  }

}

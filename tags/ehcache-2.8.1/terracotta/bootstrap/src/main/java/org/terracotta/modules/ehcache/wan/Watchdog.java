package org.terracotta.modules.ehcache.wan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private final Set<Watchable> registry = new CopyOnWriteArraySet<Watchable>();
  private final ScheduledExecutorService scheduler;

  public static Watchdog create() {
    return create(Executors.newSingleThreadScheduledExecutor(DEFAULT_THREAD_FACTORY));
  }

  static Watchdog create(final ScheduledExecutorService scheduler) {
    final Watchdog dog = new Watchdog(scheduler);
    dog.init();
    return dog;
  }

  private Watchdog(final ScheduledExecutorService scheduler) {
    this.scheduler = scheduler;
  }

  public void watch(final Watchable watchable) {
    registry.add(watchable);
    LOGGER.debug("Watchable cache '{}' registered", watchable.name());
  }

  public void unwatch(final Watchable watchable) {
    registry.remove(watchable);
    LOGGER.debug("Watchable cache '{}' unregistered", watchable.name());
  }

  public void init() {
    scheduler.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        for (final Watchable watchable : registry) {
          watchable.probeLiveness();
        }
      }
    }, 0L, WATCHDOG_INTERVAL, TimeUnit.MILLISECONDS);
    LOGGER.debug("WAN watchdog started");
  }

}

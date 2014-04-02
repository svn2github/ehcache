package org.terracotta.modules.ehcache.wan;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Eugene Shelestovich
 */
public class WatchdogTest {

  private Watchable cache1;
  private Watchable cache2;
  private Watchdog watchdog;
  private final ManuallyTriggeredScheduledExecutor scheduler = new ManuallyTriggeredScheduledExecutor();

  @Before
  public void setUp() throws Exception {
    cache1 = mock(Watchable.class);
    cache2 = mock(Watchable.class);
    watchdog = Watchdog.create(scheduler);
  }

  @Test
  public void testMustProbeAllRegisteredWatchables() {
    watchdog.watch(cache1);
    watchdog.watch(cache2);

    scheduler.trigger();

    verify(cache1).probeLiveness();
    verify(cache2).probeLiveness();
  }

  @Test
  public void testMustNotProbeUnregisteredWatchables() {
    watchdog.watch(cache1);
    watchdog.watch(cache2);
    watchdog.unwatch(cache1);

    scheduler.trigger();

    verify(cache1, never()).probeLiveness();
    verify(cache2).probeLiveness();
  }
}

class ManuallyTriggeredScheduledExecutor extends ScheduledThreadPoolExecutor {
  private Runnable runnable;

  public ManuallyTriggeredScheduledExecutor() {
    super(1);
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay, final long period, final TimeUnit unit) {
    runnable = command;
    return null;
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay, final long delay, final TimeUnit unit) {
    runnable = command;
    return null;
  }

  @Override
  public ScheduledFuture<?> schedule(final Runnable command, final long delay, final TimeUnit unit) {
    runnable = command;
    return null;
  }

  public void trigger() {
    runnable.run();
  }
}


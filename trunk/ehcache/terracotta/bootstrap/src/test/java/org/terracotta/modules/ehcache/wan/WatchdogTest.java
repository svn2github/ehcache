package org.terracotta.modules.ehcache.wan;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.modules.ehcache.WanAwareToolkitCache;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Eugene Shelestovich
 */
public class WatchdogTest {

  private ToolkitLock lock;
  private WanAwareToolkitCache cache1;
  private WanAwareToolkitCache cache2;
  private final ManuallyTriggeredScheduledExecutor scheduler = new ManuallyTriggeredScheduledExecutor();
  private Watchdog watchdog;

  @Before
  public void setUp() throws Exception {
    lock = mock(ToolkitLock.class);
    when(lock.tryLock()).thenReturn(true);
    final Toolkit toolkit = mock(Toolkit.class);
    when(toolkit.getLock(anyString())).thenReturn(lock);
    cache1 = mock(WanAwareToolkitCache.class);
    when(cache1.deactivate()).thenReturn(true);
    cache2 = mock(WanAwareToolkitCache.class);
    when(cache2.deactivate()).thenReturn(true);
    watchdog = Watchdog.create(toolkit, scheduler);
  }

  @Test
  public void testMustDeactivateCacheIfOrchestratorNotRunning() {
    watchdog.watchFor(cache1);
    watchdog.watchFor(cache2);

    scheduler.trigger();

    verify(cache1).deactivate();
    verify(cache2).deactivate();
  }

  @Test
  public void testMustNotDeactivateIfOrchestratorRunning() {
    when(lock.tryLock()).thenReturn(false);

    scheduler.trigger();

    verify(cache1, never()).deactivate();
    verify(cache2, never()).deactivate();
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


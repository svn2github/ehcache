/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package org.terracotta.modules.ehcache;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.util.concurrent.ConcurrentHashMap;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.internal.cache.BufferingToolkitCache;

import java.io.Serializable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Condition;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Eugene Shelestovich
 */
public class WatchableTest {

  private ToolkitLock configLock;
  private ToolkitLock activeLock;
  private WanAwareToolkitCache<String, String> watchable;

  @Before
  public void setUp() throws Exception {
    final ConcurrentMap<String, Serializable> configMap = new ConcurrentHashMap<String, Serializable>();
    final BufferingToolkitCache<String, String> delegate = mock(BufferingToolkitCache.class);
    Condition condition = mock(Condition.class);
    configLock = when(mock(ToolkitLock.class).getCondition()).thenReturn(condition).getMock();
    activeLock = when(mock(ToolkitLock.class).isHeldByCurrentThread()).thenReturn(false, true).getMock();
    watchable = new WanAwareToolkitCache<String, String>(
        delegate, configMap, null, configLock, activeLock, new CacheConfiguration());
  }

  @Test
  public void testMustAcquireLockOnGoLive() {
    when(activeLock.isHeldByCurrentThread()).thenReturn(false, true);
    watchable.goLive();
    verify(activeLock).lock();
  }

  @Test
  public void testMustNotAcquireLockIfAlreadyHeld() {
    when(activeLock.isHeldByCurrentThread()).thenReturn(true);
    watchable.goLive();
    verify(activeLock, never()).lock();
  }

  @Test
  public void testMustDeactivateCacheIfLockReleased() {
    watchable.goLive();
    when(activeLock.tryLock()).thenReturn(true);
    assertFalse(watchable.probeLiveness());
    assertFalse(watchable.isOrchestratorAlive());
    verify(activeLock).unlock();
  }

  @Test
  public void testMustNotDeactivateCacheIfLockHeld() {
    watchable.goLive();
    when(activeLock.tryLock()).thenReturn(false);
    assertTrue(watchable.probeLiveness());
    assertTrue(watchable.isOrchestratorAlive());
    verify(activeLock, never()).unlock();
  }
}

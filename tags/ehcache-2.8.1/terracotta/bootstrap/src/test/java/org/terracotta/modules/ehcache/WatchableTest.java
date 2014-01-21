/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package org.terracotta.modules.ehcache;

import net.sf.ehcache.util.concurrent.ConcurrentHashMap;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.internal.cache.BufferingToolkitCache;

import java.io.Serializable;
import java.util.concurrent.ConcurrentMap;

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

  private ToolkitLock lock;
  private WanAwareToolkitCache<String, String> watchable;

  @Before
  public void setUp() throws Exception {
    final ConcurrentMap<String, Serializable> configMap = new ConcurrentHashMap<String, Serializable>();
    final BufferingToolkitCache<String, String> delegate = mock(BufferingToolkitCache.class);
    lock = mock(ToolkitLock.class);
    watchable = new WanAwareToolkitCache<String, String>(
        delegate, configMap, null, null, lock);
  }

  @Test
  public void testMustAcquireLockOnGoLive() {
    when(lock.isHeldByCurrentThread()).thenReturn(false, true);
    watchable.goLive();
    verify(lock).lock();
  }

  @Test
  public void testMustNotAcquireLockIfAlreadyHeld() {
    when(lock.isHeldByCurrentThread()).thenReturn(true);
    watchable.goLive();
    verify(lock, never()).lock();
  }

  @Test
  public void testMustDeactivateCacheIfLockReleased() {
    when(lock.tryLock()).thenReturn(true);
    assertFalse(watchable.probeLiveness());
    assertFalse(watchable.isActive());
    verify(lock).unlock();
  }

  @Test
  public void testMustNotDeactivateCacheIfLockHeld() {
    when(lock.tryLock()).thenReturn(false);
    assertTrue(watchable.probeLiveness());
    verify(lock, never()).unlock();
  }
}

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.concurrency;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.concurrent.LockType;
import net.sf.ehcache.concurrent.Sync;
import net.sf.ehcache.terracotta.TerracottaNotRunningException;

import org.terracotta.modules.ehcache.store.ValueModeHandler;
import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitLockType;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class TCCacheLockProvider implements CacheLockProvider {

  private final ToolkitCache     backend;
  private final ValueModeHandler valueModeHandler;

  public TCCacheLockProvider(ToolkitCache backend, ValueModeHandler valueModeHandler) {
    this.backend = backend;
    this.valueModeHandler = valueModeHandler;
  }

  @Override
  public Sync getSyncForKey(Object key) {
    ToolkitReadWriteLock lock = createLock(key);
    return new TCSync(lock.writeLock(), lock.readLock());
  }

  private ToolkitReadWriteLock createLock(Object key) {
    try {
      Object portableKey = valueModeHandler.createPortableKey(key);
      return backend.createLockForKey(portableKey);
    } catch (IOException e) {
      throw new CacheException(e);
    }
  }

  private static class TCSync implements Sync {

    private final ToolkitLock writeLock;
    private final ToolkitLock readLock;

    public TCSync(ToolkitLock writeLock, ToolkitLock readLock) {
      this.writeLock = writeLock;
      this.readLock = readLock;
      if (writeLock.getLockType() != ToolkitLockType.WRITE) { throw new AssertionError(); }
      if (readLock.getLockType() != ToolkitLockType.READ) { throw new AssertionError(); }
    }

    private ToolkitLock getLockForType(LockType type) {
      switch (type) {
        case READ:
          return readLock;
        case WRITE:
          return writeLock;
      }
      throw new AssertionError("Unknown lock type - " + type);
    }

    @Override
    public void lock(LockType type) {
      try {
        getLockForType(type).lock();
      } catch (RuntimeException e) {
        handleTCNotRunningException(e);
      }
    }

    @Override
    public boolean tryLock(LockType type, long msec) throws InterruptedException {
      try {
        return getLockForType(type).tryLock(msec, TimeUnit.MILLISECONDS);
      } catch (RuntimeException e) {
        return handleTCNotRunningException(e);
      }

    }

    @Override
    public void unlock(LockType type) {
      try {
        getLockForType(type).unlock();
      } catch (RuntimeException e) {
        handleTCNotRunningException(e);
      }
    }

    @Override
    public boolean isHeldByCurrentThread(LockType type) {
      try {
        return getLockForType(type).isHeldByCurrentThread();
      } catch (RuntimeException e) {
        return handleTCNotRunningException(e);
      }
    }

    private boolean handleTCNotRunningException(RuntimeException e) {
      if (e.getClass().getSimpleName().equals("TCNotRunningException")) { throw new TerracottaNotRunningException(
                                                                                                                  "Clustered Cache is probably shutdown or Terracotta backend is down.",
                                                                                                                  e); }
      throw e;
    }


  }

}

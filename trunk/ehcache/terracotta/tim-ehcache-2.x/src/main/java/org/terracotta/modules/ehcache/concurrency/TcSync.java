package org.terracotta.modules.ehcache.concurrency;

import net.sf.ehcache.concurrent.LockType;
import net.sf.ehcache.concurrent.Sync;

import org.terracotta.locking.ClusteredLock;
import org.terracotta.modules.ehcache.store.ClusteredStore.SyncLockState;

import java.util.concurrent.TimeUnit;

/**
 * @author Alex Snaps
 */
public class TcSync implements Sync {

  private final ClusteredLock              lock;
  private final ThreadLocal<SyncLockState> syncLockState;

  TcSync(ThreadLocal<SyncLockState> synclockstate, final ClusteredLock lock) {
    this.syncLockState = synclockstate;
    this.lock = lock;
  }

  public void lock(final LockType type) {
    switch (type) {
      case READ:
        lock.lock(org.terracotta.locking.LockType.READ);
        break;
      case WRITE:
        lock.lock(org.terracotta.locking.LockType.WRITE);
        break;
      default:
        throw new IllegalArgumentException("Unsupported LockType " + type.name());
    }
    syncLockState.set(syncLockState.get().lockAcquired());
  }

  public boolean tryLock(final LockType type, final long timeout) throws InterruptedException {
    boolean lockAcquired = false;
    try {
      switch (type) {
        case READ:
          lockAcquired = lock.tryLock(org.terracotta.locking.LockType.READ, timeout, TimeUnit.MILLISECONDS);
          break;
        case WRITE:
          lockAcquired = lock.tryLock(org.terracotta.locking.LockType.WRITE, timeout, TimeUnit.MILLISECONDS);
          break;
        default:
          throw new IllegalArgumentException("Unsupported LockType " + type.name());
      }
      return lockAcquired;
    } finally {
      if (lockAcquired) {
        syncLockState.set(syncLockState.get().lockAcquired());
      }
    }
  }

  public void unlock(final LockType type) {
    switch (type) {
      case READ:
        lock.unlock(org.terracotta.locking.LockType.READ);
        break;
      case WRITE:
        lock.unlock(org.terracotta.locking.LockType.WRITE);
        break;
      default:
        throw new IllegalArgumentException("Unsupported LockType " + type.name());
    }
    if (isHeldByCurrentThread(LockType.READ) || isHeldByCurrentThread(LockType.WRITE)) {
      syncLockState.set(syncLockState.get().lockAcquired());
    } else {
      syncLockState.set(syncLockState.get().lockReleased());
    }
  }

  public boolean isHeldByCurrentThread(LockType type) {
    switch (type) {
      case READ:
        return lock.isHeldByCurrentThread(org.terracotta.locking.LockType.READ);
      case WRITE:
        return lock.isHeldByCurrentThread(org.terracotta.locking.LockType.WRITE);
      default:
        throw new IllegalArgumentException("Unsupported LockType " + type.name());
    }
  }

  @Override
  public String toString() {
    return lock.toString();
  }

}

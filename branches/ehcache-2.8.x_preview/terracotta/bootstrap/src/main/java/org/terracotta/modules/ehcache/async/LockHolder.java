/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.async;


import org.terracotta.toolkit.concurrent.locks.ToolkitLock;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class LockHolder {
  private static final int               PARTIES  = 2;
  private final Map<String, CyclicBarrier> holdings = new HashMap<String, CyclicBarrier>();

  public synchronized void hold(final ToolkitLock lock) {
    if (lock == null || holdings.containsKey(lock)) { return; }
    final CyclicBarrier barrier = new CyclicBarrier(PARTIES);
    Thread lockThread = new Thread(new Runnable() {
      @Override
      public void run() {
        lock.lock();
        try {
          await(barrier); // hit 1
          await(barrier); // hit 2
        } finally {
          try {
            lock.unlock();
          } catch (Throwable th) {
            // ignore any exception in unlock so that thread calling release() is not stuck at barrier.await()
          }
          await(barrier); // hit 3
        }
      }
    });
    holdings.put(lock.getName(), barrier);
    lockThread.start();
    await(barrier); // hit 1
  }

  public synchronized void release(ToolkitLock lock) {
    CyclicBarrier barrier = holdings.get(lock.getName());
    if (barrier != null) {
      releaseLock(barrier);
      holdings.remove(lock);
    }
  }

  private void releaseLock(CyclicBarrier barrier) {
    await(barrier); // hit 2
    await(barrier); // hit 3
  }

  public synchronized void reset() {
    for (CyclicBarrier barrier : holdings.values()) {
      releaseLock(barrier);
    }
    holdings.clear();
  }

  private void await(CyclicBarrier barrier) {
    try {
      barrier.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (BrokenBarrierException e) {
      // ignore
    }
  }

}

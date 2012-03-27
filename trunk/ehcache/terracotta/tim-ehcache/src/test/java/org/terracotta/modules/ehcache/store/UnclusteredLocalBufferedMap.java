/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import org.terracotta.modules.ehcache.coherence.CacheCoherence;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class UnclusteredLocalBufferedMap<K, V> extends LocalBufferedMap<K, V> {

  public UnclusteredLocalBufferedMap(ClusteredStoreBackend<Object, Object> clusteredStoreBackend,
                                     CacheCoherence incoherentNodesSet) {
    super(clusteredStoreBackend, incoherentNodesSet);
  }

  @Override
  protected Lock getConcurrentTransactionLock() {
    return new NoOpLock();
  }

  static class NoOpLock implements Lock {

    public void lock() {
      //
    }

    public void lockInterruptibly() {
      //
    }

    public Condition newCondition() {
      throw new UnsupportedOperationException();
    }

    public boolean tryLock() {
      return true;
    }

    public boolean tryLock(long time, TimeUnit unit) {
      return true;
    }

    public void unlock() {
      //
    }

  }
}

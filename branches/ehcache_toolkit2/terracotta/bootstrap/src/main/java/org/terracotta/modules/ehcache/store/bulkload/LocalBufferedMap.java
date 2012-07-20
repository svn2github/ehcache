/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store.bulkload;

import net.sf.ehcache.pool.SizeOfEngine;
import net.sf.ehcache.pool.impl.DefaultSizeOfEngine;

import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.collections.ToolkitCacheInternal;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;
import org.terracotta.toolkit.internal.meta.MetaData;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;

/**
 * @author Abhishek Sanoujam
 */
public class LocalBufferedMap<K, V> {
  private static final int                      MAX_SIZEOF_DEPTH                        = 1000;

  private static final String                   CONCURRENT_TXN_LOCK_ID                  = "local-buffer-static-concurrent-txn-lock-id";

  private static final int                      PUTS_BATCH_BYTE_SIZE                    = BulkLoadConstants
                                                                                            .getBatchedPutsBatchBytes();
  private static final long                     BATCH_TIME_MILLISECS                    = BulkLoadConstants
                                                                                            .getBatchedPutsBatchTimeMillis();
  private static final long                     THROTTLE_PUTS_BYTE_SIZE                 = BulkLoadConstants
                                                                                            .getBatchedPutsThrottlePutsAtByteSize();
  private static final Map                      EMPTY_MAP                               = Collections.EMPTY_MAP;

  private static final int                      LOCAL_MAP_INITIAL_CAPACITY              = 128;
  private static final float                    LOCAL_MAP_LOAD_FACTOR                   = 0.75f;
  private static final int                      LOCAL_MAP_INITIAL_SEGMENTS              = 128;

  protected static final int                    DEFAULT_LOCAL_BUFFER_PUTS_BATCH_SIZE    = 600;
  protected static final int                    DEFAULT_LOCAL_BUFFER_PUTS_THROTTLE_SIZE = 200000;

  private final FlushToServerThread             flushToServerThread;
  private final BulkLoadToolkitCache<K, V>      bulkLoadClusteredCache;
  private final ToolkitCacheInternal<K, V>      backend;

  private volatile Map<K, ValueWithMetaData<V>> collectBuffer;
  private volatile Map<K, ValueWithMetaData<V>> flushBuffer;
  private volatile boolean                      clearMap                                = false;
  private volatile AtomicLong                   pendingOpsSize                          = new AtomicLong();
  private final SizeOfEngine                    sizeOfEngine;
  private volatile MetaData                     clearMetaData;

  private final Lock                            concurrentTransactionLock;

  public LocalBufferedMap(String name, BulkLoadToolkitCache<K, V> bulkLoadClusteredCache,
                          ToolkitCacheInternal<K, V> backend, ToolkitInternal toolkit) {
    this.bulkLoadClusteredCache = bulkLoadClusteredCache;
    this.backend = backend;
    this.collectBuffer = newMap();
    this.flushBuffer = EMPTY_MAP;
    this.concurrentTransactionLock = toolkit.getLock(CONCURRENT_TXN_LOCK_ID, ToolkitLockTypeInternal.CONCURRENT);
    this.flushToServerThread = new FlushToServerThread("BulkLoad Flush Thread [" + name + "]", this);
    flushToServerThread.setDaemon(true);
    sizeOfEngine = new DefaultSizeOfEngine(MAX_SIZEOF_DEPTH, true);
  }

  private Map<K, ValueWithMetaData<V>> newMap() {
    return new ConcurrentHashMap<K, ValueWithMetaData<V>>(LOCAL_MAP_INITIAL_CAPACITY, LOCAL_MAP_LOAD_FACTOR,
                                                          LOCAL_MAP_INITIAL_SEGMENTS);
  }

  // this method is called under read-lock from BulkLoadClusteredCache
  public V get(K key) {
    // get from collectingBuffer or flushBuffer
    ValueWithMetaData<V> v = collectBuffer.get(key);
    if (v != null && v.isRemove()) { return null; }
    if (v != null) { return v.getValue(); }
    v = flushBuffer.get(key);
    if (v != null && v.isRemove()) { return null; }
    return v == null ? null : v.getValue();
  }

  // this method is called under read-lock from BulkLoadClusteredCache
  public V remove(K key, MetaData metaData) {
    RemoveValueWithMetaData removeVWMD = new RemoveValueWithMetaData(metaData);
    ValueWithMetaData<V> old = collectBuffer.put(key, removeVWMD);
    if (old == null) {
      pendingOpsSize.addAndGet(sizeOfEngine.sizeOf(key, removeVWMD, null).getCalculated());
      return null;
    } else {
      return old.isRemove() ? null : old.getValue();
    }
  }

  // this method is called under read-lock from BulkLoadClusteredCache
  public boolean containsKey(K key) {
    ValueWithMetaData<V> v = collectBuffer.get(key);
    if (v != null) { return !v.isRemove(); }
    v = flushBuffer.get(key);
    if (v == null || v.isRemove()) {
      return false;
    } else {
      return true;
    }
  }

  // this method is called under read-lock from BulkLoadClusteredCache
  public int getSize() {
    int size = 0;
    Map<K, ValueWithMetaData<V>> localCollectingMap = collectBuffer;
    Map<K, ValueWithMetaData<V>> localFlushMap = flushBuffer;
    for (Entry<K, ValueWithMetaData<V>> e : localCollectingMap.entrySet()) {
      if (e.getValue() != null && !e.getValue().isRemove()) {
        size++;
      }
    }
    for (Entry<K, ValueWithMetaData<V>> e : localFlushMap.entrySet()) {
      if (e.getValue() != null && !e.getValue().isRemove()) {
        size++;
      }
    }
    return size;
  }

  // this method is called under write-lock from BulkLoadClusteredCache
  public void clear(MetaData metaData) {
    collectBuffer.clear();
    flushBuffer.clear();
    // mark the backend to be cleared
    this.clearMap = true;
    this.clearMetaData = metaData;
    pendingOpsSize.set(0);
  }

  // this method is called under read-lock from BulkLoadClusteredCache
  public Set<K> getKeys() {
    Set<K> keySet = new HashSet<K>(collectBuffer.keySet());
    keySet.addAll(flushBuffer.keySet());
    return keySet;
  }

  public Set<Map.Entry<K, V>> entrySet() {
    Set<Entry<K, V>> rv = new HashSet<Map.Entry<K, V>>();
    addEntriesToSet(rv, collectBuffer);
    addEntriesToSet(rv, flushBuffer);
    return rv;
  }

  private void addEntriesToSet(Set<Entry<K, V>> rv, Map<K, ValueWithMetaData<V>> map) {
    for (Entry<K, ValueWithMetaData<V>> entry : map.entrySet()) {
      final K key = entry.getKey();
      ValueWithMetaData<V> valueWithMetaData = entry.getValue();
      final V value = valueWithMetaData.getValue();
      if (!valueWithMetaData.isRemove()) {
        rv.add(new Map.Entry<K, V>() {

          @Override
          public K getKey() {
            return key;
          }

          @Override
          public V getValue() {
            return value;
          }

          @Override
          public V setValue(V param) {
            throw new UnsupportedOperationException();
          }

        });
      }
    }
  }

  // this method is called under read-lock from BulkLoadClusteredCache
  public V put(K key, V value, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds, MetaData metaData) {
    ValueWithMetaData<V> valueWMD = new ValueWithMetaData(metaData, value, createTimeInSecs, customMaxTTISeconds,
                                                          customMaxTTLSeconds);
    ValueWithMetaData<V> rv = collectBuffer.put(key, valueWMD);
    if (rv == null) {
      // new put
      throttleIfNecessary(pendingOpsSize.addAndGet(sizeOfEngine.sizeOf(key, valueWMD, null).getCalculated()));
    }
    return rv == null ? null : rv.isRemove() ? null : rv.getValue();
  }

  private void startThreadIfNecessary() {
    flushToServerThread.start();
  }

  private void throttleIfNecessary(long currentPendingSize) {
    if (currentPendingSize <= THROTTLE_PUTS_BYTE_SIZE) { return; }
    bulkLoadClusteredCache.releaseLocalReadLock();
    try {
      while (currentPendingSize > THROTTLE_PUTS_BYTE_SIZE) {
        sleepMillis(100);
        currentPendingSize = pendingOpsSize.get();
      }
    } finally {
      bulkLoadClusteredCache.acquireLocalReadLock();
    }
  }

  private void sleepMillis(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      // ignore
    }
  }

  /* package-private method, should be used for tests only */
  ValueWithMetaData<V> internalGetFromCollectingMap(K key) {
    return collectBuffer.get(key);
  }

  /* package-private method, should be used for tests only */
  void internalPutInFlushBuffer(K key, V value, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds,
                                MetaData metaData) {
    flushBuffer.put(key, new ValueWithMetaData(metaData, value, createTimeInSecs, customMaxTTISeconds,
                                               customMaxTTLSeconds));
  }

  /* package-private method, should be used for tests only */
  void allowFlushBufferWrites() {
    if (flushBuffer == EMPTY_MAP) {
      flushBuffer = newMap();
    }
  }

  // this method is called under write lock from BulkLoadClusteredCache
  public void dispose() {
    flushAndStopBuffering();
    flushToServerThread.markFinish();
  }

  /**
   * Does not flush the buffer even if there are pending changes. Requests to terminate the periodic flush thread
   */
  public void shutdown() {
    flushToServerThread.markFinish();
  }

  // this method is called under write lock from BulkLoadClusteredCache
  // unpause the flush thread
  public void startBuffering() {
    startThreadIfNecessary();
    if (flushToServerThread.isFinished()) {
      // sane formatter
      throw new AssertionError("Start Buffering called when flush thread has already finished");
    }
    flushToServerThread.unpause();
  }

  // this method is called under write lock
  // flushes pending buffers and pauses the flushing thread
  public void flushAndStopBuffering() {

    // first flush contents of flushBuffer if flush already in progress.
    // flush thread cannot start flush once another (app) thread has called
    // this method as this is under same write-lock
    flushToServerThread.waitUntilFlushCompleteAndPause();

    // as no more puts can happen, directly drain the collectingMap to server
    switchBuffers(newMap());
    try {
      drainBufferToServer(flushBuffer);
    } finally {
      flushBuffer = EMPTY_MAP;
    }
  }

  /**
   * Only called by flush thread regularly
   * 
   * @param thread
   */
  private void doPeriodicFlush(FlushToServerThread thread) {
    Map<K, ValueWithMetaData<V>> localMap = newMap();
    bulkLoadClusteredCache.acquireLocalWriteLock();
    try {
      // mark flush in progress, done under write-lock
      if (!thread.markFlushInProgress()) return;
      switchBuffers(localMap);
    } finally {
      bulkLoadClusteredCache.releaseLocalWriteLock();
    }
    try {
      drainBufferToServer(flushBuffer);
    } finally {
      flushBuffer = EMPTY_MAP;
      thread.markFlushComplete();
    }
  }

  // This method is always called under write lock.
  private void switchBuffers(Map<K, ValueWithMetaData<V>> newBuffer) {
    flushBuffer = collectBuffer;
    collectBuffer = newBuffer;
    pendingOpsSize.set(0);
  }

  private void drainBufferToServer(final Map<K, ValueWithMetaData<V>> buffer) {
    clearIfNecessary();
    Set<Entry<K, ValueWithMetaData<V>>> entrySet = buffer.entrySet();

    // Workaround (hopefully temporary) for DEV-3814
    if (entrySet.isEmpty()) { return; }

    final Lock lock = concurrentTransactionLock;
    lock.lock();
    try {
      for (Entry<K, ValueWithMetaData<V>> e : entrySet) {
        ValueWithMetaData<V> value = e.getValue();
        K key = e.getKey();

        if (value.isRemove()) {
          backend.unlockedRemoveNoReturn(key, value.getMetaData());
        } else {
          // use incoherent put
          backend.unlockedPutNoReturn(key, value.getValue(), value.getCreateTimeInSecs(),
                                      value.getCustomMaxTTISeconds(), value.getCustomMaxTTLSeconds(),
                                      value.getMetaData());
        }
      }
    } finally {
      lock.unlock();
    }
  }

  private void clearIfNecessary() {
    if (clearMap) {
      final Lock lock = concurrentTransactionLock;
      lock.lock();
      try {
        backend.clearWithMetaData(clearMetaData);
      } finally {
        lock.unlock();
        // reset
        clearMap = false;
        clearMetaData = null;
      }
    }
  }

  private static class FlushToServerThread extends Thread {

    enum State {
      NOT_STARTED, PAUSED, SLEEP, FLUSH, FINISHED
    }

    private final LocalBufferedMap localBufferedMap;
    private State                  state = State.NOT_STARTED;

    public FlushToServerThread(String name, LocalBufferedMap localBufferedMap) {
      super(name);
      this.localBufferedMap = localBufferedMap;
    }

    public void unpause() {
      moveTo(State.PAUSED, State.SLEEP);
    }

    @Override
    public void run() {
      while (!isFinished()) {
        waitUntilNotPaused();
        if (this.localBufferedMap.pendingOpsSize.get() < PUTS_BATCH_BYTE_SIZE) {
          // do not go to sleep if we've got enough work to do
          sleepFor(BATCH_TIME_MILLISECS);
        }
        this.localBufferedMap.doPeriodicFlush(this);
      }
    }

    private void waitUntilNotPaused() {
      waitUntilStateChangesFrom(State.PAUSED);
    }

    private synchronized boolean isFinished() {
      return (state == State.FINISHED);
    }

    public void markFinish() {
      moveTo(State.FINISHED);
    }

    public boolean markFlushInProgress() {
      return moveTo(State.SLEEP, State.FLUSH);
    }

    public boolean markFlushComplete() {
      return moveTo(State.FLUSH, State.SLEEP);
    }

    public synchronized void waitUntilFlushCompleteAndPause() {
      waitUntilStateChangesFrom(State.FLUSH);
      moveTo(State.SLEEP, State.PAUSED);
    }

    @Override
    public synchronized void start() {
      if (moveTo(State.NOT_STARTED, State.PAUSED)) {
        super.start();
      }
    }

    private void sleepFor(long millis) {
      try {
        Thread.sleep(millis);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    private synchronized void waitUntilStateChangesFrom(State current) {
      while (state == current) {
        try {
          wait();
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }

    private synchronized void moveTo(State newState) {
      state = newState;
      notifyAll();
    }

    private synchronized boolean moveTo(State oldState, State newState) {
      if (state == oldState) {
        state = newState;
        notifyAll();
        return true;
      }
      return false;
    }
  }

  static class ValueWithMetaData<T> {

    private final MetaData metaData;
    private final T        value;
    private final int      createTimeInSecs;
    private final int      customMaxTTISeconds;
    private final int      customMaxTTLSeconds;

    ValueWithMetaData(MetaData metaData, T value, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds) {
      super();
      this.metaData = metaData;
      this.value = value;
      this.createTimeInSecs = createTimeInSecs;
      this.customMaxTTISeconds = customMaxTTISeconds;
      this.customMaxTTLSeconds = customMaxTTLSeconds;
    }

    MetaData getMetaData() {
      return metaData;
    }

    T getValue() {
      return value;
    }

    boolean isRemove() {
      return false;
    }

    public int getCreateTimeInSecs() {
      return createTimeInSecs;
    }

    public int getCustomMaxTTISeconds() {
      return customMaxTTISeconds;
    }

    public int getCustomMaxTTLSeconds() {
      return customMaxTTLSeconds;
    }

  }

  static class RemoveValueWithMetaData<T> extends ValueWithMetaData<T> {

    public RemoveValueWithMetaData(MetaData metaData) {
      super(metaData, null, -1, -1, -1);
    }

    @Override
    boolean isRemove() {
      return true;
    }
  }

  public boolean isKeyBeingRemoved(Object obj) {
    ValueWithMetaData<V> v = collectBuffer.get(obj);
    if (v != null && v.isRemove()) { return true; }
    return false;
  }

}

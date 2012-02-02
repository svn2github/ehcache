/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.pool.SizeOfEngine;
import net.sf.ehcache.pool.impl.DefaultSizeOfEngine;

import org.terracotta.cache.TimestampedValue;
import org.terracotta.cluster.TerracottaProperties;
import org.terracotta.locking.LockType;
import org.terracotta.locking.TerracottaLock;
import org.terracotta.meta.MetaData;
import org.terracotta.modules.ehcache.coherence.CacheCoherence;

import java.util.Collection;
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
  private static final int    MAX_SIZEOF_DEPTH                             = 1000;
  private static final int    ONE_KB                                       = 1024;
  private static final int    ONE_MB                                       = 1 * ONE_KB * ONE_KB;                                                // 1MB
  protected static final int  DEFAULT_LOCAL_BUFFER_PUTS_BATCH_BYTE_SIZE    = 5 * ONE_MB;                                                         // 5MB
  protected static final int  DEFAULT_LOCAL_BUFFER_PUTS_BATCH_TIME_MILLIS  = 600;
  protected static final int  DEFAULT_LOCAL_BUFFER_PUTS_THROTTLE_BYTE_SIZE = 10 * ONE_MB;                                                        // 10MB
  private static final String CONCURRENT_TXN_LOCK_ID                       = "local-buffer-static-concurrent-txn-lock-id";

  private static final int    PUTS_BATCH_BYTE_SIZE                         = getTerracottaProperty(CacheCoherence.LOCAL_BUFFER_PUTS_BATCH_BYTE_SIZE_PROPERTY,
                                                                                                   DEFAULT_LOCAL_BUFFER_PUTS_BATCH_BYTE_SIZE);
  private static final long   BATCH_TIME_MILLISECS                         = getTerracottaProperty(CacheCoherence.LOCAL_BUFFER_PUTS_BATCH_TIME_MILLIS_PROPERTY,
                                                                                                   DEFAULT_LOCAL_BUFFER_PUTS_BATCH_TIME_MILLIS);
  private static final long   THROTTLE_PUTS_BYTE_SIZE                      = getTerracottaProperty(CacheCoherence.LOCAL_BUFFER_PUTS_THROTTLE_BYTE_SIZE_PROPERTY,
                                                                                                   DEFAULT_LOCAL_BUFFER_PUTS_THROTTLE_BYTE_SIZE);
  private static final Map    EMPTY_MAP                                    = Collections.EMPTY_MAP;

  private static final int    LOCAL_MAP_INITIAL_CAPACITY                   = 128;
  private static final float  LOCAL_MAP_LOAD_FACTOR                        = 0.75f;
  private static final int    LOCAL_MAP_INITIAL_SEGMENTS                   = 128;

  protected static final int  DEFAULT_LOCAL_BUFFER_PUTS_BATCH_SIZE         = 600;
  protected static final int  DEFAULT_LOCAL_BUFFER_PUTS_THROTTLE_SIZE      = 200000;
  @SuppressWarnings("unused")
  private static final int    PUTS_BATCH_SIZE                              = getTerracottaProperty(CacheCoherence.LOCAL_BUFFER_PUTS_BATCH_SIZE_PROPERTY,
                                                                                                   DEFAULT_LOCAL_BUFFER_PUTS_BATCH_SIZE);
  @SuppressWarnings("unused")
  private static final long   THROTTLE_PUTS_SIZE                           = getTerracottaProperty(CacheCoherence.LOCAL_BUFFER_PUTS_THROTTLE_SIZE_PROPERTY,
                                                                                                   DEFAULT_LOCAL_BUFFER_PUTS_THROTTLE_SIZE);

  // these two properties are old, we batch based on size now

  private static int getTerracottaProperty(String propName, int defaultValue) {
    try {
      return new TerracottaProperties().getInteger(propName, defaultValue);
    } catch (NoClassDefFoundError e) {
      // for unit-tests
      return defaultValue;
    }

  }

  private final FlushToServerThread                   flushToServerThread;
  private final ClusteredStoreBackend<Object, Object> clusteredStoreBackend;
  private final CacheCoherence                        incoherentNodesSet;
  private final ValueModeHandler                      valueModeHandler;

  private volatile Map<K, ValueWithMetaData<V>>       collectBuffer;
  private volatile Map<K, ValueWithMetaData<V>>       flushBuffer;
  private volatile boolean                            clearMap       = false;
  private volatile AtomicLong                         pendingOpsSize = new AtomicLong();
  private final SizeOfEngine                          sizeOfEngine;
  private volatile MetaData                           clearMetaData;

  public LocalBufferedMap(ClusteredStoreBackend<Object, Object> clusteredStoreBackend,
                          CacheCoherence incoherentNodesSet, ValueModeHandler valueModeHandler) {
    this.clusteredStoreBackend = clusteredStoreBackend;
    this.valueModeHandler = valueModeHandler;
    this.collectBuffer = newMap();
    this.flushBuffer = EMPTY_MAP;
    this.incoherentNodesSet = incoherentNodesSet;
    this.flushToServerThread = new FlushToServerThread("Incoherent LocalBufferredMap Flush Thread ["
                                                       + clusteredStoreBackend.getConfig().getName() + "]", this);
    flushToServerThread.setDaemon(true);
    sizeOfEngine = new DefaultSizeOfEngine(MAX_SIZEOF_DEPTH, true);
  }

  private Map<K, ValueWithMetaData<V>> newMap() {
    return new ConcurrentHashMap<K, ValueWithMetaData<V>>(LOCAL_MAP_INITIAL_CAPACITY, LOCAL_MAP_LOAD_FACTOR,
                                                          LOCAL_MAP_INITIAL_SEGMENTS);
  }

  // this method is called under read-lock from ClusteredStore
  public V get(K key) {
    // get from collectingBuffer or flushBuffer
    ValueWithMetaData<V> v = collectBuffer.get(key);
    if (v != null && v.isRemove()) { return null; }
    if (v != null) { return v.getValue(); }
    v = flushBuffer.get(key);
    if (v != null && v.isRemove()) { return null; }
    return v == null ? null : v.getValue();
  }

  // this method is called under read-lock from ClusteredStore
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

  // this method is called under read-lock from ClusteredStore
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

  // this method is called under read-lock from ClusteredStore
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

  // this method is called under write-lock from ClusteredStore
  public void clear(MetaData metaData) {
    collectBuffer.clear();
    flushBuffer.clear();
    // mark the backend to be cleared
    this.clearMap = true;
    this.clearMetaData = metaData;
    pendingOpsSize.set(0);
  }

  // this method is called under read-lock from ClusteredStore
  public Collection getKeys() {
    Set<K> keySet = new HashSet<K>(collectBuffer.keySet());
    keySet.addAll(flushBuffer.keySet());
    return keySet;
  }

  // this method is called under read-lock from ClusteredStore
  public void put(K key, V value, MetaData metaData) {
    ValueWithMetaData<V> valueWMD = new ValueWithMetaData(value, metaData);
    if (collectBuffer.put(key, valueWMD) == null) {
      throttleIfNecessary(pendingOpsSize.addAndGet(sizeOfEngine.sizeOf(key, valueWMD, null).getCalculated()));
    }
  }

  void startThreadIfNecessary() {
    flushToServerThread.start();
  }

  private void throttleIfNecessary(long currentPendingSize) {
    if (currentPendingSize <= THROTTLE_PUTS_BYTE_SIZE) { return; }
    incoherentNodesSet.releaseReadLock();
    try {
      while (currentPendingSize > THROTTLE_PUTS_BYTE_SIZE) {
        sleepMillis(100);
        currentPendingSize = pendingOpsSize.get();
      }
    } finally {
      incoherentNodesSet.acquireReadLock();
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
  void internalPutInFlushBuffer(K key, V value, MetaData metaData) {
    flushBuffer.put(key, new ValueWithMetaData(value, metaData));
  }

  /* package-private method, should be used for tests only */
  void allowFlushBufferWrites() {
    if (flushBuffer == EMPTY_MAP) {
      flushBuffer = newMap();
    }
  }

  // this method is called under write lock from ClusteredStore
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

  // this method is called under write lock from ClusteredStore
  // unpause the flush thread
  public void startBuffering() {
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
    incoherentNodesSet.acquireWriteLock();
    try {
      // mark flush in progress, done under write-lock
      if (!thread.markFlushInProgress()) return;
      switchBuffers(localMap);
    } finally {
      incoherentNodesSet.releaseWriteLock();
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

    final Lock lock = getConcurrentTransactionLock();
    lock.lock();
    try {
      for (Entry<K, ValueWithMetaData<V>> e : entrySet) {
        ValueWithMetaData<V> value = e.getValue();
        K key = e.getKey();

        if (value.isRemove()) {
          clusteredStoreBackend.unlockedRemoveNoReturn(key, value.getMetaData());
        } else {
          // use incoherent put
          clusteredStoreBackend.unlockedPutNoReturn(key, value.getValue(), value.getMetaData());
        }
      }
    } finally {
      lock.unlock();
      for (ValueWithMetaData<V> value : buffer.values()) {
        V v = value.getValue();
        // probably refactor this in an AfterFlushTask interface ?
        if (v instanceof TimestampedValue) {
          valueModeHandler.processStoredValue((TimestampedValue) v);
        }
      }
    }
  }

  private void clearIfNecessary() {
    if (clearMap) {
      final Lock lock = getConcurrentTransactionLock();
      lock.lock();
      try {
        clusteredStoreBackend.clear(clearMetaData);
      } finally {
        lock.unlock();
        // reset
        clearMap = false;
        clearMetaData = null;
      }
    }
  }

  protected Lock getConcurrentTransactionLock() {
    return new TerracottaLock(CONCURRENT_TXN_LOCK_ID, LockType.CONCURRENT);
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
      waitUntilNotIn(State.PAUSED);
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
      waitUntilNotIn(State.FLUSH);
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

    private synchronized void waitUntilNotIn(State current) {
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

    ValueWithMetaData(T value, MetaData metaData) {
      this.value = value;
      this.metaData = metaData;
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
  }

  static class RemoveValueWithMetaData<T> extends ValueWithMetaData<T> {
    RemoveValueWithMetaData(MetaData metaData) {
      super(null, metaData);
    }

    @Override
    boolean isRemove() {
      return true;
    }
  }

}

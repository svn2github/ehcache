/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.writer.CacheWriter;
import net.sf.ehcache.writer.writebehind.OperationsFilter;
import net.sf.ehcache.writer.writebehind.WriteBehind;

import org.terracotta.async.AsyncCoordinator;
import org.terracotta.async.ItemScatterPolicy;
import org.terracotta.async.ProcessingBucket;
import org.terracotta.cache.serialization.DsoSerializationStrategy;
import org.terracotta.cache.serialization.SerializationStrategy;
import org.terracotta.modules.ehcache.writebehind.operations.DeleteAsyncOperation;
import org.terracotta.modules.ehcache.writebehind.operations.SingleAsyncOperation;
import org.terracotta.modules.ehcache.writebehind.operations.WriteAsyncOperation;
import org.terracotta.modules.ehcache.writebehind.snapshots.ElementSnapshot;
import org.terracotta.modules.ehcache.writebehind.snapshots.IdentityElementSnapshot;
import org.terracotta.modules.ehcache.writebehind.snapshots.IdentityKeySnapshot;
import org.terracotta.modules.ehcache.writebehind.snapshots.KeySnapshot;
import org.terracotta.modules.ehcache.writebehind.snapshots.SerializationElementSnapshot;
import org.terracotta.modules.ehcache.writebehind.snapshots.SerializationKeySnapshot;

import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A write behind implementation that relies on tim-async for its functionalities.
 * 
 * @author Geert Bevin
 * @version $Id$
 */
public class AsyncWriteBehind implements WriteBehind {

  private final AsyncCoordinator<SingleAsyncOperation> async;
  private final TerracottaConfiguration.ValueMode      valueMode;
  private final SerializationStrategy                  serializationStrategy;
  private final int                                    concurrency;

  private final Lock                                   writeLock;
  private final Lock                                   readLock;

  private volatile Status                              status = Status.UNINITIALIZED;

  /**
   * Instantiate a new instance of {@code AsyncWriteBehind} by providing the async coordinator instance that will be
   * used for the underlying behavior.
   * 
   * @param async the async coordinator instance that will be used by the write behind queue
   * @param cache the cache this write behind is bound to
   */
  public AsyncWriteBehind(AsyncCoordinator async, Ehcache cache, DsoSerializationStrategy serializationStrategy) {
    this.async = async;
    this.valueMode = cache.getCacheConfiguration().getTerracottaConfiguration().getValueMode();
    this.concurrency = cache.getCacheConfiguration().getCacheWriterConfiguration().getWriteBehindConcurrency();
    this.serializationStrategy = serializationStrategy;
    ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    writeLock = lock.writeLock();
    readLock = lock.readLock();
  }

  public void start(CacheWriter writer) {
    writeLock.lock();
    try {
      status = Status.STARTED;
      async.start(new CacheWriterWrapper(writer, serializationStrategy), concurrency,
                  new SingleAsyncOperationItemScatterPolicy(serializationStrategy));
    } finally {
      writeLock.unlock();
    }
  }

  public void write(Element element) {
    readLock.lock();
    try {
      status.checkRunning();
      ElementSnapshot snapshot = createElementSnapshot(element);
      async.add(new WriteAsyncOperation(snapshot));
    } finally {
      readLock.unlock();
    }
  }

  public void delete(CacheEntry entry) {
    readLock.lock();
    try {
      status.checkRunning();
      async.add(new DeleteAsyncOperation(createKeySnapshot(entry.getKey()), createElementSnapshot(entry.getElement())));
    } finally {
      readLock.unlock();
    }
  }

  public void setOperationsFilter(final OperationsFilter filter) {
    async.setQuarantinedItemsFilter(new OperationsFilterWrapper(filter, serializationStrategy));
  }

  private ElementSnapshot createElementSnapshot(Element element) {
    if (null == element) { return null; }

    ElementSnapshot snapshot;
    switch (valueMode) {
      case IDENTITY:
        snapshot = new IdentityElementSnapshot(element);
        break;
      case SERIALIZATION:
        try {
          snapshot = new SerializationElementSnapshot(serializationStrategy, element);
        } catch (IOException e) {
          throw new CacheException("Unexpected exception while creating a snapshot of element " + element, e);
        }
        break;
      default:
        throw new CacheException("Unsupported Terracotta value mode " + valueMode);
    }
    return snapshot;
  }

  private KeySnapshot createKeySnapshot(Object key) {
    KeySnapshot snapshot;
    switch (valueMode) {
      case IDENTITY:
        snapshot = new IdentityKeySnapshot(key);
        break;
      case SERIALIZATION:
        try {
          snapshot = new SerializationKeySnapshot(serializationStrategy, key);
        } catch (IOException e) {
          throw new CacheException("Unexpected exception while creating a snapshot of key " + key, e);
        }
        break;
      default:
        throw new CacheException("Unsupported Terracotta value mode " + valueMode);
    }
    return snapshot;
  }

  public void stop() {
    writeLock.lock();
    try {
      async.stop();
      status = Status.STOPPED;
    } finally {
      writeLock.unlock();
    }
  }

  public long getQueueSize() {
    readLock.lock();
    try {
      status.checkRunning();
      int size = 0;
      for (ProcessingBucket<SingleAsyncOperation> localBucket : async.getLocalBuckets()) {
        size += localBucket.getWaitCount();
      }
      return size;
    } finally {
      readLock.unlock();
    }
  }

  private static class SingleAsyncOperationItemScatterPolicy implements ItemScatterPolicy<SingleAsyncOperation> {

    private final SerializationStrategy serializationStrategy;

    private SingleAsyncOperationItemScatterPolicy(final SerializationStrategy serializationStrategy) {
      this.serializationStrategy = serializationStrategy;
    }

    public int selectBucket(final int count, final SingleAsyncOperation item) {
      Object key;
      try {
        key = item.getKey(serializationStrategy);
      } catch (Exception e) {
        throw new CacheException(e);
      }
      return Math.abs(key.hashCode() % count);
    }
  }

  private static enum Status {
    UNINITIALIZED,
    STARTED {
      @Override
      final void checkRunning() {
        // All good!
      }
    },
    STOPPED;

    void checkRunning() {
      throw new IllegalStateException("AsyncWriteBehind is " + this.name().toLowerCase() + "!");
    }
  }
}

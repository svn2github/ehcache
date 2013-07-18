/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.modules.ehcache.async.AsyncCoordinatorImpl.Callback;
import org.terracotta.modules.ehcache.async.exceptions.ProcessingException;
import org.terracotta.toolkit.cluster.ClusterInfo;
import org.terracotta.toolkit.internal.collections.ToolkitListInternal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ProcessingBucket<E extends Serializable> {
  private enum STOP_STATE {
    NORMAL, STOP_REQUESTED, STOPPED
  }

  private static final Logger          LOGGER                   = LoggerFactory.getLogger(ProcessingBucket.class
                                                                    .getName());
  private static final int             UNLIMITED_QUEUE_SIZE     = 0;
  private static final String          threadNamePrefix         = "ProcessingWorker|";
  private final String                 bucketName;
  private final AsyncConfig            config;
  private final ClusterInfo            cluster;
  private final ItemProcessor<E>       processor;
  private volatile ItemsFilter<E>      filter;
  private final long                   baselineTimestampMillis;
  private final Lock                   bucketWriteLock;
  private final Lock                   bucketReadLock;
  private final Condition              bucketNotEmpty;
  private final Condition              bucketNotFull;
  private final Condition              stoppedButBucketNotEmpty;
  private final ToolkitListInternal<E> toolkitList;
  private long                         lastProcessingTimeMillis = -1;
  private long                         lastWorkDoneMillis       = -1;
  private volatile STOP_STATE          stopState                = STOP_STATE.NORMAL;
  private final AtomicLong             workDelay;
  private final ProcessingWorker       processingWorkerRunnable;
  private volatile Thread              processingWorkerThread;
  private Callback                     cleanupCallback;
  private final boolean                workingOnDeadBucket;
  private volatile boolean             destroyAfterStop;

  public ProcessingBucket(String bucketName, AsyncConfig config, ToolkitListInternal<E> toolkitList,
                          ClusterInfo cluster,
                          ItemProcessor<E> processor, boolean workingOnDeadBucket) {
    this.bucketName = bucketName;
    this.config = config;
    this.cluster = cluster;
    this.processor = processor;
    this.toolkitList = toolkitList;
    this.baselineTimestampMillis = System.currentTimeMillis();
    ReentrantReadWriteLock bucketLock = new ReentrantReadWriteLock();
    this.bucketReadLock = bucketLock.readLock();
    this.bucketWriteLock = bucketLock.writeLock();
    this.bucketNotEmpty = bucketWriteLock.newCondition();
    this.bucketNotFull = bucketWriteLock.newCondition();
    this.stoppedButBucketNotEmpty = bucketWriteLock.newCondition();
    this.workDelay = new AtomicLong(config.getWorkDelay());
    this.workingOnDeadBucket = workingOnDeadBucket;
    this.processingWorkerRunnable = new ProcessingWorker(threadNamePrefix + bucketName);
    this.destroyAfterStop = true;
  }

  public String getBucketName() {
    return bucketName;
  }

  /**
   * @return returns recent time stamp when processItems() executed.
   */
  public long getLastProcessing() {
    bucketReadLock.lock();
    try {
      return lastProcessingTimeMillis;
    } finally {
      bucketReadLock.unlock();
    }
  }

  public void setItemsFilter(ItemsFilter<E> filter) {
    this.filter = filter;
  }

  private long baselinedCurrentTimeMillis() {
    return System.currentTimeMillis() - baselineTimestampMillis;
  }

  void start() {
    bucketWriteLock.lock();
    try {
      ensureNonExistingThread();
      processingWorkerThread = new Thread(processingWorkerRunnable);
      processingWorkerThread.setName(processingWorkerRunnable.getThreadName());
      processingWorkerThread.setDaemon(true);
      processingWorkerThread.start();
    } finally {
      bucketWriteLock.unlock();
    }
  }

  private void ensureNonExistingThread() {
    if (processingWorkerThread != null) { throw new AssertionError(processingWorkerRunnable.getThreadName()); }
  }

  private boolean isCancelled() {
    try {
      bucketReadLock.lock();
      try {
        return (stopState == STOP_STATE.STOPPED) || (workingOnDeadBucket && toolkitList.isEmpty());
      } finally {
        bucketReadLock.unlock();
      }
    } catch (RuntimeException e) {
      if (isTCNRE(e)) {
        return true;
      } else {
        throw e;
      }
    }
  }

  private boolean isTCNRE(Throwable th) {
    return th.getClass().getName().equals("com.tc.exception.TCNotRunningException");
  }

  public int getWaitCount() {
    bucketReadLock.lock();
    try {
      return toolkitList.size();
    } finally {
      bucketReadLock.unlock();
    }
  }

  public void stopNow() {
    bucketWriteLock.lock();
    try {
      destroyAfterStop = false;
      stopState = STOP_STATE.STOPPED;
      bucketNotEmpty.signalAll();
      bucketNotFull.signalAll();
      processingWorkerThread.interrupt();
    } finally {
      bucketWriteLock.unlock();
    }
  }

  public void stop() {
    bucketWriteLock.lock();
    try {
      workDelay.set(0);
      stopState = STOP_STATE.STOP_REQUESTED;
      while (!toolkitList.isEmpty()) {
        stoppedButBucketNotEmpty.await();
      }
      stopState = STOP_STATE.STOPPED;
      bucketNotEmpty.signalAll();
      bucketNotFull.signalAll();
      processingWorkerThread.interrupt();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      bucketWriteLock.unlock();
    }
  }

  public void destroy() {
    try {
      debug("destroying bucket " + toolkitList.getName() + " " + toolkitList.size());
      toolkitList.destroy();
    } catch (Throwable t) {
      if (isTCNRE(t) && !cluster.areOperationsEnabled()) {
        LOGGER
            .warn("destroyToolkitList caught TCNotRunningException on processing thread, but looks like we were shut down. "
                      + "This can safely be ignored!", t);
      }
    }
  }

  private String getThreadName() {
    return processingWorkerRunnable.getThreadName();
  }

  // Do not take any clustered write lock in this path.
  public void add(final E item) {
    if (null == item) return;
    int maxQueueSize = config.getMaxQueueSize();
    bucketWriteLock.lock();
    boolean interrupted = false;
    try {
      if (maxQueueSize != UNLIMITED_QUEUE_SIZE) {
        while (!isCancelled() && toolkitList.size() >= maxQueueSize) {
          try {
            bucketNotFull.await();
          } catch (final InterruptedException e) {
            interrupted = true;
          }
        }
      }
      boolean signalNotEmpty = toolkitList.isEmpty();
      toolkitList.unlockedAdd(item);
      if (signalNotEmpty) {
        bucketNotEmpty.signalAll();
      }
    } finally {
      bucketWriteLock.unlock();
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private int determineBatchSize() {
    int batchSize = config.getBatchSize();
    int listSize = toolkitList.size();
    if (listSize < batchSize) {
      batchSize = listSize;
    }
    return batchSize;
  }

  private void debug(String message) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(message);
    }
  }

  private void filterQuarantined() {
    if (null == filter) { return; }

    bucketWriteLock.lock();
    try {
      ItemsFilter<E> itemsFilter = this.filter;
      if (itemsFilter != null) {
        debug(getThreadName() + " : filterQuarantined: filtering " + toolkitList.size() + " quarantined items");
        itemsFilter.filter(toolkitList);
        debug(getThreadName() + " : filterQuarantined: retained " + toolkitList.size() + " quarantined items");
      }
    } finally {
      bucketWriteLock.unlock();
    }
  }

  /**
   * This method process items from bucket. Execution of this method does not guarantee that items from a non empty
   * bucket will be processed.
   */
  private void processItems() throws ProcessingException {
    final int workSize;

    bucketWriteLock.lock();
    try {
      // set some state related to this processing run
      lastProcessingTimeMillis = baselinedCurrentTimeMillis();

      workSize = toolkitList.size();
      // if there's no work that needs to be done, stop the processing
      if (0 == workSize) {
        debug(getThreadName() + " : processItems : nothing to process");
        return;
      }
      // filter might remove items from list, so this should be with-in writeLock
      filterQuarantined();
    } finally {
      bucketWriteLock.unlock();
    }

    // if the batching is enabled and work size is smaller than batch size, don't process anything as long as the max
    // allowed fall behind delay hasn't expired
    final int batchSize = config.getBatchSize();
    if (config.isBatchingEnabled() && batchSize > 0) {
      // wait for another round if the batch size hasn't been filled up yet and the max write delay hasn't expired yet
      if (workSize < batchSize && config.getMaxAllowedFallBehind() > lastProcessingTimeMillis - lastWorkDoneMillis) {
        bucketReadLock.lock();
        try {
          if (stopState == STOP_STATE.NORMAL) {
            debug(getThreadName() + " : processItems : only " + workSize + " work items available, waiting for "
                        + batchSize + " items to fill up a batch");
            return;
          }
        } finally {
          bucketReadLock.unlock();
        }
      }

      // enforce the rate limit and wait for another round if too much would be processed compared to
      // the last time when a batch was executed
      final int rateLimit = config.getRateLimit();
      if (rateLimit > 0) {
        final long secondsSinceLastWorkDone;
        final int effectiveBatchSize;
        bucketReadLock.lock();
        try {
          if (stopState == STOP_STATE.NORMAL) {
            secondsSinceLastWorkDone = (baselinedCurrentTimeMillis() - lastWorkDoneMillis) / 1000;
            effectiveBatchSize = determineBatchSize();
            long maxBatchSizeSinceLastWorkDone = rateLimit * secondsSinceLastWorkDone;
            if (effectiveBatchSize > maxBatchSizeSinceLastWorkDone) {
              debug(getThreadName() + " : processItems() : last work was done " + secondsSinceLastWorkDone
                          + " seconds ago, processing " + effectiveBatchSize
                          + " batch items would exceed the rate limit of " + rateLimit + ", waiting for a while.");
              return;
            }
          }
        } finally {
          bucketReadLock.unlock();
        }
      }
    }

    bucketWriteLock.lock();
    try {
      lastWorkDoneMillis = baselinedCurrentTimeMillis();
    } finally {
      bucketWriteLock.unlock();
    }

    doProcessItems();
  }

  private void doProcessItems() throws ProcessingException {
    // process the quarantined items and remove them as they're processed
    // don't process work if this node's operations have been disabled
    if (!cluster.areOperationsEnabled()) {
      return;
    } else {
      if (config.isBatchingEnabled() && config.getBatchSize() > 0) {
        processBatchedItems();
      } else {
        processListSnapshot();
      }
      if (toolkitList.isEmpty() && stopState == STOP_STATE.STOP_REQUESTED) {
        signalStop();
      }
    }
  }

  private void signalStop() {
    bucketWriteLock.lock();
    try {
      stoppedButBucketNotEmpty.signalAll();
    } finally {
      bucketWriteLock.unlock();
    }
  }

  private void processListSnapshot() throws ProcessingException {
    int size = toolkitList.size();
    debug(getThreadName() + " : processListSnapshot size " + size + " quarantined items");
    while (size-- > 0) {
      processSingleItem();
    }
  }

  private void processSingleItem() throws ProcessingException {
    // process the next item
    final E item = getItemsFromQueue(1).get(0);
    final int retryAttempts = config.getRetryAttempts();
    int executionsLeft = retryAttempts + 1;
    while (executionsLeft-- > 0) {
      try {
        processor.process(item);
        break;
      } catch (final RuntimeException e) {
        if (executionsLeft <= 0) {
          try {
            processor.throwAway(item, e);
          } catch (final Throwable th) {
            LOGGER.warn("processSingleItem caught error while throwing away an item: " + item, th);
          }
        } else {
          LOGGER.warn(getThreadName() + " : processSingleItem() : exception during processing, retrying in "
                      + retryAttempts + " milliseconds, " + executionsLeft + " retries left", e);
          try {
            Thread.sleep(config.getRetryAttemptDelay());
          } catch (InterruptedException e1) {
            Thread.currentThread().interrupt();
            throw e;
          }
        }
      }
    }
    removeFromQueue(1);
  }

  private void processBatchedItems() throws ProcessingException {
    final int effectiveBatchSize = determineBatchSize();
    List<E> batch = getItemsFromQueue(effectiveBatchSize);
    final int retryAttempts = config.getRetryAttempts();
    int executionsLeft = retryAttempts + 1;
    while (executionsLeft-- > 0) {
      try {
        processor.process(batch);
        break;
      } catch (final RuntimeException e) {
        LOGGER.warn("processBatchedItems caught error while processing batch of " + batch.size(), e);
        if (executionsLeft <= 0) {
          for (E item : batch) {
            try {
              processor.throwAway(item, e);
            } catch (final Throwable th) {
              LOGGER.warn("processBatchedItems caught error while throwing away an item: " + item, th);
            }
          }
        } else {
          LOGGER.warn(getThreadName() + " : processBatchedItems() : exception during processing, retrying in "
                      + retryAttempts + " milliseconds, " + executionsLeft + " retries left", e);
          try {
            Thread.sleep(config.getRetryAttemptDelay());
          } catch (InterruptedException e1) {
            Thread.currentThread().interrupt();
            throw e;
          }
        }
      }
    }

    removeFromQueue(effectiveBatchSize);
  }

  private List<E> getItemsFromQueue(final int effectiveBatchSize) {
    bucketReadLock.lock();
    try {
      List<E> batch = new ArrayList<E>(effectiveBatchSize);
      for (int i = 0; i < effectiveBatchSize; i++) {
        final E item = toolkitList.get(i);
        batch.add(item);
      }
      return batch;
    } finally {
      bucketReadLock.unlock();
    }
  }

  private void removeFromQueue(final int effectiveBatchSize) {
    bucketWriteLock.lock();
    try {
      boolean signalNotFull = toolkitList.size() >= this.config.getMaxQueueSize();

      for (int i = 0; i < effectiveBatchSize; i++) {
        toolkitList.remove(0);
      }

      if (signalNotFull) {
        bucketNotFull.signalAll();
      }
    } finally {
      bucketWriteLock.unlock();
    }
  }

  void setCleanupCallback(Callback cleanupDeadBucket) {
    this.cleanupCallback = cleanupDeadBucket;
  }

  private final class ProcessingWorker implements Runnable {
    private final String threadName;

    public ProcessingWorker(String threadName) {
      this.threadName = threadName;
    }

    public String getThreadName() {
      return threadName;
    }

    @Override
    public void run() {
      try {
        while (!isCancelled()) {
          // process the items if this node's operations are enabled
          if (cluster.areOperationsEnabled()) {
            try {
              processItems();
            } catch (final Throwable e) {
              if (cluster.areOperationsEnabled()) {
                if (!isTCNRE(e)) {
                  LOGGER.error("Caught error on processing bucket " + bucketName, e);
                }
              } else {
                LOGGER.warn("Caught error on processing items, but looks like we were shut down. "
                            + "This can probably be safely ignored", e);
              }
              continue;
            }
          }

          final long currentLastProcessing = getLastProcessing();

          // Wait for new items or until the work delay has expired.
          // Do not continue if the actual work delay wasn't at least the one specified in the config
          // otherwise it's possible to create a new work list for just a couple of items in case
          // the item processor is very fast, causing a large amount of data churn and broadcasts.
          // However, if the work delay is expired, the processing should start immediately.
          bucketWriteLock.lock();
          try {
            try {
              long tmpWorkDelay = workDelay.get();
              if (tmpWorkDelay != 0) {
                do {
                  bucketNotEmpty.await(tmpWorkDelay, TimeUnit.MILLISECONDS);
                  long actualWorkDelay = baselinedCurrentTimeMillis() - currentLastProcessing;
                  if (actualWorkDelay < tmpWorkDelay) {
                    tmpWorkDelay -= actualWorkDelay;
                  } else {
                    tmpWorkDelay = 0;
                  }
                } while (tmpWorkDelay > 0 && stopState == STOP_STATE.NORMAL);
              } else {
                while (!workingOnDeadBucket && stopState == STOP_STATE.NORMAL && toolkitList.isEmpty()) {
                  bucketNotEmpty.await();
                }
              }
            } catch (final InterruptedException e) {
              // if the processing worker thread is interrupted, act as if the bucket was canceled
              stop();
              Thread.currentThread().interrupt();
            }
          } finally {
            bucketWriteLock.unlock();
          }
        }
      } catch (Throwable t) {
        if (isTCNRE(t) && !cluster.areOperationsEnabled()) {
          LOGGER.warn("Caught TCNotRunningException on processing thread, but looks like we were shut down. "
                      + "This can safely be ignored!", t);
        }
      }

      if (destroyAfterStop) {
        // Destroy anyways, either stop happened or other dead-client bucket was finished processing
        if (workingOnDeadBucket) {
          cleanupCallback.callback();
        } else {
          destroy();
        }
      }
    }
  }

}

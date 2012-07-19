/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.modules.ehcache.async.AsyncCoordinatorImpl.Callback;
import org.terracotta.modules.ehcache.async.exceptions.ProcessingException;
import org.terracotta.toolkit.cluster.ClusterInfo;
import org.terracotta.toolkit.collections.ToolkitList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class ProcessingBucket<E extends Serializable> {
  private static final Logger     LOGGER                   = LoggerFactory.getLogger(ProcessingBucket.class.getName());
  private static final int        UNLIMITED_QUEUE_SIZE     = 0;

  private final String            bucketName;
  private final AsyncConfig       config;
  private final ClusterInfo       cluster;
  private final ItemProcessor<E>  processor;
  private volatile ItemsFilter<E> filter;
  private final long              baselineTimestampMillis;
  private final Lock              bucketWriteLock;
  private final Lock              bucketReadLock;
  private final Condition         bucketNotEmpty;
  private final Condition         bucketNotFull;
  private final Condition         stoppedButBucketNotEmpty;
  private final ToolkitList<E>    toolkitList;
  private long                    lastProcessingTimeMillis = -1;
  private long                    lastWorkDoneMillis       = -1;
  private volatile boolean        cancelled                = false;
  private final AtomicLong        workDelay;
  private final ProcessingWorker  processingWorkerRunnable;
  private volatile Thread         processingWorkerThread;
  private Callback                destroyCallback;
  private final boolean           workingOnDeadBucket;

  public ProcessingBucket(String bucketName, AsyncConfig config, ToolkitList toolkitList, ClusterInfo cluster,
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
    this.processingWorkerRunnable = new ProcessingWorker("ProcessingWorker-" + bucketName);
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
        return cancelled || (workingOnDeadBucket && toolkitList.isEmpty());
      } finally {
        bucketReadLock.unlock();
      }
    } catch (RuntimeException e) {
      if (e.getClass().getName().equals("com.tc.exception.TCNotRunningException")) {
        return true;
      } else {
        throw e;
      }
    }
  }

  public int getWaitCount() {
    bucketReadLock.lock();
    try {
      return toolkitList.size();
    } finally {
      bucketReadLock.unlock();
    }
  }

  public void stop() {
    bucketWriteLock.lock();
    try {
      workDelay.set(0);
      while (!toolkitList.isEmpty()) {
        stoppedButBucketNotEmpty.await();
      }
      cancelled = true;
      bucketNotEmpty.signalAll();
      processingWorkerThread.interrupt();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      bucketWriteLock.unlock();
    }
  }

  private void destroyToolkitList() {
    toolkitList.destroy();
    if (destroyCallback != null) {
      destroyCallback.callback();
    }
  }

  private String getThreadName() {
    return processingWorkerRunnable.getThreadName();
  }

  public void add(final E item) {
    if (null == item) return;
    int maxQueueSize = config.getMaxQueueSize();
    bucketWriteLock.lock();
    try {
      if (maxQueueSize != UNLIMITED_QUEUE_SIZE) {
        while (toolkitList.size() >= maxQueueSize) {
          try {
            bucketNotFull.await();
          } catch (final InterruptedException e) {
            // if the wait for items is interrupted, act as if the bucket was canceled
            stop();
            Thread.currentThread().interrupt();
            return;
          }
        }
      }
      boolean signalNotEmpty = toolkitList.size() == 0;

      toolkitList.add(item);

      if (signalNotEmpty) {
        bucketNotEmpty.signalAll();
      }
    } finally {
      bucketWriteLock.unlock();
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

  private void filterQuarantined() {
    if (null == filter) { return; }

    bucketWriteLock.lock();
    try {
      ItemsFilter<E> itemsFilter = this.filter;
      if (itemsFilter != null) {
        itemsFilter.filter(toolkitList);
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
    bucketWriteLock.lock();
    try {
      lastProcessingTimeMillis = baselinedCurrentTimeMillis();
    } finally {
      bucketWriteLock.unlock();
    }

    // set some state related to this processing run
    final int workSize;
    bucketReadLock.lock();
    try {
      if (cancelled) { return; }
      workSize = toolkitList.size();
      // if there's no work that needs to be done, stop the processing
      if (0 == workSize) {
        LOGGER.warn(getThreadName() + " : processItems() : nothing to process");
        return;
      }
    } finally {
      bucketReadLock.unlock();
    }

    filterQuarantined();
    // if the batching is enabled and work size is smaller than batch size, don't process anything as long as the
    // max allowed fall behind delay hasn't expired
    final int batchSize = config.getBatchSize();
    if (config.isBatchingEnabled() && batchSize > 0) {
      // wait for another round if the batch size hasn't been filled up yet and the max write delay
      // hasn't expired yet
      if (workSize < batchSize && config.getMaxAllowedFallBehind() > lastProcessingTimeMillis - lastWorkDoneMillis) {
        LOGGER.warn(getThreadName() + " : processItems() : only " + workSize + " work items available, waiting for "
                    + batchSize + " items to fill up a batch");
        return;
      }

      // enforce the rate limit and wait for another round if too much would be processed compared to
      // the last time when a batch was executed
      final int rateLimit = config.getRateLimit();
      if (rateLimit > 0) {
        final long secondsSinceLastWorkDone;
        final int effectiveBatchSize;
        bucketReadLock.lock();
        try {
          secondsSinceLastWorkDone = (baselinedCurrentTimeMillis() - lastWorkDoneMillis) / 1000;
          effectiveBatchSize = determineBatchSize();
        } finally {
          bucketReadLock.unlock();
        }
        final long maxBatchSizeSinceLastWorkDone = rateLimit * secondsSinceLastWorkDone;
        if (effectiveBatchSize > maxBatchSizeSinceLastWorkDone) {
          LOGGER.warn(getThreadName() + " : processItems() : last work was done " + secondsSinceLastWorkDone
                      + " seconds ago, processing " + effectiveBatchSize
                      + " batch items would exceed the rate limit of " + rateLimit + ", waiting for a while.");
          return;
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
        processSingleItem();
      }

      if (toolkitList.isEmpty() && cancelled) {
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
          processor.throwAway(item, e);
        } else {
          LOGGER.warn(getThreadName() + " : processSingleItem() : exception during processing, retrying in "
                      + retryAttempts + " milliseconds, " + executionsLeft + " retries left : " + e.getMessage());
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
        if (executionsLeft <= 0) {
          for (E item : batch) {
            processor.throwAway(item, e);
          }
        } else {
          LOGGER.warn(getThreadName() + " : processBatchedItems() : exception during processing, retrying in "
                      + retryAttempts + " milliseconds, " + executionsLeft + " retries left : " + e.getMessage());
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

  void setDestroyCallback(Callback removeOnDestroy) {
    this.destroyCallback = removeOnDestroy;
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
                LOGGER.error(bucketName + " " + e);
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
              if (workDelay.get() != 0) {
                do {
                  bucketNotEmpty.await(tmpWorkDelay, TimeUnit.MILLISECONDS);
                  long actualWorkDelay = baselinedCurrentTimeMillis() - currentLastProcessing;
                  if (actualWorkDelay < tmpWorkDelay) {
                    tmpWorkDelay -= actualWorkDelay;
                  } else {
                    tmpWorkDelay = 0;
                  }
                } while (tmpWorkDelay > 0);
              } else {
                while (toolkitList.isEmpty() && !workingOnDeadBucket) {
                  bucketNotEmpty.await();
                }
              }
            } catch (final InterruptedException e) {
              // if the wait for items is interrupted, act as if the bucket was canceled
              stop();
              Thread.currentThread().interrupt();
            }

          } finally {
            bucketWriteLock.unlock();
          }
        }
      } catch (Throwable t) {
        if (t.getClass().getName().equals("com.tc.exception.TCNotRunningException") && !cluster.areOperationsEnabled()) {
          LOGGER.warn("Caught TCNotRunningException on processing thread, but looks like we were shut down. "
                      + "This can safely be ignored!", t);
        }
      }

      // Destroy anyways, either stop happened or other dead-client bucket was finished processing
      destroyToolkitList();
    }
  }

}

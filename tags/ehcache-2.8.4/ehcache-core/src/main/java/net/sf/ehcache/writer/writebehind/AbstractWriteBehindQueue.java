/**
 *  Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache.writer.writebehind;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheWriterConfiguration;
import net.sf.ehcache.writer.CacheWriter;
import net.sf.ehcache.writer.writebehind.operations.DeleteOperation;
import net.sf.ehcache.writer.writebehind.operations.SingleOperation;
import net.sf.ehcache.writer.writebehind.operations.SingleOperationType;
import net.sf.ehcache.writer.writebehind.operations.WriteOperation;

/**
 * Abstract implementation of a WriteBehindQueue.
 *
 * @author tim
 */
public abstract class AbstractWriteBehindQueue implements WriteBehind {

  private static final Logger LOGGER = Logger.getLogger(WriteBehindQueue.class.getName());

  private static final int MS_IN_SEC = 1000;

  private final String cacheName;
  private final long minWriteDelayMs;
  private final long maxWriteDelayMs;
  private final int rateLimitPerSecond;
  private final int maxQueueSize;
  private final boolean writeBatching;
  private final int writeBatchSize;
  private final int retryAttempts;
  private final int retryAttemptDelaySeconds;
  private final Thread processingThread;

  private final ReentrantReadWriteLock queueLock = new ReentrantReadWriteLock();
  private final ReentrantReadWriteLock.ReadLock queueReadLock = queueLock.readLock();
  private final ReentrantReadWriteLock.WriteLock queueWriteLock = queueLock.writeLock();
  private final Condition queueIsFull = queueWriteLock.newCondition();
  private final Condition queueIsEmpty = queueWriteLock.newCondition();
  private final Condition queueIsStopped = queueWriteLock.newCondition();

  private final AtomicLong lastProcessing = new AtomicLong(System.currentTimeMillis());
  private final AtomicLong lastWorkDone = new AtomicLong(System.currentTimeMillis());
  private final AtomicBoolean busyProcessing = new AtomicBoolean(false);

  private volatile OperationsFilter filter;

  private CacheWriter cacheWriter;
  private boolean stopping;
  private boolean stopped;

  /**
   * Create a new write behind queue.
   *
   * @param config the configuration for the queue
   */
  public AbstractWriteBehindQueue(CacheConfiguration config) {
      this.stopping = false;
      this.stopped = true;

      this.cacheName = config.getName();

      // making a copy of the configuration locally to ensure that it will not be changed at runtime
      final CacheWriterConfiguration cacheWriterConfig = config.getCacheWriterConfiguration();
      this.minWriteDelayMs = cacheWriterConfig.getMinWriteDelay() * MS_IN_SEC;
      this.maxWriteDelayMs = cacheWriterConfig.getMaxWriteDelay() * MS_IN_SEC;
      this.rateLimitPerSecond = cacheWriterConfig.getRateLimitPerSecond();
      this.maxQueueSize = cacheWriterConfig.getWriteBehindMaxQueueSize();
      this.writeBatching = cacheWriterConfig.getWriteBatching();
      this.writeBatchSize = cacheWriterConfig.getWriteBatchSize();
      this.retryAttempts = cacheWriterConfig.getRetryAttempts();
      this.retryAttemptDelaySeconds = cacheWriterConfig.getRetryAttemptDelaySeconds();

      this.processingThread = new Thread(new ProcessingThread(), cacheName + " write-behind");
      this.processingThread.setDaemon(true);
  }

  /**
   * Quarantine items to be processed.
   *
   * @return list of operations
   */
  protected abstract List<SingleOperation> quarantineItems();

  /**
   * Add an item to the write behind queue
   *
   * @param operation operation to be done
   */
  protected abstract void addItem(SingleOperation operation);

  /**
   * Reinsert any unfinished operations into the queue.
   *
   * @param operations list of unfinished operations
   */
  protected abstract void reinsertUnprocessedItems(List<SingleOperation> operations);

  /**
   * {@inheritDoc}
   */
  public void start(CacheWriter writer) {
      queueWriteLock.lock();
      try {
          if (!stopped) {
              throw new CacheException("The write-behind queue for cache '" + cacheName + "' can't be started more than once");
          }

          if (processingThread.isAlive()) {
              throw new CacheException("The thread with name " + processingThread.getName() + " already exists and is still running");
          }

          this.stopping = false;
          this.stopped = false;
          this.cacheWriter = writer;

          processingThread.start();
      } finally {
          queueWriteLock.unlock();
      }
  }

  /**
   * {@inheritDoc}
   */
  public void setOperationsFilter(OperationsFilter filter) {
      this.filter = filter;
  }

  private long getLastProcessing() {
      return lastProcessing.get();
  }

  /**
   * Thread this will continuously process the items in the queue.
   */
  private final class ProcessingThread implements Runnable {
      public void run() {
          try {
              while (!isStopped()) {

                  processItems();

                  queueWriteLock.lock();
                  try {
                      // Wait for new items or until the min write delay has expired.
                      // Do not continue if the actual min write delay wasn't at least the one specified in the config
                      // otherwise it's possible to create a new work list for just a couple of items in case
                      // the item processor is very fast, causing a large amount of data churn.
                      // However, if the write delay is expired, the processing should start immediately.
                      try {
                          if (minWriteDelayMs != 0) {
                              long delay = minWriteDelayMs;
                              do {
                                  queueIsEmpty.await(delay, TimeUnit.MILLISECONDS);
                                  long actualDelay = System.currentTimeMillis() - getLastProcessing();
                                  if (actualDelay < minWriteDelayMs) {
                                      delay = minWriteDelayMs - actualDelay;
                                  } else {
                                      delay = 0;
                                  }
                              } while (delay > 0);
                          } else {
                              while (!stopping && getQueueSize() == 0) {
                                  queueIsEmpty.await();
                              }
                          }
                      } catch (final InterruptedException e) {
                          // if the wait for items is interrupted, act as if the bucket was cancelled
                          stop();
                          Thread.currentThread().interrupt();
                      }

                      // If the queue is stopping and no more work is outstanding, perform the actual stop operation
                      if (stopping && getQueueSize() == 0) {
                          stopTheQueueThread();
                      }
                      queueIsFull.signal();
                  } finally {
                      queueWriteLock.unlock();
                  }
              }
          } finally {
              stopTheQueueThread();
          }
      }

      private void stopTheQueueThread() {
          // Perform the actual stop operation and wake up everyone that is waiting for it.
          queueWriteLock.lock();
          try {
              stopped = true;
              stopping = false;
              queueIsStopped.signalAll();
          } finally {
              queueWriteLock.unlock();
          }
      }
  }

  private void processItems() throws CacheException {
      // ensure that the items aren't already being processed
      if (busyProcessing.get()) {
          throw new CacheException("The write behind queue for cache '" + cacheName + "' is already busy processing.");
      }

      // set some state related to this processing run
      busyProcessing.set(true);
      lastProcessing.set(System.currentTimeMillis());

      try {
          final int workSize;
          final List<SingleOperation> quarantined;

          queueWriteLock.lock();
          try {
              // quarantine local work
              if (getQueueSize() > 0) {
                  quarantined = quarantineItems();
              } else {
                  quarantined = null;
              }

              // check if work was quarantined
              if (quarantined != null) {
                  workSize = quarantined.size();
              } else {
                  workSize = 0;
              }
          } finally {
              queueWriteLock.unlock();
          }

          // if there's no work that needs to be done, stop the processing
          if (0 == workSize) {
              if (LOGGER.isLoggable(Level.FINER)) {
                  LOGGER.finer(getThreadName() + " : processItems() : nothing to process");
              }
              return;
          }

          try {
              filterQuarantined(quarantined);

              // if the batching is enabled and work size is smaller than batch size, don't process anything as long as the
              // max allowed delay hasn't expired
              if (writeBatching && writeBatchSize > 0) {
                  // wait for another round if the batch size hasn't been filled up yet and the max write delay
                  // hasn't expired yet
                  if (workSize < writeBatchSize && maxWriteDelayMs > lastProcessing.get() - lastWorkDone.get()) {
                      waitUntilEnoughWorkItemsAvailable(quarantined, workSize);
                      return;
                  }
                  // enforce the rate limit and wait for another round if too much would be processed compared to
                  // the last time when a batch was executed
                  if (rateLimitPerSecond > 0) {
                      final long secondsSinceLastWorkDone = (System.currentTimeMillis() - lastWorkDone.get()) / MS_IN_SEC;
                      final long maxBatchSizeSinceLastWorkDone = rateLimitPerSecond * secondsSinceLastWorkDone;
                      final int batchSize = determineBatchSize(quarantined);
                      if (batchSize > maxBatchSizeSinceLastWorkDone) {
                          waitUntilEnoughTimeHasPassed(quarantined, batchSize, secondsSinceLastWorkDone);
                          return;
                      }
                  }
              }

              // set some state related to this processing run
              lastWorkDone.set(System.currentTimeMillis());

              if (LOGGER.isLoggable(Level.FINER)) {
                  LOGGER.finer(getThreadName() + " : processItems() : processing started");
              }

              // process the quarantined items and remove them as they're processed
              processQuarantinedItems(quarantined);
          } catch (final RuntimeException e) {
              reassemble(quarantined);
              throw e;
          } catch (final Error e) {
              reassemble(quarantined);
              throw e;
          }
      } finally {
          busyProcessing.set(false);

          if (LOGGER.isLoggable(Level.FINER)) {
              LOGGER.finer(getThreadName() + " : processItems() : processing finished");
          }
      }
  }

  private void waitUntilEnoughWorkItemsAvailable(List<SingleOperation> quarantined, int workSize) {
      if (LOGGER.isLoggable(Level.FINER)) {
          LOGGER.finer(getThreadName() + " : processItems() : only " + workSize + " work items available, waiting for "
                  + writeBatchSize + " items to fill up a batch");
      }
      reassemble(quarantined);
  }

  private void waitUntilEnoughTimeHasPassed(List<SingleOperation> quarantined, int batchSize, long secondsSinceLastWorkDone) {
      if (LOGGER.isLoggable(Level.FINER)) {
          LOGGER.finer(getThreadName() + " : processItems() : last work was done " + secondsSinceLastWorkDone
                  + " seconds ago, processing " + batchSize + " batch items would exceed the rate limit of "
                  + rateLimitPerSecond + ", waiting for a while.");
      }
      reassemble(quarantined);
  }

  private int determineBatchSize(List<SingleOperation> quarantined) {
      int batchSize = writeBatchSize;
      if (quarantined.size() < batchSize) {
          batchSize = quarantined.size();
      }
      return batchSize;
  }

  private void filterQuarantined(List<SingleOperation> quarantined) {
      OperationsFilter operationsFilter = this.filter;
      if (operationsFilter != null) {
          operationsFilter.filter(quarantined, CastingOperationConverter.getInstance());
      }
  }

  private void processQuarantinedItems(List<SingleOperation> quarantined) {
      if (LOGGER.isLoggable(Level.CONFIG)) {
          LOGGER.config(getThreadName() + " : processItems() : processing " + quarantined.size() + " quarantined items");
      }

      if (writeBatching && writeBatchSize > 0) {
          processBatchedOperations(quarantined);
      } else {
          processSingleOperation(quarantined);

      }
  }

  private void processBatchedOperations(List<SingleOperation> quarantined) {
      final int batchSize = determineBatchSize(quarantined);

      // create batches that are separated by operation type
      final Map<SingleOperationType, List<SingleOperation>> separatedItemsPerType =
              new TreeMap<SingleOperationType, List<SingleOperation>>();
      for (int i = 0; i < batchSize; i++) {
          final SingleOperation item = quarantined.get(i);

          if (LOGGER.isLoggable(Level.CONFIG)) {
              LOGGER.config(getThreadName() + " : processItems() : adding " + item + " to next batch");
          }

          List<SingleOperation> itemsPerType = separatedItemsPerType.get(item.getType());
          if (null == itemsPerType) {
              itemsPerType = new ArrayList<SingleOperation>();
              separatedItemsPerType.put(item.getType(), itemsPerType);
          }

          itemsPerType.add(item);
      }

      // execute the batch operations
      for (List<SingleOperation> itemsPerType : separatedItemsPerType.values()) {
          int executionsLeft = retryAttempts + 1;
          while (executionsLeft-- > 0) {
              try {
                  itemsPerType.get(0).createBatchOperation(itemsPerType).performBatchOperation(cacheWriter);
                  break;
              } catch (final RuntimeException e) {
                  if (executionsLeft <= 0) {
                      for (SingleOperation singleOperation : itemsPerType) {
                          singleOperation.throwAway(cacheWriter, e);
                      }
                  } else {
                      LOGGER.warning("Exception while processing write behind queue, retrying in " + retryAttemptDelaySeconds
                              + " seconds, " + executionsLeft + " retries left : " + e.getMessage());
                      try {
                          Thread.sleep(retryAttemptDelaySeconds * MS_IN_SEC);
                      } catch (InterruptedException e1) {
                          Thread.currentThread().interrupt();
                          throw e;
                      }
                  }
              }
          }
      }

      // remove the batched items
      for (int i = 0; i < batchSize; i++) {
          quarantined.remove(0);
      }

      if (!quarantined.isEmpty()) {
          reassemble(quarantined);
      }
  }

  private void processSingleOperation(List<SingleOperation> quarantined) {
      while (!quarantined.isEmpty()) {
          // process the next item
          final SingleOperation item = quarantined.get(0);
          if (LOGGER.isLoggable(Level.CONFIG)) {
              LOGGER.config(getThreadName() + " : processItems() : processing " + item);
          }

          int executionsLeft = retryAttempts + 1;
          while (executionsLeft-- > 0) {
              try {
                  item.performSingleOperation(cacheWriter);
                  break;
              } catch (final RuntimeException e) {
                  if (executionsLeft <= 0) {
                      try {
                          item.throwAway(cacheWriter, e);
                      } catch (RuntimeException runtimeException) {
                          // TODO Really only log ?!
                          // LOGGER.warning("Throwing key '" + item.getKey() + "' away triggered an Exception!", runtimeException);
                      }
                  } else {
                      LOGGER.warning("Exception while processing write behind queue, retrying in " + retryAttemptDelaySeconds
                              + " seconds, " + executionsLeft + " retries left : " + e.getMessage());
                      try {
                          Thread.sleep(retryAttemptDelaySeconds * MS_IN_SEC);
                      } catch (InterruptedException e1) {
                          Thread.currentThread().interrupt();
                          throw e;
                      }
                  }
              }
          }

          quarantined.remove(0);
      }
  }

  /**
   * {@inheritDoc}
   */
  public void write(Element element) {
      queueWriteLock.lock();
      try {
          waitForQueueSizeToDrop();
          if (stopping || stopped) {
              throw new CacheException("The element '" + element + "' couldn't be added through the write-behind queue for cache '"
                      + cacheName + "' since it's not started.");
          }
          addItem(new WriteOperation(element));
          if (getQueueSize() + 1 < maxQueueSize) {
              queueIsFull.signal();
          }
          queueIsEmpty.signal();
      } finally {
          queueWriteLock.unlock();
      }
  }

  private void waitForQueueSizeToDrop() {
      if (maxQueueSize > 0) {
          while (getQueueSize() >= maxQueueSize) {
              try {
                  queueIsFull.await();
              } catch (InterruptedException e) {
                  stop();
                  Thread.currentThread().interrupt();
              }
          }
      }
  }

  /**
   * {@inheritDoc}
   */
  public void delete(CacheEntry entry) {
      queueWriteLock.lock();
      try {
          waitForQueueSizeToDrop();
          if (stopping || stopped) {
              throw new CacheException("The entry for key '" + entry.getKey() + "' couldn't be deleted through the write-behind "
                      + "queue for cache '" + cacheName + "' since it's not started.");
          }
          addItem(new DeleteOperation(entry));
          if (getQueueSize() + 1 < maxQueueSize) {
              queueIsFull.signal();
          }
          queueIsEmpty.signal();
      } finally {
          queueWriteLock.unlock();
      }
  }

  /**
   * {@inheritDoc}
   */
  public void stop() throws CacheException {
      queueWriteLock.lock();
      try {
          if (stopped) {
              return;
          }

          stopping = true;
          queueIsEmpty.signal();
          while (!stopped) {
              queueIsStopped.await();
          }
      } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new CacheException(e);
      } finally {
          queueWriteLock.unlock();
      }
  }

  /**
   * Gets the best estimate for items in the queue still awaiting processing.
   * Not including elements currently processed
   * @return the amount of elements still awaiting processing.
   */
  public abstract long getQueueSize();

  private boolean isStopped() {
      queueReadLock.lock();
      try {
          return stopped;
      } finally {
          queueReadLock.unlock();
      }
  }

  private String getThreadName() {
      return processingThread.getName();
  }

  private void reassemble(List<SingleOperation> quarantined) {
      queueWriteLock.lock();
      try {
          if (null == quarantined) {
              return;
          }

          reinsertUnprocessedItems(quarantined);

          queueIsEmpty.signal();
      } finally {
          queueWriteLock.unlock();
      }
  }

  /**
   * Backdoor to allow killing the processing thread for testing purposes.
   */
  protected Thread getProcessingThread() {
    return processingThread;
  }
}

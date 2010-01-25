/**
 *  Copyright 2003-2009 Terracotta, Inc.
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

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheWriterConfiguration;
import net.sf.ehcache.writer.CacheWriter;
import net.sf.ehcache.writer.writebehind.operations.DeleteOperation;
import net.sf.ehcache.writer.writebehind.operations.SingleOperation;
import net.sf.ehcache.writer.writebehind.operations.WriteOperation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of write behind with a queue that is kept in non durable local heap.
 *
 * @author Geert Bevin
 * @version $Id$
 */
public class WriteBehindQueue implements WriteBehind {
    private final static Logger LOGGER = Logger.getLogger(WriteBehindQueue.class.getName());

    private final String cacheName;
    private final CacheWriterConfiguration config;
    private final long minWriteDelayMs;
    private final long maxWriteDelayMs;
    private final Thread processingThread;

    private final ReentrantReadWriteLock queueLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock queueReadLock = queueLock.readLock();
    private final ReentrantReadWriteLock.WriteLock queueWriteLock = queueLock.writeLock();
    private final Condition queueIsEmpty = queueWriteLock.newCondition();

    private final AtomicLong lastProcessing = new AtomicLong();
    private final AtomicLong lastWorkDone = new AtomicLong();
    private final AtomicBoolean busyProcessing = new AtomicBoolean(false);

    private List<SingleOperation> waiting = new ArrayList<SingleOperation>();
    private CacheWriter cacheWriter;
    private boolean cancelled = false;

    /**
     * Create a new write behind queue.
     *
     * @param config the configuration for the queue
     */
    public WriteBehindQueue(CacheConfiguration config) {
        this.cacheName = config.getName();
        this.processingThread = new Thread(new ProcessingThread(), cacheName + " write-behind");
        this.processingThread.setDaemon(true);
        this.config = config.getCacheWriterConfiguration();
        this.minWriteDelayMs = this.config.getMinWriteDelay() * 1000;
        this.maxWriteDelayMs = this.config.getMaxWriteDelay() * 1000;
    }

    public long getLastProcessing() {
        return lastProcessing.get();
    }

    /**
     * {@inheritDoc}
     */
    public void start(CacheWriter writer) {
        queueWriteLock.lock();
        try {
            this.cacheWriter = writer;

            if (processingThread.isAlive()) {
                throw new CacheException("A thread with name " + processingThread.getName() + " already exists and is still running");
            }

            processingThread.start();
        } finally {
            queueWriteLock.unlock();
        }
    }

    private final class ProcessingThread implements Runnable {
        public void run() {
            while (!isCancelled()) {

                processItems();

                final long currentLastProcessing = getLastProcessing();

                // Wait for new items or until the min write delay has expired.
                // Do not continue if the actual min write delay wasn't at least the one specified in the config
                // otherwise it's possible to create a new work list for just a couple of items in case
                // the item processor is very fast, causing a large amount of data churn.
                // However, if the write delay is expired, the processing should start immediately.
                queueWriteLock.lock();
                try {
                    try {
                        boolean delay;
                        do {
                            queueIsEmpty.await(minWriteDelayMs, TimeUnit.MILLISECONDS);
                            long actualDelay = System.currentTimeMillis() - currentLastProcessing;
                            if (actualDelay < minWriteDelayMs) {
                                delay = true;
                            } else {
                                delay = false;
                            }
                        } while (delay);
                    } catch (final InterruptedException e) {
                        // if the wait for items is interrupted, act as if the bucket was canceled
                        stop();
                        Thread.currentThread().interrupt();
                    }

                } finally {
                    queueWriteLock.unlock();
                }
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
                if (waiting.size() > 0) {
                    quarantined = waiting;
                    waiting = new ArrayList<SingleOperation>();
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
                if (LOGGER.isLoggable(Level.FINER))
                    LOGGER.finer(getThreadName() + " : processItems() : nothing to process");
                return;
            }

            // if the batching is enabled and work size is smaller than batch size, don't process anything as long as the
            // max allowed delay hasn't expired
            final long actualWriteDelay = lastProcessing.get() - lastWorkDone.get();
            if (maxWriteDelayMs > actualWriteDelay &&
                    config.getWriteBatching() && config.getWriteBatchSize() > 0 && workSize < config.getWriteBatchSize()) {
                if (LOGGER.isLoggable(Level.FINER))
                    LOGGER.finer(getThreadName() + " : processItems() : only " + workSize + " work items available, waiting for "
                            + config.getWriteBatchSize() + " items to fill up a batch");
                return;
            }

            // set some state related to this processing run
            lastWorkDone.set(System.currentTimeMillis());

            if (LOGGER.isLoggable(Level.FINER))
                LOGGER.finer(getThreadName() + " : processItems() : processing started");

            // process the quarantined items and remove them as they're processed
            try {
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

            if (LOGGER.isLoggable(Level.FINER))
                LOGGER.finer(getThreadName() + " : processItems() : processing finished");
        }
    }

    private void processQuarantinedItems(List<SingleOperation> quarantined) {
        if (LOGGER.isLoggable(Level.CONFIG))
            LOGGER.config(getThreadName() + " : processItems() : processing " + quarantined.size() + " quarantined items");

        while (!quarantined.isEmpty()) {
            if (config.getWriteBatching() && config.getWriteBatchSize() > 0) {

                processBatchedOperations(quarantined);
            } else {
                processSingleOperation(quarantined);

            }
        }
    }

    private void processBatchedOperations(List<SingleOperation> quarantined) {
        int newBatchSize = config.getWriteBatchSize();
        if (quarantined.size() < newBatchSize) {
            newBatchSize = quarantined.size();
        }

        // create batches that are separated by operation type
        final Map<Class, List<SingleOperation>> separatedItemsPerType = new HashMap<Class, List<SingleOperation>>();
        for (int i = 0; i < newBatchSize; i++) {
            final SingleOperation item = quarantined.get(i);

            if (LOGGER.isLoggable(Level.CONFIG))
                LOGGER.config(getThreadName() + " : processItems() : adding " + item + " to next batch");

            List<SingleOperation> itemsPerType = separatedItemsPerType.get(item.getClass());
            if (null == itemsPerType) {
                itemsPerType = new ArrayList<SingleOperation>();
                separatedItemsPerType.put(item.getClass(), itemsPerType);
            }

            itemsPerType.add(item);
        }

        // execute the batch operations
        for (List<SingleOperation> itemsPerType : separatedItemsPerType.values()) {
            itemsPerType.get(0).createBatchOperation(itemsPerType).performBatchOperation(cacheWriter);
        }

        // remove the batched items
        for (int i = 0; i < newBatchSize; i++) {
            quarantined.remove(0);
        }
    }

    private void processSingleOperation(List<SingleOperation> quarantined) {
        // process the next item
        final SingleOperation item = quarantined.get(0);
        if (LOGGER.isLoggable(Level.CONFIG))
            LOGGER.config(getThreadName() + " : processItems() : processing " + item);

        item.performSingleOperation(cacheWriter);

        quarantined.remove(0);
    }

    /**
     * {@inheritDoc}
     */
    public void write(Element element) {
        queueWriteLock.lock();
        try {
            waiting.add(new WriteOperation(element));
            queueIsEmpty.signal();
        } finally {
            queueWriteLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void delete(Object key) {
        queueWriteLock.lock();
        try {
            waiting.add(new DeleteOperation(key));
            queueIsEmpty.signal();
        } finally {
            queueWriteLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stop() {
        queueWriteLock.lock();
        try {
            cancelled = true;
            queueIsEmpty.signal();
        } finally {
            queueWriteLock.unlock();
        }
    }

    private boolean isCancelled() {
        queueReadLock.lock();
        try {
            return cancelled;
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

            quarantined.addAll(waiting);

            waiting = quarantined;

            queueIsEmpty.signal();
        } finally {
            queueWriteLock.unlock();
        }
    }
}

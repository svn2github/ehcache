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
package net.sf.ehcache.constructs.refreshahead;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class implements a work queue of pooled objects. You can offer a
 * stream of objects to the backing poool of threads and it will consume them
 * and hand them to the BatchWorker as a collection to be processed (batched).
 * <p>
 *
 * Essentially, it uses BatchWorker as Callable/Future with a collection argument.
 *
 * @author cschanck
 *
 * @param <W>
 */
public class ThreadedWorkQueue<W> {

    private static final int MINUTES_OF_THE_IDLE_LIFE = 5;

    /**
     * Callback class, think of it as a Runnable with an argument that is
     * a {@link Collection}.
     *
     * @author cschanck
     *
     * @param <WW>
     */
    public static interface BatchWorker<WW> {

        /**
         * Process a batch of work.
         *
         * @param collection
         */
        public void process(Collection<? extends WW> collection);
    }

    private final LinkedBlockingQueue<W> queue;
    private final ExecutorService threadPool;
    private volatile boolean isAlive;
    private final AtomicInteger offerCounter = new AtomicInteger();
    private final AtomicInteger droppedCounter = new AtomicInteger();
    private final AtomicInteger processedCounter = new AtomicInteger();
    private final BatchWorker<W> dispatcher;
    private final int batchSize;

    /**
     * Create a work queue where work is dispatched through the given dispatcher, which the specified number
     * of threads.
     *
     * @param dispatcher Thread safe dispatcher to use to dispatch work
     * @param numberOfThreads Number of parallel threads used process work from this queue
     * @param factory {@link ThreadFactory} used to create the threads
     * @param maximumQueueSize maximum backlog of work items that can be queued before items get dropped
     * @param batchSize number of items, at a maximum, to send to a dispatcher at a time.
     */
    public ThreadedWorkQueue(BatchWorker<W> dispatcher, int numberOfThreads, ThreadFactory factory, int maximumQueueSize, int batchSize) {
        threadPool = new ThreadPoolExecutor(numberOfThreads,
                numberOfThreads,
                MINUTES_OF_THE_IDLE_LIFE,
                TimeUnit.MINUTES,
                new LinkedBlockingQueue<Runnable>(),
                factory);
        this.batchSize = batchSize;
        this.dispatcher = dispatcher;
        this.isAlive = true;
        queue = new LinkedBlockingQueue<W>(maximumQueueSize);
        for (int i = 0; i < numberOfThreads; i++) {
            threadPool.submit(new Runnable() {

                @Override
                public void run() {
                    for (; isAlive();) {
                        try {
                            pullFromQueueAndDispatch();
                        } catch (Throwable t) {
                            // eat it. if it was an interrupted, we are going to bail out in a second anyway
                        }
                    }
                }
            });
        }
    }

    /**
     * Offer a work unit to queue. Might push prior work units off of the work queue,
     * dropped forever.
     *
     * @param workUnit
     */
    public void offer(W workUnit) {
        offerCounter.incrementAndGet();
        while (!queue.offer(workUnit)) {
            if (queue.poll() != null) {
                droppedCounter.incrementAndGet();
            }
        }
    }

    /**
     * Is this work queue still accepting work.
     *
     * @return true if still alive
     */
    public boolean isAlive() {
        return isAlive;
    }

    /**
     * Get the current backlog count. An approximation, by necessity.
     *
     * @return count of items yet to be processed.
     */
    public long getBacklogCount() {
        return (offerCounter.get() - (processedCounter.get() + droppedCounter.get()));
    }

   /**
    * Gets offer counter. Cumulative tripped
    *
    * @return the offer counter
    */
   public int getOfferedCount() {
        return offerCounter.get();
    }

   /**
    * Gets dropped counter.
    *
    * @return the dropped counter
    */
    public int getDroppedCount() {
        return droppedCounter.get();
    }

   /**
    * Gets processed count.
    *
    * @return the processed count
    */
   public int getProcessedCount() {
       return processedCounter.get();
    }

   /**
     * get the dispatcher being used for this queue.
     *
     * @return dispatcher
     */
    public BatchWorker<W> getDispatcher() {
        return dispatcher;
    }

    /**
     * Get the batch size
     *
     * @return batch size
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * Shutdown this queue. Propagates an interrupt to currently executing {@link BatchWorker} threads.
     */
    public void shutdown() {
        isAlive = false;
        threadPool.shutdownNow();
        queue.clear();
    }

    /**
     * Actually do the work.
     *
     * @throws InterruptedException
     */
    private void pullFromQueueAndDispatch() throws InterruptedException {
        Collection<W> batch = new ArrayList<W>(getBatchSize());
        int currentCount = 0;
        for (W r = queue.take(); r != null; r = queue.poll()) {
            batch.add(r);
            if (++currentCount >= getBatchSize()) {
                break;
            }
        }
        // work to do and alive to do it
        if (currentCount > 0 && isAlive()) {
            processedCounter.addAndGet(batch.size());
            getDispatcher().process(batch);
        }
    }

}

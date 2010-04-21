/**
 *  Copyright 2003-2010 Terracotta, Inc.
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

package net.sf.ehcache.constructs.nonstop;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;

/**
 * Class used by NonStopCache for executing tasks within a timeout limit.
 * 
 * @author Abhishek Sanoujam
 * 
 */
public class NonStopCacheExecutorService {

    /**
     * Counter used for maintaining number of threads created by default ThreadFactory
     */
    protected static final AtomicInteger DEFAULT_FACTORY_COUNT = new AtomicInteger();

    /**
     * Default number of threads in the thread pool
     */
    public static final int DEFAULT_THREAD_POOL_SIZE = 10;

    private final ExecutorService executorService;
    private final AtomicInteger attachedCachesCount = new AtomicInteger();
    private final DisposeListener disposeListener;

    // shutdown executor service when all attached caches are dispose'd -- by default true
    private volatile boolean shutdownWhenNoCachesAttached = true;

    /**
     * Default constructor, uses {@link NonStopCacheExecutorService#DEFAULT_THREAD_POOL_SIZE} number of threads in the pool
     */
    public NonStopCacheExecutorService() {
        this(DEFAULT_THREAD_POOL_SIZE);
    }

    /**
     * Constructor accepting the maximum number of threads that can be present in the thread pool
     * 
     * @param threadPoolSize
     */
    public NonStopCacheExecutorService(final int threadPoolSize) {
        this(threadPoolSize, new ThreadFactory() {

            private final AtomicInteger counter = new AtomicInteger();

            public Thread newThread(final Runnable runnable) {
                return new Thread(runnable, "Default " + NonStopCacheExecutorService.class.getName() + "-"
                        + DEFAULT_FACTORY_COUNT.incrementAndGet() + " Executor Thread-" + counter.incrementAndGet());
            }
        });
    }

    /**
     * Constructor accepting a {@link ThreadFactory} that will be used to create threads for the pool
     * 
     * @param threadFactory
     */
    public NonStopCacheExecutorService(final ThreadFactory threadFactory) {
        this(DEFAULT_THREAD_POOL_SIZE, threadFactory);
    }

    /**
     * Constructor accepting both number of threads and the thread factory to be used
     * 
     * @param threadPoolSize
     * @param threadFactory
     */
    public NonStopCacheExecutorService(final int threadPoolSize, final ThreadFactory threadFactory) {
        // keepAlive time and maxPoolSize is ignored (does not have any effect) as we are using an unbounded work queue
        this(new ThreadPoolExecutor(threadPoolSize, threadPoolSize, Integer.MAX_VALUE, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), threadFactory));
    }

    /**
     * This constructor is private as executorService's are shut down when all associated caches are disposed. Accepting executorServices
     * from outside and shutting it down may hamper other app tasks scheduled using same executor service.
     * 
     * @param executorService
     */
    private NonStopCacheExecutorService(final ExecutorService executorService) {
        if (executorService == null) {
            throw new IllegalArgumentException("ExecutorService cannot be null");
        }
        this.executorService = executorService;
        this.disposeListener = new DisposeListener();
    }

    /**
     * Execute a {@link Callable} task with timeout. If the task does not complete within the timeout specified, throws a
     * {@link TimeoutException}
     * 
     * @param <V>
     * @param callable
     * @param timeoutValueInMillis
     * @return the return value from the callable
     * @throws TimeoutException
     * @throws CacheException
     * @throws InterruptedException
     */
    public <V> V execute(final Callable<V> callable, final long timeoutValueInMillis) throws TimeoutException, CacheException,
            InterruptedException {
        V result = null;
        long startTime = System.currentTimeMillis();
        while (true) {
            try {
                result = executorService.submit(callable).get(timeoutValueInMillis, TimeUnit.MILLISECONDS);
                break;
            } catch (InterruptedException e) {
                // XXX: do something here?
                throw e;
            } catch (RejectedExecutionException e) {
                // if the executor rejects (too many tasks executing), try until timed out
                long now = System.currentTimeMillis();
                if (now - startTime > timeoutValueInMillis) {
                    // TODO: throw another sub-class indicating job was never scheduled ?
                    throw new TimeoutException();
                } else {
                    continue;
                }
            } catch (ExecutionException e) {
                throw new CacheException(e.getCause());
            } catch (TimeoutException e) {
                // rethrow timeout exception
                throw e;
            }
        }
        return result;
    }

    /**
     * Associates a cache with this {@link NonStopCacheExecutorService}. The thread pool in {@link NonStopCacheExecutorService} shuts down
     * once all the {@link Ehcache}'s associated with this {@link NonStopCacheExecutorService} are disposed
     * 
     * @param cache
     */
    public void attachCache(Ehcache cache) {
        cache.getCacheEventNotificationService().registerListener(disposeListener);
        attachedCachesCount.incrementAndGet();
    }

    private void attachedCacheDisposed() {
        if (attachedCachesCount.decrementAndGet() == 0 && shutdownWhenNoCachesAttached) {
            executorService.shutdown();
        }
    }

    /**
     * Set whether to shutdown or not the thread pool when all associated {@link Ehcache}'s are disposed. By default its true
     * 
     * @param shutdownWhenNoCachesAttached
     *            if true, shuts down the thread pool when no cache is associated with this {@link NonStopCacheExecutorService}
     */
    public void setShutdownWhenNoCachesAttached(boolean shutdownWhenNoCachesAttached) {
        this.shutdownWhenNoCachesAttached = shutdownWhenNoCachesAttached;
    }

    /**
     * package protected method -- used for testing only
     * 
     * @return
     */
    ExecutorService getExecutorService() {
        return this.executorService;
    }

    /**
     * Private listener class for Cache.dispose()
     * 
     * @author Abhishek Sanoujam
     * 
     */
    private class DisposeListener implements CacheEventListener {

        /**
         * Call back to the {@link NonStopCacheExecutorService} that cache has disposed
         */
        public void dispose() {
            attachedCacheDisposed();
        }

        @Override
        /**
         * {@inheritDoc}.
         * Throws CloneNotSupportedException
         */
        public Object clone() throws CloneNotSupportedException {
            throw new CloneNotSupportedException();
        }

        /**
         * no-op
         */
        public void notifyElementEvicted(Ehcache cache, Element element) {
            // no-op
        }

        /**
         * no-op
         */
        public void notifyElementExpired(Ehcache cache, Element element) {
            // no-op
        }

        /**
         * no-op
         */
        public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
            // no-op
        }

        /**
         * no-op
         */
        public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
            // no-op
        }

        /**
         * no-op
         */
        public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
            // no-op
        }

        /**
         * no-op
         */
        public void notifyRemoveAll(Ehcache cache) {
            // no-op
        }
    }
}

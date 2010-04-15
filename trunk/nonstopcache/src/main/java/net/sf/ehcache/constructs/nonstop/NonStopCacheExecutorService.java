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

public class NonStopCacheExecutorService {

    protected static final AtomicInteger DEFAULT_FACTORY_COUNT = new AtomicInteger();
    public static final int DEFAULT_THREAD_POOL_SIZE = 10;

    private final ExecutorService executorService;
    private final AtomicInteger attachedCachesCount = new AtomicInteger();
    private final DisposeListener disposeListener;

    // shutdown executor service when all attached caches are dispose'd -- by default true
    private volatile boolean shutdownWhenNoCachesAttached = true;

    public NonStopCacheExecutorService() {
        this(DEFAULT_THREAD_POOL_SIZE);
    }

    public NonStopCacheExecutorService(final int threadPoolSize) {
        this(threadPoolSize, new ThreadFactory() {

            private final AtomicInteger counter = new AtomicInteger();

            public Thread newThread(final Runnable runnable) {
                return new Thread(runnable, "Default " + NonStopCacheExecutorService.class.getName() + "-"
                        + DEFAULT_FACTORY_COUNT.incrementAndGet() + " Executor Thread-" + counter.incrementAndGet());
            }
        });
    }

    public NonStopCacheExecutorService(final ThreadFactory threadFactory) {
        this(DEFAULT_THREAD_POOL_SIZE, threadFactory);
    }

    public NonStopCacheExecutorService(final int threadPoolSize, final ThreadFactory threadFactory) {
        // keepAlive time and maxPoolSize is ignored (does not have any effect) as we are using an unbounded work queue
        this(new ThreadPoolExecutor(threadPoolSize, threadPoolSize, Integer.MAX_VALUE, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), threadFactory));
    }

    public NonStopCacheExecutorService(final ExecutorService executorService) {
        if (executorService == null) {
            throw new IllegalArgumentException("ExecutorService cannot be null");
        }
        this.executorService = executorService;
        this.disposeListener = new DisposeListener();
    }

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

    public void attachCache(Ehcache cache) {
        cache.getCacheEventNotificationService().registerListener(disposeListener);
        attachedCachesCount.incrementAndGet();
    }

    private void attachedCacheDisposed() {
        if (attachedCachesCount.decrementAndGet() == 0 && shutdownWhenNoCachesAttached) {
            executorService.shutdown();
        }
    }

    public void setShutdownWhenNoCachesAttached(boolean shutdownWhenNoCachesAttached) {
        this.shutdownWhenNoCachesAttached = shutdownWhenNoCachesAttached;
    }

    // package protected method -- used for testing only
    ExecutorService getExecutorService() {
        return this.executorService;
    }

    private class DisposeListener implements CacheEventListener {

        public void dispose() {
            attachedCacheDisposed();
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            throw new CloneNotSupportedException();
        }

        public void notifyElementEvicted(Ehcache cache, Element element) {
            // no-op
        }

        public void notifyElementExpired(Ehcache cache, Element element) {
            // no-op
        }

        public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
            // no-op
        }

        public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
            // no-op
        }

        public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
            // no-op
        }

        public void notifyRemoveAll(Ehcache cache) {
            // no-op
        }
    }
}

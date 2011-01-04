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

import java.util.concurrent.BlockingQueue;
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
import net.sf.ehcache.constructs.nonstop.util.CountingThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class used by NonStopCache for executing tasks within a timeout limit.
 *
 * @author Abhishek Sanoujam
 *
 */
public class NonstopExecutorServiceImpl implements NonstopExecutorService {

    /**
     * Counter used for maintaining number of threads created by default ThreadFactory
     */
    protected static final AtomicInteger DEFAULT_FACTORY_COUNT = new AtomicInteger();

    /**
     * A string that is a part of the thread name created by the default thread factory.
     * Package protected as used by tests.
     */
    static final String EXECUTOR_THREAD_NAME_PREFIX = "Executor Thread";

    /**
     * Property name for default value for max threads pool size
     */
    static final String DEFAULT_MAX_THREAD_POOL_SIZE_PROPERTY = "net.sf.ehcache.constructs.nonstop.defaultMaxThreadPoolSize";

    /**
     * Property name for default value for core threads pool size
     */
    static final String DEFAULT_CORE_THREAD_POOL_SIZE_PROPERTY = "net.sf.ehcache.constructs.nonstop.defaultCoreThreadPoolSize";

    /**
     * Default number of threads in the thread pool
     */
    static final int DEFAULT_CORE_THREAD_POOL_SIZE = getProperty(DEFAULT_CORE_THREAD_POOL_SIZE_PROPERTY, 10);

    /**
     * Default number of maximum threads that can be in the pool.
     * Package protected as used by tests.
     */
    static final int DEFAULT_MAX_THREAD_POOL_SIZE = getProperty(DEFAULT_MAX_THREAD_POOL_SIZE_PROPERTY, 500);

    private static final Logger LOGGER = LoggerFactory.getLogger(NonstopExecutorServiceImpl.class);

    private final ThreadPoolExecutor executorService;
    private final AtomicInteger attachedCachesCount = new AtomicInteger();

    // shutdown executor service when all attached caches are dispose'd -- by default true
    private volatile boolean shutdownWhenNoCachesAttached = true;

    private CountingThreadFactory countingThreadFactory;

    private BlockingQueue<Runnable> taskQueue;

    private volatile int maxPoolSize;

    /**
     * Default constructor, uses {@link NonstopExecutorServiceImpl#DEFAULT_CORE_THREAD_POOL_SIZE} number of threads in the pool
     */
    public NonstopExecutorServiceImpl() {
        this(DEFAULT_CORE_THREAD_POOL_SIZE, DEFAULT_MAX_THREAD_POOL_SIZE);
    }

    /**
     * Constructor accepting the maximum number of threads that can be present in the thread pool
     *
     * @param coreThreadPoolSize
     */
    public NonstopExecutorServiceImpl(final int coreThreadPoolSize, final int maxThreadPoolSize) {
        this(coreThreadPoolSize, maxThreadPoolSize, new ThreadFactory() {

            private final AtomicInteger counter = new AtomicInteger();

            public Thread newThread(final Runnable runnable) {
                Thread thread = new Thread(runnable, "Default " + NonstopExecutorServiceImpl.class.getSimpleName() + " Thread Factory-"
                        + DEFAULT_FACTORY_COUNT.incrementAndGet() + " " + EXECUTOR_THREAD_NAME_PREFIX + "-" + counter.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    /**
     * Constructor accepting a {@link ThreadFactory} that will be used to create threads for the pool
     *
     * @param threadFactory
     */
    public NonstopExecutorServiceImpl(final ThreadFactory threadFactory) {
        this(DEFAULT_CORE_THREAD_POOL_SIZE, DEFAULT_MAX_THREAD_POOL_SIZE, threadFactory);
    }

    /**
     * Constructor accepting both number of threads and the thread factory to be used
     *
     * @param coreThreadPoolSize
     * @param threadFactory
     */
    public NonstopExecutorServiceImpl(final int coreThreadPoolSize, final int maxThreadPoolSize, final ThreadFactory threadFactory) {
        this(coreThreadPoolSize, maxThreadPoolSize, new LinkedBlockingQueue<Runnable>(), new CountingThreadFactory(threadFactory));
    }

    private NonstopExecutorServiceImpl(final int corePoolSize, final int maxPoolSize, BlockingQueue<Runnable> taskQueue,
            CountingThreadFactory countingThreadFactory) {
        this(new ThreadPoolExecutor(corePoolSize, maxPoolSize, Integer.MAX_VALUE, TimeUnit.MILLISECONDS, taskQueue, countingThreadFactory));
        this.maxPoolSize = maxPoolSize;
        this.taskQueue = taskQueue;
        this.countingThreadFactory = countingThreadFactory;
    }

    /**
     * This constructor is private as executorService's are shut down when all associated caches are disposed. Accepting executorServices
     * from outside and shutting it down may hamper other app tasks scheduled using same executor service.
     *
     * @param executorService
     */
    private NonstopExecutorServiceImpl(final ThreadPoolExecutor executorService) {
        if (executorService == null) {
            throw new IllegalArgumentException("ExecutorService cannot be null");
        }
        this.executorService = executorService;
    }

    private static int getProperty(String propertyName, int defaultValue) {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue == null || "".equals(propertyValue.trim())) {
            return defaultValue;
        }
        int value = 0;
        try {
            value = Integer.parseInt(propertyValue);
        } catch (NumberFormatException e) {
            value = defaultValue;
            LOGGER.warn("Invalid value specified for property \"" + propertyName + "\"=" + propertyValue + ", using default value: "
                    + defaultValue);
        }
        return value;
    }

    /**
     * Used in tests only.
     *
     * @return the task queue of the executor
     */
    BlockingQueue<Runnable> getTaskQueue() {
        return taskQueue;
    }

    /**
     * {@inheritDoc}
     */
    public <V> V execute(final Callable<V> callable, final long timeoutValueInMillis) throws TimeoutException, CacheException,
            InterruptedException {
        int attempt = 0;
        V result = null;
        long startTime = System.nanoTime();
        while (true) {
            try {
                attempt++;
                if (countingThreadFactory != null && countingThreadFactory.getNumberOfThreads() < maxPoolSize) {
                    synchronized (executorService) {
                        if (countingThreadFactory.getNumberOfThreads() < maxPoolSize) {
                            executorService.setCorePoolSize(incrementCorePoolSize(executorService.getCorePoolSize()));
                        }
                    }
                }
                result = executorService.submit(callable).get(timeoutValueInMillis, TimeUnit.MILLISECONDS);
                break;
            } catch (InterruptedException e) {
                // XXX: do something here?
                throw e;
            } catch (RejectedExecutionException e) {
                // if the executor rejects (too many tasks executing), try until timed out
                long now = System.nanoTime();
                if (now - startTime > TimeUnit.NANOSECONDS.convert(timeoutValueInMillis, TimeUnit.MILLISECONDS)) {
                    throw new TaskNotSubmittedTimeoutException(attempt);
                } else {
                    continue;
                }
            } catch (ExecutionException e) {
                Throwable rootCause = getRootCause(e);
                if (rootCause.getClass().getSimpleName().equals("TCNotRunningException")) {
                    throw new TimeoutException(rootCause.getMessage());
                }

                if (e.getCause() instanceof CacheException) {
                    throw (CacheException) e.getCause();
                }

                throw new CacheException(e.getCause());
            } catch (TimeoutException e) {
                // rethrow timeout exception
                throw e;
            }
        }
        return result;
    }

    private Throwable getRootCause(final Throwable exception) {
        Throwable e = exception;
        while (e.getCause() != null) {
            e = e.getCause();
        }
        return e;
    }

    private int incrementCorePoolSize(int currentCorePoolSize) {
        int rv = 2 * currentCorePoolSize;
        return rv > maxPoolSize ? maxPoolSize : rv;
    }

    /**
     * {@inheritDoc}
     */
    public void shutdown() {
        executorService.shutdownNow();
    }

    /**
     * package protected method -- used for testing only
     *
     * @return
     */
    ExecutorService getExecutorService() {
        return this.executorService;
    }
}

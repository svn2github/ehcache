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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A thread pool that creates another thread pool per requesting thread.
 *
 * @author Abhishek Sanoujam
 *
 */
public class NonstopThreadPool {

    private final ThreadFactory threadFactory;
    private final List<WorkerThreadLocal> workers = new ArrayList<WorkerThreadLocal>();
    private final Object workersLock = new Object();
    private final AtomicReference<State> state = new AtomicReference<State>(State.RUNNING);
    private final ThreadLocal<WorkerThreadLocal> workerThreadLocal = new ThreadLocal<WorkerThreadLocal>() {
        @Override
        protected WorkerThreadLocal initialValue() {
            WorkerThreadLocal local = new WorkerThreadLocal(threadFactory);
            synchronized (workersLock) {
                if (state.get() == State.SHUTDOWN) {
                    rejectExecutionAfterShutdown();
                }
                workers.add(local);
            }
            return local;
        }

    };

    /**
     * Constructor accepting the threadFactory
     *
     * @param threadFactory
     */
    public NonstopThreadPool(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
    }

    private void rejectExecutionAfterShutdown() {
        throw new RejectedExecutionException("The thread pool has already shut down.");
    }

    /**
     * Submit a callable task to be executed by the thread pool
     *
     * @param <T>
     * @param task
     * @return Future of the task
     */
    public <T> Future<T> submit(Callable<T> task) {
        if (task == null) {
            throw new NullPointerException("Task cannot be null");
        }
        return workerThreadLocal.get().submit(task);
    }

    /**
     * Shuts down the thread pool
     */
    public void shutdownNow() {
        state.set(State.SHUTDOWN);
        synchronized (workersLock) {
            for (WorkerThreadLocal worker : workers) {
                worker.shutdown();
            }
        }
    }

    /**
     * Private class
     *
     * @author Abhishek Sanoujam
     *
     */
    private static class WorkerThreadLocal {
        private static final int CORE_POOL_SIZE_PER_APP_THREAD = 1;
        private static final int MAXIMUM_POOL_SIZE_PER_APP_THREAD = 50;
        private static final int POOL_THREAD_PER_APP_THREAD_KEEP_ALIVE_SECONDS = 300;

        private final ThreadPoolExecutor threadPoolExecutor;

        public WorkerThreadLocal(ThreadFactory threadFactory) {
            // using a synchrounous queue here
            // Maximum of MAXIMUM_POOL_SIZE_PER_APP_THREAD requests can be accomodated from one app thread
            this.threadPoolExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE_PER_APP_THREAD, MAXIMUM_POOL_SIZE_PER_APP_THREAD,
                    POOL_THREAD_PER_APP_THREAD_KEEP_ALIVE_SECONDS, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), threadFactory);
        }

        public void shutdown() {
            this.threadPoolExecutor.shutdownNow();
        }

        public <T> Future<T> submit(Callable<T> task) {
            return threadPoolExecutor.submit(task);
        }

    }

    /**
     * Private enum maintaining state of the pool
     *
     * @author Abhishek Sanoujam
     *
     */
    private static enum State {
        RUNNING, SHUTDOWN;
    }

}

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
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
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
                worker.shutdownNow();
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

        private final Worker worker;

        public WorkerThreadLocal(ThreadFactory threadFactory) {
            this.worker = new Worker();
            threadFactory.newThread(worker).start();
        }

        public void shutdownNow() {
            worker.shutdownNow();
        }

        public <T> Future<T> submit(Callable<T> task) {
            FutureTask<T> ftask = new FutureTask<T>(task);
            worker.addTask(ftask);
            return ftask;
        }
    }

    /**
     * Worker class
     *
     * @author Abhishek Sanoujam
     *
     */
    private static class Worker implements Runnable {
        private final WorkerTaskHolder workerTaskHolder;
        private volatile boolean shutdown;
        private volatile boolean runningTask;
        private volatile Thread workerThread;

        public Worker() {
            this.workerTaskHolder = new WorkerTaskHolder();
        }

        public void run() {
            workerThread = Thread.currentThread();
            while (!shutdown) {
                waitUntilTaskAvailable();
                if (shutdown) {
                    break;
                }
                Runnable task = workerTaskHolder.consumeTask();
                if (task != null) {
                    runningTask = true;
                    if (Thread.currentThread().isInterrupted()) {
                        continue;
                    }
                    task.run();
                    runningTask = false;
                }
            }
        }

        public void shutdownNow() {
            shutdown = true;
            synchronized (this) {
                this.notifyAll();
            }
            if (runningTask) {
                // interrupt thread if still running task
                // XXX: fix small race where task not yet running, can miss the interrupt
                workerThread.interrupt();
            }
        }

        public void addTask(Runnable runnable) {
            synchronized (this) {
                workerTaskHolder.addTask(runnable);
                this.notifyAll();
            }
        }

        private void waitUntilTaskAvailable() {
            synchronized (this) {
                while (!workerTaskHolder.isTaskAvailable()) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
            }
        }
    }

    /**
     * Private class maintaining single pending task
     *
     * @author Abhishek Sanoujam
     *
     */
    private static class WorkerTaskHolder {
        private Runnable task;

        public synchronized void addTask(Runnable runnable) {
            // keep only 1 pending task
            this.task = runnable;
        }

        public synchronized Runnable consumeTask() {
            if (task == null) {
                return null;
            }
            Runnable rv = task;
            task = null;
            return rv;
        }

        public synchronized boolean isTaskAvailable() {
            return task != null;
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

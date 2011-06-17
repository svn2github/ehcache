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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.Map.Entry;
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

    private static final long POLL_TIME_MILLIS = 1000;
    private static final long NUM_OF_POLLS_BEFORE_CHECK_THREADS_ALIVE = 100;

    private final ThreadFactory threadFactory;
    private final Map<Thread, WorkerThreadLocal> workers = new WeakHashMap<Thread, WorkerThreadLocal>();
    private final Object workersLock = new Object();
    private final AtomicReference<State> state = new AtomicReference<State>(State.RUNNING);
    private final ReferenceQueue<Thread> gcedThreadsReferenceQueue = new ReferenceQueue<Thread>();
    private final ThreadLocal<WorkerThreadLocal> workerThreadLocal = new ThreadLocal<WorkerThreadLocal>() {
        @Override
        protected WorkerThreadLocal initialValue() {
            WorkerThreadLocal local = new WorkerThreadLocal(threadFactory, gcedThreadsReferenceQueue);
            synchronized (workersLock) {
                if (state.get() == State.SHUTDOWN) {
                    rejectExecutionAfterShutdown();
                }
                workers.put(Thread.currentThread(), local);
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
        startReaperThread();
    }

    private void startReaperThread() {
        Thread reaperThread = new Thread(new ReaperThread(), "non stop reaper thread");
        reaperThread.start();
    }

    /**
     * class which manages the alive non stop threads
     *
     * @author Raghvendra Singh
     *
     */
    private class ReaperThread implements Runnable {

        public void run() {
            int pollCount = 0;
            while (state.get() != State.SHUTDOWN) {
                WeakWorkerReference gcedThreadReference = null;
                try {
                    gcedThreadReference = (WeakWorkerReference) gcedThreadsReferenceQueue.remove(POLL_TIME_MILLIS);
                    // check if threads are alive every 10 loop and shut them down
                    if (++pollCount == NUM_OF_POLLS_BEFORE_CHECK_THREADS_ALIVE) {
                        Set<Thread> deadThreads = new HashSet<Thread>();
                        pollCount = 0;
                        synchronized (workersLock) {
                            for (Entry<Thread, WorkerThreadLocal> entry : workers.entrySet()) {
                                if (!entry.getKey().isAlive()) {
                                    entry.getValue().shutdownNow();
                                    deadThreads.add(entry.getKey());
                                }
                            }

                            for (Thread th : deadThreads) {
                                workers.remove(th);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    // ignored
                }
                if (gcedThreadReference != null) {
                    gcedThreadReference.getWorker().shutdownNow();
                }
            }
        }
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
            for (WorkerThreadLocal worker : workers.values()) {
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
        private final WeakWorkerReference appThreadReference;

        public WorkerThreadLocal(ThreadFactory threadFactory, ReferenceQueue<Thread> gcedThreadsReferenceQueue) {
            this.worker = new Worker();
            threadFactory.newThread(worker).start();
            this.appThreadReference = new WeakWorkerReference(this.worker, Thread.currentThread(), gcedThreadsReferenceQueue);
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
        private volatile Thread workerThread;
        private volatile boolean runningTask;

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
                    synchronized (this) {
                        runningTask = true;
                        if (shutdown) {
                            break;
                        }
                    }
                    task.run();
                    synchronized (this) {
                        runningTask = false;
                    }
                }
            }
        }

        public void shutdownNow() {
            shutdown = true;
            synchronized (this) {
                this.notifyAll();
                if (runningTask) {
                    // interrupt if running task already
                    workerThread.interrupt();
                }
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
                    if (shutdown) {
                        return;
                    }
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
     * private class maintaining the app thread and its corresponding worker thread
     *
     * @author Raghvendra Singh
     */
    private static class WeakWorkerReference extends WeakReference<Thread> {

        private final Worker worker;

        public WeakWorkerReference(Worker worker, Thread referent, ReferenceQueue<? super Thread> q) {
            super(referent, q);
            this.worker = worker;
        }

        public Worker getWorker() {
            return this.worker;
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

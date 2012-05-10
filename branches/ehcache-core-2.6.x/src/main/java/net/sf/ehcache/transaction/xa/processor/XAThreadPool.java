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
package net.sf.ehcache.transaction.xa.processor;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fast and minimalistic thread pool from which threads can be reserved and used many times until they
 * are manually released.
 *
 * @author Ludovic Orban
 */
public class XAThreadPool {

    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Reserve a thread from the pool
     * @return a MultiRunner which wraps the reserved thread
     */
    public synchronized MultiRunner getMultiRunner() {
        MultiRunner multiRunner = new MultiRunner();
        executor.submit(multiRunner);
        return multiRunner;
    }

    /**
     * Shutdown the thread pool and release all resources
     */
    public synchronized void shutdown() {
        executor.shutdown();
    }

    /**
     * Pooled thread wrapper which allows reuse of the same thread
     */
    public static final class MultiRunner implements Runnable {
        private final CyclicBarrier startBarrier = new CyclicBarrier(2);
        private final CyclicBarrier endBarrier = new CyclicBarrier(2);
        private volatile Callable callable;
        private volatile boolean released;
        private volatile Object result;
        private volatile Exception exception;

        private MultiRunner() {
        }

        /**
         * Execute a Callable on the wrapped thread and return its result
         * @param callable The Callable to execute
         * @return the Object returned by the Callable
         * @throws ExecutionException thrown when something went wrong during execution
         * @throws InterruptedException thrown when the executing thread got interrupted
         */
        public Object execute(Callable callable) throws ExecutionException, InterruptedException {
            if (released) {
                throw new IllegalStateException("MultiRunner has been released");
            }
            if (callable == null) {
                throw new NullPointerException("callable cannot be null");
            }

            try {
                this.callable = callable;
                this.exception = null;
                startBarrier.await();

                endBarrier.await();
                if (exception != null) {
                    throw new ExecutionException("XA execution error", exception);
                }
                return result;
            } catch (BrokenBarrierException e) {
                throw new ExecutionException("error executing " + callable, e);
            }
        }

        /**
         * Release the wrapped thread back the the containing thread pool
         */
        public void release() {
            try {
                callable = null;
                released = true;
                startBarrier.await();
            } catch (InterruptedException e) {
                // ignore
            } catch (BrokenBarrierException e) {
                // ignore
            }
        }

        /**
         * {@inheritDoc}
         */
        public void run() {
            try {
                while (true) {
                    startBarrier.await();

                    if (callable != null) {
                        try {
                            result = callable.call();
                        } catch (Exception e) {
                            exception = e;
                        }
                        endBarrier.await();
                    } else {
                        return;
                    }
                }
            } catch (InterruptedException e) {
                released = true;
            } catch (BrokenBarrierException e) {
                released = true;
            }
        }
    }

}

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

package net.sf.ehcache.constructs.nonstop.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A custom {@link ThreadFactory} that maintains a count of how many threads this factory has created
 *
 * @author Abhishek Sanoujam
 *
 */
public class CountingThreadFactory implements ThreadFactory {

    private final AtomicInteger count = new AtomicInteger();
    private final ThreadFactory actualFactory;

    /**
     * Constructor accepting the actual thread factory that will create the threads
     *
     * @param actualFactory
     *            the actual factory
     */
    public CountingThreadFactory(ThreadFactory actualFactory) {
        this.actualFactory = actualFactory;
    }

    /**
     * {@inheritDoc}
     */
    public Thread newThread(Runnable r) {
        Thread newThread = actualFactory.newThread(new RunnableWithLifeCycle(this, r));
        if (newThread != null) {
            count.incrementAndGet();
        }
        return newThread;
    }

    /**
     * Returns the number of threads this factory has created and currently alive
     *
     * @return Returns the number of threads this factory has created and currently alive
     */
    public int getNumberOfThreads() {
        return count.get();
    }

    private void threadExecutionComplete() {
        count.decrementAndGet();
    }

    /**
     * Runnable which also fires lifecycle events
     *
     * @author Abhishek Sanoujam
     *
     */
    private static class RunnableWithLifeCycle implements Runnable {

        private final Runnable actualRunnable;
        private final CountingThreadFactory countingThreadFactory;

        /**
         * Constructor accepting a {@link CountingThreadFactory} and the actual runnable
         *
         * @param countingThreadFactory
         * @param actualRunnable
         */
        public RunnableWithLifeCycle(CountingThreadFactory countingThreadFactory, Runnable actualRunnable) {
            super();
            this.countingThreadFactory = countingThreadFactory;
            this.actualRunnable = actualRunnable;
        }

        /**
         * {@inheritDoc}
         */
        public void run() {
            try {
                if (actualRunnable != null) {
                    actualRunnable.run();
                }
            } finally {
                countingThreadFactory.threadExecutionComplete();
            }
        }

    }

}

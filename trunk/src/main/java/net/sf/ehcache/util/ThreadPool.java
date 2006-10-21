/**
 *  Copyright 2003-2006 Greg Luck
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
package net.sf.ehcache.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.ehcache.config.ThreadPoolConfiguration;

/**
 * A simple thread pool implementation 
 * 
 * @author Jody Brownell
 * @since 1.2.4
 * @version $Id$
 */
public class ThreadPool {
    private static final Log LOG = LogFactory.getLog(ThreadPool.class.getName());

    private boolean alive;
    private final List poolOfThreads;
    private final BlockingQueue queue;
    private final ThreadPoolConfiguration configuration;

    /**
     * Create a pool from the threadPoolConfiguration.
     * 
     * @param threadPoolConfiguration the thread pool threadPoolConfiguration
     */
    public ThreadPool(final ThreadPoolConfiguration threadPoolConfiguration) {
        this.configuration = threadPoolConfiguration;

        queue = new BlockingQueue();
        poolOfThreads = new ArrayList(threadPoolConfiguration.getThreads().intValue());

        PoolThread thread;
        for (int i = 0; i < threadPoolConfiguration.getThreads().intValue(); i++) {
            thread = new PoolThread(this.queue);
            thread.setPriority(threadPoolConfiguration.getPriority().intValue());
            thread.setName("ehcache/pools/" + this.configuration.getName()
                    + "[" + (i + 1) + "]");

            thread.setDaemon(true);
            thread.start();

            poolOfThreads.add(thread);
        }
        alive = true;
    }

    /**
     * Submit a task to the queue for eventual execution.
     * 
     * @param task the task to execute
     * @throws InterruptedException if the task is added, but the queue is full and dispose is called
     */
    public void submit(Runnable task) throws InterruptedException {
        if (!alive) {
            return;
        }

        if (task == null) {
            return;
        }

        queue.add(task);
    }

    /**
     * Destroys this thread pool.
     */
    public synchronized void dispose() {
        if (alive) {
            PoolThread thread;
            Iterator threadsIterator = this.poolOfThreads.iterator();
            while (threadsIterator.hasNext()) {
                queue.clear();

                thread = (PoolThread) threadsIterator.next();
                thread.interrupt();

                threadsIterator.remove();
            }

            alive = false;
        }
    }

    /**
     * The configuration for this thread pool
     */
    public ThreadPoolConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * A rudimentry task executor.
     * 
     * @author Jody Brownell
     * @version $Id$
     * @since 1.2.4
     */
    private static final class PoolThread extends Thread {

        private final BlockingQueue queue;

        PoolThread(BlockingQueue queue) {
            this.queue = queue;
        }

        public void run() {
            Runnable latest;
            while (!isInterrupted()) {
                try {
                    latest = (Runnable) queue.remove();

                    // just to be sure - bail when we can
                    if (isInterrupted()) {
                        return;
                    }

                    // once we start the task, do not allow it to be interrupted
                    if (latest != null) {
                        latest.run();
                    }
                } catch (InterruptedException e) {
                    break;

                } catch (Throwable e) {
                    LOG.error("Caught exception executing task", e);
                }
            }
        }
    }
}

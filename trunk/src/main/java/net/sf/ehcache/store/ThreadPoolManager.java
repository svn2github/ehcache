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
package net.sf.ehcache.store;

import net.sf.ehcache.config.ConfigurationHelper;
import net.sf.ehcache.util.ThreadPool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Timer;
import java.util.TimerTask;

/**
 * A manger for Thread Pools. Each CacheManager has one ThreadPoolManager.
 *
 * @author Jody Brownell
 * @author Greg Luck
 * @version $Id$
 * @since 1.2.4
 */
public final class ThreadPoolManager {


    private static final Log LOG = LogFactory.getLog(ThreadPoolManager.class.getName());

    private boolean alive;
    private final Timer timer;
    private final ThreadPool diskStoreSpoolingThreadPool;
    private final ThreadPool diskStoreExpiryThreadPool;


    /**
     * Create an instance of the cache services facade.
     *
     * @param helper  the configuration for the cache instance
     */
    public ThreadPoolManager(ConfigurationHelper helper) {
        timer = new Timer(true);
        diskStoreSpoolingThreadPool = helper.createDiskStoreSpoolingThreadPool();
        diskStoreExpiryThreadPool = helper.createDiskStoreExpiryThreadPool();
        alive = true;
    }


    /**
     * Destroy this instance of Resources and all of its services.
     */
    public synchronized void dispose() {
        checkAlive();
        timer.cancel();
        alive = false;
        diskStoreSpoolingThreadPool.dispose();
        diskStoreExpiryThreadPool.dispose();
    }

    /**
     * @see Timer#schedule(TimerTask,long)
     */
    public synchronized void scheduleTask(TimerTask task, long millis) throws IllegalArgumentException {
        checkAlive();
        if (task == null) {
            throw new IllegalArgumentException("Expecting non null TimerTask");
        }
        timer.schedule(task, millis);
    }

    /**
     * Check to see this Resources instance is available for use.
     */
    protected final void checkAlive() {
        if (!alive) {
            throw new IllegalStateException("ThreadPoolManager no longer active");
        }
    }

    /**
     * Submit a Runnable task for processing.
     *
     * @param task     the task to execute in the pool
     * @throws InterruptedException     if the job is submitted, and blocked in the pool queue, then the thread interrupted.
     * @throws IllegalArgumentException if poolName or task is null OR the pool cannot be found
     */
    public synchronized void executeExpiry(Runnable task) throws InterruptedException, IllegalArgumentException {
        checkAlive();
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        diskStoreExpiryThreadPool.submit(task);
    }

    /**
     * Submit a Runnable task for processing.
     *
     * @param task     the task to execute in the pool
     * @throws InterruptedException     if the job is submitted, and blocked in the pool queue, then the thread interrupted.
     * @throws IllegalArgumentException if poolName or task is null OR the pool cannot be found
     */
    public synchronized void executeSpool(Runnable task) throws InterruptedException, IllegalArgumentException {
        checkAlive();
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        diskStoreSpoolingThreadPool.submit(task);
    }





}

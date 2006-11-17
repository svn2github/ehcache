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

package net.sf.ehcache.config;

/**
 * A class to represent a thread pool resource.
 * 
 * @author Jody Brownell
 * @author Greg Luck
 * @since 1.2.4
 * @version $Id$
 */
public class ThreadPoolConfiguration {


    /**
     * the name of the pool
     */
    protected String name;

    /**
     * The priority of the threads in the pool
     */
    protected Integer priority;

    /**
     * The maximum size of the queue before a task is blocked. The default is virtually non-blocking.
     */
    protected int taskQueueSize = Integer.MAX_VALUE;

    /**
     * The number of threads in the pool
     */
    protected Integer threads;

    /**
     * Empty constructor
     */
    public ThreadPoolConfiguration() {

    }

    /**
     * Full constructor
     * @param name
     * @param priority
     * @param threads
     */
    public ThreadPoolConfiguration(String name, Integer priority, Integer threads) {
        this.name = name;
        this.priority = priority;
        this.threads = threads;
    }


    /**
     * Get the name of the pool
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the pool
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the number of threads in the pool
     */
    public Integer getThreads() {
        return threads;
    }

    /**
     * Set the number of threads in the pool
     * @param threads the number of threads to allocate in the pool
     * @throws IllegalArgumentException if threads < 1
     */
    public void setThreads(Integer threads) throws IllegalArgumentException {
         if (threads != null && threads.intValue() < 1) {
            throw new IllegalArgumentException("Threads must be greater than 0");
        }
        this.threads = threads;
    }

    /**
     * Get the priority of the threads in the pool.
     */
    public Integer getPriority() {
        return priority;
    }

    /**
     * Set the priority which threads in the pool
     * 
     * @param priority between {@link Thread#MIN_PRIORITY} and {@link Thread#MAX_PRIORITY}
     * @throws IllegalArgumentException of the value is not in the expected range
     */
    public void setPriority(Integer priority) throws IllegalArgumentException {
        if (priority != null && (priority.intValue() < Thread.MIN_PRIORITY || priority.intValue() > Thread.MAX_PRIORITY)) {
            throw new IllegalArgumentException("Expecting thread priority >= "
                + Thread.MIN_PRIORITY + " and <= " + Thread.MAX_PRIORITY);
        }
        this.priority = priority;
    }

    /**
     * Get the size of the queue in front of the thread pool
     */
    public int getTaskQueueSize() {
        return taskQueueSize;
    }

    /**
     * Set the number of elements in the queue before a submit will block
     */
    public void setTaskQueueSize(int taskQueueSize) {
        this.taskQueueSize = taskQueueSize;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "name: " + this.name + ", count: " + this.threads
                + ", priority: " + this.priority + ", queueSize: "
                + this.taskQueueSize;
    }
}

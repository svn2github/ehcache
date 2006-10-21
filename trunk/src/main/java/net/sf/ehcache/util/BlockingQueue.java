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

import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * A blocking queue mechanism backed by a LinkedHashSet which delegates the uniqueness policy to 
 * the elements which are added. If uniqueness is important, override to hashcode/equals to enforce it.
 * If frequency is important, use hashcode/equals from Object. 
 * 
 * @author Jody Brownell
 * @since 1.2.4
 * @version $Id$
 */
public class BlockingQueue {
    private int capacity;
    private LinkedHashSet backingCollection;

    /**
     * Default constuctor. Create a Queue with a maxium size of Integer.MAX_VALUE.
     */
    public BlockingQueue() {
        this(Integer.MAX_VALUE);
    }

    /**
     * Create a queue with a maximum capacity
     * 
     * @param capacity the maximum number of elements it will hold
     */
    public BlockingQueue(int capacity) {
        this.capacity = capacity;
        backingCollection = new LinkedHashSet();
    }

    /**
     * Remove an object from the queue. If there is nothing in the queue, the calling thread is blocked until an item is
     * added to the queue or interupt is called on the thread.
     * 
     * @return the first object available in the Queue
     * @throws InterruptedException if interupt is called on the blocking thread
     */
    public synchronized Object remove() throws InterruptedException {
        while (backingCollection.size() == 0) {
            wait();
        }

        Iterator iter = backingCollection.iterator();
        iter.hasNext();

        Object o = iter.next();
        
        // DOH - dont forget to remove the element. Runaway thread otherwise
        iter.remove();

        // All contenders must be notified
        notifyAll();

        return o;
    }

    /**
     * Add an object to the queue. If the number of items in the queue is equal to the maximum capacity, the calling
     * thread will be blocked until the item can be added or interupt is called on the calling thread.
     * 
     * @param o the object to add to the back of the queue
     * @throws InterruptedException if interupt is called on the blocking thread
     */
    public synchronized void add(Object o) throws InterruptedException {
        while (backingCollection.size() >= capacity) {
            wait();
        }

        boolean changed = backingCollection.add(o);

        // All contenders must be notified but only if an element has been added
        if (changed) {
            notifyAll();
        }
    }

    /**
     * Get the number of elements currently stored in the queue.
     * 
     * @return the number of elements
     */
    public int size() {
        return backingCollection.size();
    }

    /**
     * Return the maximum number of elements before this queue will block the next time {@link BlockingQueue#add(Object)}is
     * called
     * 
     * @return the queue capacity
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Clear the queue of all its contents.
     */
    public synchronized void clear() {
        // dont clear/notify threads if there is no reason to do so
        if (backingCollection.size() > 0) {
            backingCollection.clear();

            // All contenders must be notified if something has changed
            notifyAll();
        }
    }
}
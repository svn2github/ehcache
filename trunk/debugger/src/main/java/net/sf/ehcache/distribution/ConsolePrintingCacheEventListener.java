/**
 *  Copyright 2003-2009 Terracotta, Inc.
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


package net.sf.ehcache.distribution;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;

/**
 * Prints cache event messages to the console.
 *
 * @author Greg Luck
 * @version $Id: CountingCacheEventListener.java 519 2007-07-27 07:11:45Z gregluck $
 */
public class ConsolePrintingCacheEventListener implements CacheEventListener {

    private volatile int eventsReceivedCount;

    /**
     * Called immediately after an attempt to remove an element. The remove method will block until
     * this method returns.
     * <p/>
     * This notification is received regardless of whether the cache had an element matching
     * the removal key or not. If an element was removed, the element is passed to this method,
     * otherwise a synthetic element, with only the key set is passed in.
     * <p/>
     * This notification is not called for the following special cases:
     * <ol>
     * <li>removeAll was called. See {@link #notifyRemoveAll(net.sf.ehcache.Ehcache)}
     * <li>An element was evicted from the cache.
     * See {@link #notifyElementEvicted(net.sf.ehcache.Ehcache, net.sf.ehcache.Element)}
     * </ol>
     *
     * @param cache   the cache emitting the notification
     * @param element the element just deleted, or a synthetic element with just the key set if
     *                no element was removed.
     */
    public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
        Object key = null;
        if (element != null) {
            key = element.getKey();
        }
        eventsReceivedCount++;
        System.out.println("Received remove notification for element " + key);
    }

    /**
     * Called immediately after an element has been put into the cache. The
     * {@link net.sf.ehcache.Cache#put(net.sf.ehcache.Element)} method
     * will block until this method returns.
     * <p/>
     * Implementers may wish to have access to the Element's fields, including value, so the
     * element is provided. Implementers should be careful not to modify the element. The
     * effect of any modifications is undefined.
     *
     * @param cache   the cache emitting the notification
     * @param element the element which was just put into the cache.
     */
    public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
        eventsReceivedCount++;
        System.out.println("Received put notification for element " + element);
    }

    /**
     * Called immediately after an element has been put into the cache and the element already
     * existed in the cache. This is thus an update.
     * <p/>
     * The {@link net.sf.ehcache.Cache#put(net.sf.ehcache.Element)} method
     * will block until this method returns.
     * <p/>
     * Implementers may wish to have access to the Element's fields, including value, so the
     * element is provided. Implementers should be careful not to modify the element. The
     * effect of any modifications is undefined.
     *
     * @param cache   the cache emitting the notification
     * @param element the element which was just put into the cache.
     */
    public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
        eventsReceivedCount++;
        System.out.println("Received update notification for element " + element);
    }

    /**
     * Called immediately after an element is <i>found</i> to be expired. The
     * {@link net.sf.ehcache.Cache#remove(Object)} method will block until this method returns.
     * <p/>
     * As the {@link net.sf.ehcache.Element} has been expired, only what was the key of the element is known.
     * <p/>
     * Elements are checked for expiry in ehcache at the following times:
     * <ul>
     * <li>When a get request is made
     * <li>When an element is spooled to the diskStore in accordance with a MemoryStore
     * eviction policy
     * <li>In the DiskStore when the expiry thread runs, which by default is
     * {@link net.sf.ehcache.Cache#DEFAULT_EXPIRY_THREAD_INTERVAL_SECONDS}
     * </ul>
     * If an element is found to be expired, it is deleted and this method is notified.
     *
     * @param cache   the cache emitting the notification
     * @param element the element that has just expired
     *                <p/>
     *                Deadlock Warning: expiry will often come from the <code>DiskStore</code>
     *                expiry thread. It holds a lock to the DiskStorea the time the
     *                notification is sent. If the implementation of this method calls into a
     *                synchronized <code>Cache</code> method and that subsequently calls into
     *                DiskStore a deadlock will result. Accordingly implementers of this method
     *                should not call back into Cache.
     */
    public void notifyElementExpired(Ehcache cache, Element element) {
        Object key = null;
        if (element != null) {
            key = element.getKey();
        }
        eventsReceivedCount++;
        System.out.println("Received expired notification for element " + key);
    }

    /**
     * Called immediately after an element is evicted from the cache. Evicted in this sense
     * means evicted from one store and not moved to another, so that it exists nowhere in the
     * local cache.
     * <p/>
     * In a sense the Element has been <i>removed</i> from the cache, but it is different,
     * thus the separate notification.
     *
     * @param cache   the cache emitting the notification
     * @param element the element that has just been evicted
     */
    public void notifyElementEvicted(Ehcache cache, Element element) {
        Object key = null;
        if (element != null) {
            key = element.getKey();
        }
        eventsReceivedCount++;
        System.out.println("Received eviction notification for element " + key);

    }

    /**
     * Called during {@link net.sf.ehcache.Ehcache#removeAll()} to indicate that the all
     * elements have been removed from the cache in a bulk operation. The usual
     * {@link #notifyElementRemoved(net.sf.ehcache.Ehcache, net.sf.ehcache.Element)}
     * is not called.
     * <p/>
     * This notification exists because clearing a cache is a special case. It is often
     * not practical to serially process notifications where potentially millions of elements
     * have been bulk deleted.
     *
     * @param cache the cache emitting the notification
     */
    public void notifyRemoveAll(Ehcache cache) {
        eventsReceivedCount++;
        System.out.println("Received removeAll notification.");
    }

    /**
     * Give the listener a chance to cleanup and free resources when no longer needed
     */
    public void dispose() {
        //nothing required
    }

    /**
     * Creates a clone of this listener. This method will only be called by ehcache before a
     * cache is initialized.
     * <p/>
     * This may not be possible for listeners after they have been initialized. Implementations
     * should throw CloneNotSupportedException if they do not support clone.
     *
     * @return a clone
     * @throws CloneNotSupportedException if the listener could not be cloned.
     */
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Not supported");
    }

    /**
     * @return how many events have been received
     */
    public int getEventsReceivedCount() {
        return eventsReceivedCount;
    }
}


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

package net.sf.ehcache.event;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.CacheException;

/**
 * Allows implementers to register callback methods that will be executed when a cache event occurs.
 * The events include:
 * <ol>
 * <li>put Element
 * <li>update Element
 * <li>remove Element
 * <li>an Element expires, either because timeToLive or timeToIdle has been reached.
 * </ol>
 * <p/>
 * Callbacks to these methods are synchronous and unsynchronized. It is the responsibility of the implementer
 * to safely handle the potential performance and thread safety issues depending on what their listener is doing.
 * <p/>
 * Events are guaranteed to be notified in the order in which they occurred.
 * <p/>
 * Cache also has putQuiet and removeQuiet methods which do not notify listeners.
 *
 * @author Greg Luck
 * @version $Id$
 * @see CacheManagerEventListener
 * @since 1.2
 */
public interface CacheEventListener {

    /**
     * Called immediately after an element has been removed. The remove method will block until
     * this method returns.
     * <p/>
     * Ehcache does not chech for
     * <p/>
     * As the {@link net.sf.ehcache.Element} has been removed, only what was the key of the element is known.
     * <p/>
     *
     * @param cache   the cache emitting the notification
     * @param element just deleted
     */
    void notifyElementRemoved(final Cache cache, final Element element) throws CacheException;

    /**
     * Called immediately after an element has been put into the cache. The {@link net.sf.ehcache.Cache#put(net.sf.ehcache.Element)} method
     * will block until this method returns.
     * <p/>
     * Implementers may wish to have access to the Element's fields, including value, so the element is provided.
     * Implementers should be careful not to modify the element. The effect of any modifications is undefined.
     *
     * @param cache   the cache emitting the notification
     * @param element the element which was just put into the cache.
     */
    void notifyElementPut(final Cache cache, final Element element) throws CacheException;

    /**
     * Called immediately after an element has been put into the cache and the element already
     * existed in the cache. This is thus an update.
     * <p/>
     * The {@link net.sf.ehcache.Cache#put(net.sf.ehcache.Element)} method
     * will block until this method returns.
     * <p/>
     * Implementers may wish to have access to the Element's fields, including value, so the element is provided.
     * Implementers should be careful not to modify the element. The effect of any modifications is undefined.
     *
     * @param cache   the cache emitting the notification
     * @param element the element which was just put into the cache.
     */
    void notifyElementUpdated(final Cache cache, final Element element) throws CacheException;


    /**
     * Called immediately after an element is <i>found</i> to be expired. The
     * {@link net.sf.ehcache.Cache#remove(Object)} method will block until this method returns.
     * <p/>
     * As the {@link Element} has been expired, only what was the key of the element is known.
     * <p/>
     * Elements are checked for expiry in ehcache at the following times:
     * <ul>
     * <li>When a get request is made
     * <li>When an element is spooled to the diskStore in accordance with a MemoryStore eviction policy
     * <li>In the DiskStore when the expiry thread runs, which by default is
     * {@link net.sf.ehcache.Cache#DEFAULT_EXPIRY_THREAD_INTERVAL_SECONDS}
     * </ul>
     * If an element is found to be expired, it is deleted and this method is notified.
     *
     * @param cache   the cache emitting the notification
     * @param element the element that has just expired
     *                <p/>
     *                Deadlock Warning: expiry will often come from the <code>DiskStore</code> expiry thread. It holds a lock to the
     *                DiskStorea the time the notification is sent. If the implementation of this method calls into a
     *                synchronized <code>Cache</code> method and that subsequently calls into DiskStore a deadlock will result.
     *                Accordingly implementers of this method should not call back into Cache.
     */
    void notifyElementExpired(final Cache cache, final Element element);


    /**
     * Give the replicator a chance to cleanup and free resources when no longer needed
     */
    void dispose();
}

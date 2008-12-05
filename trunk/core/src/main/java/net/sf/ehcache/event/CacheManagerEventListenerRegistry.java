/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
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

import net.sf.ehcache.Status;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Registered listeners for registering and unregistering CacheManagerEventListeners and sending notifications to registrants.
 * <p/>
 * There is one of these per CacheManager. It is a composite listener.
 *
 * @author Greg Luck
 * @version $Id$
 * @since 1.3
 */
public class CacheManagerEventListenerRegistry implements CacheManagerEventListener {

    private Status status;

    /**
     * A Set of CacheEventListeners keyed by listener instance.
     * CacheEventListener implementations that will be notified of this cache's events.
     *
     * @see CacheManagerEventListener
     */
    private Set listeners;

    /**
     * Construct a new registry
     */
    public CacheManagerEventListenerRegistry() {
        status = Status.STATUS_UNINITIALISED;
        listeners = new HashSet();
    }

    /**
     * Adds a listener to the notification service. No guarantee is made that listeners will be
     * notified in the order they were added.
     *
     * @param cacheManagerEventListener the listener to add. Can be null, in which case nothing happens
     * @return true if the listener is being added and was not already added
     */
    public final boolean registerListener(CacheManagerEventListener cacheManagerEventListener) {
        if (cacheManagerEventListener == null) {
            return false;
        }
        return listeners.add(cacheManagerEventListener);
    }

    /**
     * Removes a listener from the notification service.
     *
     * @param cacheManagerEventListener the listener to remove
     * @return true if the listener was present
     */
    public final boolean unregisterListener(CacheManagerEventListener cacheManagerEventListener) {
        return listeners.remove(cacheManagerEventListener);
    }

    /**
     * Returns whether or not at least one cache manager event listeners has been registered.
     *
     * @return true if a one or more listeners have registered, otherwise false
     */
    public boolean hasRegisteredListeners() {
        return listeners.size() > 0;
    }

    /**
     * Gets a Set of the listeners registered to this class
     *
     * @return a set of type <code>CacheManagerEventListener</code>
     */
    public Set getRegisteredListeners() {
        return listeners;
    }

    /**
     * Initialises the listeners, ready to receive events.
     */
    public void init() {
        //init once
        Iterator iterator = listeners.iterator();
        while (iterator.hasNext()) {
            CacheManagerEventListener cacheManagerEventListener = (CacheManagerEventListener) iterator.next();
            cacheManagerEventListener.init();
        }
    }

    /**
     * Returns the listener status.
     *
     * @return the status at the point in time the method is called
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Tell listeners to dispose themselves.
     * Because this method is only ever called from a synchronized cache method, it does not itself need to be
     * synchronized.
     */
    public void dispose() {
        Iterator iterator = listeners.iterator();
        while (iterator.hasNext()) {
            CacheManagerEventListener cacheManagerEventListener = (CacheManagerEventListener) iterator.next();
            cacheManagerEventListener.dispose();
        }
        listeners.clear();
    }

    /**
     * Called immediately after a cache has been added and activated.
     * <p/>
     * Note that the CacheManager calls this method from a synchronized method. Any attempt to
     * call a synchronized method on CacheManager from this method will cause a deadlock.
     * <p/>
     * Note that activation will also cause a CacheEventListener status change notification
     * from {@link net.sf.ehcache.Status#STATUS_UNINITIALISED} to
     * {@link net.sf.ehcache.Status#STATUS_ALIVE}. Care should be taken on processing that
     * notification because:
     * <ul>
     * <li>the cache will not yet be accessible from the CacheManager.
     * <li>the addCaches methods which cause this notification are synchronized on the
     * CacheManager. An attempt to call {@link net.sf.ehcache.CacheManager#getEhcache(String)}
     * will cause a deadlock.
     * </ul>
     * The calling method will block until this method returns.
     * <p/>
     *
     * @param cacheName the name of the <code>Cache</code> the operation relates to
     * @see CacheEventListener
     */
    public void notifyCacheAdded(String cacheName) {
        Iterator iterator = listeners.iterator();
        while (iterator.hasNext()) {
            CacheManagerEventListener cacheManagerEventListener = (CacheManagerEventListener) iterator.next();
            cacheManagerEventListener.notifyCacheAdded(cacheName);
        }
    }

    /**
     * Called immediately after a cache has been disposed and removed. The calling method will
     * block until this method returns.
     * <p/>
     * Note that the CacheManager calls this method from a synchronized method. Any attempt to
     * call a synchronized method on CacheManager from this method will cause a deadlock.
     * <p/>
     * Note that a {@link CacheEventListener} status changed will also be triggered. Any
     * attempt from that notification to access CacheManager will also result in a deadlock.
     *
     * @param cacheName the name of the <code>Cache</code> the operation relates to
     */
    public void notifyCacheRemoved(String cacheName) {
        Iterator iterator = listeners.iterator();
        while (iterator.hasNext()) {
            CacheManagerEventListener cacheManagerEventListener = (CacheManagerEventListener) iterator.next();
            cacheManagerEventListener.notifyCacheRemoved(cacheName);
        }
    }

    /**
     * Returns a string representation of the object. In general, the
     * <code>toString</code> method returns a string that
     * "textually represents" this object. The result should
     * be a concise but informative representation that is easy for a
     * person to read.
     *
     * @return a string representation of the object.
     */
    public String toString() {
        StringBuffer stringBuffer = new StringBuffer(" cacheManagerEventListeners: ");
        for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
            CacheManagerEventListener cacheManagerEventListener = (CacheManagerEventListener) iterator.next();
            stringBuffer.append(cacheManagerEventListener.getClass().getName()).append(" ");
        }
        return stringBuffer.toString();
    }
}

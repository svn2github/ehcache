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
import net.sf.ehcache.distribution.CacheReplicator;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Registered listeners for registering and unregistering CacheEventListeners and multicasting notifications to registrants.
 * <p/>
 * There is one of these per Cache
 *
 * @author Greg Luck
 * @version $Id: RegisteredEventListeners.java,v 1.1 2006/03/09 06:38:19 gregluck Exp $
 */
public class RegisteredEventListeners {

    /**
     * A Map of CacheEventListeners keyed by listener class.
     * CacheEventListener implementations that will be notified of this cache's events.
     *
     * @see CacheEventListener
     */
    private Set cacheEventListeners = new HashSet();
    private Cache cache;

    /**
     * Constructs a new notification service
     *
     * @param cache
     */
    public RegisteredEventListeners(Cache cache) {
        this.cache = cache;
    }


    /**
     * Notifies all registered listeners, in no guaranteed order, that an element was removed
     *
     * @param element
     * @param remoteEvent whether the event came from a remote cache peer
     * @see CacheEventListener#notifyElementRemoved
     */
    public void notifyElementRemoved(Element element, boolean remoteEvent) throws CacheException {
        Iterator iterator = cacheEventListeners.iterator();
        while (iterator.hasNext()) {
            CacheEventListener cacheEventListener = (CacheEventListener) iterator.next();
            if (!isCircularNotification(remoteEvent, cacheEventListener)) {
                cacheEventListener.notifyElementRemoved(cache, element);
            }
        }
    }

    /**
     * Notifies all registered listeners, in no guaranteed order, that an element was put into the cache
     *
     * @param element
     * @param remoteEvent whether the event came from a remote cache peer
     * @see CacheEventListener#notifyElementPut(net.sf.ehcache.Cache,net.sf.ehcache.Element)
     */
    public void notifyElementPut(Element element, boolean remoteEvent) throws CacheException {
        Iterator iterator = cacheEventListeners.iterator();
        while (iterator.hasNext()) {
            CacheEventListener cacheEventListener = (CacheEventListener) iterator.next();
            if (!isCircularNotification(remoteEvent, cacheEventListener)) {
                cacheEventListener.notifyElementPut(cache, element);
            }
        }
    }

    /**
     * Notifies all registered listeners, in no guaranteed order, that an element in the cache was updated
     *
     * @param element
     * @param remoteEvent whether the event came from a remote cache peer
     * @see CacheEventListener#notifyElementPut(net.sf.ehcache.Cache,net.sf.ehcache.Element)
     */
    public void notifyElementUpdated(Element element, boolean remoteEvent) {
        Iterator iterator = cacheEventListeners.iterator();
        while (iterator.hasNext()) {
            CacheEventListener cacheEventListener = (CacheEventListener) iterator.next();
            if (!isCircularNotification(remoteEvent, cacheEventListener)) {
                cacheEventListener.notifyElementUpdated(cache, element);
            }
        }
    }

    /**
     * Notifies all registered listeners, in no guaranteed order, that an element has expired
     *
     * @param element
     * @param remoteEvent whether the event came from a remote cache peer
     * @see CacheEventListener#notifyElementExpired
     */
    public void notifyElementExpiry(Element element, boolean remoteEvent) {
        Iterator iterator = cacheEventListeners.iterator();
        while (iterator.hasNext()) {
            CacheEventListener cacheEventListener = (CacheEventListener) iterator.next();
            if (!isCircularNotification(remoteEvent, cacheEventListener)) {
                cacheEventListener.notifyElementExpired(cache, element);
            }
        }
    }


    /**
     * CacheReplicators should not be notified of events received remotely, as this would cause
     * a circular notification
     *
     * @param remoteEvent
     * @param cacheEventListener
     * @return true is notifiying the listener would cause a circular notification
     */
    private boolean isCircularNotification(boolean remoteEvent, CacheEventListener cacheEventListener) {
        return remoteEvent && cacheEventListener instanceof CacheReplicator;
    }


    /**
     * Adds a listener to the notification service. No guarantee is made that listeners will be
     * notified in the order they were added.
     *
     * @param cacheEventListener
     * @return true if the listener is being added and was not already added
     */
    public boolean registerListener(CacheEventListener cacheEventListener) {
        if (cacheEventListener == null) {
            return false;
        }
        return cacheEventListeners.add(cacheEventListener);
    }

    /**
     * Removes a listener from the notification service.
     *
     * @param cacheEventListener
     * @return true if the listener was present
     */
    public boolean unregisterListener(CacheEventListener cacheEventListener) {
        return cacheEventListeners.remove(cacheEventListener);
    }

    /**
     * Gets a list of the listeners registered to this class
     *
     * @return a list of type <code>CacheEventListener</code>
     */
    public Set getCacheEventListeners() {
        return cacheEventListeners;
    }

    /**
     * Tell listeners to dispose themselves.
     * Because this method is only ever called from a synchronized cache method, it does not itself need to be
     * synchronized.
     */
    public void dispose() {
        Iterator iterator = cacheEventListeners.iterator();
        while (iterator.hasNext()) {
            CacheEventListener cacheEventListener = (CacheEventListener) iterator.next();
            cacheEventListener.dispose();
        }

        cacheEventListeners.clear();
    }
}

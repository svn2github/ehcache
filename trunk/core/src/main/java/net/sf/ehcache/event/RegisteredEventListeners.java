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

package net.sf.ehcache.event;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.distribution.CacheReplicator;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Registered listeners for registering and unregistering CacheEventListeners and multicasting notifications to registrants.
 * <p/>
 * There is one of these per Cache.
 * <p/>
 * This class also has counters to accumulate the numbers of each type of event for statistics purposes.
 *
 * @author Greg Luck
 * @version $Id$
 */
public class RegisteredEventListeners {

    /**
     * A Set of CacheEventListeners keyed by listener instance.
     * CacheEventListener implementations that will be notified of this cache's events.
     *
     * @see CacheEventListener
     */
    private final Set cacheEventListeners = new HashSet();
    private final Ehcache cache;

    private long elementsRemovedCounter;
    private long elementsPutCounter;
    private long elementsUpdatedCounter;
    private long elementsExpiredCounter;
    private long elementsEvictedCounter;
    private long elementsRemoveAllCounter;

    /**
     * Constructs a new notification service
     *
     * @param cache
     */
    public RegisteredEventListeners(Ehcache cache) {
        this.cache = cache;
    }


    /**
     * Notifies all registered listeners, in no guaranteed order, that an element was removed
     *
     * @param element
     * @param remoteEvent whether the event came from a remote cache peer
     * @see CacheEventListener#notifyElementRemoved
     */
    public final void notifyElementRemoved(Element element, boolean remoteEvent) throws CacheException {
        elementsRemovedCounter++;
        if (hasCacheEventListeners()) {
            Iterator iterator = createThreadSafeIterator();
            while (iterator.hasNext()) {
                CacheEventListener cacheEventListener = (CacheEventListener) iterator.next();
                if (!isCircularNotification(remoteEvent, cacheEventListener)) {
                    cacheEventListener.notifyElementRemoved(cache, element);
                }
            }
        }
    }

    /**
     * Notifies all registered listeners, in no guaranteed order, that an element was put into the cache
     *
     * @param element
     * @param remoteEvent whether the event came from a remote cache peer
     * @see CacheEventListener#notifyElementPut(net.sf.ehcache.Ehcache,net.sf.ehcache.Element)
     */
    public final void notifyElementPut(Element element, boolean remoteEvent) throws CacheException {
        elementsPutCounter++;
        if (hasCacheEventListeners()) {
            Iterator iterator = createThreadSafeIterator();
            while (iterator.hasNext()) {
                CacheEventListener cacheEventListener = (CacheEventListener) iterator.next();
                if (!isCircularNotification(remoteEvent, cacheEventListener)) {
                    cacheEventListener.notifyElementPut(cache, element);
                }
            }

        }
    }

    /**
     * Notifies all registered listeners, in no guaranteed order, that an element in the cache was updated
     *
     * @param element
     * @param remoteEvent whether the event came from a remote cache peer
     * @see CacheEventListener#notifyElementPut(net.sf.ehcache.Ehcache,net.sf.ehcache.Element)
     */
    public final void notifyElementUpdated(Element element, boolean remoteEvent) {
        elementsUpdatedCounter++;
        if (hasCacheEventListeners()) {
            Iterator iterator = createThreadSafeIterator();

            while (iterator.hasNext()) {
                CacheEventListener cacheEventListener = (CacheEventListener) iterator.next();
                if (!isCircularNotification(remoteEvent, cacheEventListener)) {
                    cacheEventListener.notifyElementUpdated(cache, element);
                }
            }
        }
    }

    /**
     * Notifies all registered listeners, in no guaranteed order, that an element has expired
     *
     * @param element     the Element to perform the notification on
     * @param remoteEvent whether the event came from a remote cache peer
     * @see CacheEventListener#notifyElementExpired
     */
    public final void notifyElementExpiry(Element element, boolean remoteEvent) {
        elementsExpiredCounter++;
        if (hasCacheEventListeners()) {
            Iterator iterator = createThreadSafeIterator();
            while (iterator.hasNext()) {
                CacheEventListener cacheEventListener = (CacheEventListener) iterator.next();
                if (!isCircularNotification(remoteEvent, cacheEventListener)) {
                    cacheEventListener.notifyElementExpired(cache, element);
                }
            }
        }
    }

    /**
     * Returns whether or not at least one cache event listeners has been registered.
     *
     * @return true if a one or more listeners have registered, otherwise false
     */
    public final boolean hasCacheEventListeners() {
        return cacheEventListeners.size() > 0;
    }

    /**
     * Notifies all registered listeners, in no guaranteed order, that an element has been
     * evicted from the cache
     *
     * @param element     the Element to perform the notification on
     * @param remoteEvent whether the event came from a remote cache peer
     * @see CacheEventListener#notifyElementEvicted
     */
    public void notifyElementEvicted(Element element, boolean remoteEvent) {
        elementsEvictedCounter++;
        if (hasCacheEventListeners()) {
            Iterator iterator = createThreadSafeIterator();
            while (iterator.hasNext()) {
                CacheEventListener cacheEventListener = (CacheEventListener) iterator.next();
                if (!isCircularNotification(remoteEvent, cacheEventListener)) {
                    cacheEventListener.notifyElementEvicted(cache, element);
                }
            }
        }
    }


    /**
     * Notifies all registered listeners, in no guaranteed order, that removeAll
     * has been called and all elements cleared
     *
     * @param remoteEvent whether the event came from a remote cache peer
     * @see CacheEventListener#notifyElementEvicted
     */
    public void notifyRemoveAll(boolean remoteEvent) {
        elementsRemoveAllCounter++;
        if (hasCacheEventListeners()) {
            Iterator iterator = createThreadSafeIterator();
            while (iterator.hasNext()) {
                CacheEventListener cacheEventListener = (CacheEventListener) iterator.next();
                if (!isCircularNotification(remoteEvent, cacheEventListener)) {
                    cacheEventListener.notifyRemoveAll(cache);
                }
            }
        }
    }

    private Iterator createThreadSafeIterator() {
        HashSet copy;
        synchronized (cacheEventListeners) {
            copy = new HashSet(cacheEventListeners);
        }
        return copy.iterator();
    }

    /**
     * CacheReplicators should not be notified of events received remotely, as this would cause
     * a circular notification
     *
     * @param remoteEvent
     * @param cacheEventListener
     * @return true is notifiying the listener would cause a circular notification
     */
    private static boolean isCircularNotification(boolean remoteEvent, CacheEventListener cacheEventListener) {
        return remoteEvent && cacheEventListener instanceof CacheReplicator;
    }


    /**
     * Adds a listener to the notification service. No guarantee is made that listeners will be
     * notified in the order they were added.
     *
     * @param cacheEventListener
     * @return true if the listener is being added and was not already added
     */
    public final boolean registerListener(CacheEventListener cacheEventListener) {
        if (cacheEventListener == null) {
            return false;
        }
        boolean added;
        synchronized (cacheEventListeners) {
            added = cacheEventListeners.add(cacheEventListener);
        }
        return added;
    }

    /**
     * Removes a listener from the notification service.
     *
     * @param cacheEventListener
     * @return true if the listener was present
     */
    public final boolean unregisterListener(CacheEventListener cacheEventListener) {
        boolean removed;
        synchronized (cacheEventListeners) {
            removed = cacheEventListeners.remove(cacheEventListener);
        }
        return removed;
    }


    /**
     * Gets a list of the listeners registered to this class
     *
     * @return a list of type <code>CacheEventListener</code>
     */
    public final Set getCacheEventListeners() {
        return cacheEventListeners;
    }

    /**
     * Tell listeners to dispose themselves.
     * Because this method is only ever called from a synchronized cache method, it does not itself need to be
     * synchronized.
     */
    public final void dispose() {
        Iterator iterator = createThreadSafeIterator();
        while (iterator.hasNext()) {
            CacheEventListener cacheEventListener = (CacheEventListener) iterator.next();
            cacheEventListener.dispose();
        }

        synchronized (cacheEventListeners) {
            cacheEventListeners.clear();
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
    public final String toString() {
        StringBuffer stringBuffer = new StringBuffer(" cacheEventListeners: ");
            for (Iterator iterator = createThreadSafeIterator(); iterator.hasNext();) {
                CacheEventListener cacheEventListener = (CacheEventListener) iterator.next();
                stringBuffer.append(cacheEventListener.getClass().getName()).append(" ");
            }
        return stringBuffer.toString();
    }

    /**
     * Clears all event counters
     */
    public void clearCounters() {
        elementsRemovedCounter = 0;
        elementsPutCounter = 0;
        elementsUpdatedCounter = 0;
        elementsExpiredCounter = 0;
        elementsEvictedCounter = 0;
        elementsRemoveAllCounter = 0;
    }

    /**
     * Gets the number of events, irrespective of whether there are any registered listeners.
     *
     * @return the number of events since cache creation or last clearing of counters
     */
    public long getElementsRemovedCounter() {
        return elementsRemovedCounter;
    }

    /**
     * Gets the number of events, irrespective of whether there are any registered listeners.
     *
     * @return the number of events since cache creation or last clearing of counters
     */
    public long getElementsPutCounter() {
        return elementsPutCounter;
    }

    /**
     * Gets the number of events, irrespective of whether there are any registered listeners.
     *
     * @return the number of events since cache creation or last clearing of counters
     */
    public long getElementsUpdatedCounter() {
        return elementsUpdatedCounter;
    }

    /**
     * Gets the number of events, irrespective of whether there are any registered listeners.
     *
     * @return the number of events since cache creation or last clearing of counters
     */
    public long getElementsExpiredCounter() {
        return elementsExpiredCounter;
    }

    /**
     * Gets the number of events, irrespective of whether there are any registered listeners.
     *
     * @return the number of events since cache creation or last clearing of counters
     */
    public long getElementsEvictedCounter() {
        return elementsEvictedCounter;
    }

    /**
     * Gets the number of events, irrespective of whether there are any registered listeners.
     *
     * @return the number of events since cache creation or last clearing of counters
     */
    public long getElementsRemoveAllCounter() {
        return elementsRemoveAllCounter;
    }
}

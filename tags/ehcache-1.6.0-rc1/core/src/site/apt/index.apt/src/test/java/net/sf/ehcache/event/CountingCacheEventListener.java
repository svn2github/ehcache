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

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Counts listener notifications.
 * <p/>
 * The methods also check that we hold the Cache lock.
 *
 * @author Greg Luck
 * @version $Id$
 */
public class CountingCacheEventListener implements CacheEventListener {

    private static final List CACHE_ELEMENTS_PUT = Collections.synchronizedList(new ArrayList());
    private static final List CACHE_ELEMENTS_UPDATED = Collections.synchronizedList(new ArrayList());
    private static final List CACHE_ELEMENTS_REMOVED = Collections.synchronizedList(new ArrayList());
    private static final List CACHE_ELEMENTS_EXPIRED = Collections.synchronizedList(new ArrayList());
    private static final List CACHE_ELEMENTS_EVICTED = Collections.synchronizedList(new ArrayList());
    private static final List CACHE_REMOVE_ALLS = Collections.synchronizedList(new ArrayList());


    /**
     * Accessor
     */
    public static List getCacheElementsRemoved(Ehcache cache) {
        return extractListForGivenCache(CACHE_ELEMENTS_REMOVED, cache);
    }


    /**
     * Accessor
     */
    public static List getCacheElementsPut(Ehcache cache) {
        return extractListForGivenCache(CACHE_ELEMENTS_PUT, cache);
    }

    /**
     * Accessor
     */
    public static List getCacheElementsUpdated(Ehcache cache) {
        return extractListForGivenCache(CACHE_ELEMENTS_UPDATED, cache);
    }

    /**
     * Accessor
     */
    public static List getCacheElementsExpired(Ehcache cache) {
        return extractListForGivenCache(CACHE_ELEMENTS_EXPIRED, cache);
    }

    /**
     * Accessor
     */
    public static List getCacheElementsEvicted(Ehcache cache) {
        return extractListForGivenCache(CACHE_ELEMENTS_EVICTED, cache);
    }

    /**
     * Accessor
     */
    public static List getCacheRemoveAlls(Ehcache cache) {
        return extractListForGivenCache(CACHE_REMOVE_ALLS, cache);
    }


    /**
     * Resets the counters to 0
     */
    public static void resetCounters() {
        synchronized (CACHE_ELEMENTS_REMOVED) {
            CACHE_ELEMENTS_REMOVED.clear();
        }
        synchronized (CACHE_ELEMENTS_PUT) {
            CACHE_ELEMENTS_PUT.clear();
        }
        synchronized (CACHE_ELEMENTS_UPDATED) {
            CACHE_ELEMENTS_UPDATED.clear();
        }
        synchronized (CACHE_ELEMENTS_EXPIRED) {
            CACHE_ELEMENTS_EXPIRED.clear();
        }
        synchronized (CACHE_ELEMENTS_EVICTED) {
            CACHE_ELEMENTS_EVICTED.clear();
        }
        synchronized (CACHE_REMOVE_ALLS) {
            CACHE_REMOVE_ALLS.clear();
        }
    }


    /**
     * @param notificationList
     * @param cache            the cache to filter on. If null, there is not filtering and all entries are returned.
     * @return a list of notifications for the cache
     */
    private static List extractListForGivenCache(List notificationList, Ehcache cache) {
        ArrayList list = new ArrayList();
        synchronized (notificationList) {
            for (int i = 0; i < notificationList.size(); i++) {
                CounterEntry counterEntry = (CounterEntry) notificationList.get(i);
                if (counterEntry.cache.equals(cache)) {
                    list.add(counterEntry.getElement());
                } else if (cache == null) {
                    list.add(counterEntry.getElement());
                }
            }
        }
        return list;
    }


    /**
     * {@inheritDoc}
     */
    public void notifyElementRemoved(final Ehcache cache, final Element element) {
        checkSynchronizedAccessToCacheOk(cache);
        CACHE_ELEMENTS_REMOVED.add(new CounterEntry(cache, element));
    }

    /**
     * Called immediately after an element has been put into the cache. The {@link net.sf.ehcache.Cache#put(net.sf.ehcache.Element)} method
     * will block until this method returns.
     * <p/>
     * Implementers may wish to have access to the Element's fields, including value, so the element is provided.
     * Implementers should be careful not to modify the element. The effect of any modifications is undefined.
     *
     * @param cache
     * @param element the element which was just put into the cache.
     */
    public void notifyElementPut(final Ehcache cache, final Element element) {
        checkSynchronizedAccessToCacheOk(cache);
        CACHE_ELEMENTS_PUT.add(new CounterEntry(cache, element));
    }


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
    public void notifyElementUpdated(final Ehcache cache, final Element element) throws CacheException {
        CACHE_ELEMENTS_UPDATED.add(new CounterEntry(cache, element));
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementExpired(final Ehcache cache, final Element element) {
        CACHE_ELEMENTS_EXPIRED.add(new CounterEntry(cache, element));
    }


    /**
     * {@inheritDoc}
     */
    public void notifyElementEvicted(final Ehcache cache, final Element element) {
        CACHE_ELEMENTS_EVICTED.add(new CounterEntry(cache, element));
    }

    /**
     * {@inheritDoc}
     */
    public void notifyRemoveAll(final Ehcache cache) {
        CACHE_REMOVE_ALLS.add(new CounterEntry(cache, null));
    }

    /**
     * Give the replicator a chance to cleanup and free resources when no longer needed
     * <p/>
     * Clean up static counters
     */
    public void dispose() {
        resetCounters();
    }

    /**
     * This counter should be called from calls synchonized on Cache. These methods should hold the lock
     * therefore this is ok.
     *
     * @param cache
     */
    private void checkSynchronizedAccessToCacheOk(Ehcache cache) {
        try {
            cache.get("justasyncrhonizationtest");
        } catch (CacheException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A Counter entry
     */
    public static class CounterEntry {

        private Ehcache cache;
        private Element element;

        /**
         * Construct a new event
         *
         * @param cache
         * @param element
         */
        public CounterEntry(Ehcache cache, Element element) {
            this.cache = cache;
            this.element = element;
        }

        /**
         * @return the cache the event relates to
         */
        public Ehcache getCache() {
            return cache;
        }

        /**
         * @return the payload
         */
        public Serializable getElement() {
            return element;
        }


    }


    /**
     * Creates a clone of this listener. This method will only be called by ehcache before a cache is initialized.
     * <p/>
     * This may not be possible for listeners after they have been initialized. Implementations should throw
     * CloneNotSupportedException if they do not support clone.
     * <p/>
     * This class uses static counters. Clones will share the same counters.
     *
     * @return a clone
     * @throws CloneNotSupportedException if the listener could not be cloned.
     */
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }


}

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
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Counts listener notifications.
 * <p/>
 * The methods also check that we hold the Cache lock.
 *
 * @author Greg Luck
 * @version $Id: CountingCacheEventListener.java,v 1.1 2006/03/09 06:38:20 gregluck Exp $
 */
public class CountingCacheEventListener implements CacheEventListener {

    private static List cacheElementsPut = new ArrayList();
    private static List cacheElementsUpdated = new ArrayList();
    private static List cacheElementsRemoved = new ArrayList();
    private static List cacheElementsExpired = new ArrayList();



    /**
     * Accessor
     */
    public static List getCacheElementsRemoved(Cache cache) {
        return extractListForGivenCache(cacheElementsRemoved, cache);
    }


    /**
     * Accessor
     */
    public static List getCacheElementsPut(Cache cache) {
        return extractListForGivenCache(cacheElementsPut, cache);
    }

    /**
     * Accessor
     */
    public static List getCacheElementsUpdated(Cache cache) {
        return extractListForGivenCache(cacheElementsUpdated, cache);
    }

    /**
     * Accessor
     */
    public static List getCacheElementsExpired(Cache cache) {
        return extractListForGivenCache(cacheElementsExpired, cache);
    }

    /**
     * Resets the counters to 0
     */
    public static void resetCounters() {
        cacheElementsRemoved.clear();
        cacheElementsPut.clear();
        cacheElementsExpired.clear();
    }


    /**
     *
     * @param notificationList
     * @param cache the cache to filter on. If null, there is not filtering and all entries are returned.
     * @return a list of notifications for the cache
     */
    private static List extractListForGivenCache(List notificationList, Cache cache) {
        ArrayList list = new ArrayList();
        for (int i = 0; i < notificationList.size(); i++) {
            CounterEntry counterEntry = (CounterEntry) notificationList.get(i);
            if (counterEntry.cache.equals(cache)) {
                list.add(counterEntry.getElement());
            } else if (cache == null) {
                list.add(counterEntry.getElement());
            }
        }
        return list;
    }


    /**
     * {@inheritDoc}
     */
    public void notifyElementRemoved(final Cache cache, final Element element) {
        checkSynchonizedAccessToCacheOk(cache);
        cacheElementsRemoved.add(new CounterEntry(cache, element));
    }

    /**
     * Called immediately after an element has been put into the cache. The {@link net.sf.ehcache.Cache#put(net.sf.ehcache.Element)} method
     * will block until this method returns.
     * <p/>
     * Implementers may wish to have access to the Element's fields, including value, so the element is provided.
     * Implementers should be careful not to modify the element. The effect of any modifications is undefined.
     *
     * @param cache
     * @param element   the element which was just put into the cache.
     */
    public void notifyElementPut(final Cache cache, final Element element) {
        checkSynchonizedAccessToCacheOk(cache);
        cacheElementsPut.add(new CounterEntry(cache, element));
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
    public void notifyElementUpdated(final Cache cache, final Element element) throws CacheException {
        cacheElementsUpdated.add(new CounterEntry(cache, element));
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementExpired(final Cache cache, final Element element) {
        cacheElementsExpired.add(new CounterEntry(cache, element));
    }

    /**
     * Give the replicator a chance to cleanup and free resources when no longer needed
     * <p/>
     * Clean up static counters
     */
    public void dispose() {
        cacheElementsPut = new ArrayList();
        cacheElementsUpdated = new ArrayList();
        cacheElementsRemoved = new ArrayList();
        cacheElementsExpired = new ArrayList();
    }

    /**
     * This counter should be called from calls synchonized on Cache. These methods should hold the lock
     * therefore this is ok.
     * @param cache
     */
    private void checkSynchonizedAccessToCacheOk(Cache cache) {
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

        private Cache cache;
        private Element element;

        /**
         * Construct a new event
         * @param cache
         * @param element
         */
        public CounterEntry(Cache cache, Element element) {
            this.cache = cache;
            this.element = element;
        }

        /**
         * @return the cache the event relates to
         */
        public Cache getCache() {
            return cache;
        }

        /**
         * @return the payload
         */
        public Serializable getElement() {
            return element;
        }


    }


}

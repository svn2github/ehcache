/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 - 2004 Greg Luck.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by Greg Luck
 *       (http://sourceforge.net/users/gregluck) and contributors.
 *       See http://sourceforge.net/project/memberlist.php?group_id=93232
 *       for a list of contributors"
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "EHCache" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For written
 *    permission, please contact Greg Luck (gregluck at users.sourceforge.net).
 *
 * 5. Products derived from this software may not be called "EHCache"
 *    nor may "EHCache" appear in their names without prior written
 *    permission of Greg Luck.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL GREG LUCK OR OTHER
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by contributors
 * individuals on behalf of the EHCache project.  For more
 * information on EHCache, please see <http://ehcache.sourceforge.net/>.
 *
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

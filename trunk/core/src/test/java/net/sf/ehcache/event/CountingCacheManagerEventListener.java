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
import net.sf.ehcache.Status;

import java.util.ArrayList;
import java.util.List;


/**
 * A counting cache manager listener.
 * @author Greg Luck
 * @version $Id$
 */
public class CountingCacheManagerEventListener implements CacheManagerEventListener {


    private static List cacheNamesAdded = new ArrayList();
    private static List cacheNamesRemoved = new ArrayList();

    /**
     * Accessor
     */
    public static List getCacheNamesAdded() {
        return cacheNamesAdded;
    }

    /**
     * Resets the counters to 0
     */
    public static void resetCounters() {
        cacheNamesAdded.clear();
        cacheNamesRemoved.clear();
    }

    /**
     * Accessor
     */
    public static List getCacheNamesRemoved() {
        return cacheNamesRemoved;
    }

    /**
     * Call to start the listeners and do any other required initialisation.
     *
     * @throws net.sf.ehcache.CacheException - all exceptions are wrapped in CacheException
     */
    public void init() throws CacheException {
        //noop
    }

    /**
     * Returns the listener status.
     *
     * @return the status at the point in time the method is called
     */
    public Status getStatus() {
        return Status.STATUS_ALIVE;
    }

    /**
     * Stop the listener and free any resources.
     *
     * @throws net.sf.ehcache.CacheException - all exceptions are wrapped in CacheException
     */
    public void dispose() throws CacheException {
        //noop
    }

    /**
     * Called immediately after a cache has been added and activated.
     * <p/>
     * Note that the CacheManager calls this method from a synchronized method. Any attempt to call a synchronized
     * method on CacheManager from this method will cause a deadlock.
     * <p/>
     * Note that activation will also cause a CacheEventListener status change notification from
     * {@link net.sf.ehcache.Status#STATUS_UNINITIALISED} to {@link net.sf.ehcache.Status#STATUS_ALIVE}. Care should be
     * taken on processing that notification because:
     * <ul>
     * <li>the cache will not yet be accessible from the CacheManager.
     * <li>the addCaches methods whih cause this notification are synchronized on the CacheManager. An attempt to call
     * {@link net.sf.ehcache.CacheManager#getCache(String)} will cause a deadlock.
     * </ul>
     * The calling method will block until this method returns.
     * <p/>
     *
     * @param cacheName the name of the <code>Cache</code> the operation relates to
     * @see net.sf.ehcache.event.CacheEventListener
     */
    public void notifyCacheAdded(String cacheName) {
        cacheNamesAdded.add(cacheName);
    }

    /**
     * Called immediately after a cache has been disposed and removed. The calling method will block until
     * this method returns.
     * <p/>
     * Note that the CacheManager calls this method from a synchronized method. Any attempt to call a synchronized
     * method on CacheManager from this method will cause a deadlock.
     * <p/>
     * Note that a {@link net.sf.ehcache.event.CacheEventListener} status changed will also be triggered. Any attempt from that notification
     * to access CacheManager will also result in a deadlock.
     *
     * @param cacheName the name of the <code>Cache</code> the operation relates to
     */
    public void notifyCacheRemoved(String cacheName) {
        cacheNamesRemoved.add(cacheName);
    }
}

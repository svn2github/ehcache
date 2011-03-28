/**
 *  Copyright 2003-2010 Terracotta, Inc.
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

/**
 * Allows implementers to register callback methods that will be executed when a
 * <code>CacheManager</code> event occurs.
 *
 * The lifecycle events are:
 * <ol>
 * <li>init
 * <li>dispose
 * </ol>
 *
 *
 * CacheManager change events are:
 * <ol>
 * <li>adding a <code>Cache</code>
 * <li>removing a <code>Cache</code>
 * </ol>
 *
 * Note that the caches that are part of the initial configuration are not considered "changes".
 * It is only caches added or removed beyond the initial config.
 *
 * Callbacks to these methods are synchronous and unsynchronized. It is the responsibility of
 * the implementer to safely handle the potential performance and thread safety issues
 * depending on what their listener is doing.
 * @author Greg Luck
 * @version $Id$
 * @since 1.2
 * @see CacheEventListener
 */
public interface CacheManagerEventListener {

    /**
     * Call to start the listeners and do any other required initialisation.
     * init should also handle any work to do with the caches that are part of the initial configuration.
     * @throws CacheException - all exceptions are wrapped in CacheException
     */
    void init() throws CacheException;


    /**
     * Returns the listener status.
     * @return the status at the point in time the method is called
     */
    Status getStatus();

    /**
     * Stop the listener and free any resources.
     * @throws CacheException - all exceptions are wrapped in CacheException
     */
    void dispose() throws CacheException;

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
     * @param cacheName the name of the <code>Cache</code> the operation relates to
     * @see CacheEventListener
     */
    void notifyCacheAdded(String cacheName);

    /**
     * Called immediately after a cache has been disposed and removed. The calling method will
     * block until this method returns.
     * <p/>
     * Note that the CacheManager calls this method from a synchronized method. Any attempt to
     * call a synchronized method on CacheManager from this method will cause a deadlock.
     * <p/>
     * Note that a {@link CacheEventListener} status changed will also be triggered. Any
     * attempt from that notification to access CacheManager will also result in a deadlock.
     * @param cacheName the name of the <code>Cache</code> the operation relates to
     */
    void notifyCacheRemoved(String cacheName);

}

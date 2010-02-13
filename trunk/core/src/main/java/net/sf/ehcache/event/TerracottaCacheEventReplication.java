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
import net.sf.ehcache.Status;
import net.sf.ehcache.distribution.CacheReplicator;

/**
 * Creates a wrapper for sending out cache events through the Terracotta cluster
 *
 * @author Geert Bevin
 * @version $Id$
 */
public class TerracottaCacheEventReplication implements CacheReplicator {
    private Status status = Status.STATUS_ALIVE;

    /**
     * {@inheritDoc}
     */
    public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
        createCacheEventReplicator(cache).notifyElementRemoved(cache, element);
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
        createCacheEventReplicator(cache).notifyElementPut(cache, element);
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
        createCacheEventReplicator(cache).notifyElementUpdated(cache, element);
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementExpired(Ehcache cache, Element element) {
        createCacheEventReplicator(cache).notifyElementExpired(cache, element);
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementEvicted(Ehcache cache, Element element) {
        createCacheEventReplicator(cache).notifyElementEvicted(cache, element);
    }

    /**
     * {@inheritDoc}
     */
    public void notifyRemoveAll(Ehcache cache) {
        createCacheEventReplicator(cache).notifyRemoveAll(cache);
    }

    private CacheEventListener createCacheEventReplicator(Ehcache cache) {
        return cache.getCacheManager().createTerracottaEventReplicator(cache);
    }

    /**
     * {@inheritDoc}
     */
    public TerracottaCacheEventReplication clone() throws CloneNotSupportedException {
        return (TerracottaCacheEventReplication) super.clone();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isReplicateUpdatesViaCopy() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public final boolean notAlive() {
        return !alive();
    }

    /**
     * {@inheritDoc}
     */
    public final boolean alive() {
        return status != null && (status.equals(Status.STATUS_ALIVE));
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        status = Status.STATUS_SHUTDOWN;
    }
}
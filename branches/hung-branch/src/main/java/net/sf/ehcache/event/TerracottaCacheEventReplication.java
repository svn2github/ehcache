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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

/**
 * Creates a wrapper for sending out cache events through the Terracotta cluster
 *
 * @author Geert Bevin
 * @version $Id$
 */
public class TerracottaCacheEventReplication implements CacheEventListener {

    private final ConcurrentMap<Ehcache, CacheEventListener> replicators = new ConcurrentHashMap<Ehcache, CacheEventListener>();

    /**
     * {@inheritDoc}
     */
    public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
        if (cache.getCacheConfiguration().isTerracottaClustered()) {
            createCacheEventReplicator(cache).notifyElementRemoved(cache, element);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
        if (cache.getCacheConfiguration().isTerracottaClustered()) {
            createCacheEventReplicator(cache).notifyElementPut(cache, element);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
        if (cache.getCacheConfiguration().isTerracottaClustered()) {
            createCacheEventReplicator(cache).notifyElementUpdated(cache, element);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementExpired(Ehcache cache, Element element) {
        if (cache.getCacheConfiguration().isTerracottaClustered()) {
            createCacheEventReplicator(cache).notifyElementExpired(cache, element);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementEvicted(Ehcache cache, Element element) {
        if (cache.getCacheConfiguration().isTerracottaClustered()) {
            createCacheEventReplicator(cache).notifyElementEvicted(cache, element);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void notifyRemoveAll(Ehcache cache) {
        if (cache.getCacheConfiguration().isTerracottaClustered()) {
            createCacheEventReplicator(cache).notifyRemoveAll(cache);
        }
    }

    private CacheEventListener createCacheEventReplicator(Ehcache cache) {
        // the race is not a problem here, since the event replicator will only be created once in the clustered instance factory
        // this replicator map is simply a locally cached version, several puts for the same cache will result in the same value being put
        CacheEventListener replicator = replicators.get(cache);
        if (null == replicator) {
            replicator = cache.getCacheManager().createTerracottaEventReplicator(cache);
            replicators.put(cache, replicator);
        }

        return replicator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TerracottaCacheEventReplication clone() throws CloneNotSupportedException {
        return (TerracottaCacheEventReplication) super.clone();
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        // nothing to do
    }
}

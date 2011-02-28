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

package net.sf.ehcache.constructs.unlockedreadsview;

import java.io.Serializable;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.UnlockedReadsViewHelper;
import net.sf.ehcache.constructs.EhcacheDecoratorAdapter;
import net.sf.ehcache.statistics.LiveCacheStatisticsData;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.store.TerracottaStore;

/**
 * An Ehcache decorator that provides unlocked reads to the underlying {@link Cache}.
 * Other operations like put, remove etc are delegated to the underlying cache and may be coherent or incoherent based on the cache.
 * This decorator only works with caches which are clustered with Terracotta.
 * <p>
 * The purpose of this is to allow business logic faster access to data. It is akin to the READ_UNCOMMITTED database isolation level.
 * Normally a read lock must first be obtained to read data backed with Terracotta. If there is an outstanding write lock, the read lock
 * queues up. This is done so that the <i>happens before</i> guarantee can be made. However if the business logic is happy to read stale
 * data even if a write lock has been acquired in preparation for changing it, then much higher speeds can be obtained. Note that this view
 * is only going to give incoherent reads to the underlying cache and not writes. Writes are going to be either coherent or incoherent
 * depending on the underlying cache.
 * 
 * @author Abhishek Sanoujam
 * 
 */
public class UnlockedReadsView extends EhcacheDecoratorAdapter {

    private final String viewName;
    private final TerracottaStore terracottaStore;
    private final LiveCacheStatisticsData liveCacheStatisticsData;

    /**
     * Constructor accepting the underlying cache and a name for this unlocked reads view
     * 
     * @param underlyingCache
     * @param incoherentViewName
     */
    public UnlockedReadsView(final Cache underlyingCache, final String incoherentViewName) {
        super(underlyingCache);
        this.viewName = incoherentViewName;
        Store store = new UnlockedReadsViewHelper(underlyingCache).getStore();
        if (!(store instanceof TerracottaStore)) {
            throw new IllegalArgumentException(UnlockedReadsView.class.getName()
                    + " can be used to decorate caches clustered with Terracotta only.");
        }
        this.terracottaStore = (TerracottaStore) store;
        this.liveCacheStatisticsData = (LiveCacheStatisticsData) underlyingCache.getLiveCacheStatistics();
    }

    /**
     * Returns the name of this decorator
     */
    @Override
    public String getName() {
        return this.viewName;
    }

    /**
     * Provides unlocked reads to the underlying cache
     */
    @Override
    public Element get(final Object key) throws IllegalStateException, CacheException {
        if (isStatisticsEnabled()) {
            long start = System.currentTimeMillis();
            Element element = getFromStoreWithExpiryCheck(key, false, true, true);
            if (element == null) {
                liveCacheStatisticsData.cacheMissNotFound();
            }
            long end = System.currentTimeMillis();
            liveCacheStatisticsData.addGetTimeMillis(end - start);
            return element;
        } else {
            return getFromStoreWithExpiryCheck(key, false, false, true);
        }
    }

    /**
     * Provides unlocked reads to the underlying cache
     */
    @Override
    public Element get(final Serializable key) throws IllegalStateException, CacheException {
        return get((Object) key);
    }

    /**
     * Provides unlocked reads to the underlying cache
     */
    @Override
    public Element getQuiet(final Object key) throws IllegalStateException, CacheException {
        return getFromStoreWithExpiryCheck(key, true, false, false);
    }

    /**
     * Provides unlocked reads to the underlying cache
     */
    @Override
    public Element getQuiet(final Serializable key) throws IllegalStateException, CacheException {
        return getQuiet((Object) key);
    }

    /**
     * 
     * Provides unlocked reads to the underlying cache.
     * 
     * @param key
     * @param quiet
     *            does not update last access time if true
     * @param updateStats
     *            updates stats if true
     * @param notifyListeners
     *            notifies listeners if true
     * @return element associated with key if not expired
     */
    private Element getFromStoreWithExpiryCheck(final Object key, final boolean quiet, final boolean updateStats,
            final boolean notifyListeners) {
        Element element = null;
        if (quiet) {
            element = terracottaStore.unlockedGetQuiet(key);
        } else {
            element = terracottaStore.unlockedGet(key);
        }

        if (element != null) {
            if (isExpired(element)) {
                if (updateStats) {
                    liveCacheStatisticsData.cacheMissExpired();
                }
                element = terracottaStore.remove(key);
                if (notifyListeners) {
                    getCacheEventNotificationService().notifyElementExpiry(element, false);
                }
                element = null;
            } else {
                if (!quiet) {
                    element.updateAccessStatistics();
                }
                if (updateStats) {
                    liveCacheStatisticsData.cacheHitInMemory();
                }
            }
        }
        return element;
    }

}

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

package net.sf.ehcache.constructs.incoherentview;

import java.io.Serializable;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheStoreHelper;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.EhcacheDecoratorAdapter;
import net.sf.ehcache.statistics.LiveCacheStatisticsData;
import net.sf.ehcache.store.Store;

import org.terracotta.modules.ehcache.store.ClusteredStore;

public class IncoherentViewCache extends EhcacheDecoratorAdapter {

    private final String viewName;
    private final ClusteredStore clusteredStore;
    private final LiveCacheStatisticsData liveCacheStatisticsData;

    public IncoherentViewCache(final Cache decoratedCache, final String incoherentViewName) {
        super(decoratedCache);
        this.viewName = incoherentViewName;
        Store store = new CacheStoreHelper(decoratedCache).getCacheMemoryStore();
        if (!(store instanceof ClusteredStore)) {
            throw new IllegalArgumentException(IncoherentViewCache.class.getName()
                    + " can be used to decorate caches clustered with Terracotta only.");
        }
        this.clusteredStore = (ClusteredStore) store;
        this.liveCacheStatisticsData = (LiveCacheStatisticsData) decoratedCache.getLiveCacheStatistics();
    }

    @Override
    public String getName() {
        return this.viewName;
    }

    @Override
    public Element get(final Object key) throws IllegalStateException, CacheException {
        if (isStatisticsEnabled()) {
            long start = System.currentTimeMillis();
            Element element = getFromStoreWithExpiryCheck(key, true, true);
            if (element == null) {
                liveCacheStatisticsData.cacheMissNotFound();
            }
            long end = System.currentTimeMillis();
            liveCacheStatisticsData.addGetTimeMillis(end - start);
            return element;
        } else {
            return getFromStoreWithExpiryCheck(key, false, true);
        }
    }

    @Override
    public Element get(final Serializable key) throws IllegalStateException, CacheException {
        return get((Object) key);
    }

    @Override
    public Element getQuiet(final Object key) throws IllegalStateException, CacheException {
        return getFromStoreWithExpiryCheck(key, false, false);
    }

    @Override
    public Element getQuiet(final Serializable key) throws IllegalStateException, CacheException {
        return getQuiet((Object) key);
    }

    /**
     * Gets from the clustered store, always uses incoherent reads on the store. Before returning, elements are checked for expiry and
     * returns null if element is already expired.
     * Updates statistics and notifies listener (of removes due to element expire) based on parameters
     * 
     * @param key
     * @param updateStats
     *            updates stats if true
     * @param notifyListeners
     *            notifies listeners if true
     * @return element associated with key if not expired
     */
    private Element getFromStoreWithExpiryCheck(final Object key, final boolean updateStats, final boolean notifyListeners) {
        Element element = null;
        if (updateStats) {
            element = clusteredStore.unlockedGet(key);
        } else {
            element = clusteredStore.unlockedGetQuiet(key);
        }

        if (element != null) {
            if (isExpired(element)) {
                if (updateStats) {
                    liveCacheStatisticsData.cacheMissExpired();
                }
                element = clusteredStore.remove(key);
                if (notifyListeners) {
                    getCacheEventNotificationService().notifyElementExpiry(element, false);
                }
                element = null;
            } else {
                if (updateStats) {
                    element.updateAccessStatistics();
                    liveCacheStatisticsData.cacheHitInMemory();
                }
            }
        }
        return element;
    }

}

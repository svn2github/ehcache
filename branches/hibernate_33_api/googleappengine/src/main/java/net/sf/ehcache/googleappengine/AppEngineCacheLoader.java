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


package net.sf.ehcache.googleappengine;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Status;
import net.sf.ehcache.loader.CacheLoader;

import java.util.Collection;
import java.util.Map;

/**
 * To search against MemCache on a local cache miss, use cache.getWithLoader() together with a CacheLoader for MemCache.
 * Implementation note: do not use javax.cache.*, as Ehcache declares to be a provider and thus clashes with GAE...
 *
 * @author C&eacute;drik LIME
 * @see "http://ehcache.org/documentation/googleappengine.html"
 * @see "http://ehcache.org/documentation/cache_loaders.html"
 */
public class AppEngineCacheLoader implements CacheLoader {
    private String cacheName; 
    private String guid;
    private MemcacheService memCache;

    /**
     *
     */
    public AppEngineCacheLoader(String cacheName, String guid) {
        this.cacheName = cacheName;
        this.guid = guid;
    }

    /**
     * {@inheritDoc}
     */
    public CacheLoader clone(Ehcache cache) throws CloneNotSupportedException {
        AppEngineCacheLoader clone = new AppEngineCacheLoader(cache.getName(), cache.getGuid());
        return clone;
    }

    /**
     * {@inheritDoc}
     */
    public void init() {
        memCache = MemcacheServiceFactory.getMemcacheService();
        memCache.setNamespace(cacheName);
    }

    /**
     * @return same cache name for GAE Memcache as EhCache
     */
    protected String getCacheName() {
        return cacheName;
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() throws CacheException {
        cacheName = null;
        guid = null;
        memCache = null;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return "GAE-" + getCacheName();
    }

    /**
     * {@inheritDoc}
     */
    public Status getStatus() {
        return memCache == null ? Status.STATUS_UNINITIALISED : Status.STATUS_ALIVE;
    }

    /**
     * {@inheritDoc}
     */
    public Object load(Object key) throws CacheException {
        return memCache.get(key);
    }

    /**
     * {@inheritDoc}
     */
    public Object load(Object key, Object argument) {
        return load(key);
    }

    /**
     * {@inheritDoc}
     */
    public Map loadAll(Collection keys) {
        return memCache.getAll(keys);
    }

    /**
     * {@inheritDoc}
     */
    public Map loadAll(Collection keys, Object argument) {
        return loadAll(keys);
    }
}

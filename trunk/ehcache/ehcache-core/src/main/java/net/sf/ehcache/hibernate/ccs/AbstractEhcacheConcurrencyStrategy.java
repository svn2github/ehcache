/**
 *  Copyright Terracotta, Inc.
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
package net.sf.ehcache.hibernate.ccs;

import net.sf.ehcache.hibernate.EhCache;
import org.hibernate.cache.Cache;
import org.hibernate.cache.CacheConcurrencyStrategy;
import org.hibernate.cache.CacheException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Superclass of all Ehcache specific cache concurrency strategies.
 *
 * @author Chris Dennis
 */
@Deprecated
abstract class AbstractEhcacheConcurrencyStrategy implements CacheConcurrencyStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(EhcacheReadOnlyCache.class);

    /**
     * Ehcache instance this strategy accesses.
     */
    protected EhCache cache;

    /**
     * {@inheritDoc}
     * 
     * @throws CacheException if the underlying cache is not an Ehcache
     */
    public final void setCache(Cache cache) throws CacheException {
        if (cache instanceof EhCache) {
            this.cache = (EhCache) cache;
        } else {
            throw new CacheException("Ehcache concurrency strategies must be used with Ehcache caches");
        }
    }

    /**
     * {@inheritDoc}
     */
    public final Cache getCache() {
        return cache;
    }

    /**
     * {@inheritDoc}
     */
    public final String getRegionName() {
        return cache.getRegionName();
    }

    /**
     * {@inheritDoc}
     */
    public final void remove(Object key) throws CacheException {
        cache.remove(key);
    }

    /**
     * {@inheritDoc}
     */
    public final void clear() throws CacheException {
        cache.clear();
    }

    /**
     * {@inheritDoc}
     */
    public final void destroy() {
        try {
            cache.destroy();
        } catch (Exception e) {
            LOG.warn("could not destroy cache", e);
        }
    }
}

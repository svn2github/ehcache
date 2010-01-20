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
package net.sf.ehcache.hibernate.regions;

import java.util.HashMap;
import java.util.Map;
import net.sf.ehcache.Ehcache;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.Region;
import org.hibernate.cache.Timestamper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An EhCache specific data region implementation.
 * <p>
 * This class is the ultimate superclass for all EhCache Hibernate cache regions.
 * 
 * @author Chris Dennis
 * @author Greg Luck
 * @author Emmanuel Bernard
 */
abstract class EhCacheDataRegion implements Region {

    private static final Logger LOG = LoggerFactory.getLogger(EhCacheDataRegion.class);
    private static final int CACHE_LOCK_TIMEOUT = 60000 * Timestamper.ONE_MS;

    /**
     * EhCache instance backing this Hibernate data region.
     */
    protected final Ehcache cache;

    /**
     * Create a Hibernate data region backed by the given EhCache instance.
     */
    EhCacheDataRegion(Ehcache cache) {
        this.cache = cache;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return cache.getName();
    }

    /**
     * {@inheritDoc}
     */
    public void destroy() throws CacheException {
        try {
            cache.getCacheManager().removeCache(cache.getName());
        } catch (IllegalStateException e) {
            //When Spring and Hibernate are both involved this will happen in normal shutdown operation.
            //Do not throw an exception, simply log this one.
            LOG.debug("This can happen if multiple frameworks both try to shutdown ehcache", e);
        } catch (net.sf.ehcache.CacheException e) {
            throw new CacheException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getSizeInMemory() {
        try {
            return cache.calculateInMemorySize();
        } catch (Throwable t) {
            return -1;
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getElementCountInMemory() {
        try {
            return cache.getMemoryStoreSize();
        } catch (net.sf.ehcache.CacheException ce) {
            throw new CacheException(ce);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getElementCountOnDisk() {
        return cache.getDiskStoreSize();
    }

    /**
     * {@inheritDoc}
     */
    public Map toMap() {
        try {
            Map result = new HashMap();
            for (Object key : cache.getKeys()) {
                result.put(key, cache.get(key).getObjectValue());
            }
            return result;
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long nextTimestamp() {
        return Timestamper.next();
    }

    /**
     * {@inheritDoc}
     */
    public int getTimeout() {
        return CACHE_LOCK_TIMEOUT;
    }

    /**
     * Return the Ehcache instance backing this Hibernate data region.
     */
    public Ehcache getEhcache() {
        return cache;
    }
}

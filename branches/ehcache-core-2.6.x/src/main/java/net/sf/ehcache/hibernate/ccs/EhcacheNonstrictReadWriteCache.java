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

import java.util.Comparator;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.access.SoftLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ehcache specific non-strict read/write cache concurrency strategy.
 * <p>
 * This is the Ehcache specific equivalent to Hibernate's NonstrictReadWriteCache.
 *
 * @author Chris Dennis
 */
@Deprecated
public class EhcacheNonstrictReadWriteCache extends AbstractEhcacheConcurrencyStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(EhcacheNonstrictReadWriteCache.class);

    /**
     * {@inheritDoc}
     */
    public Object get(Object key, long txTimestamp) throws CacheException {
        return cache.get(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean put(Object key, Object value, long txTimestamp, Object version, Comparator versionComparator, boolean minimalPut)
            throws CacheException {
        if (minimalPut && cache.get(key) != null) {
            return false;
        } else {
            cache.put(key, value);
            return true;
        }
    }

    /**
     * Caching is non-strict so soft locks are not implemented.
     */
    public SoftLock lock(Object key, Object version) throws CacheException {
        return null;
    }

    /**
     * Removes the stale item.
     */
    public void evict(Object key) throws CacheException {
        cache.remove(key);
    }

    /**
     * Removes the invalidated item.
     */
    public boolean update(Object key, Object value, Object currentVersion, Object previousVersion) throws CacheException {
        evict(key);
        return false;
    }

    /**
     * A No-Op, since we are an asynchronous cache concurrency strategy.
     */
    public boolean insert(Object key, Object value, Object currentVersion) throws CacheException {
        return false;
    }

    /**
     * Removes the invalidated item.
     */
    public void release(Object key, SoftLock lock) throws CacheException {
        cache.remove(key);
    }

    /**
     * Removes the invalidated item.
     */
    public boolean afterUpdate(Object key, Object value, Object version, SoftLock lock) throws CacheException {
        release(key, lock);
        return false;
    }

    /**
     * A No-Op.
     */
    public boolean afterInsert(Object key, Object value, Object version) throws CacheException {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return cache + "(non-strict-read-write)";
    }
}

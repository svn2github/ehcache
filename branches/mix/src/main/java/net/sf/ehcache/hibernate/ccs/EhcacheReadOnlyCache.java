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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.cache.access.SoftLock;

/**
 * Ehcache specific read-only cache concurrency strategy.
 * <p>
 * This is the Ehcache specific equivalent to Hibernate's ReadOnlyCache.
 *
 * @author Chris Dennis
 */
@Deprecated
public class EhcacheReadOnlyCache extends AbstractEhcacheConcurrencyStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(EhcacheReadOnlyCache.class);

    /**
     * {@inheritDoc}
     */
    public Object get(Object key, long timestamp) throws CacheException {
        return cache.get(key);
    }

    /**
     * Throws UnsupportedOperationException since items in a read-only cache should not be mutated.
     *
     * @throws UnsupportedOperationException always
     */
    public SoftLock lock(Object key, Object version) throws UnsupportedOperationException {
        LOG.error("Application attempted to edit read only item: " + key);
        throw new UnsupportedOperationException("Can't write to a readonly object");
    }

    /**
     * {@inheritDoc}
     */
    public boolean put(Object key, Object value, long timestamp, Object version,
            Comparator versionComparator, boolean minimalPut) throws CacheException {
        if (minimalPut && cache.get(key) != null) {
            return false;
        } else {
            cache.put(key, value);
            return true;
        }
    }

    /**
     * Logs an error since items in a read-only cache should not be mutated.
     */
    public void release(Object key, SoftLock lock) {
        LOG.error("Application attempted to edit read only item: " + key);
        //throw new UnsupportedOperationException("Can't write to a readonly object");
    }

    /**
     * Throws UnsupportedOperationException since items in a read-only cache should not be mutated.
     *
     * @throws UnsupportedOperationException always
     */
    public boolean afterUpdate(Object key, Object value, Object version, SoftLock lock) throws UnsupportedOperationException {
        LOG.error("Application attempted to edit read only item: " + key);
        throw new UnsupportedOperationException("Can't write to a readonly object");
    }

    /**
     * Inserts the specified item into the cache.
     */
    public boolean afterInsert(Object key, Object value, Object version) throws CacheException {
        cache.update(key, value);
        return true;
    }

    /**
     * A No-Op, since we are an asynchronous cache concurrency strategy.
     */
    public void evict(Object key) throws CacheException {
    }

    /**
     * A No-Op, since we are an asynchronous cache concurrency strategy.
     */
    public boolean insert(Object key, Object value, Object currentVersion) {
        return false;
    }

    /**
     * Throws UnsupportedOperationException since items in a read-only cache should not be mutated.
     *
     * @throws UnsupportedOperationException always
     */
    public boolean update(Object key, Object value, Object currentVersion, Object previousVersion) throws UnsupportedOperationException  {
        LOG.error("Application attempted to edit read only item: " + key);
        throw new UnsupportedOperationException("Can't write to a readonly object");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return cache + "(read-only)";
    }
}

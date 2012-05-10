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
package net.sf.ehcache.hibernate.strategy;

import net.sf.ehcache.hibernate.regions.EhcacheEntityRegion;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.EntityRegion;
import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.cache.access.SoftLock;
import org.hibernate.cfg.Settings;

/**
 * Ehcache specific non-strict read/write entity region access strategy
 *
 * @author Chris Dennis
 */
public class NonStrictReadWriteEhcacheEntityRegionAccessStrategy extends AbstractEhcacheAccessStrategy<EhcacheEntityRegion>
        implements EntityRegionAccessStrategy {

    /**
     * Create a non-strict read/write access strategy accessing the given collection region.
     */   
    public NonStrictReadWriteEhcacheEntityRegionAccessStrategy(EhcacheEntityRegion region, Settings settings) {
        super(region, settings);
    }

    /**
     * {@inheritDoc}
     */
    public EntityRegion getRegion() {
        return region;
    }

    /**
     * {@inheritDoc}
     */
    public Object get(Object key, long txTimestamp) throws CacheException {
        return region.get(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
            throws CacheException {
        if (minimalPutOverride && region.contains(key)) {
            return false;
        } else {
            region.put(key, value);
            return true;
        }
    }

    /**
     * Since this is a non-strict read/write strategy item locking is not used.
     */
    public SoftLock lockItem(Object key, Object version) throws CacheException {
        return null;
    }

    /**
     * Since this is a non-strict read/write strategy item locking is not used.
     */
    public void unlockItem(Object key, SoftLock lock) throws CacheException {
        region.remove(key);
    }

    /**
     * Returns <code>false</code> since this is an asynchronous cache access strategy.
     */
    public boolean insert(Object key, Object value, Object version) throws CacheException {
        return false;
    }

    /**
     * Returns <code>false</code> since this is a non-strict read/write cache access strategy
     */
    public boolean afterInsert(Object key, Object value, Object version) throws CacheException {
        return false;
    }

    /**
     * Removes the entry since this is a non-strict read/write cache strategy.
     */
    public boolean update(Object key, Object value, Object currentVersion, Object previousVersion) throws CacheException {
        remove(key);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean afterUpdate(Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock)
            throws CacheException {
        unlockItem(key, lock);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(Object key) throws CacheException {
        region.remove(key);
    }
}

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
 * Ehcache specific read/write entity region access strategy
 *
 * @author Chris Dennis
 */
public class ReadWriteEhcacheEntityRegionAccessStrategy extends AbstractReadWriteEhcacheAccessStrategy<EhcacheEntityRegion>
        implements EntityRegionAccessStrategy {

    /**
     * Create a read/write access strategy accessing the given entity region.
     */
    public ReadWriteEhcacheEntityRegionAccessStrategy(EhcacheEntityRegion region, Settings settings) {
        super(region, settings);
    }

    /**
     * {@inheritDoc}
     */
    public EntityRegion getRegion() {
        return region;
    }

    /**
     * A no-op since this is an asynchronous cache access strategy.
     */
    public boolean insert(Object key, Object value, Object version) throws CacheException {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Inserts will only succeed if there is no existing value mapped to this key.
     */
    public boolean afterInsert(Object key, Object value, Object version) throws CacheException {
        region.writeLock(key);
        try {
            Lockable item = (Lockable) region.get(key);
            if (item == null) {
                region.put(key, new Item(value, version, region.nextTimestamp()));
                return true;
            } else {
                return false;
            }
        } finally {
            region.writeUnlock(key);
        }
    }

    /**
     * A no-op since this is an asynchronous cache access strategy.
     */
    public boolean update(Object key, Object value, Object currentVersion, Object previousVersion) throws CacheException {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Updates will only succeed if this entry was locked by this transaction and exclusively this transaction for the
     * duration of this transaction.  It is important to also note that updates will fail if the soft-lock expired during
     * the course of this transaction.
     */
    public boolean afterUpdate(Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock)
            throws CacheException {
        //what should we do with previousVersion here?
        region.writeLock(key);
        try {
            Lockable item = (Lockable) region.get(key);

            if (item != null && item.isUnlockable(lock)) {
                Lock lockItem = (Lock) item;
                if (lockItem.wasLockedConcurrently()) {
                    decrementLock(key, lockItem);
                    return false;
                } else {
                    region.put(key, new Item(value, currentVersion, region.nextTimestamp()));
                    return true;
                }
            } else {
                handleMissingLock(key, item);
                return false;
            }
        } finally {
            region.writeUnlock(key);
        }
    }
}

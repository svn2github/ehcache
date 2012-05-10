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

import net.sf.ehcache.hibernate.regions.EhcacheCollectionRegion;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.CollectionRegion;
import org.hibernate.cache.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.access.SoftLock;
import org.hibernate.cfg.Settings;

/**
 * Ehcache specific read-only collection region access strategy
 *
 * @author Chris Dennis
 */
public class ReadOnlyEhcacheCollectionRegionAccessStrategy extends AbstractEhcacheAccessStrategy<EhcacheCollectionRegion>
        implements CollectionRegionAccessStrategy {

    /**
     * Create a read-only access strategy accessing the given collection region.
     */
    public ReadOnlyEhcacheCollectionRegionAccessStrategy(EhcacheCollectionRegion region, Settings settings) {
        super(region, settings);
    }

    /**
     * {@inheritDoc}
     */
    public CollectionRegion getRegion() {
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
     * Throws UnsupportedOperationException since this cache is read-only
     *
     * @throws UnsupportedOperationException always
     */
    public SoftLock lockItem(Object key, Object version) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Can't write to a readonly object");
    }

    /**
     * A no-op since this cache is read-only
     */
    public void unlockItem(Object key, SoftLock lock) throws CacheException {
        //throw new UnsupportedOperationException("Can't write to a readonly object");
    }
}

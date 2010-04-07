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
package net.sf.ehcache.hibernate.regions.jta;

import net.sf.ehcache.hibernate.regions.EhcacheEntityRegion;
import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.EntityRegion;
import org.hibernate.cache.access.AccessType;
import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.cfg.Settings;

import java.util.Map;

/**
 * JTA EntityRegion.
 *
 * @author Chris Dennis
 * @author Ludovic Orban
 */
public class TransactionalEntityRegion implements EntityRegion {

    private final EhcacheEntityRegion region;
    private final Settings settings;

    /**
     * Construct a new entity region.
     * @param region the Hibernate region.
     * @param settings the Hibernate settings.
     */
    public TransactionalEntityRegion(EhcacheEntityRegion region, Settings settings) {
        this.region = region;
        this.settings = settings;
    }

    /**
     * {@inheritDoc}
     */
    public EntityRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException {
        return g(accessType, region);
    }

    private EntityRegionAccessStrategy g(AccessType accessType, EhcacheEntityRegion region) {
        if (AccessType.TRANSACTIONAL.equals(accessType) || AccessType.READ_ONLY.equals(accessType)) {
            if (isTransactionAware()) {
                return new TransactionalEntityRegionAccessStrategy(region, region.getEhcache(), settings);
            } else {
                throw new CacheException("Cannot provide transactional access - underlying Ehcache (" +
                        region.getName() + ") is not transactional");
            }
        } else {
            if (isTransactionAware()) {
                throw new CacheException("Cannot provide non-transactional access (" + accessType +
                        ") - underlying Ehcache (" + region.getName() + ") is transactional");
            } else {
                return region.buildAccessStrategy(accessType);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public CacheDataDescription getCacheDataDescription() {
        return region.getCacheDataDescription();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isTransactionAware() {
        return region.getEhcache().getCacheConfiguration().isTransactional();
    }

    /**
     * {@inheritDoc}
     */
    public void destroy() throws CacheException {
        region.destroy();
    }

    /**
     * Determine whether this region contains data for the given key.
     * The semantic here is whether the cache contains data visible for the current
     * call context. This should be viewed as a "best effort", meaning blocking should be
     * avoid if possible.
     * (New in Hibernate 3.5.)
     *
     * @param key - The cache key
     * @return True if the underlying cache contains corresponding data; false otherwise.
     */
    public boolean contains(Object key) {
        return region.contains(key);
    }

    /**
     * {@inheritDoc}
     */
    public long getElementCountInMemory() {
        return region.getElementCountInMemory();
    }

    /**
     * {@inheritDoc}
     */
    public long getElementCountOnDisk() {
        return region.getElementCountOnDisk();
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return region.getName();
    }

    /**
     * {@inheritDoc}
     */
    public long getSizeInMemory() {
        return region.getSizeInMemory();
    }

    /**
     * {@inheritDoc}
     */
    public int getTimeout() {
        return region.getTimeout();
    }

    /**
     * {@inheritDoc}
     */
    public long nextTimestamp() {
        return region.nextTimestamp();
    }

    /**
     * {@inheritDoc}
     */
    public Map toMap() {
        return region.toMap();
    }
}

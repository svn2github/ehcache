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

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.hibernate.strategy.NonStrictReadWriteEhCacheCollectionRegionAccessStrategy;
import net.sf.ehcache.hibernate.strategy.ReadOnlyEhCacheCollectionRegionAccessStrategy;
import net.sf.ehcache.hibernate.strategy.ReadWriteEhCacheCollectionRegionAccessStrategy;

import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.CollectionRegion;
import org.hibernate.cache.access.AccessType;
import org.hibernate.cache.access.CollectionRegionAccessStrategy;
import org.hibernate.cfg.Settings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A collection region specific wrapper around an EhCache instance.
 * <p>
 * This implementation returns EhCache specific access strategy instances for all the non-transactional access types.  Transactional access
 * is not supported.
 *
 * @author Chris Dennis
 */
public class EhCacheCollectionRegion extends EhCacheTransactionalDataRegion implements CollectionRegion {

    private static final Logger LOG = LoggerFactory.getLogger(EhCacheEntityRegion.class);

    /**
     * Constructs an EhCacheCollectionRegion around the given underlying cache.
     */
    public EhCacheCollectionRegion(Ehcache underlyingCache, Settings settings, CacheDataDescription metadata) {
        super(underlyingCache, settings, metadata);
    }

    /**
     * {@inheritDoc}
     */
    public CollectionRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException {
        if (AccessType.READ_ONLY.equals(accessType)) {
            if (metadata.isMutable()) {
                LOG.warn("read-only cache configured for mutable entity [" + getName() + "]");
            }
            return new ReadOnlyEhCacheCollectionRegionAccessStrategy(this, settings);
        } else if (AccessType.READ_WRITE.equals(accessType)) {
            return new ReadWriteEhCacheCollectionRegionAccessStrategy(this, settings);
        } else if (AccessType.NONSTRICT_READ_WRITE.equals(accessType)) {
            return new NonStrictReadWriteEhCacheCollectionRegionAccessStrategy(this, settings);
        } else if (AccessType.TRANSACTIONAL.equals(accessType)) {
            throw new CacheException("Transactional access is not supported by the Ehcache region factory.");
        } else {
            throw new IllegalArgumentException("unrecognized access strategy type [" + accessType + "]");
        }
    }
}

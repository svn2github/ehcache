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
import net.sf.ehcache.hibernate.regions.EhcacheEntityRegion;

import org.hibernate.cache.access.AccessType;
import org.hibernate.cache.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class implementing {@link EhcacheAccessStrategyFactory}
 *
 * @author Abhishek Sanoujam
 *
 */
public class EhcacheAccessStrategyFactoryImpl implements EhcacheAccessStrategyFactory {

    private static final Logger LOG = LoggerFactory.getLogger(EhcacheAccessStrategyFactoryImpl.class);

    /**
     * {@inheritDoc}
     */
    public EntityRegionAccessStrategy createEntityRegionAccessStrategy(EhcacheEntityRegion entityRegion, AccessType accessType) {
        if (AccessType.READ_ONLY.equals(accessType)) {
            if (entityRegion.getCacheDataDescription().isMutable()) {
                LOG.warn("read-only cache configured for mutable entity [" + entityRegion.getName() + "]");
            }
            return new ReadOnlyEhcacheEntityRegionAccessStrategy(entityRegion, entityRegion.getSettings());
        } else if (AccessType.READ_WRITE.equals(accessType)) {
            return new ReadWriteEhcacheEntityRegionAccessStrategy(entityRegion, entityRegion.getSettings());
        } else if (AccessType.NONSTRICT_READ_WRITE.equals(accessType)) {
            return new NonStrictReadWriteEhcacheEntityRegionAccessStrategy(entityRegion, entityRegion.getSettings());
        } else if (AccessType.TRANSACTIONAL.equals(accessType)) {
            return new TransactionalEhcacheEntityRegionAccessStrategy(entityRegion, entityRegion.getEhcache(), entityRegion.getSettings());
        } else {
            throw new IllegalArgumentException("unrecognized access strategy type [" + accessType + "]");
        }
    }

    /**
     * {@inheritDoc}
     */
    public CollectionRegionAccessStrategy createCollectionRegionAccessStrategy(EhcacheCollectionRegion collectionRegion,
            AccessType accessType) {
        if (AccessType.READ_ONLY.equals(accessType)) {
            if (collectionRegion.getCacheDataDescription().isMutable()) {
                LOG.warn("read-only cache configured for mutable entity [" + collectionRegion.getName() + "]");
            }
            return new ReadOnlyEhcacheCollectionRegionAccessStrategy(collectionRegion, collectionRegion.getSettings());
        } else if (AccessType.READ_WRITE.equals(accessType)) {
            return new ReadWriteEhcacheCollectionRegionAccessStrategy(collectionRegion, collectionRegion.getSettings());
        } else if (AccessType.NONSTRICT_READ_WRITE.equals(accessType)) {
            return new NonStrictReadWriteEhcacheCollectionRegionAccessStrategy(collectionRegion, collectionRegion.getSettings());
        } else if (AccessType.TRANSACTIONAL.equals(accessType)) {
            return new TransactionalEhcacheCollectionRegionAccessStrategy(collectionRegion, collectionRegion.getEhcache(), collectionRegion
                    .getSettings());
        } else {
            throw new IllegalArgumentException("unrecognized access strategy type [" + accessType + "]");
        }
    }

}

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
package net.sf.ehcache.hibernate.regions;

import java.util.Properties;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.hibernate.strategy.EhcacheAccessStrategyFactory;

import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.EntityRegion;
import org.hibernate.cache.access.AccessType;
import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.cfg.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An entity region specific wrapper around an Ehcache instance.
 * <p>
 * This implementation returns Ehcache specific access strategy instances for all the non-transactional access types. Transactional access
 * is not supported.
 *
 * @author Chris Dennis
 * @author Abhishek Sanoujam
 */
public class EhcacheEntityRegion extends EhcacheTransactionalDataRegion implements EntityRegion {

    private static final Logger LOG = LoggerFactory.getLogger(EhcacheEntityRegion.class);

    /**
     * Constructs an EhcacheEntityRegion around the given underlying cache.
     *
     * @param accessStrategyFactory
     */
    public EhcacheEntityRegion(EhcacheAccessStrategyFactory accessStrategyFactory, Ehcache underlyingCache, Settings settings,
            CacheDataDescription metadata, Properties properties) {
        super(accessStrategyFactory, underlyingCache, settings, metadata, properties);
    }

    /**
     * {@inheritDoc}
     */
    public EntityRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException {
        return accessStrategyFactory.createEntityRegionAccessStrategy(this, accessType);
    }
}

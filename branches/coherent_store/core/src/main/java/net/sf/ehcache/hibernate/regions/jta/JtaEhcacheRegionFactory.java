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

import java.util.Properties;

import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.CollectionRegion;
import org.hibernate.cache.EntityRegion;

import net.sf.ehcache.hibernate.EhCacheRegionFactory;
import net.sf.ehcache.hibernate.regions.EhcacheCollectionRegion;
import net.sf.ehcache.hibernate.regions.EhcacheEntityRegion;

/**
 * JTA RegionFactory.
 *
 * @author Chris Dennis
 * @author Ludovic Orban
 */
public class JtaEhcacheRegionFactory extends EhCacheRegionFactory {

    /**
     * Construct a new region factory.
     * @param prop the hibernate properties.
     */
    public JtaEhcacheRegionFactory(Properties prop) {
        super(prop);
    }

    /**
     * {@inheritDoc}
     */
    public EntityRegion buildEntityRegion(String regionName, Properties properties, CacheDataDescription metadata) throws CacheException {
        return new TransactionalEntityRegion((EhcacheEntityRegion) super.buildEntityRegion(regionName, properties, metadata), settings);
    }

    /**
     * {@inheritDoc}
     */
    public CollectionRegion buildCollectionRegion(String regionName, Properties properties, CacheDataDescription metadata)
            throws CacheException {
        return new TransactionalCollectionRegion((EhcacheCollectionRegion) super.buildCollectionRegion(regionName, properties, metadata),
                settings);
    }
}

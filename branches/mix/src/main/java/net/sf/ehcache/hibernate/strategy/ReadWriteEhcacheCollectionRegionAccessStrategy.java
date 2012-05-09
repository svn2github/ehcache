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
import org.hibernate.cache.CollectionRegion;

import org.hibernate.cache.access.CollectionRegionAccessStrategy;
import org.hibernate.cfg.Settings;

/**
 * Ehcache specific read/write collection region access strategy
 *
 * @author Chris Dennis
 */
public class ReadWriteEhcacheCollectionRegionAccessStrategy extends AbstractReadWriteEhcacheAccessStrategy<EhcacheCollectionRegion>
        implements CollectionRegionAccessStrategy {

    /**
     * Create a read/write access strategy accessing the given collection region.
     */
    public ReadWriteEhcacheCollectionRegionAccessStrategy(EhcacheCollectionRegion region, Settings settings) {
        super(region, settings);
    }

    /**
     * {@inheritDoc}
     */
    public CollectionRegion getRegion() {
        return region;
    }
}

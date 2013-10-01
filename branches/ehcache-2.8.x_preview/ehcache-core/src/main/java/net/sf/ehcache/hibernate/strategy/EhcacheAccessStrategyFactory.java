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

/**
 * Factory to create {@link EntityRegionAccessStrategy}
 *
 * @author Abhishek Sanoujam
 *
 */
public interface EhcacheAccessStrategyFactory {

    /**
     * Create {@link EntityRegionAccessStrategy} for the input {@link EhcacheEntityRegion} and {@link AccessType}
     *
     * @param entityRegion
     * @param accessType
     * @return the created {@link EntityRegionAccessStrategy}
     */
    public EntityRegionAccessStrategy createEntityRegionAccessStrategy(EhcacheEntityRegion entityRegion, AccessType accessType);

    /**
     * Create {@link CollectionRegionAccessStrategy} for the input {@link EhcacheCollectionRegion} and {@link AccessType}
     *
     * @param collectionRegion
     * @param accessType
     * @return the created {@link CollectionRegionAccessStrategy}
     */
    public CollectionRegionAccessStrategy createCollectionRegionAccessStrategy(EhcacheCollectionRegion collectionRegion,
            AccessType accessType);

}

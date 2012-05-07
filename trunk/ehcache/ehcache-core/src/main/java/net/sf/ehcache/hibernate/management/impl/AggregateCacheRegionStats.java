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

package net.sf.ehcache.hibernate.management.impl;

/**
 * @author gkeim
 * 
 */
public class AggregateCacheRegionStats extends CacheRegionStats {
    private int nodeCount;

    /**
     * @param region
     */
    public AggregateCacheRegionStats(String region) {
        super(region);
    }

    /**
     * @param stats
     */
    public void aggregate(CacheRegionStats stats) {
        nodeCount++;
        hitCount += stats.getHitCount();
        missCount += stats.getMissCount();
        putCount += stats.getPutCount();
        hitRatio = determineHitRatio();

        // just add the in memory count together, an average will be returned when the getter is used
        elementCountInMemory += stats.getElementCountInMemory();

        // the largest element count on disk is the one that is the most correct
        if (stats.getElementCountOnDisk() > elementCountOnDisk) {
            elementCountOnDisk = stats.getElementCountOnDisk();
        }
        // elementCountTotal is the same for each node, since it's the total count in the cluster
        // no real aggregation is needed, just use the same total count
        elementCountTotal = stats.getElementCountTotal();
    }

    /**
     * @see net.sf.ehcache.hibernate.management.impl.CacheRegionStats#getElementCountInMemory()
     */
    @Override
    public long getElementCountInMemory() {
        return elementCountInMemory / nodeCount;
    }
}

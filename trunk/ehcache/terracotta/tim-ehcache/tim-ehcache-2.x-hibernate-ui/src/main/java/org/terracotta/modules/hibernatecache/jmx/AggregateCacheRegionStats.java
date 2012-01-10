/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.hibernatecache.jmx;

public class AggregateCacheRegionStats extends CacheRegionStats {

  private int nodeCount = 0;

  public AggregateCacheRegionStats(String region) {
    super(region);
  }

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

  @Override
  public long getElementCountInMemory() {
    return elementCountInMemory / nodeCount;
  }
}

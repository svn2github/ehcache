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

package net.sf.ehcache.hibernate.management.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import org.hibernate.stat.SecondLevelCacheStatistics;

/**
 * @author gkeim
 *
 */
public class CacheRegionStats implements Serializable {
  private static final String        COMPOSITE_TYPE_NAME        = "CacheRegionStats";
  private static final String        COMPOSITE_TYPE_DESCRIPTION = "Statistics per Cache-region";
  private static final String[]      ITEM_NAMES                 = new String[] {"region", "shortName", "hitCount",
      "missCount", "putCount", "hitRatio", "elementCountInMemory", "elementCountOnDisk", "elementCountTotal",
      "hitLatency", "loadLatency", "latencyCacheHit", "latencyCacheTime", "latencyDbHit", "latencyDbTime", };
  private static final String[]      ITEM_DESCRIPTIONS          = new String[] {"region", "shortName", "hitCount",
      "missCount", "putCount", "hitRatio", "elementCountInMemory", "elementCountOnDisk", "elementCountTotal",
      "hitLatency", "loadLatency", "latencyCacheHit", "latencyCacheTime", "latencyDbHit", "latencyDbTime", };
  private static final OpenType[]    ITEM_TYPES                 = new OpenType[] {SimpleType.STRING,
      SimpleType.STRING, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.DOUBLE, SimpleType.LONG,
      SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG,
      SimpleType.LONG, SimpleType.LONG,                         };
  private static final CompositeType COMPOSITE_TYPE;
  private static final String        TABULAR_TYPE_NAME          = "Statistics by Cache-region";
  private static final String        TABULAR_TYPE_DESCRIPTION   = "All Cache Region Statistics";
  private static final String[]      INDEX_NAMES                = new String[] {"region", };
  private static final TabularType   TABULAR_TYPE;

  static {
    try {
      COMPOSITE_TYPE = new CompositeType(COMPOSITE_TYPE_NAME, COMPOSITE_TYPE_DESCRIPTION, ITEM_NAMES,
                                         ITEM_DESCRIPTIONS, ITEM_TYPES);
      TABULAR_TYPE = new TabularType(TABULAR_TYPE_NAME, TABULAR_TYPE_DESCRIPTION, COMPOSITE_TYPE, INDEX_NAMES);
    } catch (OpenDataException e) {
      throw new RuntimeException(e);
    }
  }

  
  /**
   * region
   */
  protected final String region;

  /**
   * shortName
   */
  protected final String shortName;
  
  /**
   * shortName
   */
  protected long hitCount;
  
  /**
   * missCount
   */
  protected long missCount;
  
  /**
   * putCount
   */
  protected long putCount;
  
  /**
   * hitRatio
   */
  protected double hitRatio;
  
  /**
   * elementCountInMemory
   */
  protected long elementCountInMemory;
  
  /**
   * elementCountOnDisk
   */
  protected long elementCountOnDisk;
  
  /**
   * elementCountTotal
   */
  protected long elementCountTotal;
  
  /**
   * hitLatency
   */
  protected long hitLatency;
  
  /**
   * loadLatency
   */
  protected long loadLatency;
  
  /**
   * latencyCacheHit
   */
  protected long latencyCacheHit;
  
  /**
   * latencyCacheTime
   */
  protected long latencyCacheTime;
  
  /**
   * latencyDbHit
   */
  protected long latencyDbHit;

  /**
   * latencyDbTime
   */
  protected long latencyDbTime;

  /**
   * @param region
   */
  public CacheRegionStats(String region) {
    this.region = region;
    this.shortName = CacheRegionUtils.determineShortName(region);
  }

  /**
   * @param region
   * @param src
   */
  public CacheRegionStats(String region, SecondLevelCacheStatistics src) {
    this(region);
    this.hitCount = src.getHitCount();
    this.missCount = src.getMissCount();
    this.putCount = src.getPutCount();
    this.hitRatio = determineHitRatio();
    this.elementCountInMemory = src.getElementCountInMemory();
    this.elementCountOnDisk = src.getElementCountOnDisk();
    this.elementCountTotal = src.getElementCountInMemory() + src.getElementCountOnDisk();
  }

//  public CacheRegionStats(String region, SecondLevelCacheStatistics src, ConcurrentLatencyStatistics latencyStats) {
//    this(region, src);
//    hitLatency = latencyStats.getAvgLoadTimeFromCache();
//    loadLatency = latencyStats.getAvgLoadTimeFromDatabase();
//    this.latencyCacheHit = latencyStats.getHitsToCache();
//    this.latencyCacheTime = latencyStats.getTimeLoadingFromCache();
//    this.latencyDbHit = latencyStats.getHitsToDatabase();
//    this.latencyDbTime = latencyStats.getTimeLoadingFromDatabase();
//  }

  /**
   * @param cData
   */
  public CacheRegionStats(final CompositeData cData) {
    int i = 0;
    region = (String) cData.get(ITEM_NAMES[i++]);
    shortName = (String) cData.get(ITEM_NAMES[i++]);
    hitCount = (Long) cData.get(ITEM_NAMES[i++]);
    missCount = (Long) cData.get(ITEM_NAMES[i++]);
    putCount = (Long) cData.get(ITEM_NAMES[i++]);
    hitRatio = (Double) cData.get(ITEM_NAMES[i++]);
    elementCountInMemory = (Long) cData.get(ITEM_NAMES[i++]);
    elementCountOnDisk = (Long) cData.get(ITEM_NAMES[i++]);
    elementCountTotal = (Long) cData.get(ITEM_NAMES[i++]);
    hitLatency = (Long) cData.get(ITEM_NAMES[i++]);
    loadLatency = (Long) cData.get(ITEM_NAMES[i++]);
    latencyCacheHit = (Long) cData.get(ITEM_NAMES[i++]);
    latencyCacheTime = (Long) cData.get(ITEM_NAMES[i++]);
    latencyDbHit = (Long) cData.get(ITEM_NAMES[i++]);
    latencyDbTime = (Long) cData.get(ITEM_NAMES[i++]);
  }

  /**
   * determineHitRatio
   */
  protected double determineHitRatio() {
    double result = 0;
    long readCount = getHitCount() + getMissCount();
    if (readCount > 0) {
      result = getHitCount() / ((double) readCount);
    }
    return result;
  }

  /**
   * toString
   */
  @Override
  public String toString() {
    return "region=" + getRegion() + "shortName=" + getShortName() + ", hitCount=" + getHitCount() + ", missCount="
           + getMissCount() + ", putCount" + getPutCount() + ", hitRatio" + getHitRatio() + ", elementCountInMemory="
           + getElementCountInMemory() + ", elementCountOnDisk=" + getElementCountOnDisk() + ", elementCountTotal="
           + getElementCountTotal() + ", hitLatency=" + getHitLatency() + ", loadLatency=" + getLoadLatency();
  }

  /**
   * getRegion
   */
  public String getRegion() {
    return region;
  }

  /**
   * getShortName
   */
  public String getShortName() {
    return shortName;
  }

  /**
   * getHitCount
   */
  public long getHitCount() {
    return hitCount;
  }

  /**
   * getMissCount
   */
  public long getMissCount() {
    return missCount;
  }

  /**
   * getPutCount
   */
  public long getPutCount() {
    return putCount;
  }

  /**
   * getHitRatio
   */
  public double getHitRatio() {
    return hitRatio;
  }

  /**
   * getElementCountInMemory
   */
  public long getElementCountInMemory() {
    return elementCountInMemory;
  }

  /**
   * getElementCountOnDisk
   */
  public long getElementCountOnDisk() {
    return elementCountOnDisk;
  }

  /**
   * getElementCountTotal
   */
  public long getElementCountTotal() {
    return elementCountTotal;
  }

  /**
   * getHitLatency
   */
  public long getHitLatency() {
    return hitLatency;
  }

  /**
   * getLoadLatency
   */
  public long getLoadLatency() {
    return loadLatency;
  }

  /**
   * getLatencyCacheHit
   */
  public long getLatencyCacheHit() {
    return latencyCacheHit;
  }

  /**
   * getLatencyCacheTime
   */
  public long getLatencyCacheTime() {
    return latencyCacheTime;
  }

  /**
   * getLatencyDbHit
   */
  public long getLatencyDbHit() {
    return latencyDbHit;
  }

  /**
   * getLatencyDbTime
   */
  public long getLatencyDbTime() {
    return latencyDbTime;
  }

  /**
   * toCompositeData
   */
  public CompositeData toCompositeData() {
    try {
      return new CompositeDataSupport(COMPOSITE_TYPE, ITEM_NAMES, new Object[] {getRegion(), getShortName(),
          getHitCount(), getMissCount(), getPutCount(), getHitRatio(), getElementCountInMemory(),
          getElementCountOnDisk(), getElementCountTotal(), getHitLatency(), getLoadLatency(), getLatencyCacheHit(),
          getLatencyCacheTime(), getLatencyDbHit(), getLatencyDbTime(), });
    } catch (OpenDataException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * newTabularDataInstance
   */
  public static TabularData newTabularDataInstance() {
    return new TabularDataSupport(TABULAR_TYPE);
  }

  /**
   * fromTabularData
   * @param tabularData
   */
  public static CacheRegionStats[] fromTabularData(final TabularData tabularData) {
    final List<CacheRegionStats> countList = new ArrayList(tabularData.size());
    for (final Iterator pos = tabularData.values().iterator(); pos.hasNext();) {
      countList.add(new CacheRegionStats((CompositeData) pos.next()));
    }
    return countList.toArray(new CacheRegionStats[countList.size()]);
  }
}

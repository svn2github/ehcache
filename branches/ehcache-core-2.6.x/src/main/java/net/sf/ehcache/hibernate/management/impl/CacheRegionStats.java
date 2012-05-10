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
      "missCount", "putCount", "hitRatio", "elementCountInMemory", "elementCountOnDisk", "elementCountTotal", };
  private static final String[]      ITEM_DESCRIPTIONS          = new String[] {"region", "shortName", "hitCount",
      "missCount", "putCount", "hitRatio", "elementCountInMemory", "elementCountOnDisk", "elementCountTotal", };
  private static final OpenType[]    ITEM_TYPES                 = new OpenType[] {SimpleType.STRING,
      SimpleType.STRING, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.DOUBLE, SimpleType.LONG,
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
   * region name
   */
  protected final String             region;
  
  /**
   * region short name
   */
  protected final String             shortName;
  
  /**
   * hit count
   */
  protected long                     hitCount;
  
  /**
   * miss count
   */
  protected long                     missCount;
  
  /**
   * put count
   */
  protected long                     putCount;
  
  /**
   * hit ratio
   */
  protected double                   hitRatio;
  
  /**
   * in-memory element count
   */
  protected long                     elementCountInMemory;
  
  /**
   * on-disk element count
   */
  protected long                     elementCountOnDisk;
  
  /**
   * total element count
   */
  protected long                     elementCountTotal;

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
    
    try {
        this.hitCount = BeanUtils.getLongBeanProperty(src, "hitCount");
        this.missCount = BeanUtils.getLongBeanProperty(src, "missCount");
        this.putCount = BeanUtils.getLongBeanProperty(src, "putCount");
        this.hitRatio = determineHitRatio();
        this.elementCountInMemory = BeanUtils.getLongBeanProperty(src, "elementCountInMemory");
        this.elementCountOnDisk = BeanUtils.getLongBeanProperty(src, "elementCountOnDisk");
        this.elementCountTotal = BeanUtils.getLongBeanProperty(src, "elementCountOnDisk");
    } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException("Exception retrieving statistics", e);
    }
  }

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
  }

  private static int safeParseInt(String s) {
      try {
          return Integer.parseInt(s);
      } catch (Exception e) {
          return -1;
      }
  }

  /**
   * @return hit ratio
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
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "region=" + getRegion() + "shortName=" + getShortName() + ", hitCount=" + getHitCount() + ", missCount="
           + getMissCount() + ", putCount" + getPutCount() + ", hitRatio" + getHitRatio() + ", elementCountInMemory="
           + getElementCountInMemory() + ", elementCountOnDisk=" + getElementCountOnDisk() + ", elementCountTotal="
           + getElementCountTotal();
  }

  /**
   * @return region name
   */
  public String getRegion() {
    return region;
  }

  /**
   * @return short name
   */
  public String getShortName() {
    return shortName;
  }

  /**
   * @return hit count
   */
  public long getHitCount() {
    return hitCount;
  }

  /**
   * @return miss count
   */
  public long getMissCount() {
    return missCount;
  }

  /**
   * @return put count
   */
  public long getPutCount() {
    return putCount;
  }

  /**
   * @return hit ratio
   */
  public double getHitRatio() {
    return hitRatio;
  }

  /**
   * @return in-memory element count
   */
  public long getElementCountInMemory() {
    return elementCountInMemory;
  }

  /**
   * @return on-disk element count
   */
  public long getElementCountOnDisk() {
    return elementCountOnDisk;
  }

  /**
   * @return total element count
   */
  public long getElementCountTotal() {
    return elementCountTotal;
  }

  /**
   * @return composite data
   */
  public CompositeData toCompositeData() {
    try {
      return new CompositeDataSupport(COMPOSITE_TYPE, ITEM_NAMES, new Object[] {getRegion(), getShortName(),
          getHitCount(), getMissCount(), getPutCount(), getHitRatio(), getElementCountInMemory(),
          getElementCountOnDisk(), getElementCountTotal(), });
    } catch (OpenDataException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return tabular data
   */
  public static TabularData newTabularDataInstance() {
    return new TabularDataSupport(TABULAR_TYPE);
  }

  /**
   * @param tabularData
   * @return array of region statistics
   */
  public static CacheRegionStats[] fromTabularData(final TabularData tabularData) {
    final List<CacheRegionStats> countList = new ArrayList(tabularData.size());
    for (final Iterator pos = tabularData.values().iterator(); pos.hasNext();) {
      countList.add(new CacheRegionStats((CompositeData) pos.next()));
    }
    return countList.toArray(new CacheRegionStats[countList.size()]);
  }
}

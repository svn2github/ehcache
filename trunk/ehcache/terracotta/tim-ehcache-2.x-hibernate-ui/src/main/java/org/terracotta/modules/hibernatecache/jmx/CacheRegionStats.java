/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.hibernatecache.jmx;

import org.hibernate.stat.SecondLevelCacheStatistics;
import org.terracotta.modules.hibernatecache.presentation.CacheRegionUtils;

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

public class CacheRegionStats implements Serializable {
  private static final String        COMPOSITE_TYPE_NAME        = "CacheRegionStats";
  private static final String        COMPOSITE_TYPE_DESCRIPTION = "Statistics per Cache-region";
  private static final String[]      ITEM_NAMES                 = new String[] { "region", "shortName", "hitCount",
      "missCount", "putCount", "hitRatio", "elementCountInMemory", "elementCountOnDisk", "elementCountTotal" };
  private static final String[]      ITEM_DESCRIPTIONS          = new String[] { "region", "shortName", "hitCount",
      "missCount", "putCount", "hitRatio", "elementCountInMemory", "elementCountOnDisk", "elementCountTotal" };
  private static final OpenType[]    ITEM_TYPES                 = new OpenType[] { SimpleType.STRING,
      SimpleType.STRING, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.DOUBLE, SimpleType.LONG,
      SimpleType.LONG, SimpleType.LONG                         };
  private static final CompositeType COMPOSITE_TYPE;
  private static final String        TABULAR_TYPE_NAME          = "Statistics by Cache-region";
  private static final String        TABULAR_TYPE_DESCRIPTION   = "All Cache Region Statistics";
  private static final String[]      INDEX_NAMES                = new String[] { "region" };
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

  protected final String             region;
  protected final String             shortName;
  protected long                     hitCount;
  protected long                     missCount;
  protected long                     putCount;
  protected double                   hitRatio;
  protected long                     elementCountInMemory;
  protected long                     elementCountOnDisk;
  protected long                     elementCountTotal;

  public CacheRegionStats(String region) {
    this.region = region;
    this.shortName = CacheRegionUtils.determineShortName(region);
  }

  public CacheRegionStats(String region, SecondLevelCacheStatistics src) {
    this(region);
    this.hitCount = src.getHitCount();
    this.missCount = src.getMissCount();
    this.putCount = src.getPutCount();
    this.hitRatio = determineHitRatio();
    this.elementCountInMemory = src.getElementCountInMemory();
    this.elementCountOnDisk = src.getElementCountOnDisk();
    this.elementCountTotal = /* src.getElementCountInMemory() + */src.getElementCountOnDisk();
  }

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

  protected double determineHitRatio() {
    double result = 0;
    long readCount = getHitCount() + getMissCount();
    if (readCount > 0) {
      result = getHitCount() / ((double) readCount);
    }
    return result;
  }

  @Override
  public String toString() {
    return "region=" + getRegion() + " shortName=" + getShortName() + ", hitCount=" + getHitCount() + ", missCount="
           + getMissCount() + ", putCount" + getPutCount() + ", hitRatio" + getHitRatio() + ", elementCountInMemory="
           + getElementCountInMemory() + ", elementCountOnDisk=" + getElementCountOnDisk() + ", elementCountTotal="
           + getElementCountTotal();
  }

  public String getRegion() {
    return region;
  }

  public String getShortName() {
    return shortName;
  }

  public long getHitCount() {
    return hitCount;
  }

  public long getMissCount() {
    return missCount;
  }

  public long getPutCount() {
    return putCount;
  }

  public double getHitRatio() {
    return hitRatio;
  }

  public long getElementCountInMemory() {
    return elementCountInMemory;
  }

  public long getElementCountOnDisk() {
    return elementCountOnDisk;
  }

  public long getElementCountTotal() {
    return elementCountTotal;
  }

  public CompositeData toCompositeData() {
    try {
      return new CompositeDataSupport(COMPOSITE_TYPE, ITEM_NAMES, new Object[] { getRegion(), getShortName(),
          getHitCount(), getMissCount(), getPutCount(), getHitRatio(), getElementCountInMemory(),
          getElementCountOnDisk(), getElementCountTotal() });
    } catch (OpenDataException e) {
      throw new RuntimeException(e);
    }
  }

  public static TabularData newTabularDataInstance() {
    return new TabularDataSupport(TABULAR_TYPE);
  }

  public static CacheRegionStats[] fromTabularData(final TabularData tabularData) {
    final List<CacheRegionStats> countList = new ArrayList(tabularData.size());
    for (final Iterator pos = tabularData.values().iterator(); pos.hasNext();) {
      countList.add(new CacheRegionStats((CompositeData) pos.next()));
    }
    return countList.toArray(new CacheRegionStats[countList.size()]);
  }

}

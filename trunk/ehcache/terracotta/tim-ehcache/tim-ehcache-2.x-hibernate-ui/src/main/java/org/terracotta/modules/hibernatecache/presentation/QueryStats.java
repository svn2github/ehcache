/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.hibernatecache.presentation;

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

import org.hibernate.stat.QueryStatistics;

public class QueryStats implements Serializable {
  private static final String        COMPOSITE_TYPE_NAME        = "QueryStats";
  private static final String        COMPOSITE_TYPE_DESCRIPTION = "Statistics per Query";
  private static final String[]      ITEM_NAMES                 = new String[] { "query", "cacheHitCount",
      "cacheMissCount", "cachePutCount", "executionCount", "executionRowCount", "executionAvgTime", "executionMaxTime",
      "executionMinTime"                                       };
  private static final String[]      ITEM_DESCRIPTIONS          = new String[] { "query", "cacheHitCount",
      "cacheMissCount", "cachePutCount", "executionCount", "executionRowCount", "executionAvgTime", "executionMaxTime",
      "executionMinTime"                                       };
  private static final OpenType[]    ITEM_TYPES                 = new OpenType[] { SimpleType.STRING, SimpleType.LONG,
      SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG,
      SimpleType.LONG                                          };
  private static final CompositeType COMPOSITE_TYPE;
  private static final String        TABULAR_TYPE_NAME          = "Statistics by Query";
  private static final String        TABULAR_TYPE_DESCRIPTION   = "All Query Statistics";
  private static final String[]      INDEX_NAMES                = new String[] { "query" };
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

  protected final String             query;
  protected long                     cacheHitCount;
  protected long                     cacheMissCount;
  protected long                     cachePutCount;
  protected long                     executionCount;
  protected long                     executionRowCount;
  protected long                     executionAvgTime;
  protected long                     executionMaxTime;
  protected long                     executionMinTime;

  public QueryStats(String name) {
    this.query = name;
  }

  public QueryStats(String name, QueryStatistics src) {
    this(name);
    this.cacheHitCount = src.getCacheHitCount();
    this.cacheMissCount = src.getCacheMissCount();
    this.cachePutCount = src.getCachePutCount();
    this.executionCount = src.getExecutionCount();
    this.executionRowCount = src.getExecutionRowCount();
    this.executionAvgTime = src.getExecutionAvgTime();
    this.executionMaxTime = src.getExecutionMaxTime();
    this.executionMinTime = src.getExecutionMinTime();
  }

  public QueryStats(final CompositeData cData) {
    int i = 0;
    query = (String) cData.get(ITEM_NAMES[i++]);
    cacheHitCount = (Long) cData.get(ITEM_NAMES[i++]);
    cacheMissCount = (Long) cData.get(ITEM_NAMES[i++]);
    cachePutCount = (Long) cData.get(ITEM_NAMES[i++]);
    executionCount = (Long) cData.get(ITEM_NAMES[i++]);
    executionRowCount = (Long) cData.get(ITEM_NAMES[i++]);
    executionAvgTime = (Long) cData.get(ITEM_NAMES[i++]);
    executionMaxTime = (Long) cData.get(ITEM_NAMES[i++]);
    executionMinTime = (Long) cData.get(ITEM_NAMES[i++]);
  }

  public void add(QueryStats stats) {
    cacheHitCount += stats.getCacheHitCount();
    cacheMissCount += stats.getCacheMissCount();
    cachePutCount += stats.getCachePutCount();
    executionCount += stats.getExecutionCount();
    executionRowCount += stats.getExecutionRowCount();
    executionAvgTime += stats.getExecutionAvgTime();
    executionMaxTime += stats.getExecutionMaxTime();
    executionMinTime += stats.getExecutionMinTime();
  }

  @Override
  public String toString() {
    return "query=" + query + ", cacheHitCount=" + cacheHitCount + ", cacheMissCount=" + cacheMissCount
           + ", cachePutCount=" + cachePutCount + ", executionCount=" + executionCount + ", executionRowCount="
           + executionRowCount + ", executionAvgTime=" + executionAvgTime + ", executionMaxTime=" + executionMaxTime
           + ", executionMinTime=" + executionMinTime;
  }

  public String getQuery() {
    return query;
  }

  public long getCacheHitCount() {
    return cacheHitCount;
  }

  public long getCacheMissCount() {
    return cacheMissCount;
  }

  public long getCachePutCount() {
    return cachePutCount;
  }

  public long getExecutionCount() {
    return executionCount;
  }

  public long getExecutionRowCount() {
    return executionRowCount;
  }

  public long getExecutionAvgTime() {
    return executionAvgTime;
  }

  public long getExecutionMaxTime() {
    return executionMaxTime;
  }

  public long getExecutionMinTime() {
    return executionMinTime;
  }

  public CompositeData toCompositeData() {
    try {
      return new CompositeDataSupport(COMPOSITE_TYPE, ITEM_NAMES, new Object[] { query, cacheHitCount, cacheMissCount,
          cachePutCount, executionCount, executionRowCount, executionAvgTime, executionMaxTime, executionMinTime });
    } catch (OpenDataException e) {
      throw new RuntimeException(e);
    }
  }

  public static TabularData newTabularDataInstance() {
    return new TabularDataSupport(TABULAR_TYPE);
  }

  public static QueryStats[] fromTabularData(final TabularData tabularData) {
    final List<QueryStats> countList = new ArrayList(tabularData.size());
    for (final Iterator pos = tabularData.values().iterator(); pos.hasNext();) {
      countList.add(new QueryStats((CompositeData) pos.next()));
    }
    return countList.toArray(new QueryStats[countList.size()]);
  }
}

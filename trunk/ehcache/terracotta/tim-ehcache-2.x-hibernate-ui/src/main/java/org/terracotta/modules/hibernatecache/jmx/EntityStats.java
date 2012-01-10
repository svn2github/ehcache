/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.hibernatecache.jmx;

import org.hibernate.stat.EntityStatistics;
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

/**
 * When we only support Java 6, all of this OpenMBean scaffolding can be removed in favor or MXBeans.
 * 
 * @author gkeim
 */
public class EntityStats implements Serializable {
  private static final String        COMPOSITE_TYPE_NAME        = "EntityStats";
  private static final String        COMPOSITE_TYPE_DESCRIPTION = "Statistics per Entity";
  private static final String[]      ITEM_NAMES                 = new String[] { "name", "shortName", "loadCount",
      "updateCount", "insertCount", "deleteCount", "fetchCount", "optimisticFailureCount" };
  private static final String[]      ITEM_DESCRIPTIONS          = new String[] { "name", "shortName", "loadCount",
      "updateCount", "insertCount", "deleteCount", "fetchCount", "optimisticFailureCount" };
  private static final OpenType[]    ITEM_TYPES                 = new OpenType[] { SimpleType.STRING,
      SimpleType.STRING, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG,
      SimpleType.LONG                                          };
  private static final CompositeType COMPOSITE_TYPE;
  private static final String        TABULAR_TYPE_NAME          = "Statistics by Entity";
  private static final String        TABULAR_TYPE_DESCRIPTION   = "All Entity Statistics";
  private static final String[]      INDEX_NAMES                = new String[] { "name" };
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

  protected final String             name;
  protected final String             shortName;
  protected long                     loadCount;
  protected long                     updateCount;
  protected long                     insertCount;
  protected long                     deleteCount;
  protected long                     fetchCount;
  protected long                     optimisticFailureCount;

  public EntityStats(String name) {
    this.name = name;
    this.shortName = CacheRegionUtils.determineShortName(name);
  }

  public EntityStats(String name, EntityStatistics src) {
    this(name);
    this.loadCount = src.getLoadCount();
    this.updateCount = src.getUpdateCount();
    this.insertCount = src.getInsertCount();
    this.deleteCount = src.getDeleteCount();
    this.fetchCount = src.getFetchCount();
    this.optimisticFailureCount = src.getOptimisticFailureCount();
  }

  public EntityStats(final CompositeData cData) {
    int i = 0;
    name = (String) cData.get(ITEM_NAMES[i++]);
    shortName = (String) cData.get(ITEM_NAMES[i++]);
    loadCount = (Long) cData.get(ITEM_NAMES[i++]);
    updateCount = (Long) cData.get(ITEM_NAMES[i++]);
    insertCount = (Long) cData.get(ITEM_NAMES[i++]);
    deleteCount = (Long) cData.get(ITEM_NAMES[i++]);
    fetchCount = (Long) cData.get(ITEM_NAMES[i++]);
    optimisticFailureCount = (Long) cData.get(ITEM_NAMES[i++]);
  }

  public void add(EntityStats stats) {
    loadCount += stats.getLoadCount();
    updateCount += stats.getUpdateCount();
    insertCount += stats.getInsertCount();
    deleteCount += stats.getDeleteCount();
    fetchCount += stats.getFetchCount();
    optimisticFailureCount += stats.getOptimisticFailureCount();
  }

  @Override
  public String toString() {
    return "name=" + name + ", shortName=" + shortName + ",loadCount=" + loadCount + ", updateCount=" + updateCount
           + ", insertCount=" + insertCount + ", deleteCount=" + deleteCount + ", fetchCount=" + fetchCount
           + ", optimisticFailureCount" + optimisticFailureCount;
  }

  public String getName() {
    return name;
  }

  public String getShortName() {
    return shortName;
  }

  public long getLoadCount() {
    return loadCount;
  }

  public long getUpdateCount() {
    return updateCount;
  }

  public long getInsertCount() {
    return insertCount;
  }

  public long getDeleteCount() {
    return deleteCount;
  }

  public long getFetchCount() {
    return fetchCount;
  }

  public long getOptimisticFailureCount() {
    return optimisticFailureCount;
  }

  public CompositeData toCompositeData() {
    try {
      return new CompositeDataSupport(COMPOSITE_TYPE, ITEM_NAMES, new Object[] { name, shortName, loadCount,
          updateCount, insertCount, deleteCount, fetchCount, optimisticFailureCount });
    } catch (OpenDataException e) {
      throw new RuntimeException(e);
    }
  }

  public static TabularData newTabularDataInstance() {
    return new TabularDataSupport(TABULAR_TYPE);
  }

  public static EntityStats[] fromTabularData(final TabularData tabularData) {
    final List<EntityStats> countList = new ArrayList(tabularData.size());
    for (final Iterator pos = tabularData.values().iterator(); pos.hasNext();) {
      countList.add(new EntityStats((CompositeData) pos.next()));
    }
    return countList.toArray(new EntityStats[countList.size()]);
  }

}

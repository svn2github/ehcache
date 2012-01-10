/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.hibernatecache.jmx;

import org.hibernate.stat.CollectionStatistics;
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

public class CollectionStats implements Serializable {
  private static final String        COMPOSITE_TYPE_NAME        = "CollectionsStats";
  private static final String        COMPOSITE_TYPE_DESCRIPTION = "Statistics per Collections";
  private static final String[]      ITEM_NAMES                 = new String[] { "roleName", "shortName", "loadCount",
      "fetchCount", "updateCount", "removeCount", "recreateCount" };
  private static final String[]      ITEM_DESCRIPTIONS          = new String[] { "roleName", "shortName", "loadCount",
      "fetchCount", "updateCount", "removeCount", "recreateCount" };
  private static final OpenType[]    ITEM_TYPES                 = new OpenType[] { SimpleType.STRING,
      SimpleType.STRING, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG };
  private static final CompositeType COMPOSITE_TYPE;
  private static final String        TABULAR_TYPE_NAME          = "Statistics by Collection";
  private static final String        TABULAR_TYPE_DESCRIPTION   = "All Collection Statistics";
  private static final String[]      INDEX_NAMES                = new String[] { "roleName" };
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

  protected final String             roleName;
  protected final String             shortName;
  protected long                     loadCount;
  protected long                     fetchCount;
  protected long                     updateCount;
  protected long                     removeCount;
  protected long                     recreateCount;

  public CollectionStats(String roleName) {
    this.roleName = roleName;
    this.shortName = CacheRegionUtils.determineShortName(roleName);
  }

  public CollectionStats(String name, CollectionStatistics src) {
    this(name);
    this.loadCount = src.getLoadCount();
    this.fetchCount = src.getFetchCount();
    this.updateCount = src.getUpdateCount();
    this.removeCount = src.getRemoveCount();
    this.recreateCount = src.getRecreateCount();
  }

  public CollectionStats(final CompositeData cData) {
    int i = 0;
    roleName = (String) cData.get(ITEM_NAMES[i++]);
    shortName = (String) cData.get(ITEM_NAMES[i++]);
    loadCount = (Long) cData.get(ITEM_NAMES[i++]);
    fetchCount = (Long) cData.get(ITEM_NAMES[i++]);
    updateCount = (Long) cData.get(ITEM_NAMES[i++]);
    removeCount = (Long) cData.get(ITEM_NAMES[i++]);
    recreateCount = (Long) cData.get(ITEM_NAMES[i++]);
  }

  public void add(CollectionStats stats) {
    loadCount += stats.getLoadCount();
    fetchCount += stats.getFetchCount();
    updateCount += stats.getUpdateCount();
    removeCount += stats.getRemoveCount();
    recreateCount += stats.getRecreateCount();
  }

  @Override
  public String toString() {
    return "roleName=" + roleName + "shortName=" + shortName + ", loadCount=" + loadCount + ", fetchCount="
           + fetchCount + ", updateCount=" + updateCount + ", removeCount=" + removeCount + ", recreateCount"
           + recreateCount;
  }

  public String getRoleName() {
    return roleName;
  }

  public String getShortName() {
    return shortName;
  }

  public long getLoadCount() {
    return loadCount;
  }

  public long getFetchCount() {
    return fetchCount;
  }

  public long getUpdateCount() {
    return updateCount;
  }

  public long getRemoveCount() {
    return removeCount;
  }

  public long getRecreateCount() {
    return recreateCount;
  }

  public CompositeData toCompositeData() {
    try {
      return new CompositeDataSupport(COMPOSITE_TYPE, ITEM_NAMES, new Object[] { roleName, shortName, loadCount,
          fetchCount, updateCount, removeCount, recreateCount });
    } catch (OpenDataException e) {
      throw new RuntimeException(e);
    }
  }

  public static TabularData newTabularDataInstance() {
    return new TabularDataSupport(TABULAR_TYPE);
  }

  public static CollectionStats[] fromTabularData(final TabularData tabularData) {
    final List<CollectionStats> countList = new ArrayList(tabularData.size());
    for (final Iterator pos = tabularData.values().iterator(); pos.hasNext();) {
      countList.add(new CollectionStats((CompositeData) pos.next()));
    }
    return countList.toArray(new CollectionStats[countList.size()]);
  }

}

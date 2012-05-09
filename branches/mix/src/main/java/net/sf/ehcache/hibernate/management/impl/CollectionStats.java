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

import org.hibernate.stat.CollectionStatistics;

/**
 * CollectionStats
 * @author gkeim
 */
public class CollectionStats implements Serializable {
  private static final String        COMPOSITE_TYPE_NAME        = "CollectionsStats";
  private static final String        COMPOSITE_TYPE_DESCRIPTION = "Statistics per Collections";
  private static final String[]      ITEM_NAMES                 = new String[] {"roleName", "shortName", "loadCount",
      "fetchCount", "updateCount", "removeCount", "recreateCount", };
  private static final String[]      ITEM_DESCRIPTIONS          = new String[] {"roleName", "shortName", "loadCount",
      "fetchCount", "updateCount", "removeCount", "recreateCount", };
  private static final OpenType[]    ITEM_TYPES                 = new OpenType[] {SimpleType.STRING,
      SimpleType.STRING, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, SimpleType.LONG, };
  private static final CompositeType COMPOSITE_TYPE;
  private static final String        TABULAR_TYPE_NAME          = "Statistics by Collection";
  private static final String        TABULAR_TYPE_DESCRIPTION   = "All Collection Statistics";
  private static final String[]      INDEX_NAMES                = new String[] {"roleName", };
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
   * roleName
   */
  protected final String roleName;
  
  /**
   * shortName
   */
  protected final String shortName;
  
  /**
   * loadCount
   */
  protected long loadCount;
  
  /**
   * fetchCount
   */
  protected long fetchCount;
  
  /**
   * updateCount
   */
  protected long updateCount;
  
  /**
   * removeCount
   */
  protected long removeCount;
  
  /**
   * recreateCount
   */
  protected long recreateCount;
  

  /**
   * @param roleName
   */
  public CollectionStats(String roleName) {
    this.roleName = roleName;
    this.shortName = CacheRegionUtils.determineShortName(roleName);
  }

  /**
   * @param name
   * @param src
   */
  public CollectionStats(String name, CollectionStatistics src) {
    this(name);

    try {
      this.loadCount = BeanUtils.getLongBeanProperty(src, "loadCount");
      this.fetchCount = BeanUtils.getLongBeanProperty(src, "fetchCount");
      this.updateCount = BeanUtils.getLongBeanProperty(src, "updateCount");
      this.removeCount = BeanUtils.getLongBeanProperty(src, "removeCount");
      this.recreateCount = BeanUtils.getLongBeanProperty(src, "recreateCount");
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Exception retrieving statistics", e);
    }
  }

  /**
   * @param cData
   */
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

  private static int safeParseInt(String s) {
      try {
          return Integer.parseInt(s);
      } catch (Exception e) {
          return -1;
      }
  }
  
  /**
   * @param stats
   */
  public void add(CollectionStats stats) {
    loadCount += stats.getLoadCount();
    fetchCount += stats.getFetchCount();
    updateCount += stats.getUpdateCount();
    removeCount += stats.getRemoveCount();
    recreateCount += stats.getRecreateCount();
  }

  /**
   * toString
   */
  @Override
  public String toString() {
    return "roleName=" + roleName + "shortName=" + shortName + ", loadCount=" + loadCount + ", fetchCount="
           + fetchCount + ", updateCount=" + updateCount + ", removeCount=" + removeCount + ", recreateCount"
           + recreateCount;
  }

  /**
   * getRoleName
   */
  public String getRoleName() {
    return roleName;
  }

  /**
   * getShortName
   */
  public String getShortName() {
    return shortName;
  }

  /**
   * getLoadCount
   */
  public long getLoadCount() {
    return loadCount;
  }

  /**
   * getFetchCount
   */
  public long getFetchCount() {
    return fetchCount;
  }

  /**
   * getUpdateCount
   */
  public long getUpdateCount() {
    return updateCount;
  }

  /**
   * getRemoveCount
   */
  public long getRemoveCount() {
    return removeCount;
  }

  /**
   * getRecreateCount
   */
  public long getRecreateCount() {
    return recreateCount;
  }

  /**
   * toCompositeData
   */
  public CompositeData toCompositeData() {
    try {
      return new CompositeDataSupport(COMPOSITE_TYPE, ITEM_NAMES, new Object[] {roleName, shortName, loadCount,
          fetchCount, updateCount, removeCount, recreateCount, });
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
   */
  public static CollectionStats[] fromTabularData(final TabularData tabularData) {
    final List<CollectionStats> countList = new ArrayList(tabularData.size());
    for (final Iterator pos = tabularData.values().iterator(); pos.hasNext();) {
      countList.add(new CollectionStats((CompositeData) pos.next()));
    }
    return countList.toArray(new CollectionStats[countList.size()]);
  }

}

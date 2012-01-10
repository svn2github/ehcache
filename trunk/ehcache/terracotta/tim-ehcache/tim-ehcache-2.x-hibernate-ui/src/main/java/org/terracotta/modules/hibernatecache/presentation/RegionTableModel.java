/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.hibernatecache.presentation;

import org.terracotta.modules.hibernatecache.jmx.CacheRegionStats;

import com.tc.admin.common.XObjectTableModel;

public class RegionTableModel extends XObjectTableModel {
  private static String[] fields      = { "ShortName", "HitRatio", "HitCount", "MissCount", "PutCount",
      "ElementCountInMemory", "ElementCountTotal" };
  private static String[] colHeadings = { "Region", "Hit Ratio", "Hits", "Misses", "Puts", "In-Memory Count",
      "Total Count"                  };

  public RegionTableModel() {
    super(CacheRegionStats.class, fields, colHeadings);
  }

  @Override
  public boolean isCellEditable(int row, int col) {
    return false;
  }
}

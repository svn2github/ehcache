/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.hibernatecache.presentation;

import com.tc.admin.common.XObjectTableModel;

public class RegionInfoTableModel extends XObjectTableModel {
  private static final String[] fields      = { "ShortName", "Enabled", "TTI", "TTL", "TargetMaxInMemoryCount",
      "TargetMaxTotalCount"                };
  private static final String[] colHeadings = { "Region", "Cached", "TTI", "TTL", "Target Max In-Memory Count",
      "Target Max Total Count"             };

  public RegionInfoTableModel() {
    super(CacheRegionInfo.class, fields, colHeadings);
  }

  @Override
  public boolean isCellEditable(int row, int col) {
    return false;
  }
}

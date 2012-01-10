/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.hibernatecache.presentation;

import com.tc.admin.common.XObjectTableModel;

public class QueryTableModel extends XObjectTableModel {
  private static final String[] fields      = { "Query", /* "CacheHitCount", "CacheMissCount", "CachePutCount", */
                                            "ExecutionCount", "ExecutionRowCount", "ExecutionAvgTime",
      "ExecutionMaxTime", "ExecutionMinTime" };
  private static final String[] colHeadings = { "Query", /* "Cache Hits", "Cache Misses", "Cache Puts", */"Executions",
      "Rows", "Avg Time", "Max Time", "Min Time" };

  public QueryTableModel() {
    super(QueryStats.class, fields, colHeadings);
  }

  @Override
  public boolean isCellEditable(int row, int col) {
    return false;
  }
}

/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.hibernatecache.presentation;

import org.terracotta.modules.hibernatecache.jmx.CollectionStats;

import com.tc.admin.common.XObjectTableModel;

public class CollectionTableModel extends XObjectTableModel {
  private static final String[] fields      = { "ShortName", "LoadCount", "FetchCount", "UpdateCount", "RemoveCount",
      "RecreateCount"                      };
  private static final String[] colHeadings = { "Role Name", "Loads", "Fetches", "Updates", "Removes", "Recreates" };

  public CollectionTableModel() {
    super(CollectionStats.class, fields, colHeadings);
  }

  @Override
  public boolean isCellEditable(int row, int col) {
    return false;
  }
}

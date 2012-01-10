/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.hibernatecache.presentation;

import org.terracotta.modules.hibernatecache.jmx.EntityStats;

import com.tc.admin.common.XObjectTableModel;

public class EntityTableModel extends XObjectTableModel {
  private static String[] fields      = { "ShortName", "LoadCount", "UpdateCount", "InsertCount", "DeleteCount",
      "FetchCount", "OptimisticFailureCount" };
  private static String[] colHeadings = { "Name", "Loads", "Updates", "Inserts", "Deletes", "Fetches",
      "Optimistic Failures"          };

  public EntityTableModel() {
    super(EntityStats.class, fields, colHeadings);
  }

  @Override
  public boolean isCellEditable(int row, int col) {
    return false;
  }
}

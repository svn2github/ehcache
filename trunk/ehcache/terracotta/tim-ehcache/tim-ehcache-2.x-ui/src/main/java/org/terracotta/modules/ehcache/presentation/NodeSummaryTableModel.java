/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation;

import org.terracotta.modules.ehcache.presentation.model.CacheManagerInstance;

import com.tc.admin.common.XObjectTableModel;

public class NodeSummaryTableModel extends XObjectTableModel {
  private static final String[] FIELDS  = { "ClientName", "InstanceCount", "EnabledCount", "BulkLoadEnabledCount",
      "StatisticsEnabledCount"         };
  private static final String[] HEADERS = { "Node", "Caches", "Enabled", "BulkLoading", "Statistics" };

  public NodeSummaryTableModel() {
    super(CacheManagerInstance.class, FIELDS, HEADERS);
  }

  @Override
  public boolean isCellEditable(int row, int col) {
    return false;
  }
}

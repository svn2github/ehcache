/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation;

import org.terracotta.modules.ehcache.presentation.model.CacheModel;

import com.tc.admin.common.XObjectTableModel;

public class CacheSummaryTableModel extends XObjectTableModel {
  private static final String[] FIELDS  = { "ShortName", "EnabledCount", "BulkLoadEnabledCount",
      "StatisticsEnabledCount"         };
  private static final String[] HEADERS = { "Cache", "Enabled", "BulkLoading", "Statistics" };

  public CacheSummaryTableModel() {
    super(CacheModel.class, FIELDS, HEADERS);
  }

  @Override
  public boolean isCellEditable(int row, int col) {
    return false;
  }
}

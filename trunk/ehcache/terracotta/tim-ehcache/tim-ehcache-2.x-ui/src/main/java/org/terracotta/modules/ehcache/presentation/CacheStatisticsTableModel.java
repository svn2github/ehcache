/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.ehcache.presentation;

import org.terracotta.modules.ehcache.presentation.model.CacheStatisticsModel;

import com.tc.admin.common.XObjectTableModel;

public class CacheStatisticsTableModel extends XObjectTableModel {
  public CacheStatisticsTableModel() {
    super(CacheStatisticsModel.class, CacheStatisticsModel.ATTRS, CacheStatisticsModel.HEADERS);
  }

  public CacheStatisticsTableModel(String[] columns) {
    super(CacheStatisticsModel.class, columns, CacheStatisticsModel.headersForAttributes(columns));
  }

  @Override
  public boolean isCellEditable(int row, int col) {
    return false;
  }
}

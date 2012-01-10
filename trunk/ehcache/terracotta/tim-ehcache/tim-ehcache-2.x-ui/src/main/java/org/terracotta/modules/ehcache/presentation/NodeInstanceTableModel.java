/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation;

import static org.terracotta.modules.ehcache.presentation.model.CacheModelInstance.BULK_LOAD_ENABLED_DESC_PROP;
import static org.terracotta.modules.ehcache.presentation.model.CacheModelInstance.CONSISTENCY_PROP;
import static org.terracotta.modules.ehcache.presentation.model.CacheModelInstance.ENABLED_PROP;
import static org.terracotta.modules.ehcache.presentation.model.CacheModelInstance.PINNED_TO_STORE_PROP;
import static org.terracotta.modules.ehcache.presentation.model.CacheModelInstance.SHORT_NAME_PROP;
import static org.terracotta.modules.ehcache.presentation.model.CacheModelInstance.STATISTICS_ENABLED_PROP;
import static org.terracotta.modules.ehcache.presentation.model.CacheModelInstance.TERRACOTTA_CLUSTERED_PROP;

import org.terracotta.modules.ehcache.presentation.model.CacheModelInstance;

import com.tc.admin.common.XObjectTableModel;

public class NodeInstanceTableModel extends XObjectTableModel {
  private static final String[] FIELDS  = { SHORT_NAME_PROP, TERRACOTTA_CLUSTERED_PROP, ENABLED_PROP,
      BULK_LOAD_ENABLED_DESC_PROP, CONSISTENCY_PROP, STATISTICS_ENABLED_PROP, PINNED_TO_STORE_PROP };
  private static final String[] HEADERS = { "Cache", "Terracotta-clustered", "Enabled", "Mode", "Consistency",
      "Statistics", "Pinned"           };

  public NodeInstanceTableModel() {
    super(CacheModelInstance.class, FIELDS, HEADERS);
  }

  @Override
  public boolean isCellEditable(int row, int col) {
    return false;
  }
}

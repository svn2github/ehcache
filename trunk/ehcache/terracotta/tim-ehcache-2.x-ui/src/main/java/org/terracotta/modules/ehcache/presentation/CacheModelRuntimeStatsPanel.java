/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */

package org.terracotta.modules.ehcache.presentation;

import org.terracotta.modules.ehcache.presentation.model.CacheModel;

import com.tc.admin.common.ApplicationContext;

import java.util.concurrent.Callable;

public class CacheModelRuntimeStatsPanel extends BaseEhcacheRuntimeStatsPanel {
  private final CacheModel cacheModel;

  public CacheModelRuntimeStatsPanel(ApplicationContext appContext, CacheModel cacheModel) {
    super(appContext, cacheModel.getCacheManagerModel());
    this.cacheModel = cacheModel;
  }

  private class CacheModelTableModelWorker extends TableModelWorker {
    private CacheModelTableModelWorker() {
      super(new Callable<CacheStatisticsTableModel>() {
        public CacheStatisticsTableModel call() throws Exception {
          CacheStatisticsTableModel result = new CacheStatisticsTableModel(getEffectiveTableColumns());
          result.add(cacheModel.getAggregateCacheStatistics());
          return result;
        }
      });
    }
  }

  @Override
  protected TableModelWorker createTableModelWorker() {
    return new CacheModelTableModelWorker();
  }
}

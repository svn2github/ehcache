/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation;

import org.terracotta.modules.ehcache.presentation.model.CacheModelInstance;

import com.tc.admin.common.ApplicationContext;

import java.util.concurrent.Callable;

public class CacheModelInstanceRuntimeStatsPanel extends ClientEhCacheRuntimeStatsPanel {
  private final CacheModelInstance cacheModelInstance;

  public CacheModelInstanceRuntimeStatsPanel(ApplicationContext appContext, CacheModelInstance cacheModelInstance) {
    super(appContext, cacheModelInstance.getCacheManagerInstance().getCacheManagerModel(), cacheModelInstance
        .getCacheManagerInstance().getClient());
    this.cacheModelInstance = cacheModelInstance;
  }

  private class CacheModelInstanceTableModelWorker extends TableModelWorker {
    private CacheModelInstanceTableModelWorker() {
      super(new Callable<CacheStatisticsTableModel>() {
        public CacheStatisticsTableModel call() throws Exception {
          CacheStatisticsTableModel result = new CacheStatisticsTableModel(getEffectiveTableColumns());
          result.add(cacheModelInstance.getCacheStatistics());
          return result;
        }
      });
    }
  }

  @Override
  protected TableModelWorker createTableModelWorker() {
    return new CacheModelInstanceTableModelWorker();
  }
}

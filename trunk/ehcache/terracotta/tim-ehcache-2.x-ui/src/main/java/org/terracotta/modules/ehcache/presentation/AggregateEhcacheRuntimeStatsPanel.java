/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */

package org.terracotta.modules.ehcache.presentation;

import org.terracotta.modules.ehcache.presentation.model.CacheManagerModel;
import org.terracotta.modules.ehcache.presentation.model.CacheModel;

import com.tc.admin.common.ApplicationContext;

import java.util.Iterator;
import java.util.concurrent.Callable;

public class AggregateEhcacheRuntimeStatsPanel extends BaseEhcacheRuntimeStatsPanel {
  public AggregateEhcacheRuntimeStatsPanel(ApplicationContext appContext, CacheManagerModel cacheManagerModel) {
    super(appContext, cacheManagerModel);
  }

  private class AggregateTableModelWorker extends TableModelWorker {
    private AggregateTableModelWorker() {
      super(new Callable<CacheStatisticsTableModel>() {
        public CacheStatisticsTableModel call() throws Exception {
          CacheStatisticsTableModel result = new CacheStatisticsTableModel(getEffectiveTableColumns());
          for (Iterator<CacheModel> iter = cacheManagerModel.cacheModelIterator(); iter.hasNext();) {
            CacheModel cacheModel = iter.next();
            result.add(cacheModel.getAggregateCacheStatistics());
          }
          return result;
        }
      });
    }
  }

  @Override
  protected TableModelWorker createTableModelWorker() {
    return new AggregateTableModelWorker();
  }
}

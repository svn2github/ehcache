/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */

package org.terracotta.modules.ehcache.presentation;

import static org.terracotta.modules.ehcache.presentation.EhcachePresentationUtils.CLUSTERED_ICON;
import static org.terracotta.modules.ehcache.presentation.EhcachePresentationUtils.NON_CLUSTERED_ICON;

import org.terracotta.modules.ehcache.presentation.model.CacheManagerInstance;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerModel;
import org.terracotta.modules.ehcache.presentation.model.CacheModel;
import org.terracotta.modules.ehcache.presentation.model.CacheStatisticsModel;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XTable.BaseRenderer;
import com.tc.admin.model.IClient;

import java.awt.BorderLayout;
import java.util.Iterator;
import java.util.concurrent.Callable;

public class ClientEhCacheRuntimeStatsPanel extends BaseEhcacheRuntimeStatsPanel {
  protected IClient client;

  public ClientEhCacheRuntimeStatsPanel(ApplicationContext appContext, CacheManagerModel cacheManagerModel,
                                        IClient client) {
    super(appContext, cacheManagerModel);
    this.client = client;
  }

  @Override
  protected XContainer createTopPanel() {
    XContainer panel = super.createTopPanel();
    XLabel label = new XLabel("Terracotta-clustered", CLUSTERED_ICON);
    panel.add(label, BorderLayout.WEST);
    return panel;
  }

  private class ClientTableModelWorker extends TableModelWorker {
    private ClientTableModelWorker() {
      super(new Callable<CacheStatisticsTableModel>() {
        public CacheStatisticsTableModel call() throws Exception {
          CacheStatisticsTableModel result = new CacheStatisticsTableModel(getEffectiveTableColumns());
          Iterator<CacheModel> iter = cacheManagerModel.cacheModelIterator();
          while (iter.hasNext()) {
            CacheStatisticsModel csm = iter.next().getCacheStatistics(client);
            if (csm != null) {
              result.add(csm);
            }
          }
          return result;
        }
      });
    }
  }

  @Override
  protected TableModelWorker createTableModelWorker() {
    return new ClientTableModelWorker();
  }

  @Override
  protected void setCacheStatisticsTableModel(CacheStatisticsTableModel tableModel) {
    super.setCacheStatisticsTableModel(tableModel);
    cacheTable.getColumnModel().getColumn(0).setCellRenderer(new CacheNameRenderer());
  }

  private class CacheNameRenderer extends BaseRenderer {
    @Override
    public void setValue(Object value) {
      super.setValue(value);

      String cacheName = value.toString();
      label.setIcon(isCacheTerracottaClustered(cacheName) ? CLUSTERED_ICON : NON_CLUSTERED_ICON);
    }
  }

  protected boolean isCacheTerracottaClustered(String cacheName) {
    CacheManagerInstance cacheManagerInstance = cacheManagerModel.getInstance(client);
    if (cacheManagerInstance != null) { return cacheManagerInstance.isCacheTerracottaClustered(cacheName); }
    return false;
  }
}

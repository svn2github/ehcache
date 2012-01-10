/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.ehcache.presentation;

import org.terracotta.modules.ehcache.presentation.model.CacheManagerModel;
import org.terracotta.modules.ehcache.presentation.model.CacheModel;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XTabbedPane;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

@SuppressWarnings("serial")
public class CacheManagerPanel extends BaseClusterModelPanel implements PropertyChangeListener {
  private final CacheManagerModel     cacheManagerModel;

  private EhcacheOverviewPanel        cacheOverviewPanel;
  private EhcachePerformancePanel     cachePerformancePanel;
  private EhcacheRuntimeStatsPanel    cacheStatsPanel;
  private CacheManagerSizingPanel sizingPanel;

  // private EhcacheContentsPanel cacheContentsPanel;

  public CacheManagerPanel(ApplicationContext appContext, CacheManagerModel cacheManagerModel) {
    super(appContext, cacheManagerModel.getClusterModel());
    this.cacheManagerModel = cacheManagerModel;
  }

  CacheManagerModel getCacheManagerModel() {
    return cacheManagerModel;
  }

  @Override
  public void setup() {
    super.setup();
    revalidate();
    repaint();
  }

  @Override
  protected void init() {
    cacheOverviewPanel.setup();
    cachePerformancePanel.setup();
    cacheStatsPanel.setup();
    sizingPanel.setup();

    // cacheContentsPanel.setup();
  }

  @Override
  protected XContainer createMainPanel() {
    XContainer panel = new XContainer(new BorderLayout());
    XTabbedPane tabbedPane = new XTabbedPane();
    tabbedPane.add(bundle.getString("overview"), cacheOverviewPanel = new EhcacheOverviewPanel(appContext,
                                                                                               cacheManagerModel));
    tabbedPane.add(bundle.getString("performance"),
                   cachePerformancePanel = new EhcachePerformancePanel(appContext, cacheManagerModel));
    tabbedPane.add(bundle.getString("statistics"), cacheStatsPanel = new EhcacheRuntimeStatsPanel(appContext,
                                                                                                  cacheManagerModel));
    tabbedPane.add(bundle.getString("sizing"), sizingPanel = new CacheManagerSizingPanel(appContext,
                                                                                             cacheManagerModel));

    // tabbedPane.add(bundle.getString("contents"), cacheContentsPanel = new EhcacheContentsPanel(appContext,
    // cacheManagerModel));
    panel.add(tabbedPane, BorderLayout.CENTER);

    cacheStatsPanel.addPropertyChangeListener(this);

    return panel;
  }

  public void propertyChange(PropertyChangeEvent evt) {
    String prop = evt.getPropertyName();

    if ("SelectedCacheModel".equals(prop)) {
      CacheModel cacheModel = (CacheModel) evt.getNewValue();
      cacheStatsPanel.setSelectedCacheModel(cacheModel);
    }
  }
}

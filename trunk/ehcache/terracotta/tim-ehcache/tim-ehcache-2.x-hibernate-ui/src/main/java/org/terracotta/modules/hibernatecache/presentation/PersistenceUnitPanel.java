/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.hibernatecache.presentation;

import com.tc.admin.AbstractClusterListener;
import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.PagedView;
import com.tc.admin.common.XContainer;
import com.tc.admin.model.IClusterModel;

import java.awt.BorderLayout;

import javax.swing.JComponent;

@SuppressWarnings("serial")
public class PersistenceUnitPanel extends XContainer {
  private final ApplicationContext   appContext;
  private final IClusterModel        clusterModel;
  private final ClusterListener      clusterListener;
  private final String               persistenceUnit;

  private PagedView                  pagedView;
  private H2LCPanel                  h2lcPanel;
  private HibernateRuntimeStatsPanel runtimeStatsPanel;

  public PersistenceUnitPanel(ApplicationContext appContext, IClusterModel clusterModel, String persistenceUnit) {
    super();

    this.appContext = appContext;
    this.clusterModel = clusterModel;
    this.persistenceUnit = persistenceUnit;
    this.clusterListener = new ClusterListener(clusterModel);
  }

  public void setup() {
    setLayout(new BorderLayout());
    add(createMainPanel());
    revalidate();
    repaint();

    clusterModel.addPropertyChangeListener(clusterListener);
    if (clusterModel.isReady()) {
      init();
    }
  }

  private PagedView createMainPanel() {
    this.pagedView = new PagedView();
    pagedView.addPage(createRuntimeStatisticsPanel());
    pagedView.addPage(createH2LCPanel());
    return this.pagedView;
  }

  public void setViewBy(String name) {
    pagedView.setPage(name);
  }

  private JComponent createH2LCPanel() {
    h2lcPanel = new H2LCPanel(appContext, clusterModel, persistenceUnit);
    h2lcPanel.setName(HibernatePresentationPanel.SECOND_LEVEL_CACHE_KEY);
    return h2lcPanel;
  }

  protected H2LCPanel getH2LCPanel() {
    return h2lcPanel;
  }

  private JComponent createRuntimeStatisticsPanel() {
    runtimeStatsPanel = new HibernateRuntimeStatsPanel(appContext, clusterModel, persistenceUnit);
    runtimeStatsPanel.setName(HibernatePresentationPanel.HIBERNATE_KEY);
    return runtimeStatsPanel;
  }

  protected HibernateRuntimeStatsPanel getHibernateStatsPanel() {
    return runtimeStatsPanel;
  }

  private class ClusterListener extends AbstractClusterListener {
    private ClusterListener(IClusterModel clusterModel) {
      super(clusterModel);
    }

    @Override
    protected void handleReady() {
      if (clusterModel.isReady()) {
        init();
      } else {
        suspend();
      }
    }
  }

  private void init() {
    h2lcPanel.setup();
    runtimeStatsPanel.setup();
  }

  private void suspend() {
    /**/
  }

  @Override
  public void tearDown() {
    clusterModel.removePropertyChangeListener(clusterListener);
    clusterListener.tearDown();

    super.tearDown();
  }
}

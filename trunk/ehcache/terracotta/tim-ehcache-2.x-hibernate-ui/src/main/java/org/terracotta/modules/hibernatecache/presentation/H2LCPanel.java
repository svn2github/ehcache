/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.hibernatecache.presentation;

import com.tc.admin.AbstractClusterListener;
import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XTabbedPane;
import com.tc.admin.model.IClusterModel;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.ResourceBundle;

import javax.swing.SwingConstants;

public class H2LCPanel extends XContainer {
  private final ApplicationContext    appContext;
  private final IClusterModel         clusterModel;
  private final String                persistenceUnit;
  private final ClusterListener       clusterListener;

  private XContainer                  mainPanel;
  private OverviewPanel               overviewPanel;
  private CacheRegionsPanel           cacheRegionsPanel;
  private H2LCRuntimeStatsPanel       h2lcStatsPanel;
  private XContainer                  messagePanel;
  private XLabel                      messageLabel;
  private boolean                     inited;

  private static final ResourceBundle bundle = ResourceBundle.getBundle(HibernateResourceBundle.class.getName());

  public H2LCPanel(ApplicationContext appContext, IClusterModel clusterModel, String persistenceUnit) {
    super(new BorderLayout());

    this.appContext = appContext;
    this.clusterModel = clusterModel;
    this.persistenceUnit = persistenceUnit;
    this.clusterListener = new ClusterListener(clusterModel);
  }

  public void setup() {
    mainPanel = createMainPanel();
    messagePanel = createMessagePanel();

    clusterModel.addPropertyChangeListener(clusterListener);
    if (clusterModel.isReady()) {
      init();
      removeAll();
      add(mainPanel);
    } else {
      add(messagePanel);
      messageLabel.setText(appContext.getString("cluster.not.ready.msg"));
    }
  }

  private XContainer createMainPanel() {
    XContainer panel = new XContainer(new BorderLayout());
    XTabbedPane tabbedPane = new XTabbedPane();

    overviewPanel = new OverviewPanel(appContext, clusterModel, persistenceUnit);
    tabbedPane.add(bundle.getString("overview"), overviewPanel);

    h2lcStatsPanel = new H2LCRuntimeStatsPanel(appContext, clusterModel, persistenceUnit);
    tabbedPane.add(bundle.getString("statistics"), h2lcStatsPanel);

    cacheRegionsPanel = new CacheRegionsPanel(appContext, clusterModel, persistenceUnit);
    tabbedPane.add(bundle.getString("configuration"), cacheRegionsPanel);

    panel.add(tabbedPane, BorderLayout.CENTER);
    return panel;
  }

  private XContainer createMessagePanel() {
    XContainer panel = new XContainer(new BorderLayout());
    panel.add(messageLabel = new XLabel());
    messageLabel.setText(appContext.getString("initializing"));
    messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
    messageLabel.setFont((Font) appContext.getObject("message.label.font"));
    return panel;
  }

  private class ClusterListener extends AbstractClusterListener {
    private ClusterListener(IClusterModel clusterModel) {
      super(clusterModel);
    }

    @Override
    protected void handleReady() {
      removeAll();
      if (clusterModel.isReady()) {
        if (!inited) {
          init();
        }
        add(mainPanel);
      } else {
        messageLabel.setText(appContext.getString("cluster.not.ready.msg"));
        add(messagePanel);
      }
      revalidate();
      repaint();
    }
  }

  private void init() {
    overviewPanel.setup();
    cacheRegionsPanel.setup();
    h2lcStatsPanel.setup();
  }

  /*
   * TODO: This is a lack-of-time hack. Real solution is a top-level model with events.
   */
  public void cacheRegionsChanged(int cachedRegions, int totalRegions) {
    overviewPanel.updateCachedRegionsCount(cachedRegions, totalRegions);
  }

  @Override
  public void tearDown() {
    clusterModel.removePropertyChangeListener(clusterListener);
    clusterListener.tearDown();

    super.tearDown();
  }
}

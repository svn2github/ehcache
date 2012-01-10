/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.hibernatecache.presentation;

import com.tc.admin.AbstractClusterListener;
import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.model.IClusterModel;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.SwingConstants;

public class VuMeterPanel extends XContainer {
  private final ApplicationContext appContext;
  private final IClusterModel      clusterModel;
  private final String             persistenceUnit;
  private final ClusterListener    clusterListener;
  private final boolean            globalMode;

  private XContainer               mainPanel;
  private AggregateVuMeterPanel    vuMeterPanel;
  private XContainer               messagePanel;
  private XLabel                   messageLabel;
  private boolean                  inited;

  public VuMeterPanel(ApplicationContext appContext, IClusterModel clusterModel, String persistenceUnit,
                      boolean globalMode) {
    super(new BorderLayout());

    this.appContext = appContext;
    this.clusterModel = clusterModel;
    this.persistenceUnit = persistenceUnit;
    this.clusterListener = new ClusterListener(clusterModel);
    this.globalMode = globalMode;
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
    panel.add(vuMeterPanel = new AggregateVuMeterPanel(appContext, clusterModel, persistenceUnit, globalMode),
              BorderLayout.CENTER);
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
    vuMeterPanel.setup();
  }

  @Override
  public void tearDown() {
    clusterModel.removePropertyChangeListener(clusterListener);
    clusterListener.tearDown();
    super.tearDown();
  }
}

/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.hibernatecache.presentation;

import com.tc.admin.AbstractClusterListener;
import com.tc.admin.ClusterElementChooser;
import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.ComponentNode;
import com.tc.admin.common.PagedView;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XSplitPane;
import com.tc.admin.common.XTreeNode;
import com.tc.admin.dso.ClientNode;
import com.tc.admin.dso.ClientsNode;
import com.tc.admin.model.ClientConnectionListener;
import com.tc.admin.model.IClient;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

public class H2LCRuntimeStatsPanel extends XContainer implements ActionListener, ClientConnectionListener,
    PropertyChangeListener {
  private final ApplicationContext    appContext;
  private final IClusterModel         clusterModel;
  private final String                persistenceUnit;
  private final ClusterListener       clusterListener;

  private XLabel                      currentViewLabel;
  private ElementChooser              elementChooser;
  private XSplitPane                  splitter;
  private PagedView                   chartPagedView;
  private PagedView                   tablePagedView;
  private XContainer                  mainPanel;
  private XContainer                  messagePanel;
  private XLabel                      messageLabel;
  private boolean                     inited;

  private static final ResourceBundle bundle              = ResourceBundle.getBundle(HibernateResourceBundle.class
                                                              .getName());

  private static final String         AGGREGATE_NODE_NAME = "AggregateNode";

  public H2LCRuntimeStatsPanel(ApplicationContext appContext, IClusterModel clusterModel, String persistenceUnit) {
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
      addNodePanels();
      removeAll();
      add(mainPanel);
    } else {
      add(messagePanel);
      messageLabel.setText(appContext.getString("cluster.not.ready.msg"));
    }
  }

  private XContainer createMainPanel() {
    XContainer topPanel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.anchor = GridBagConstraints.EAST;

    Font headerFont = (Font) appContext.getObject("header.label.font");
    XLabel headerLabel = new XLabel(appContext.getString("current.view.type"));
    topPanel.add(headerLabel, gbc);
    headerLabel.setFont(headerFont);
    gbc.gridx++;

    topPanel.add(currentViewLabel = new XLabel(), gbc);
    gbc.gridx++;

    // filler
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    topPanel.add(new XLabel(), gbc);
    gbc.gridx++;

    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.NONE;

    headerLabel = new XLabel(appContext.getString("select.view"));
    topPanel.add(headerLabel, gbc);
    headerLabel.setFont(headerFont);
    gbc.gridx++;

    topPanel.add(elementChooser = new ElementChooser(), gbc);

    topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.black));

    XContainer panel = new XContainer(new BorderLayout());
    panel.add(topPanel, BorderLayout.NORTH);

    splitter = new XSplitPane(JSplitPane.VERTICAL_SPLIT, chartPagedView = new PagedView(),
                              tablePagedView = new PagedView());
    splitter.setPreferences(appContext.getPrefs().node(getClass().getName() + "/split"));
    splitter.setDefaultDividerLocation(0.5);
    panel.add(splitter, BorderLayout.CENTER);

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

  private class ElementChooser extends ClusterElementChooser {
    ElementChooser() {
      super(clusterModel, H2LCRuntimeStatsPanel.this);
    }

    @Override
    protected XTreeNode[] createTopLevelNodes() {
      XTreeNode aggregateViewsNode = new XTreeNode(appContext.getString("aggregate.view"));
      ComponentNode aggregateViewNode = new ComponentNode(bundle.getString("cluster.stats"));
      aggregateViewNode.setName(AGGREGATE_NODE_NAME);
      aggregateViewsNode.add(aggregateViewNode);
      ClientsNode clientsNode = new ClientsNode(appContext, clusterModel) {
        @Override
        protected void updateLabel() {/**/
        }
      };
      clientsNode.setLabel(appContext.getString("runtime.stats.per.client.view"));
      return new XTreeNode[] { aggregateViewsNode, clientsNode };
    }

    @Override
    protected boolean acceptPath(TreePath path) {
      Object o = path.getLastPathComponent();
      if (o instanceof XTreeNode) {
        XTreeNode node = (XTreeNode) o;
        return AGGREGATE_NODE_NAME.equals(node.getName()) || node instanceof ClientNode;
      }
      return false;
    }
  }

  public void actionPerformed(ActionEvent e) {
    ElementChooser chsr = (ElementChooser) e.getSource();
    XTreeNode node = (XTreeNode) chsr.getSelectedObject();
    String name = node.getName();
    if (node instanceof ClientNode) {
      IClient client = ((ClientNode) node).getClient();
      if (!chartPagedView.hasPage(name)) {
        chartPagedView.addPage(createClientH2LCStatsChartPanel(client));
        tablePagedView.addPage(createClientRuntimeStatsPanel(client));
      }
    }
    chartPagedView.setPage(name);
    tablePagedView.setPage(name);
    TreePath path = elementChooser.getSelectedPath();
    Object type = path.getPathComponent(1);
    currentViewLabel.setText(type.toString());
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
          addNodePanels();
        }
        add(mainPanel);
      } else {
        messageLabel.setText(appContext.getString("cluster.not.ready.msg"));
        add(messagePanel);
      }
      revalidate();
      repaint();
    }

    @Override
    protected void handleActiveCoordinator(IServer oldActive, IServer newActive) {
      if (oldActive != null) {
        oldActive.removeClientConnectionListener(H2LCRuntimeStatsPanel.this);
      }
      if (newActive != null) {
        newActive.addClientConnectionListener(H2LCRuntimeStatsPanel.this);
      }
    }
  }

  public void clientConnected(final IClient client) {
    /**/
  }

  public void clientDisconnected(final IClient client) {
    SwingUtilities.invokeLater(new ClientDisconnectHandler(client));
  }

  private class ClientDisconnectHandler implements Runnable {
    private final IClient client;

    ClientDisconnectHandler(IClient client) {
      this.client = client;
    }

    public void run() {
      String pageName = client.toString();
      if (pageName != null) {
        chartPagedView.removePage(pageName);
        tablePagedView.removePage(pageName);
      }
    }
  }

  public void propertyChange(PropertyChangeEvent evt) {
    String prop = evt.getPropertyName();
    if (PagedView.PROP_CURRENT_PAGE.equals(prop)) {
      String newPage = (String) evt.getNewValue();
      elementChooser.setSelectedPath(newPage);
    }
  }

  private void addNodePanels() {
    elementChooser.setupTreeModel();

    chartPagedView.removeAll();
    tablePagedView.removeAll();
    chartPagedView.addPage(createAggregateH2LCStatsChartPanel());
    tablePagedView.addPage(createAggregateRuntimeStatsPanel());
    IServer activeCoord = clusterModel.getActiveCoordinator();
    if (activeCoord != null) {
      activeCoord.addClientConnectionListener(this);
    }
    elementChooser.setSelectedPath(AGGREGATE_NODE_NAME);
    chartPagedView.addPropertyChangeListener(this);
    inited = true;
  }

  private AggregateH2LCRuntimeStatsPanel createAggregateRuntimeStatsPanel() {
    AggregateH2LCRuntimeStatsPanel panel = new AggregateH2LCRuntimeStatsPanel(appContext, clusterModel, persistenceUnit);
    panel.setName(AGGREGATE_NODE_NAME);
    panel.setup();
    return panel;
  }

  private ClientH2LCRuntimeStatsPanel createClientRuntimeStatsPanel(IClient client) {
    ClientH2LCRuntimeStatsPanel panel = new ClientH2LCRuntimeStatsPanel(appContext, clusterModel, client,
                                                                        persistenceUnit);
    panel.setName(client.toString());
    panel.setup();
    return panel;
  }

  private AggregateH2LCStatsChartPanel createAggregateH2LCStatsChartPanel() {
    AggregateH2LCStatsChartPanel panel = new AggregateH2LCStatsChartPanel(appContext, clusterModel, persistenceUnit);
    panel.setName(AGGREGATE_NODE_NAME);
    panel.setup();
    return panel;
  }

  private ClientH2LCStatsChartPanel createClientH2LCStatsChartPanel(IClient client) {
    ClientH2LCStatsChartPanel panel = new ClientH2LCStatsChartPanel(appContext, clusterModel, client, persistenceUnit);
    panel.setName(client.toString());
    panel.setup();
    return panel;
  }

  public IClusterModel getClusterModel() {
    return clusterModel;
  }

  public String getPersistenceUnit() {
    return persistenceUnit;
  }

  @Override
  public void tearDown() {
    IServer activeCoord = clusterModel.getActiveCoordinator();
    if (activeCoord != null) {
      activeCoord.removeClientConnectionListener(this);
    }

    clusterModel.removePropertyChangeListener(clusterListener);
    clusterListener.tearDown();

    chartPagedView.removePropertyChangeListener(this);
    elementChooser.removeActionListener(this);

    super.tearDown();
  }
}

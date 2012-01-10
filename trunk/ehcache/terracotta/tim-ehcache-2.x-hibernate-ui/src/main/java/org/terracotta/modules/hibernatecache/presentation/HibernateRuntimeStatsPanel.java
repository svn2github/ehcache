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
import com.tc.admin.common.XTreeNode;
import com.tc.admin.dso.ClientNode;
import com.tc.admin.dso.ClientsNode;
import com.tc.admin.model.ClientConnectionListener;
import com.tc.admin.model.IClient;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
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
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

public class HibernateRuntimeStatsPanel extends XContainer implements ActionListener, ClientConnectionListener,
    PropertyChangeListener {
  private final ApplicationContext    appContext;
  private final IClusterModel         clusterModel;
  private final String                persistenceUnit;
  private final ClusterListener       clusterListener;

  private XLabel                      currentViewLabel;
  private ElementChooser              elementChooser;
  private PagedView                   pagedView;
  private XContainer                  mainPanel;
  private XContainer                  messagePanel;
  private XLabel                      messageLabel;
  private boolean                     inited;

  private static final ResourceBundle bundle              = ResourceBundle.getBundle(HibernateResourceBundle.class
                                                              .getName());

  private static final String         AGGREGATE_NODE_NAME = "AggregateNode";

  public HibernateRuntimeStatsPanel(ApplicationContext appContext, IClusterModel clusterModel, String persistenceUnit) {
    super(new BorderLayout());

    this.appContext = appContext;
    this.clusterModel = clusterModel;
    this.persistenceUnit = persistenceUnit;
    this.clusterListener = new ClusterListener(clusterModel);

    setBorder(BorderFactory.createTitledBorder("Hibernate Statistics"));
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
    XContainer panel = new XContainer(new BorderLayout());

    panel.add(pagedView = new PagedView(), BorderLayout.CENTER);

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
    panel.add(topPanel, BorderLayout.NORTH);

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
      super(clusterModel, HibernateRuntimeStatsPanel.this);
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
    if (pagedView.hasPage(name)) {
      pagedView.setPage(name);
    }
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
        oldActive.removeClientConnectionListener(HibernateRuntimeStatsPanel.this);
      }
      if (newActive != null) {
        newActive.addClientConnectionListener(HibernateRuntimeStatsPanel.this);
      }
    }
  }

  public void clientConnected(final IClient client) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        pagedView.addPage(createClientPanel(client));
      }
    });
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
      Component page = pagedView.getPage(client.toString());
      if (page != null) {
        pagedView.remove(page);
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

    pagedView.removeAll();
    pagedView.addPage(createAggregatePanel());
    IServer activeCoord = clusterModel.getActiveCoordinator();
    if (activeCoord != null) {
      for (IClient client : activeCoord.getClients()) {
        pagedView.addPage(createClientPanel(client));
      }
      activeCoord.addClientConnectionListener(this);
    }
    elementChooser.setSelectedPath(AGGREGATE_NODE_NAME);
    pagedView.addPropertyChangeListener(this);
    inited = true;
  }

  private AggregateHibernateRuntimeStatsPanel createAggregatePanel() {
    AggregateHibernateRuntimeStatsPanel panel = new AggregateHibernateRuntimeStatsPanel(appContext, clusterModel,
                                                                                        persistenceUnit);
    panel.setName(AGGREGATE_NODE_NAME);
    panel.setup();
    return panel;
  }

  private ClientHibernateRuntimeStatsPanel createClientPanel(IClient client) {
    ClientHibernateRuntimeStatsPanel panel = new ClientHibernateRuntimeStatsPanel(appContext, clusterModel, client,
                                                                                  persistenceUnit);
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

    pagedView.removePropertyChangeListener(this);
    elementChooser.removeActionListener(this);

    super.tearDown();
  }
}

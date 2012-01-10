/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation;

import org.terracotta.modules.ehcache.presentation.model.CacheManagerInstance;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerInstanceAdapter;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerModel;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerModelAdapter;
import org.terracotta.modules.ehcache.presentation.model.CacheModel;
import org.terracotta.modules.ehcache.presentation.model.CacheModelInstance;

import com.tc.admin.ClusterElementChooser;
import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.PagedView;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XSplitPane;
import com.tc.admin.common.XTabbedPane;
import com.tc.admin.common.XTreeNode;
import com.tc.admin.dso.ClientNode;
import com.tc.admin.dso.ClientsNode;
import com.tc.admin.model.IClient;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;

public class EhcacheRuntimeStatsPanel extends BaseClusterModelPanel implements ActionListener, PropertyChangeListener,
    HierarchyListener, TreeModelListener {
  private final CacheManagerModel          cacheManagerModel;
  private final CacheModelListener         cacheModelListener;
  private final CacheModelInstanceListener cacheModelInstanceListener;

  private XLabel                           currentViewLabel;
  private ElementChooser                   elementChooser;
  private XTreeNode                        aggregateViewsNode;
  private XTreeNode                        aggregateViewNode;
  private ClientsNode                      clientsNode;
  private XSplitPane                       splitter;
  private PagedView                        chartPagedView;
  private PagedView                        tablePagedView;

  private final ManageStatisticsAction     manageStatsAction;
  private final QueryForStatsMessage       queryForStatsMessage;

  private static final String              AGGREGATE_NODE_NAME              = "AggregateNode";

  private static final String              CACHE_MODEL_NAME_PREFIX          = "Aggregate ";

  private static final String              CACHE_MODEL_INSTANCE_NAME_PREFIX = "Instance of ";

  public EhcacheRuntimeStatsPanel(ApplicationContext appContext, CacheManagerModel cacheManagerModel) {
    super(appContext, cacheManagerModel.getClusterModel());

    this.cacheManagerModel = cacheManagerModel;
    this.cacheModelListener = new CacheModelListener();
    this.cacheModelInstanceListener = new CacheModelInstanceListener();

    manageStatsAction = new ManageStatisticsAction();
    addHierarchyListener(this);
    queryForStatsMessage = new QueryForStatsMessage(manageStatsAction);
  }

  @Override
  public void setup() {
    super.setup();

    cacheManagerModel.addCacheManagerModelListener(cacheModelListener);
    for (CacheManagerInstance cmi : cacheManagerModel.cacheManagerInstances()) {
      cmi.addCacheManagerInstanceListener(cacheModelInstanceListener);
    }
  }

  private String cacheModelInstanceName(CacheModelInstance cacheModelInstance) {
    return CACHE_MODEL_INSTANCE_NAME_PREFIX + cacheModelInstance.getCacheName() + " on "
           + cacheModelInstance.getClientName();
  }

  private class CacheModelListener extends CacheManagerModelAdapter {
    @Override
    public void cacheModelAdded(final CacheModel cacheModel) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          addToAggregate(cacheModel);
        }
      });
    }

    @Override
    public void cacheModelRemoved(final CacheModel cacheModel) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          removeFromAggregate(cacheModel);
        }
      });
    }

    @Override
    public void instanceAdded(final CacheManagerInstance cmi) {
      cmi.addCacheManagerInstanceListener(cacheModelInstanceListener);

      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          ClientNode clientNode = clientNode(cmi);
          if (clientNode != null) {
            for (CacheModelInstance cacheModelInstance : cmi.cacheModelInstances()) {
              CacheModelInstanceNode node = new CacheModelInstanceNode(cacheModelInstance);
              node.setName(cacheModelInstanceName(cacheModelInstance));
              clientNode.addChild(node);
            }
          }
        }
      });
    }

    @Override
    public void instanceRemoved(final CacheManagerInstance cmi) {
      cmi.removeCacheManagerInstanceListener(cacheModelInstanceListener);

      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          ClientNode clientNode = clientNode(cmi);
          if (clientNode != null) {
            clientNode.tearDownChildren();
          }
        }
      });
    }
  }

  private ClientNode clientNode(CacheManagerInstance cmi) {
    IClient client = cmi.getClient();
    for (int i = 0; i < clientsNode.getChildCount(); i++) {
      ClientNode clientNode = (ClientNode) clientsNode.getChildAt(i);
      if (client == clientNode.getClient()) { return clientNode; }
    }
    return null;
  }

  private class CacheModelInstanceListener extends CacheManagerInstanceAdapter {
    @Override
    public void cacheModelInstanceAdded(final CacheModelInstance cacheModelInstance) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          ClientNode clientNode = clientNode(cacheModelInstance.getCacheManagerInstance());
          if (clientNode != null) {
            CacheModelInstanceNode node = new CacheModelInstanceNode(cacheModelInstance);
            node.setName(cacheModelInstanceName(cacheModelInstance));
            clientNode.addChild(node);
          }
        }
      });
    }

    @Override
    public void cacheModelInstanceRemoved(final CacheModelInstance cacheModelInstance) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          ClientNode clientNode = clientNode(cacheModelInstance.getCacheManagerInstance());
          if (clientNode != null) {
            for (int i = 0; i < clientNode.getChildCount(); i++) {
              CacheModelInstanceNode node = (CacheModelInstanceNode) clientNode.getChildAt(i);
              if (node.getCacheModelInstance() == cacheModelInstance) {
                clientNode.removeChild(node);
                node.tearDown();
                break;
              }
            }
          }
        }
      });
    }
  }

  private void addToAggregate(CacheModel cacheModel) {
    CacheModelNode node = new CacheModelNode(cacheModel);
    node.setName(CACHE_MODEL_NAME_PREFIX + cacheModel.getCacheName());
    aggregateViewNode.addChild(node);
  }

  private void removeFromAggregate(CacheModel cacheModel) {
    for (int i = 0; i < aggregateViewNode.getChildCount(); i++) {
      CacheModelNode node = (CacheModelNode) aggregateViewNode.getChildAt(i);
      if (node.getCacheModel() == cacheModel) {
        aggregateViewNode.removeChild(node);
        node.tearDown();
        break;
      }
    }
  }

  private void removePagesForNode(XTreeNode node) {
    chartPagedView.removePage(node.getName());
    tablePagedView.removePage(node.getName());
  }

  @Override
  protected XContainer createMainPanel() {
    XContainer panel = new XContainer(new BorderLayout());
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

    splitter = new XSplitPane(JSplitPane.VERTICAL_SPLIT, chartPagedView = new PagedView(),
                              tablePagedView = new PagedView());
    splitter.setPreferences(appContext.getPrefs().node(getClass().getName() + "/split"));
    splitter.setResizeWeight(1.0);
    splitter.setDefaultDividerLocation(0.5);
    panel.add(splitter, BorderLayout.CENTER);

    return panel;
  }

  private class CacheModelNode extends XTreeNode {
    private final CacheModel cacheModel;

    private CacheModelNode(CacheModel cacheModel) {
      super(cacheModel);
      this.cacheModel = cacheModel;
    }

    public CacheModel getCacheModel() {
      return cacheModel;
    }
  }

  private class CacheModelInstanceNode extends XTreeNode {
    private final CacheModelInstance cacheModelInstance;

    private CacheModelInstanceNode(CacheModelInstance cacheModelInstance) {
      super(cacheModelInstance);
      this.cacheModelInstance = cacheModelInstance;
      setIcon(cacheModelInstance.isTerracottaClustered() ? EhcachePresentationUtils.CLUSTERED_ICON
          : EhcachePresentationUtils.NON_CLUSTERED_ICON);
    }

    public CacheModelInstance getCacheModelInstance() {
      return cacheModelInstance;
    }

    @Override
    public String toString() {
      return cacheModelInstance.getCacheName();
    }
  }

  private class ElementChooser extends ClusterElementChooser {
    ElementChooser() {
      super(cacheManagerModel.getClusterModel(), EhcacheRuntimeStatsPanel.this);
    }

    @Override
    protected XTreeNode[] createTopLevelNodes() {
      aggregateViewsNode = new XTreeNode(appContext.getString("aggregate.view"));
      aggregateViewNode = new XTreeNode(bundle.getString("cluster.stats"));
      aggregateViewNode.setName(AGGREGATE_NODE_NAME);
      aggregateViewsNode.addChild(aggregateViewNode);
      for (CacheModel cacheModel : cacheManagerModel.cacheModels()) {
        addToAggregate(cacheModel);
      }
      clientsNode = new ClientsNode(appContext, cacheManagerModel.getClusterModel()) {
        @Override
        protected void updateLabel() {/**/
        }
      };
      for (int i = 0; i < clientsNode.getChildCount(); i++) {
        ClientNode clientNode = (ClientNode) clientsNode.getChildAt(i);
        IClient client = clientNode.getClient();
        CacheManagerInstance cmi = cacheManagerModel.getInstance(client);
        if (cmi != null) {
          for (CacheModelInstance cacheModelInstance : cmi.cacheModelInstances()) {
            CacheModelInstanceNode node = new CacheModelInstanceNode(cacheModelInstance);
            node.setName(cacheModelInstanceName(cacheModelInstance));
            clientNode.addChild(node);
          }
        }
      }
      clientsNode.setLabel(appContext.getString("runtime.stats.per.client.view"));
      treeModel.addTreeModelListener(EhcacheRuntimeStatsPanel.this);
      return new XTreeNode[] { aggregateViewsNode, clientsNode };
    }

    @Override
    protected boolean acceptPath(TreePath path) {
      Object o = path.getLastPathComponent();
      if (o instanceof XTreeNode) {
        XTreeNode node = (XTreeNode) o;
        return AGGREGATE_NODE_NAME.equals(node.getName()) || node instanceof ClientNode
               || node instanceof CacheModelNode || node instanceof CacheModelInstanceNode;
      }
      return false;
    }

    @Override
    public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
      XTreeNode node = (XTreeNode) event.getPath().getLastPathComponent();
      if (node == aggregateViewsNode || node == clientsNode) { throw new ExpandVetoException(event); }

      if (node == aggregateViewNode || node instanceof ClientNode) {
        for (int i = 0; i < node.getChildCount(); i++) {
          XTreeNode child = (XTreeNode) node.getChildAt(i);
          if (chartPagedView.hasPage(child.getName())) { throw new ExpandVetoException(event); }
        }
      }
    }

    public void expandPath(TreePath treePath) {
      tree.expandPath(treePath);
      treeModelChanged();
    }
  }

  public void actionPerformed(ActionEvent e) {
    ElementChooser chsr = (ElementChooser) e.getSource();
    XTreeNode node = (XTreeNode) chsr.getSelectedObject();
    String name = node.getName();
    if (!chartPagedView.hasPage(name)) {
      // pages for ClientNode, CacheModelNode, and CacheModelInstanceNode are lazily created
      if (node instanceof ClientNode) {
        IClient client = ((ClientNode) node).getClient();
        chartPagedView.addPage(createClientEhcacheStatsChartPanel(client));
        tablePagedView.addPage(createClientRuntimeStatsPanel(client));
      } else if (node instanceof CacheModelNode) {
        CacheModel cacheModel = ((CacheModelNode) node).getCacheModel();
        chartPagedView.addPage(createCacheModelStatsChartPanel(cacheModel));
        tablePagedView.addPage(createCacheModelRuntimeStatsPanel(cacheModel));
      } else if (node instanceof CacheModelInstanceNode) {
        CacheModelInstance cacheModelInstance = ((CacheModelInstanceNode) node).getCacheModelInstance();
        chartPagedView.addPage(createCacheModelInstanceStatsChartPanel(cacheModelInstance));
        tablePagedView.addPage(createCacheModelInstanceRuntimeStatsPanel(cacheModelInstance));
      }
    }
    chartPagedView.setPage(name);
    tablePagedView.setPage(name);
    TreePath path = elementChooser.getSelectedPath();
    Object type = path.getPathComponent(1);
    String currentView = type.toString();
    if (node instanceof CacheModelInstanceNode) {
      currentView += "> " + path.getPathComponent(2);
    }
    currentViewLabel.setText(currentView);
  }

  @Override
  public void clientConnected(final IClient client) {
    /**/
  }

  @Override
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
        removePage(chartPagedView, pageName);
        removePage(tablePagedView, pageName);
      }
    }
  }

  private void removePage(PagedView pagedView, String pageName) {
    Component page = pagedView.getPage(pageName);
    if (page != null) {
      pagedView.removePage(pageName);
      if (page instanceof XContainer) {
        ((XContainer) page).tearDown();
      } else if (page instanceof XTabbedPane) {
        ((XTabbedPane) page).tearDown();
      }
    }
  }

  public void propertyChange(PropertyChangeEvent evt) {
    String prop = evt.getPropertyName();
    if (PagedView.PROP_CURRENT_PAGE.equals(prop)) {
      String newPage = (String) evt.getNewValue();
      elementChooser.setSelectedPath(newPage);
    } else if ("SelectedCacheModel".equals(prop)) {
      firePropertyChange("SelectedCacheModel", evt.getOldValue(), evt.getNewValue());
    }
  }

  public void setSelectedCacheModel(CacheModel cacheModel) {
    if (tablePagedView != null) {
      for (Component page : tablePagedView.getComponents()) {
        if (page instanceof BaseEhcacheRuntimeStatsPanel) {
          ((BaseEhcacheRuntimeStatsPanel) page).setSelectedCacheModel(cacheModel);
        }
      }
    }
  }

  @Override
  protected void init() {
    elementChooser.setupTreeModel();

    chartPagedView.removeAll();
    tablePagedView.removeAll();
    chartPagedView.addPage(createAggregateEhcacheStatsChartPanel());
    tablePagedView.addPage(createAggregateRuntimeStatsPanel());
    elementChooser.setSelectedPath(AGGREGATE_NODE_NAME);
    chartPagedView.addPropertyChangeListener(this);
  }

  private AggregateEhcacheRuntimeStatsPanel createAggregateRuntimeStatsPanel() {
    AggregateEhcacheRuntimeStatsPanel panel = new AggregateEhcacheRuntimeStatsPanel(appContext, cacheManagerModel);
    panel.setName(AGGREGATE_NODE_NAME);
    panel.setup();
    panel.addPropertyChangeListener(this);
    return panel;
  }

  private ClientEhCacheRuntimeStatsPanel createClientRuntimeStatsPanel(IClient client) {
    ClientEhCacheRuntimeStatsPanel panel = new ClientEhCacheRuntimeStatsPanel(appContext, cacheManagerModel, client);
    panel.setName(client.toString());
    panel.setup();
    panel.addPropertyChangeListener(this);
    return panel;
  }

  private AggregateEhcacheStatsChartPanel createAggregateEhcacheStatsChartPanel() {
    AggregateEhcacheStatsChartPanel panel = new AggregateEhcacheStatsChartPanel(appContext, cacheManagerModel);
    panel.setName(AGGREGATE_NODE_NAME);
    panel.setup();
    return panel;
  }

  private ClientEhcacheStatsChartPanel createClientEhcacheStatsChartPanel(IClient client) {
    ClientEhcacheStatsChartPanel panel = new ClientEhcacheStatsChartPanel(appContext, cacheManagerModel, client);
    panel.setName(client.toString());
    panel.setup();
    return panel;
  }

  private CacheModelStatsChartPanel createCacheModelStatsChartPanel(CacheModel cacheModel) {
    CacheModelStatsChartPanel panel = new CacheModelStatsChartPanel(appContext, cacheModel);
    panel.setName(CACHE_MODEL_NAME_PREFIX + cacheModel.getCacheName());
    panel.setup();
    return panel;
  }

  private CacheModelRuntimeStatsPanel createCacheModelRuntimeStatsPanel(CacheModel cacheModel) {
    CacheModelRuntimeStatsPanel panel = new CacheModelRuntimeStatsPanel(appContext, cacheModel);
    panel.setName(CACHE_MODEL_NAME_PREFIX + cacheModel.getCacheName());
    panel.setup();
    panel.addPropertyChangeListener(this);
    return panel;
  }

  private CacheModelInstanceStatsChartPanel createCacheModelInstanceStatsChartPanel(CacheModelInstance cacheModelInstance) {
    CacheModelInstanceStatsChartPanel panel = new CacheModelInstanceStatsChartPanel(appContext, cacheModelInstance);
    panel.setName(cacheModelInstanceName(cacheModelInstance));
    panel.setup();
    return panel;
  }

  private CacheModelInstanceRuntimeStatsPanel createCacheModelInstanceRuntimeStatsPanel(CacheModelInstance cacheModelInstance) {
    CacheModelInstanceRuntimeStatsPanel panel = new CacheModelInstanceRuntimeStatsPanel(appContext, cacheModelInstance);
    panel.setName(cacheModelInstanceName(cacheModelInstance));
    panel.setup();
    panel.addPropertyChangeListener(this);
    return panel;
  }

  protected CacheManagerModel getCacheManagerModel() {
    return cacheManagerModel;
  }

  private class ManageStatisticsAction extends AbstractAction {
    public void actionPerformed(ActionEvent ae) {
      Component c = EhcacheRuntimeStatsPanel.this;
      Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, c);
      ManageMessage msg = new ManageStatisticsMessage(frame, appContext, cacheManagerModel);
      int result = JOptionPane.showConfirmDialog(c, msg, msg.getTitle(), JOptionPane.OK_CANCEL_OPTION);
      if (result == JOptionPane.OK_OPTION) {
        Boolean toNewcomers = null;
        if (msg.hasApplyToNewcomersToggle()) {
          toNewcomers = Boolean.valueOf(msg.shouldApplyToNewcomers());
        }
        msg.apply(toNewcomers);
        testDismissQueryForStatsMessage();
      }
      msg.tearDown();
    }
  }

  private void testDismissQueryForStatsMessage() {
    if (queryForStatsMessage.isShowing()) {
      JDialog queryForStatsDialog = (JDialog) SwingUtilities.getAncestorOfClass(JDialog.class, queryForStatsMessage);
      if (queryForStatsDialog != null) {
        queryForStatsDialog.setVisible(false);
      }
    }
  }

  public void hierarchyChanged(HierarchyEvent e) {
    if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
      if (isShowing()) {
        if (cacheManagerModel.getStatisticsEnabledCount() < cacheManagerModel.getCacheModelInstanceCount()
            && queryForStatsMessage.shouldShowAgain()) {
          Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
          int answer = JOptionPane.showConfirmDialog(this, queryForStatsMessage, frame.getTitle(),
                                                     JOptionPane.YES_NO_OPTION);
          if (answer == JOptionPane.YES_OPTION) {
            appContext.submit(new Runnable() {
              public void run() {
                cacheManagerModel.setStatisticsEnabled(true, true);
              }
            });
          }
          if (!queryForStatsMessage.shouldShowAgain()) {
            removeHierarchyListener(EhcacheRuntimeStatsPanel.this);
          }
        }
      }
    }
  }

  public void treeNodesChanged(TreeModelEvent e) {
    /**/
  }

  public void treeNodesInserted(TreeModelEvent e) {
    Object parent = e.getTreePath().getLastPathComponent();
    if (parent instanceof ClientsNode) {
      for (Object child : e.getChildren()) {
        ClientNode clientNode = (ClientNode) child;
        IClient client = clientNode.getClient();
        CacheManagerInstance cmi = cacheManagerModel.getInstance(client);
        if (cmi != null) {
          for (CacheModelInstance cacheModelInstance : cmi.cacheModelInstances()) {
            CacheModelInstanceNode node = new CacheModelInstanceNode(cacheModelInstance);
            node.setName(cacheModelInstanceName(cacheModelInstance));
            clientNode.addChild(node);
          }
        }
      }
      elementChooser.expandPath(new TreePath(clientsNode.getPath()));
    }
  }

  public void treeNodesRemoved(TreeModelEvent e) {
    for (Object child : e.getChildren()) {
      removePagesForNode((XTreeNode) child);
    }
  }

  public void treeStructureChanged(TreeModelEvent e) {
    /**/
  }

  @Override
  public void tearDown() {
    cacheManagerModel.removeCacheManagerModelListener(cacheModelListener);
    for (CacheManagerInstance cmi : cacheManagerModel.cacheManagerInstances()) {
      cmi.removeCacheManagerInstanceListener(cacheModelInstanceListener);
    }

    chartPagedView.removePropertyChangeListener(this);
    elementChooser.removeActionListener(this);

    super.tearDown();
  }
}

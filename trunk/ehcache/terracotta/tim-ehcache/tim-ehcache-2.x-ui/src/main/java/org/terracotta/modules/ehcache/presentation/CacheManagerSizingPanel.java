/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation;

import org.apache.commons.lang.StringUtils;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerInstance;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerModel;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerModelAdapter;

import com.tc.admin.ClusterElementChooser;
import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.PagedView;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XRootNode;
import com.tc.admin.common.XTreeModel;
import com.tc.admin.common.XTreeNode;
import com.tc.admin.dso.ClientNode;
import com.tc.admin.dso.ClientsNode;
import com.tc.admin.model.IClient;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
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
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class CacheManagerSizingPanel extends BaseClusterModelPanel implements ActionListener,
    PropertyChangeListener, TreeModelListener, HierarchyListener {
  private final CacheManagerModel      cacheManagerModel;
  private CacheManagerListener         cacheManagerAdapter;

  private static final Font            LABEL_FONT = new Font("Dialog", Font.BOLD, 12);

  private ElementChooser               elementChooser;
  private ClientsNode                  clientsNode;
  private PagedView                    pagedView;
  private XContainer                   cacheManagerSizesPanel;

  private final ManageStatisticsAction manageStatsAction;
  private final QueryForStatsMessage   queryForStatsMessage;

  public static final String           ENTRIES    = "Entries";
  public static final String           BYTES      = "Bytes";

  public CacheManagerSizingPanel(ApplicationContext appContext, CacheManagerModel cacheManagerModel) {
    super(appContext, cacheManagerModel.getClusterModel());
    this.cacheManagerModel = cacheManagerModel;
    this.cacheManagerModel.addCacheManagerModelListener(cacheManagerAdapter = new CacheManagerListener());

    manageStatsAction = new ManageStatisticsAction();
    addHierarchyListener(this);
    queryForStatsMessage = new QueryForStatsMessage(manageStatsAction);
  }

  @Override
  public void clientConnected(IClient client) {
    /**/
  }

  @Override
  public void clientDisconnected(IClient client) {
    String name = client.toString();
    if (pagedView.hasPage(name)) {
      ClientCacheManagerSizingPanel panel = (ClientCacheManagerSizingPanel) pagedView.getPage(name);
      if (panel != null) {
        pagedView.removePage(name);
        panel.tearDown();
      }
    }
  }

  private class CacheManagerListener extends CacheManagerModelAdapter {
    @Override
    public void instanceAdded(CacheManagerInstance instance) {
      IClient client = instance.getClient();
      String name = client.toString();
      if (pagedView.hasPage(name)) {
        boolean isCurrentPage = pagedView.getPage().equals(name);
        ClientCacheManagerSizingPanel panel = (ClientCacheManagerSizingPanel) pagedView.getPage(name);
        if (panel != null) {
          pagedView.removePage(name);
          panel.tearDown();
        }
        if (isCurrentPage) {
          pagedView.addPage(createClientSizingPanel(client));
          pagedView.setPage(name);
        }
      }
    }

    @Override
    public void instanceRemoved(CacheManagerInstance instance) {
      IClient client = instance.getClient();
      String name = client.toString();
      if (pagedView.hasPage(name)) {
        ClientCacheManagerSizingPanel panel = (ClientCacheManagerSizingPanel) pagedView.getPage(name);
        if (panel != null) {
          pagedView.removePage(name);
          panel.tearDown();
        }
      }
    }
  }

  @Override
  protected void init() {
    elementChooser.setupTreeModel();

    pagedView.removeAll();
    pagedView.addPropertyChangeListener(this);
  }

  private JComponent createClientSizingPanel(IClient client) {
    ClientCacheManagerSizingPanel result = new ClientCacheManagerSizingPanel(appContext, cacheManagerModel,
                                                                                     cacheManagerModel
                                                                                         .getInstance(client));
    result.setup();
    result.setName(client.toString());
    return result;
  }

  public void propertyChange(PropertyChangeEvent evt) {
    String prop = evt.getPropertyName();
    if (PagedView.PROP_CURRENT_PAGE.equals(prop)) {
      String newPage = (String) evt.getNewValue();
      elementChooser.setSelectedPath(newPage);
    }
  }

  private class ElementChooser extends ClusterElementChooser {
    ElementChooser() {
      super(cacheManagerModel.getClusterModel(), CacheManagerSizingPanel.this);
    }

    @Override
    protected TreeModel createTreeModel() {
      return new XTreeModel();
    }

    @Override
    protected XTreeNode[] createTopLevelNodes() {
      clientsNode = new ClientsNode(appContext, cacheManagerModel.getClusterModel()) {
        @Override
        protected void updateLabel() {/**/
        }

        @Override
        public XTreeModel getModel() {
          return (XTreeModel) treeModel;
        }
      };
      clientsNode.setLabel(appContext.getString("runtime.stats.per.client.view"));
      treeModel.addTreeModelListener(CacheManagerSizingPanel.this);
      ((XTreeModel) treeModel).setRoot(clientsNode);
      return new XTreeNode[] {};
    }

    @Override
    protected boolean acceptPath(TreePath path) {
      return path.getLastPathComponent() instanceof ClientNode;
    }

    @Override
    public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
      XTreeNode node = (XTreeNode) event.getPath().getLastPathComponent();
      if (node == clientsNode) { throw new ExpandVetoException(event); }
    }

    public void expandPath(TreePath treePath) {
      tree.expandPath(treePath);
      treeModelChanged();
    }

    private void resetTreeModel() {
      ((XTreeModel) treeModel).setRoot(new XRootNode());
    }
  }

  public void actionPerformed(ActionEvent e) {
    ElementChooser chsr = (ElementChooser) e.getSource();
    XTreeNode node = (XTreeNode) chsr.getSelectedObject();
    String name = node.getName();
    if (!pagedView.hasPage(name)) {
      if (node instanceof ClientNode) {
        IClient client = ((ClientNode) node).getClient();
        pagedView.addPage(createClientSizingPanel(client));
      }
    }
    Component child = pagedView.getPage(name);
    if (child instanceof ClientCacheManagerSizingPanel) {
      updateSizingMessage(((ClientCacheManagerSizingPanel) child).getCacheManagerInstance());
    } else {
      cacheManagerSizesPanel.setVisible(false);
    }
    pagedView.setPage(name);
  }

  @Override
  protected XContainer createMainPanel() {
    XContainer result = new XContainer(new BorderLayout());
    result.add(createSelectorPanel(), BorderLayout.NORTH);
    result.add(pagedView = new PagedView(), BorderLayout.CENTER);
    return result;
  }

  private void refreshCurrentPage() {
    Component currentPage = pagedView.getPage(pagedView.getPage());
    if (currentPage instanceof ClientCacheManagerSizingPanel) {
      ((ClientCacheManagerSizingPanel) currentPage).refresh();
    }
  }

  private XContainer createSelectorPanel() {
    XContainer panel = new XContainer(new BorderLayout());
    cacheManagerSizesPanel = new XContainer(new GridBagLayout());
    panel.add(cacheManagerSizesPanel, BorderLayout.CENTER);
    XContainer clusterElementChooserPanel = new XContainer(new FlowLayout());
    XLabel label;
    clusterElementChooserPanel.add(label = new XLabel("Select View:"));
    label.setFont(LABEL_FONT);
    clusterElementChooserPanel.add(elementChooser = new ElementChooser());
    panel.add(clusterElementChooserPanel, BorderLayout.EAST);
    panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.black));
    return panel;
  }

  public void updateSizingMessage(CacheManagerInstance cmi) {
    cacheManagerSizesPanel.removeAll();

    if (cmi != null) {
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.gridx = gbc.gridy = 0;
      gbc.fill = GridBagConstraints.NONE;
      gbc.weightx = 0.0;
      gbc.insets = new Insets(1, 5, 1, 5);

      if (cmi.hasSizeBasedPooling()) {
        StringBuilder sb = new StringBuilder("<html>This CacheManager is using size-based pooling for ");
        List<String> pooledTiers = new ArrayList<String>();
        if (cmi.getMaxBytesLocalHeap() > 0) {
          pooledTiers.add("local heap");
        }
        if (cmi.getMaxBytesLocalOffHeap() > 0) {
          pooledTiers.add("local offheap");
        }
        if (cmi.getMaxBytesLocalDisk() > 0) {
          pooledTiers.add("local disk");
        }
        sb.append(StringUtils.join(pooledTiers.toArray(new String[0]), ", "));
        sb.append(".");
        sb.append("</html>");
        cacheManagerSizesPanel.add(new XLabel(sb.toString()), gbc);
      } else if (cmi.hasSizeBasedCache()) {
        cacheManagerSizesPanel
            .add(new XLabel(
                            "This non-pooled CacheManager can contain a mix of size-based and entry count-based caches.",
                            EhcachePresentationUtils.ALERT_ICON), gbc);
      }
      if (cacheManagerSizesPanel.getComponentCount() > 0) {
        gbc.gridx++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        cacheManagerSizesPanel.add(new XLabel(), gbc);
        cacheManagerSizesPanel.setVisible(true);
      }
    }
  }

  public void treeNodesChanged(TreeModelEvent e) {
    /**/
  }

  public void treeNodesInserted(TreeModelEvent e) {
    Object parent = e.getTreePath().getLastPathComponent();
    if (parent instanceof ClientsNode) {
      elementChooser.expandPath(new TreePath(clientsNode.getPath()));

      if (elementChooser.getSelectedPath() == null) {
        for (Object child : e.getChildren()) {
          elementChooser.setSelectedPath(new TreePath(((ClientNode) child).getPath()));
          return;
        }
      }
    }
  }

  public void treeNodesRemoved(TreeModelEvent e) {
    for (Object child : e.getChildren()) {
      pagedView.removePage(((XTreeNode) child).getName());
    }
  }

  public void treeStructureChanged(TreeModelEvent e) {
    /**/
  }

  private class ManageStatisticsAction extends AbstractAction {
    public void actionPerformed(ActionEvent ae) {
      Component c = CacheManagerSizingPanel.this;
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
            cacheManagerModel.setStatisticsEnabled(true, true);
            refreshCurrentPage();
          }
          if (!queryForStatsMessage.shouldShowAgain()) {
            removeHierarchyListener(CacheManagerSizingPanel.this);
          }
        }
      }
    }
  }

  @Override
  public void tearDown() {
    cacheManagerModel.removeCacheManagerModelListener(cacheManagerAdapter);
    elementChooser.resetTreeModel();
    pagedView.removePropertyChangeListener(this);
    elementChooser.removeActionListener(this);
    super.tearDown();
  }
}

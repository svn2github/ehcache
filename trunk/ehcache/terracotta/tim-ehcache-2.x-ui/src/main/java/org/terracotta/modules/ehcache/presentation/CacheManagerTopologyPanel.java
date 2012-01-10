/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.ehcache.presentation;

import org.terracotta.modules.ehcache.presentation.TopologyPanel.Mode;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerInstance;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerInstanceListener;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerModel;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerModelListener;
import org.terracotta.modules.ehcache.presentation.model.CacheModel;
import org.terracotta.modules.ehcache.presentation.model.CacheModelInstance;
import org.terracotta.modules.ehcache.presentation.model.ClusteredCacheModel;
import org.terracotta.modules.ehcache.presentation.model.StandaloneCacheModel;

import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.ExceptionHelper;
import com.tc.admin.common.XCheckBox;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XObjectTable;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XTable;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;

public class CacheManagerTopologyPanel extends TopologyPanelPage implements CacheManagerModelListener,
    CacheManagerInstanceListener, ListSelectionListener {
  private final CacheManagerModel   cacheManagerModel;

  private final TopologyPanel       topologyPanel;
  private GridBagLayout             layout;
  private Set<CacheManagerInstance> selectedCacheManagerInstances;
  private NodeSummaryTableModel     nodeSummaryTableModel;
  private XObjectTable              nodeSummaryTable;
  private NodeInstanceTableModel    nodeInstanceTableModel;
  private XObjectTable              nodeInstanceTable;
  private XContainer                nodeInstancePanelHolder;
  private XContainer                nodeInstancePanel;

  public CacheManagerTopologyPanel(TopologyPanel topologyPanel) {
    super(topologyPanel.getApplicationContext(), topologyPanel.getCacheManagerModel());
    this.topologyPanel = topologyPanel;
    this.cacheManagerModel = topologyPanel.getCacheManagerModel();
    setName(Mode.CACHE_MANAGER.toString());
  }

  public CacheManagerTopologyPanel(TopologyPanel topologyPanel, Set<CacheManagerInstance> selectedCacheManagerInstances) {
    this(topologyPanel);
    this.selectedCacheManagerInstances = selectedCacheManagerInstances;
  }

  @Override
  public void setup() {
    super.setup();
    this.cacheManagerModel.addCacheManagerModelListener(this);
    revalidate();
    repaint();
  }

  @Override
  protected void resume() {
    super.resume();
  }

  @Override
  protected void init() {
    for (Iterator<CacheManagerInstance> iter = cacheManagerModel.cacheManagerInstanceIterator(); iter.hasNext();) {
      CacheManagerInstance instance = iter.next();
      nodeSummaryTableModel.add(instance);
      instance.addCacheManagerInstanceListener(this);
    }
    nodeSummaryTableModel.fireTableDataChanged();
    if (selectedCacheManagerInstances != null) {
      nodeSummaryTable.setSelection(selectedCacheManagerInstances.toArray(new CacheManagerInstance[0]));
      selectedCacheManagerInstances = null;
    } else if (nodeSummaryTableModel.getRowCount() > 0) {
      CacheManagerInstance instance = (CacheManagerInstance) nodeSummaryTableModel.getObjectAt(0);
      if (instance.getInstanceCount() > 0) {
        nodeSummaryTable.setSelectedRow(0);
      }
    }
  }

  @Override
  protected XContainer createMainPanel() {
    XContainer panel = new XContainer(layout = new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(1, 1, 1, 1);
    gbc.gridx = gbc.gridy = 0;
    gbc.weightx = gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    panel.add(createNodeSummaryPanel(), gbc);
    gbc.gridy++;

    nodeInstancePanel = createNodeInstancePanel();
    nodeInstancePanelHolder = new XContainer(new BorderLayout());
    gbc.weighty = 0;
    panel.add(nodeInstancePanelHolder, gbc);

    return panel;
  }

  private XContainer createSelectAllPanel(XTable table) {
    XContainer panel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(new XLabel(), gbc);
    gbc.gridx++;
    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    XCheckBox selectAllToggle = new XCheckBox("Select/De-select All");
    panel.add(selectAllToggle, gbc);
    selectAllToggle.addActionListener(new SelectAllToggleListener(table));
    return panel;
  }

  private XContainer createNodeSummaryPanel() {
    XContainer panel = new XContainer(new BorderLayout());
    String title = MessageFormat.format(bundle.getString("node.summary"), cacheManagerModel.getName());
    panel.setBorder(BorderFactory.createTitledBorder(title));
    nodeSummaryTableModel = new NodeSummaryTableModel();
    nodeSummaryTable = new XObjectTable(nodeSummaryTableModel);
    nodeSummaryTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    nodeSummaryTable.getSelectionModel().addListSelectionListener(this);
    panel.add(createSelectAllPanel(nodeSummaryTable), BorderLayout.SOUTH);
    panel.add(new XScrollPane(nodeSummaryTable));
    nodeSummaryTable.setDefaultRenderer(Boolean.class, new BooleanRenderer());
    JPopupMenu popup = topologyPanel.createPopup();
    nodeSummaryTable.setPopupMenu(popup);
    popup.addPopupMenuListener(new PopupMenuAdapter() {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        updateActions(nodeSummaryTable);
      }
    });

    return panel;
  }

  /*
   * This is the selection listener for the nodeSummaryTable.
   */
  public void valueChanged(ListSelectionEvent e) {
    if (e == null || !e.getValueIsAdjusting()) {
      nodeInstanceTableModel.clear();

      boolean showNodeInstancePanel = false;
      CacheManagerInstance cacheManagerInstance = null;
      int[] rows = nodeSummaryTable.getSelectedRows();
      if (rows.length == 1) {
        cacheManagerInstance = (CacheManagerInstance) nodeSummaryTableModel.getObjectAt(rows[0]);
        if (cacheManagerInstance.getInstanceCount() > 0) {
          for (Iterator<CacheModelInstance> iter = cacheManagerInstance.cacheModelInstanceIter(); iter.hasNext();) {
            nodeInstanceTableModel.add(iter.next());
          }
          nodeInstanceTableModel.fireTableDataChanged();
          String title = MessageFormat.format(bundle.getString("node.instance.detail"), cacheManagerModel.getName(),
                                              cacheManagerInstance.getClient());
          nodeInstancePanel.setBorder(BorderFactory.createTitledBorder(title));
          showNodeInstancePanel = true;
        }
      }
      setNodeInstancePanelVisible(showNodeInstancePanel);
      updateActions(nodeSummaryTable);
    }
  }

  @Override
  public void updateActions(XTable table) {
    int cacheInstanceCount = 0;
    int clusteredCacheInstanceCount = 0;
    int enabled = 0;
    int statistics = 0;
    int bulkload = 0;
    boolean anySelected = false;

    if (table == nodeSummaryTable) {
      Set<CacheManagerInstance> cacheManagerInstances = getSelectedCacheManagerInstances();
      for (CacheManagerInstance cmi : cacheManagerInstances) {
        cacheInstanceCount += cmi.getInstanceCount();
        clusteredCacheInstanceCount += cmi.getTerracottaClusteredInstanceCount();
        enabled += cmi.getEnabledCount();
        statistics += cmi.getStatisticsEnabledCount();
        bulkload += cmi.getBulkLoadEnabledCount();
      }
      anySelected = cacheManagerInstances.size() > 0;
    } else {
      Set<CacheModelInstance> cacheModelInstances = getSelectedCacheModelInstances();
      for (CacheModelInstance cmi : cacheModelInstances) {
        if (cmi.isEnabled()) {
          enabled++;
        }
        if (cmi.isStatisticsEnabled()) {
          statistics++;
        }
        if (cmi.isTerracottaClustered()) {
          clusteredCacheInstanceCount++;
        }
        if (cmi.isBulkLoadEnabled()) {
          bulkload++;
        }
      }
      anySelected = cacheModelInstances.size() > 0;
    }

    topologyPanel.disableCachesAction.setEnabled(enabled > 0);
    topologyPanel.enableCachesAction.setEnabled(enabled < cacheInstanceCount);
    topologyPanel.disableStatisticsAction.setEnabled(statistics > 0);
    topologyPanel.enableStatisticsAction.setEnabled(statistics < cacheInstanceCount);
    topologyPanel.disableBulkLoadingAction.setEnabled(bulkload > 0);
    topologyPanel.enableBulkLoadingAction.setEnabled(bulkload < clusteredCacheInstanceCount);
    topologyPanel.clearCachesAction.setEnabled(enabled > 0);
    topologyPanel.showConfigAction.setEnabled(anySelected);
  }

  private void setNodeInstancePanelVisible(boolean visible) {
    if (visible) {
      if (nodeInstancePanel.getParent() == null) {
        nodeInstancePanelHolder.add(nodeInstancePanel);
        GridBagConstraints gbc = layout.getConstraints(nodeInstancePanelHolder);
        gbc.weighty = 1.0;
        layout.setConstraints(nodeInstancePanelHolder, gbc);
      }
    } else {
      if (nodeInstancePanel.getParent() != null) {
        nodeInstancePanelHolder.remove(nodeInstancePanel);
        GridBagConstraints gbc = layout.getConstraints(nodeInstancePanelHolder);
        gbc.weighty = 0.0;
        layout.setConstraints(nodeInstancePanelHolder, gbc);
      }
    }

    nodeInstancePanel.setVisible(visible);

    nodeInstancePanelHolder.revalidate();
    nodeInstancePanelHolder.repaint();
  }

  private XContainer createNodeInstancePanel() {
    XContainer panel = new XContainer(new BorderLayout());
    nodeInstanceTableModel = new NodeInstanceTableModel();
    nodeInstanceTable = new XObjectTable(nodeInstanceTableModel);
    nodeInstanceTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
          updateActions(nodeInstanceTable);
        }
      }
    });
    panel.add(createSelectAllPanel(nodeInstanceTable), BorderLayout.SOUTH);
    panel.add(new XScrollPane(nodeInstanceTable));
    nodeInstanceTable.setDefaultRenderer(Boolean.class, new BooleanRenderer());
    nodeInstanceTable.getColumnModel().getColumn(3).setCellRenderer(new ModeRenderer());
    JPopupMenu popup = topologyPanel.createPopup();
    nodeInstanceTable.setPopupMenu(popup);
    popup.addPopupMenuListener(new PopupMenuAdapter() {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        updateActions(nodeInstanceTable);
      }
    });
    return panel;
  }

  public void instanceAdded(final CacheManagerInstance instance) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        nodeSummaryTableModel.add(instance);
        int row = nodeSummaryTableModel.getRowCount() - 1;
        nodeSummaryTableModel.fireTableRowsInserted(row, row);
        if (row == 0 && instance.getInstanceCount() > 0) {
          nodeSummaryTable.setSelectedRow(0);
        }
        instance.addCacheManagerInstanceListener(CacheManagerTopologyPanel.this);
      }
    });
  }

  public void instanceRemoved(final CacheManagerInstance instance) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        int row = nodeSummaryTableModel.getObjectIndex(instance);
        if (row != -1) {
          nodeSummaryTableModel.remove(instance);
          nodeSummaryTableModel.fireTableRowsDeleted(row, row);
        }
        instance.removeCacheManagerInstanceListener(CacheManagerTopologyPanel.this);
      }
    });
  }

  public void cacheModelAdded(final CacheModel cacheModel) {
    /**/
  }

  public void cacheModelRemoved(final CacheModel cacheModel) {
    /**/
  }

  public void cacheModelChanged(final CacheModel cacheModel) {
    /**/
  }

  public Set<CacheManagerInstance> getSelectedCacheManagerInstances() {
    return Collections.unmodifiableSet(new HashSet(Arrays.asList(nodeSummaryTable.getSelection())));
  }

  public Set<CacheModelInstance> getSelectedCacheModelInstances() {
    return Collections.unmodifiableSet(new HashSet(Arrays.asList(nodeInstanceTable.getSelection())));
  }

  public Set<CacheModel> getSelectedCacheModels() {
    Set<CacheModel> result = new HashSet<CacheModel>();
    for (CacheModelInstance cmi : getSelectedCacheModelInstances()) {
      result.add(cmi.getCacheModel());
    }
    return result;
  }

  private boolean isCacheManagerInstanceSelected(CacheManagerInstance instance) {
    Set<CacheManagerInstance> nodeSummarySelection = new HashSet(Arrays.asList(nodeSummaryTable.getSelection()));
    return nodeSummarySelection.contains(instance);
  }

  private void updateCacheManagerInstanceRow(CacheManagerInstance instance) {
    int row = nodeSummaryTableModel.getObjectIndex(instance);
    if (row != -1) {
      nodeSummaryTableModel.fireTableRowsUpdated(row, row);
    }
  }

  public void cacheManagerInstanceChanged(CacheManagerInstance cacheManagerInstance) {
    /**/
  }

  public void cacheModelInstanceAdded(final CacheModelInstance instance) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        CacheManagerInstance cacheManagerInstance = instance.getCacheManagerInstance();
        if (isCacheManagerInstanceSelected(cacheManagerInstance)) {
          nodeInstanceTableModel.add(instance);
          int row = nodeInstanceTableModel.getRowCount() - 1;
          nodeInstanceTableModel.fireTableRowsInserted(row, row);
          setNodeInstancePanelVisible(true);
        }
        updateCacheManagerInstanceRow(cacheManagerInstance);
      }
    });
  }

  public void cacheModelInstanceChanged(final CacheModelInstance instance) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        if (isCacheManagerInstanceSelected(instance.getCacheManagerInstance())) {
          int row = nodeInstanceTableModel.getObjectIndex(instance);
          if (row != -1) {
            nodeInstanceTableModel.fireTableRowsUpdated(row, row);
          }
        }
        updateCacheManagerInstanceRow(instance.getCacheManagerInstance());
      }
    });
  }

  public void cacheModelInstanceRemoved(final CacheModelInstance instance) {
    final CacheManagerInstance parent = instance.getCacheManagerInstance();
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        if (isCacheManagerInstanceSelected(parent)) {
          int row = nodeInstanceTableModel.getObjectIndex(instance);
          if (row != -1) {
            nodeInstanceTableModel.remove(instance);
            nodeInstanceTableModel.fireTableRowsDeleted(row, row);
            if (nodeInstanceTableModel.getRowCount() == 0) {
              setNodeInstancePanelVisible(false);
            }
          }
        }
        updateCacheManagerInstanceRow(parent);
      }
    });
  }

  public void clusteredCacheModelAdded(ClusteredCacheModel cacheModel) {
    /**/
  }

  public void clusteredCacheModelRemoved(ClusteredCacheModel cacheModel) {
    /**/
  }

  public void clusteredCacheModelChanged(ClusteredCacheModel cacheModel) {
    /**/
  }

  public void standaloneCacheModelAdded(StandaloneCacheModel cacheModel) {
    /**/
  }

  public void standaloneCacheModelRemoved(StandaloneCacheModel cacheModel) {
    /**/
  }

  public void standaloneCacheModelChanged(StandaloneCacheModel cacheModel) {
    /**/
  }

  // Workers

  private class ClearCachesWorker extends BasicWorker<Void> {
    private ClearCachesWorker(final JPopupMenu popupMenu) {
      super(new Callable<Void>() {
        public Void call() throws Exception {
          if (popupMenu == nodeSummaryTable.getPopupMenu()) {
            Set<CacheManagerInstance> cacheManagerInstances = getSelectedCacheManagerInstances();
            if (cacheManagerInstances.size() == cacheManagerModel.getInstanceCount()) {
              cacheManagerModel.clearAllCaches();
            } else {
              for (CacheManagerInstance cacheManagerInstance : cacheManagerInstances) {
                cacheManagerInstance.clearAll();
              }
            }
          } else {
            CacheManagerInstance cacheManagerInstance = (CacheManagerInstance) nodeSummaryTableModel
                .getObjectAt(nodeSummaryTable.getSelectedRow());
            Set<CacheModelInstance> cacheModelInstances = getSelectedCacheModelInstances();
            if (cacheModelInstances.size() == cacheManagerInstance.getInstanceCount()) {
              cacheManagerInstance.clearAll();
            } else {
              for (CacheModelInstance cacheModelInstance : cacheModelInstances) {
                cacheModelInstance.removeAll();
              }
            }
          }
          return null;
        }
      });
    }

    @Override
    protected void finished() {
      Exception e = getException();
      if (e != null) {
        Throwable rootCause = ExceptionHelper.getRootCause(e);
        if (!(rootCause instanceof IOException)) {
          appContext.log(e);
        }
      }
    }
  }

  @Override
  protected BasicWorker<Void> createClearCachesWorker(JPopupMenu popupMenu) {
    return new ClearCachesWorker(popupMenu);
  }

  private class EnableCachesWorker extends BasicWorker<Void> {
    private EnableCachesWorker(final JPopupMenu popupMenu, final boolean enable, final boolean flush) {
      super(new Callable<Void>() {
        public Void call() throws Exception {
          if (popupMenu == nodeSummaryTable.getPopupMenu()) {
            Set<CacheManagerInstance> cacheManagerInstances = getSelectedCacheManagerInstances();
            if (cacheManagerInstances.size() == cacheManagerModel.getInstanceCount()) {
              cacheManagerModel.setCachesEnabled(enable, true);
              if (flush) {
                cacheManagerModel.clearAllCaches();
              }
            } else {
              for (CacheManagerInstance cacheManagerInstance : cacheManagerInstances) {
                cacheManagerInstance.setCachesEnabled(enable);
              }
            }
          } else {
            CacheManagerInstance cacheManagerInstance = (CacheManagerInstance) nodeSummaryTableModel
                .getObjectAt(nodeSummaryTable.getSelectedRow());
            Set<CacheModelInstance> cacheModelInstances = getSelectedCacheModelInstances();
            if (cacheModelInstances.size() == cacheManagerInstance.getInstanceCount()) {
              cacheManagerInstance.setCachesEnabled(enable);
            } else {
              for (CacheModelInstance cacheModelInstance : cacheModelInstances) {
                cacheModelInstance.setEnabled(enable);
              }
            }
          }
          return null;
        }
      });
    }

    @Override
    protected void finished() {
      Exception e = getException();
      if (e != null) {
        Throwable rootCause = ExceptionHelper.getRootCause(e);
        if (!(rootCause instanceof IOException)) {
          appContext.log(e);
        }
      }
    }
  }

  @Override
  protected BasicWorker<Void> createEnableCachesWorker(JPopupMenu popupMenu, boolean enable, boolean flush) {
    return new EnableCachesWorker(popupMenu, enable, flush);
  }

  private class StatisticsControlWorker extends BasicWorker<Void> {
    private StatisticsControlWorker(final JPopupMenu popupMenu, final boolean enable) {
      super(new Callable<Void>() {
        public Void call() throws Exception {
          if (popupMenu == nodeSummaryTable.getPopupMenu()) {
            Set<CacheManagerInstance> cacheManagerInstances = getSelectedCacheManagerInstances();
            if (cacheManagerInstances.size() == cacheManagerModel.getInstanceCount()) {
              cacheManagerModel.setStatisticsEnabled(enable, true);
            } else {
              for (CacheManagerInstance cacheManagerInstance : cacheManagerInstances) {
                cacheManagerInstance.setStatisticsEnabled(enable);
              }
            }
          } else {
            CacheManagerInstance cacheManagerInstance = (CacheManagerInstance) nodeSummaryTableModel
                .getObjectAt(nodeSummaryTable.getSelectedRow());
            Set<CacheModelInstance> cacheModelInstances = getSelectedCacheModelInstances();
            if (cacheModelInstances.size() == cacheManagerInstance.getInstanceCount()) {
              cacheManagerInstance.setStatisticsEnabled(enable);
            } else {
              for (CacheModelInstance cacheModelInstance : cacheModelInstances) {
                cacheModelInstance.setStatisticsEnabled(enable);
              }
            }
          }
          return null;
        }
      });
    }

    @Override
    protected void finished() {
      Exception e = getException();
      if (e != null) {
        Throwable rootCause = ExceptionHelper.getRootCause(e);
        if (!(rootCause instanceof IOException)) {
          appContext.log(e);
        }
      }
    }
  }

  @Override
  protected BasicWorker<Void> createStatisticsControlWorker(JPopupMenu popupMenu, boolean enable) {
    return new StatisticsControlWorker(popupMenu, enable);
  }

  private class BulkLoadControlWorker extends BasicWorker<Void> {
    private BulkLoadControlWorker(final JPopupMenu popupMenu, final boolean enable) {
      super(new Callable<Void>() {
        public Void call() throws Exception {
          if (popupMenu == nodeSummaryTable.getPopupMenu()) {
            Set<CacheManagerInstance> cacheManagerInstances = getSelectedCacheManagerInstances();
            if (cacheManagerInstances.size() == cacheManagerModel.getInstanceCount()) {
              cacheManagerModel.setBulkLoadEnabled(enable, true);
            } else {
              for (CacheManagerInstance cacheManagerInstance : cacheManagerInstances) {
                cacheManagerInstance.setCachesBulkLoadEnabled(enable);
              }
            }
          } else {
            CacheManagerInstance cacheManagerInstance = (CacheManagerInstance) nodeSummaryTableModel
                .getObjectAt(nodeSummaryTable.getSelectedRow());
            Set<CacheModelInstance> cacheModelInstances = getSelectedCacheModelInstances();
            if (cacheModelInstances.size() == cacheManagerInstance.getInstanceCount()) {
              cacheManagerInstance.setCachesBulkLoadEnabled(enable);
            } else {
              for (CacheModelInstance cacheModelInstance : cacheModelInstances) {
                if (!cacheModelInstance.isTransactional()) {
                  cacheModelInstance.setBulkLoadEnabled(enable);
                }
              }
            }
          }
          return null;
        }
      });
    }

    @Override
    protected void finished() {
      Exception e = getException();
      if (e != null) {
        Throwable rootCause = ExceptionHelper.getRootCause(e);
        if (!(rootCause instanceof IOException)) {
          appContext.log(e);
        }
      }
    }
  }

  @Override
  protected BasicWorker<Void> createBulkLoadControlWorker(JPopupMenu popupMenu, boolean bulkLoadEnabled) {
    return new BulkLoadControlWorker(popupMenu, bulkLoadEnabled);
  }

  private class ConfigurationGenerator implements Callable<String> {
    final JPopupMenu popupMenu;

    private ConfigurationGenerator(final JPopupMenu popupMenu) {
      this.popupMenu = popupMenu;
    }

    public String call() throws Exception {
      if (popupMenu == nodeSummaryTable.getPopupMenu()) {
        Set<CacheManagerInstance> cacheManagerInstances = getSelectedCacheManagerInstances();
        if (cacheManagerInstances.size() == cacheManagerModel.getInstanceCount()) {
          return cacheManagerModel.generateActiveConfigDeclaration();
        } else {
          StringBuilder sb = new StringBuilder();
          for (CacheManagerInstance cacheManagerInstance : cacheManagerInstances) {
            sb.append(cacheManagerInstance.generateActiveConfigDeclaration());
            sb.append("\n");
          }
          return sb.toString();
        }
      } else {
        CacheManagerInstance cacheManagerInstance = (CacheManagerInstance) nodeSummaryTableModel
            .getObjectAt(nodeSummaryTable.getSelectedRow());
        Set<CacheModelInstance> cacheModelInstances = getSelectedCacheModelInstances();
        if (cacheModelInstances.size() == cacheManagerInstance.getInstanceCount()) {
          return cacheManagerInstance.generateActiveConfigDeclaration();
        } else {
          StringBuilder sb = new StringBuilder();
          for (CacheModelInstance cacheModelInstance : cacheModelInstances) {
            sb.append(cacheModelInstance.generateActiveConfigDeclaration());
            sb.append("\n");
          }
          return sb.toString();
        }
      }
    }
  }

  @Override
  protected Callable<String> createConfigurationGenerator(JPopupMenu popupMenu) {
    return new ConfigurationGenerator(popupMenu);
  }

  @Override
  public void tearDown() {
    cacheManagerModel.removeCacheManagerModelListener(this);
    for (Iterator<CacheManagerInstance> iter = cacheManagerModel.cacheManagerInstanceIterator(); iter.hasNext();) {
      iter.next().removeCacheManagerInstanceListener(this);
    }

    nodeSummaryTableModel.clear();
    nodeInstanceTableModel.clear();

    super.tearDown();
  }
}

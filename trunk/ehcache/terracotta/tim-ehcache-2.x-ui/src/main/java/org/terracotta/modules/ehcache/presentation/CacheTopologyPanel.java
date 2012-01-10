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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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

public class CacheTopologyPanel extends TopologyPanelPage implements CacheManagerModelListener,
    CacheManagerInstanceListener, ListSelectionListener, PropertyChangeListener {
  private final TopologyPanel     topologyPanel;
  private GridBagLayout           layout;
  private Set<CacheModel>         selectedCacheModels;
  private final CacheManagerModel cacheManagerModel;
  private CacheSummaryTableModel  cacheSummaryTableModel;
  private XObjectTable            cacheSummaryTable;
  private CacheInstanceTableModel cacheInstanceTableModel;
  private XObjectTable            cacheInstanceTable;
  private XContainer              cacheInstancePanelHolder;
  private XContainer              cacheInstancePanel;

  public CacheTopologyPanel(TopologyPanel topologyPanel) {
    super(topologyPanel.getApplicationContext(), topologyPanel.getCacheManagerModel());
    this.topologyPanel = topologyPanel;
    this.cacheManagerModel = topologyPanel.getCacheManagerModel();
    setName(Mode.CACHE.toString());
  }

  public CacheTopologyPanel(TopologyPanel topologyPanel, Set<CacheModel> selectedCacheModels) {
    this(topologyPanel);
    this.selectedCacheModels = selectedCacheModels;
  }

  @Override
  public void setup() {
    super.setup();
    this.cacheManagerModel.addCacheManagerModelListener(this);
    revalidate();
    repaint();
  }

  @Override
  protected void init() {
    for (Iterator<CacheManagerInstance> iter = cacheManagerModel.cacheManagerInstanceIterator(); iter.hasNext();) {
      iter.next().addCacheManagerInstanceListener(this);
    }
    for (Iterator<CacheModel> iter = cacheManagerModel.cacheModelIterator(); iter.hasNext();) {
      CacheModel cacheModel = iter.next();
      cacheSummaryTableModel.add(cacheModel);
      cacheModel.addPropertyChangeListener(this);
    }
    cacheSummaryTableModel.fireTableDataChanged();
    if (selectedCacheModels != null) {
      cacheSummaryTable.setSelection(selectedCacheModels.toArray(new CacheModel[0]));
      selectedCacheModels = null;
    }
  }

  @Override
  protected XContainer createMainPanel() {
    XContainer panel = new XContainer(layout = new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(1, 1, 1, 1);
    gbc.gridx = gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;

    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    panel.add(createCacheSummaryPanel(), gbc);
    gbc.gridy++;

    cacheInstancePanel = createCacheInstancePanel();
    cacheInstancePanelHolder = new XContainer(new BorderLayout());
    gbc.weighty = 0.0;
    panel.add(cacheInstancePanelHolder, gbc);

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

  private XContainer createCacheSummaryPanel() {
    final XContainer panel = new XContainer(new BorderLayout());
    panel.setBorder(BorderFactory.createTitledBorder(bundle.getString("cache.summary")));
    cacheSummaryTableModel = new CacheSummaryTableModel();
    cacheSummaryTable = new XObjectTable(cacheSummaryTableModel);
    cacheSummaryTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    cacheSummaryTable.getSelectionModel().addListSelectionListener(this);
    panel.add(createSelectAllPanel(cacheSummaryTable), BorderLayout.SOUTH);
    panel.add(new XScrollPane(cacheSummaryTable));
    cacheSummaryTable.setDefaultRenderer(Boolean.class, new BooleanRenderer());
    JPopupMenu popup = topologyPanel.createPopup();
    cacheSummaryTable.setPopupMenu(popup);
    popup.addPopupMenuListener(new PopupMenuAdapter() {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        updateActions(cacheSummaryTable);
      }
    });
    return panel;
  }

  /*
   * This is the list selection listener for the cacheSummaryTable.
   */
  public void valueChanged(ListSelectionEvent e) {
    if (!e.getValueIsAdjusting()) {
      cacheInstanceTableModel.clear();

      boolean showCacheInstancePanel = false;
      CacheModel cacheModel = null;
      int[] rows = cacheSummaryTable.getSelectedRows();
      if (rows.length == 1) {
        cacheModel = (CacheModel) cacheSummaryTableModel.getObjectAt(rows[0]);
        for (CacheModelInstance cacheModelInstance : cacheModel.cacheModelInstances()) {
          cacheInstanceTableModel.add(cacheModelInstance);
        }
        cacheInstanceTableModel.fireTableDataChanged();
        String title = MessageFormat.format(bundle.getString("cache.detail"), cacheModel.getCacheName());
        cacheInstancePanel.setBorder(BorderFactory.createTitledBorder(title));
        showCacheInstancePanel = true;
      }
      setCacheInstancePanelVisible(showCacheInstancePanel);
      updateActions(cacheSummaryTable);
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

    if (table == cacheSummaryTable) {
      Set<CacheModel> cacheModels = getSelectedCacheModels();
      for (CacheModel cacheModel : cacheModels) {
        cacheInstanceCount += cacheModel.getInstanceCount();
        clusteredCacheInstanceCount += cacheModel.getTerracottaClusteredInstanceCount();
        enabled += cacheModel.getEnabledCount();
        statistics += cacheModel.getStatisticsEnabledCount();
        bulkload += cacheModel.getBulkLoadEnabledCount();
      }
      anySelected = cacheModels.size() > 0;
    } else {
      Set<CacheModelInstance> cacheModelInstances = getSelectedCacheModelInstances();
      cacheInstanceCount = cacheModelInstances.size();
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
      anySelected = cacheInstanceCount > 0;
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

  private void setCacheInstancePanelVisible(boolean visible) {
    if (visible) {
      if (cacheInstancePanel.getParent() == null) {
        cacheInstancePanelHolder.add(cacheInstancePanel);
        GridBagConstraints gbc = layout.getConstraints(cacheInstancePanelHolder);
        gbc.weighty = 1.0;
        layout.setConstraints(cacheInstancePanelHolder, gbc);
      }
    } else {
      if (cacheInstancePanel.getParent() != null) {
        cacheInstancePanelHolder.remove(cacheInstancePanel);
        GridBagConstraints gbc = layout.getConstraints(cacheInstancePanelHolder);
        gbc.weighty = 0.0;
        layout.setConstraints(cacheInstancePanelHolder, gbc);
      }
    }

    cacheInstancePanel.setVisible(visible);

    cacheInstancePanelHolder.revalidate();
    cacheInstancePanelHolder.repaint();
  }

  private XContainer createCacheInstancePanel() {
    XContainer panel = new XContainer(new BorderLayout());
    cacheInstanceTableModel = new CacheInstanceTableModel();
    cacheInstanceTable = new XObjectTable(cacheInstanceTableModel);
    cacheInstanceTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
          updateActions(cacheInstanceTable);
        }
      }
    });
    panel.add(createSelectAllPanel(cacheInstanceTable), BorderLayout.SOUTH);
    panel.add(new XScrollPane(cacheInstanceTable));
    cacheInstanceTable.setDefaultRenderer(Boolean.class, new BooleanRenderer());
    cacheInstanceTable.getColumnModel().getColumn(3).setCellRenderer(new ModeRenderer());
    JPopupMenu popup = topologyPanel.createPopup();
    cacheInstanceTable.setPopupMenu(popup);
    popup.addPopupMenuListener(new PopupMenuAdapter() {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        updateActions(cacheInstanceTable);
      }
    });
    return panel;
  }

  public void instanceAdded(final CacheManagerInstance instance) {
    instance.addCacheManagerInstanceListener(this);
  }

  public void instanceRemoved(final CacheManagerInstance instance) {
    instance.removeCacheManagerInstanceListener(this);

    int row = cacheInstanceTableModel.getObjectIndex(instance);
    if (row != -1) {
      cacheInstanceTableModel.remove(instance);
      cacheInstanceTableModel.fireTableRowsDeleted(row, row);
    }
  }

  public void cacheModelAdded(final CacheModel cacheModel) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        cacheSummaryTableModel.add(cacheModel);
        int row = cacheSummaryTableModel.getRowCount() - 1;
        cacheSummaryTableModel.fireTableRowsInserted(row, row);
        cacheModel.addPropertyChangeListener(CacheTopologyPanel.this);
      }
    });
  }

  public void cacheModelRemoved(final CacheModel cacheModel) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        int row = cacheSummaryTableModel.getObjectIndex(cacheModel);
        if (row != -1) {
          cacheSummaryTableModel.remove(cacheModel);
          cacheSummaryTableModel.fireTableRowsDeleted(row, row);
        }
        cacheModel.removePropertyChangeListener(CacheTopologyPanel.this);
      }
    });
  }

  public void cacheModelChanged(final CacheModel cacheModel) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        updateCacheSummaryRow(cacheModel);
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

  public Set<CacheModel> getSelectedCacheModels() {
    return Collections.unmodifiableSet(new HashSet(Arrays.asList(cacheSummaryTable.getSelection())));
  }

  public Set<CacheModelInstance> getSelectedCacheModelInstances() {
    return Collections.unmodifiableSet(new HashSet(Arrays.asList(cacheInstanceTable.getSelection())));
  }

  public Set<CacheManagerInstance> getSelectedCacheManagerInstances() {
    Set<CacheManagerInstance> result = new HashSet<CacheManagerInstance>();
    for (CacheModelInstance cacheModelInstance : getSelectedCacheModelInstances()) {
      result.add(cacheModelInstance.getCacheManagerInstance());
    }
    return result;
  }

  private boolean isCacheModelSelected(CacheModel cacheModel) {
    Set<CacheModel> cacheModelSelection = new HashSet(Arrays.asList(cacheSummaryTable.getSelection()));
    return cacheModelSelection.contains(cacheModel);
  }

  private void updateCacheSummaryRow(CacheModel cacheModel) {
    int row = cacheSummaryTableModel.getObjectIndex(cacheModel);
    if (row != -1) {
      cacheSummaryTableModel.fireTableRowsUpdated(row, row);
    }
  }

  public void cacheManagerInstanceChanged(CacheManagerInstance cacheManagerInstance) {
    /**/
  }

  public void cacheModelInstanceAdded(final CacheModelInstance instance) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        if (isCacheModelSelected(instance.getCacheModel())) {
          cacheInstanceTableModel.add(instance);
          int row = cacheInstanceTableModel.getRowCount() - 1;
          cacheInstanceTableModel.fireTableRowsInserted(row, row);
        }
        updateCacheSummaryRow(instance.getCacheModel());
      }
    });
  }

  public void cacheModelInstanceChanged(final CacheModelInstance instance) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        if (isCacheModelSelected(instance.getCacheModel())) {
          int row = cacheInstanceTableModel.getObjectIndex(instance);
          if (row != -1) {
            cacheInstanceTableModel.fireTableRowsUpdated(row, row);
          }
        }
        updateCacheSummaryRow(instance.getCacheModel());
      }
    });
  }

  public void cacheModelInstanceRemoved(final CacheModelInstance instance) {
    final CacheModel cacheModel = instance.getCacheModel();
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        if (isCacheModelSelected(cacheModel)) {
          int row = cacheInstanceTableModel.getObjectIndex(instance);
          if (row != -1) {
            cacheInstanceTableModel.remove(instance);
            cacheInstanceTableModel.fireTableRowsDeleted(row, row);
          }
        }
        updateCacheSummaryRow(cacheModel);
      }
    });
  }

  public void propertyChange(PropertyChangeEvent evt) {
    Object source = evt.getSource();
    if (source instanceof CacheModel) {
      CacheModel cacheModel = (CacheModel) source;
      int row = cacheSummaryTableModel.getObjectIndex(cacheModel);
      if (row != -1) {
        cacheSummaryTableModel.fireTableRowsUpdated(row, row);
      }
    }
  }

  // Workers

  private class ClearCachesWorker extends BasicWorker<Void> {
    private ClearCachesWorker(final JPopupMenu popupMenu) {
      super(new Callable<Void>() {
        public Void call() throws Exception {
          if (popupMenu == cacheSummaryTable.getPopupMenu()) {
            Set<CacheModel> cacheModels = getSelectedCacheModels();
            if (cacheModels.size() == cacheManagerModel.getCacheModelCount()) {
              cacheManagerModel.clearAllCaches();
            } else {
              for (CacheModel cacheModel : cacheModels) {
                cacheModel.removeAll();
              }
            }
          } else {
            CacheModel cacheModel = (CacheModel) cacheSummaryTableModel.getObjectAt(cacheSummaryTable.getSelectedRow());
            Set<CacheModelInstance> cacheModelInstances = getSelectedCacheModelInstances();
            if (cacheModelInstances.size() == cacheModel.getInstanceCount()) {
              cacheModel.removeAll();
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
          if (popupMenu == cacheSummaryTable.getPopupMenu()) {
            Set<CacheModel> cacheModels = getSelectedCacheModels();
            if (cacheModels.size() == cacheManagerModel.getCacheModelCount()) {
              cacheManagerModel.setCachesEnabled(enable, true);
              if (flush) {
                cacheManagerModel.clearAllCaches();
              }
            } else {
              for (CacheModel cacheModel : cacheModels) {
                cacheModel.setEnabled(enable);
                if (flush) {
                  cacheModel.removeAll();
                }
              }
            }
          } else {
            CacheModel cacheModel = (CacheModel) cacheSummaryTableModel.getObjectAt(cacheSummaryTable.getSelectedRow());
            Set<CacheModelInstance> cacheModelInstances = getSelectedCacheModelInstances();
            if (cacheModelInstances.size() == cacheModel.getInstanceCount()) {
              cacheModel.setEnabled(enable);
              if (flush) {
                cacheModel.removeAll();
              }
            } else {
              for (CacheModelInstance cacheModelInstance : cacheModelInstances) {
                cacheModelInstance.setEnabled(enable);
                if (flush) {
                  cacheModelInstance.removeAll();
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
  protected BasicWorker<Void> createEnableCachesWorker(JPopupMenu popupMenu, boolean enable, boolean flush) {
    return new EnableCachesWorker(popupMenu, enable, flush);
  }

  private class StatisticsControlWorker extends BasicWorker<Void> {
    private StatisticsControlWorker(final JPopupMenu popupMenu, final boolean enable) {
      super(new Callable<Void>() {
        public Void call() throws Exception {
          if (popupMenu == cacheSummaryTable.getPopupMenu()) {
            Set<CacheModel> cacheModels = getSelectedCacheModels();
            if (cacheModels.size() == cacheManagerModel.getCacheModelCount()) {
              cacheManagerModel.setStatisticsEnabled(enable, true);
            } else {
              for (CacheModel cacheModel : cacheModels) {
                cacheModel.setStatisticsEnabled(enable);
              }
            }
          } else {
            CacheModel cacheModel = (CacheModel) cacheSummaryTableModel.getObjectAt(cacheSummaryTable.getSelectedRow());
            Set<CacheModelInstance> cacheModelInstances = getSelectedCacheModelInstances();
            if (cacheModelInstances.size() == cacheModel.getInstanceCount()) {
              cacheModel.setStatisticsEnabled(enable);
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
          if (popupMenu == cacheSummaryTable.getPopupMenu()) {
            Set<CacheModel> cacheModels = getSelectedCacheModels();
            if (cacheModels.size() == cacheManagerModel.getCacheModelCount()) {
              cacheManagerModel.setBulkLoadEnabled(enable, true);
            } else {
              for (CacheModel cacheModel : cacheModels) {
                cacheModel.setBulkLoadEnabled(enable);
              }
            }
          } else {
            CacheModel cacheModel = (CacheModel) cacheSummaryTableModel.getObjectAt(cacheSummaryTable.getSelectedRow());
            Set<CacheModelInstance> cacheModelInstances = getSelectedCacheModelInstances();
            if (cacheModelInstances.size() == cacheModel.getInstanceCount()) {
              cacheModel.setBulkLoadEnabled(enable);
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
    private final JPopupMenu popupMenu;

    private ConfigurationGenerator(final JPopupMenu popupMenu) {
      this.popupMenu = popupMenu;
    }

    public String call() throws Exception {
      if (popupMenu == cacheSummaryTable.getPopupMenu()) {
        Set<CacheModel> cacheModels = getSelectedCacheModels();
        if (cacheModels.size() == cacheManagerModel.getCacheModelCount()) {
          return cacheManagerModel.generateActiveConfigDeclaration();
        } else {
          StringBuilder sb = new StringBuilder();
          for (CacheModel cacheModel : cacheModels) {
            sb.append(cacheModel.generateActiveConfigDeclaration());
            sb.append("\n");
          }
          return sb.toString();
        }
      } else {
        CacheModel cacheModel = (CacheModel) cacheSummaryTableModel.getObjectAt(cacheSummaryTable.getSelectedRow());
        Set<CacheModelInstance> cacheModelInstances = getSelectedCacheModelInstances();
        if (cacheModelInstances.size() == cacheModel.getInstanceCount()) {
          return cacheModel.generateActiveConfigDeclaration();
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
    for (Iterator<CacheModel> iter = cacheManagerModel.cacheModelIterator(); iter.hasNext();) {
      iter.next().removePropertyChangeListener(this);
    }

    super.tearDown();
  }
}

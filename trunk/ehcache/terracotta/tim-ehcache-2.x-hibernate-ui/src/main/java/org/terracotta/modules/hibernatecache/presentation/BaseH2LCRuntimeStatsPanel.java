/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.hibernatecache.presentation;

import static org.terracotta.modules.hibernatecache.presentation.CacheRegionUtils.CLEAR_STATS_ICON;
import static org.terracotta.modules.hibernatecache.presentation.CacheRegionUtils.DISABLE_STATS_ICON;
import static org.terracotta.modules.hibernatecache.presentation.CacheRegionUtils.ENABLE_STATS_ICON;
import net.sf.ehcache.hibernate.management.api.EhcacheStats;

import org.terracotta.modules.hibernatecache.jmx.CacheRegionStats;
import org.terracotta.modules.hibernatecache.jmx.HibernateStatsUtils;

import com.tc.admin.AbstractClusterListener;
import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.ExceptionHelper;
import com.tc.admin.common.XButton;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XObjectTable;
import com.tc.admin.common.XObjectTableModel;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XTable.PercentRenderer;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

public abstract class BaseH2LCRuntimeStatsPanel extends XContainer implements HierarchyListener, NotificationListener {
  protected final ApplicationContext    appContext;
  protected final IClusterModel         clusterModel;
  protected final ClusterListener       clusterListener;
  protected final String                persistenceUnit;
  protected final ObjectName            statsBeanObjectName;
  protected final ObjectName            statsBeanPattern;

  protected XObjectTable                regionTable;
  protected XObjectTableModel           regionTableModel;
  protected XButton                     refreshRegionsButton;
  private JToggleButton                 toggleStatsEnabledButton;
  protected XButton                     clearAllStatsButton;
  private boolean                       statsEnabled;
  protected HibernateStatsMBeanProvider beanProvider;
  protected int                         sortColumn         = -1;
  protected int                         sortDirection      = -1;

  protected static final String         CACHE_REGION_STATS = "CacheRegionStats";

  private static final ResourceBundle   bundle             = ResourceBundle.getBundle(HibernateResourceBundle.class
                                                               .getName());

  public BaseH2LCRuntimeStatsPanel(ApplicationContext appContext, IClusterModel clusterModel, String persistenceUnit) {
    super();

    this.appContext = appContext;
    this.clusterModel = clusterModel;
    this.persistenceUnit = persistenceUnit;
    this.clusterListener = new ClusterListener(clusterModel);
    this.beanProvider = new HibernateStatsMBeanProvider(clusterModel, persistenceUnit);
    try {
      statsBeanObjectName = HibernateStatsUtils.getHibernateStatsBeanName(persistenceUnit);
      statsBeanPattern = new ObjectName(statsBeanObjectName.getCanonicalName() + ",*");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected IClusterModel getClusterModel() {
    return clusterModel;
  }

  public void setup() {
    this.beanProvider.addNotificationListener(this);

    setLayout(new BorderLayout());
    add(createRegionTablePanel());
    revalidate();
    repaint();

    clusterModel.addPropertyChangeListener(clusterListener);
    if (clusterModel.isReady()) {
      init();
    }
  }

  private void init() {
    /**/
  }

  private void suspend() {
    /**/
  }

  protected JComponent createRegionTablePanel() {
    XContainer panel = new XContainer(new BorderLayout());

    regionTable = new XObjectTable(regionTableModel = new RegionTableModel()) {
      @Override
      public String getToolTipText(MouseEvent me) {
        int hitRowIndex = rowAtPoint(me.getPoint());
        int hitColIndex = columnAtPoint(me.getPoint());
        if (hitRowIndex != -1) {
          if (hitColIndex == 0) {
            CacheRegionStats crs = (CacheRegionStats) regionTableModel.getObjectAt(hitRowIndex);
            return crs.getRegion();
          } else if (hitColIndex > 1) {
            int sum = 0;
            for (int i = 0; i < regionTableModel.getRowCount(); i++) {
              Number n = (Number) regionTableModel.getValueAt(i, hitColIndex);
              sum += n.intValue();
            }
            return Integer.toString(sum) + " Total " + regionTableModel.getColumnName(hitColIndex);
          }
        }
        return super.getToolTipText(me);
      }
    };
    regionTable.getColumnModel().getColumn(1).setCellRenderer(new PercentRenderer());
    regionTable.addHierarchyListener(this);
    panel.add(new XScrollPane(regionTable), BorderLayout.CENTER);

    XContainer bottomPanel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);
    refreshRegionsButton = new XButton(appContext.getString("refresh"),
                                       new ImageIcon(
                                                     BaseH2LCRuntimeStatsPanel.class
                                                         .getResource("/com/tc/admin/icons/refresh.gif")));
    bottomPanel.add(refreshRegionsButton, gbc);
    refreshRegionsButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent ae) {
        updateRegionStats();
      }
    });
    gbc.gridx++;

    bottomPanel
        .add(toggleStatsEnabledButton = new JToggleButton(bundle.getString("disable.stats"), DISABLE_STATS_ICON), gbc);
    toggleStatsEnabledButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        try {
          queryToggleStatsEnabled();
        } catch (Exception e) {
          appContext.log(e);
        }
      }
    });
    gbc.gridx++;

    bottomPanel.add(clearAllStatsButton = new XButton(bundle.getString("clear.all.stats"), CLEAR_STATS_ICON), gbc);
    clearAllStatsButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent ae) {
        clearAllStats();
        updateRegionStats();
      }
    });

    panel.add(bottomPanel, BorderLayout.SOUTH);

    return panel;
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

  public void hierarchyChanged(final HierarchyEvent e) {
    XObjectTable table = (XObjectTable) e.getComponent();

    if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
      if (table.isShowing()) {
        if (table == regionTable) {
          updateRegionStats();
        }
      }
    }
  }

  protected class RegionStatsWorker extends BasicWorker<Map<String, ? extends CacheRegionStats>> {
    protected RegionStatsWorker(final Callable<Map<String, ? extends CacheRegionStats>> callable) {
      super(callable);
    }

    @Override
    protected void finished() {
      Exception e = getException();
      if (e != null) {
        appContext.log(ExceptionHelper.getRootCause(e));
      } else {
        Map<String, ? extends CacheRegionStats> result = getResult();
        regionTableModel.clear();
        if (result != null) {
          Iterator<String> iter = result.keySet().iterator();
          while (iter.hasNext()) {
            CacheRegionStats cacheRegionStats = result.get(iter.next());
            if (cacheRegionStats != null) {
              regionTableModel.add(cacheRegionStats);
            }
          }
          sortTable(regionTable);
        }
      }
      refreshRegionsButton.setEnabled(true);
    }
  }

  protected abstract RegionStatsWorker createRegionStatsWorker();

  protected void updateRegionStats() {
    refreshRegionsButton.setEnabled(false);
    appContext.execute(createRegionStatsWorker());
  }

  private void queryToggleStatsEnabled() {
    XLabel msg = new XLabel(bundle.getString(statsEnabled ? "disable.stats.confirm" : "enable.stats.confirm"));
    Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
    int answer = JOptionPane.showConfirmDialog(this, msg, frame.getTitle(), JOptionPane.OK_CANCEL_OPTION);
    if (answer == JOptionPane.OK_OPTION) {
      appContext.submit(new ToggleStatsEnabledWorker());
    } else {
      toggleStatsEnabledButton.setSelected(false);
      toggleStatsEnabledButton.setEnabled(true);
    }
  }

  private class ToggleStatsEnabledWorker extends BasicWorker<Void> {
    private ToggleStatsEnabledWorker() {
      super(new Callable<Void>() {
        public Void call() throws Exception {
          setStatsEnabled(!statsEnabled);
          return null;
        }
      });
      toggleStatsEnabledButton.setEnabled(false);
    }

    @Override
    protected void finished() {
      Exception e = getException();
      if (e != null) {
        Throwable rootCause = ExceptionHelper.getRootCause(e);
        if (!(rootCause instanceof IOException)) {
          appContext.log(e);
        }
        toggleStatsEnabledButton.setSelected(false);
        toggleStatsEnabledButton.setEnabled(true);
      }
    }
  }

  private void setStatsEnabled(boolean enabled) throws Exception {
    IServer activeCoord = clusterModel.getActiveCoordinator();
    Set<ObjectName> onSet = activeCoord.queryNames(statsBeanPattern, null);
    activeCoord.invoke(onSet, enabled ? "enableStats" : "disableStats", Integer.MAX_VALUE, TimeUnit.SECONDS);
  }

  private void clearAllStats() {
    queryClearAllStats();
  }

  private void queryClearAllStats() {
    XLabel msg = new XLabel(bundle.getString("clear.all.counters.confirm"));
    Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
    int answer = JOptionPane.showConfirmDialog(this, msg, frame.getTitle(), JOptionPane.OK_CANCEL_OPTION);
    if (answer == JOptionPane.OK_OPTION) {
      appContext.submit(new ClearAllStatsWorker());
    } else {
      clearAllStatsButton.setSelected(false);
      clearAllStatsButton.setEnabled(true);
    }
  }

  private class ClearAllStatsWorker extends BasicWorker<Void> {
    private ClearAllStatsWorker() {
      super(new Callable<Void>() {
        public Void call() throws Exception {
          _clearAllStats();
          return null;
        }
      });
      clearAllStatsButton.setEnabled(false);
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
      clearAllStatsButton.setSelected(false);
      clearAllStatsButton.setEnabled(true);
    }
  }

  private void _clearAllStats() throws Exception {
    IServer activeCoord = clusterModel.getActiveCoordinator();
    Set<ObjectName> onSet = activeCoord.queryNames(statsBeanPattern, null);
    activeCoord.invoke(onSet, "clearStats", Integer.MAX_VALUE, TimeUnit.SECONDS);
  }

  private void sortTable(final XObjectTable table) {
    if (sortColumn == -1) {
      sortColumn = table.getSortColumn();
      sortDirection = table.getSortDirection();
    }

    table.setSortColumn(sortColumn);
    table.setSortDirection(sortDirection);

    table.sort();
    ((XObjectTableModel) table.getModel()).fireTableDataChanged();
  }

  private void handleStatisticsEnabled(boolean enabled) {
    statsEnabled = enabled;
    String labelKey = enabled ? "disable.stats" : "enable.stats";
    toggleStatsEnabledButton.setText(bundle.getString(labelKey));
    toggleStatsEnabledButton.setIcon(enabled ? DISABLE_STATS_ICON : ENABLE_STATS_ICON);
    toggleStatsEnabledButton.setSelected(false);
    toggleStatsEnabledButton.setEnabled(true);
  }

  public void handleNotification(final Notification notif, final Object handBack) {
    String type = notif.getType();
    if (EhcacheStats.CACHE_STATISTICS_RESET.equals(type)) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          if (regionTable.isShowing()) {
            updateRegionStats();
          }
        }
      });
    } else if (EhcacheStats.CACHE_STATISTICS_ENABLED.equals(type)) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          boolean enabled = (Boolean) notif.getUserData();
          handleStatisticsEnabled(enabled);
        }
      });
    }

  }

  @Override
  public void tearDown() {
    beanProvider.removeNotificationListener(this);
    clusterModel.removePropertyChangeListener(clusterListener);
    clusterListener.tearDown();

    regionTableModel.clear();

    super.tearDown();
  }
}

/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.hibernatecache.presentation;

import net.sf.ehcache.hibernate.management.api.EhcacheStats;

import org.terracotta.modules.hibernatecache.jmx.CollectionStats;
import org.terracotta.modules.hibernatecache.jmx.EntityStats;
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
import com.tc.admin.common.XTabbedPane;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;

import java.awt.BorderLayout;
import java.awt.Component;
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
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public abstract class BaseHibernateRuntimeStatsPanel extends XContainer implements HierarchyListener, ChangeListener,
    NotificationListener {
  protected final ApplicationContext          appContext;
  protected final IClusterModel               clusterModel;
  protected final ClusterListener             clusterListener;
  protected final String                      persistenceUnit;
  protected final HibernateStatsMBeanProvider beanProvider;
  protected final ObjectName                  statsBeanObjectName;

  protected XTabbedPane                       tabbedPane;
  protected XObjectTable                      entityTable;
  protected XObjectTableModel                 entityTableModel;
  protected XButton                           refreshEntitiesButton;
  protected XObjectTable                      collectionTable;
  protected XObjectTableModel                 collectionTableModel;
  protected XButton                           refreshCollectionsButton;
  protected XObjectTable                      queryTable;
  protected XObjectTableModel                 queryTableModel;
  protected XButton                           refreshQueriesButton;

  protected static final String               ENTITY_STATS     = "EntityStats";
  protected static final String               COLLECTION_STATS = "CollectionStats";
  protected static final String               QUERY_STATS      = "QueryStats";

  private final static String                 BUSY             = "Busy...";

  private static final ResourceBundle         bundle           = ResourceBundle.getBundle(HibernateResourceBundle.class
                                                                   .getName());

  public BaseHibernateRuntimeStatsPanel(ApplicationContext appContext, IClusterModel clusterModel,
                                        String persistenceUnit) {
    super();

    this.appContext = appContext;
    this.clusterModel = clusterModel;
    this.persistenceUnit = persistenceUnit;
    this.clusterListener = new ClusterListener(clusterModel);
    this.beanProvider = new HibernateStatsMBeanProvider(clusterModel, persistenceUnit);
    try {
      statsBeanObjectName = HibernateStatsUtils.getHibernateStatsBeanName(persistenceUnit);
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
    add(createTabbedPane());
    revalidate();
    repaint();

    clusterModel.addPropertyChangeListener(clusterListener);
    if (clusterModel.isReady()) {
      init();
    }

    addHierarchyListener(this);
  }

  private JComponent createEntityTablePanel() {
    XContainer panel = new XContainer(new BorderLayout());

    entityTable = new XObjectTable(entityTableModel = new EntityTableModel()) {
      @Override
      public String getToolTipText(MouseEvent me) {
        int hitRowIndex = rowAtPoint(me.getPoint());
        int hitColIndex = columnAtPoint(me.getPoint());
        if (hitRowIndex != -1) {
          if (hitColIndex == 0) {
            EntityStats es = (EntityStats) entityTableModel.getObjectAt(hitRowIndex);
            return es.getName();
          } else {
            int sum = 0;
            for (int i = 0; i < entityTableModel.getRowCount(); i++) {
              Number n = (Number) entityTableModel.getValueAt(i, hitColIndex);
              sum += n.intValue();
            }
            return Integer.toString(sum) + " Total " + entityTableModel.getColumnName(hitColIndex);
          }
        }
        return super.getToolTipText(me);
      }
    };
    entityTable.addHierarchyListener(this);
    panel.add(new XScrollPane(entityTable), BorderLayout.CENTER);

    XContainer bottomPanel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);
    bottomPanel.add(refreshEntitiesButton = new XButton(appContext.getString("refresh")), gbc);
    refreshEntitiesButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent ae) {
        updateEntityStats();
      }
    });
    gbc.gridx++;

    XButton clearAllStatsButton = new XButton(bundle.getString("clear.all.stats"));
    bottomPanel.add(clearAllStatsButton, gbc);
    clearAllStatsButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent ae) {
        clearAllStats();
      }
    });

    panel.add(bottomPanel, BorderLayout.SOUTH);

    return panel;
  }

  private JComponent createCollectionTablePanel() {
    XContainer panel = new XContainer(new BorderLayout());

    collectionTable = new XObjectTable(collectionTableModel = new CollectionTableModel()) {
      @Override
      public String getToolTipText(MouseEvent me) {
        int hitRowIndex = rowAtPoint(me.getPoint());
        int hitColIndex = columnAtPoint(me.getPoint());
        if (hitRowIndex != -1) {
          if (hitColIndex == 0) {
            CollectionStats cs = (CollectionStats) collectionTableModel.getObjectAt(hitRowIndex);
            return cs.getRoleName();
          } else {
            int sum = 0;
            for (int i = 0; i < collectionTableModel.getRowCount(); i++) {
              Number n = (Number) collectionTableModel.getValueAt(i, hitColIndex);
              sum += n.intValue();
            }
            return Integer.toString(sum) + " Total " + collectionTableModel.getColumnName(hitColIndex);
          }
        }
        return super.getToolTipText(me);
      }
    };
    collectionTable.addHierarchyListener(this);
    panel.add(new XScrollPane(collectionTable), BorderLayout.CENTER);

    XContainer bottomPanel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);
    refreshCollectionsButton = new XButton(appContext.getString("refresh"));
    bottomPanel.add(refreshCollectionsButton, gbc);
    refreshCollectionsButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent ae) {
        updateCollectionStats();
      }
    });
    gbc.gridx++;

    XButton clearAllCountersButton = new XButton(bundle.getString("clear.all.stats"));
    bottomPanel.add(clearAllCountersButton, gbc);
    clearAllCountersButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent ae) {
        clearAllStats();
      }
    });

    panel.add(bottomPanel, BorderLayout.SOUTH);

    return panel;
  }

  private JComponent createQueryTablePanel() {
    XContainer panel = new XContainer(new BorderLayout());

    queryTable = new XObjectTable(queryTableModel = new QueryTableModel()) {
      @Override
      public String getToolTipText(MouseEvent me) {
        int hitRowIndex = rowAtPoint(me.getPoint());
        int hitColIndex = columnAtPoint(me.getPoint());
        if (hitRowIndex != -1) {
          if (hitColIndex == 0) {
            QueryStats qs = (QueryStats) queryTableModel.getObjectAt(hitRowIndex);
            return qs.getQuery();
          } else {
            int sum = 0;
            for (int i = 0; i < queryTableModel.getRowCount(); i++) {
              Number n = (Number) queryTableModel.getValueAt(i, hitColIndex);
              sum += n.intValue();
            }
            return Integer.toString(sum) + " Total " + queryTableModel.getColumnName(hitColIndex);
          }
        }
        return super.getToolTipText(me);
      }
    };
    queryTable.addHierarchyListener(this);
    panel.add(new XScrollPane(queryTable), BorderLayout.CENTER);

    XContainer bottomPanel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);
    refreshQueriesButton = new XButton(appContext.getString("refresh"));
    bottomPanel.add(refreshQueriesButton, gbc);
    refreshQueriesButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent ae) {
        updateQueryStats();
      }
    });
    gbc.gridx++;

    XButton clearAllCountersButton = new XButton(bundle.getString("clear.all.stats"));
    bottomPanel.add(clearAllCountersButton, gbc);
    clearAllCountersButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent ae) {
        clearAllStats();
      }
    });

    panel.add(bottomPanel, BorderLayout.SOUTH);

    return panel;
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

  private void _clearAllStats() throws Exception {
    IServer activeCoord = clusterModel.getActiveCoordinator();
    ObjectName tmpl = new ObjectName(HibernateStatsUtils.STATS_BEAN_NAME_PREFIX + ",name=" + persistenceUnit + ",*");
    Set<ObjectName> onSet = activeCoord.queryNames(tmpl, null);
    activeCoord.invoke(onSet, "clearStats", Integer.MAX_VALUE, TimeUnit.SECONDS);
  }

  private XTabbedPane createTabbedPane() {
    tabbedPane = new XTabbedPane();
    tabbedPane.addTab(bundle.getString("entities"), createEntityTablePanel());
    tabbedPane.addTab(bundle.getString("collections"), createCollectionTablePanel());
    tabbedPane.addTab(bundle.getString("queries"), createQueryTablePanel());
    tabbedPane.addChangeListener(this);
    return tabbedPane;
  }

  private static int selectedTabIndex = 0;

  public void stateChanged(ChangeEvent e) {
    int index = tabbedPane.getSelectedIndex();
    setSelectedTabIndex(index);
  }

  public static void setSelectedTabIndex(int index) {
    selectedTabIndex = index;
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
    /**/
  }

  private void suspend() {
    /**/
  }

  public void hierarchyChanged(final HierarchyEvent e) {
    Component comp = e.getComponent();

    if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
      if (comp.isShowing()) {
        if (comp == entityTable) {
          updateEntityStats();
        } else if (comp == collectionTable) {
          updateCollectionStats();
        } else if (comp == queryTable) {
          updateQueryStats();
        } else {
          tabbedPane.setSelectedIndex(selectedTabIndex);
        }
      }
    }
  }

  private void sortTable(final XObjectTable table) {
    table.sort();
    ((XObjectTableModel) table.getModel()).fireTableDataChanged();
  }

  protected class EntityStatsWorker extends BasicWorker<Map<String, EntityStats>> {
    protected EntityStatsWorker(final Callable<Map<String, EntityStats>> callable) {
      super(callable);
    }

    @Override
    protected void finished() {
      Exception e = getException();
      if (e != null) {
        appContext.log(ExceptionHelper.getRootCause(e));
      } else {
        Map<String, EntityStats> result = getResult();
        entityTableModel.clear();
        if (result != null) {
          Iterator<String> iter = result.keySet().iterator();
          while (iter.hasNext()) {
            EntityStats entityStats = result.get(iter.next());
            if (entityStats != null) {
              entityTableModel.add(entityStats);
            }
          }
          sortTable(entityTable);
        }
      }
      refreshEntitiesButton.setText(appContext.getString("refresh"));
      refreshEntitiesButton.setEnabled(true);
    }
  }

  protected abstract EntityStatsWorker createEntityStatsWorker();

  protected void updateEntityStats() {
    refreshEntitiesButton.setText(BUSY);
    refreshEntitiesButton.setEnabled(false);
    appContext.execute(createEntityStatsWorker());
  }

  protected class CollectionStatsWorker extends BasicWorker<Map<String, CollectionStats>> {
    protected CollectionStatsWorker(final Callable<Map<String, CollectionStats>> callable) {
      super(callable);
    }

    @Override
    protected void finished() {
      Exception e = getException();
      if (e != null) {
        appContext.log(ExceptionHelper.getRootCause(e));
      } else {
        collectionTableModel.clear();
        Map<String, CollectionStats> result = getResult();
        if (result != null) {
          Iterator<String> iter = result.keySet().iterator();
          while (iter.hasNext()) {
            CollectionStats collectionStats = result.get(iter.next());
            if (collectionStats != null) {
              collectionTableModel.add(collectionStats);
            }
          }
          sortTable(collectionTable);
        }
      }
      refreshCollectionsButton.setText(appContext.getString("refresh"));
      refreshCollectionsButton.setEnabled(true);
    }
  }

  protected abstract CollectionStatsWorker createCollectionStatsWorker();

  private void updateCollectionStats() {
    refreshCollectionsButton.setText(BUSY);
    refreshCollectionsButton.setEnabled(false);
    appContext.execute(createCollectionStatsWorker());
  }

  protected class QueryStatsWorker extends BasicWorker<Map<String, QueryStats>> {
    protected QueryStatsWorker(final Callable<Map<String, QueryStats>> callable) {
      super(callable);
    }

    @Override
    protected void finished() {
      Exception e = getException();
      if (e != null) {
        appContext.log(ExceptionHelper.getRootCause(e));
      } else {
        Map<String, QueryStats> result = getResult();
        queryTableModel.clear();
        if (result != null) {
          Iterator<String> iter = result.keySet().iterator();
          while (iter.hasNext()) {
            QueryStats queryStats = result.get(iter.next());
            if (queryStats != null) {
              queryTableModel.add(queryStats);
            }
          }
          sortTable(queryTable);
        }
      }
      refreshQueriesButton.setText(appContext.getString("refresh"));
      refreshQueriesButton.setEnabled(true);
    }
  }

  protected abstract QueryStatsWorker createQueryStatsWorker();

  private void updateQueryStats() {
    refreshQueriesButton.setText(BUSY);
    refreshQueriesButton.setEnabled(false);
    appContext.execute(createQueryStatsWorker());
  }

  public void handleNotification(Notification notif, Object handBack) {
    if (EhcacheStats.CACHE_STATISTICS_RESET.equals(notif.getType())) {
      if (entityTable.isShowing()) {
        updateEntityStats();
      } else if (collectionTable.isShowing()) {
        updateCollectionStats();
      } else {
        updateQueryStats();
      }
    }
  }

  @Override
  public void tearDown() {
    tabbedPane.removeChangeListener(this);
    beanProvider.removeNotificationListener(this);
    clusterModel.removePropertyChangeListener(clusterListener);
    clusterListener.tearDown();

    entityTableModel.clear();
    collectionTableModel.clear();
    queryTableModel.clear();

    super.tearDown();
  }
}

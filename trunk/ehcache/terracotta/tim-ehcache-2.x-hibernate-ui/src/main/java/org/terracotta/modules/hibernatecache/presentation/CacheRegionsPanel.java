/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.hibernatecache.presentation;

import static org.terracotta.modules.hibernatecache.presentation.CacheRegionUtils.GENERATE_CONFIG_ICON;
import net.sf.ehcache.hibernate.management.api.EhcacheHibernateMBean;
import net.sf.ehcache.hibernate.management.api.EhcacheStats;

import org.terracotta.modules.hibernatecache.jmx.HibernateStatsUtils;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.ExceptionHelper;
import com.tc.admin.common.WindowHelper;
import com.tc.admin.common.XButton;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XObjectTable;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XTable.BaseRenderer;
import com.tc.admin.common.XTextArea;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.Format;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.swing.JDialog;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class CacheRegionsPanel extends XContainer implements NotificationListener, ListSelectionListener,
    PropertyChangeListener {
  private final ApplicationContext           appContext;
  private final IClusterModel                clusterModel;
  private final String                       persistenceUnit;
  private final Map<String, CacheRegionInfo> regionInfoMap;

  private XLabel                             summaryLabel;
  private XObjectTable                       table;
  private RegionInfoTableModel               tableModel;
  private CacheRegionDetailPanel             detailPanel;
  private HibernateStatsMBeanProvider        beanProvider;
  private XButton                            showConfigButton;

  private static final ResourceBundle        bundle = ResourceBundle.getBundle(HibernateResourceBundle.class.getName());

  public CacheRegionsPanel(ApplicationContext appContext, IClusterModel clusterModel, String persistenceUnit) {
    super(new BorderLayout());

    this.appContext = appContext;
    this.clusterModel = clusterModel;
    this.persistenceUnit = persistenceUnit;
    this.regionInfoMap = new HashMap<String, CacheRegionInfo>();
  }

  public void setup() {
    add(createTopPanel(), BorderLayout.NORTH);

    table = new XObjectTable(tableModel = new RegionInfoTableModel());
    table.getColumnModel().getColumn(1).setCellRenderer(new CachedColumnRenderer());
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.getSelectionModel().addListSelectionListener(this);
    detailPanel = new CacheRegionDetailPanel(appContext);
    add(new XScrollPane(table), BorderLayout.CENTER);

    appContext.submit(new RegionInfoGetter());
  }

  private XContainer createTopPanel() {
    XContainer panel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.gridx = gbc.gridy = 0;

    panel.add(summaryLabel = new XLabel(), gbc);
    gbc.gridx++;

    // filler
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    panel.add(new XLabel(), gbc);
    gbc.gridx++;

    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    showConfigButton = new XButton(bundle.getString("generate.configuration"), GENERATE_CONFIG_ICON);
    panel.add(showConfigButton, gbc);
    showConfigButton.addActionListener(new ShowConfigAction());
    showConfigButton.setEnabled(false);

    return panel;
  }

  private class ShowConfigAction implements ActionListener {
    public void actionPerformed(final ActionEvent e) {
      Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, CacheRegionsPanel.this);
      JDialog dialog = new JDialog(frame, frame.getTitle(), true);
      XTextArea textArea = new XTextArea();
      textArea.setEditable(false);
      textArea.setText(beanProvider.getBean().generateActiveConfigDeclaration());
      dialog.getContentPane().add(new XScrollPane(textArea));
      dialog.setSize(500, 600);
      WindowHelper.center(dialog);
      dialog.setVisible(true);
    }
  }

  private class RegionInfoGetter extends BasicWorker<Map<String, CacheRegionInfo>> {
    private RegionInfoGetter() {
      super(new Callable<Map<String, CacheRegionInfo>>() {
        public Map<String, CacheRegionInfo> call() throws Exception {
          Map<String, CacheRegionInfo> result = new LinkedHashMap<String, CacheRegionInfo>();
          IServer activeCoord = clusterModel.getActiveCoordinator();
          ObjectName tmpl = HibernateStatsUtils.getHibernateStatsBeanName(persistenceUnit + ",*");
          Set<ObjectName> onSet = activeCoord.queryNames(tmpl, null);
          Iterator<ObjectName> onIter = onSet.iterator();
          createBeanProvider();
          while (onIter.hasNext()) {
            EhcacheHibernateMBean statsBean = activeCoord.getMBeanProxy(onIter.next(), EhcacheHibernateMBean.class);
            if (result.size() == 0) {
              Map<String, Map<String, Object>> regionAttrs = statsBean.getRegionCacheAttributes();
              Iterator<Entry<String, Map<String, Object>>> entryIter = regionAttrs.entrySet().iterator();
              while (entryIter.hasNext()) {
                String regionName = entryIter.next().getKey();
                if (!regionName.endsWith("org.hibernate.cache.UpdateTimestampsCache")) {
                  CacheRegionInfo regionInfo = new CacheRegionInfo(regionName, regionAttrs.get(regionName),
                                                                   beanProvider);
                  result.put(regionName, regionInfo);
                  regionInfo.addPropertyChangeListener(CacheRegionsPanel.this);
                }
              }
            }
          }
          return result;
        }
      });
      regionInfoMap.clear();
    }

    @Override
    protected void finished() {
      Exception e = getException();
      if (e != null) {
        appContext.log(ExceptionHelper.getRootCause(e));
      } else {
        int clusteredRegionCount = 0;
        Map<String, CacheRegionInfo> result;
        if ((result = getResult()) != null) {
          regionInfoMap.putAll(result);
          Iterator<CacheRegionInfo> iter = regionInfoMap.values().iterator();
          while (iter.hasNext()) {
            CacheRegionInfo regionInfo = iter.next();
            tableModel.add(regionInfo);
            if (regionInfo.isEnabled()) {
              clusteredRegionCount++;
            }
          }
          tableModel.fireTableDataChanged();
          if (tableModel.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
          }
        }
        updateSummaryLabel(clusteredRegionCount);
        showConfigButton.setEnabled(true);
      }
    }
  }

  private void createBeanProvider() {
    if (beanProvider != null) {
      beanProvider.removeNotificationListener(this);
    }
    beanProvider = new HibernateStatsMBeanProvider(clusterModel, persistenceUnit);
    beanProvider.addNotificationListener(this);
  }

  private void updateSummaryLabel(final int clusteredRegionCount) {
    String format = bundle.getString("regions.summary.format");
    summaryLabel.setText(MessageFormat.format(format, clusteredRegionCount, regionInfoMap.size()));

    /*
     * TODO: need a real model.
     */
    H2LCPanel h2lcPanel = (H2LCPanel) SwingUtilities.getAncestorOfClass(H2LCPanel.class, this);
    if (h2lcPanel != null) {
      h2lcPanel.cacheRegionsChanged(clusteredRegionCount, regionInfoMap.size());
    }
  }

  public static class CachedColumnRenderer extends BaseRenderer {
    private final String on  = bundle.getString("on");
    private final String off = bundle.getString("off");

    public CachedColumnRenderer() {
      super((Format) null);
      label.setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override
    public void setValue(final Object value) {
      String text = "";
      if (value instanceof Boolean) {
        text = ((Boolean) value).booleanValue() ? on : off;
      }
      setText(text);
    }
  }

  public void valueChanged(final ListSelectionEvent e) {
    CacheRegionInfo regionInfo = null;
    if (!e.getValueIsAdjusting()) {
      int row = table.getSelectedRow();
      if (row != -1) {
        regionInfo = (CacheRegionInfo) tableModel.getObjectAt(row);
      }
      if (regionInfo != null) {
        detailPanel.setCacheRegion(regionInfo);
        add(detailPanel, BorderLayout.SOUTH);
      } else {
        remove(detailPanel);
      }
    }
  }

  public void propertyChange(final PropertyChangeEvent evt) {
    int row = tableModel.getObjectIndex(evt.getSource());
    if (row != -1) {
      tableModel.fireTableRowsUpdated(row, row);
    }
    updateSummaryLabel(determineClusteredRegionCount());
  }

  private int determineClusteredRegionCount() {
    int clusteredRegionCount = 0;
    Iterator<CacheRegionInfo> iter = regionInfoMap.values().iterator();
    while (iter.hasNext()) {
      if (iter.next().isEnabled()) {
        clusteredRegionCount++;
      }
    }
    return clusteredRegionCount;
  }

  protected ApplicationContext getApplicationContext() {
    return appContext;
  }

  protected IClusterModel getClusterModel() {
    return clusterModel;
  }

  protected String getPersistenceUnit() {
    return persistenceUnit;
  }

  public void handleNotification(Notification notif, Object handBack) {
    String type = notif.getType();
    String msg = notif.getMessage();
    Object userData = notif.getUserData();

    if (EhcacheStats.CACHE_ENABLED.equals(type)) {
      boolean enabled = (Boolean) userData;
      Iterator<CacheRegionInfo> iter = regionInfoMap.values().iterator();
      while (iter.hasNext()) {
        iter.next()._setEnabled(enabled);
      }
      tableModel.fireTableRowsUpdated(0, tableModel.getRowCount() - 1);
      detailPanel.updateRegion();
      updateSummaryLabel(determineClusteredRegionCount());
    } else if (EhcacheStats.CACHE_REGION_CHANGED.equals(type)) {
      CacheRegionInfo regionInfo = regionInfoMap.get(msg);
      if (regionInfo != null) {
        regionInfo.setAttributes((Map<String, Object>) userData);
        int row = tableModel.getObjectIndex(regionInfo);
        tableModel.fireTableRowsUpdated(row, row);
        if (table.isRowSelected(row)) {
          detailPanel.setCacheRegion(regionInfo);
        }
        updateSummaryLabel(determineClusteredRegionCount());
      }
    }
  }

  @Override
  public void tearDown() {
    regionInfoMap.clear();
    tableModel.clear();

    super.tearDown();
  }
}

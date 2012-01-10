/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation;

import static org.terracotta.modules.ehcache.presentation.EhcachePresentationUtils.CLUSTERED_ICON;
import static org.terracotta.modules.ehcache.presentation.EhcachePresentationUtils.NON_CLUSTERED_ICON;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnitSource;
import org.jfree.chart.axis.TickUnits;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.PieSectionEntity;
import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.labels.StandardPieToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.data.RangeType;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.ui.Layer;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.TextAnchor;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerInstance;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerInstanceListener;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerModel;
import org.terracotta.modules.ehcache.presentation.model.CacheModelInstance;
import org.terracotta.modules.ehcache.presentation.model.CacheTierSize;
import org.terracotta.modules.ehcache.presentation.model.SettingsCacheModel;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.ExceptionHelper;
import com.tc.admin.common.PagedView;
import com.tc.admin.common.StatusView;
import com.tc.admin.common.ThinDecimalFormat;
import com.tc.admin.common.ThinMemoryFormat;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XComboBox;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XSplitPane;
import com.tc.admin.common.XTable;
import com.tc.admin.common.XTable.BaseRenderer;
import com.tc.admin.common.XTable.PercentRenderer;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

public class ClientCacheManagerSizingPanel extends BaseClusterModelPanel implements HierarchyListener,
    CacheManagerInstanceListener {
  private final CacheManagerInstance             cacheManagerInstance;

  private Map<CacheModelInstance, CacheTierSize> cacheSizes;

  private PagedView                              pagedView;

  private XLabel                                 cacheSummaryLabel;
  private XContainer                             selectedCachePanel;
  private XComboBox                              tierChooser;
  private ActionListener                         tierChooserListener;
  private XComboBox                              selectedCacheChooser;
  private SelectedCacheListener                  selectedCacheListener;

  private XContainer                             cacheManagerCapacityPanel;

  private PiePlot                                tierUsageByCachePlot;
  private RectangleInsets                        tierUsageByCachePlotInsets;
  private ChartPanel                             tierUsageByCachePanel;
  private DefaultTableModel                      tierUsageByCacheTableModel;
  private XTable                                 tierUsageByCacheTable;
  private ListSelectionListener                  tierUsageByCacheTableSelectionListener;
  private XLabel                                 tierUsageByCacheLabel;
  private DefaultPieDataset                      tierUsageByCacheDataset;
  private StatusView                             tierUsageByCacheStatusView;

  private DefaultCategoryDataset                 localHeapCapacityDataset;
  private DefaultCategoryDataset                 localOffHeapCapacityDataset;
  private DefaultCategoryDataset                 localDiskCapacityDataset;
  private DefaultCategoryDataset                 remoteCapacityDataset;

  private ChartPanel                             localHeapCapacityPanel;
  private ChartPanel                             localOffHeapCapacityPanel;
  private ChartPanel                             localDiskCapacityPanel;
  private ChartPanel                             remoteCapacityPanel;

  private CategoryPlot                           localHeapCapacityPlot;
  private CategoryPlot                           localOffHeapCapacityPlot;
  private CategoryPlot                           localDiskCapacityPlot;
  private CategoryPlot                           remoteCapacityPlot;

  private CategoryItemRenderer                   localHeapCapacityPlotRenderer;
  private CategoryItemRenderer                   localOffHeapCapacityPlotRenderer;
  private CategoryItemRenderer                   localDiskCapacityPlotRenderer;
  private CategoryItemRenderer                   remoteCapacityPlotRenderer;

  private XLabel                                 localHeapCapacityLabel;
  private XLabel                                 localOffHeapCapacityLabel;
  private XLabel                                 localDiskCapacityLabel;
  private XLabel                                 remoteCapacityLabel;

  private UtilizationToolTipGenerator            localHeapCapacityTipGenerator;
  private UtilizationToolTipGenerator            localOffHeapCapacityTipGenerator;
  private UtilizationToolTipGenerator            localDiskCapacityTipGenerator;
  private RemoteUtilizationToolTipGenerator      remoteCapacityTipGenerator;

  private ChartPanel                             cacheLocalHeapCapacityPanel;
  private ChartPanel                             cacheLocalOffHeapCapacityPanel;
  private ChartPanel                             cacheLocalDiskCapacityPanel;

  private CategoryPlot                           cacheLocalHeapCapacityPlot;
  private DefaultCategoryDataset                 cacheLocalHeapCapacityDataset;
  private CategoryItemRenderer                   cacheLocalHeapCapacityPlotRenderer;

  private CategoryPlot                           cacheLocalOffHeapCapacityPlot;
  private DefaultCategoryDataset                 cacheLocalOffHeapCapacityDataset;
  private CategoryItemRenderer                   cacheLocalOffHeapCapacityPlotRenderer;

  private CategoryPlot                           cacheLocalDiskCapacityPlot;
  private DefaultCategoryDataset                 cacheLocalDiskCapacityDataset;
  private CategoryItemRenderer                   cacheLocalDiskCapacityPlotRenderer;

  private StatusView                             cacheLocalDiskCapacityStatusView;

  private XLabel                                 cacheLocalHeapCapacityLabel;
  private XLabel                                 cacheLocalOffHeapCapacityLabel;
  private XLabel                                 cacheLocalDiskCapacityLabel;

  private UtilizationToolTipGenerator            cacheLocalHeapCapacityTipGenerator;
  private UtilizationToolTipGenerator            cacheLocalOffHeapCapacityTipGenerator;
  private UtilizationToolTipGenerator            cacheLocalDiskCapacityTipGenerator;
  private TierChartListener                      cacheLocalDiskCapacityChartListener;
  private RemoteUtilizationToolTipGenerator      cacheRemoteCapacityTipGenerator;

  private DefaultCategoryDataset                 localHeapMissesDataset;
  private DefaultCategoryDataset                 localOffHeapMissesDataset;
  private DefaultCategoryDataset                 localDiskMissesDataset;

  private CategoryPlot                           localHeapMissesPlot;
  private CategoryPlot                           localOffHeapMissesPlot;
  private CategoryPlot                           localDiskMissesPlot;

  private StatusView                             localDiskMissesStatusView;

  private XLabel                                 localHeapMissesLabel;
  private XLabel                                 localOffHeapMissesLabel;
  private XLabel                                 localDiskMissesLabel;

  private RefreshAction                          refreshAction;

  private static final String                    SIZES_PAGED_VIEW                   = "SizesView";
  private static final String                    COUNTS_PAGED_VIEW                  = "CountsView";

  private static final ThinDecimalFormat         THIN_DECIMAL_FORMAT                = ThinDecimalFormat.INSTANCE;
  private static final ThinMemoryFormat          THIN_MEMORY_FORMAT                 = ThinMemoryFormat.INSTANCE;

  private static final String                    USED                               = "Used";
  private static final String                    MISS_RATE                          = "Miss Rate";

  private static final String                    LOCAL_HEAP                         = "Local Heap";
  private static final String                    LOCAL_OFFHEAP                      = "Local OffHeap";
  private static final String                    LOCAL_DISK                         = "Local Disk";
  private static final String                    REMOTE                             = "Remote";
  private static final String                    AVAILABLE                          = "Available";

  private static final Dimension                 CHART_MIN_SIZE                     = new Dimension(200, 40);
  private static final Font                      LABEL_FONT                         = new Font("Dialog", Font.BOLD, 12);
  private static final Font                      CHART_LABEL_FONT                   = new Font("Dialog", Font.PLAIN, 10);
  private static final Color                     CHART_LABEL_FG                     = Color.gray;

  private static String[]                        L1_TIERS                           = { LOCAL_HEAP, LOCAL_OFFHEAP,
      LOCAL_DISK, REMOTE                                                           };
  private static final double                    PIE_SLICE_EXPLODE_PERCENT          = 0.10d;

  private static final TickUnitSource            MEMORY_TICK_UNITS                  = createMemoryIntegerTickUnits();

  protected static final String                  SIZE_IN_BYTES                      = "Size in Bytes";
  protected static final String                  SIZE_IN_BYTES_REMOTE               = SIZE_IN_BYTES + " (est.)";

  private static final String[]                  CACHE_SIZES_TABLE_COLUMNS          = { "Cache", SIZE_IN_BYTES,
      "% of Used", "Entries", "Mean Entry Size"                                    };

  private final MemorySizeTableRenderer          MEMORY_SIZE_TABLE_RENDERER         = new MemorySizeTableRenderer();
  private final PercentMemoryUsedRenderer        PERCENT_MEMORY_USED_TABLE_RENDERER = new PercentMemoryUsedRenderer();
  private final EntryCountTableRenderer          ENTRY_COUNT_TABLE_RENDERER         = new EntryCountTableRenderer();

  private static final Color                     MAX_COLOR                          = Color.red;
  private static final Color                     RESERVED_COLOR                     = new Color(255, 201, 14);

  private static final Color[]                   SCHEME_1                           = { new Color(234, 211, 153),
      new Color(153, 217, 234), new Color(234, 171, 153)                           };

  private static final Color[]                   SCHEME_2                           = { new Color(228, 214, 196),
      new Color(154, 177, 208), new Color(145, 189, 112)                           };

  private static final Color[]                   SCHEME_3                           = { new Color(196, 210, 228),
      new Color(208, 185, 154), new Color(224, 225, 193)                           };

  private static final Color[][]                 ALL_SCHEMES                        = { SCHEME_1, SCHEME_2, SCHEME_3 };

  private static final String                    LOCAL_DISK_WITH_CLUSTERED_TIP      = "Local Disk not available with Terracotta-clustered caches";
  private static final String                    REMOTE_WITH_NON_CLUSTERED_TIP      = "Remote tier not available for non-Terracotta clustered caches";

  private static final Color[]                   CURRENT_SCHEME                     = ALL_SCHEMES[1];

  private static final Color                     LOCAL_HEAP_COLOR                   = CURRENT_SCHEME[0];
  private static final Color                     LOCAL_OFFHEAP_COLOR                = CURRENT_SCHEME[1];
  private static final Color                     LOCAL_DISK_COLOR                   = CURRENT_SCHEME[2];
  private static final Color                     REMOTE_COLOR                       = new Color(234, 171, 153);

  private static final double                    MAXIMUM_BAR_WIDTH                  = 0.25d;
  private static final float                     FOREGROUND_ALPHA                   = 1.0f;

  private static final Color                     SELECTED_TIER_PLOT_BG              = Color.white;
  private static final Color                     UNSELECTED_TIER_PLOT_BG            = null;

  private static final float                     SELECTED_TIER_PLOT_ALPHA           = 1.0f;
  private static final float                     UNSELECTED_TIER_PLOT_ALPHA         = 0.5f;

  private static final int                       DEFAULT_REFRESH_TIMER_SECONDS      = 30;
  private static final int                       REFRESH_TIMER_SECONDS              = Integer
                                                                                        .getInteger("ClientCacheManagerSizingPanel.refreshTimerSeconds",
                                                                                                    DEFAULT_REFRESH_TIMER_SECONDS);

  private final Timer                            refreshTimer                       = createRefreshTimer();

  public ClientCacheManagerSizingPanel(ApplicationContext appContext, CacheManagerModel cacheManagerModel,
                                       CacheManagerInstance cacheManagerInstance) {
    super(appContext, cacheManagerModel.getClusterModel());
    this.cacheManagerInstance = cacheManagerInstance;
    this.addHierarchyListener(this);
  }

  @Override
  protected void init() {
    if (cacheManagerInstance != null) {
      cacheManagerInstance.addCacheManagerInstanceListener(this);
    }
  }

  public CacheManagerInstance getCacheManagerInstance() {
    return cacheManagerInstance;
  }

  private Timer createRefreshTimer() {
    Timer result = new Timer(REFRESH_TIMER_SECONDS * 1000, new RefreshTimerAction());
    result.setRepeats(false);
    return result;
  }

  private class RefreshTimerAction implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      refresh();
    }
  }

  public void hierarchyChanged(final HierarchyEvent e) {
    if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
      if (isShowing()) {
        refresh();
      } else {
        refreshTimer.stop();
      }
    }
  }

  private class GetSizesWorker extends BasicWorker<Map<CacheModelInstance, CacheTierSize>> {
    private GetSizesWorker() {
      super(new Callable<Map<CacheModelInstance, CacheTierSize>>() {
        public Map<CacheModelInstance, CacheTierSize> call() throws Exception {
          return cacheManagerInstance.getSizes();
        }
      });
      refreshAction.setEnabled(false);
    }

    @Override
    protected void finished() {
      Exception e = getException();
      if (e != null) {
        Throwable rootCause = ExceptionHelper.getRootCause(e);
        if (!(rootCause instanceof IOException)) {
          appContext.log(e);
        }
      } else {
        update(getResult());
      }
      refreshAction.setEnabled(true);
    }
  }

  void refresh() {
    if (cacheManagerInstance != null) {
      if (cacheManagerInstance.hasSizeBasedPooling() || cacheManagerInstance.hasSizeBasedCache()) {
        pagedView.setPage(SIZES_PAGED_VIEW);
        refreshTimer.stop();
        appContext.submit(new GetSizesWorker());
      } else {
        pagedView.setPage(COUNTS_PAGED_VIEW);
      }
    }
  }

  private void update(Map<CacheModelInstance, CacheTierSize> theCacheSizes) {
    this.cacheSizes = theCacheSizes;

    if (theCacheSizes != null && !theCacheSizes.isEmpty()) {
      long reservedLocalHeapBytes = 0;
      long usedLocalHeapBytes = 0;
      long usedLocalHeapEntries = 0;

      long reservedLocalOffHeapBytes = 0;
      long usedLocalOffHeapBytes = 0;
      long usedLocalOffHeapEntries = 0;

      long reservedLocalDiskBytes = 0;
      long usedLocalDiskBytes = 0;
      long usedLocalDiskEntries = 0;

      long reservedRemoteBytes = 0;
      long usedRemoteBytes = 0;
      long usedRemoteEntries = 0;
      long maxRemoteEntries = 0;

      String selectedCacheName = (String) selectedCacheChooser.getSelectedItem();
      selectedCacheChooser.removeActionListener(selectedCacheListener);
      selectedCacheChooser.removeAllItems();

      Set<Comparable> keys = new TreeSet<Comparable>();
      String selectedTier = selectedTier();
      int tcClusteredCount = cacheManagerInstance.getTerracottaClusteredInstanceCount();
      boolean isPooledCacheManager = cacheManagerInstance.hasSizeBasedPooling();

      tierUsageByCacheDataset.clear();

      for (Entry<CacheModelInstance, CacheTierSize> entry : theCacheSizes.entrySet()) {
        CacheModelInstance cmi = entry.getKey();
        SettingsCacheModel scm = cacheManagerInstance.getSettingsCacheModel(cmi);
        if (!isPooledCacheManager && !scm.hasSizeBasedLimits()) {
          continue;
        }

        boolean tcClustered = cmi.isTerracottaClustered();
        CacheTierSize tierSize = entry.getValue();
        String cacheName = cmi.getCacheName();
        Number cacheTierSize = Long.valueOf(0);

        if (selectedTier.equals(LOCAL_HEAP)) {
          cacheTierSize = tierSize.getLocalHeapSizeInBytes();
        } else if (selectedTier.equals(LOCAL_OFFHEAP)) {
          cacheTierSize = tierSize.getLocalOffHeapSizeInBytes();
        } else if (selectedTier.equals(LOCAL_DISK)) {
          if (!tcClustered) {
            cacheTierSize = tierSize.getLocalDiskSizeInBytes();
          }
        } else {
          if (tcClustered) {
            cacheTierSize = tierSize.sizeInBytesForTier(REMOTE);
          }
        }
        tierUsageByCacheDataset.setValue(cacheName, cacheTierSize);

        reservedLocalHeapBytes += tierSize.getMaxBytesLocalHeap();
        usedLocalHeapBytes += tierSize.getLocalHeapSizeInBytes();
        usedLocalHeapEntries += tierSize.getLocalHeapSize();

        reservedLocalOffHeapBytes += tierSize.getMaxBytesLocalOffHeap();
        usedLocalOffHeapBytes += tierSize.getLocalOffHeapSizeInBytes();
        usedLocalOffHeapEntries += tierSize.getLocalOffHeapSize();

        if (tcClustered) {
          reservedRemoteBytes += tierSize.maxBytesForTier(REMOTE);
          usedRemoteBytes += tierSize.sizeInBytesForTier(REMOTE);
          usedRemoteEntries += tierSize.getLocalDiskSize();
          maxRemoteEntries += tierSize.maxEntriesForTier(REMOTE);
        } else {
          reservedLocalDiskBytes += tierSize.getMaxBytesLocalDisk();
          usedLocalDiskBytes += tierSize.getLocalDiskSizeInBytes();
          usedLocalDiskEntries += tierSize.getLocalDiskSize();
        }

        selectedCacheChooser.addItem(cacheName);

        keys.add(cacheName);
      }

      renderPieChartDetails(keys, tierUsageByCachePanel);

      long maxLocalHeapBytes = cacheManagerInstance.getMaxBytesLocalHeap();
      long maxLocalOffHeapBytes = cacheManagerInstance.getMaxBytesLocalOffHeap();
      long maxLocalDiskBytes = cacheManagerInstance.getMaxBytesLocalDisk();
      long maxRemoteBytes = reservedRemoteBytes;
      long max, used;

      reservedRemoteBytes = 0;

      Color tierColor;
      if (selectedTier.equals(LOCAL_HEAP)) {
        max = maxLocalHeapBytes;
        used = usedLocalHeapBytes;
        updateUnusedLabel(maxLocalHeapBytes, reservedLocalHeapBytes, usedLocalHeapBytes, usedLocalHeapEntries);
        tierColor = LOCAL_HEAP_COLOR;
      } else if (selectedTier.equals(LOCAL_OFFHEAP)) {
        max = maxLocalOffHeapBytes;
        used = usedLocalOffHeapBytes;
        updateUnusedLabel(maxLocalOffHeapBytes, reservedLocalOffHeapBytes, usedLocalOffHeapBytes,
                          usedLocalOffHeapEntries);
        tierColor = LOCAL_OFFHEAP_COLOR;
      } else if (selectedTier.equals(LOCAL_DISK)) {
        max = maxLocalDiskBytes;
        used = usedLocalDiskBytes;
        updateUnusedLabel(maxLocalDiskBytes, reservedLocalDiskBytes, usedLocalDiskBytes, usedLocalDiskEntries);
        tierColor = LOCAL_DISK_COLOR;
      } else {
        max = maxRemoteBytes;
        used = usedRemoteBytes;
        updateUnusedLabel(maxRemoteBytes, reservedRemoteBytes, usedRemoteBytes, usedRemoteEntries);
        tierColor = REMOTE_COLOR;
      }
      if (max > 0 && (max - used) > 0) {
        tierUsageByCacheDataset.setValue(AVAILABLE, max - used);
      }
      tierUsageByCacheStatusView.setText(selectedTier);
      tierUsageByCacheStatusView.setIndicator(tierColor);

      cacheManagerCapacityPanel.removeAll();
      cacheManagerCapacityPanel.setLayout(new GridLayout(0, 1));

      localHeapCapacityDataset.clear();
      localHeapCapacityPlot.clearRangeMarkers();
      if (reservedLocalHeapBytes > 0) {
        addReservedRangeValueMarker(localHeapCapacityPlot, reservedLocalHeapBytes);
      }
      localHeapCapacityDataset.setValue(usedLocalHeapBytes, USED, LOCAL_HEAP);
      if (maxLocalHeapBytes > 0) {
        addMaximumRangeValueMarker(localHeapCapacityPlot, maxLocalHeapBytes);
      }
      localHeapCapacityLabel.setText(handleReserved(reservedLocalHeapBytes, THIN_MEMORY_FORMAT) + " Used: "
                                     + THIN_MEMORY_FORMAT.format(usedLocalHeapBytes)
                                     + handleAvailable(maxLocalHeapBytes, usedLocalHeapBytes, THIN_MEMORY_FORMAT));
      cacheManagerCapacityPanel.add(localHeapCapacityPanel);
      updatePlotRangeAxis(localHeapCapacityPlot);
      updateTierPlotSelectionGraphics(localHeapCapacityPlot, selectedTier, LOCAL_HEAP);
      localHeapCapacityTipGenerator.setup(LOCAL_HEAP, usedLocalHeapBytes, reservedLocalHeapBytes, maxLocalHeapBytes,
                                          usedLocalHeapEntries);

      localOffHeapCapacityDataset.clear();
      localOffHeapCapacityPlot.clearRangeMarkers();
      if (reservedLocalOffHeapBytes > 0) {
        addReservedRangeValueMarker(localOffHeapCapacityPlot, reservedLocalOffHeapBytes);
      }
      localOffHeapCapacityDataset.setValue(usedLocalOffHeapBytes, USED, LOCAL_OFFHEAP);
      if (maxLocalOffHeapBytes > 0) {
        addMaximumRangeValueMarker(localOffHeapCapacityPlot, maxLocalOffHeapBytes);
      }
      localOffHeapCapacityLabel.setText(handleReserved(reservedLocalOffHeapBytes, THIN_MEMORY_FORMAT)
                                        + " Used: "
                                        + THIN_MEMORY_FORMAT.format(usedLocalOffHeapBytes)
                                        + handleAvailable(maxLocalOffHeapBytes, usedLocalOffHeapBytes,
                                                          THIN_MEMORY_FORMAT));
      cacheManagerCapacityPanel.add(localOffHeapCapacityPanel);
      updatePlotRangeAxis(localOffHeapCapacityPlot);
      updateTierPlotSelectionGraphics(localOffHeapCapacityPlot, selectedTier, LOCAL_OFFHEAP);
      localOffHeapCapacityTipGenerator.setup(LOCAL_OFFHEAP, usedLocalOffHeapBytes, reservedLocalOffHeapBytes,
                                             maxLocalOffHeapBytes, usedLocalOffHeapEntries);

      localDiskCapacityDataset.clear();
      localDiskCapacityPlot.clearRangeMarkers();
      if (tcClusteredCount != cacheManagerInstance.getInstanceCount()) {
        if (reservedLocalDiskBytes > 0) {
          addReservedRangeValueMarker(localDiskCapacityPlot, reservedLocalDiskBytes);
        }
        localDiskCapacityDataset.setValue(usedLocalDiskBytes, USED, LOCAL_DISK);
        if (maxLocalDiskBytes > 0) {
          addMaximumRangeValueMarker(localDiskCapacityPlot, maxLocalDiskBytes);
        }
        localDiskCapacityLabel.setText(handleReserved(reservedLocalDiskBytes, THIN_MEMORY_FORMAT) + " Used: "
                                       + THIN_MEMORY_FORMAT.format(usedLocalDiskBytes)
                                       + handleAvailable(maxLocalDiskBytes, usedLocalDiskBytes, THIN_MEMORY_FORMAT));
        cacheManagerCapacityPanel.add(localDiskCapacityPanel);
        updatePlotRangeAxis(localDiskCapacityPlot);
        updateTierPlotSelectionGraphics(localDiskCapacityPlot, selectedTier, LOCAL_DISK);
        localDiskCapacityTipGenerator.setup(LOCAL_DISK, usedLocalDiskBytes, reservedLocalDiskBytes, maxLocalDiskBytes,
                                            usedLocalDiskEntries);
      }

      remoteCapacityDataset.clear();
      remoteCapacityPlot.clearRangeMarkers();
      if (tcClusteredCount > 0) {
        if (reservedRemoteBytes > 0) {
          addReservedRangeValueMarker(remoteCapacityPlot, reservedRemoteBytes);
        }
        remoteCapacityDataset.setValue(usedRemoteBytes, USED, REMOTE);
        if (maxRemoteBytes > 0) {
          addMaximumRangeValueMarker(remoteCapacityPlot, maxRemoteBytes);
        }
        remoteCapacityLabel.setText("Used: " + THIN_MEMORY_FORMAT.format(usedRemoteBytes)
                                    + handleAvailable(maxRemoteBytes, usedRemoteBytes, THIN_MEMORY_FORMAT));
        cacheManagerCapacityPanel.add(remoteCapacityPanel);
        updatePlotRangeAxis(remoteCapacityPlot);
        updateTierPlotSelectionGraphics(remoteCapacityPlot, selectedTier, REMOTE);
        remoteCapacityTipGenerator.setup(REMOTE, maxRemoteEntries, maxRemoteBytes, usedRemoteEntries, usedRemoteBytes);
      }

      long upperBound = determineMax(maxLocalHeapBytes, reservedLocalHeapBytes, usedLocalHeapBytes,
                                     maxLocalOffHeapBytes, reservedLocalOffHeapBytes, usedLocalOffHeapBytes,
                                     maxLocalDiskBytes, reservedLocalDiskBytes, usedLocalDiskBytes, maxRemoteBytes,
                                     reservedRemoteBytes, usedRemoteBytes);
      setRangeAxisPlusExtra(upperBound, localHeapCapacityPlot, localOffHeapCapacityPlot, localDiskCapacityPlot,
                            remoteCapacityPlot);

      if (selectedCacheName == null) {
        selectedCacheName = (String) tierUsageByCachePlot.getDataset().getKeys().get(0);
      }
      selectedCacheChooser.setSelectedItem(selectedCacheName);
      selectCacheSlice(selectedCacheName);

      updateTierUsageByCacheTable(selectedCacheName);

      selectedCacheChooser.addActionListener(selectedCacheListener);

      tierChooser.setModel(new DefaultComboBoxModel(L1_TIERS));
      if (tcClusteredCount == 0) {
        tierChooser.removeItem(REMOTE);
      }
      if (tcClusteredCount == cacheManagerInstance.getInstanceCount()) {
        tierChooser.removeItem(LOCAL_DISK);
      }
      setSelectedTier(selectedTier);

      refreshTimer.start();
    }
  }

  private void updateTierPlotSelectionGraphics(Plot plot, String selectedTier, String tierName) {
    plot.setBackgroundAlpha(selectedTier.equals(tierName) ? SELECTED_TIER_PLOT_ALPHA : UNSELECTED_TIER_PLOT_ALPHA);
    plot.setBackgroundPaint(selectedTier.equals(tierName) ? SELECTED_TIER_PLOT_BG : UNSELECTED_TIER_PLOT_BG);
  }

  private void setSelectedTier(String tierName) {
    tierChooser.removeActionListener(tierChooserListener);
    tierChooser.setSelectedItem(tierName);
    tierChooser.addActionListener(tierChooserListener);
  }

  private long determineMax(long... values) {
    long result = 0;
    for (long value : values) {
      result = Math.max(result, value);
    }
    return result;
  }

  private void setRangeAxisPlusExtra(long value, CategoryPlot... plots) {
    if (value > 10) {
      value += (value / 10);
    } else {
      value++;
    }
    for (CategoryPlot plot : plots) {
      plot.getRangeAxis().setRange(0, value);
    }
  }

  private static final int MAX_LABEL_OFFSET      = 1;
  private static final int RESERVED_LABEL_OFFSET = CHART_LABEL_FONT.getSize() + 2;

  private void addMaximumRangeValueMarker(CategoryPlot plot, long value) {
    addRangeValueMarker(plot, value, "Max", MAX_COLOR, MAX_LABEL_OFFSET);
  }

  private void addReservedRangeValueMarker(CategoryPlot plot, long value) {
    addRangeValueMarker(plot, value, "Reserved", RESERVED_COLOR, RESERVED_LABEL_OFFSET);
  }

  private void addRangeValueMarker(CategoryPlot plot, long value, String label, Color color, int labelOffset) {
    float[] dash = { 2.0f };
    BasicStroke stroke = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f);
    ValueMarker marker = new ValueMarker(value, color, new BasicStroke(1.0f), color, stroke, 1.0f);
    plot.addRangeMarker(marker, Layer.BACKGROUND);
    marker.setLabel(label);
    marker.setLabelOffset(new RectangleInsets(labelOffset, 5, 0, 0));
    marker.setLabelFont(CHART_LABEL_FONT);
    marker.setLabelPaint(CHART_LABEL_FG);
    marker.setLabelAnchor(RectangleAnchor.TOP_LEFT);
    marker.setLabelTextAnchor(TextAnchor.TOP_RIGHT);

    marker = new ValueMarker(value, color, new BasicStroke(1.0f), color, stroke, 1.0f);
    plot.addRangeMarker(marker, Layer.BACKGROUND);
    marker.setLabel(THIN_MEMORY_FORMAT.format(value));
    marker.setLabelOffset(new RectangleInsets(labelOffset, 0, 0, 5));
    marker.setLabelFont(CHART_LABEL_FONT);
    marker.setLabelPaint(CHART_LABEL_FG);
    marker.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
    marker.setLabelTextAnchor(TextAnchor.TOP_LEFT);
  }

  private void updateUnusedLabel(long maxBytes, long reservedBytes, long usedBytes, long usedEntries) {
    StringBuilder sb = new StringBuilder();
    if ((maxBytes - usedBytes) > 0) {
      sb.append("Available Space: ");
      long availableBytes = maxBytes - usedBytes;
      sb.append(THIN_MEMORY_FORMAT.format(availableBytes));
      sb.append(" (");
      sb.append(NumberFormat.getPercentInstance().format(availableBytes / (double) maxBytes));
      sb.append(")");
      sb.append(", Entries: ");
      sb.append(THIN_DECIMAL_FORMAT.format(usedEntries));
    } else if ((reservedBytes - usedBytes) > 0) {
      long availableBytes = reservedBytes - usedBytes;
      sb.append("Available Space: ");
      sb.append(THIN_MEMORY_FORMAT.format(availableBytes));
      sb.append(" (");
      sb.append(NumberFormat.getPercentInstance().format(availableBytes / (double) reservedBytes));
      sb.append(")");
      sb.append(", Entries: ");
      sb.append(THIN_DECIMAL_FORMAT.format(usedEntries));
    } else {
      sb.append("Entries: ");
      sb.append(THIN_DECIMAL_FORMAT.format(usedEntries));
    }
    tierUsageByCacheLabel.setText(sb.toString());
  }

  private static void updatePlotRangeAxis(CategoryPlot categoryPlot) {
    NumberAxis rangeAxis = (NumberAxis) categoryPlot.getRangeAxis();
    rangeAxis.setNumberFormatOverride(THIN_MEMORY_FORMAT);
    rangeAxis.setStandardTickUnits(MEMORY_TICK_UNITS);
  }

  private void renderPieChartDetails(Set<Comparable> keys, ChartPanel chartPanel) {
    PieDataset pieDataset = ((PiePlot) chartPanel.getChart().getPlot()).getDataset();
    tierUsageByCacheTableModel.setRowCount(0);
    for (Comparable key : keys) {
      tierUsageByCacheTableModel.addRow(CACHE_PIE_VALUES_GENERATOR.getTableItemArray(pieDataset, key));
    }
  }

  private XContainer createSizesView() {
    XContainer result = new XContainer(new BorderLayout());
    XSplitPane splitter = new XSplitPane(JSplitPane.VERTICAL_SPLIT, createCacheManagerPanel(), createCachePanel());
    splitter.setDefaultDividerLocation(0.5d);
    splitter.setBorder(null);
    result.add(splitter, BorderLayout.CENTER);
    result.add(createBottomPanel(), BorderLayout.SOUTH);
    result.setName(SIZES_PAGED_VIEW);
    return result;
  }

  private XContainer createCountsView() {
    XContainer result = new XContainer(new BorderLayout());
    XLabel label = new XLabel(
                              "Not available unless the CacheManager is using size-based pooling or contains some size-based caches.");
    label.setHorizontalAlignment(SwingConstants.CENTER);
    result.add(label);
    result.setName(COUNTS_PAGED_VIEW);
    return result;
  }

  @Override
  protected XContainer createMainPanel() {
    pagedView = new PagedView();
    pagedView.add(createSizesView());
    pagedView.add(createCountsView());
    return pagedView;
  }

  protected JComponent createBottomPanel() {
    EhcacheToolBar bottomPanel = new EhcacheToolBar();
    bottomPanel.add(refreshAction = new RefreshAction());
    return bottomPanel;
  }

  private class RefreshAction extends XAbstractAction {
    private RefreshAction() {
      super(bundle.getString("refresh"), new ImageIcon(
                                                       RefreshAction.class
                                                           .getResource("/com/tc/admin/icons/refresh.gif")));
    }

    public void actionPerformed(final ActionEvent ae) {
      refresh();
    }
  }

  private XContainer createCacheManagerPanel() {
    XContainer result = new XContainer(new BorderLayout());
    XSplitPane splitter;
    result.add(splitter = new XSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                         cacheManagerCapacityPanel = createCacheManagerCapacityPanel(),
                                         createCacheManagerUsageByCachePanel()));
    splitter.setDefaultDividerLocation(0.5d);
    splitter.setBorder(null);
    return result;
  }

  private XContainer createCacheManagerUsageByCachePanel() {
    XContainer topPanel = new XContainer(new BorderLayout());
    XContainer tierChooserPanel = new XContainer(new FlowLayout());
    XLabel label;
    tierChooserPanel.add(label = new XLabel("Tier:"));
    label.setFont(LABEL_FONT);
    tierChooser = new XComboBox(new DefaultComboBoxModel(L1_TIERS));
    Map<String, Color> indicatorMap = new HashMap<String, Color>();
    indicatorMap.put(LOCAL_HEAP, LOCAL_HEAP_COLOR);
    indicatorMap.put(LOCAL_OFFHEAP, LOCAL_OFFHEAP_COLOR);
    indicatorMap.put(LOCAL_DISK, LOCAL_DISK_COLOR);
    indicatorMap.put(REMOTE, REMOTE_COLOR);
    tierChooser.setRenderer(new TierChooserRenderer(indicatorMap));
    tierChooser.addActionListener(tierChooserListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        tierUsageByCacheTable.getColumnModel().getColumn(1)
            .setHeaderValue(selectedTier().equals(REMOTE) ? SIZE_IN_BYTES_REMOTE : SIZE_IN_BYTES);
        refresh();
      }
    });
    tierChooserPanel.add(tierChooser);
    topPanel.add(tierChooserPanel, BorderLayout.WEST);
    topPanel.add(new XLabel("Terracotta-clustered", CLUSTERED_ICON), BorderLayout.EAST);

    XScrollPane scrollPane = new XScrollPane(createTierUsageByCacheTable());
    scrollPane.setPreferredSize(new Dimension(280, 200));
    XSplitPane splitter = new XSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, createTierUsageByCacheChartPanel());
    splitter.setDefaultDividerLocation(0.4d);
    splitter.setBorder(null);

    XContainer result = new XContainer(new BorderLayout());
    result.add(topPanel, BorderLayout.NORTH);
    result.add(splitter, BorderLayout.CENTER);
    result.setBorder(BorderFactory.createTitledBorder("CacheManager Relative Cache Sizes"));
    return result;
  }

  private ChartPanel createTierUsageByCacheChartPanel() {
    tierUsageByCachePanel = createPieChart(tierUsageByCacheDataset = new DefaultPieDataset(), false);
    tierUsageByCachePanel.addChartMouseListener(new TierUsageByCacheChartListener());
    tierUsageByCachePlot = (PiePlot) tierUsageByCachePanel.getChart().getPlot();
    tierUsageByCachePlot.setOutlineVisible(false);
    tierUsageByCachePlotInsets = new RectangleInsets(0, 0, CHART_LABEL_FONT.getSize2D(), 0);
    tierUsageByCachePlot.setInsets(tierUsageByCachePlotInsets);
    tierUsageByCachePlot.setForegroundAlpha(0.6f);
    tierUsageByCachePlot.setSectionPaint(AVAILABLE, Color.white);
    tierUsageByCacheLabel = addOverlayLabel(tierUsageByCachePanel,
                                            tierUsageByCacheStatusView = createOverlayLabel(LOCAL_HEAP,
                                                                                            LOCAL_HEAP_COLOR));
    tierUsageByCachePanel.setBackground((Color) tierUsageByCachePlot.getBackgroundPaint());
    return tierUsageByCachePanel;
  }

  private XTable createTierUsageByCacheTable() {
    tierUsageByCacheTableModel = new DefaultTableModel(CACHE_SIZES_TABLE_COLUMNS, 0) {
      @Override
      public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
          case 0:
            return String.class;
          case 1:
            return Long.class;
          case 2:
            return Double.class;
          case 3:
            return Double.class;
          case 4:
            return Long.class;
          case 5:
            return Double.class;
        }
        return Object.class;
      }

      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };
    tierUsageByCacheTable = new XTable(tierUsageByCacheTableModel) {
      @Override
      public String getToolTipText(MouseEvent me) {
        int hitRowIndex = rowAtPoint(me.getPoint());
        if (hitRowIndex != -1) {
          int hitColIndex = columnAtPoint(me.getPoint());
          String cacheName = (String) tierUsageByCacheTable.getValueAt(hitRowIndex, 0);
          CacheModelInstance cmi = cacheManagerInstance.getCacheModelInstance(cacheName);
          boolean tcClustered = cmi.isTerracottaClustered();
          String selectedTier = selectedTier();

          switch (hitColIndex) {
            case 0: {
              return (String) tierUsageByCacheTable.getValueAt(hitRowIndex, 0);
            }
            case 1: {
              if (tcClustered && selectedTier.equals(LOCAL_DISK)) {
                return LOCAL_DISK_WITH_CLUSTERED_TIP;
              } else if (!tcClustered && selectedTier.equals(REMOTE)) {
                return REMOTE_WITH_NON_CLUSTERED_TIP;
              } else {
                Long value = (Long) tierUsageByCacheTable.getValueAt(hitRowIndex, 1);
                return NumberFormat.getNumberInstance().format(value) + " Bytes";
              }
            }
            case 2: {
              if (tcClustered && selectedTier.equals(LOCAL_DISK)) {
                return LOCAL_DISK_WITH_CLUSTERED_TIP;
              } else if (!tcClustered && selectedTier.equals(REMOTE)) {
                return REMOTE_WITH_NON_CLUSTERED_TIP;
              } else {
                return super.getToolTipText(me);
              }
            }
            case 3: {
              if (tcClustered && selectedTier.equals(LOCAL_DISK)) {
                return LOCAL_DISK_WITH_CLUSTERED_TIP;
              } else if (!tcClustered && selectedTier.equals(REMOTE)) {
                return REMOTE_WITH_NON_CLUSTERED_TIP;
              } else {
                Long value = (Long) tierUsageByCacheTable.getValueAt(hitRowIndex, 3);
                return NumberFormat.getNumberInstance().format(value) + " Entries";
              }
            }
            case 4: {
              if (tcClustered && selectedTier.equals(LOCAL_DISK)) {
                return LOCAL_DISK_WITH_CLUSTERED_TIP;
              } else if (!tcClustered && selectedTier.equals(REMOTE)) {
                return REMOTE_WITH_NON_CLUSTERED_TIP;
              } else {
                Double value = (Double) tierUsageByCacheTable.getValueAt(hitRowIndex, 4);
                return NumberFormat.getNumberInstance().format(value) + " Bytes";
              }
            }
          }
        }
        return super.getToolTipText(me);
      }

      @Override
      protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel) {
          @Override
          public String getToolTipText(MouseEvent me) {
            int column;
            if ((column = columnAtPoint(me.getPoint())) != -1) {
              String selectedTier = selectedTier().toLowerCase();
              switch (column) {
                case 0:
                  return "The cache name";
                case 1:
                  return "Size of the cache on " + selectedTier;
                case 2:
                  return "Portion of " + selectedTier + " usage taken by the cache";
                case 3:
                  return "Number of the cache entries on the " + selectedTier;
                case 4:
                  return "Mean size of an entry on " + selectedTier;
              }
            }
            return null;
          }
        };
      }
    };
    trySetAutoCreateRowSorter(tierUsageByCacheTable);
    tierUsageByCacheTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    tierUsageByCacheTable.getSelectionModel()
        .addListSelectionListener(tierUsageByCacheTableSelectionListener = new ListSelectionListener() {
          public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
              int row = tierUsageByCacheTable.getSelectedRow();
              if (row != -1) {
                String cacheName = (String) tierUsageByCacheTable.getValueAt(row, 0);
                selectedCacheChooser.setSelectedItem(cacheName);
              }
            }
          }
        });
    tierUsageByCacheTable.getColumnModel().getColumn(0).setCellRenderer(new CacheNameRenderer());
    tierUsageByCacheTable.getColumnModel().getColumn(1).setCellRenderer(MEMORY_SIZE_TABLE_RENDERER);
    tierUsageByCacheTable.getColumnModel().getColumn(2).setCellRenderer(PERCENT_MEMORY_USED_TABLE_RENDERER);
    tierUsageByCacheTable.getColumnModel().getColumn(3).setCellRenderer(ENTRY_COUNT_TABLE_RENDERER);
    tierUsageByCacheTable.getColumnModel().getColumn(4).setCellRenderer(MEMORY_SIZE_TABLE_RENDERER);
    return tierUsageByCacheTable;
  }

  protected static StatusView createOverlayLabel(String text, Color fg, Color indicator) {
    StatusView result = new StatusView();
    result.setText(text);
    result.setIndicator(indicator);
    result.setFont(CHART_LABEL_FONT);
    result.getLabel().setHorizontalAlignment(SwingConstants.CENTER);
    result.setForeground(fg);
    return result;
  }

  protected static StatusView createOverlayLabel(String text, Color indicator) {
    return createOverlayLabel(text, CHART_LABEL_FG, indicator);
  }

  private XLabel addOverlayLabel(ChartPanel chartPanel, JComponent label) {
    Plot plot = chartPanel.getChart().getPlot();
    int bottomInset = plot.getInsets().equals(tierUsageByCachePlotInsets) ? 1 : 4;
    chartPanel.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.weighty = gbc.weightx = 1.0;
    gbc.insets = new Insets(10, 10, bottomInset, 1);
    gbc.fill = GridBagConstraints.BOTH;
    gbc.anchor = GridBagConstraints.WEST;
    chartPanel.add(new XLabel(), gbc);
    gbc.gridy++;
    gbc.weighty = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    chartPanel.add(label, gbc);
    gbc.gridx++;
    gbc.insets = new Insets(0, 0, 0, 0);
    chartPanel.add(new XLabel(), gbc);
    gbc.gridx++;
    gbc.anchor = GridBagConstraints.EAST;
    gbc.insets = new Insets(10, 1, bottomInset, 10);
    XLabel result;
    chartPanel.add(result = new XLabel(), gbc);
    label.setOpaque(false);
    result.setForeground(label.getForeground());
    return result;
  }

  private XContainer createCacheManagerCapacityPanel() {
    XContainer result = new XContainer(new GridLayout(3, 1));
    localHeapCapacityPanel = createCapacityBarChart(localHeapCapacityDataset = new DefaultCategoryDataset(), false);
    localHeapCapacityLabel = addOverlayLabel(localHeapCapacityPanel, createOverlayLabel(LOCAL_HEAP, LOCAL_HEAP_COLOR));
    localHeapCapacityPanel.setBorder(null);
    localHeapCapacityPanel.addChartMouseListener(new TierChartListener(LOCAL_HEAP));
    localHeapCapacityPlot = (CategoryPlot) localHeapCapacityPanel.getChart().getPlot();
    localHeapCapacityPlot.getDomainAxis().setVisible(false);
    localHeapCapacityPlotRenderer = localHeapCapacityPlot.getRenderer();
    localHeapCapacityTipGenerator = (UtilizationToolTipGenerator) localHeapCapacityPlotRenderer
        .getBaseToolTipGenerator();
    localHeapCapacityPlotRenderer.setSeriesPaint(0, LOCAL_HEAP_COLOR);
    result.add(localHeapCapacityPanel);

    localOffHeapCapacityPanel = createCapacityBarChart(localOffHeapCapacityDataset = new DefaultCategoryDataset(),
                                                       false);
    localOffHeapCapacityLabel = addOverlayLabel(localOffHeapCapacityPanel,
                                                createOverlayLabel(LOCAL_OFFHEAP, LOCAL_OFFHEAP_COLOR));
    localOffHeapCapacityPanel.setBorder(null);
    localOffHeapCapacityPanel.addChartMouseListener(new TierChartListener(LOCAL_OFFHEAP));
    localOffHeapCapacityPlot = (CategoryPlot) localOffHeapCapacityPanel.getChart().getPlot();
    localOffHeapCapacityPlot.getDomainAxis().setVisible(false);
    localOffHeapCapacityPlotRenderer = localOffHeapCapacityPlot.getRenderer();
    localOffHeapCapacityTipGenerator = (UtilizationToolTipGenerator) localOffHeapCapacityPlotRenderer
        .getBaseToolTipGenerator();
    localOffHeapCapacityPlotRenderer.setSeriesPaint(0, LOCAL_OFFHEAP_COLOR);
    result.add(localOffHeapCapacityPanel);

    localDiskCapacityPanel = createCapacityBarChart(localDiskCapacityDataset = new DefaultCategoryDataset(), false);
    localDiskCapacityLabel = addOverlayLabel(localDiskCapacityPanel, createOverlayLabel(LOCAL_DISK, LOCAL_DISK_COLOR));
    localDiskCapacityPanel.setBorder(null);
    localDiskCapacityPanel.addChartMouseListener(new TierChartListener(LOCAL_DISK));
    localDiskCapacityPlot = (CategoryPlot) localDiskCapacityPanel.getChart().getPlot();
    localDiskCapacityPlot.getDomainAxis().setVisible(false);
    localDiskCapacityPlotRenderer = localDiskCapacityPlot.getRenderer();
    localDiskCapacityTipGenerator = (UtilizationToolTipGenerator) localDiskCapacityPlotRenderer
        .getBaseToolTipGenerator();
    localDiskCapacityPlotRenderer.setSeriesPaint(0, LOCAL_DISK_COLOR);
    result.add(localDiskCapacityPanel);

    remoteCapacityPanel = createCapacityBarChart(remoteCapacityDataset = new DefaultCategoryDataset(), false);
    remoteCapacityLabel = addOverlayLabel(remoteCapacityPanel, createOverlayLabel(REMOTE + " (estimate)", REMOTE_COLOR));
    remoteCapacityPanel.setBorder(null);
    remoteCapacityPanel.addChartMouseListener(new TierChartListener(REMOTE));
    remoteCapacityPlot = (CategoryPlot) remoteCapacityPanel.getChart().getPlot();
    remoteCapacityPlot.getDomainAxis().setVisible(false);
    remoteCapacityPlotRenderer = remoteCapacityPlot.getRenderer();
    remoteCapacityPlotRenderer
        .setBaseToolTipGenerator(remoteCapacityTipGenerator = new RemoteUtilizationToolTipGenerator());
    remoteCapacityPlotRenderer.setSeriesPaint(0, REMOTE_COLOR);
    result.add(remoteCapacityPanel);

    result.setBorder(BorderFactory.createTitledBorder("CacheManager Utilization by Tier"));
    return result;
  }

  /* private */static IntervalMarker createReservedMarker(long value) {
    Color c = new Color(255, 230, 138);
    return new IntervalMarker(0, value, c, new BasicStroke(0.5f), c, new BasicStroke(0.5f), 0.3f);
  }

  private class CacheNameRenderer extends BaseRenderer {
    @Override
    public void setValue(Object value) {
      super.setValue(value);

      String cacheName = value.toString();
      label.setIcon(cacheManagerInstance.isCacheTerracottaClustered(cacheName) ? CLUSTERED_ICON : NON_CLUSTERED_ICON);
    }
  }

  private static void trySetAutoCreateRowSorter(JTable table) {
    try {
      Method m = table.getClass().getMethod("setAutoCreateRowSorter", new Class[] { Boolean.TYPE });
      m.invoke(table, Boolean.TRUE);
    } catch (Exception e) {
      /**/
    }
  }

  private class MemorySizeTableRenderer extends BaseRenderer {
    public MemorySizeTableRenderer() {
      super(THIN_MEMORY_FORMAT);
      label.setHorizontalAlignment(SwingConstants.RIGHT);
    }

    @Override
    public void setValue(JTable table, int row, int col) {
      String cacheName = (String) table.getValueAt(row, 0);
      if (selectedTier().equals(LOCAL_DISK)) {
        CacheModelInstance cmi = cacheManagerInstance.getCacheModelInstance(cacheName);
        if (cmi != null && cmi.isTerracottaClustered()) {
          setText("na");
          return;
        }
      } else if (selectedTier().equals(REMOTE)) {
        CacheModelInstance cmi = cacheManagerInstance.getCacheModelInstance(cacheName);
        if (cmi != null && !cmi.isTerracottaClustered()) {
          setText("na");
          return;
        }
      }
      super.setValue(table, row, col);
    }
  }

  private class PercentMemoryUsedRenderer extends PercentRenderer {
    private PercentMemoryUsedRenderer() {
      super();
    }

    @Override
    public void setValue(JTable table, int row, int col) {
      String cacheName = (String) table.getValueAt(row, 0);
      String selectedTier = selectedTier();

      if (selectedTier.equals(LOCAL_DISK)) {
        CacheModelInstance cmi = cacheManagerInstance.getCacheModelInstance(cacheName);
        if (cmi != null && cmi.isTerracottaClustered()) {
          setText("na");
          return;
        }
      } else if (selectedTier.equals(REMOTE)) {
        CacheModelInstance cmi = cacheManagerInstance.getCacheModelInstance(cacheName);
        if (cmi != null && !cmi.isTerracottaClustered()) {
          setText("na");
          return;
        }
      }
      super.setValue(table, row, col);
    }
  }

  private class EntryCountTableRenderer extends BaseRenderer {
    public EntryCountTableRenderer() {
      super(THIN_DECIMAL_FORMAT);
      label.setHorizontalAlignment(SwingConstants.RIGHT);
    }

    @Override
    public void setValue(JTable table, int row, int col) {
      String cacheName = (String) table.getValueAt(row, 0);
      String selectedTier = selectedTier();

      if (selectedTier.equals(REMOTE)) {
        CacheModelInstance cmi = cacheManagerInstance.getCacheModelInstance(cacheName);
        if (cmi != null && !cmi.isTerracottaClustered()) {
          setText("na");
          return;
        }
      } else if (selectedTier.equals(LOCAL_DISK)) {
        CacheModelInstance cmi = cacheManagerInstance.getCacheModelInstance(cacheName);
        if (cmi != null && cmi.isTerracottaClustered()) {
          setText("na");
          return;
        }
      }
      super.setValue(table, row, col);
    }
  }

  private XContainer createCachePanel() {
    selectedCachePanel = new XContainer(new BorderLayout());
    XSplitPane splitter = new XSplitPane(JSplitPane.HORIZONTAL_SPLIT, createCacheCapacityPanel(),
                                         createMissesByTierChart());
    splitter.setDefaultDividerLocation(0.5d);
    selectedCachePanel.add(splitter, BorderLayout.CENTER);
    XContainer topPanel = new XContainer(new BorderLayout());
    XContainer topContent = new XContainer(new FlowLayout());
    XLabel label;
    topContent.add(label = new XLabel("Selected Cache:"));
    label.setFont(LABEL_FONT);
    topContent.add(selectedCacheChooser = new XComboBox(new DefaultComboBoxModel()));
    selectedCacheChooser.setRenderer(new CacheComboRenderer());
    selectedCacheListener = new SelectedCacheListener();
    topContent.add(cacheSummaryLabel = new XLabel());
    topPanel.add(topContent, BorderLayout.WEST);
    selectedCachePanel.add(topPanel, BorderLayout.NORTH);
    return selectedCachePanel;
  }

  private class CacheComboRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                  boolean cellHasFocus) {
      Component result = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      if (value != null) {
        setIcon(cacheManagerInstance.isCacheTerracottaClustered(value.toString()) ? CLUSTERED_ICON : NON_CLUSTERED_ICON);
      }
      return result;
    }
  }

  private XContainer createCacheCapacityPanel() {
    XContainer result = new XContainer(new GridLayout(3, 1));
    cacheLocalHeapCapacityPanel = createCapacityBarChart(cacheLocalHeapCapacityDataset = new DefaultCategoryDataset(),
                                                         false);
    cacheLocalHeapCapacityLabel = addOverlayLabel(cacheLocalHeapCapacityPanel,
                                                  createOverlayLabel(LOCAL_HEAP, LOCAL_HEAP_COLOR));
    cacheLocalHeapCapacityPanel.setBorder(null);
    cacheLocalHeapCapacityPanel.addChartMouseListener(new TierChartListener(LOCAL_HEAP));
    cacheLocalHeapCapacityPlot = (CategoryPlot) cacheLocalHeapCapacityPanel.getChart().getPlot();
    cacheLocalHeapCapacityPlot.getDomainAxis().setVisible(false);
    cacheLocalHeapCapacityPlotRenderer = cacheLocalHeapCapacityPlot.getRenderer();
    cacheLocalHeapCapacityTipGenerator = (UtilizationToolTipGenerator) cacheLocalHeapCapacityPlotRenderer
        .getBaseToolTipGenerator();
    cacheLocalHeapCapacityPlotRenderer.setSeriesPaint(0, LOCAL_HEAP_COLOR);
    result.add(cacheLocalHeapCapacityPanel);

    cacheLocalOffHeapCapacityPanel = createCapacityBarChart(cacheLocalOffHeapCapacityDataset = new DefaultCategoryDataset(),
                                                            false);
    cacheLocalOffHeapCapacityLabel = addOverlayLabel(cacheLocalOffHeapCapacityPanel,
                                                     createOverlayLabel(LOCAL_OFFHEAP, LOCAL_OFFHEAP_COLOR));
    cacheLocalOffHeapCapacityPanel.setBorder(null);
    cacheLocalOffHeapCapacityPanel.addChartMouseListener(new TierChartListener(LOCAL_OFFHEAP));
    cacheLocalOffHeapCapacityPlot = (CategoryPlot) cacheLocalOffHeapCapacityPanel.getChart().getPlot();
    cacheLocalOffHeapCapacityPlot.getDomainAxis().setVisible(false);
    cacheLocalOffHeapCapacityPlotRenderer = cacheLocalOffHeapCapacityPlot.getRenderer();
    cacheLocalOffHeapCapacityTipGenerator = (UtilizationToolTipGenerator) cacheLocalOffHeapCapacityPlotRenderer
        .getBaseToolTipGenerator();
    cacheLocalOffHeapCapacityPlotRenderer.setSeriesPaint(0, LOCAL_OFFHEAP_COLOR);
    result.add(cacheLocalOffHeapCapacityPanel);

    cacheLocalDiskCapacityPanel = createCapacityBarChart(cacheLocalDiskCapacityDataset = new DefaultCategoryDataset(),
                                                         false);
    cacheLocalDiskCapacityLabel = addOverlayLabel(cacheLocalDiskCapacityPanel,
                                                  cacheLocalDiskCapacityStatusView = createOverlayLabel(LOCAL_DISK,
                                                                                                        LOCAL_DISK_COLOR));
    cacheLocalDiskCapacityPanel.setBorder(null);
    cacheLocalDiskCapacityPanel
        .addChartMouseListener(cacheLocalDiskCapacityChartListener = new TierChartListener(LOCAL_DISK));
    cacheLocalDiskCapacityPlot = (CategoryPlot) cacheLocalDiskCapacityPanel.getChart().getPlot();
    cacheLocalDiskCapacityPlot.getDomainAxis().setVisible(false);
    cacheLocalDiskCapacityPlotRenderer = cacheLocalDiskCapacityPlot.getRenderer();
    cacheLocalDiskCapacityTipGenerator = (UtilizationToolTipGenerator) cacheLocalDiskCapacityPlotRenderer
        .getBaseToolTipGenerator();
    cacheRemoteCapacityTipGenerator = new RemoteUtilizationToolTipGenerator();
    cacheLocalDiskCapacityPlotRenderer.setSeriesPaint(0, LOCAL_DISK_COLOR);
    result.add(cacheLocalDiskCapacityPanel);

    result.setBorder(BorderFactory.createTitledBorder("Cache Utilization by Tier"));
    return result;
  }

  private class SelectedCacheListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      refreshTimer.stop();
      selectCacheSlice((String) selectedCacheChooser.getSelectedItem());
      refreshTimer.start();
    }
  }

  private ChartPanel createPieChart(PieDataset dataset, boolean showLegend) {
    return createPieChart(dataset, showLegend, null, 0d);
  }

  private ChartPanel createPieChart(PieDataset dataset, boolean showLegend, String explodeSlice, double explodePercent) {
    JFreeChart chart = ChartFactory.createPieChart("", dataset, showLegend, true, false);
    chart.setAntiAlias(true);
    chart.setBackgroundPaint(null);
    PiePlot plot = (PiePlot) chart.getPlot();
    plot.setToolTipGenerator(new CachePieToolTipGenerator());
    if (explodeSlice != null) {
      plot.setExplodePercent(explodeSlice, explodePercent);
    }
    plot.setIgnoreZeroValues(true);
    plot.setMaximumLabelWidth(0.2);
    plot.setSimpleLabels(false);
    plot.setSectionOutlinesVisible(true);
    plot.setNoDataMessage("No data available");
    final ChartPanel chartPanel = new ChartPanel(chart);
    chartPanel.setPopupMenu(null);
    chartPanel.setMouseZoomable(false);
    chartPanel.setMouseWheelEnabled(false);
    chartPanel.setPreferredSize(CHART_MIN_SIZE);
    return chartPanel;
  }

  private XContainer createMissesByTierChart() {
    XContainer result = new XContainer(new GridLayout(3, 1));

    ChartPanel missesPanel = createBarChart(localHeapMissesDataset = new DefaultCategoryDataset(), false);
    localHeapMissesLabel = addOverlayLabel(missesPanel, createOverlayLabel(LOCAL_HEAP, LOCAL_HEAP_COLOR));
    missesPanel.setBorder(null);
    missesPanel.addChartMouseListener(new TierChartListener(LOCAL_HEAP));
    localHeapMissesPlot = (CategoryPlot) missesPanel.getChart().getPlot();
    localHeapMissesPlot.getDomainAxis().setVisible(false);
    localHeapMissesPlot.getRenderer().setSeriesPaint(0, LOCAL_HEAP_COLOR);
    result.add(missesPanel);

    missesPanel = createBarChart(localOffHeapMissesDataset = new DefaultCategoryDataset(), false);
    localOffHeapMissesLabel = addOverlayLabel(missesPanel, createOverlayLabel(LOCAL_OFFHEAP, LOCAL_OFFHEAP_COLOR));
    missesPanel.setBorder(null);
    missesPanel.addChartMouseListener(new TierChartListener(LOCAL_OFFHEAP));
    localOffHeapMissesPlot = (CategoryPlot) missesPanel.getChart().getPlot();
    localOffHeapMissesPlot.getDomainAxis().setVisible(false);
    localOffHeapMissesPlot.getRenderer().setSeriesPaint(0, LOCAL_OFFHEAP_COLOR);
    result.add(missesPanel);

    missesPanel = createBarChart(localDiskMissesDataset = new DefaultCategoryDataset(), false);
    localDiskMissesLabel = addOverlayLabel(missesPanel,
                                           localDiskMissesStatusView = createOverlayLabel(LOCAL_DISK, LOCAL_DISK_COLOR));
    missesPanel.setBorder(null);
    missesPanel.addChartMouseListener(new TierChartListener(LOCAL_DISK));
    localDiskMissesPlot = (CategoryPlot) missesPanel.getChart().getPlot();
    localDiskMissesPlot.getDomainAxis().setVisible(false);
    localDiskMissesPlot.getRenderer().setSeriesPaint(0, LOCAL_DISK_COLOR);
    result.add(missesPanel);

    result.setBorder(BorderFactory.createTitledBorder("Cache Miss Rate by Tier"));

    return result;
  }

  private ChartPanel createBarChart(CategoryDataset dataset, boolean showLegend) {
    JFreeChart chart = ChartFactory.createBarChart("", "", "", dataset, PlotOrientation.HORIZONTAL, showLegend, true,
                                                   false);
    chart.setAntiAlias(true);
    chart.setBackgroundPaint(null);
    CategoryPlot plot = (CategoryPlot) chart.getPlot();
    plot.setForegroundAlpha(FOREGROUND_ALPHA);
    NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
    rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
    BarRenderer renderer = (BarRenderer) plot.getRenderer();
    renderer.setDrawBarOutline(true);
    renderer.setMaximumBarWidth(MAXIMUM_BAR_WIDTH);
    renderer.setShadowVisible(false);
    ChartPanel chartPanel = new ChartPanel(chart);
    chartPanel.setPopupMenu(null);
    chartPanel.setOpaque(false);
    chartPanel.setMouseZoomable(false);
    chartPanel.setMouseWheelEnabled(false);
    chartPanel.setPreferredSize(CHART_MIN_SIZE);
    return chartPanel;
  }

  private static class UtilizationToolTipGenerator extends StandardCategoryToolTipGenerator {
    private String tip;

    void setup(String tierName, long used, long reserved, long max, long entries) {
      StringBuilder sb = new StringBuilder();
      sb.append("<html><table cellspacing=0 cellpadding=0 border=0>");
      sb.append("<tr><td>");
      sb.append("<u>");
      sb.append(tierName);
      sb.append(":</u>");
      sb.append("</td></tr>");
      sb.append("<tr><td>");
      sb.append("used = ");
      sb.append(THIN_MEMORY_FORMAT.format(used));
      sb.append(" (");
      sb.append(THIN_DECIMAL_FORMAT.format(entries));
      sb.append(" entries)");
      sb.append("</td></tr>");
      if (reserved > 0) {
        sb.append("<tr><td>");
        sb.append("reserved = ");
        sb.append(THIN_MEMORY_FORMAT.format(reserved));
        sb.append("</td></tr>");
      }
      if (max > 0) {
        sb.append("<tr><td>");
        sb.append("max = ");
        sb.append(THIN_MEMORY_FORMAT.format(max));
        sb.append("</td></tr>");
      }
      sb.append("</table></html>");
      tip = sb.toString();
    }

    @Override
    public String generateToolTip(CategoryDataset dataset, int row, int column) {
      return tip != null ? tip : super.generateToolTip(dataset, row, column);
    }
  }

  private static class RemoteUtilizationToolTipGenerator extends StandardCategoryToolTipGenerator {
    private String tip;

    void setup(String tierName, long maxEntries, long estimatedMaxSize, long entries, long estimatedSize) {
      StringBuilder sb = new StringBuilder();
      sb.append("<html><table cellspacing=0 cellpadding=0 border=0>");
      sb.append("<tr><td>");
      sb.append("<u>");
      sb.append(tierName);
      sb.append(":</u>");
      sb.append("</td></tr>");
      sb.append("<tr><td>");
      sb.append("used = ");
      sb.append(THIN_DECIMAL_FORMAT.format(entries));
      sb.append(" entries (~ ");
      sb.append(THIN_MEMORY_FORMAT.format(estimatedSize));
      sb.append(")");
      sb.append("</td></tr>");
      if (maxEntries > 0) {
        sb.append("<tr><td>");
        sb.append("max = ");
        sb.append(THIN_DECIMAL_FORMAT.format(maxEntries));
        sb.append(" entries (~ ");
        sb.append(THIN_MEMORY_FORMAT.format(estimatedMaxSize));
        sb.append(")");
        sb.append("</td></tr>");
      }
      sb.append("</table></html>");
      tip = sb.toString();
    }

    @Override
    public String generateToolTip(CategoryDataset dataset, int row, int column) {
      return tip != null ? tip : super.generateToolTip(dataset, row, column);
    }
  }

  private static ChartPanel createCapacityBarChart(DefaultCategoryDataset dataset, boolean showLegend) {
    JFreeChart chart = ChartFactory.createBarChart("", "", "", dataset, PlotOrientation.HORIZONTAL, showLegend, true,
                                                   false);
    chart.setAntiAlias(true);
    chart.setBackgroundPaint(null);
    CategoryPlot categoryplot = (CategoryPlot) chart.getPlot();
    BarRenderer renderer = (BarRenderer) categoryplot.getRenderer();
    renderer.setBaseToolTipGenerator(new UtilizationToolTipGenerator());
    renderer.setDrawBarOutline(true);
    renderer.setMaximumBarWidth(MAXIMUM_BAR_WIDTH);
    renderer.setShadowVisible(false);
    categoryplot.setForegroundAlpha(FOREGROUND_ALPHA);
    ((NumberAxis) categoryplot.getRangeAxis()).setNumberFormatOverride(THIN_MEMORY_FORMAT);
    ((NumberAxis) categoryplot.getRangeAxis()).setStandardTickUnits(MEMORY_TICK_UNITS);
    ((NumberAxis) categoryplot.getRangeAxis()).setRangeType(RangeType.POSITIVE);
    ChartPanel chartPanel = new ChartPanel(chart);
    chartPanel.setPopupMenu(null);
    chartPanel.setOpaque(false);
    chartPanel.setMouseZoomable(false);
    chartPanel.setMouseWheelEnabled(false);
    chartPanel.setPreferredSize(CHART_MIN_SIZE);
    chartPanel.setBorder(BorderFactory.createTitledBorder("Utilization by Tier"));
    return chartPanel;
  }

  private static TickUnits createMemoryIntegerTickUnits() {
    TickUnits units = new TickUnits();
    NumberFormat numberFormat = THIN_MEMORY_FORMAT;
    units.add(new NumberTickUnit(1, numberFormat, 2));
    units.add(new NumberTickUnit(8, numberFormat, 2));
    units.add(new NumberTickUnit(16, numberFormat, 2));
    units.add(new NumberTickUnit(32, numberFormat, 2));
    units.add(new NumberTickUnit(64, numberFormat, 5));
    units.add(new NumberTickUnit(128, numberFormat, 2));
    units.add(new NumberTickUnit(256, numberFormat, 2));
    units.add(new NumberTickUnit(512, numberFormat, 5));

    long kbyte = ThinMemoryFormat.KBYTE;
    units.add(new NumberTickUnit(kbyte, numberFormat, 2));
    units.add(new NumberTickUnit(8 * kbyte, numberFormat, 2));
    units.add(new NumberTickUnit(16 * kbyte, numberFormat, 2));
    units.add(new NumberTickUnit(32 * kbyte, numberFormat, 2));
    units.add(new NumberTickUnit(64 * kbyte, numberFormat, 2));
    units.add(new NumberTickUnit(128 * kbyte, numberFormat, 5));
    units.add(new NumberTickUnit(256 * kbyte, numberFormat, 2));
    units.add(new NumberTickUnit(512 * kbyte, numberFormat, 2));

    long mbyte = ThinMemoryFormat.MBYTE;
    units.add(new NumberTickUnit(mbyte, numberFormat, 2));
    units.add(new NumberTickUnit(8 * mbyte, numberFormat, 2));
    units.add(new NumberTickUnit(16 * mbyte, numberFormat, 2));
    units.add(new NumberTickUnit(32 * mbyte, numberFormat, 2));
    units.add(new NumberTickUnit(64 * mbyte, numberFormat, 5));
    units.add(new NumberTickUnit(128 * mbyte, numberFormat, 2));
    units.add(new NumberTickUnit(256 * mbyte, numberFormat, 2));
    units.add(new NumberTickUnit(512 * mbyte, numberFormat, 5));

    long gbyte = ThinMemoryFormat.GBYTE;
    units.add(new NumberTickUnit(gbyte, numberFormat, 2));
    units.add(new NumberTickUnit(8 * gbyte, numberFormat, 2));
    units.add(new NumberTickUnit(16 * gbyte, numberFormat, 2));
    units.add(new NumberTickUnit(32 * gbyte, numberFormat, 2));
    units.add(new NumberTickUnit(64 * gbyte, numberFormat, 5));
    units.add(new NumberTickUnit(128 * gbyte, numberFormat, 2));
    units.add(new NumberTickUnit(256 * gbyte, numberFormat, 2));
    units.add(new NumberTickUnit(512 * gbyte, numberFormat, 5));

    long tbyte = ThinMemoryFormat.TBYTE;
    units.add(new NumberTickUnit(tbyte, numberFormat));
    units.add(new NumberTickUnit(8 * tbyte, numberFormat));
    units.add(new NumberTickUnit(16 * tbyte, numberFormat));
    units.add(new NumberTickUnit(32 * tbyte, numberFormat));
    units.add(new NumberTickUnit(64 * tbyte, numberFormat));
    units.add(new NumberTickUnit(128 * tbyte, numberFormat));
    units.add(new NumberTickUnit(256 * tbyte, numberFormat));
    units.add(new NumberTickUnit(512 * tbyte, numberFormat));

    return units;
  }

  private class TierUsageByCacheChartListener extends PieChartListener {
    @Override
    void selectSection(Comparable sectionKey) {
      refreshTimer.stop();
      selectCacheSlice(sectionKey);
      refreshTimer.start();
    }
  }

  private void selectTier(String tierName) {
    tierChooser.setSelectedItem(tierName);
  }

  private String selectedTier() {
    return tierChooser.getSelectedItem().toString();
  }

  private abstract class PieChartListener implements ChartMouseListener {
    public void chartMouseClicked(ChartMouseEvent e) {
      ChartEntity chartEntity = e.getEntity();
      if (chartEntity instanceof PieSectionEntity) {
        selectSection(((PieSectionEntity) chartEntity).getSectionKey());
      }
    }

    public void chartMouseMoved(ChartMouseEvent e) {
      /**/
    }

    abstract void selectSection(Comparable sectionKey);
  }

  private class TierChartListener implements ChartMouseListener {
    private String tierName;

    private TierChartListener(String tierName) {
      this.tierName = tierName;
    }

    public void chartMouseClicked(ChartMouseEvent e) {
      selectTier(tierName);
    }

    public void chartMouseMoved(ChartMouseEvent e) {
      /**/
    }

    private synchronized void setTier(String tierName) {
      this.tierName = tierName;
    }
  }

  private void updateCacheSizeByTier() {
    String selectedCache = (String) selectedCacheChooser.getSelectedItem();
    if (selectedCache != null) {
      String selectedTier = selectedTier();

      CacheModelInstance cmi = cacheManagerInstance.getCacheModelInstance(selectedCache);
      if (cmi == null) { return; }

      boolean tcClustered = cmi.isTerracottaClustered();
      CacheTierSize tierSize = cacheSizes.get(cmi);
      long usedLocalHeap = tierSize.getLocalHeapSizeInBytes();

      long localHeapMissRate = tierSize.getLocalHeapMissRate();
      long localOffHeapRate = tierSize.getLocalOffHeapMissRate();
      long localDiskRate = tierSize.getLocalDiskMissRate();

      localHeapMissesDataset.clear();
      localOffHeapMissesDataset.clear();
      localDiskMissesDataset.clear();

      if (localHeapMissRate > 0) {
        localHeapMissesDataset.setValue(localHeapMissRate, LOCAL_HEAP, MISS_RATE);
      }
      localHeapMissesLabel.setText("Misses/sec.: " + THIN_DECIMAL_FORMAT.format(localHeapMissRate));
      updateTierPlotSelectionGraphics(localHeapMissesPlot, selectedTier, LOCAL_HEAP);

      if (localOffHeapRate > 0) {
        localOffHeapMissesDataset.setValue(localOffHeapRate, LOCAL_OFFHEAP, MISS_RATE);
      }
      localOffHeapMissesLabel.setText("Misses/sec.: " + THIN_DECIMAL_FORMAT.format(localOffHeapRate));
      updateTierPlotSelectionGraphics(localOffHeapMissesPlot, selectedTier, LOCAL_OFFHEAP);

      if (localDiskRate > 0) {
        localDiskMissesDataset.setValue(localDiskRate, tcClustered ? REMOTE : LOCAL_DISK, MISS_RATE);
      }
      localDiskMissesPlot.getRenderer().setSeriesPaint(0, tcClustered ? REMOTE_COLOR : LOCAL_DISK_COLOR);
      localDiskMissesStatusView.setIndicator(tcClustered ? REMOTE_COLOR : LOCAL_DISK_COLOR);
      localDiskMissesStatusView.getLabel().setText(tcClustered ? REMOTE : LOCAL_DISK);
      localDiskMissesLabel.setText("Misses/sec.: " + THIN_DECIMAL_FORMAT.format(localDiskRate));
      updateTierPlotSelectionGraphics(localDiskMissesPlot, selectedTier, tcClustered ? REMOTE : LOCAL_DISK);

      long maxMissesUpper = determineMax(localHeapMissRate, localOffHeapRate, localDiskRate);
      setRangeAxisPlusExtra(maxMissesUpper, localHeapMissesPlot, localOffHeapMissesPlot, localDiskMissesPlot);

      cacheLocalHeapCapacityDataset.clear();
      cacheLocalHeapCapacityPlot.clearRangeMarkers();
      long reservedLocalHeap = tierSize.getMaxBytesLocalHeap();
      if (reservedLocalHeap > 0) {
        cacheLocalHeapCapacityPlot.getRangeAxis().setRange(0, reservedLocalHeap);
        addMaximumRangeValueMarker(cacheLocalHeapCapacityPlot, reservedLocalHeap);
      } else {
        cacheLocalHeapCapacityPlot.getRangeAxis().setAutoRange(true);
      }
      cacheLocalHeapCapacityDataset.setValue(usedLocalHeap, USED, LOCAL_HEAP);
      cacheLocalHeapCapacityLabel.setText(handleMaximum(reservedLocalHeap, THIN_MEMORY_FORMAT) + "  Used: "
                                          + THIN_MEMORY_FORMAT.format(usedLocalHeap)
                                          + handleAvailable(reservedLocalHeap, usedLocalHeap, THIN_MEMORY_FORMAT));
      updatePlotRangeAxis(cacheLocalHeapCapacityPlot);
      updateTierPlotSelectionGraphics(cacheLocalHeapCapacityPlot, selectedTier, LOCAL_HEAP);
      cacheLocalHeapCapacityTipGenerator.setup(LOCAL_HEAP, usedLocalHeap, 0, reservedLocalHeap,
                                               tierSize.getLocalHeapSize());

      cacheLocalOffHeapCapacityDataset.clear();
      cacheLocalOffHeapCapacityPlot.clearRangeMarkers();
      long reservedLocalOffHeap = tierSize.getMaxBytesLocalOffHeap();
      if (reservedLocalOffHeap > 0) {
        cacheLocalOffHeapCapacityPlot.getRangeAxis().setRange(0, reservedLocalOffHeap);
        addMaximumRangeValueMarker(cacheLocalOffHeapCapacityPlot, reservedLocalOffHeap);
      } else {
        cacheLocalOffHeapCapacityPlot.getRangeAxis().setAutoRange(true);
      }
      long usedLocalOffHeap = tierSize.getLocalOffHeapSizeInBytes();
      cacheLocalOffHeapCapacityDataset.setValue(usedLocalOffHeap, USED, LOCAL_OFFHEAP);
      cacheLocalOffHeapCapacityLabel.setText(handleMaximum(reservedLocalOffHeap, THIN_MEMORY_FORMAT)
                                             + "  Used: "
                                             + THIN_MEMORY_FORMAT.format(usedLocalOffHeap)
                                             + handleAvailable(reservedLocalOffHeap, usedLocalOffHeap,
                                                               THIN_MEMORY_FORMAT));
      updatePlotRangeAxis(cacheLocalOffHeapCapacityPlot);
      updateTierPlotSelectionGraphics(cacheLocalOffHeapCapacityPlot, selectedTier, LOCAL_OFFHEAP);
      cacheLocalOffHeapCapacityTipGenerator.setup(LOCAL_OFFHEAP, usedLocalOffHeap, 0, reservedLocalOffHeap,
                                                  tierSize.getLocalOffHeapSize());

      cacheLocalDiskCapacityDataset.clear();
      cacheLocalDiskCapacityPlot.clearRangeMarkers();
      long usedLocalDisk = tierSize.sizeInBytesForTier(tcClustered ? REMOTE : LOCAL_DISK);
      long reservedLocalDisk = tierSize.maxBytesForTier(tcClustered ? REMOTE : LOCAL_DISK);
      if (reservedLocalDisk > 0) {
        cacheLocalDiskCapacityPlot.getRangeAxis().setRange(0, Math.max(reservedLocalDisk, usedLocalDisk));
        addMaximumRangeValueMarker(cacheLocalDiskCapacityPlot, reservedLocalDisk);
      } else {
        cacheLocalDiskCapacityPlot.getRangeAxis().setAutoRange(true);
      }
      cacheLocalDiskCapacityDataset.setValue(usedLocalDisk, USED, tcClustered ? REMOTE : LOCAL_DISK);
      cacheLocalDiskCapacityStatusView.getLabel().setText(tcClustered ? REMOTE + " (estimate)" : LOCAL_DISK);
      cacheLocalDiskCapacityStatusView.setIndicator(tcClustered ? REMOTE_COLOR : LOCAL_DISK_COLOR);
      cacheLocalDiskCapacityPlot.getRenderer().setSeriesPaint(0, tcClustered ? REMOTE_COLOR : LOCAL_DISK_COLOR);
      cacheLocalDiskCapacityLabel.setText(handleMaximum(reservedLocalDisk, THIN_MEMORY_FORMAT) + "  Used: "
                                          + THIN_MEMORY_FORMAT.format(usedLocalDisk)
                                          + handleAvailable(reservedLocalDisk, usedLocalDisk, THIN_MEMORY_FORMAT));
      cacheLocalDiskCapacityChartListener.setTier(tcClustered ? REMOTE : LOCAL_DISK);
      updatePlotRangeAxis(cacheLocalDiskCapacityPlot);
      updateTierPlotSelectionGraphics(cacheLocalDiskCapacityPlot, selectedTier, tcClustered ? REMOTE : LOCAL_DISK);
      CategoryToolTipGenerator tipGenerator;
      if (tcClustered) {
        cacheRemoteCapacityTipGenerator.setup(REMOTE, tierSize.maxEntriesForTier(REMOTE), reservedLocalDisk,
                                              tierSize.entriesForTier(REMOTE), usedLocalDisk);
        tipGenerator = cacheRemoteCapacityTipGenerator;
      } else {
        cacheLocalDiskCapacityTipGenerator.setup(LOCAL_DISK, usedLocalDisk, 0, reservedLocalDisk,
                                                 tierSize.getLocalDiskSize());
        tipGenerator = cacheLocalDiskCapacityTipGenerator;
      }
      cacheLocalDiskCapacityPlot.getRenderer().setBaseToolTipGenerator(tipGenerator);

      synchronizeRangeAxes(cacheLocalHeapCapacityPlot, cacheLocalOffHeapCapacityPlot, cacheLocalDiskCapacityPlot);
    }
  }

  private void synchronizeRangeAxes(CategoryPlot... plots) {
    double upperBound = 0;

    for (CategoryPlot plot : plots) {
      upperBound = Math.max(upperBound, plot.getRangeAxis().getUpperBound());
    }
    if (upperBound > 10) {
      upperBound += (upperBound / 10);
    } else {
      upperBound++;
    }
    for (CategoryPlot plot : plots) {
      plot.getRangeAxis().setRange(0, upperBound);
    }
  }

  private String handleReserved(long reserved, DecimalFormat format) {
    if (reserved <= 0) { return "  Reserved: " + format.format(0); }
    return "";
  }

  private String handleMaximum(long max, DecimalFormat format) {
    if (max <= 0) { return "  Max: " + format.format(0); }
    return "";
  }

  private String handleAvailable(long max, long used, DecimalFormat format) {
    if (max > 0 && (max - used) > 0) { return "  Available: " + format.format(max - used); }
    return "";
  }

  private void selectSlice(PiePlot plot, Comparable sliceKey) {
    List<Comparable> keys = plot.getDataset().getKeys();
    for (Comparable key : keys) {
      double explodePercent = 0.0d;
      if (key.equals(sliceKey)) {
        explodePercent = PIE_SLICE_EXPLODE_PERCENT;
        selectedCacheChooser.setSelectedItem(key.toString());
      }
      plot.setExplodePercent(key, explodePercent);
    }
  }

  private boolean listeningToCacheChooser() {
    List<ActionListener> l = new ArrayList<ActionListener>(Arrays.asList(selectedCacheChooser.getActionListeners()));
    return l.contains(selectedCacheListener);
  }

  private void selectSliceQuietly(PiePlot plot, Comparable sliceKey) {
    boolean wasListening = listeningToCacheChooser();
    if (wasListening) {
      selectedCacheChooser.removeActionListener(selectedCacheListener);
    }
    selectSlice(plot, sliceKey);
    if (wasListening) {
      selectedCacheChooser.addActionListener(selectedCacheListener);
    }
  }

  private void updateTierUsageByCacheTable(String selectedCache) {
    int cacheTableRow = cacheTableRowForName(selectedCache);
    if (cacheTableRow != -1) {
      tierUsageByCacheTable.getSelectionModel().removeListSelectionListener(tierUsageByCacheTableSelectionListener);
      tierUsageByCacheTable.setSelectedRow(cacheTableRow);
      tierUsageByCacheTable.getSelectionModel().addListSelectionListener(tierUsageByCacheTableSelectionListener);
    }
  }

  private void selectCacheSlice(Comparable sliceKey) {
    if (!sliceKey.equals(AVAILABLE)) {
      selectSliceQuietly(tierUsageByCachePlot, sliceKey);

      updateTierUsageByCacheTable(sliceKey.toString());
      updateCacheSizeByTier();

      String cacheSummaryText = null;
      CacheModelInstance cmi = cacheManagerInstance.getCacheModelInstance(sliceKey.toString());
      if (cmi != null) {
        if (cmi.isPinned()) {
          cacheSummaryText = " is pinned to " + cmi.getPinnedToStore();
        } else {
          SettingsCacheModel scm = cacheManagerInstance.getSettingsCacheModel(cmi);
          if (scm != null) {
            if (scm.hasSizeBasedLimits()) {
              cacheSummaryText = " is size-based";
            } else {
              cacheSummaryText = " is pool-based";
            }
          }
        }
      }
      cacheSummaryLabel.setText(cacheSummaryText);
    }
  }

  private int cacheTableRowForName(String cacheName) {
    for (int row = 0; row < tierUsageByCacheTableModel.getRowCount(); row++) {
      String name = (String) tierUsageByCacheTable.getValueAt(row, 0);
      if (cacheName.equals(name)) { return row; }
    }
    return -1;
  }

  private final CachePieValuesGenerator CACHE_PIE_VALUES_GENERATOR = new CachePieValuesGenerator();

  private class CachePieValuesGenerator extends StandardPieToolTipGenerator {
    public CachePieValuesGenerator() {
      super();
    }

    @Override
    protected String generateSectionLabel(PieDataset dataset, Comparable key) {
      return super.generateSectionLabel(dataset, key);
    }

    private Object[] getTableItemArray(PieDataset dataset, Comparable key) {
      String selectedTier = selectedTier();
      String cacheName = key.toString();
      Object[] result = new Object[5];
      long totalUsed = sizeInBytesForTier(selectedTier);
      result[0] = cacheName;
      Long value = Long.valueOf(sizeInBytesForTier(cacheName, selectedTier));
      result[1] = (value != null) ? value : 0;
      double percentOfUsed = 0.0;
      if (value != null) {
        double v = value.doubleValue();
        if (v > 0.0) {
          percentOfUsed = v / (totalUsed > 0 ? totalUsed : 1);
        }
      }
      result[2] = Double.valueOf(percentOfUsed);

      long entries = entriesForTier(cacheName, selectedTier);
      result[3] = Long.valueOf(entries);
      result[4] = (value != null ? value.doubleValue() : 0) / (entries > 0 ? entries : 1);

      return result;
    }
  }

  private long sizeInBytesForTier(String tierName) {
    long result = 0;
    for (CacheModelInstance cmi : cacheManagerInstance.cacheModelInstances()) {
      if (cmi != null) {
        if ((tierName.equals(REMOTE) && !cmi.isTerracottaClustered())
            || (tierName.equals(LOCAL_DISK) && cmi.isTerracottaClustered())) {
          continue;
        }
        CacheTierSize cts = cacheSizes.get(cmi);
        if (cts != null) {
          result += cts.sizeInBytesForTier(tierName);
        }
      }
    }
    return result;
  }

  private long sizeInBytesForTier(String cacheName, String tierName) {
    CacheModelInstance cmi = cacheManagerInstance.getCacheModelInstance(cacheName);
    if (cmi != null) {
      CacheTierSize cts = cacheSizes.get(cmi);
      if (cts != null) { return cts.sizeInBytesForTier(tierName); }
    }
    return 0L;
  }

  private long entriesForTier(String cacheName, String tierName) {
    CacheModelInstance cmi = cacheManagerInstance.getCacheModelInstance(cacheName);
    if (cmi != null) {
      CacheTierSize cts = cacheSizes.get(cmi);
      if (cts != null) { return cts.entriesForTier(tierName); }
    }
    return 0L;
  }

  private class CachePieToolTipGenerator extends StandardPieToolTipGenerator {
    public CachePieToolTipGenerator() {
      super("{0}: {1} ({2})", THIN_MEMORY_FORMAT, NumberFormat.getPercentInstance());
    }

    @Override
    protected String generateSectionLabel(PieDataset dataset, Comparable key) {
      return super.generateSectionLabel(dataset, key);
    }
  }

  private void refreshLater() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        refresh();
      }
    });
  }

  public void cacheManagerInstanceChanged(CacheManagerInstance theCacheManagerInstance) {
    /**/
  }

  public void cacheModelInstanceAdded(CacheModelInstance cacheModelInstance) {
    refreshLater();
  }

  public void cacheModelInstanceRemoved(CacheModelInstance cacheModelInstance) {
    refreshLater();
  }

  public void cacheModelInstanceChanged(CacheModelInstance cacheModelInstance) {
    /**/
  }

  @Override
  public void tearDown() {
    if (cacheManagerInstance != null) {
      cacheManagerInstance.removeCacheManagerInstanceListener(this);
    }
    super.tearDown();
  }
}

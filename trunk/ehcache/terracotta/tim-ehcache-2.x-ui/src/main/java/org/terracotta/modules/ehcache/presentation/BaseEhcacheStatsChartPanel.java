/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation;

import org.apache.commons.lang.StringUtils;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.TimeSeries;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerModel;

import com.tc.admin.AbstractClusterListener;
import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.StatusView;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XTabbedPane;
import com.tc.admin.dso.BaseRuntimeStatsPanel;
import com.tc.admin.model.ClientConnectionListener;
import com.tc.admin.model.IClient;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;
import com.tc.admin.model.PolledAttributesResult;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.swing.BorderFactory;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

public class BaseEhcacheStatsChartPanel extends BaseRuntimeStatsPanel implements ClientConnectionListener,
    PreferenceChangeListener {
  protected final IClusterModel                   clusterModel;
  protected final CacheManagerModel               cacheManagerModel;
  protected final ClusterListener                 clusterListener;
  protected ObjectName                            statsBeanObjectName;

  private XTabbedPane                             tabbedPane;
  protected BaseRuntimeStatsPanel                 usagePanel;
  protected BaseRuntimeStatsPanel                 searchPanel;
  private int                                     searchTabIndex;
  protected BaseRuntimeStatsPanel                 jtaPanel;
  private int                                     jtaTabIndex;
  protected BaseRuntimeStatsPanel                 writeBehindPanel;
  private int                                     writeBehindTabIndex;

  public static final int                         CHART_COUNT                        = 4;
  public ChartPanel[]                             chartPanels                        = new ChartPanel[CHART_COUNT];
  public ChartContentProvider[]                   chartProviders                     = new ChartContentProvider[CHART_COUNT];

  private final Map<String, ChartContentProvider> contentProviderMap;

  protected final Preferences                     activeProvidersPrefs;

  protected final static String                   ACTIVE_PROVIDERS_PREFS_KEY         = "ActiveChartProviders";
  protected final static String                   DEFAULT_ACTIVE_PROVIDERS           = "CacheHitRatio,CacheHitMissRate,CacheUpdateRate,CachePutRate";

  protected TimeSeries                            cacheSearchRateSeries;
  protected TimeSeries                            cacheAverageSearchTimeSeries;
  protected TimeSeries                            transactionCommitRateSeries;
  protected TimeSeries                            transactionRollbackRateSeries;
  protected TimeSeries                            writerQueueLengthSeries;
  protected TimeSeries                            writerMaxQueueSizeSeries;

  protected XLabel                                cacheSearchRateLabel;
  protected XLabel                                cacheAverageSearchTimeLabel;
  protected XLabel                                transactionCommitRateLabel;
  protected XLabel                                transactionRollbackRateLabel;
  protected StatusView                            writerQueueLengthLabel;
  protected StatusView                            writerMaxQueueSizeLabel;

  protected final String                          cacheSearchRateLabelFormat         = "{0,number,integer} Searches/sec.";
  protected final String                          cacheAverageSearchTimeLabelFormat  = "{0,number,integer} millis./Search";
  protected final String                          transactionCommitRateLabelFormat   = "{0,number,integer} Commits/sec.";
  protected final String                          transactionRollbackRateLabelFormat = "{0,number,integer} Rollbacks/sec.";
  protected final String                          writerQueueLengthLabelFormat       = "{0,number,integer} Pending Writes";
  protected final String                          writerMaxQueueSizeLabelFormat      = "Max Pending Writes: {0,number,integer}";

  private static final boolean                    showCacheHitRatioLabel             = true;

  protected static final String                   CACHE_HIT_SAMPLE_ATTR              = "CacheHitRate";
  protected static final String                   CACHE_IN_MEMORY_HIT_SAMPLE_ATTR    = "CacheInMemoryHitRate";
  protected static final String                   CACHE_OFF_HEAP_HIT_SAMPLE_ATTR     = "CacheOffHeapHitRate";
  protected static final String                   CACHE_ON_DISK_HIT_SAMPLE_ATTR      = "CacheOnDiskHitRate";
  protected static final String                   CACHE_MISS_SAMPLE_ATTR             = "CacheMissRate";
  protected static final String                   CACHE_IN_MEMORY_MISS_SAMPLE_ATTR   = "CacheInMemoryMissRate";
  protected static final String                   CACHE_OFF_HEAP_MISS_SAMPLE_ATTR    = "CacheOffHeapMissRate";
  protected static final String                   CACHE_ON_DISK_MISS_SAMPLE_ATTR     = "CacheOnDiskMissRate";
  protected static final String                   CACHE_PUT_SAMPLE_ATTR              = "CachePutRate";
  protected static final String                   CACHE_UPDATE_SAMPLE_ATTR           = "CacheUpdateRate";
  protected static final String                   CACHE_REMOVE_SAMPLE_ATTR           = "CacheRemoveRate";
  protected static final String                   CACHE_EVICTION_SAMPLE_ATTR         = "CacheEvictionRate";
  protected static final String                   CACHE_EXPIRATION_SAMPLE_ATTR       = "CacheExpirationRate";
  protected static final String                   CACHE_AVERAGE_GET_TIME_ATTR        = "CacheAverageGetTime";

  protected static final String                   CACHE_SEARCH_RATE_ATTR             = "CacheSearchRate";
  protected static final String                   CACHE_AVERAGE_SEARCH_TIME_ATTR     = "CacheAverageSearchTime";
  protected static final String                   TRANSACTION_COMMIT_RATE_ATTR       = "TransactionCommitRate";
  protected static final String                   TRANSACTION_ROLLBACK_RATE_ATTR     = "TransactionRollbackRate";
  protected static final String                   WRITER_QUEUE_LENGTH_ATTR           = "WriterQueueLength";
  protected static final String                   WRITER_MAX_QUEUE_SIZE_ATTR         = "WriterMaxQueueSize";
  protected static final String                   HAS_WRITE_BEHIND_WRITER_ATTR       = "HasWriteBehindWriter";
  protected static final String                   TRANSACTIONAL_ATTR                 = "Transactional";
  protected static final String                   SEARCHABLE_ATTR                    = "Searchable";

  protected static final String[]                 POLLED_ATTRS                       = { CACHE_HIT_SAMPLE_ATTR,
      CACHE_IN_MEMORY_HIT_SAMPLE_ATTR, CACHE_OFF_HEAP_HIT_SAMPLE_ATTR, CACHE_ON_DISK_HIT_SAMPLE_ATTR,
      CACHE_MISS_SAMPLE_ATTR, CACHE_IN_MEMORY_MISS_SAMPLE_ATTR, CACHE_OFF_HEAP_MISS_SAMPLE_ATTR,
      CACHE_ON_DISK_MISS_SAMPLE_ATTR, CACHE_PUT_SAMPLE_ATTR, CACHE_UPDATE_SAMPLE_ATTR, CACHE_REMOVE_SAMPLE_ATTR,
      CACHE_EVICTION_SAMPLE_ATTR, CACHE_EXPIRATION_SAMPLE_ATTR, CACHE_AVERAGE_GET_TIME_ATTR, CACHE_SEARCH_RATE_ATTR,
      CACHE_AVERAGE_SEARCH_TIME_ATTR, TRANSACTION_COMMIT_RATE_ATTR, TRANSACTION_ROLLBACK_RATE_ATTR,
      WRITER_QUEUE_LENGTH_ATTR, WRITER_MAX_QUEUE_SIZE_ATTR, HAS_WRITE_BEHIND_WRITER_ATTR, TRANSACTIONAL_ATTR,
      SEARCHABLE_ATTR                                                               };

  protected static final Set<String>              POLLED_ATTRS_SET                   = new HashSet<String>(
                                                                                                           Arrays
                                                                                                               .asList(POLLED_ATTRS));

  protected static final ResourceBundle           bundle                             = ResourceBundle
                                                                                         .getBundle(EhcacheResourceBundle.class
                                                                                             .getName());

  protected BaseEhcacheStatsChartPanel(ApplicationContext appContext, CacheManagerModel cacheManagerModel) {
    super(appContext);

    this.clusterModel = cacheManagerModel.getClusterModel();
    this.cacheManagerModel = cacheManagerModel;
    this.clusterListener = new ClusterListener(clusterModel);

    contentProviderMap = new LinkedHashMap<String, ChartContentProvider>();
    contentProviderMap.put("CacheHitRatio", new HitRatioProvider());
    contentProviderMap.put("CacheHitMissRate", new HitMissRateProvider());
    contentProviderMap.put("CacheUpdateRate", new UpdateRateProvider());
    contentProviderMap.put("CachePutRate", new PutRateProvider());
    contentProviderMap.put("CacheRemoveRate", new RemoveRateProvider());
    contentProviderMap.put("CacheEvictionRate", new EvictionRateProvider());
    contentProviderMap.put("CacheExpirationRate", new ExpirationRateProvider());
    contentProviderMap.put("CacheInMemoryHitRate", new InMemoryHitRateProvider());
    contentProviderMap.put("CacheOffHeapHitRate", new OffHeapHitRateProvider());
    contentProviderMap.put("CacheOnDiskHitRate", new OnDiskHitRateProvider());
    contentProviderMap.put("CacheInMemoryMissRate", new InMemoryMissRateProvider());
    contentProviderMap.put("CacheOffHeapMissRate", new OffHeapMissRateProvider());
    contentProviderMap.put("CacheOnDiskMissRate", new OnDiskMissRateProvider());
    contentProviderMap.put("CacheAverageGetTime", new AverageGetTimeProvider());

    this.activeProvidersPrefs = appContext.getPrefs().node(BaseEhcacheStatsChartPanel.class.getName());

    setup(chartsPanel);

    this.activeProvidersPrefs.addPreferenceChangeListener(this);
  }

  protected ObjectName getBeanName() throws MalformedObjectNameException {
    return EhcacheStatsUtils.getSampledCacheManagerBeanName(cacheManagerModel.getName());
  }

  @Override
  public void preferenceChange(PreferenceChangeEvent evt) {
    String key = evt.getKey();
    String newValue = evt.getNewValue();

    if (ACTIVE_PROVIDERS_PREFS_KEY.equals(key)) {
      String[] providerNames = StringUtils.split(newValue, ",");
      for (int i = 0; i < CHART_COUNT; i++) {
        String providerName = providerNames[i];
        ChartContentProvider contentProvider = chartProviders[i];
        if (!contentProvider.getName().equals(providerName)) {
          ChartPanel chartPanel = chartPanels[i];
          chartPanel.removeAll();
          ChartContentProvider chartProvider = contentProviderMap.get(providerName);
          chartProvider.configure(chartPanel);
          chartProviders[i] = chartProvider;
        }
      }
    }
  }

  public void setup() {
    try {
      this.statsBeanObjectName = getBeanName();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    clusterModel.addPropertyChangeListener(clusterListener);
    if (clusterModel.isReady()) {
      init();
    }
  }

  public CacheManagerModel getCacheManagerModel() {
    return cacheManagerModel;
  }

  public IClusterModel getClusterModel() {
    return clusterModel;
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

    @Override
    protected void handleActiveCoordinator(IServer oldActive, IServer newActive) {
      if (oldActive != null) {
        oldActive.removeClientConnectionListener(BaseEhcacheStatsChartPanel.this);
      }
      if (newActive != null) {
        newActive.addClientConnectionListener(BaseEhcacheStatsChartPanel.this);
      }
    }

    @Override
    protected void handleUncaughtError(Exception e) {
      if (appContext != null) {
        appContext.log(e);
      } else {
        super.handleUncaughtError(e);
      }
    }
  }

  protected void init() {
    IServer activeCoord = clusterModel.getActiveCoordinator();
    if (activeCoord != null) {
      activeCoord.addClientConnectionListener(this);
    }
    addPolledAttributeListener();
    setAutoStart(false);
    startMonitoringRuntimeStats();
  }

  public void suspend() {
    removePolledAttributeListener();
  }

  protected void addPolledAttributeListener() {
    /**/
  }

  protected void removePolledAttributeListener() {
    /**/
  }

  public void clientConnected(IClient client) {
    /**/
  }

  public void clientDisconnected(IClient client) {
    /**/
  }

  private XContainer createUsagePanel() {
    XContainer result = new XContainer(new BorderLayout());
    XLabel label = new XLabel("Use chart context-menu to change graphed metric",
                              EhcachePresentationUtils.LIGHT_BULB_ICON);
    label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 3));
    result.add(label, BorderLayout.NORTH);
    label.setHorizontalAlignment(SwingConstants.RIGHT);
    result.add(usagePanel = new BaseRuntimeStatsPanel(appContext));
    return result;
  }

  @Override
  protected synchronized void setup(final XContainer chartsPanel) {
    chartsPanel.setLayout(new BorderLayout());
    chartsPanel.add(tabbedPane = new XTabbedPane());
    tabbedPane.addTab("Usage", createUsagePanel());
    tabbedPane.setToolTipTextAt(0, "Use chart context-menus to change graphed metric.");
    searchTabIndex = tabbedPane.getTabCount();
    tabbedPane.addTab("Search", searchPanel = new BaseRuntimeStatsPanel(appContext));
    jtaTabIndex = tabbedPane.getTabCount();
    tabbedPane.addTab("JTA", jtaPanel = new BaseRuntimeStatsPanel(appContext));
    writeBehindTabIndex = tabbedPane.getTabCount();
    tabbedPane.addTab("Write-Behind", writeBehindPanel = new BaseRuntimeStatsPanel(appContext));

    usagePanel.getChartsPanel().setLayout(new GridLayout(2, 2));
    int i = 0;
    String activeProvidersPrefValue = this.activeProvidersPrefs.get(ACTIVE_PROVIDERS_PREFS_KEY,
                                                                    DEFAULT_ACTIVE_PROVIDERS);
    String[] activeProviders = StringUtils.split(activeProvidersPrefValue, ",");
    String[] defaultProviders = StringUtils.split(DEFAULT_ACTIVE_PROVIDERS, ",");
    for (String providerName : activeProviders) {
      if ((chartProviders[i] = contentProviderMap.get(providerName)) == null) {
        chartProviders[i] = contentProviderMap.get(defaultProviders[i]);
      }
      chartPanels[i] = setupChartContentProvider(usagePanel.getChartsPanel(), chartProviders[i]);
      i++;
    }

    searchPanel.getChartsPanel().setLayout(new GridLayout(2, 1));
    setupCacheSearchRatePanel(searchPanel.getChartsPanel());
    setupCacheAverageSearchTimePanel(searchPanel.getChartsPanel());

    jtaPanel.getChartsPanel().setLayout(new GridLayout(2, 1));
    setupTransactionCommitRatePanel(jtaPanel.getChartsPanel());
    setupTransactionRollbackRatePanel(jtaPanel.getChartsPanel());

    writeBehindPanel.getChartsPanel().setLayout(new GridLayout(1, 1));
    setupWriteBehindPanel(writeBehindPanel.getChartsPanel());
  }

  public ChartPanel[] getChartPanels() {
    return chartPanels;
  }

  public ChartContentProvider[] getChartContentProviders() {
    return chartProviders;
  }

  private class UpdateRateProvider implements ChartContentProvider {
    private TimeSeries   timeSeries;
    private XLabel       label;
    private final String labelFormat = "{0,number,integer} Updates/sec.";
    private long         value;

    public void configure(ChartPanel chartPanel) {
      String text = bundle.getString("cache.update.rate");
      timeSeries = createTimeSeries(text);
      JFreeChart chart = createChart(timeSeries, false);
      chartPanel.setChart(chart);
      chartPanel.setBorder(new TitledBorder(text));
      chartPanel.setToolTipText(text);
      chartPanel.setLayout(new BorderLayout());
      chartPanel.add(label = createOverlayLabel());

      ((XYPlot) chart.getPlot()).getRenderer().setSeriesPaint(0, EhcachePresentationUtils.PUT_FILL_COLOR);
    }

    public void acceptPolledAttributeResult(PolledAttributesResult result, IClient client, ObjectName on) {
      Number n = (Number) result.getPolledAttribute(client, on, CACHE_UPDATE_SAMPLE_ATTR);
      if (n != null) {
        value += n.longValue();
      }
    }

    public void render() {
      updateSeries(timeSeries, Long.valueOf(value));
      label.setText(MessageFormat.format(labelFormat, value));
      value = 0;
    }

    public void clear() {
      if (timeSeries != null) {
        timeSeries.clear();
      }
    }

    public String getName() {
      return "CacheUpdateRate";
    }
  }

  private class HitRatioProvider implements ChartContentProvider {
    private TimeSeries   timeSeries;
    private XLabel       label;
    private final String labelFormat = "{0,number,##}% Hit Ratio";
    private long         hitRate;
    private long         missRate;

    public void configure(ChartPanel chartPanel) {
      String text = bundle.getString("cache.hit.ratio");
      timeSeries = createTimeSeries(text);
      JFreeChart chart = createChart(timeSeries, false);
      chartPanel.setChart(chart);
      chartPanel.setBorder(new TitledBorder(text));
      chartPanel.setToolTipText(text);
      chartPanel.setLayout(new BorderLayout());
      chartPanel.add(label = createOverlayLabel());

      XYPlot plot = (XYPlot) chart.getPlot();
      ((NumberAxis) plot.getRangeAxis()).setRange(0.0, 105.0);

      plot.getRenderer().setSeriesPaint(0, EhcachePresentationUtils.HIT_FILL_COLOR);
    }

    public void acceptPolledAttributeResult(PolledAttributesResult result, IClient client, ObjectName on) {
      Number n = (Number) result.getPolledAttribute(client, on, CACHE_HIT_SAMPLE_ATTR);
      if (n != null) {
        hitRate += n.longValue();
      }
      n = (Number) result.getPolledAttribute(client, on, CACHE_MISS_SAMPLE_ATTR);
      if (n != null) {
        missRate += n.longValue();
      }
    }

    public void render() {
      long readRate = hitRate + missRate;
      double cacheHitRatio = 0;
      if (readRate > 0) {
        cacheHitRatio = (hitRate / ((double) readRate)) * 100;
      }
      updateSeries(timeSeries, Double.valueOf(cacheHitRatio));
      if (showCacheHitRatioLabel) {
        label.setText(MessageFormat.format(labelFormat, cacheHitRatio));
      }
      hitRate = missRate = 0;
    }

    public void clear() {
      if (timeSeries != null) {
        timeSeries.clear();
      }
    }

    public String getName() {
      return "CacheHitRatio";
    }
  }

  private class PutRateProvider implements ChartContentProvider {
    private TimeSeries   timeSeries;
    private XLabel       label;
    private final String labelFormat = "{0,number,integer} Puts/sec.";
    private long         value;

    public void configure(ChartPanel chartPanel) {
      String text = bundle.getString("cache.put.rate");
      timeSeries = createTimeSeries(text);
      JFreeChart chart = createChart(timeSeries, false);
      chartPanel.setChart(chart);
      chartPanel.setBorder(new TitledBorder(text));
      chartPanel.setToolTipText(text);
      chartPanel.setLayout(new BorderLayout());
      chartPanel.add(label = createOverlayLabel());

      ((XYPlot) chart.getPlot()).getRenderer().setSeriesPaint(0, EhcachePresentationUtils.PUT_FILL_COLOR);
    }

    public void acceptPolledAttributeResult(PolledAttributesResult result, IClient client, ObjectName on) {
      Number n = (Number) result.getPolledAttribute(client, on, CACHE_PUT_SAMPLE_ATTR);
      if (n != null) {
        value += n.longValue();
      }
    }

    public void render() {
      updateSeries(timeSeries, Long.valueOf(value));
      label.setText(MessageFormat.format(labelFormat, value));
      value = 0;
    }

    public void clear() {
      if (timeSeries != null) {
        timeSeries.clear();
      }
    }

    public String getName() {
      return "CachePutRate";
    }
  }

  private class RemoveRateProvider implements ChartContentProvider {
    private TimeSeries   timeSeries;
    private XLabel       label;
    private final String labelFormat = "{0,number,integer} Removes/sec.";
    private long         value;

    public void configure(ChartPanel chartPanel) {
      String text = bundle.getString("cache.remove.rate");
      timeSeries = createTimeSeries(text);
      JFreeChart chart = createChart(timeSeries, false);
      chartPanel.setChart(chart);
      chartPanel.setBorder(new TitledBorder(text));
      chartPanel.setToolTipText(text);
      chartPanel.setLayout(new BorderLayout());
      chartPanel.add(label = createOverlayLabel());

      ((XYPlot) chart.getPlot()).getRenderer().setSeriesPaint(0, EhcachePresentationUtils.MISS_FILL_COLOR);
    }

    public void acceptPolledAttributeResult(PolledAttributesResult result, IClient client, ObjectName on) {
      Number n = (Number) result.getPolledAttribute(client, on, CACHE_REMOVE_SAMPLE_ATTR);
      if (n != null) {
        value += n.longValue();
      }
    }

    public void render() {
      updateSeries(timeSeries, Long.valueOf(value));
      label.setText(MessageFormat.format(labelFormat, value));
      value = 0;
    }

    public void clear() {
      if (timeSeries != null) {
        timeSeries.clear();
      }
    }

    public String getName() {
      return "CacheRemoveRate";
    }
  }

  private class EvictionRateProvider implements ChartContentProvider {
    private TimeSeries   timeSeries;
    private XLabel       label;
    private final String labelFormat = "{0,number,integer} Evictions/sec.";
    private long         value;

    public void configure(ChartPanel chartPanel) {
      String text = bundle.getString("cache.eviction.rate");
      timeSeries = createTimeSeries(text);
      JFreeChart chart = createChart(timeSeries, false);
      chartPanel.setChart(chart);
      chartPanel.setBorder(new TitledBorder(text));
      chartPanel.setToolTipText(text);
      chartPanel.setLayout(new BorderLayout());
      chartPanel.add(label = createOverlayLabel());

      ((XYPlot) chart.getPlot()).getRenderer().setSeriesPaint(0, EhcachePresentationUtils.MISS_FILL_COLOR);
    }

    public void acceptPolledAttributeResult(PolledAttributesResult result, IClient client, ObjectName on) {
      Number n = (Number) result.getPolledAttribute(client, on, CACHE_EVICTION_SAMPLE_ATTR);
      if (n != null) {
        value += n.longValue();
      }
    }

    public void render() {
      updateSeries(timeSeries, Long.valueOf(value));
      label.setText(MessageFormat.format(labelFormat, value));
      value = 0;
    }

    public void clear() {
      if (timeSeries != null) {
        timeSeries.clear();
      }
    }

    public String getName() {
      return "CacheEvictionRate";
    }
  }

  private class ExpirationRateProvider implements ChartContentProvider {
    private TimeSeries   timeSeries;
    private XLabel       label;
    private final String labelFormat = "{0,number,integer} Expirations/sec.";
    private long         value;

    public void configure(ChartPanel chartPanel) {
      String text = bundle.getString("cache.expiration.rate");
      timeSeries = createTimeSeries(text);
      JFreeChart chart = createChart(timeSeries, false);
      chartPanel.setChart(chart);
      chartPanel.setBorder(new TitledBorder(text));
      chartPanel.setToolTipText(text);
      chartPanel.setLayout(new BorderLayout());
      chartPanel.add(label = createOverlayLabel());

      ((XYPlot) chart.getPlot()).getRenderer().setSeriesPaint(0, EhcachePresentationUtils.MISS_FILL_COLOR);
    }

    public void acceptPolledAttributeResult(PolledAttributesResult result, IClient client, ObjectName on) {
      Number n = (Number) result.getPolledAttribute(client, on, CACHE_EXPIRATION_SAMPLE_ATTR);
      if (n != null) {
        value += n.longValue();
      }
    }

    public void render() {
      updateSeries(timeSeries, Long.valueOf(value));
      label.setText(MessageFormat.format(labelFormat, value));
      value = 0;
    }

    public void clear() {
      if (timeSeries != null) {
        timeSeries.clear();
      }
    }

    public String getName() {
      return "CacheExpirationRate";
    }
  }

  private class InMemoryHitRateProvider implements ChartContentProvider {
    private TimeSeries   timeSeries;
    private XLabel       label;
    private final String labelFormat = "{0,number,integer} InMemory Hits/sec.";
    private long         value;

    public void configure(ChartPanel chartPanel) {
      String text = bundle.getString("cache.in-memory.hit.rate");
      timeSeries = createTimeSeries(text);
      JFreeChart chart = createChart(timeSeries, false);
      chartPanel.setChart(chart);
      chartPanel.setBorder(new TitledBorder(text));
      chartPanel.setToolTipText(text);
      chartPanel.setLayout(new BorderLayout());
      chartPanel.add(label = createOverlayLabel());

      ((XYPlot) chart.getPlot()).getRenderer().setSeriesPaint(0, EhcachePresentationUtils.HIT_FILL_COLOR);
    }

    public void acceptPolledAttributeResult(PolledAttributesResult result, IClient client, ObjectName on) {
      Number n = (Number) result.getPolledAttribute(client, on, CACHE_IN_MEMORY_HIT_SAMPLE_ATTR);
      if (n != null) {
        value += n.longValue();
      }
    }

    public void render() {
      updateSeries(timeSeries, Long.valueOf(value));
      label.setText(MessageFormat.format(labelFormat, value));
      value = 0;
    }

    public void clear() {
      if (timeSeries != null) {
        timeSeries.clear();
      }
    }

    public String getName() {
      return "CacheInMemoryHitRate";
    }
  }

  private class OffHeapHitRateProvider implements ChartContentProvider {
    private TimeSeries   timeSeries;
    private XLabel       label;
    private final String labelFormat = "{0,number,integer} OffHeap Hits/sec.";
    private long         value;

    public void configure(ChartPanel chartPanel) {
      String text = bundle.getString("cache.off-heap.hit.rate");
      timeSeries = createTimeSeries(text);
      JFreeChart chart = createChart(timeSeries, false);
      chartPanel.setChart(chart);
      chartPanel.setBorder(new TitledBorder(text));
      chartPanel.setToolTipText(text);
      chartPanel.setLayout(new BorderLayout());
      chartPanel.add(label = createOverlayLabel());

      ((XYPlot) chart.getPlot()).getRenderer().setSeriesPaint(0, EhcachePresentationUtils.HIT_FILL_COLOR);
    }

    public void acceptPolledAttributeResult(PolledAttributesResult result, IClient client, ObjectName on) {
      Number n = (Number) result.getPolledAttribute(client, on, CACHE_OFF_HEAP_HIT_SAMPLE_ATTR);
      if (n != null) {
        value += n.longValue();
      }
    }

    public void render() {
      updateSeries(timeSeries, Long.valueOf(value));
      label.setText(MessageFormat.format(labelFormat, value));
      value = 0;
    }

    public void clear() {
      if (timeSeries != null) {
        timeSeries.clear();
      }
    }

    public String getName() {
      return "CacheOffHeapHitRate";
    }
  }

  private class OnDiskHitRateProvider implements ChartContentProvider {
    private TimeSeries   timeSeries;
    private XLabel       label;
    private final String labelFormat = "{0,number,integer} OnDisk Hits/sec.";
    private long         value;

    public void configure(ChartPanel chartPanel) {
      String text = bundle.getString("cache.on-disk.hit.rate");
      timeSeries = createTimeSeries(text);
      JFreeChart chart = createChart(timeSeries, false);
      chartPanel.setChart(chart);
      chartPanel.setBorder(new TitledBorder(text));
      chartPanel.setToolTipText(text);
      chartPanel.setLayout(new BorderLayout());
      chartPanel.add(label = createOverlayLabel());

      ((XYPlot) chart.getPlot()).getRenderer().setSeriesPaint(0, EhcachePresentationUtils.HIT_FILL_COLOR);
    }

    public void acceptPolledAttributeResult(PolledAttributesResult result, IClient client, ObjectName on) {
      Number n = (Number) result.getPolledAttribute(client, on, CACHE_ON_DISK_HIT_SAMPLE_ATTR);
      if (n != null) {
        value += n.longValue();
      }
    }

    public void render() {
      updateSeries(timeSeries, Long.valueOf(value));
      label.setText(MessageFormat.format(labelFormat, value));
      value = 0;
    }

    public void clear() {
      if (timeSeries != null) {
        timeSeries.clear();
      }
    }

    public String getName() {
      return "CacheOnDiskHitRate";
    }
  }

  private class InMemoryMissRateProvider implements ChartContentProvider {
    private TimeSeries   timeSeries;
    private XLabel       label;
    private final String labelFormat = "{0,number,integer} InMemory Misses/sec.";
    private long         value;

    public void configure(ChartPanel chartPanel) {
      String text = bundle.getString("cache.in-memory.miss.rate");
      timeSeries = createTimeSeries(text);
      JFreeChart chart = createChart(timeSeries, false);
      chartPanel.setChart(chart);
      chartPanel.setBorder(new TitledBorder(text));
      chartPanel.setToolTipText(text);
      chartPanel.setLayout(new BorderLayout());
      chartPanel.add(label = createOverlayLabel());

      ((XYPlot) chart.getPlot()).getRenderer().setSeriesPaint(0, EhcachePresentationUtils.MISS_FILL_COLOR);
    }

    public void acceptPolledAttributeResult(PolledAttributesResult result, IClient client, ObjectName on) {
      Number n = (Number) result.getPolledAttribute(client, on, CACHE_IN_MEMORY_MISS_SAMPLE_ATTR);
      if (n != null) {
        value += n.longValue();
      }
    }

    public void render() {
      updateSeries(timeSeries, Long.valueOf(value));
      label.setText(MessageFormat.format(labelFormat, value));
      value = 0;
    }

    public void clear() {
      if (timeSeries != null) {
        timeSeries.clear();
      }
    }

    public String getName() {
      return "CacheInMemoryMissRate";
    }
  }

  private class OffHeapMissRateProvider implements ChartContentProvider {
    private TimeSeries   timeSeries;
    private XLabel       label;
    private final String labelFormat = "{0,number,integer} OffHeap Misses/sec.";
    private long         value;

    public void configure(ChartPanel chartPanel) {
      String text = bundle.getString("cache.off-heap.miss.rate");
      timeSeries = createTimeSeries(text);
      JFreeChart chart = createChart(timeSeries, false);
      chartPanel.setChart(chart);
      chartPanel.setBorder(new TitledBorder(text));
      chartPanel.setToolTipText(text);
      chartPanel.setLayout(new BorderLayout());
      chartPanel.add(label = createOverlayLabel());

      ((XYPlot) chart.getPlot()).getRenderer().setSeriesPaint(0, EhcachePresentationUtils.MISS_FILL_COLOR);
    }

    public void acceptPolledAttributeResult(PolledAttributesResult result, IClient client, ObjectName on) {
      Number n = (Number) result.getPolledAttribute(client, on, CACHE_OFF_HEAP_MISS_SAMPLE_ATTR);
      if (n != null) {
        value += n.longValue();
      }
    }

    public void render() {
      updateSeries(timeSeries, Long.valueOf(value));
      label.setText(MessageFormat.format(labelFormat, value));
      value = 0;
    }

    public void clear() {
      if (timeSeries != null) {
        timeSeries.clear();
      }
    }

    public String getName() {
      return "CacheOffHeapMissRate";
    }
  }

  private class OnDiskMissRateProvider implements ChartContentProvider {
    private TimeSeries   timeSeries;
    private XLabel       label;
    private final String labelFormat = "{0,number,integer} OnDisk Misses/sec.";
    private long         value;

    public void configure(ChartPanel chartPanel) {
      String text = bundle.getString("cache.on-disk.miss.rate");
      timeSeries = createTimeSeries(text);
      JFreeChart chart = createChart(timeSeries, false);
      chartPanel.setChart(chart);
      chartPanel.setBorder(new TitledBorder(text));
      chartPanel.setToolTipText(text);
      chartPanel.setLayout(new BorderLayout());
      chartPanel.add(label = createOverlayLabel());

      ((XYPlot) chart.getPlot()).getRenderer().setSeriesPaint(0, EhcachePresentationUtils.MISS_FILL_COLOR);
    }

    public void acceptPolledAttributeResult(PolledAttributesResult result, IClient client, ObjectName on) {
      Number n = (Number) result.getPolledAttribute(client, on, CACHE_ON_DISK_MISS_SAMPLE_ATTR);
      if (n != null) {
        value += n.longValue();
      }
    }

    public void render() {
      updateSeries(timeSeries, Long.valueOf(value));
      label.setText(MessageFormat.format(labelFormat, value));
      value = 0;
    }

    public void clear() {
      if (timeSeries != null) {
        timeSeries.clear();
      }
    }

    public String getName() {
      return "CacheOnDiskMissRate";
    }
  }

  private class AverageGetTimeProvider implements ChartContentProvider {
    private TimeSeries   timeSeries;
    private XLabel       label;
    private final String labelFormat = "Avg. Get Time: {0,number,#0.000} ms.";
    private float        value;
    private int          nodeCount;

    public void configure(ChartPanel chartPanel) {
      String text = "Average Get Time";
      timeSeries = createTimeSeries(text);
      JFreeChart chart = createChart(timeSeries, false);
      chartPanel.setChart(chart);
      chartPanel.setBorder(new TitledBorder(text));
      chartPanel.setToolTipText(text);
      chartPanel.setLayout(new BorderLayout());
      chartPanel.add(label = createOverlayLabel());

      XYPlot plot = (XYPlot) chart.getPlot();
      plot.getRenderer().setSeriesPaint(0, EhcachePresentationUtils.PUT_FILL_COLOR);
      NumberAxis numberAxis = (NumberAxis) plot.getRangeAxis();
      numberAxis.setAutoRangeMinimumSize(0.01d);
    }

    public void acceptPolledAttributeResult(PolledAttributesResult result, IClient client, ObjectName on) {
      Number n = (Number) result.getPolledAttribute(client, on, CACHE_AVERAGE_GET_TIME_ATTR);
      if (n != null) {
        value += n.floatValue();
        nodeCount++;
      }
    }

    public void render() {
      if (nodeCount > 0) {
        value /= nodeCount;
      } else {
        value = 0;
      }
      Float f = Float.valueOf(value);
      updateSeries(timeSeries, f);
      label.setText(MessageFormat.format(labelFormat, f));
      value = 0;
      nodeCount = 0;
    }

    public void clear() {
      if (timeSeries != null) {
        timeSeries.clear();
      }
    }

    public String getName() {
      return "CacheAverageGetTime";
    }
  }

  private class HitMissRateProvider implements ChartContentProvider {
    private TimeSeries   hitRateSeries;
    private TimeSeries   missRateSeries;

    private StatusView   missRateLabel;
    private StatusView   hitRateLabel;

    private final String missRateLabelFormat = "{0,number,integer} Misses/sec.";
    private final String hitRateLabelFormat  = "{0,number,integer} Hits/sec.";

    private long         missRate;
    private long         hitRate;

    public void configure(ChartPanel chartPanel) {
      hitRateSeries = createTimeSeries(bundle.getString("cache.hit.rate"));
      missRateSeries = createTimeSeries(bundle.getString("cache.miss.rate"));
      JFreeChart chart = createChart(new TimeSeries[] { hitRateSeries, missRateSeries }, false);
      chartPanel.setChart(chart);
      chartPanel.setBorder(new TitledBorder("Cache Hit/Miss Rate"));
      chartPanel.setToolTipText("Cache Hit/Miss Rate");
      chartPanel.setLayout(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();
      XContainer labelHolder = new XContainer(new GridLayout(0, 1));
      labelHolder.add(hitRateLabel = createStatusLabel(EhcachePresentationUtils.HIT_DRAW_COLOR));
      labelHolder.add(missRateLabel = createStatusLabel(EhcachePresentationUtils.MISS_DRAW_COLOR));
      labelHolder.setOpaque(false);
      chartPanel.add(labelHolder, gbc);

      ((XYPlot) chart.getPlot()).getRenderer().setSeriesPaint(0, EhcachePresentationUtils.HIT_FILL_COLOR);
      ((XYPlot) chart.getPlot()).getRenderer().setSeriesPaint(1, EhcachePresentationUtils.MISS_FILL_COLOR);
    }

    public void acceptPolledAttributeResult(PolledAttributesResult result, IClient client, ObjectName on) {
      Number n = (Number) result.getPolledAttribute(client, on, CACHE_HIT_SAMPLE_ATTR);
      if (n != null) {
        hitRate += n.longValue();
      }
      n = (Number) result.getPolledAttribute(client, on, CACHE_MISS_SAMPLE_ATTR);
      if (n != null) {
        missRate += n.longValue();
      }
    }

    public void render() {
      updateSeries(missRateSeries, Long.valueOf(missRate));
      missRateLabel.setText(MessageFormat.format(missRateLabelFormat, missRate));

      updateSeries(hitRateSeries, Long.valueOf(hitRate));
      hitRateLabel.setText(MessageFormat.format(hitRateLabelFormat, hitRate));

      missRate = hitRate = 0;
    }

    public void clear() {
      if (hitRateSeries != null) {
        hitRateSeries.clear();
      }
      if (missRateSeries != null) {
        missRateSeries.clear();
      }
    }

    public String getName() {
      return "CacheHitMissRate";
    }
  }

  public interface ChartContentProvider {
    String getName();

    void configure(ChartPanel chartPanel);

    void acceptPolledAttributeResult(PolledAttributesResult result, IClient client, ObjectName on);

    void render();

    void clear();
  }

  private class ChartPanelProviderHandler implements ActionListener {
    private final ChartPanel chartPanel;
    private final String     providerName;

    ChartPanelProviderHandler(ChartPanel chartPanel, String providerName) {
      this.chartPanel = chartPanel;
      this.providerName = providerName;
    }

    public void actionPerformed(ActionEvent e) {
      chartPanel.removeAll();
      ChartContentProvider chartProvider = contentProviderMap.get(providerName);
      chartProvider.configure(chartPanel);
      chartProviders[indexOfChartPanel(chartPanel)] = chartProvider;
      activeProvidersPrefs.put(ACTIVE_PROVIDERS_PREFS_KEY, StringUtils.join(chartProviderNames(), ","));
      chartPanel.revalidate();
      chartPanel.repaint();
    }
  }

  private String[] chartProviderNames() {
    String[] result = new String[CHART_COUNT];
    int i = 0;
    for (ChartContentProvider provider : chartProviders) {
      result[i++] = provider.getName();
    }
    return result;
  }

  private int indexOfChartPanel(ChartPanel chartPanel) {
    return Arrays.asList(chartPanels).indexOf(chartPanel);
  }

  protected ChartPanel setupChartContentProvider(XContainer parent, ChartContentProvider provider) {
    ChartPanel chartPanel = createChartPanel(null);
    provider.configure(chartPanel);
    JPopupMenu popup = chartPanel.getPopupMenu();
    JMenu changeChartMenu = new JMenu("Change chart to...");
    popup.add(changeChartMenu);
    for (String providerName : contentProviderMap.keySet()) {
      JMenuItem subitem = new JMenuItem(providerName);
      changeChartMenu.add(subitem);
      subitem.addActionListener(new ChartPanelProviderHandler(chartPanel, providerName));
    }
    changeChartMenu.addSeparator();
    JMenuItem restoreDefaultsMenu = new JMenuItem("Restore default charts");
    restoreDefaultsMenu.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        activeProvidersPrefs.put(ACTIVE_PROVIDERS_PREFS_KEY, DEFAULT_ACTIVE_PROVIDERS);
      }
    });
    changeChartMenu.add(restoreDefaultsMenu);
    parent.add(chartPanel);
    chartPanel.setPreferredSize(fDefaultGraphSize);
    return chartPanel;
  }

  protected void setupCacheSearchRatePanel(final XContainer parent) {
    String text = bundle.getString("cache.search.rate");
    cacheSearchRateSeries = createTimeSeries(text);
    ChartPanel chartPanel = createChartPanel(createChart(cacheSearchRateSeries, false));
    parent.add(chartPanel);
    chartPanel.setPreferredSize(fDefaultGraphSize);
    chartPanel.setBorder(new TitledBorder(text));
    chartPanel.setToolTipText(text);
    chartPanel.setLayout(new BorderLayout());
    chartPanel.add(cacheSearchRateLabel = createOverlayLabel());
  }

  protected void setupCacheAverageSearchTimePanel(final XContainer parent) {
    String text = bundle.getString("cache.average.search.time");
    cacheAverageSearchTimeSeries = createTimeSeries(text);
    ChartPanel chartPanel = createChartPanel(createChart(cacheAverageSearchTimeSeries, false));
    parent.add(chartPanel);
    chartPanel.setPreferredSize(fDefaultGraphSize);
    chartPanel.setBorder(new TitledBorder(text));
    chartPanel.setToolTipText(text);
    chartPanel.setLayout(new BorderLayout());
    chartPanel.add(cacheAverageSearchTimeLabel = createOverlayLabel());
  }

  protected void setupTransactionCommitRatePanel(final XContainer parent) {
    String text = bundle.getString("transaction.commit.rate");
    transactionCommitRateSeries = createTimeSeries(text);
    ChartPanel chartPanel = createChartPanel(createChart(transactionCommitRateSeries, false));
    parent.add(chartPanel);
    chartPanel.setPreferredSize(fDefaultGraphSize);
    chartPanel.setBorder(new TitledBorder(text));
    chartPanel.setToolTipText(text);
    chartPanel.setLayout(new BorderLayout());
    chartPanel.add(transactionCommitRateLabel = createOverlayLabel());
  }

  protected void setupTransactionRollbackRatePanel(final XContainer parent) {
    String text = bundle.getString("transaction.rollback.rate");
    transactionRollbackRateSeries = createTimeSeries(text);
    ChartPanel chartPanel = createChartPanel(createChart(transactionRollbackRateSeries, false));
    parent.add(chartPanel);
    chartPanel.setPreferredSize(fDefaultGraphSize);
    chartPanel.setBorder(new TitledBorder(text));
    chartPanel.setToolTipText(text);
    chartPanel.setLayout(new BorderLayout());
    chartPanel.add(transactionRollbackRateLabel = createOverlayLabel());
  }

  protected void setupWriteBehindPanel(final XContainer parent) {
    String text = bundle.getString("writer.queue.length");
    writerQueueLengthSeries = createTimeSeries(text);
    writerMaxQueueSizeSeries = createTimeSeries(bundle.getString("writer.max.queue.size"));
    JFreeChart chart = createChart(new TimeSeries[] { writerMaxQueueSizeSeries, writerQueueLengthSeries }, false);
    ChartPanel chartPanel = createChartPanel(chart);
    parent.add(chartPanel);
    chartPanel.setPreferredSize(fDefaultGraphSize);
    chartPanel.setBorder(new TitledBorder(text));
    chartPanel.setToolTipText(text);
    chartPanel.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    XContainer labelHolder = new XContainer(new GridLayout(0, 1));
    labelHolder.add(writerMaxQueueSizeLabel = createStatusLabel(EhcachePresentationUtils.MISS_DRAW_COLOR));
    labelHolder.add(writerQueueLengthLabel = createStatusLabel(EhcachePresentationUtils.PUT_DRAW_COLOR));
    labelHolder.setOpaque(false);
    chartPanel.add(labelHolder, gbc);

    ((XYPlot) chart.getPlot()).getRenderer().setSeriesPaint(0, EhcachePresentationUtils.MISS_FILL_COLOR);
    ((XYPlot) chart.getPlot()).getRenderer().setSeriesPaint(1, EhcachePresentationUtils.PUT_FILL_COLOR);
  }

  protected void updateAllSeries(final long searchRate, final long averageSearchTime, final long txCommitRate,
                                 final long txRollbackRate, final long writerQueueLength, final int writerMaxQueueSize,
                                 final boolean hasAnyWriters, final boolean isTransactional, final boolean isSearchable) {
    if (!isMonitoringRuntimeStats()) { return; }

    tmpDate.setTime(System.currentTimeMillis());

    for (ChartContentProvider chartProvider : chartProviders) {
      chartProvider.render();
    }

    if (cacheSearchRateSeries != null) {
      updateSeries(cacheSearchRateSeries, Long.valueOf(searchRate));
      cacheSearchRateLabel.setText(MessageFormat.format(cacheSearchRateLabelFormat, searchRate));
    }
    if (cacheAverageSearchTimeSeries != null) {
      updateSeries(cacheAverageSearchTimeSeries, Long.valueOf(averageSearchTime));
      cacheAverageSearchTimeLabel.setText(MessageFormat.format(cacheAverageSearchTimeLabelFormat, averageSearchTime));
    }
    if (transactionCommitRateSeries != null) {
      updateSeries(transactionCommitRateSeries, Long.valueOf(txCommitRate));
      transactionCommitRateLabel.setText(MessageFormat.format(transactionCommitRateLabelFormat, txCommitRate));
    }
    if (transactionRollbackRateSeries != null) {
      updateSeries(transactionRollbackRateSeries, Long.valueOf(txRollbackRate));
      transactionRollbackRateLabel.setText(MessageFormat.format(transactionRollbackRateLabelFormat, txRollbackRate));
    }
    if (writerQueueLengthSeries != null) {
      updateSeries(writerQueueLengthSeries, Long.valueOf(writerQueueLength));
      writerQueueLengthLabel.setText(MessageFormat.format(writerQueueLengthLabelFormat, writerQueueLength));
    }
    if (writerMaxQueueSizeSeries != null) {
      updateSeries(writerMaxQueueSizeSeries, Long.valueOf(writerMaxQueueSize));
      writerMaxQueueSizeLabel.setText(MessageFormat.format(writerMaxQueueSizeLabelFormat, writerMaxQueueSize));
    }

    int selectedTab = tabbedPane.getSelectedIndex();

    boolean isSearchTabEnabled = tabbedPane.isEnabledAt(searchTabIndex);
    if (isSearchTabEnabled != isSearchable) {
      tabbedPane.setEnabledAt(searchTabIndex, isSearchable);
      if (selectedTab == searchTabIndex && !isSearchable) {
        tabbedPane.setSelectedIndex(0);
      }
    }

    boolean isJTATabEnabled = tabbedPane.isEnabledAt(jtaTabIndex);
    if (isJTATabEnabled != isTransactional) {
      tabbedPane.setEnabledAt(jtaTabIndex, isTransactional);
      if (selectedTab == jtaTabIndex && !isTransactional) {
        tabbedPane.setSelectedIndex(0);
      }
    }

    boolean isWriteBehindTabEnabled = tabbedPane.isEnabledAt(writeBehindTabIndex);
    if (isWriteBehindTabEnabled != hasAnyWriters) {
      tabbedPane.setEnabledAt(writeBehindTabIndex, hasAnyWriters);
      if (selectedTab == writeBehindTabIndex && !hasAnyWriters) {
        tabbedPane.setSelectedIndex(0);
      }
    }
  }

  private void clearAllTimeSeries() {
    for (Entry<String, ChartContentProvider> entry : contentProviderMap.entrySet()) {
      entry.getValue().clear();
    }

    if (cacheSearchRateSeries != null) {
      cacheSearchRateSeries.clear();
    }
    if (cacheAverageSearchTimeSeries != null) {
      cacheAverageSearchTimeSeries.clear();
    }
    if (transactionCommitRateSeries != null) {
      transactionCommitRateSeries.clear();
    }
    if (transactionRollbackRateSeries != null) {
      transactionRollbackRateSeries.clear();
    }
    if (writerQueueLengthSeries != null) {
      writerQueueLengthSeries.clear();
    }
    if (writerMaxQueueSizeSeries != null) {
      writerMaxQueueSizeSeries.clear();
    }
  }

  @Override
  public void tearDown() {
    removePolledAttributeListener();
    IServer activeCoord = clusterModel.getActiveCoordinator();
    if (activeCoord != null) {
      activeCoord.removeClientConnectionListener(this);
    }
    clusterModel.removePropertyChangeListener(clusterListener);
    clusterListener.tearDown();
    clearAllTimeSeries();

    super.tearDown();
  }
}

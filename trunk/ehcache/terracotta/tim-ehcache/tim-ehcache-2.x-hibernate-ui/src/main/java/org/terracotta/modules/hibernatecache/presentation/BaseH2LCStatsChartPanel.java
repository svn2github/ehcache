/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.hibernatecache.presentation;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.TimeSeries;
import org.terracotta.modules.hibernatecache.jmx.HibernateStatsUtils;

import com.tc.admin.AbstractClusterListener;
import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.StatusView;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.dso.BaseRuntimeStatsPanel;
import com.tc.admin.model.ClientConnectionListener;
import com.tc.admin.model.IClient;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ResourceBundle;

import javax.management.ObjectName;
import javax.swing.border.TitledBorder;

public class BaseH2LCStatsChartPanel extends BaseRuntimeStatsPanel implements ClientConnectionListener {
  protected final IClusterModel       clusterModel;
  protected final ClusterListener     clusterListener;
  protected final String              persistenceUnit;
  protected final ObjectName          statsBeanObjectName;

  protected TimeSeries                queryExecutionRateSeries;
  protected TimeSeries                cacheHitRateSeries;
  protected TimeSeries                cacheHitRatioSeries;
  protected TimeSeries                cachePutRateSeries;
  protected TimeSeries                cacheMissRateSeries;

  protected XLabel                    queryExecutionRateLabel;
  protected XLabel                    cacheHitRatioLabel;
  protected XLabel                    cachePutRateLabel;
  protected StatusView                cacheMissRateLabel;
  protected StatusView                cacheHitRateLabel;

  protected final String              queryExecutionRateLabelFormat = "{0,number,integer} Queries/sec.";
  protected final String              cacheHitRatioLabelFormat      = "{0,number,##}% Hit Ratio";
  protected final String              cachePutRateLabelFormat       = "{0,number,integer} Puts/sec.";
  protected final String              cacheMissRateLabelFormat      = "{0,number,integer} Misses/sec.";
  protected final String              cacheHitRateLabelFormat       = "{0,number,integer} Hits/sec.";

  private static final boolean        showCacheHitRatioLabel        = true;

  protected static final String       QUERY_EXECUTION_SAMPLE_ATTR   = "QueryExecutionSample";
  protected static final String       DBSQL_EXECUTION_SAMPLE_ATTR   = "DBSQLExecutionSample";
  protected static final String       CACHE_HIT_SAMPLE_ATTR         = "CacheHitSample";
  protected static final String       CACHE_PUT_SAMPLE_ATTR         = "CachePutSample";
  protected static final String       CACHE_MISS_SAMPLE_ATTR        = "CacheMissSample";

  protected static final String[]     POLLED_ATTRS                  = { DBSQL_EXECUTION_SAMPLE_ATTR,
      CACHE_HIT_SAMPLE_ATTR, CACHE_PUT_SAMPLE_ATTR, CACHE_MISS_SAMPLE_ATTR };

  private static final ResourceBundle bundle                        = ResourceBundle
                                                                        .getBundle(HibernateResourceBundle.class
                                                                            .getName());

  protected BaseH2LCStatsChartPanel(ApplicationContext appContext, IClusterModel clusterModel, String persistenceUnit) {
    super(appContext);
    this.clusterModel = clusterModel;
    this.clusterListener = new ClusterListener(clusterModel);
    this.persistenceUnit = persistenceUnit;

    try {
      statsBeanObjectName = HibernateStatsUtils.getHibernateStatsBeanName(persistenceUnit);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    setup(chartsPanel);
  }

  public void setup() {
    clusterModel.addPropertyChangeListener(clusterListener);
    if (clusterModel.isReady()) {
      init();
    }
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
        oldActive.removeClientConnectionListener(BaseH2LCStatsChartPanel.this);
      }
      if (newActive != null) {
        newActive.addClientConnectionListener(BaseH2LCStatsChartPanel.this);
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

  @Override
  protected synchronized void setup(final XContainer chartsPanel) {
    chartsPanel.setLayout(new GridLayout(2, 2));
    setupCacheHitRatioPanel(chartsPanel);
    setupCacheMissRatePanel(chartsPanel);
    setupQueryExecutionRatePanel(chartsPanel);
    setupCachePutRatePanel(chartsPanel);
  }

  protected void setupQueryExecutionRatePanel(final XContainer parent) {
    String text = bundle.getString("sql.execution.rate");
    queryExecutionRateSeries = createTimeSeries(text);
    ChartPanel chartPanel = createChartPanel(createChart(queryExecutionRateSeries, false));
    parent.add(chartPanel);
    chartPanel.setPreferredSize(fDefaultGraphSize);
    chartPanel.setBorder(new TitledBorder(text));
    chartPanel.setToolTipText(text);
    chartPanel.setLayout(new BorderLayout());
    chartPanel.add(queryExecutionRateLabel = createOverlayLabel());
  }

  protected void setupCacheHitRatioPanel(final XContainer parent) {
    cacheHitRatioSeries = createTimeSeries(bundle.getString("cache.hit.ratio"));
    JFreeChart chart = createXYStepChart(cacheHitRatioSeries);
    ChartPanel chartPanel = createChartPanel(chart);
    parent.add(chartPanel);
    chartPanel.setPreferredSize(fDefaultGraphSize);
    chartPanel.setBorder(new TitledBorder("Cache Hit Ratio"));
    chartPanel.setToolTipText("Second Level Cache Hit Ratio");
    chartPanel.setLayout(new BorderLayout());
    chartPanel.add(cacheHitRatioLabel = createOverlayLabel());

    XYPlot plot = (XYPlot) chart.getPlot();
    ((NumberAxis) plot.getRangeAxis()).setRange(0.0, 105.0);

    plot.getRenderer().setSeriesPaint(0, CacheRegionUtils.HIT_FILL_COLOR);
  }

  protected void setupCachePutRatePanel(final XContainer parent) {
    cachePutRateSeries = createTimeSeries(bundle.getString("cache.put.rate"));
    JFreeChart chart = createChart(cachePutRateSeries, false);
    ChartPanel chartPanel = createChartPanel(chart);
    parent.add(chartPanel);
    chartPanel.setPreferredSize(fDefaultGraphSize);
    chartPanel.setBorder(new TitledBorder("Cache Put Rate"));
    chartPanel.setToolTipText("Second Level Cache Put Rate");
    chartPanel.setLayout(new BorderLayout());
    chartPanel.add(cachePutRateLabel = createOverlayLabel());

    ((XYPlot) chart.getPlot()).getRenderer().setSeriesPaint(0, CacheRegionUtils.PUT_FILL_COLOR);
  }

  protected void setupCacheMissRatePanel(final XContainer parent) {
    cacheHitRateSeries = createTimeSeries(bundle.getString("cache.hit.rate"));
    cacheMissRateSeries = createTimeSeries(bundle.getString("cache.miss.rate"));
    JFreeChart chart = createChart(new TimeSeries[] { cacheHitRateSeries, cacheMissRateSeries }, false);
    ChartPanel chartPanel = createChartPanel(chart);
    parent.add(chartPanel);
    chartPanel.setPreferredSize(fDefaultGraphSize);
    chartPanel.setBorder(new TitledBorder("Cache Hit/Miss Rate"));
    chartPanel.setToolTipText("Second Level Cache Hit/Miss Rate");
    chartPanel.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    XContainer labelHolder = new XContainer(new GridLayout(0, 1));
    labelHolder.add(cacheHitRateLabel = createStatusLabel(CacheRegionUtils.HIT_DRAW_COLOR));
    labelHolder.add(cacheMissRateLabel = createStatusLabel(CacheRegionUtils.MISS_DRAW_COLOR));
    labelHolder.setOpaque(false);
    chartPanel.add(labelHolder, gbc);

    ((XYPlot) chart.getPlot()).getRenderer().setSeriesPaint(0, CacheRegionUtils.HIT_FILL_COLOR);
    ((XYPlot) chart.getPlot()).getRenderer().setSeriesPaint(1, CacheRegionUtils.MISS_FILL_COLOR);
  }

  protected void updateAllSeries(final long executionRate, final long hitRate, final long putRate, final long missRate) {
    tmpDate.setTime(System.currentTimeMillis());

    if (queryExecutionRateSeries != null) {
      updateSeries(queryExecutionRateSeries, Long.valueOf(executionRate));
      queryExecutionRateLabel.setText(MessageFormat.format(queryExecutionRateLabelFormat, executionRate));
    }
    if (cacheHitRatioSeries != null) {
      long readRate = hitRate + missRate;
      double cacheHitRatio = 0;
      if (readRate > 0) {
        cacheHitRatio = (hitRate / ((double) readRate)) * 100;
      }
      updateSeries(cacheHitRatioSeries, Double.valueOf(cacheHitRatio));
      if (showCacheHitRatioLabel) {
        cacheHitRatioLabel.setText(MessageFormat.format(cacheHitRatioLabelFormat, cacheHitRatio));
      }
    }
    if (cachePutRateSeries != null) {
      updateSeries(cachePutRateSeries, Long.valueOf(putRate));
      cachePutRateLabel.setText(MessageFormat.format(cachePutRateLabelFormat, putRate));
    }
    if (cacheMissRateSeries != null) {
      updateSeries(cacheHitRateSeries, Long.valueOf(hitRate));
      updateSeries(cacheMissRateSeries, Long.valueOf(missRate));
      cacheMissRateLabel.setText(MessageFormat.format(cacheMissRateLabelFormat, missRate));
    }
    if (cacheHitRateSeries != null) {
      updateSeries(cacheHitRateSeries, Long.valueOf(hitRate));
      cacheHitRateLabel.setText(MessageFormat.format(cacheHitRateLabelFormat, hitRate));
    }
  }

  private void clearAllTimeSeries() {
    ArrayList<TimeSeries> list = new ArrayList<TimeSeries>();

    if (queryExecutionRateSeries != null) {
      list.add(queryExecutionRateSeries);
      queryExecutionRateSeries = null;
    }
    if (cacheHitRateSeries != null) {
      list.add(cacheHitRateSeries);
      cacheHitRateSeries = null;
    }
    if (cacheHitRatioSeries != null) {
      list.add(cacheHitRatioSeries);
      cacheHitRatioSeries = null;
    }
    if (cachePutRateSeries != null) {
      list.add(cachePutRateSeries);
      cachePutRateSeries = null;
    }
    if (cacheMissRateSeries != null) {
      list.add(cacheMissRateSeries);
      cacheMissRateSeries = null;
    }

    Iterator<TimeSeries> iter = list.iterator();
    while (iter.hasNext()) {
      iter.next().clear();
    }
  }

  @Override
  public void tearDown() {
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

/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.ehcache.presentation;

import org.terracotta.modules.ehcache.presentation.model.CacheManagerInstance;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerInstanceListener;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerModel;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerModelListener;
import org.terracotta.modules.ehcache.presentation.model.CacheModel;
import org.terracotta.modules.ehcache.presentation.model.CacheModelInstance;
import org.terracotta.modules.ehcache.presentation.model.ClusteredCacheModel;
import org.terracotta.modules.ehcache.presentation.model.StandaloneCacheModel;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.XButton;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.model.IClient;
import com.tc.admin.model.PolledAttributesResult;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.Iterator;

import javax.management.ObjectName;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class AggregateEhcacheStatsChartPanel extends BaseEhcacheStatsChartPanel implements CacheManagerModelListener,
    CacheManagerInstanceListener {
  private XLabel  summaryLabel;
  private XButton manageStatsButton;

  public AggregateEhcacheStatsChartPanel(ApplicationContext appContext, CacheManagerModel cacheManagerModel) {
    super(appContext, cacheManagerModel);
    setupSummaryPanel();
  }

  @Override
  public void setup() {
    super.setup();
    cacheManagerModel.addCacheManagerModelListener(this);
  }

  @Override
  protected void init() {
    super.init();
    for (Iterator<CacheManagerInstance> iter = cacheManagerModel.cacheManagerInstanceIterator(); iter.hasNext();) {
      iter.next().addCacheManagerInstanceListener(this);
    }
    handleSummaryText();
  }

  private void setupSummaryPanel() {
    XContainer panel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.gridx = gbc.gridy = 0;

    panel.add(summaryLabel = new XLabel(), gbc);
    summaryLabel.setIcon(EhcachePresentationUtils.ALERT_ICON);
    gbc.gridx++;

    panel.add(manageStatsButton = new XButton("Manage Statistics..."), gbc);
    manageStatsButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        Component c = AggregateEhcacheStatsChartPanel.this;
        Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, c);
        ManageMessage msg = new ManageStatisticsMessage(frame, appContext, cacheManagerModel);
        int result = JOptionPane.showConfirmDialog(c, msg, msg.getTitle(), JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
          Boolean toNewcomers = null;
          if (msg.hasApplyToNewcomersToggle()) {
            toNewcomers = Boolean.valueOf(msg.shouldApplyToNewcomers());
          }
          msg.apply(toNewcomers);
        }
        msg.tearDown();
      }
    });
    gbc.gridx++;

    // filler
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;

    panel.add(new XLabel(), gbc);

    add(panel, BorderLayout.NORTH);
  }

  private void handleSummaryText() {
    int statsEnabledCount = cacheManagerModel.getStatisticsEnabledCount();
    int cacheModelInstanceCount = cacheManagerModel.getCacheModelInstanceCount();
    String text = null;
    Icon icon = null;
    if (cacheModelInstanceCount > 0 && statsEnabledCount == 0) {
      text = "No cache instances have statistics enabled.";
      icon = EhcachePresentationUtils.ALERT_ICON;
    } else if (statsEnabledCount < cacheModelInstanceCount) {
      text = MessageFormat.format("Statistics enabled on {0} of {1} cache instances.", statsEnabledCount,
                                  cacheModelInstanceCount);
      icon = EhcachePresentationUtils.WARN_ICON;
    }
    summaryLabel.setText(text);
    summaryLabel.setIcon(icon);
    summaryLabel.setVisible(text != null);
    manageStatsButton.setVisible(text != null);
  }

  private void handleSummaryTextLater() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        handleSummaryText();
      }
    });
  }

  @Override
  public void attributesPolled(PolledAttributesResult result) {
    long searchCount = 0;
    long averageSearchTime = 0;
    long txCommitCount = 0;
    long txRollbackCount = 0;
    long writerQueueLength = 0;
    int writerMaxQueueSize = 0;
    boolean hasAnyWriters = false;
    boolean transactional = false;
    boolean searchable = false;

    for (IClient client : clusterModel.getClients()) {
      ObjectName on = client.getTunneledBeanName(statsBeanObjectName);
      Number n;
      Boolean b;

      for (ChartContentProvider chartProvider : chartProviders) {
        chartProvider.acceptPolledAttributeResult(result, client, on);
      }

      n = (Number) result.getPolledAttribute(client, on, CACHE_SEARCH_RATE_ATTR);
      if (n != null) {
        searchCount += n.longValue();
      }

      n = (Number) result.getPolledAttribute(client, on, CACHE_AVERAGE_SEARCH_TIME_ATTR);
      if (n != null) {
        averageSearchTime += n.longValue();
      }

      n = (Number) result.getPolledAttribute(client, on, TRANSACTION_COMMIT_RATE_ATTR);
      if (n != null) {
        txCommitCount += n.longValue();
      }

      n = (Number) result.getPolledAttribute(client, on, TRANSACTION_ROLLBACK_RATE_ATTR);
      if (n != null) {
        txRollbackCount += n.longValue();
      }

      n = (Number) result.getPolledAttribute(client, on, WRITER_QUEUE_LENGTH_ATTR);
      if (n != null) {
        writerQueueLength += n.longValue();
      }

      n = (Number) result.getPolledAttribute(client, on, WRITER_MAX_QUEUE_SIZE_ATTR);
      if (n != null) {
        writerMaxQueueSize += n.intValue();
      }

      b = (Boolean) result.getPolledAttribute(client, on, HAS_WRITE_BEHIND_WRITER_ATTR);
      if (b != null && b.booleanValue()) {
        hasAnyWriters = true;
      }

      b = (Boolean) result.getPolledAttribute(client, on, TRANSACTIONAL_ATTR);
      if (b != null && b.booleanValue()) {
        transactional = true;
      }

      b = (Boolean) result.getPolledAttribute(client, on, SEARCHABLE_ATTR);
      if (b != null && b.booleanValue()) {
        searchable = b.booleanValue();
      }
    }

    final long theSearchCount = searchCount;
    final long theAverageSearchTime = averageSearchTime;
    final long theTxCommitCount = txCommitCount;
    final long theTxRollbackCount = txRollbackCount;
    final long theWriterQueueLength = writerQueueLength;
    final int theWriterMaxQueueSize = writerMaxQueueSize;
    final boolean theHasAnyWriters = hasAnyWriters;
    final boolean theTransactional = transactional;
    final boolean theSearchable = searchable;
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        updateAllSeries(theSearchCount, theAverageSearchTime, theTxCommitCount, theTxRollbackCount,
                        theWriterQueueLength, theWriterMaxQueueSize, theHasAnyWriters, theTransactional, theSearchable);
      }
    });
  }

  @Override
  public void clientConnected(final IClient client) {
    registerClientPolledAttributes(client);
  }

  private void registerClientPolledAttributes(IClient client) {
    ObjectName on = client.getTunneledBeanName(statsBeanObjectName);
    client.addPolledAttributeListener(on, POLLED_ATTRS_SET, this);
  }

  private void deregisterClientPolledAttributes(IClient client) {
    ObjectName on = client.getTunneledBeanName(statsBeanObjectName);
    client.removePolledAttributeListener(on, POLLED_ATTRS_SET, this);
  }

  @Override
  public void clientDisconnected(final IClient client) {
    deregisterClientPolledAttributes(client);
  }

  @Override
  protected void addPolledAttributeListener() {
    for (IClient client : clusterModel.getClients()) {
      registerClientPolledAttributes(client);
    }
  }

  @Override
  protected void removePolledAttributeListener() {
    for (IClient client : clusterModel.getClients()) {
      deregisterClientPolledAttributes(client);
    }
  }

  public void cacheManagerInstanceChanged(CacheManagerInstance cacheManagerInstance) {
    /**/
  }

  public void cacheModelInstanceAdded(CacheModelInstance cacheModelInstance) {
    handleSummaryTextLater();
  }

  public void cacheModelInstanceChanged(CacheModelInstance cacheModelInstance) {
    handleSummaryTextLater();
  }

  public void cacheModelInstanceRemoved(CacheModelInstance cacheModelInstance) {
    handleSummaryTextLater();
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

  public void instanceAdded(CacheManagerInstance instance) {
    instance.addCacheManagerInstanceListener(this);
    handleSummaryText();
  }

  public void instanceRemoved(CacheManagerInstance instance) {
    instance.removeCacheManagerInstanceListener(this);
    handleSummaryText();
  }

  @Override
  public void tearDown() {
    cacheManagerModel.removeCacheManagerModelListener(this);
    for (Iterator<CacheManagerInstance> iter = cacheManagerModel.cacheManagerInstanceIterator(); iter.hasNext();) {
      iter.next().removeCacheManagerInstanceListener(this);
    }
    super.tearDown();
  }
}

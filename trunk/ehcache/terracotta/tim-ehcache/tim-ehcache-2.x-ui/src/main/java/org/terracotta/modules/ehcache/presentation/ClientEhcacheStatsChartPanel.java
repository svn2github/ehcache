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

import javax.management.ObjectName;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class ClientEhcacheStatsChartPanel extends BaseEhcacheStatsChartPanel implements CacheManagerInstanceListener,
    CacheManagerModelListener {
  protected final IClient        client;
  protected CacheManagerInstance cacheManagerInstance;

  protected XLabel               summaryLabel;
  protected XButton              manageStatsButton;

  public ClientEhcacheStatsChartPanel(ApplicationContext appContext, CacheManagerModel cacheManagerModel, IClient client) {
    super(appContext, cacheManagerModel);
    this.client = client;
    setInstance(cacheManagerModel.getInstance(client));
    setupSummaryPanel();
  }

  private synchronized CacheManagerInstance getInstance() {
    return cacheManagerInstance;
  }

  private void setInstance(CacheManagerInstance cmi) {
    CacheManagerInstance oldInstance = null;

    synchronized (this) {
      oldInstance = cacheManagerInstance;
      cacheManagerInstance = cmi;
    }

    if (oldInstance != null) {
      oldInstance.removeCacheManagerInstanceListener(this);
    }
    if (cmi != null) {
      cmi.addCacheManagerInstanceListener(this);
    }
  }

  @Override
  public void setup() {
    super.setup();
    cacheManagerModel.addCacheManagerModelListener(this);
  }

  private void setupSummaryPanel() {
    XContainer panel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.gridx = gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;

    panel.add(summaryLabel = new XLabel(), gbc);
    summaryLabel.setIcon(EhcachePresentationUtils.ALERT_ICON);
    gbc.gridx++;

    panel.add(manageStatsButton = new XButton("Manage Statistics..."), gbc);
    manageStatsButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        Component c = ClientEhcacheStatsChartPanel.this;
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

  @Override
  protected void init() {
    super.init();
    updateSummary();
  }

  protected void updateSummary() {
    CacheManagerInstance cmi = getInstance();
    if (cmi != null) {
      String text = null;
      Icon icon = null;
      int statsEnabledCount = cmi.getStatisticsEnabledCount();
      int instanceCount = cmi.getInstanceCount();
      if (instanceCount > 0 && statsEnabledCount == 0) {
        text = "No cache instances on this node have statistics enabled.";
        icon = EhcachePresentationUtils.ALERT_ICON;
      } else if (statsEnabledCount < instanceCount) {
        text = MessageFormat.format("Statistics enabled on {0} of {1} cache instances.", statsEnabledCount,
                                    instanceCount);
        icon = EhcachePresentationUtils.WARN_ICON;
      }
      summaryLabel.setText(text);
      summaryLabel.setIcon(icon);
      summaryLabel.setVisible(text != null);
      manageStatsButton.setVisible(text != null);
    } else {
      summaryLabel.setText(bundle.getString("cache-manager.not.resident.on.client"));
      summaryLabel.setIcon(EhcachePresentationUtils.ALERT_ICON);
    }
  }

  private void updateSummaryLater() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        updateSummary();
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

    ObjectName on = client.getTunneledBeanName(statsBeanObjectName);

    for (ChartContentProvider chartProvider : chartProviders) {
      chartProvider.acceptPolledAttributeResult(result, client, on);
    }

    Number n = (Number) result.getPolledAttribute(client, on, CACHE_SEARCH_RATE_ATTR);
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

    Boolean b = (Boolean) result.getPolledAttribute(client, on, HAS_WRITE_BEHIND_WRITER_ATTR);
    if (b != null) {
      hasAnyWriters = b.booleanValue();
    }

    b = (Boolean) result.getPolledAttribute(client, on, TRANSACTIONAL_ATTR);
    if (b != null) {
      transactional = b.booleanValue();
    }

    b = (Boolean) result.getPolledAttribute(client, on, SEARCHABLE_ATTR);
    if (b != null) {
      searchable = b.booleanValue();
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
  protected void addPolledAttributeListener() {
    ObjectName on = client.getTunneledBeanName(statsBeanObjectName);
    client.addPolledAttributeListener(on, POLLED_ATTRS_SET, this);
  }

  @Override
  protected void removePolledAttributeListener() {
    ObjectName on = client.getTunneledBeanName(statsBeanObjectName);
    client.removePolledAttributeListener(on, POLLED_ATTRS_SET, this);
  }

  public void cacheModelAdded(CacheModel cacheModel) {
    /**/
  }

  public void cacheModelChanged(CacheModel cacheModel) {
    /**/
  }

  public void cacheModelRemoved(CacheModel cacheModel) {
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
    CacheManagerInstance cmi = getInstance();

    if (cmi == null) {
      setInstance(cacheManagerModel.getInstance(client));
      updateSummaryLater();
    }
  }

  public void instanceRemoved(CacheManagerInstance instance) {
    CacheManagerInstance cmi = getInstance();
    if (cmi == instance) {
      setInstance(null);
    }
    updateSummaryLater();
  }

  public void cacheManagerInstanceChanged(CacheManagerInstance theCacheManagerInstance) {
    /**/
  }

  public void cacheModelInstanceAdded(CacheModelInstance cacheModelInstance) {
    updateSummaryLater();
  }

  public void cacheModelInstanceChanged(CacheModelInstance cacheModelInstance) {
    updateSummaryLater();
  }

  public void cacheModelInstanceRemoved(CacheModelInstance cacheModelInstance) {
    updateSummaryLater();
  }

  @Override
  public void tearDown() {
    cacheManagerModel.removeCacheManagerModelListener(this);
    setInstance(null);
    super.tearDown();
  }
}

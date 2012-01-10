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
import com.tc.admin.common.StatusView;
import com.tc.admin.common.XButton;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;

import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.text.MessageFormat;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class EhcachePerformancePanel extends BaseClusterModelPanel implements CacheManagerModelListener,
    CacheManagerInstanceListener, HierarchyListener {
  private final CacheManagerModel      cacheManagerModel;

  private XLabel                       summaryLabel;
  private final ManageStatisticsAction manageStatsAction;
  private XButton                      manageStatsButton;
  private final QueryForStatsMessage   queryForStatsMessage;
  private final AggregateVuMeterPanel  vuMeterGlobalPanel;
  private final AggregateVuMeterPanel  vuMeterPerCachePanel;

  public EhcachePerformancePanel(ApplicationContext appContext, CacheManagerModel cacheManagerModel) {
    super(appContext, cacheManagerModel.getClusterModel());

    this.cacheManagerModel = cacheManagerModel;

    vuMeterGlobalPanel = new AggregateVuMeterPanel(appContext, cacheManagerModel, true);
    vuMeterGlobalPanel.setBorder(BorderFactory.createTitledBorder(bundle.getString("global.cache.performance")));

    vuMeterPerCachePanel = new AggregateVuMeterPanel(appContext, cacheManagerModel, false);
    vuMeterPerCachePanel.setBorder(BorderFactory.createTitledBorder(bundle.getString("per-cache.performance")));

    manageStatsAction = new ManageStatisticsAction();
    addHierarchyListener(this);
    queryForStatsMessage = new QueryForStatsMessage(manageStatsAction);
  }

  @Override
  public void setup() {
    this.cacheManagerModel.addCacheManagerModelListener(this);

    super.setup();

    revalidate();
    repaint();
  }

  @Override
  protected void init() {
    vuMeterGlobalPanel.setup();
    vuMeterPerCachePanel.setup();

    for (Iterator<CacheManagerInstance> iter = cacheManagerModel.cacheManagerInstanceIterator(); iter.hasNext();) {
      iter.next().addCacheManagerInstanceListener(this);
    }
    handleSummaryText();
  }

  @Override
  protected XContainer createMainPanel() {
    XContainer panel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.gridx = gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;

    panel.add(createSummaryPanel(), gbc);
    gbc.gridy++;

    panel.add(createLegend(), gbc);
    gbc.gridy++;

    gbc.insets = new Insets(1, 1, 1, 1);
    panel.add(vuMeterGlobalPanel, gbc);
    gbc.gridy++;

    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    panel.add(vuMeterPerCachePanel, gbc);

    return panel;
  }

  private XContainer createSummaryPanel() {
    XContainer panel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.gridx = gbc.gridy = 0;

    panel.add(summaryLabel = new XLabel(), gbc);
    summaryLabel.setIcon(EhcachePresentationUtils.ALERT_ICON);
    gbc.gridx++;

    panel.add(manageStatsButton = new XButton("Manage Statistics..."), gbc);
    manageStatsButton.addActionListener(manageStatsAction);
    gbc.gridx++;

    // filler
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;

    panel.add(new XLabel(), gbc);

    return panel;
  }

  private XContainer createLegend() {
    XContainer panel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(0, 1, 0, 1);
    gbc.gridx = gbc.gridy = 0;

    XLabel label;

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    panel.add(label = new XLabel(bundle.getString("current.value")), gbc);
    label.setHorizontalAlignment(SwingConstants.LEFT);
    gbc.gridx++;

    StatusView sv;

    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    panel.add(sv = new StatusView(), gbc);
    sv.setIndicator(EhcachePresentationUtils.HIT_FILL_COLOR);
    sv.setText(bundle.getString("hits"));
    gbc.gridx++;

    panel.add(sv = new StatusView(), gbc);
    sv.setIndicator(EhcachePresentationUtils.MISS_FILL_COLOR);
    sv.setText(bundle.getString("misses"));
    gbc.gridx++;

    panel.add(sv = new StatusView(), gbc);
    sv.setIndicator(EhcachePresentationUtils.PUT_FILL_COLOR);
    sv.setText(bundle.getString("puts"));
    gbc.gridx++;

    gbc.anchor = GridBagConstraints.EAST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    panel.add(label = new XLabel(bundle.getString("local.maximum.value")), gbc);
    label.setHorizontalAlignment(SwingConstants.RIGHT);

    return panel;
  }

  private void handleSummaryText() {
    String text = null;
    Icon icon = null;
    int statsEnabledCount = cacheManagerModel.getStatisticsEnabledCount();
    int instanceCount = cacheManagerModel.getInstanceCount();
    if (instanceCount > 0 && statsEnabledCount == 0) {
      text = "No cache instances have statistics enabled.";
      icon = EhcachePresentationUtils.ALERT_ICON;
    } else if (statsEnabledCount < instanceCount) {
      text = MessageFormat
          .format("Statistics enabled on {0} of {1} cache instances.", statsEnabledCount, instanceCount);
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

  public void instanceAdded(CacheManagerInstance instance) {
    instance.addCacheManagerInstanceListener(this);
    handleSummaryTextLater();
  }

  public void instanceRemoved(CacheManagerInstance instance) {
    instance.removeCacheManagerInstanceListener(this);
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

  private class ManageStatisticsAction extends AbstractAction {
    public void actionPerformed(ActionEvent ae) {
      Component c = EhcachePerformancePanel.this;
      Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, c);
      ManageMessage msg = new ManageStatisticsMessage(frame, appContext, cacheManagerModel);
      int result = JOptionPane.showConfirmDialog(c, msg, msg.getTitle(), JOptionPane.OK_CANCEL_OPTION);
      if (result == JOptionPane.OK_OPTION) {
        Boolean toNewcomers = null;
        if (msg.hasApplyToNewcomersToggle()) {
          toNewcomers = Boolean.valueOf(msg.shouldApplyToNewcomers());
        }
        msg.apply(toNewcomers);
        testDismissQueryForStatsMessage();
      }
      msg.tearDown();
    }
  }

  private void testDismissQueryForStatsMessage() {
    if (queryForStatsMessage.isShowing()) {
      JDialog queryForStatsDialog = (JDialog) SwingUtilities.getAncestorOfClass(JDialog.class, queryForStatsMessage);
      if (queryForStatsDialog != null) {
        queryForStatsDialog.setVisible(false);
      }
    }
  }

  public void hierarchyChanged(HierarchyEvent e) {
    if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
      if (isShowing()) {
        if (cacheManagerModel.getStatisticsEnabledCount() < cacheManagerModel.getCacheModelInstanceCount()
            && queryForStatsMessage.shouldShowAgain()) {
          Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
          int answer = JOptionPane.showConfirmDialog(this, queryForStatsMessage, frame.getTitle(),
                                                     JOptionPane.YES_NO_OPTION);
          if (answer == JOptionPane.YES_OPTION) {
            cacheManagerModel.setStatisticsEnabled(true, true);
          }
          if (!queryForStatsMessage.shouldShowAgain()) {
            removeHierarchyListener(EhcachePerformancePanel.this);
          }
        }
      }
    }
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

/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.ehcache.presentation;

import static org.terracotta.modules.ehcache.presentation.EhcachePresentationUtils.BULK_LOAD_DISABLED_ICON;
import static org.terracotta.modules.ehcache.presentation.EhcachePresentationUtils.BULK_LOAD_ENABLED_ICON;
import static org.terracotta.modules.ehcache.presentation.EhcachePresentationUtils.CLEAR_CACHE_ICON;
import static org.terracotta.modules.ehcache.presentation.EhcachePresentationUtils.DISABLE_CACHE_ICON;
import static org.terracotta.modules.ehcache.presentation.EhcachePresentationUtils.ENABLE_CACHE_ICON;

import org.terracotta.modules.ehcache.presentation.model.CacheManagerInstance;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerModel;
import org.terracotta.modules.ehcache.presentation.model.CacheModel;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.ButtonSet;
import com.tc.admin.common.WindowHelper;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XCheckBox;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XTextArea;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;

public class TopologyPanel extends BaseClusterModelPanel {
  private final CacheManagerModel cacheManagerModel;

  private Mode                    mode;
  private TopologyPanelPage       currentPage;
  private ButtonSet               pageSelector;
  private XContainer              pageHolder;

  EnableCachesAction              disableCachesAction;
  EnableCachesAction              enableCachesAction;
  StatisticsControlAction         enableStatisticsAction;
  StatisticsControlAction         disableStatisticsAction;
  ClearCachesAction               clearCachesAction;
  BulkLoadingControlAction        enableBulkLoadingAction;
  BulkLoadingControlAction        disableBulkLoadingAction;
  ShowConfigAction                showConfigAction;

  public static enum Mode {
    CACHE_MANAGER, CACHE
  }

  public TopologyPanel(ApplicationContext appContext, CacheManagerModel cacheManagerModel) {
    super(appContext, cacheManagerModel.getClusterModel());

    this.cacheManagerModel = cacheManagerModel;

    enableCachesAction = new EnableCachesAction(true);
    disableCachesAction = new EnableCachesAction(false);
    enableStatisticsAction = new StatisticsControlAction(true);
    disableStatisticsAction = new StatisticsControlAction(false);
    enableBulkLoadingAction = new BulkLoadingControlAction(true);
    disableBulkLoadingAction = new BulkLoadingControlAction(false);
    clearCachesAction = new ClearCachesAction();
    showConfigAction = new ShowConfigAction();
  }

  public CacheManagerModel getCacheManagerModel() {
    return cacheManagerModel;

  }

  public void setMode(Mode mode) {
    this.mode = mode;
  }

  public Mode getMode() {
    return mode;
  }

  @Override
  protected void init() {
    /**/
  }

  @Override
  protected XContainer createMainPanel() {
    XContainer topPanel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(1, 5, 1, 0);
    topPanel.add(new XLabel("View by: "), gbc);
    gbc.gridx++;
    gbc.insets = new Insets(1, 0, 1, 0);
    topPanel.add(pageSelector = new ButtonSet(new FlowLayout(FlowLayout.CENTER, 0, 0)), gbc);
    JRadioButton rb = new JRadioButton("CacheManager Instances");
    pageSelector.add(rb);
    rb.setName(Mode.CACHE_MANAGER.toString());
    pageSelector.add(rb = new JRadioButton("Caches"));
    rb.setName(Mode.CACHE.toString());
    pageSelector.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String name = ((ButtonSet) e.getSource()).getSelected();
        if (!name.equals(currentPage.getName())) {
          TopologyPanelPage oldPage = currentPage;
          if (Mode.CACHE.toString().equals(name)) {
            CacheManagerTopologyPanel cmtp = (CacheManagerTopologyPanel) currentPage;
            currentPage = createCacheView(cmtp.getSelectedCacheModels());
          } else {
            CacheTopologyPanel ctp = (CacheTopologyPanel) currentPage;
            currentPage = createCacheManagerView(ctp.getSelectedCacheManagerInstances());
          }
          pageHolder.removeAll();
          oldPage.tearDown();
          currentPage.setup();
          pageHolder.add(currentPage);
          pageHolder.revalidate();
          pageHolder.repaint();
        }
      }
    });
    pageSelector.setSelectedIndex(0);

    pageHolder = new XContainer(new BorderLayout());
    pageHolder.add(currentPage = createCacheManagerView());
    currentPage.setup();

    XContainer result = new XContainer(new BorderLayout());
    result.add(topPanel, BorderLayout.NORTH);
    result.add(pageHolder, BorderLayout.CENTER);

    return result;
  }

  private CacheManagerTopologyPanel createCacheManagerView() {
    setMode(Mode.CACHE_MANAGER);
    CacheManagerTopologyPanel result = new CacheManagerTopologyPanel(this);
    return result;
  }

  private CacheManagerTopologyPanel createCacheManagerView(Set<CacheManagerInstance> selectedCacheManagerInstances) {
    setMode(Mode.CACHE_MANAGER);
    CacheManagerTopologyPanel result = new CacheManagerTopologyPanel(this, selectedCacheManagerInstances);
    return result;
  }

  private CacheTopologyPanel createCacheView(Set<CacheModel> selectedCacheModels) {
    setMode(Mode.CACHE);
    CacheTopologyPanel result = new CacheTopologyPanel(this, selectedCacheModels);
    return result;
  }

  JPopupMenu createPopup() {
    JPopupMenu popup = new JPopupMenu();
    popup.add(enableCachesAction);
    popup.add(disableCachesAction);
    popup.addSeparator();
    popup.add(enableBulkLoadingAction);
    popup.add(disableBulkLoadingAction);
    popup.addSeparator();
    popup.add(enableStatisticsAction);
    popup.add(disableStatisticsAction);
    popup.addSeparator();
    popup.add(clearCachesAction);
    popup.addSeparator();
    popup.add(showConfigAction);
    return popup;
  }

  // Actions

  class ClearCachesAction extends XAbstractAction {
    private ClearCachesAction() {
      super(bundle.getString("clear.caches"), CLEAR_CACHE_ICON);
      putValue(SHORT_DESCRIPTION, bundle.getString("clear.caches.tip"));
    }

    public void actionPerformed(ActionEvent e) {
      JMenuItem mitem = (JMenuItem) e.getSource();
      JPopupMenu popup = (JPopupMenu) mitem.getParent();
      queryClearSelectedCaches(popup);
    }
  }

  private void queryClearSelectedCaches(JPopupMenu popupMenu) {
    XLabel msg = new XLabel(bundle.getString("clear.selected.caches.confirm"));
    msg.setFont((Font) bundle.getObject("message.label.font"));
    msg.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
    int answer = JOptionPane.showConfirmDialog(this, msg, frame.getTitle(), JOptionPane.OK_CANCEL_OPTION);
    if (answer == JOptionPane.OK_OPTION) {
      appContext.submit(currentPage.createClearCachesWorker(popupMenu));
    }
  }

  private void queryEnableSelectedCaches(JPopupMenu popupMenu) {
    XLabel msg = new XLabel(bundle.getString("enable.selected.caches.confirm"));
    msg.setFont((Font) bundle.getObject("message.label.font"));
    msg.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
    int answer = JOptionPane.showConfirmDialog(this, msg, frame.getTitle(), JOptionPane.OK_CANCEL_OPTION);
    if (answer == JOptionPane.OK_OPTION) {
      appContext.submit(currentPage.createEnableCachesWorker(popupMenu, true, false));
    }
  }

  private void queryDisableSelectedCaches(JPopupMenu popupMenu) {
    XContainer panel = new XContainer(new GridBagLayout());
    XLabel label = new XLabel(bundle.getString("disable.selected.caches.confirm"));
    label.setFont((Font) bundle.getObject("message.label.font"));
    label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    XCheckBox cb = new XCheckBox("Clear Contents");
    cb.setSelected(true);
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.anchor = GridBagConstraints.WEST;
    panel.add(label, gbc);
    gbc.gridy++;
    panel.add(cb, gbc);
    Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
    int answer = JOptionPane.showConfirmDialog(this, panel, frame.getTitle(), JOptionPane.OK_CANCEL_OPTION);
    if (answer == JOptionPane.OK_OPTION) {
      appContext.submit(currentPage.createEnableCachesWorker(popupMenu, false, cb.isSelected()));
    }
  }

  class EnableCachesAction extends XAbstractAction {
    private final boolean enable;

    private EnableCachesAction(boolean enable) {
      super();
      this.enable = enable;

      putValue(NAME, bundle.getString(enable ? "enable.caches" : "disable.caches"));
      putValue(SMALL_ICON, enable ? ENABLE_CACHE_ICON : DISABLE_CACHE_ICON);
      putValue(SHORT_DESCRIPTION, bundle.getString(enable ? "enable.caches.tip" : "disable.caches.tip"));
    }

    public void actionPerformed(ActionEvent e) {
      JMenuItem mitem = (JMenuItem) e.getSource();
      JPopupMenu popup = (JPopupMenu) mitem.getParent();
      if (enable) {
        queryEnableSelectedCaches(popup);
      } else {
        queryDisableSelectedCaches(popup);
      }
    }
  }

  class StatisticsControlAction extends AbstractAction {
    private final boolean enable;

    StatisticsControlAction(boolean enable) {
      super();
      this.enable = enable;
      putValue(NAME, bundle.getString(enable ? "enable.statistics" : "disable.statistics"));
      putValue(SHORT_DESCRIPTION, bundle.getString(enable ? "enable.statistics.tip" : "disable.statistics.tip"));
      putValue(SMALL_ICON, enable ? EhcachePresentationUtils.ENABLE_STATS_ICON
          : EhcachePresentationUtils.DISABLE_STATS_ICON);
    }

    public void actionPerformed(ActionEvent ae) {
      JMenuItem mitem = (JMenuItem) ae.getSource();
      JPopupMenu popup = (JPopupMenu) mitem.getParent();
      if (enable) {
        queryEnableSelectedStats(popup);
      } else {
        queryDisableSelectedStats(popup);
      }
    }
  }

  private void queryEnableSelectedStats(JPopupMenu popupMenu) {
    XLabel msg = new XLabel(MessageFormat.format(bundle.getString("enable.selected.statistics.confirm"),
                                                 cacheManagerModel.getName()));
    msg.setFont((Font) bundle.getObject("message.label.font"));
    msg.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
    int answer = JOptionPane.showConfirmDialog(this, msg, frame.getTitle(), JOptionPane.OK_CANCEL_OPTION);
    if (answer == JOptionPane.OK_OPTION) {
      appContext.submit(currentPage.createStatisticsControlWorker(popupMenu, true));
    }
  }

  private void queryDisableSelectedStats(JPopupMenu popupMenu) {
    XLabel msg = new XLabel(MessageFormat.format(bundle.getString("disable.selected.statistics.confirm"),
                                                 cacheManagerModel.getName()));
    msg.setFont((Font) bundle.getObject("message.label.font"));
    msg.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
    int answer = JOptionPane.showConfirmDialog(this, msg, frame.getTitle(), JOptionPane.OK_CANCEL_OPTION);
    if (answer == JOptionPane.OK_OPTION) {
      appContext.submit(currentPage.createStatisticsControlWorker(popupMenu, false));
    }
  }

  class BulkLoadingControlAction extends XAbstractAction {
    private final boolean enable;

    private BulkLoadingControlAction(boolean enable) {
      super();
      this.enable = enable;
      setName(bundle.getString(enable ? "enable.bulkload" : "disable.bulkload"));
      setSmallIcon(enable ? BULK_LOAD_DISABLED_ICON : BULK_LOAD_ENABLED_ICON);
      putValue(SHORT_DESCRIPTION, bundle.getString(enable ? "enable.bulkload.tip" : "disable.bulkload.tip"));
    }

    public void actionPerformed(ActionEvent e) {
      JMenuItem mitem = (JMenuItem) e.getSource();
      JPopupMenu popup = (JPopupMenu) mitem.getParent();
      if (enable) {
        queryEnableBulkLoad(popup);
      } else {
        queryDisableBulkLoad(popup);
      }
    }
  }

  private void queryEnableBulkLoad(final JPopupMenu popup) {
    XLabel msg = new XLabel(MessageFormat.format(bundle.getString("enable.bulkload.confirm"),
                                                 cacheManagerModel.getName()));
    msg.setFont((Font) bundle.getObject("message.label.font"));
    msg.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
    int answer = JOptionPane.showConfirmDialog(this, msg, frame.getTitle(), JOptionPane.OK_CANCEL_OPTION);
    if (answer == JOptionPane.OK_OPTION) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          appContext.submit(currentPage.createBulkLoadControlWorker(popup, true));
        }
      });
    }
  }

  private void queryDisableBulkLoad(final JPopupMenu popup) {
    XLabel msg = new XLabel(MessageFormat.format(bundle.getString("disable.bulkload.confirm"),
                                                 cacheManagerModel.getName()));
    msg.setFont((Font) bundle.getObject("message.label.font"));
    msg.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
    int answer = JOptionPane.showConfirmDialog(this, msg, frame.getTitle(), JOptionPane.OK_CANCEL_OPTION);
    if (answer == JOptionPane.OK_OPTION) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          appContext.submit(currentPage.createBulkLoadControlWorker(popup, false));
        }
      });
    }
  }

  class ShowConfigAction extends XAbstractAction {
    private ShowConfigAction() {
      super("Show Configuration");
    }

    public void actionPerformed(final ActionEvent ae) {
      JMenuItem mitem = (JMenuItem) ae.getSource();
      JPopupMenu popup = (JPopupMenu) mitem.getParent();
      Callable<String> configGenerator = currentPage.createConfigurationGenerator(popup);
      Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, TopologyPanel.this);
      JDialog dialog = new JDialog(frame, frame.getTitle(), true);
      XTextArea textArea = new XTextArea();
      textArea.setEditable(false);
      String text;
      try {
        text = configGenerator.call();
      } catch (Exception e) {
        text = e.toString();
      }
      textArea.setText(text);
      dialog.getContentPane().add(new XScrollPane(textArea));
      dialog.setSize(500, 600);
      WindowHelper.center(dialog);
      dialog.setVisible(true);
    }
  }
}

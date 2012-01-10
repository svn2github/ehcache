/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.hibernatecache.presentation;

import static org.terracotta.modules.hibernatecache.presentation.CacheRegionUtils.CLEAR_CACHE_ICON;
import static org.terracotta.modules.hibernatecache.presentation.CacheRegionUtils.DISABLE_CACHE_ICON;
import static org.terracotta.modules.hibernatecache.presentation.CacheRegionUtils.ENABLE_CACHE_ICON;

import com.tc.admin.AbstractClusterListener;
import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.ExceptionHelper;
import com.tc.admin.common.StatusView;
import com.tc.admin.common.XCheckBox;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.model.IClusterModel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class OverviewPanel extends XContainer {
  private final ApplicationContext    appContext;
  private final IClusterModel         clusterModel;
  private final ClusterListener       clusterListener;
  private final String                persistenceUnit;

  private HibernateStatsMBeanProvider beanProvider;
  private XContainer                  mainPanel;
  private XContainer                  messagePanel;
  private XLabel                      messageLabel;
  private XLabel                      summaryLabel;
  private JToggleButton               enableAllRegionsButton;
  private JToggleButton               disableAllRegionsButton;
  private JToggleButton               flushButton;
  private VuMeterPanel                vuMeterGlobalPanel;
  private VuMeterPanel                vuMeterPerRegionPanel;
  private boolean                     inited;

  private static final ResourceBundle bundle = ResourceBundle.getBundle(HibernateResourceBundle.class.getName());

  public OverviewPanel(ApplicationContext appContext, IClusterModel clusterModel, String persistenceUnit) {
    super(new BorderLayout());

    this.appContext = appContext;
    this.clusterModel = clusterModel;
    this.persistenceUnit = persistenceUnit;
    this.clusterListener = new ClusterListener(clusterModel);
  }

  public void setup() {
    mainPanel = createMainPanel();
    messagePanel = createMessagePanel();
    beanProvider = new HibernateStatsMBeanProvider(clusterModel, persistenceUnit);

    clusterModel.addPropertyChangeListener(clusterListener);
    if (clusterModel.isReady()) {
      init();
      removeAll();
      add(mainPanel);
    } else {
      add(messagePanel);
      messageLabel.setText(appContext.getString("cluster.not.ready.msg"));
    }
  }

  private XContainer createMessagePanel() {
    XContainer panel = new XContainer(new BorderLayout());
    panel.add(messageLabel = new XLabel());
    messageLabel.setText(appContext.getString("initializing"));
    messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
    messageLabel.setFont((Font) appContext.getObject("message.label.font"));
    return panel;
  }

  private class ClusterListener extends AbstractClusterListener {
    private ClusterListener(IClusterModel clusterModel) {
      super(clusterModel);
    }

    @Override
    protected void handleReady() {
      removeAll();
      if (clusterModel.isReady()) {
        if (!inited) {
          init();
        }
        add(mainPanel);
      } else {
        messageLabel.setText(appContext.getString("cluster.not.ready.msg"));
        add(messagePanel);
      }
      revalidate();
      repaint();
    }
  }

  private void init() {
    updateCachedRegionsCount(0, 0);
    vuMeterGlobalPanel.setup();
    vuMeterPerRegionPanel.setup();
  }

  private XContainer createMainPanel() {
    XContainer panel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.gridx = gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;

    panel.add(summaryLabel = new XLabel(), gbc);
    gbc.gridy++;

    panel.add(createToolBar(), gbc);
    gbc.gridy++;

    panel.add(createLegend(), gbc);
    gbc.gridy++;

    gbc.insets = new Insets(1, 1, 1, 1);
    panel.add(createGlobalCachePerformancePanel(), gbc);
    gbc.gridy++;

    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    panel.add(createPerRegionCachePerformancePanel(), gbc);

    return panel;
  }

  private XContainer createToolBar() {
    XContainer panel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(3, 1, 3, 0);
    gbc.gridx = gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;

    panel.add(enableAllRegionsButton = new JToggleButton(bundle.getString("enable.cache"), ENABLE_CACHE_ICON), gbc);
    enableAllRegionsButton.addActionListener(new EnableAllRegionsAction());
    gbc.gridx++;

    panel.add(disableAllRegionsButton = new JToggleButton(bundle.getString("disable.cache"), DISABLE_CACHE_ICON), gbc);
    disableAllRegionsButton.addActionListener(new DisableAllRegionsAction());
    gbc.gridx++;

    gbc.insets = new Insets(3, 5, 3, 0);
    panel.add(flushButton = new JToggleButton(bundle.getString("evict.all.entries"), CLEAR_CACHE_ICON), gbc);
    flushButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        queryFlushCache();
      }
    });
    gbc.gridx++;

    // filler
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(new XLabel(), gbc);

    panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.black));

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
    sv.setIndicator(CacheRegionUtils.HIT_FILL_COLOR);
    sv.setText(bundle.getString("hits"));
    gbc.gridx++;

    panel.add(sv = new StatusView(), gbc);
    sv.setIndicator(CacheRegionUtils.MISS_FILL_COLOR);
    sv.setText(bundle.getString("misses"));
    gbc.gridx++;

    panel.add(sv = new StatusView(), gbc);
    sv.setIndicator(CacheRegionUtils.PUT_FILL_COLOR);
    sv.setText(bundle.getString("puts"));
    gbc.gridx++;

    gbc.anchor = GridBagConstraints.EAST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    panel.add(label = new XLabel(bundle.getString("local.maximum.value")), gbc);
    label.setHorizontalAlignment(SwingConstants.RIGHT);

    return panel;
  }

  private XContainer createGlobalCachePerformancePanel() {
    vuMeterGlobalPanel = new VuMeterPanel(appContext, clusterModel, persistenceUnit, true);
    vuMeterGlobalPanel.setBorder(BorderFactory.createTitledBorder(bundle.getString("global.cache.performance")));
    return vuMeterGlobalPanel;
  }

  private XContainer createPerRegionCachePerformancePanel() {
    vuMeterPerRegionPanel = new VuMeterPanel(appContext, clusterModel, persistenceUnit, false);
    vuMeterPerRegionPanel.setBorder(BorderFactory.createTitledBorder(bundle.getString("per-region.cache.performance")));
    return vuMeterPerRegionPanel;
  }

  private void queryEnableAllRegions() {
    XLabel msg = new XLabel(bundle.getString("enable.all.cache.regions.confirm"));
    Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
    int answer = JOptionPane.showConfirmDialog(this, msg, frame.getTitle(), JOptionPane.OK_CANCEL_OPTION);
    if (answer == JOptionPane.OK_OPTION) {
      appContext.submit(new EnableAllRegionsWorker());
    } else {
      enableAllRegionsButton.setSelected(false);
      enableAllRegionsButton.setEnabled(true);
    }
  }

  private void queryDisableAllRegions() {
    XContainer panel = new XContainer(new GridBagLayout());
    XLabel label = new XLabel(bundle.getString("disable.all.cache.regions.confirm"));
    XCheckBox cb = new XCheckBox("Flush Caches");
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
      appContext.submit(new DisableAllRegionsWorker(cb.isSelected()));
    } else {
      disableAllRegionsButton.setSelected(false);
      disableAllRegionsButton.setEnabled(true);
    }
  }

  private void queryFlushCache() {
    XLabel msg = new XLabel(bundle.getString("flush.cache.confirm"));
    Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
    int answer = JOptionPane.showConfirmDialog(this, msg, frame.getTitle(), JOptionPane.OK_CANCEL_OPTION);
    if (answer == JOptionPane.OK_OPTION) {
      appContext.submit(new FlushCacheWorker());
    } else {
      flushButton.setSelected(false);
      flushButton.setEnabled(true);
    }
  }

  private class FlushCacheWorker extends BasicWorker<Void> {
    private FlushCacheWorker() {
      super(new Callable<Void>() {
        public Void call() throws Exception {
          beanProvider.getBean().flushRegionCaches();
          return null;
        }
      });
      flushButton.setEnabled(false);
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
      flushButton.setSelected(false);
      flushButton.setEnabled(true);
    }
  }

  private class EnableAllRegionsAction implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      queryEnableAllRegions();
    }
  }

  private class EnableAllRegionsWorker extends BasicWorker<Void> {
    private EnableAllRegionsWorker() {
      super(new Callable<Void>() {
        public Void call() throws Exception {
          setCacheRegionsEnabled(true);
          return null;
        }
      });
      enableAllRegionsButton.setEnabled(false);
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
      enableAllRegionsButton.setSelected(false);
      enableAllRegionsButton.setEnabled(true);
    }
  }

  private class DisableAllRegionsAction implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      queryDisableAllRegions();
    }
  }

  private class DisableAllRegionsWorker extends BasicWorker<Void> {
    private DisableAllRegionsWorker(final boolean flushCaches) {
      super(new Callable<Void>() {
        public Void call() throws Exception {
          setCacheRegionsEnabled(false);
          if (flushCaches) {
            beanProvider.getBean().flushRegionCaches();
          }
          return null;
        }
      });
      disableAllRegionsButton.setEnabled(false);
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
      disableAllRegionsButton.setSelected(false);
      disableAllRegionsButton.setEnabled(true);
    }
  }

  private void setCacheRegionsEnabled(boolean enabled) {
    beanProvider.getBean().setRegionCachesEnabled(enabled);
  }

  protected void updateCachedRegionsCount(final int clusteredRegionCount, final int allRegionsCount) {
    String format = bundle.getString("regions.summary.format");
    summaryLabel.setText(MessageFormat.format(format, clusteredRegionCount, allRegionsCount));

    enableAllRegionsButton.setEnabled(clusteredRegionCount != allRegionsCount);
    disableAllRegionsButton.setEnabled(clusteredRegionCount > 0);
  }

  public String getPersistenceUnit() {
    return persistenceUnit;
  }

  protected ApplicationContext getApplicationContext() {
    return appContext;
  }

  protected IClusterModel getClusterModel() {
    return clusterModel;
  }

  @Override
  public void tearDown() {
    clusterModel.removePropertyChangeListener(clusterListener);
    clusterListener.tearDown();

    super.tearDown();
  }
}

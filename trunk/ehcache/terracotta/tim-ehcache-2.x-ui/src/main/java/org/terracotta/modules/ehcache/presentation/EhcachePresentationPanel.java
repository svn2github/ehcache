/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.ehcache.presentation;

import org.terracotta.modules.configuration.Presentation;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerInstance;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerInstanceListener;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerModel;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerModelListener;
import org.terracotta.modules.ehcache.presentation.model.CacheModel;
import org.terracotta.modules.ehcache.presentation.model.CacheModelInstance;
import org.terracotta.modules.ehcache.presentation.model.ClusteredCacheModel;
import org.terracotta.modules.ehcache.presentation.model.EhcacheModel;
import org.terracotta.modules.ehcache.presentation.model.EhcacheModelListener;
import org.terracotta.modules.ehcache.presentation.model.StandaloneCacheModel;

import com.tc.admin.AbstractClusterListener;
import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.PagedView;
import com.tc.admin.common.XComboBox;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.model.IClusterModel;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class EhcachePresentationPanel extends Presentation implements EhcacheModelListener {
  private ApplicationContext          appContext;
  private IClusterModel               clusterModel;
  private ClusterListener             clusterListener;
  private EhcacheModel                ehcacheModel;
  private XContainer                  mainPanel;
  private XComboBox                   cacheManagerSelector;
  private XLabel                      summaryLabel;
  private XContainer                  messagePanel;
  private XLabel                      messageLabel;
  private PagedView                   pagedView;
  private MyListener                  myListener;

  private static final ResourceBundle bundle = ResourceBundle.getBundle(EhcacheResourceBundle.class.getName());

  private static final Icon           icon;

  static {
    try {
      icon = new ImageIcon(EhcachePresentationPanel.class.getResource("ehcache.png"));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public EhcachePresentationPanel() {
    super();
  }

  private synchronized IClusterModel getClusterModel() {
    return clusterModel;
  }

  @Override
  public void setup(ApplicationContext appContext, IClusterModel clusterModel) {
    this.appContext = appContext;
    this.clusterModel = clusterModel;
    this.ehcacheModel = new EhcacheModel(clusterModel);
    this.ehcacheModel.addEhcacheModelListener(this);
    this.myListener = new MyListener();

    setup();
  }

  private class StartupWorker extends BasicWorker<Void> {
    private StartupWorker() {
      super(new Callable<Void>() {
        public Void call() throws Exception {
          ehcacheModel.startup();
          return null;
        }
      });
    }

    @Override
    protected void finished() {
      Exception e = getException();
      if (e != null) {
        appContext.log(e);
      }
      clusterModel.addPropertyChangeListener(clusterListener = new ClusterListener(clusterModel));
      if (clusterModel.isReady()) {
        init();
      }
    }
  }

  @Override
  public void startup() {
    appContext.submit(new StartupWorker());
  }

  @Override
  public Icon getIcon() {
    return icon;
  }

  private void setup() {
    setLayout(new BorderLayout());
    mainPanel = new XContainer(new BorderLayout());
    XContainer topPanel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);

    XLabel headerLabel;
    topPanel.add(headerLabel = new XLabel(bundle.getString("cache.manager")), gbc);
    headerLabel.setFont((Font) appContext.getObject("header.label.font"));
    gbc.gridx++;
    gbc.ipadx = 10;

    topPanel.add(cacheManagerSelector = new XComboBox(), gbc);
    cacheManagerSelector.addItemListener(new CacheManagerSelectionListener());
    gbc.gridx++;

    topPanel.add(summaryLabel = new XLabel(), gbc);
    gbc.gridx++;

    // filler
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    topPanel.add(new XLabel(), gbc);
    gbc.gridx++;

    mainPanel.add(topPanel, BorderLayout.NORTH);
    mainPanel.add(pagedView = new PagedView(), BorderLayout.CENTER);

    messagePanel = new XContainer(new BorderLayout());
    messagePanel.add(messageLabel = new XLabel());
    messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
    messageLabel.setFont((Font) appContext.getObject("message.label.font"));
    messageLabel.setText(appContext.getString("cluster.not.ready.msg"));
  }

  private class ClusterListener extends AbstractClusterListener {
    private ClusterListener(IClusterModel clusterModel) {
      super(clusterModel);
    }

    @Override
    protected void handleReady() {
      if (tornDown.get()) { return; }

      if (clusterModel.isReady()) {
        init();
      } else {
        suspend();
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

  private void init() {
    removeAll();
    cacheManagerSelector.removeAllItems();
    String[] cacheManagerNames = ehcacheModel.getCacheManagerNames();
    if (cacheManagerNames != null && cacheManagerNames.length > 0) {
      add(mainPanel, BorderLayout.CENTER);
      for (String cacheManagerName : cacheManagerNames) {
        cacheManagerSelector.addItem(cacheManagerName);
      }
      addModelListeners(ehcacheModel.getCacheManagerModel(cacheManagerNames[0]));
      handleSummaryText();
      firePropertyChange(Presentation.PROP_PRESENTATION_READY, false, true);
    } else {
      messageLabel.setText(bundle.getString("no.cache-managers.msg"));
      add(messagePanel);
    }
    revalidate();
    repaint();
  }

  private void suspend() {
    removeAll();
    cacheManagerSelector.removeAllItems();
    messageLabel.setText(appContext.getString("cluster.not.ready.msg"));
    add(messagePanel);
    revalidate();
    repaint();
  }

  private class CacheManagerSelectionListener implements ItemListener {
    public void itemStateChanged(ItemEvent e) {
      IClusterModel theClusterModel = getClusterModel();
      if (theClusterModel == null) { return; }

      String currentPage = pagedView.getPage();
      if (currentPage != null) {
        removeModelListeners(ehcacheModel.getCacheManagerModel(currentPage));
      }

      String cacheManagerName = (String) cacheManagerSelector.getSelectedItem();
      if (cacheManagerName != null) {
        CacheManagerPanel cacheManagerPage;
        if (!pagedView.hasPage(cacheManagerName)) {
          cacheManagerPage = new CacheManagerPanel(appContext, ehcacheModel.getCacheManagerModel(cacheManagerName));
          cacheManagerPage.setName(cacheManagerName);
          pagedView.addPage(cacheManagerPage);
          cacheManagerPage.setup();
        } else {
          cacheManagerPage = (CacheManagerPanel) pagedView.getPage(cacheManagerName);
        }
        pagedView.setPage(cacheManagerName);
        if (cacheManagerPage != null) {
          addModelListeners(cacheManagerPage.getCacheManagerModel());
        }
        handleSummaryText();
      }
    }
  }

  private void addModelListeners(CacheManagerModel cacheManagerModel) {
    if (cacheManagerModel != null) {
      cacheManagerModel.addCacheManagerModelListener(myListener);
      for (Iterator<CacheManagerInstance> iter = cacheManagerModel.cacheManagerInstanceIterator(); iter.hasNext();) {
        iter.next().addCacheManagerInstanceListener(myListener);
      }
    }
  }

  private void removeModelListeners(CacheManagerModel cacheManagerModel) {
    if (cacheManagerModel != null) {
      cacheManagerModel.removeCacheManagerModelListener(myListener);
      for (Iterator<CacheManagerInstance> iter = cacheManagerModel.cacheManagerInstanceIterator(); iter.hasNext();) {
        iter.next().removeCacheManagerInstanceListener(myListener);
      }
    }
  }

  private CacheManagerModel getSelection() {
    String cacheManagerName = (String) cacheManagerSelector.getSelectedItem();
    return cacheManagerName != null ? ehcacheModel.getCacheManagerModel(cacheManagerName) : null;
  }

  private class MyListener implements CacheManagerModelListener, CacheManagerInstanceListener, Runnable {
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
      SwingUtilities.invokeLater(this);
    }

    public void instanceRemoved(CacheManagerInstance instance) {
      SwingUtilities.invokeLater(this);
    }

    public void cacheManagerInstanceChanged(CacheManagerInstance cacheManagerInstance) {
      /**/
    }

    public void cacheModelInstanceAdded(CacheModelInstance cacheModelInstance) {
      SwingUtilities.invokeLater(this);
    }

    public void cacheModelInstanceChanged(CacheModelInstance cacheModelInstance) {
      /**/
    }

    public void cacheModelInstanceRemoved(CacheModelInstance cacheModelInstance) {
      SwingUtilities.invokeLater(this);
    }

    public void run() {
      if (!tornDown.get()) {
        handleSummaryText();
      }
    }
  }

  private void handleSummaryText() {
    CacheManagerModel cacheManagerModel = getSelection();
    if (cacheManagerModel != null) {
      int cacheModelInstanceCount = cacheManagerModel.getCacheModelInstanceCount();
      String summary = MessageFormat.format(bundle.getString("cache-manager.residence.summary"),
                                            cacheManagerModel.getInstanceCount(), cacheModelInstanceCount);
      summaryLabel.setText(summary);
    }
  }

  private void testAnyCacheManagers() {
    boolean changedHierarchy = false;
    DefaultComboBoxModel puModel = (DefaultComboBoxModel) cacheManagerSelector.getModel();
    if (puModel.getSize() == 0) {
      if (mainPanel.getParent() != null) {
        removeAll();
      }
      messageLabel.setText(bundle.getString("no.cache-managers.msg"));
      if (messagePanel.getParent() == null) {
        add(messagePanel);
        changedHierarchy = true;
      }
    } else {
      if (messagePanel.getParent() != null) {
        removeAll();
      }
      if (mainPanel.getParent() == null) {
        add(mainPanel);
        changedHierarchy = true;
      }
    }
    if (changedHierarchy) {
      revalidate();
      repaint();
    }
  }

  public void cacheManagerModelAdded(final CacheManagerModel cacheManagerModel) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        if (tornDown.get()) { return; }
        String cacheManagerName = cacheManagerModel.getName();
        DefaultComboBoxModel comboBoxModel = (DefaultComboBoxModel) cacheManagerSelector.getModel();
        if (comboBoxModel.getIndexOf(cacheManagerName) == -1) {
          comboBoxModel.addElement(cacheManagerName);
          appContext.setStatus("Added Ehcache CacheManager '" + cacheManagerName + "'");
          testAnyCacheManagers();
          if (cacheManagerSelector.getItemCount() == 1) {
            firePropertyChange(Presentation.PROP_PRESENTATION_READY, false, true);
          }
          addModelListeners(cacheManagerModel);
        }
      }
    });
  }

  public void cacheManagerModelRemoved(final CacheManagerModel cacheManagerModel) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        if (tornDown.get()) { return; }
        String cacheManagerName = cacheManagerModel.getName();
        DefaultComboBoxModel comboBoxModel = (DefaultComboBoxModel) cacheManagerSelector.getModel();
        int index = comboBoxModel.getIndexOf(cacheManagerName);
        if (index != -1) {
          CacheManagerPanel page = (CacheManagerPanel) pagedView.getPage(cacheManagerName);
          if (page != null) {
            pagedView.removePage(cacheManagerName);
            page.tearDown();
          }
          comboBoxModel.removeElementAt(index);
          appContext.setStatus("Removed Ehcache CacheManager '" + cacheManagerName + "'");
          testAnyCacheManagers();
          if (cacheManagerSelector.getItemCount() == 0) {
            firePropertyChange(Presentation.PROP_PRESENTATION_READY, true, false);
          }
          removeModelListeners(cacheManagerModel);
        }
      }
    });
  }

  @Override
  public boolean isReady() {
    return cacheManagerSelector != null && cacheManagerSelector.getItemCount() > 0;
  }

  private final AtomicBoolean tornDown = new AtomicBoolean(false);

  @Override
  public void tearDown() {
    if (!tornDown.compareAndSet(false, true)) { return; }

    if (pagedView != null) {
      String currentPage = pagedView.getPage();
      if (currentPage != null && ehcacheModel != null) {
        removeModelListeners(ehcacheModel.getCacheManagerModel(currentPage));
      }
      if (mainPanel != null) {
        mainPanel.tearDown();
      }
    }

    if (ehcacheModel != null) {
      ehcacheModel.removeEhcacheModelListener(this);
      if (clusterListener != null) {
        if (clusterModel != null) {
          clusterModel.removePropertyChangeListener(clusterListener);
        }
        clusterListener.tearDown();
      }
      ehcacheModel.tearDown();
    }

    removeAll();
  }
}

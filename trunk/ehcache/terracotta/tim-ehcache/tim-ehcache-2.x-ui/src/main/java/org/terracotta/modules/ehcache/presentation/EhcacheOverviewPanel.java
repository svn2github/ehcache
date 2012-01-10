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
import com.tc.admin.common.WindowHelper;
import com.tc.admin.common.XButton;
import com.tc.admin.common.XContainer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

public class EhcacheOverviewPanel extends BaseClusterModelPanel implements CacheManagerModelListener,
    CacheManagerInstanceListener {
  private final CacheManagerModel cacheManagerModel;

  private TopologyPanel           topologyPanel;
  private ManageMessage           currentManageMessage;

  public EhcacheOverviewPanel(ApplicationContext appContext, CacheManagerModel cacheManagerModel) {
    super(appContext, cacheManagerModel.getClusterModel());
    this.cacheManagerModel = cacheManagerModel;
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
    topologyPanel.setup();

    for (Iterator<CacheManagerInstance> iter = cacheManagerModel.cacheManagerInstanceIterator(); iter.hasNext();) {
      iter.next().addCacheManagerInstanceListener(this);
    }
  }

  @Override
  protected XContainer createMainPanel() {
    XContainer panel = new XContainer(new BorderLayout());
    panel.add(createAltToolBar(), BorderLayout.NORTH);
    panel.add(createTopologyPanel(), BorderLayout.CENTER);
    return panel;
  }

  public JComponent createAltToolBar() {
    XContainer panel = new XContainer(new FlowLayout(FlowLayout.CENTER, 1, 3));

    panel.add(new XButton(new ManageEnablementAction()));
    panel.add(new XButton(new ManageBulkLoadAction()));
    panel.add(new XButton(new ManageStatisticsAction()));
    panel.add(new XButton(new ManageContentsAction()));
    panel.add(new XButton(new ManageSettingsAction2()));

    return panel;
  }

  private XContainer createTopologyPanel() {
    XContainer panel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = gbc.weighty = 1.0;

    panel.add(topologyPanel = new TopologyPanel(appContext, cacheManagerModel), gbc);

    return panel;
  }

  private abstract class AbstractManageAction extends AbstractAction {
    protected AbstractManageAction(String name) {
      super(name);
    }

    protected abstract ManageMessage createMessage();

    public void actionPerformed(ActionEvent ae) {
      Component c = EhcacheOverviewPanel.this;
      currentManageMessage = createMessage();
      boolean yesLast = UIManager.getBoolean("OptionPane.isYesLast");
      Object[] options;
      final String applyLabel = currentManageMessage.createApplyLabel();
      String cancelLabel = UIManager.getString("OptionPane.cancelButtonText", c.getLocale());
      Object defaultOption = applyLabel;
      if (yesLast) {
        options = new String[] { cancelLabel, applyLabel };
      } else {
        options = new String[] { applyLabel, cancelLabel };
      }
      final Object[] theOptions = options;
      final int result = JOptionPane.showOptionDialog(c, currentManageMessage, currentManageMessage.getTitle(),
                                                      JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                                                      options, defaultOption);
      if (currentManageMessage != null) {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            if (result != JOptionPane.CLOSED_OPTION && theOptions[result].equals(applyLabel)) {
              Boolean toNewcomers = null;
              if (currentManageMessage.hasApplyToNewcomersToggle()) {
                toNewcomers = Boolean.valueOf(currentManageMessage.shouldApplyToNewcomers());
              }
              currentManageMessage.apply(toNewcomers);
            }
            currentManageMessage.tearDown();
            currentManageMessage = null;
          }
        });
      }
    }
  }

  private class ManageEnablementAction extends AbstractManageAction {
    private ManageEnablementAction() {
      super(bundle.getString("Manage Active Caches..."));
    }

    @Override
    protected ManageMessage createMessage() {
      Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, EhcacheOverviewPanel.this);
      return new ManageEnablementMessage(frame, appContext, cacheManagerModel, topologyPanel.getMode());
    }
  }

  private class ManageStatisticsAction extends AbstractManageAction {
    private ManageStatisticsAction() {
      super(bundle.getString("Cache Statistics..."));
    }

    @Override
    protected ManageMessage createMessage() {
      Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, EhcacheOverviewPanel.this);
      return new ManageStatisticsMessage(frame, appContext, cacheManagerModel, topologyPanel.getMode());
    }
  }

  private class ManageBulkLoadAction extends AbstractManageAction {
    private ManageBulkLoadAction() {
      super(bundle.getString("Cache BulkLoading..."));
    }

    @Override
    protected ManageMessage createMessage() {
      Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, EhcacheOverviewPanel.this);
      return new ManageBulkLoadingMessage(frame, appContext, cacheManagerModel, topologyPanel.getMode());
    }
  }

  private class ManageContentsAction extends AbstractManageAction {
    private ManageContentsAction() {
      super(bundle.getString("Clear Cache Contents..."));
    }

    @Override
    protected ManageMessage createMessage() {
      Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, EhcacheOverviewPanel.this);
      return new ManageContentsMessage(frame, appContext, cacheManagerModel, topologyPanel.getMode());
    }
  }

  private class ManageSettingsAction2 extends AbstractAction {
    private ManageSettingsAction2() {
      super(bundle.getString("Cache Configuration..."));
    }

    private CacheModelMessage createMessage() {
      return new CacheModelMessage(getCacheManagerModel());
    }

    public void actionPerformed(ActionEvent ae) {
      Component c = EhcacheOverviewPanel.this;
      Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, c);
      final CacheModelMessage msg = createMessage();
      final String applyLabel = "Apply";
      final String closeLabel = "Close";
      final JOptionPane optionPane = new JOptionPane(msg, JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                                                     new String[] { applyLabel, closeLabel }, applyLabel);
      final JDialog dialog = new JDialog(frame, bundle.getString("Manage Cache Configuration"), true);
      dialog.setContentPane(optionPane);
      dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
      dialog.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent we) {
          if (msg.canClose()) {
            dialog.setVisible(false);
          }
        }
      });
      optionPane.addPropertyChangeListener(new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent e) {
          String prop = e.getPropertyName();
          String newValue = e.getNewValue().toString();
          if (dialog.isVisible() && (e.getSource() == optionPane) && prop.equals(JOptionPane.VALUE_PROPERTY)) {
            if ((closeLabel.equals(newValue) && msg.canClose())) {
              dialog.setVisible(false);
            } else if (applyLabel.equals(newValue)) {
              msg.apply();
            }
          }
          if (dialog.isVisible()) {
            optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
          }
        }
      });
      dialog.pack();
      WindowHelper.center(dialog, c);
      dialog.setVisible(true);
      msg.tearDown();
    }
  }

  protected CacheManagerModel getCacheManagerModel() {
    return cacheManagerModel;
  }

  public void instanceAdded(CacheManagerInstance instance) {
    instance.addCacheManagerInstanceListener(this);
  }

  public void instanceRemoved(CacheManagerInstance instance) {
    instance.removeCacheManagerInstanceListener(this);
  }

  public void cacheModelAdded(final CacheModel cacheModel) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        setStatus(MessageFormat.format(bundle.getString("overview.cacheModelAdded"), cacheModel.getCacheName(),
                                       cacheManagerModel.getName()));
      }
    });
  }

  public void cacheModelRemoved(final CacheModel cacheModel) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        setStatus(MessageFormat.format(bundle.getString("overview.cacheModelRemoved"), cacheModel.getCacheName(),
                                       cacheManagerModel.getName()));
      }
    });
  }

  public void cacheModelChanged(final CacheModel cacheModel) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        setStatus(MessageFormat.format(bundle.getString("overview.cacheModelChanged"), cacheModel.getCacheName(),
                                       cacheManagerModel.getName()));
      }
    });
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
    /**/
  }

  public void cacheModelInstanceChanged(CacheModelInstance cacheModelInstance) {
    /**/
  }

  public void cacheModelInstanceRemoved(CacheModelInstance cacheModelInstance) {
    /**/
  }

  @Override
  public void tearDown() {
    if (currentManageMessage != null) {
      JDialog dialog = (JDialog) SwingUtilities.getAncestorOfClass(JDialog.class, currentManageMessage);
      if (dialog != null) {
        dialog.setVisible(false);
      }
    }

    cacheManagerModel.removeCacheManagerModelListener(this);
    for (Iterator<CacheManagerInstance> iter = cacheManagerModel.cacheManagerInstanceIterator(); iter.hasNext();) {
      iter.next().removeCacheManagerInstanceListener(this);
    }

    super.tearDown();
  }
}

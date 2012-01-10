/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.hibernatecache.presentation;

import org.terracotta.modules.configuration.Presentation;
import org.terracotta.modules.hibernatecache.jmx.HibernateStatsUtils;

import com.tc.admin.AbstractClusterListener;
import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.ButtonSet;
import com.tc.admin.common.PagedView;
import com.tc.admin.common.XComboBox;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Set;

import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class HibernatePresentationPanel extends Presentation implements NotificationListener {
  private ApplicationContext          appContext;
  private IClusterModel               clusterModel;
  private ClusterListener             clusterListener;
  private XContainer                  mainPanel;
  private XComboBox                   puSelector;
  private ButtonSet                   viewButtonSet;
  private PagedView                   pagedView;
  private XContainer                  messagePanel;
  private XLabel                      messageLabel;

  private static final ObjectName     statsBeanPattern;
  static {
    try {
      statsBeanPattern = new ObjectName(HibernateStatsUtils.STATS_BEAN_NAME_PREFIX + ",*");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static final Icon           icon;

  static {
    try {
      icon = new ImageIcon(HibernatePresentationPanel.class.getResource("Hibernate.png"));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static final ResourceBundle bundle                 = ResourceBundle.getBundle(HibernateResourceBundle.class
                                                                 .getName());

  public static final String          HIBERNATE_KEY          = "Hibernate";
  public static final String          SECOND_LEVEL_CACHE_KEY = "Second-Level Cache";

  private static final String         EMPTY_PAGE_NAME        = "NULL_PAGE";

  public HibernatePresentationPanel() {
    super();
  }

  @Override
  public void setup(ApplicationContext appContext, IClusterModel clusterModel) {
    this.appContext = appContext;
    this.clusterModel = clusterModel;
    setup();
  }

  @Override
  public void startup() {
    clusterModel.addPropertyChangeListener(clusterListener = new ClusterListener(clusterModel));
    if (clusterModel.isReady()) {
      init();
    }
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
    topPanel.add(headerLabel = new XLabel(bundle.getString("persistence.unit")), gbc);
    headerLabel.setFont((Font) appContext.getObject("header.label.font"));
    gbc.gridx++;
    gbc.ipadx = 10;

    topPanel.add(puSelector = new XComboBox(), gbc);
    puSelector.addItemListener(new PUSelectionListener());
    gbc.gridx++;

    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;

    // filler
    topPanel.add(new XLabel(), gbc);
    gbc.gridx++;

    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.insets = new Insets(3, 3, 3, 3);
    topPanel.add(headerLabel = new XLabel(bundle.getString("select.feature")), gbc);
    headerLabel.setFont((Font) appContext.getObject("header.label.font"));
    headerLabel.setHorizontalAlignment(SwingConstants.RIGHT);
    gbc.gridx++;

    topPanel.add(viewButtonSet = new ButtonSet(new FlowLayout(FlowLayout.LEFT, 0, 0)), gbc);
    JToggleButton radioButton;
    viewButtonSet.add(radioButton = new JToggleButton(bundle.getString("hibernate")));
    radioButton.setName(HIBERNATE_KEY);
    viewButtonSet.add(radioButton = new JToggleButton(bundle.getString("second-level.cache")));
    radioButton.setName(SECOND_LEVEL_CACHE_KEY);
    viewButtonSet.setSelected(HIBERNATE_KEY);
    viewButtonSet.addActionListener(new ViewByListener());

    mainPanel.add(topPanel, BorderLayout.NORTH);
    mainPanel.add(pagedView = new PagedView(), BorderLayout.CENTER);
    XLabel emptyPage = new XLabel();
    emptyPage.setName(EMPTY_PAGE_NAME);
    pagedView.addPage(emptyPage);

    messagePanel = new XContainer(new BorderLayout());
    messagePanel.add(messageLabel = new XLabel());
    messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
    messageLabel.setFont((Font) appContext.getObject("message.label.font"));
    messageLabel.setText(appContext.getString("cluster.not.ready.msg"));
  }

  public String getViewByName() {
    return viewButtonSet.getSelected();
  }

  private class ViewByListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      String pu = (String) puSelector.getSelectedItem();
      if (pu != null) {
        if (pagedView.hasPage(pu)) {
          PersistenceUnitPanel puPage = (PersistenceUnitPanel) pagedView.getPage(pu);
          puPage.setViewBy(viewButtonSet.getSelected());
        }
      }
    }
  }

  private class PUSelectionListener implements ItemListener {
    public void itemStateChanged(ItemEvent e) {
      String pu = (String) puSelector.getSelectedItem();
      if (pu != null) {
        PersistenceUnitPanel puPage;
        if (!pagedView.hasPage(pu)) {
          puPage = new PersistenceUnitPanel(appContext, clusterModel, pu);
          puPage.setName(pu);
          pagedView.addPage(puPage);
          puPage.setup();
        } else {
          puPage = (PersistenceUnitPanel) pagedView.getPage(pu);
        }
        puPage.setViewBy(viewButtonSet.getSelected());
        pagedView.setPage(pu);
      } else {
        pagedView.setPage(EMPTY_PAGE_NAME);
      }
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
  }

  private void addMBeanServerDelegateListener() {
    IServer activeCoord = clusterModel.getActiveCoordinator();
    try {
      ObjectName on = new ObjectName("JMImplementation:type=MBeanServerDelegate");
      activeCoord.addNotificationListener(on, this);
    } catch (Exception e) {
      /**/
    }
  }

  private void removeMBeanServerDelegateListener() {
    IServer activeCoord = clusterModel.getActiveCoordinator();
    if (activeCoord != null) {
      try {
        ObjectName on = new ObjectName("JMImplementation:type=MBeanServerDelegate");
        activeCoord.addNotificationListener(on, this);
      } catch (Exception e) {
        /**/
      }
    }
  }

  private void init() {
    removeAll();
    puSelector.removeAllItems();

    addMBeanServerDelegateListener();

    IServer activeCoord = clusterModel.getActiveCoordinator();
    DefaultComboBoxModel puModel = (DefaultComboBoxModel) puSelector.getModel();

    try {
      Set<ObjectName> onSet = activeCoord.queryNames(statsBeanPattern, null);
      Iterator<ObjectName> onIter = onSet.iterator();
      while (onIter.hasNext()) {
        ObjectName on = onIter.next();
        String persistenceUnit = on.getKeyProperty("name");
        if (puModel.getIndexOf(persistenceUnit) == -1) {
          puModel.addElement(persistenceUnit);
        }
      }
      boolean isReady = isReady();
      firePropertyChange(Presentation.PROP_PRESENTATION_READY, !isReady, isReady);
    } catch (Exception e) {
      appContext.log(e);
    }

    testAnyPersistenceUnits();
  }

  private void suspend() {
    removeAll();
    puSelector.removeAllItems();
    messageLabel.setText(appContext.getString("cluster.not.ready.msg"));
    add(messagePanel);
    revalidate();
    repaint();
  }

  private void testAnyPersistenceUnits() {
    boolean changedHierarchy = false;
    if (puSelector.getItemCount() == 0) {
      if (mainPanel.isShowing()) {
        removeAll();
      }
      messageLabel.setText(bundle.getString("no.session-factories.msg"));
      if (!messagePanel.isShowing()) {
        add(messagePanel);
        changedHierarchy = true;
      }
    } else {
      if (messagePanel.isShowing()) {
        removeAll();
      }
      if (!mainPanel.isShowing()) {
        add(mainPanel);
        changedHierarchy = true;
      }
    }
    if (changedHierarchy) {
      revalidate();
      repaint();
    }
  }

  private boolean testRegisterPersistenceUnit(ObjectName on) {
    boolean result = false;

    try {
      if (statsBeanPattern.apply(on)) {
        DefaultComboBoxModel puModel = (DefaultComboBoxModel) puSelector.getModel();
        String persistenceUnit = on.getKeyProperty("name");
        if (puModel.getIndexOf(persistenceUnit) == -1) {
          puModel.addElement(persistenceUnit);
          appContext.setStatus("Added Hibernate persistence-unit '" + persistenceUnit + "'");
          result = true;
        }
        testAnyPersistenceUnits();
        if (puSelector.getItemCount() == 1) {
          firePropertyChange(Presentation.PROP_PRESENTATION_READY, false, true);
        }
      }
    } catch (Exception e) {
      appContext.log(e);
    }

    return result;
  }

  private boolean testUnregisterPersistenceUnit(ObjectName on) {
    boolean result = false;

    try {
      if (statsBeanPattern.apply(on)) {
        String persistenceUnit = on.getKeyProperty("name");
        if (persistenceUnit != null) {
          IServer activeCoord = clusterModel.getActiveCoordinator();
          ObjectName tmpl = HibernateStatsUtils.getHibernateStatsBeanName(persistenceUnit);
          tmpl = new ObjectName(tmpl.getCanonicalName() + ",*");
          Set<ObjectName> onSet = activeCoord.queryNames(tmpl, null);
          if (onSet.size() == 0) {
            DefaultComboBoxModel puModel = (DefaultComboBoxModel) puSelector.getModel();
            int index = puModel.getIndexOf(persistenceUnit);
            if (index != -1) {
              pagedView.removePage(persistenceUnit);
              puModel.removeElementAt(index);
              result = true;
              appContext.setStatus("Removed Hibernate persistence-unit '" + persistenceUnit + "'");
            }
          }
          if (puSelector.getItemCount() == 0) {
            firePropertyChange(Presentation.PROP_PRESENTATION_READY, true, false);
          }
        }
      }
    } catch (Exception e) {
      appContext.log(e);
    }

    return result;
  }

  public void handleNotification(Notification notification, Object handback) {
    String type = notification.getType();
    if (notification instanceof MBeanServerNotification) {
      final MBeanServerNotification mbsn = (MBeanServerNotification) notification;
      if (type.equals(MBeanServerNotification.REGISTRATION_NOTIFICATION)) {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            testRegisterPersistenceUnit(mbsn.getMBeanName());
          }
        });
      } else if (type.equals(MBeanServerNotification.UNREGISTRATION_NOTIFICATION)) {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            testUnregisterPersistenceUnit(mbsn.getMBeanName());
          }
        });
      }
    }
  }

  @Override
  public boolean isReady() {
    return puSelector != null && puSelector.getItemCount() > 0;
  }

  @Override
  public void tearDown() {
    removeMBeanServerDelegateListener();

    clusterModel.removePropertyChangeListener(clusterListener);
    clusterListener.tearDown();

    pagedView.tearDown();

    removeAll();
  }
}

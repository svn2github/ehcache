/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.ehcache.presentation;

import com.tc.admin.AbstractClusterListener;
import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.model.ClientConnectionListener;
import com.tc.admin.model.IClient;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.lang.reflect.InvocationTargetException;
import java.util.ResourceBundle;

import javax.management.MBeanException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public abstract class BaseClusterModelPanel extends XContainer implements ClientConnectionListener {
  protected final ApplicationContext    appContext;
  protected final IClusterModel         clusterModel;
  protected final ClusterListener       clusterListener;

  protected XContainer                  mainPanel;
  protected XContainer                  messagePanel;
  protected XLabel                      messageLabel;

  protected static final ResourceBundle bundle = ResourceBundle.getBundle(EhcacheResourceBundle.class.getName());

  public BaseClusterModelPanel(ApplicationContext appContext, IClusterModel clusterModel) {
    super(new BorderLayout());

    this.appContext = appContext;
    this.clusterModel = clusterModel;
    this.clusterListener = new ClusterListener(clusterModel);
  }

  protected abstract void init();

  protected abstract XContainer createMainPanel();

  public void clientConnected(IClient client) {
    /**/
  }

  public void clientDisconnected(IClient client) {
    /**/
  }

  public ApplicationContext getApplicationContext() {
    return appContext;
  }

  public IClusterModel getClusterModel() {
    return clusterModel;
  }

  public IServer getActiveCoordinator() {
    return clusterModel.getActiveCoordinator();
  }

  public void setup() {
    clusterModel.addPropertyChangeListener(clusterListener);
    IServer activeCoord = getActiveCoordinator();
    if (activeCoord != null) {
      activeCoord.addClientConnectionListener(this);
    }

    setLayout(new BorderLayout());

    mainPanel = createMainPanel();
    messagePanel = createMessagePanel();

    if (clusterModel.isReady()) {
      resume();
    } else {
      suspend();
    }
  }

  private class ClusterListener extends AbstractClusterListener {
    private ClusterListener(IClusterModel clusterModel) {
      super(clusterModel);
    }

    @Override
    protected void handleReady() {
      if (clusterModel.isReady()) {
        resume();
      } else {
        suspend();
      }
    }

    @Override
    protected void handleActiveCoordinator(IServer oldActive, IServer newActive) {
      if (oldActive != null) {
        oldActive.removeClientConnectionListener(BaseClusterModelPanel.this);
      }
      if (newActive != null) {
        newActive.addClientConnectionListener(BaseClusterModelPanel.this);
      }
    }

    @Override
    protected void handleUncaughtError(Exception e) {
      appContext.log(e);
    }
  }

  protected void resume() {
    removeAll();
    init();
    add(mainPanel);
    revalidate();
    repaint();
  }

  protected void suspend() {
    removeAll();
    messageLabel.setText(appContext.getString("cluster.not.ready.msg"));
    add(messagePanel);
    revalidate();
    repaint();
  }

  private XContainer createMessagePanel() {
    XContainer panel = new XContainer(new BorderLayout());
    panel.add(messageLabel = new XLabel());
    messageLabel.setText(appContext.getString("initializing"));
    messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
    messageLabel.setFont((Font) appContext.getObject("message.label.font"));
    return panel;
  }

  protected void setStatus(String text) {
    appContext.setStatus(text);
  }

  protected static void updateAction(AbstractAction action, String name, Icon icon) {
    updateAction(action, name, null, icon);
  }

  protected static void updateAction(AbstractAction action, String name, String tooltip, Icon icon) {
    action.putValue(Action.NAME, name);
    action.putValue(Action.SMALL_ICON, icon);
    if (icon != null && tooltip == null) {
      tooltip = name;
    }
    action.putValue(Action.SHORT_DESCRIPTION, tooltip);
  }

  protected void showError(String msg) {
    Frame f = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
    JOptionPane.showMessageDialog(this, new ErrorMessage(msg), f.getTitle(), JOptionPane.ERROR_MESSAGE);
  }

  protected void showError(String msg, Throwable t) {
    if (t instanceof MBeanException) {
      t = ((MBeanException) t).getCause();
    }
    if (t instanceof InvocationTargetException) {
      t = ((InvocationTargetException) t).getCause();
    }
    Frame f = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
    JOptionPane.showMessageDialog(this, new ErrorMessage(msg, t), f.getTitle(), JOptionPane.ERROR_MESSAGE);
  }

  protected static GridBagConstraints createConstraint() {
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.gridx = gbc.gridy = 0;
    return gbc;
  }

  @Override
  public void tearDown() {
    IServer activeCoord = getActiveCoordinator();
    if (activeCoord != null) {
      activeCoord.removeClientConnectionListener(this);
    }

    clusterModel.removePropertyChangeListener(clusterListener);
    clusterListener.tearDown();

    if (mainPanel != null && mainPanel.getParent() == null) {
      mainPanel.tearDown();
    }

    if (messagePanel != null && messagePanel.getParent() == null) {
      messagePanel.tearDown();
    }

    super.tearDown();
  }
}

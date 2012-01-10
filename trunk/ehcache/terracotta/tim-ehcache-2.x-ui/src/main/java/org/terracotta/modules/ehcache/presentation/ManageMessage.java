/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation;

import org.terracotta.modules.ehcache.presentation.AbstractViewPage.CacheModelInstanceControl;
import org.terracotta.modules.ehcache.presentation.TopologyPanel.Mode;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerInstance;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerModel;
import org.terracotta.modules.ehcache.presentation.model.CacheModel;
import org.terracotta.modules.ehcache.presentation.model.CacheModelInstance;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.ButtonSet;
import com.tc.admin.common.ExceptionHelper;
import com.tc.admin.common.WindowHelper;
import com.tc.admin.common.XCheckBox;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.util.concurrent.ThreadUtil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.ObjectName;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public abstract class ManageMessage extends XContainer implements ManageChannel, PropertyChangeListener {
  private final CacheManagerModel       cacheManagerModel;
  private final Frame                   frame;
  private final ApplicationContext      appContext;
  private final String                  title;

  private AbstractViewPage              currentPage;
  private ButtonSet                     pageSelector;
  private final XContainer              pageHolder;
  protected XCheckBox                   selectAllToggle;
  private XCheckBox                     applyToNewcomersToggle;
  private XLabel                        overviewLabel;
  private final String                  overviewMsg;

  private JDialog                       waitDialog;
  private XLabel                        waitMsgLabel;
  private final AtomicBoolean           hideWaitDialog       = new AtomicBoolean(false);
  private WaitDialogListener            waitDialogListener;

  protected static final ResourceBundle bundle               = ResourceBundle.getBundle(EhcacheResourceBundle.class
                                                                 .getName());

  protected static final long           MIN_WAIT_DIALOG_TIME = 1500;

  public ManageMessage(Frame frame, ApplicationContext appContext, CacheManagerModel cacheManagerModel, String phrase,
                       String overviewMsg, Mode mode) {
    super(new BorderLayout());

    this.frame = frame;
    this.appContext = appContext;
    this.cacheManagerModel = cacheManagerModel;
    this.overviewMsg = overviewMsg;
    this.title = phrase + " on " + cacheManagerModel.getName();

    XContainer topPanel = new XContainer(new BorderLayout());
    topPanel.add(createOverviewPanel(), BorderLayout.NORTH);
    topPanel.add(createViewByPanel(), BorderLayout.CENTER);
    pageHolder = new XContainer(new BorderLayout());
    currentPage = (mode == Mode.CACHE_MANAGER) ? createNodeView() : createCacheView();
    pageSelector.setSelected(mode.toString());
    pageHolder.add(currentPage);

    XContainer bottomPanel = new XContainer(new BorderLayout());
    createApplyToNewcomersToggle(bottomPanel);

    bottomPanel.add(selectAllToggle = new XCheckBox("Select/De-select All"), BorderLayout.EAST);
    selectAllToggle.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        currentPage.selectAll(selectAllToggle.isSelected());
        if (selectAllToggle.isSelected()) {
          setApplyToNewcomersToggleVisible(true);
        }
      }
    });
    selectAllToggle.setSelected(isAllSelected());

    bottomPanel.add(new XLabel("Terracotta-clustered", EhcachePresentationUtils.CLUSTERED_ICON), BorderLayout.WEST);

    add(topPanel, BorderLayout.NORTH);
    add(pageHolder, BorderLayout.CENTER);
    add(bottomPanel, BorderLayout.SOUTH);
  }

  public ManageMessage(Frame frame, ApplicationContext appContext, CacheManagerModel cacheManagerModel, String phrase,
                       String overviewMsg) {
    this(frame, appContext, cacheManagerModel, phrase, overviewMsg, Mode.CACHE_MANAGER);
  }

  private XContainer createOverviewPanel() {
    XContainer result = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.insets = new Insets(1, 5, 1, 0);
    result.add(overviewLabel = new XLabel(overviewMsg), gbc);
    overviewLabel.setIcon(EhcachePresentationUtils.ALERT_ICON);
    result.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.black));
    return result;
  }

  private XContainer createViewByPanel() {
    XContainer result = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(1, 5, 1, 0);
    result.add(new XLabel("View by: "), gbc);
    gbc.gridx++;
    gbc.insets = new Insets(1, 0, 1, 0);
    result.add(pageSelector = new ButtonSet(new FlowLayout(FlowLayout.CENTER, 0, 0)), gbc);
    JRadioButton rb = new JRadioButton("CacheManager Instances");
    rb.setName(Mode.CACHE_MANAGER.toString());
    pageSelector.add(rb);
    pageSelector.add(rb = new JRadioButton("Caches"));
    rb.setName(Mode.CACHE.toString());
    pageSelector.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String name = ((ButtonSet) e.getSource()).getSelected();
        if (!name.equals(currentPage.getName())) {
          Map<CacheModelInstance, CacheModelInstanceControl> selection = currentPage.getSelection();
          currentPage.removePropertyChangeListener(ManageMessage.this);
          pageHolder.removeAll();
          if (Mode.CACHE.toString().equals(name)) {
            currentPage = createCacheView(selection);
          } else {
            currentPage = createNodeView(selection);
          }
          pageHolder.add(currentPage);
          pageHolder.revalidate();
          pageHolder.repaint();
        }
      }
    });
    return result;
  }

  public String getTitle() {
    return title;
  }

  public XLabel getOverviewLabel() {
    return overviewLabel;
  }

  public void resetOverviewLabel() {
    overviewLabel.setText(overviewMsg);
  }

  protected void createApplyToNewcomersToggle(XContainer panel) {
    panel.add(applyToNewcomersToggle = new XCheckBox("Apply to newcomers"), BorderLayout.WEST);
    applyToNewcomersToggle.setSelected(true);
    applyToNewcomersToggle.setVisible(false);

  }

  private NodeViewPage createNodeView() {
    return createNodeView(null);
  }

  private NodeViewPage createNodeView(Map<CacheModelInstance, CacheModelInstanceControl> selection) {
    NodeViewPage result = new NodeViewPage(this, cacheManagerModel);
    result.addPropertyChangeListener(this);
    if (selection != null) {
      result.setSelection(selection);
    }
    return result;
  }

  private CacheViewPage createCacheView() {
    return createCacheView(null);
  }

  private CacheViewPage createCacheView(Map<CacheModelInstance, CacheModelInstanceControl> selection) {
    CacheViewPage result = new CacheViewPage(this, cacheManagerModel);
    result.addPropertyChangeListener(this);
    if (selection != null) {
      result.setSelection(selection);
    }
    return result;
  }

  protected void setApplyToNewcomersToggleVisible(boolean visible) {
    if (applyToNewcomersToggle != null) {
      applyToNewcomersToggle.setVisible(visible);
    }
  }

  public boolean hasApplyToNewcomersToggle() {
    return applyToNewcomersToggle != null;
  }

  public boolean shouldApplyToNewcomers() {
    return applyToNewcomersToggle != null && applyToNewcomersToggle.isSelected();
  }

  public void propertyChange(PropertyChangeEvent evt) {
    String prop = evt.getPropertyName();
    if (AbstractViewPage.ALL_SELECTED_PROP.equals(prop) || AbstractViewPage.ALL_DESELECTED_PROP.equals(prop)) {
      setApplyToNewcomersToggleVisible(true);
      selectAllToggle.setSelected(AbstractViewPage.ALL_SELECTED_PROP.equals(prop));
    } else if (AbstractViewPage.SOME_SELECTED_PROP.equals(prop)) {
      setApplyToNewcomersToggleVisible(false);
    }
  }

  public boolean isAllSelected() {
    return currentPage.isAllSelected();
  }

  public boolean isAllDeselected() {
    return currentPage.isAllDeselected();
  }

  public boolean isSomeSelected() {
    return currentPage.isSomeSelected();
  }

  protected String createApplyLabel() {
    return "OK";
  }

  /**
   * These are overridden by ManageBulkLoadingMessage to disable transactional caches.
   */
  public boolean isEnabled(CacheModelInstance cacheModel) {
    return true;
  }

  public boolean isEnabled(CacheModel cacheModel) {
    return true;
  }

  public boolean isEnabled(CacheManagerInstance cacheModel) {
    return true;
  }

  public abstract boolean getValue(CacheModelInstance cacheModelInstance);

  public abstract boolean getValue(CacheManagerInstance cacheManagerInstance);

  public abstract boolean getValue(CacheManagerModel theCacheManagerModel);

  public abstract boolean getValue(CacheModel cacheModel);

  public abstract void setValue(CacheManagerModel cacheManagerModel, boolean value, boolean applyToNewcomers);

  public abstract void setNodeViewValues(Map<CacheManagerInstance, Boolean> cacheManagerInstances,
                                         Map<CacheModelInstance, Boolean> cacheModelInstances);

  public abstract void setCacheViewValues(Map<CacheModel, Boolean> cacheModels,
                                          Map<CacheModelInstance, Boolean> cacheModelInstances);

  public CacheManagerModel getCacheManagerModel() {
    return cacheManagerModel;
  }

  public void apply(Boolean toNewcomers) {
    if (currentPage != null) {
      currentPage.apply(toNewcomers);
    }
  }

  protected void setValueImpl(final CacheManagerModel cacheManagerModel, final String prop, final boolean value,
                              final String msg) {
    showWaitDialog(msg, new Runnable() {
      public void run() {
        long startTime = System.currentTimeMillis();
        Map<CacheModelInstance, Object> attrMap = new HashMap<CacheModelInstance, Object>();
        for (CacheModelInstance cacheModelInstance : cacheManagerModel.allCacheModelInstances()) {
          attrMap.put(cacheModelInstance, value);
        }
        try {
          Map<CacheModelInstance, Exception> result = cacheManagerModel.setCacheModelInstanceAttribute(prop, attrMap);
          if (result != null && result.size() > 0) {
            reportCacheModelInstanceErrors(result);
            return;
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
        long timeTaken = System.currentTimeMillis() - startTime;
        if (timeTaken < MIN_WAIT_DIALOG_TIME) {
          ThreadUtil.reallySleep(MIN_WAIT_DIALOG_TIME - timeTaken);
        }
        hideWaitDialog(msg);
      }
    });
  }

  protected void setNodeViewValuesImpl(final Map<CacheManagerInstance, Boolean> cacheManagerInstances,
                                       final Map<CacheModelInstance, Boolean> cacheModelInstances, final String prop,
                                       final String msg) {
    showWaitDialog(msg, new Runnable() {
      public void run() {
        long startTime = System.currentTimeMillis();
        Map<CacheModelInstance, Object> attrMap = new HashMap<CacheModelInstance, Object>();
        for (CacheManagerInstance cacheManagerInstance : cacheManagerInstances.keySet()) {
          Object val = cacheManagerInstances.get(cacheManagerInstance);
          for (CacheModelInstance cmi : cacheManagerInstance.cacheModelInstances()) {
            attrMap.put(cmi, val);
          }
        }
        for (CacheModelInstance cacheModelInstance : cacheModelInstances.keySet()) {
          attrMap.put(cacheModelInstance, cacheModelInstances.get(cacheModelInstance));
        }
        try {
          Map<CacheModelInstance, Exception> result = cacheManagerModel.setCacheModelInstanceAttribute(prop, attrMap);
          if (result != null && result.size() > 0) {
            reportCacheModelInstanceErrors(result);
            return;
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
        long timeTaken = System.currentTimeMillis() - startTime;
        if (timeTaken < MIN_WAIT_DIALOG_TIME) {
          ThreadUtil.reallySleep(MIN_WAIT_DIALOG_TIME - timeTaken);
        }
        hideWaitDialog(msg);
      }
    });
  }

  protected void setCacheViewValuesImpl(final Map<CacheModel, Boolean> cacheModels,
                                        final Map<CacheModelInstance, Boolean> cacheModelInstances, final String prop,
                                        final String msg) {
    showWaitDialog(msg, new Runnable() {
      public void run() {
        long startTime = System.currentTimeMillis();
        Map<CacheModelInstance, Object> attrMap = new HashMap<CacheModelInstance, Object>();
        for (CacheModel cacheModel : cacheModels.keySet()) {
          Object val = cacheModels.get(cacheModel);
          for (CacheModelInstance cmi : cacheModel.cacheModelInstances()) {
            attrMap.put(cmi, val);
          }
        }
        for (CacheModelInstance cacheModelInstance : cacheModelInstances.keySet()) {
          attrMap.put(cacheModelInstance, cacheModelInstances.get(cacheModelInstance));
        }
        try {
          Map<CacheModelInstance, Exception> result = cacheManagerModel.setCacheModelInstanceAttribute(prop, attrMap);
          if (result != null && result.size() > 0) {
            reportCacheModelInstanceErrors(result);
            return;
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
        long timeTaken = System.currentTimeMillis() - startTime;
        if (timeTaken < MIN_WAIT_DIALOG_TIME) {
          ThreadUtil.reallySleep(MIN_WAIT_DIALOG_TIME - timeTaken);
        }
        hideWaitDialog(msg);
      }
    });
  }

  private void createWaitDialog(String msg) {
    waitDialog = new JDialog(frame, frame.getTitle(), true);
    waitDialog.getRootPane().setWindowDecorationStyle(JRootPane.INFORMATION_DIALOG);
    waitDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    XContainer panel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.insets = new Insets(5, 5, 5, 5);
    gbc.anchor = GridBagConstraints.NORTH;
    panel.add(waitMsgLabel = new XLabel("<html>" + msg + "</html>"), gbc);
    gbc.gridy++;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weighty = 1.0;
    panel.add(new XLabel(), gbc); // filler
    // waitMsgLabel.setFont((Font) appContext.getObject("message.label.font"));
    waitDialog.getContentPane().add(new JScrollPane(panel));
    waitDialog.pack();
    waitDialog.setSize(450, 150);
    WindowHelper.center(waitDialog, frame);
    waitDialog.addHierarchyListener(waitDialogListener = new WaitDialogListener());
  }

  private class WaitDialogListener implements HierarchyListener {
    Runnable runner;

    public void hierarchyChanged(HierarchyEvent e) {
      if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
        if (waitDialog.isShowing()) {
          if (runner != null) {
            appContext.submit(runner);
          }
        }
      }
    }
  }

  private void setMsgText(String msg) {
    waitMsgLabel.setText("<html>" + msg + "</html>");
    waitMsgLabel.paintImmediately(0, 0, waitMsgLabel.getWidth(), waitMsgLabel.getHeight());
  }

  protected void reportErrors(final Map<ObjectName, Exception> result) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        StringBuilder sb = new StringBuilder("<p>There were errors:</p><table border=1 cellspacing=1 cellpadding=1>");
        for (ObjectName on : result.keySet()) {
          Throwable t = ExceptionHelper.getRootCause(result.get(on));
          appContext.log(t);
          sb.append("<tr><td>");
          sb.append(on.getKeyProperty("name"));
          sb.append(":");
          sb.append("</td><td>");
          sb.append(t.getLocalizedMessage());
          sb.append("</td></tr>");
        }
        sb.append("</table>");
        setMsgText(sb.toString());
        JButton btn = new JButton("Close");
        btn.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ae) {
            waitDialog.setVisible(false);
          }
        });
        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.add(btn);
        waitDialog.getContentPane().add(btnPanel, BorderLayout.SOUTH);
        waitDialog.getRootPane().setWindowDecorationStyle(JRootPane.ERROR_DIALOG);
        waitDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        Dimension size = waitDialog.getSize();
        waitDialog.pack();
        waitDialog.setSize(size);
      }
    });
  }

  protected void reportCacheModelInstanceErrors(final Map<CacheModelInstance, Exception> result) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        StringBuilder sb = new StringBuilder("<p>There were errors:</p><table border=1 cellspacing=1 cellpadding=1>");
        for (CacheModelInstance cmi : result.keySet()) {
          Throwable t = ExceptionHelper.getRootCause(result.get(cmi));
          appContext.log(t);
          sb.append("<tr><td>");
          sb.append(cmi);
          sb.append(":");
          sb.append("</td><td width='300'><p>");
          String msg = t.getLocalizedMessage();
          if (msg == null) {
            msg = t.getClass().getSimpleName();
          }
          sb.append(msg);
          sb.append("</p></td></tr>");
        }
        sb.append("</table>");
        setMsgText(sb.toString());
        JButton btn = new JButton("Close");
        btn.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ae) {
            waitDialog.setVisible(false);
          }
        });
        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.add(btn);
        waitDialog.getContentPane().add(btnPanel, BorderLayout.SOUTH);
        waitDialog.getRootPane().setWindowDecorationStyle(JRootPane.ERROR_DIALOG);
        waitDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        Dimension size = waitDialog.getSize();
        waitDialog.pack();
        waitDialog.setSize(size);
      }
    });
  }

  protected void showWaitDialog(final String msg, final Runnable runner) {
    hideWaitDialog.set(false);
    if (waitDialog == null) {
      createWaitDialog(msg);
    } else {
      setMsgText(msg);
    }
    appContext.setStatus(msg);
    waitDialogListener.runner = runner;
    waitDialog.setVisible(true);
  }

  protected void hideWaitDialog(final String msg) {
    hideWaitDialog.set(true);
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        appContext.setStatus("");
        if (hideWaitDialog.getAndSet(false)) {
          waitDialog.setVisible(false);
        }
      }
    });
  }

  @Override
  public void tearDown() {
    if (currentPage != null) {
      currentPage.removePropertyChangeListener(this);
    }

    if (waitDialog != null) {
      waitDialog.setVisible(false);
    }

    super.tearDown();
  }
}

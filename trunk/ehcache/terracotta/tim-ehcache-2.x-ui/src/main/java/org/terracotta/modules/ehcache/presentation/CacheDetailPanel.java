/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.ehcache.presentation;

import static org.terracotta.modules.ehcache.presentation.EhcachePresentationUtils.BULK_LOAD_DISABLED_ICON;
import static org.terracotta.modules.ehcache.presentation.EhcachePresentationUtils.BULK_LOAD_ENABLED_ICON;

import org.terracotta.modules.ehcache.presentation.model.SettingsCacheModel;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.ExceptionHelper;
import com.tc.admin.common.XCheckBox;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XSpinner;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultFormatter;

// TODO: remove this

public class CacheDetailPanel extends XContainer implements PropertyChangeListener {
  private final ApplicationContext    appContext;
  private SettingsCacheModel          cacheModel;
  private TitledBorder                titledBorder;
  private XSpinner                    tti;
  private XSpinner                    ttl;
  private XSpinner                    maxEntriesLocalDisk;
  private XSpinner                    maxEntriesLocalHeap;
  private BulkLoadControlAction       enableBulkLoadAction;
  private BulkLoadControlAction       disableBulkLoadAction;
  private XCheckBox                   loggingToggle;

  private static final ResourceBundle bundle = ResourceBundle.getBundle(EhcacheResourceBundle.class.getName());

  public CacheDetailPanel(final ApplicationContext appContext) {
    super(new BorderLayout());

    this.appContext = appContext;

    add(createOperationsPanel(), BorderLayout.NORTH);
    add(createSettingsPanel(), BorderLayout.CENTER);

    setBorder(titledBorder = BorderFactory.createTitledBorder(""));
  }

  private EhcacheToolBar createOperationsPanel() {
    EhcacheToolBar toolBar = new EhcacheToolBar();

    toolBar.add(enableBulkLoadAction = new BulkLoadControlAction(true));
    toolBar.add(disableBulkLoadAction = new BulkLoadControlAction(false));

    return toolBar;
  }

  private XContainer createSettingsPanel() {
    XContainer panel = new XContainer(new GridBagLayout());
    Insets labelInsets = new Insets(0, 2, 0, 2);
    Insets fieldInsets = new Insets(0, 2, 3, 2);
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = labelInsets;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;

    XLabel label = new XLabel(bundle.getString("tti"));
    panel.add(label, gbc);
    gbc.gridy++;

    gbc.insets = fieldInsets;
    panel.add(tti = createIntegerField(), gbc);
    tti.addChangeListener(new ChangeListener() {
      public void stateChanged(final ChangeEvent e) {
        setTTI();
      }
    });
    new IntegerFieldDocumentListener(tti);
    gbc.gridy--;
    gbc.gridx++;

    gbc.insets = labelInsets;
    label = new XLabel(bundle.getString("target.max.total.count"));
    panel.add(label, gbc);
    gbc.gridy++;

    gbc.insets = fieldInsets;
    panel.add(maxEntriesLocalDisk = createIntegerField(), gbc);
    maxEntriesLocalDisk.addChangeListener(new ChangeListener() {
      public void stateChanged(final ChangeEvent e) {
        setMaxEntriesLocalDisk();
      }
    });
    new IntegerFieldDocumentListener(maxEntriesLocalDisk);
    gbc.gridx = 0;
    gbc.gridy++;

    gbc.insets = labelInsets;
    panel.add(label = new XLabel(bundle.getString("ttl")), gbc);
    gbc.gridy++;

    gbc.insets = fieldInsets;
    panel.add(ttl = createIntegerField(), gbc);
    ttl.addChangeListener(new ChangeListener() {
      public void stateChanged(final ChangeEvent e) {
        setTTL();
      }
    });
    new IntegerFieldDocumentListener(ttl);
    gbc.gridy--;
    gbc.gridx++;

    gbc.insets = labelInsets;
    panel.add(label = new XLabel(bundle.getString("target.max.in-memory.count")), gbc);
    gbc.gridy++;

    gbc.insets = fieldInsets;
    panel.add(maxEntriesLocalHeap = createIntegerField(), gbc);
    maxEntriesLocalHeap.addChangeListener(new ChangeListener() {
      public void stateChanged(final ChangeEvent e) {
        setMaxEntriesLocalHeap();
      }
    });
    new IntegerFieldDocumentListener(maxEntriesLocalHeap);
    gbc.gridy++;
    gbc.gridx = 0;

    gbc.insets = labelInsets;
    panel.add(loggingToggle = new XCheckBox(bundle.getString("logging.enabled")), gbc);
    loggingToggle.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent ae) {
        cacheModel.setLoggingEnabled(loggingToggle.isSelected());
      }
    });

    XContainer result = new XContainer(new BorderLayout());
    result.add(panel, BorderLayout.WEST);
    result.setBorder(BorderFactory.createTitledBorder(bundle.getString("cache.settings")));

    return result;
  }

  private void queryEnableBulkLoad() {
    XLabel msg = new XLabel(MessageFormat.format(bundle.getString("enable.cache.bulkload.confirm"),
                                                 cacheModel.getCacheName()));
    Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
    int answer = JOptionPane.showConfirmDialog(this.getParent(), msg, frame.getTitle(), JOptionPane.OK_CANCEL_OPTION);
    if (answer == JOptionPane.OK_OPTION) {
      appContext.submit(new BulkLoadControlWorker(true));
    }
  }

  private void queryDisableBulkLoad() {
    XLabel msg = new XLabel(MessageFormat.format(bundle.getString("disable.cache.bulkload.confirm"),
                                                 cacheModel.getCacheName()));
    Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
    int answer = JOptionPane.showConfirmDialog(this.getParent(), msg, frame.getTitle(), JOptionPane.OK_CANCEL_OPTION);
    if (answer == JOptionPane.OK_OPTION) {
      appContext.submit(new BulkLoadControlWorker(false));
    }
  }

  private class BulkLoadControlWorker extends BasicWorker<Void> {
    AbstractAction action;

    private BulkLoadControlWorker(final boolean enable) {
      super(new Callable<Void>() {
        public Void call() throws Exception {
          cacheModel.setBulkLoadEnabled(enable);
          return null;
        }
      });
      action = enable ? enableBulkLoadAction : disableBulkLoadAction;
      action.setEnabled(false);
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
      action.setEnabled(true);
    }
  }

  private class BulkLoadControlAction extends AbstractAction {
    private final boolean enable;

    BulkLoadControlAction(boolean enable) {
      super();
      this.enable = enable;
      putValue(NAME, bundle.getString(enable ? "enable.bulkload" : "disable.bulkload"));
      putValue(SHORT_DESCRIPTION, bundle.getString(enable ? "enable.bulkload.tip" : "disable.bulkload.tip"));
      putValue(SMALL_ICON, enable ? BULK_LOAD_DISABLED_ICON : BULK_LOAD_ENABLED_ICON);
    }

    public void actionPerformed(ActionEvent ae) {
      if (enable) {
        queryEnableBulkLoad();
      } else {
        queryDisableBulkLoad();
      }
    }
  }

  public SettingsCacheModel setCacheModel(final SettingsCacheModel cacheModel) {
    SettingsCacheModel oldCacheModel = this.cacheModel;
    if (oldCacheModel != cacheModel) {
      if ((this.cacheModel = cacheModel) != null) {
        cacheModel.addPropertyChangeListener(this);
        refreshUI();
        revalidate();
        repaint();
      }
    }
    return oldCacheModel;
  }

  private void refreshUI() {
    titledBorder.setTitle(cacheModel.getCacheName());
    tti.getModel().setValue(cacheModel.getTimeToIdleSeconds());
    ttl.getModel().setValue(cacheModel.getTimeToLiveSeconds());
    maxEntriesLocalDisk.getModel().setValue(cacheModel.getMaxEntriesLocalDisk());
    maxEntriesLocalHeap.getModel().setValue(cacheModel.getMaxEntriesLocalHeap());
    loggingToggle.setSelected(cacheModel.isLoggingEnabled());
  }

  public void updateCache() {
    setCacheModel(this.cacheModel);
  }

  private void setTTI() {
    if (cacheModel != null) {
      cacheModel.setTimeToIdleSeconds(((Number) tti.getValue()).intValue());
    }
  }

  private void setTTL() {
    if (cacheModel != null) {
      cacheModel.setTimeToLiveSeconds(((Number) ttl.getValue()).intValue());
    }
  }

  private void setMaxEntriesLocalDisk() {
    if (cacheModel != null) {
      cacheModel.setMaxEntriesLocalDisk(((Number) maxEntriesLocalDisk.getValue()).intValue());
    }
  }

  private void setMaxEntriesLocalHeap() {
    if (cacheModel != null) {
      cacheModel.setMaxEntriesLocalHeap(((Number) maxEntriesLocalHeap.getValue()).intValue());
    }
  }

  public void propertyChange(PropertyChangeEvent evt) {
    refreshUI();
  }

  private static XSpinner createIntegerField() {
    final XSpinner spinner = new XSpinner();
    JTextField editor = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
    editor.setColumns(10);

    spinner.setUI(new javax.swing.plaf.basic.BasicSpinnerUI() {
      @Override
      protected Component createNextButton() {
        Component c = new JButton();
        c.setPreferredSize(new Dimension(0, 0));
        c.setFocusable(false);
        return c;
      }

      @Override
      protected Component createPreviousButton() {
        Component c = new JButton();
        c.setPreferredSize(new Dimension(0, 0));
        c.setFocusable(false);
        return c;
      }
    });

    spinner.setBorder(null);
    ((DefaultFormatter) ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().getFormatter())
        .setAllowsInvalid(false);
    ((JComponent) editor.getParent()).setBorder(UIManager.getBorder("TextField.border"));

    return spinner;
  }

  private static class IntegerFieldDocumentListener implements DocumentListener, ChangeListener {
    private final XSpinner field;
    private final Timer    timer;

    private IntegerFieldDocumentListener(XSpinner field) {
      this.field = field;
      JTextField editor = ((JSpinner.DefaultEditor) field.getEditor()).getTextField();
      editor.getDocument().addDocumentListener(this);
      timer = new Timer(750, new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          try {
            getField().commitEdit();
          } catch (ParseException pe) {
            /**/
          }
        }
      });
      timer.setRepeats(false);

      SpinnerNumberModel numberModel = (SpinnerNumberModel) field.getModel();
      numberModel.setMinimum(-1);
      numberModel.setMaximum(Integer.MAX_VALUE);
      numberModel.setStepSize(1000);

      field.addChangeListener(this);
    }

    private XSpinner getField() {
      return field;
    }

    public void changedUpdate(DocumentEvent e) {
      /**/
    }

    public void insertUpdate(DocumentEvent e) {
      timer.restart();
    }

    public void removeUpdate(DocumentEvent e) {
      timer.restart();
    }

    public void stateChanged(final ChangeEvent e) {
      timer.stop();
    }
  }
}

/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.hibernatecache.presentation;

import static org.terracotta.modules.hibernatecache.presentation.CacheRegionUtils.CLEAR_CACHE_ICON;
import static org.terracotta.modules.hibernatecache.presentation.CacheRegionUtils.DISABLE_CACHE_ICON;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.BasicWorker;
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
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
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

public class CacheRegionDetailPanel extends XContainer {
  private final ApplicationContext    appContext;
  private CacheRegionInfo             regionInfo;
  private TitledBorder                titledBorder;
  private XSpinner                    tti;
  private XSpinner                    ttl;
  private XSpinner                    targetMaxTotalCount;
  private XSpinner                    targetMaxInMemoryCount;
  private JToggleButton               enablementButton;
  private JToggleButton               flushButton;
  private XCheckBox                   loggingToggle;

  private static final ResourceBundle bundle = ResourceBundle.getBundle(HibernateResourceBundle.class.getName());

  public CacheRegionDetailPanel(final ApplicationContext appContext) {
    super(new BorderLayout());

    this.appContext = appContext;

    add(createOperationsPanel(), BorderLayout.NORTH);
    add(createSettingsPanel(), BorderLayout.CENTER);

    setBorder(titledBorder = BorderFactory.createTitledBorder(""));
  }

  private XContainer createOperationsPanel() {
    XContainer panel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(1, 3, 1, 3);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;

    panel.add(enablementButton = new JToggleButton(bundle.getString("disable.region"), DISABLE_CACHE_ICON), gbc);
    enablementButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent ae) {
        queryToggleRegionEnabled();
      }
    });
    gbc.gridx++;

    panel.add(flushButton = new JToggleButton(bundle.getString("evict.all.entries.in.region"), CLEAR_CACHE_ICON), gbc);
    flushButton.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent ae) {
        queryFlushRegion();
      }
    });
    gbc.gridx++;

    // Filler
    gbc.weightx = 1.0;
    panel.add(new XLabel(), gbc);

    return panel;
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
    panel.add(targetMaxTotalCount = createIntegerField(), gbc);
    targetMaxTotalCount.addChangeListener(new ChangeListener() {
      public void stateChanged(final ChangeEvent e) {
        setMaxGlobalEntries();
      }
    });
    new IntegerFieldDocumentListener(targetMaxTotalCount);
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
    panel.add(targetMaxInMemoryCount = createIntegerField(), gbc);
    targetMaxInMemoryCount.addChangeListener(new ChangeListener() {
      public void stateChanged(final ChangeEvent e) {
        setMaxLocalEntries();
      }
    });
    new IntegerFieldDocumentListener(targetMaxInMemoryCount);
    gbc.gridy++;
    gbc.gridx = 0;

    gbc.insets = labelInsets;
    panel.add(loggingToggle = new XCheckBox(bundle.getString("logging.enabled")), gbc);
    loggingToggle.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent ae) {
        regionInfo.setLoggingEnabled(loggingToggle.isSelected());
      }
    });

    XContainer result = new XContainer(new BorderLayout());
    result.add(panel, BorderLayout.WEST);
    result.setBorder(BorderFactory.createTitledBorder(bundle.getString("region.settings")));

    return result;
  }

  private void queryFlushRegion() {
    XLabel msg = new XLabel(MessageFormat.format(bundle.getString("evict.all.entries.in.region.confirm"),
                                                 regionInfo.getRegionName()));
    Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
    int answer = JOptionPane.showConfirmDialog(this.getParent(), msg, frame.getTitle(), JOptionPane.OK_CANCEL_OPTION);
    if (answer == JOptionPane.OK_OPTION) {
      appContext.submit(new FlushRegionWorker());
    } else {
      flushButton.setSelected(false);
      flushButton.setEnabled(true);
    }
  }

  private class FlushRegionWorker extends BasicWorker<Void> {
    private FlushRegionWorker() {
      super(new Callable<Void>() {
        public Void call() throws Exception {
          regionInfo.flush();
          return null;
        }
      });
      flushButton.setEnabled(false);
    }

    @Override
    protected void finished() {
      Exception e = getException();
      if (e != null) {
        appContext.log(e);
      }
      flushButton.setSelected(false);
      flushButton.setEnabled(true);
    }
  }

  private void queryToggleRegionEnabled() {
    String msgKey = regionInfo.isEnabled() ? "disable.region.confirm" : "enable.region.confirm";
    XLabel label = new XLabel(MessageFormat.format(bundle.getString(msgKey), regionInfo.getRegionName()));
    XCheckBox cb = null;
    Object msg = label;
    if (regionInfo.isEnabled()) {
      XContainer panel = new XContainer(new GridBagLayout());
      cb = new XCheckBox("Flush Region");
      cb.setSelected(true);
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.gridx = gbc.gridy = 0;
      gbc.insets = new Insets(3, 3, 3, 3);
      gbc.anchor = GridBagConstraints.WEST;
      panel.add(label, gbc);
      gbc.gridy++;
      panel.add(cb, gbc);
      msg = panel;
    }
    Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
    int answer = JOptionPane.showConfirmDialog(this.getParent(), msg, frame.getTitle(), JOptionPane.OK_CANCEL_OPTION);
    if (answer == JOptionPane.OK_OPTION) {
      appContext.submit(new ToggleEnabledWorker(cb != null ? cb.isSelected() : false));
    } else {
      enablementButton.setSelected(false);
      enablementButton.setEnabled(true);
    }
  }

  private class ToggleEnabledWorker extends BasicWorker<Boolean> {
    private ToggleEnabledWorker(final boolean flushRegion) {
      super(new Callable<Boolean>() {
        public Boolean call() throws Exception {
          boolean currState = regionInfo.isEnabled();
          regionInfo.setEnabled(!currState);
          if (currState && flushRegion) {
            regionInfo.flush();
          }
          return regionInfo.isEnabled();
        }
      });
      enablementButton.setEnabled(false);
    }

    @Override
    protected void finished() {
      boolean finalState;
      Exception e = getException();
      if (e != null) {
        appContext.log(e);
        finalState = regionInfo.isEnabled();
      } else {
        finalState = getResult();
      }
      enablementButton.setText(bundle.getString(finalState ? "disable.region" : "enable.region"));
      enablementButton.setSelected(false);
      enablementButton.setEnabled(true);
    }
  }

  public void setCacheRegion(final CacheRegionInfo regionInfo) {
    if ((this.regionInfo = regionInfo) != null) {
      titledBorder.setTitle(regionInfo.getRegionName());
      tti.getModel().setValue(regionInfo.getTTI());
      ttl.getModel().setValue(regionInfo.getTTL());
      targetMaxTotalCount.getModel().setValue(regionInfo.getTargetMaxTotalCount());
      targetMaxInMemoryCount.getModel().setValue(regionInfo.getTargetMaxInMemoryCount());
      loggingToggle.setSelected(regionInfo.isLoggingEnabled());
      enablementButton.setText(bundle.getString(regionInfo.isEnabled() ? "disable.region" : "enable.region"));

      revalidate();
      repaint();
    }
  }

  public void updateRegion() {
    setCacheRegion(this.regionInfo);
  }

  private void setTTI() {
    if (regionInfo != null) {
      regionInfo.setTTI(((Number) tti.getValue()).intValue());
    }
  }

  private void setTTL() {
    if (regionInfo != null) {
      regionInfo.setTTL(((Number) ttl.getValue()).intValue());
    }
  }

  private void setMaxGlobalEntries() {
    if (regionInfo != null) {
      regionInfo.setTargetMaxTotalCount(((Number) targetMaxTotalCount.getValue()).intValue());
    }
  }

  private void setMaxLocalEntries() {
    if (regionInfo != null) {
      regionInfo.setTargetMaxInMemoryCount(((Number) targetMaxInMemoryCount.getValue()).intValue());
    }
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

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation;

import org.apache.commons.lang.StringUtils;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerInstance;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerModel;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerModelListener;
import org.terracotta.modules.ehcache.presentation.model.CacheModel;
import org.terracotta.modules.ehcache.presentation.model.CacheModelInstance;
import org.terracotta.modules.ehcache.presentation.model.ClusteredCacheModel;
import org.terracotta.modules.ehcache.presentation.model.SettingsCacheModel;
import org.terracotta.modules.ehcache.presentation.model.StandaloneCacheModel;

import com.tc.admin.common.ExceptionHelper;
import com.tc.admin.common.XComboBox;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XTextField;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class CacheModelMessage extends XContainer implements CacheManagerModelListener {
  private final CacheManagerModel    cacheManagerModel;

  private XComboBox                  cacheManagerInstanceSelector;
  private ActionListener             cacheManagerInstanceSelectorListener;
  private CacheManagerInstance       lastSelectedCacheManagerInstance;
  private XComboBox                  cacheSettingsInstanceSelector;
  private ActionListener             cacheSettingsInstanceSelectorListener;
  private CacheModelInstanceWrapper  lastSelectedCacheSettingsInstance;
  private final DefaultComboBoxModel cacheSettingsComboModel;
  private XLabel                     errorLabel;
  private XContainer                 cacheSettingsPanel;

  private StringField                maxBytesLocalDiskField;
  private StringField                maxBytesLocalHeapField;
  private StringField                maxBytesLocalOffHeapField;
  private LongField                  maxEntriesLocalHeapField;
  private LongField                  maxEntriesLocalDiskField;
  private LongField                  ttiField;
  private LongField                  ttlField;
  private IntegerField               maxElementsInCacheField;

  private static final Icon          ERROR_ICON       = EhcachePresentationUtils.ALERT_ICON;
  private static final Icon          BLANK_ICON       = EhcachePresentationUtils.BLANK_ICON;
  private static final String        EMPTY_ERROR_TEXT = "No Error";

  public CacheModelMessage(CacheManagerModel cacheManagerModel) {
    super(new BorderLayout());

    this.cacheManagerModel = cacheManagerModel;

    XContainer panel = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.gridx = gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(new XLabel("Cache Manager Instance:"), gbc);
    gbc.gridx++;
    Set<CacheManagerInstance> cmis = new HashSet<CacheManagerInstance>();
    for (CacheManagerInstance cmi : cacheManagerModel.cacheManagerInstances()) {
      cmis.add(cmi);
    }
    panel.add(cacheManagerInstanceSelector = new XComboBox(new DefaultComboBoxModel(cmis
                  .toArray(new CacheManagerInstance[0]))), gbc);
    cacheManagerInstanceSelector.addActionListener(cacheManagerInstanceSelectorListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            cacheManagerInstanceSelector.removeActionListener(cacheManagerInstanceSelectorListener);
            cacheSettingsInstanceSelector.removeActionListener(cacheSettingsInstanceSelectorListener);

            if (canClose()) {
              cacheSettingsComboModel.removeAllElements();
              lastSelectedCacheManagerInstance = (CacheManagerInstance) cacheManagerInstanceSelector.getSelectedItem();
              List<CacheModelInstanceWrapper> list = getSettingsList(lastSelectedCacheManagerInstance);
              for (CacheModelInstanceWrapper cmiw : list) {
                cacheSettingsComboModel.addElement(cmiw);
              }
              setupCacheSettingsPanel();
            } else {
              cacheManagerInstanceSelector.setSelectedItem(lastSelectedCacheManagerInstance);
            }

            cacheManagerInstanceSelector.addActionListener(cacheManagerInstanceSelectorListener);
            cacheSettingsInstanceSelector.addActionListener(cacheSettingsInstanceSelectorListener);
          }
        });
      }
    });
    lastSelectedCacheManagerInstance = (CacheManagerInstance) cacheManagerInstanceSelector.getSelectedItem();
    gbc.gridx--;
    gbc.gridy++;
    panel.add(new XLabel("Cache Instance:"), gbc);
    gbc.gridx++;
    List<CacheModelInstanceWrapper> list = getSettingsList((CacheManagerInstance) cacheManagerInstanceSelector
        .getSelectedItem());
    cacheSettingsComboModel = new DefaultComboBoxModel(list.toArray(new CacheModelInstanceWrapper[0]));
    panel.add(cacheSettingsInstanceSelector = new XComboBox(cacheSettingsComboModel), gbc);
    cacheSettingsInstanceSelector.addActionListener(cacheSettingsInstanceSelectorListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            cacheManagerInstanceSelector.removeActionListener(cacheManagerInstanceSelectorListener);
            cacheSettingsInstanceSelector.removeActionListener(cacheSettingsInstanceSelectorListener);

            if (canClose()) {
              lastSelectedCacheSettingsInstance = (CacheModelInstanceWrapper) cacheSettingsInstanceSelector
                  .getSelectedItem();
              setupCacheSettingsPanel();
            } else {
              cacheSettingsInstanceSelector.setSelectedItem(lastSelectedCacheSettingsInstance);
            }

            cacheManagerInstanceSelector.addActionListener(cacheManagerInstanceSelectorListener);
            cacheSettingsInstanceSelector.addActionListener(cacheSettingsInstanceSelectorListener);
          }
        });
      }
    });
    lastSelectedCacheSettingsInstance = (CacheModelInstanceWrapper) cacheSettingsInstanceSelector.getSelectedItem();
    gbc.gridx--;
    gbc.gridy++;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    panel.add(errorLabel = new XLabel(), gbc);
    setErrorText(null);
    gbc.gridy++;
    gbc.gridheight = GridBagConstraints.REMAINDER;
    panel.add(cacheSettingsPanel = new XContainer(new GridBagLayout()), gbc);
    setupCacheSettingsPanel();

    add(panel);

    cacheManagerModel.addCacheManagerModelListener(this);
  }

  private CacheModelInstance getSelectedCacheModelInstance() {
    CacheModelInstanceWrapper wrapper = (CacheModelInstanceWrapper) cacheSettingsInstanceSelector.getSelectedItem();
    return (wrapper != null ? wrapper.cmi : null);
  }

  private void setupCacheSettingsPanel() {
    setup(getSelectedCacheModelInstance());
  }

  private CacheManagerInstance getSelectedCacheManagerInstance() {
    return (CacheManagerInstance) cacheManagerInstanceSelector.getSelectedItem();
  }

  private XLabel newRightAdjustedLabel(String text) {
    XLabel label = new XLabel(text);
    label.setHorizontalAlignment(SwingConstants.LEFT);
    label.setHorizontalTextPosition(SwingConstants.TRAILING);
    label.setIcon(BLANK_ICON);
    return label;
  }

  private void setup(final CacheModelInstance cmi) {
    cacheSettingsPanel.removeAll();

    if (cmi != null) {
      boolean isClustered = cmi.isTerracottaClustered();
      CacheManagerInstance cacheManagerInstance = getSelectedCacheManagerInstance();
      SettingsCacheModel scm = cacheManagerInstance.getSettingsCacheModel(cmi);
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.insets = new Insets(3, 3, 3, 3);
      gbc.gridx = gbc.gridy = 0;
      gbc.weightx = 1.0;
      gbc.anchor = GridBagConstraints.WEST;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      XLabel label;

      if (isClustered) {
        gbc.weightx = 0.0;
        cacheSettingsPanel.add(label = newRightAdjustedLabel("MaxElementsInCache:"), gbc);
        gbc.gridx++;
        gbc.weightx = 1.0;
        cacheSettingsPanel.add(maxElementsInCacheField = new IntegerField(cmi, "MaxElementsOnDisk", (int) scm
                                   .getMaxEntriesLocalDisk()), gbc);
        label.setLabelFor(maxElementsInCacheField);
        gbc.gridx--;
        gbc.gridy++;
      }

      if (cmi.getMaxEntriesLocalHeap() > 0 || (!isClustered && cmi.getMaxEntriesLocalDisk() > 0)) {
        gbc.weightx = 0.0;
        cacheSettingsPanel.add(label = newRightAdjustedLabel("MaxEntriesLocalHeap:"), gbc);
        gbc.gridx++;
        gbc.weightx = 1.0;
        cacheSettingsPanel.add(maxEntriesLocalHeapField = new LongField(cmi, "MaxEntriesLocalHeap", cmi
                                   .getMaxEntriesLocalHeap()), gbc);
        label.setLabelFor(maxEntriesLocalHeapField);
        gbc.gridx--;
        gbc.gridy++;
        if (!isClustered && cmi.getMaxEntriesLocalDisk() > 0) {
          gbc.weightx = 0.0;
          cacheSettingsPanel.add(label = newRightAdjustedLabel("MaxEntriesLocalDisk:"), gbc);
          gbc.gridx++;
          gbc.weightx = 1.0;
          cacheSettingsPanel.add(maxEntriesLocalDiskField = new LongField(cmi, "MaxEntriesLocalDisk", cmi
                                     .getMaxEntriesLocalDisk()), gbc);
          label.setLabelFor(maxEntriesLocalDiskField);
          gbc.gridx--;
          gbc.gridy++;
        }
      }
      if (cmi.getMaxEntriesLocalHeap() <= 0
          && (cacheManagerInstance.getMaxBytesLocalHeap() <= 0 || cmi.getMaxBytesLocalHeap() > 0)) {
        gbc.weightx = 0.0;
        cacheSettingsPanel.add(label = newRightAdjustedLabel("MaxBytesLocalHeap:"), gbc);
        gbc.gridx++;
        gbc.weightx = 1.0;
        cacheSettingsPanel.add(maxBytesLocalHeapField = new StringField(cmi, "MaxBytesLocalHeapAsString", cmi
                                   .getMaxBytesLocalHeapAsString()), gbc);
        label.setLabelFor(maxBytesLocalHeapField);
        gbc.gridx--;
        gbc.gridy++;
      }
      if (!isClustered && cmi.getMaxEntriesLocalDisk() <= 0
          && (cacheManagerInstance.getMaxBytesLocalDisk() <= 0 || cmi.getMaxBytesLocalDisk() > 0)) {
        gbc.weightx = 0.0;
        cacheSettingsPanel.add(label = newRightAdjustedLabel("MaxBytesLocalDisk:"), gbc);
        gbc.gridx++;
        gbc.weightx = 1.0;
        cacheSettingsPanel.add(maxBytesLocalDiskField = new StringField(cmi, "MaxBytesLocalDiskAsString", cmi
                                   .getMaxBytesLocalDiskAsString()), gbc);
        label.setLabelFor(maxBytesLocalDiskField);
        gbc.gridx--;
        gbc.gridy++;
      }

      gbc.weightx = 0.0;
      cacheSettingsPanel.add(label = newRightAdjustedLabel("TTI:"), gbc);
      gbc.gridx++;
      long tti = isClustered ? scm.getTimeToIdleSeconds() : cmi.getTimeToIdleSeconds();
      gbc.weightx = 1.0;
      cacheSettingsPanel.add(ttiField = new LongField(cmi, "TimeToIdleSeconds", tti), gbc);
      label.setLabelFor(ttiField);
      gbc.gridx--;
      gbc.gridy++;
      gbc.weightx = 0.0;
      cacheSettingsPanel.add(label = newRightAdjustedLabel("TTL:"), gbc);
      gbc.gridx++;
      long ttl = isClustered ? scm.getTimeToLiveSeconds() : cmi.getTimeToLiveSeconds();
      gbc.weightx = 1.0;
      cacheSettingsPanel.add(ttlField = new LongField(cmi, "TimeToLiveSeconds", ttl), gbc);
      label.setLabelFor(ttlField);

      cacheSettingsPanel.setBorder(BorderFactory.createTitledBorder(cmi.toString()));
    }

    cacheSettingsPanel.revalidate();
    cacheSettingsPanel.repaint();
  }

  private void update(final CacheModelInstance cmi) {
    if (cmi != null) {
      boolean isClustered = cmi.isTerracottaClustered();
      CacheManagerInstance cacheManagerInstance = getSelectedCacheManagerInstance();
      SettingsCacheModel scm = cacheManagerInstance.getSettingsCacheModel(cmi);

      if (isClustered) {
        maxElementsInCacheField.setValue((int) scm.getMaxEntriesLocalDisk());
      }

      if (cmi.getMaxEntriesLocalHeap() > 0 || (!isClustered && cmi.getMaxEntriesLocalDisk() > 0)) {
        maxEntriesLocalHeapField.setValue(cmi.getMaxEntriesLocalHeap());
        if (!isClustered) {
          maxEntriesLocalDiskField.setValue(cmi.getMaxEntriesLocalDisk());
        }
      } else {
        if (cacheManagerInstance.getMaxBytesLocalHeap() <= 0 || cmi.getMaxBytesLocalHeap() > 0) {
          maxBytesLocalHeapField.setValue(cmi.getMaxBytesLocalHeapAsString());
        }
        if (!isClustered && (cacheManagerInstance.getMaxBytesLocalDisk() <= 0 || cmi.getMaxBytesLocalDisk() > 0)) {
          maxBytesLocalDiskField.setValue(cmi.getMaxBytesLocalDiskAsString());
        }
      }

      long tti = isClustered ? scm.getTimeToIdleSeconds() : cmi.getTimeToIdleSeconds();
      ttiField.setValue(tti);

      long ttl = isClustered ? scm.getTimeToLiveSeconds() : cmi.getTimeToLiveSeconds();
      ttlField.setValue(ttl);
    }
  }

  private class StringField extends XTextField {
    private final CacheModelInstance cmi;
    private final String             attribute;
    private String                   initialValue;

    StringField(CacheModelInstance cmi, String attribute, String value) {
      super(value);

      this.cmi = cmi;
      this.attribute = attribute;
      this.initialValue = value;
    }

    Object getValue() throws Exception {
      return getText();
    }

    void setValue(String value) {
      if (!getText().equals(value)) {
        setText(initialValue = value);
      }
    }

    boolean hasError() {
      return !StringUtils.isEmpty(getToolTipText());
    }

    boolean isChanged() {
      return !initialValue.equals(getText());
    }

    boolean isChangedOrHasError() {
      return isChanged() || hasError();
    }

    boolean testApply() {
      return isChangedOrHasError() ? apply() : true;
    }

    boolean apply() {
      CacheManagerInstance cacheManagerInstance = getSelectedCacheManagerInstance();

      initialValue = getText();
      try {
        cacheManagerInstance.setAttribute(cmi.getBeanName(), attribute, getValue());
        setToolTipText(null);
        return true;
      } catch (Exception e) {
        Throwable t = ExceptionHelper.getRootCause(e);
        setToolTipText(t.getMessage());
        return false;
      }
    }
  }

  private class LongField extends StringField {
    LongField(CacheModelInstance cmi, String attribute, long value) {
      super(cmi, attribute, Long.toString(value));
    }

    void setValue(long value) {
      super.setValue(Long.toString(value));
    }

    @Override
    Object getValue() throws Exception {
      try {
        return Long.parseLong(getText());
      } catch (NumberFormatException nfe) {
        throw new IllegalArgumentException("Cannot be converted to a long");
      }
    }
  }

  private class IntegerField extends StringField {
    IntegerField(CacheModelInstance cmi, String attribute, int value) {
      super(cmi, attribute, Integer.toString(value));
    }

    void setValue(int value) {
      super.setValue(Integer.toString(value));
    }

    @Override
    Object getValue() throws Exception {
      try {
        return Integer.parseInt(getText());
      } catch (NumberFormatException nfe) {
        throw new IllegalArgumentException("Cannot be converted to a integer");
      }
    }
  }

  private List<CacheModelInstanceWrapper> sort(List<CacheModelInstanceWrapper> list) {
    if (list != null) {
      CacheModelInstanceWrapper[] a = list.toArray(new CacheModelInstanceWrapper[0]);
      Arrays.sort(a);
      list.clear();
      list.addAll(Arrays.asList(a));
    }
    return list;
  }

  private List<CacheModelInstanceWrapper> getSettingsList(CacheManagerInstance cacheManagerInstance) {
    List<CacheModelInstanceWrapper> result = new ArrayList<CacheModelInstanceWrapper>();
    for (CacheModelInstance cmi : cacheManagerInstance.cacheModelInstances()) {
      result.add(new CacheModelInstanceWrapper(cmi));
    }
    return sort(result);
  }

  private static class CacheModelInstanceWrapper implements Comparable {
    CacheModelInstance cmi;

    CacheModelInstanceWrapper(CacheModelInstance cmi) {
      this.cmi = cmi;
    }

    @Override
    public String toString() {
      return cmi.getCacheName();
    }

    public int compareTo(Object o) {
      if (!(o instanceof CacheModelInstanceWrapper)) { throw new IllegalArgumentException(
                                                                                          "Not a CacheModelInstanceWrapper"); }
      CacheModelInstanceWrapper other = (CacheModelInstanceWrapper) o;
      return cmi.compareTo(other.cmi);
    }
  }

  private boolean hasChangesOrError() {
    for (Component c : cacheSettingsPanel.getComponents()) {
      if (c instanceof StringField) {
        StringField sf = (StringField) c;
        if (sf.isChangedOrHasError()) { return true; }
      }
    }
    return false;
  }

  public boolean canClose() {
    if (hasChangesOrError()) {
      Dialog dialog = (Dialog) SwingUtilities.getAncestorOfClass(Dialog.class, this);
      int result = JOptionPane.showConfirmDialog(this, "There a unapplied changes. Would you like to apply them?",
                                                 dialog.getTitle(), JOptionPane.YES_NO_CANCEL_OPTION);
      switch (result) {
        case JOptionPane.CANCEL_OPTION:
          return false;
        case JOptionPane.YES_OPTION:
          return apply();
        default:
          return true;
      }
    }
    return true;
  }

  private void setErrorText(String text) {
    boolean isEmpty = StringUtils.isEmpty(text);
    errorLabel.setText(text);
    errorLabel.setIcon(isEmpty ? null : ERROR_ICON);
    if (isEmpty) {
      errorLabel.setText(EMPTY_ERROR_TEXT);
      errorLabel.setForeground(errorLabel.getBackground());
    } else {
      errorLabel.setForeground(null);
    }
  }

  public boolean apply() {
    boolean result = true;
    setErrorText(null);
    for (Component c : cacheSettingsPanel.getComponents()) {
      if (c instanceof StringField) {
        StringField sf = (StringField) c;
        XLabel label = (XLabel) sf.getClientProperty("labeledBy");
        if (!sf.testApply()) {
          if (StringUtils.equals(EMPTY_ERROR_TEXT, errorLabel.getText())) {
            setErrorText(sf.getToolTipText());
            label.setIcon(ERROR_ICON);
          } else {
            label.setIcon(BLANK_ICON);
          }
          result = false;
        } else {
          label.setIcon(BLANK_ICON);
        }
      }
    }
    return result;
  }

  public void instanceAdded(CacheManagerInstance instance) {
    updateCacheManageInstanceSelectorLater();
  }

  public void instanceRemoved(CacheManagerInstance instance) {
    updateCacheManageInstanceSelectorLater();
  }

  public void cacheModelAdded(CacheModel cacheModel) {
    /**/
  }

  public void cacheModelRemoved(CacheModel cacheModel) {
    /**/
  }

  public void cacheModelChanged(CacheModel cacheModel) {
    if (isShowing()) {
      CacheModelInstance cmi = getSelectedCacheModelInstance();
      if (cacheModel.getCacheName().equals(cmi.getCacheName())) {
        update(cmi);
      }
    }
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

  private void updateCacheManageInstanceSelectorLater() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        updateCacheManageInstanceSelector();
      }
    });
  }

  private void updateCacheManageInstanceSelector() {
    int selIndex = cacheManagerInstanceSelector.getSelectedIndex();
    cacheManagerInstanceSelector.setModel(new DefaultComboBoxModel(cacheManagerModel.cacheManagerInstances()
        .toArray(new CacheManagerInstance[0])));
    if (selIndex != -1 && selIndex < cacheManagerInstanceSelector.getItemCount()) {
      cacheManagerInstanceSelector.setSelectedIndex(selIndex);
    }
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

  @Override
  public void tearDown() {
    if (cacheManagerModel != null) {
      cacheManagerModel.removeCacheManagerModelListener(this);
    }
    super.tearDown();
  }
}

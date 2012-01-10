/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation;

import com.tc.admin.common.XButton;
import com.tc.admin.common.XCheckBox;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;

public class QueryForStatsMessage extends XContainer implements ActionListener {
  private final XLabel                  label;
  private static boolean                shouldShowAgain = true;
  private XCheckBox                     noShowToggle;

  protected static final ResourceBundle bundle          = ResourceBundle.getBundle(EhcacheResourceBundle.class
                                                            .getName());

  public QueryForStatsMessage(ActionListener advancedHandler) {
    super(new GridBagLayout());

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.gridx = gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.CENTER;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;

    label = new XLabel(bundle.getString("query.enable.all.statistics"));
    label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    label.setFont((Font) bundle.getObject("message.label.font"));
    add(label, gbc);
    gbc.gridy++;

    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.NONE;
    XButton advancedButton = new XButton("Advanced...");
    add(advancedButton, gbc);
    advancedButton.addActionListener(advancedHandler);
    gbc.gridx++;

    gbc.anchor = GridBagConstraints.EAST;
    add(noShowToggle = new XCheckBox("Don't show again"), gbc);
    noShowToggle.addActionListener(this);
  }

  public void setLabel(String text) {
    label.setText(text);
  }

  public boolean shouldShowAgain() {
    return shouldShowAgain;
  }

  public void actionPerformed(ActionEvent e) {
    boolean showAgainVal = !noShowToggle.isSelected();
    setShouldShowAgain(showAgainVal);
  }

  public static void setShouldShowAgain(boolean valShouldShowAgain) {
    shouldShowAgain = valShouldShowAgain;
  }
}

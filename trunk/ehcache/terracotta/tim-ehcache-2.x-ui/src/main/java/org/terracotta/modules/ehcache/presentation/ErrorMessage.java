package org.terracotta.modules.ehcache.presentation;
/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */


import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class ErrorMessage extends JPanel {
  public ErrorMessage(String msg) {
    this(msg, null);
  }

  public ErrorMessage(String msg, Throwable t) {
    super(new BorderLayout());
    if (msg != null) {
      JLabel label = new JLabel(msg);
      label.setFont(new Font("Dialog", Font.BOLD, 12));
      add(label, BorderLayout.NORTH);
    }
    if (t != null) {
      JTextArea textArea = new JTextArea(t.toString(), 4, 80);
      textArea.setLineWrap(true);
      textArea.setWrapStyleWord(true);
      textArea.setEditable(false);
      add(new JScrollPane(textArea));
    }
  }
}

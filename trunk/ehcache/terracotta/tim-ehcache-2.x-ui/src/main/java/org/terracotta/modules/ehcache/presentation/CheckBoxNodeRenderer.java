package org.terracotta.modules.ehcache.presentation;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ItemListener;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;

public class CheckBoxNodeRenderer implements TreeCellRenderer {
  private final CheckBoxRenderer        checkBoxRenderer = new CheckBoxRenderer();
  private final DefaultTreeCellRenderer stdRenderer      = new DefaultTreeCellRenderer();

  private final Color                   selectionForeground;
  private final Color                   selectionBackground;
  private final Color                   textForeground;
  private final Color                   textBackground;

  protected CheckBoxRenderer getRenderer() {
    return checkBoxRenderer;
  }

  public CheckBoxNodeRenderer() {
    Font fontValue = UIManager.getFont("Tree.font");
    if (fontValue != null) {
      checkBoxRenderer.setFont(fontValue);
    }
    Boolean booleanValue = (Boolean) UIManager.get("Tree.drawsFocusBorderAroundIcon");
    checkBoxRenderer.setFocusPainted((booleanValue != null) && (booleanValue.booleanValue()));

    selectionForeground = UIManager.getColor("Tree.selectionForeground");
    selectionBackground = UIManager.getColor("Tree.selectionBackground");
    textForeground = UIManager.getColor("Tree.textForeground");
    textBackground = UIManager.getColor("Tree.textBackground");
  }

  public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
                                                boolean leaf, int row, boolean hasFocus) {
    String stringValue = tree.convertValueToText(value, selected, expanded, leaf, row, false);
    checkBoxRenderer.setText(stringValue);
    checkBoxRenderer.setSelected(false);

    checkBoxRenderer.setEnabled(tree.isEnabled());

    if (selected) {
      checkBoxRenderer.setForeground(selectionForeground);
      checkBoxRenderer.setBackground(selectionBackground);
    } else {
      checkBoxRenderer.setForeground(textForeground);
      checkBoxRenderer.setBackground(textBackground);
    }

    if (value instanceof DefaultMutableTreeNode) {
      Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
      if (userObject instanceof SelectionModel) {
        SelectionModel node = (SelectionModel) userObject;
        checkBoxRenderer.setText(node.getText());
        checkBoxRenderer.setSelected(node.isSelected());
        checkBoxRenderer.setEnabled(node.isEnabled());
        checkBoxRenderer.setIcon(node.getIcon());
        return checkBoxRenderer;
      }
    }

    return stdRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
  }

  public static class CheckBoxRenderer extends JPanel {
    private JCheckBox checkBox;
    private JLabel    label;
    private boolean   enabled;

    private CheckBoxRenderer() {
      super(new FlowLayout(FlowLayout.CENTER, 0, 0));

      add(checkBox = new JCheckBox());
      add(label = new JLabel());

      label.setIconTextGap(1);
      checkBox.setOpaque(false);
      checkBox.setMargin(new Insets(0, 0, 0, 0));
      setOpaque(false);
      setEnabled(true);
      setBorder(null);
    }

    public void setSelected(boolean selected) {
      checkBox.setSelected(selected);
    }

    public boolean isSelected() {
      return checkBox.isSelected();
    }

    private void setText(String text) {
      label.setText(text);
    }

    public String getText() {
      return label.getText();
    }

    private void setIcon(Icon icon) {
      label.setIcon(icon);
    }

    public Icon getIcon() {
      return label.getIcon();
    }

    @Override
    public boolean isEnabled() {
      return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
      this.enabled = enabled;

      checkBox.setEnabled(enabled);
      label.setEnabled(enabled);
    }

    private void setFocusPainted(boolean focusPainted) {
      checkBox.setFocusPainted(focusPainted);
    }

    public void addItemListener(ItemListener itemListener) {
      checkBox.addItemListener(itemListener);
    }
  }
}

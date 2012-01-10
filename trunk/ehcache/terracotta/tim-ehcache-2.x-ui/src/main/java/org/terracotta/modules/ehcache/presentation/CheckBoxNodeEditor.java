/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation;

import org.terracotta.modules.ehcache.presentation.CheckBoxNodeRenderer.CheckBoxRenderer;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreePath;

/**
 * You need to extends DefaultTreeModel and override valueForPathChanged expecting a newValue of type Boolean.
 */

public class CheckBoxNodeEditor extends AbstractCellEditor implements TreeCellEditor {
  private final CheckBoxNodeRenderer renderer = new CheckBoxNodeRenderer();
  private final JTree                tree;

  public CheckBoxNodeEditor(JTree tree) {
    this.tree = tree;
  }

  public Object getCellEditorValue() {
    return Boolean.valueOf(renderer.getRenderer().isSelected());
  }

  @Override
  public boolean isCellEditable(EventObject event) {
    boolean result = false;
    if (event instanceof MouseEvent) {
      MouseEvent mouseEvent = (MouseEvent) event;
      TreePath path = tree.getPathForLocation(mouseEvent.getX(), mouseEvent.getY());
      if (path != null) {
        Object node = path.getLastPathComponent();
        if (node instanceof DefaultMutableTreeNode) {
          Object userObject = ((DefaultMutableTreeNode) node).getUserObject();
          return userObject instanceof SelectionModel && ((SelectionModel) userObject).isEnabled();
        }
      }
    }
    return result;
  }

  public Component getTreeCellEditorComponent(JTree theTree, Object value, boolean selected, boolean expanded,
                                              boolean leaf, int row) {
    Component result = renderer.getTreeCellRendererComponent(theTree, value, selected, expanded, leaf, row, true);

    // editor always selected / focused
    ItemListener itemListener = new ItemListener() {
      public void itemStateChanged(ItemEvent itemEvent) {
        if (stopCellEditing()) {
          fireEditingStopped();
        }
      }
    };
    if (result instanceof CheckBoxRenderer) {
      ((CheckBoxRenderer) result).addItemListener(itemListener);
    }

    return result;
  }
}
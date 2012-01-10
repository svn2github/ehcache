package org.terracotta.modules.ehcache.presentation;

import com.tc.admin.common.StatusView;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.io.Serializable;
import java.util.Map;

import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

public class TierChooserRenderer extends StatusView implements ListCellRenderer, Serializable {
  private final Map<String, Color> indicatorMap;

  protected final static Border    noFocusBorder        = new EmptyBorder(1, 1, 1, 1);
  private final static Border      SAFE_NO_FOCUS_BORDER = new EmptyBorder(1, 1, 1, 1);

  public TierChooserRenderer(Map<String, Color> indicatorMap) {
    super();
    this.indicatorMap = indicatorMap;
    setOpaque(true);
    setBorder(getNoFocusBorder());
  }

  private static Border getNoFocusBorder() {
    if (System.getSecurityManager() != null) {
      return SAFE_NO_FOCUS_BORDER;
    } else {
      return noFocusBorder;
    }
  }

  @Override
  public Dimension getPreferredSize() {
    Dimension size;

    if ((this.getLabel().getText() == null) || (this.getLabel().getText().equals(""))) {
      setText(" ");
      size = super.getPreferredSize();
      setText("");
    } else {
      size = super.getPreferredSize();
    }

    return size;
  }

  public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                boolean cellHasFocus) {
    if (isSelected) {
      setBackground(list.getSelectionBackground());
      setForeground(list.getSelectionForeground());
    } else {
      setBackground(list.getBackground());
      setForeground(list.getForeground());
    }

    setEnabled(list.isEnabled());
    setFont(list.getFont());
    setText((value == null) ? "" : value.toString());

    if (value != null) {
      setIndicator(indicatorMap.get(value.toString()));
    }

    Border border = null;
    if (cellHasFocus) {
      if (isSelected) {
        border = UIManager.getBorder("List.focusSelectedCellHighlightBorder");
      }
      if (border == null) {
        border = UIManager.getBorder("List.focusCellHighlightBorder");
      }
    } else {
      border = getNoFocusBorder();
    }
    setBorder(border);

    return this;
  }

  @Override
  public boolean isOpaque() {
    Color back = getBackground();
    Component p = getParent();
    if (p != null) {
      p = p.getParent();
    }
    boolean colorMatch = (back != null) && (p != null) && back.equals(p.getBackground()) && p.isOpaque();
    return !colorMatch && super.isOpaque();
  }

  public static class UIResource extends TierChooserRenderer implements javax.swing.plaf.UIResource {
    public UIResource(Map<String, Color> indicatorMap) {
      super(indicatorMap);
    }
  }
}

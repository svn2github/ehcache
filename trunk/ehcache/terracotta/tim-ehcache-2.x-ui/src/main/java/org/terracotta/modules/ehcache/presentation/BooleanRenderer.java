package org.terracotta.modules.ehcache.presentation;

import com.tc.admin.common.XTable.BaseRenderer;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

public class BooleanRenderer extends BaseRenderer {
  private final boolean          textual;
  private final Border           emptyBorder   = BorderFactory.createEmptyBorder(2, 0, 1, 0);

  private static final ImageIcon ENABLED_ICON  = new ImageIcon(
                                                               BooleanRenderer.class
                                                                   .getResource("/com/tc/admin/icons/correct.gif"));
  private static final ImageIcon DISABLED_ICON = new ImageIcon(
                                                               BooleanRenderer.class
                                                                   .getResource("/com/tc/admin/icons/wrong.gif"));

  public BooleanRenderer() {
    this(false);
  }

  public BooleanRenderer(boolean textual) {
    super();
    this.textual = textual;
  }

  @Override
  public void setValue(Object value) {
    label.setHorizontalAlignment(SwingConstants.CENTER);
    if (textual) {
      label.setText(value != null ? value.toString() : "");
    } else {
      boolean enabled = ((Boolean) value).booleanValue();
      label.setIcon(enabled ? ENABLED_ICON : DISABLED_ICON);
      label.setBorder(emptyBorder);
    }
  }
}

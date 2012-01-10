/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation;

import javax.swing.Icon;

public interface SelectionModel {
  boolean isSelected();

  void setSelected(boolean selected);

  boolean isEnabled();

  void setEnabled(boolean enabled);

  String getText();

  Icon getIcon();
}

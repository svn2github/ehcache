/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation;

import java.lang.reflect.Field;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

public class EhcacheToolBar extends JToolBar {
  public EhcacheToolBar() {
    super();
    setFloatable(false);
  }

  @Override
  protected JButton createActionComponent(Action a) {
    JButton b = super.createActionComponent(a);
    b.setHorizontalTextPosition(SwingConstants.TRAILING);
    b.setVerticalTextPosition(SwingConstants.CENTER);
    b.putClientProperty("hideActionText", Boolean.FALSE);
    Class<?> bClass = b.getClass();
    for (Field field : bClass.getDeclaredFields()) {
      if (field.getName().equals("hideActionText")) {
        try {
          field.setAccessible(true);
          field.setBoolean(b, false);
        } catch (Exception e) {/**/
        }
      }
    }
    return b;
  }
}

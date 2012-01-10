/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation;

import java.awt.Color;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class EhcachePresentationUtils {
  public static final Color HIT_COLOR               = Color.green;
  public static final Color MISS_COLOR              = Color.red;
  public static final Color PUT_COLOR               = Color.blue;
  public final static Color HIT_FILL_COLOR          = HIT_COLOR.brighter().brighter().brighter();
  public final static Color MISS_FILL_COLOR         = MISS_COLOR.brighter().brighter().brighter();
  public final static Color PUT_FILL_COLOR          = PUT_COLOR.brighter().brighter().brighter();
  public final static Color HIT_DRAW_COLOR          = HIT_COLOR.darker();
  public final static Color MISS_DRAW_COLOR         = MISS_COLOR.darker();
  public final static Color PUT_DRAW_COLOR          = PUT_COLOR.darker();

  public final static Icon  DISABLE_CACHE_ICON      = new ImageIcon(
                                                                    EhcachePresentationUtils.class
                                                                        .getResource("cart_delete.png"));
  public final static Icon  ENABLE_CACHE_ICON       = new ImageIcon(
                                                                    EhcachePresentationUtils.class
                                                                        .getResource("cart_put.png"));
  public final static Icon  CLEAR_CACHE_ICON        = new ImageIcon(
                                                                    EhcachePresentationUtils.class
                                                                        .getResource("cart_remove.png"));
  public final static Icon  ENABLE_STATS_ICON       = new ImageIcon(
                                                                    EhcachePresentationUtils.class
                                                                        .getResource("chart_line_add.png"));
  public final static Icon  DISABLE_STATS_ICON      = new ImageIcon(
                                                                    EhcachePresentationUtils.class
                                                                        .getResource("chart_line_delete.png"));
  public final static Icon  CLEAR_STATS_ICON        = new ImageIcon(
                                                                    EhcachePresentationUtils.class
                                                                        .getResource("chart_line.png"));
  public final static Icon  GENERATE_CONFIG_ICON    = new ImageIcon(
                                                                    EhcachePresentationUtils.class
                                                                        .getResource("/com/tc/admin/icons/export_wiz.gif"));
  public final static Icon  BULK_LOAD_DISABLED_ICON = new ImageIcon(
                                                                    EhcachePresentationUtils.class
                                                                        .getResource("arrow_merge.png"));
  public final static Icon  BULK_LOAD_ENABLED_ICON  = new ImageIcon(
                                                                    EhcachePresentationUtils.class
                                                                        .getResource("arrow_branch.png"));
  public final static Icon  ALERT_ICON              = new ImageIcon(
                                                                    EhcachePresentationUtils.class
                                                                        .getResource("/com/tc/admin/icons/alert12x12.gif"));
  public final static Icon  BLANK_ICON              = new ImageIcon(
                                                                    EhcachePresentationUtils.class
                                                                        .getResource("/com/tc/admin/icons/blank12x12.gif"));
  public final static Icon  WARN_ICON               = new ImageIcon(
                                                                    EhcachePresentationUtils.class
                                                                        .getResource("/com/tc/admin/icons/warning_obj.gif"));
  public final static Icon  CLUSTERED_ICON          = new ImageIcon(
                                                                    EhcachePresentationUtils.class
                                                                        .getResource("/com/tc/admin/icons/progress_task_yellow.gif"));

  public final static Icon  NON_CLUSTERED_ICON      = new ImageIcon(
                                                                    EhcachePresentationUtils.class
                                                                        .getResource("/com/tc/admin/icons/progress_task_grey.gif"));

  public final static Icon  LIGHT_BULB_ICON         = new ImageIcon(
                                                                    EhcachePresentationUtils.class
                                                                        .getResource("lightbulb.png"));

  public static String determineShortName(String fullName) {
    String result = fullName;

    if (fullName != null) {
      String[] comps = fullName.split("\\.");
      if (comps.length == 1) { return fullName; }
      for (int i = 0; i < comps.length; i++) {
        String comp = comps[i];
        if (i < (comps.length - 1) && (comp.length() > 0)) {
          comps[i] = Character.toString(comp.charAt(0));
        }
      }
      result = join(comps, '.');
    }

    return result;
  }

  private static String join(String[] elements, char c) {
    if (elements == null) { return null; }
    StringBuilder sb = new StringBuilder();
    for (String s : elements) {
      sb.append(s).append(c);
    }
    return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";
  }
}

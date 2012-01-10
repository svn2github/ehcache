/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.hibernatecache.presentation;

import java.awt.Color;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public abstract class CacheRegionUtils {
  public static final Color HIT_COLOR            = Color.green;
  public static final Color MISS_COLOR           = Color.red;
  public static final Color PUT_COLOR            = Color.blue;
  public final static Color HIT_FILL_COLOR       = CacheRegionUtils.HIT_COLOR.brighter().brighter().brighter();
  public final static Color MISS_FILL_COLOR      = CacheRegionUtils.MISS_COLOR.brighter().brighter().brighter();
  public final static Color PUT_FILL_COLOR       = CacheRegionUtils.PUT_COLOR.brighter().brighter().brighter();
  public final static Color HIT_DRAW_COLOR       = CacheRegionUtils.HIT_COLOR.darker();
  public final static Color MISS_DRAW_COLOR      = CacheRegionUtils.MISS_COLOR.darker();
  public final static Color PUT_DRAW_COLOR       = CacheRegionUtils.PUT_COLOR.darker();

  public final static Icon  DISABLE_CACHE_ICON   = new ImageIcon(CacheRegionUtils.class.getResource("cart_delete.png"));
  public final static Icon  ENABLE_CACHE_ICON    = new ImageIcon(CacheRegionUtils.class.getResource("cart_put.png"));
  public final static Icon  CLEAR_CACHE_ICON     = new ImageIcon(CacheRegionUtils.class.getResource("cart_remove.png"));
  public final static Icon  ENABLE_STATS_ICON    = new ImageIcon(CacheRegionUtils.class
                                                     .getResource("chart_line_add.png"));
  public final static Icon  DISABLE_STATS_ICON   = new ImageIcon(CacheRegionUtils.class
                                                     .getResource("chart_line_delete.png"));
  public final static Icon  CLEAR_STATS_ICON     = new ImageIcon(CacheRegionUtils.class.getResource("chart_line.png"));
  public final static Icon  GENERATE_CONFIG_ICON = new ImageIcon(CacheRegionUtils.class
                                                     .getResource("/com/tc/admin/icons/export_wiz.gif"));

  public static String determineShortName(String fullName) {
    String result = fullName;

    if (fullName != null) {
      String[] comps = fullName.split("\\.");
      if (comps.length == 1) { return fullName; }
      boolean truncate = true;
      for (int i = 0; i < comps.length; i++) {
        String comp = comps[i];
        char c = comp.charAt(0);
        if (truncate && Character.isUpperCase(c)) {
          truncate = false;
        }
        if (truncate) {
          comps[i] = Character.toString(c);
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
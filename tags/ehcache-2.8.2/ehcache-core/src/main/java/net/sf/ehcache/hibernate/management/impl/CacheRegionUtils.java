/**
 *  Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache.hibernate.management.impl;

import java.awt.Color;

/**
 * CacheRegionUtils
 * 
 * @author gkeim
 */
public abstract class CacheRegionUtils {
  /**
   * HIT_COLOR
   */
  public static final Color HIT_COLOR = Color.green;
  
  /**
   * MISS_COLOR
   */
  public static final Color MISS_COLOR = Color.red;
  
  /**
   * PUT_COLOR
   */
  public static final Color PUT_COLOR = Color.blue;
  
  /**
   * HIT_FILL_COLOR
   */
  public static final Color HIT_FILL_COLOR = CacheRegionUtils.HIT_COLOR.brighter().brighter().brighter();
  
  /**
   * MISS_FILL_COLOR
   */
  public static final Color MISS_FILL_COLOR = CacheRegionUtils.MISS_COLOR.brighter().brighter().brighter();
  
  /**
   * PUT_FILL_COLOR
   */
  public static final Color PUT_FILL_COLOR = CacheRegionUtils.PUT_COLOR.brighter().brighter().brighter();
  
  /**
   * HIT_DRAW_COLOR
   */
  public static final Color HIT_DRAW_COLOR = CacheRegionUtils.HIT_COLOR.darker();
  
  /**
   * MISS_DRAW_COLOR
   */
  public static final Color MISS_DRAW_COLOR = CacheRegionUtils.MISS_COLOR.darker();
  
  /**
   * PUT_DRAW_COLOR
   */
  public static final Color PUT_DRAW_COLOR = CacheRegionUtils.PUT_COLOR.darker();
  

  /**
   * determineShortName
   * 
   * @param fullName
   */
  public static String determineShortName(String fullName) {
    String result = fullName;

    if (fullName != null) {
      String[] comps = fullName.split("\\.");
      if (comps.length == 1) {
          return fullName;
      }
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

  /**
   * join
   * 
   * @param elements
   * @param c
   */
  private static String join(String[] elements, char c) {
    if (elements == null) { return null; }
    StringBuilder sb = new StringBuilder();
    for (String s : elements) {
      sb.append(s).append(c);
    }
    return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";
  }
}

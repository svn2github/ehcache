/**
 * All content copyright 2010 (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package net.sf.ehcache.servermaplocalcache;

import java.util.Date;

public abstract class DebugUtil {

  public static final boolean DEBUG = false;

  public static void debug(String msg) {
    if (DEBUG) {
      System.out.println(new Date() + " [" + Thread.currentThread().getName() + "]: " + msg);
    }
  }

}

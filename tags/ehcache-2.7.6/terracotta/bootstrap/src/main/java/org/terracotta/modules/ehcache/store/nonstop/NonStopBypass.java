/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store.nonstop;

import net.sf.ehcache.util.lang.VicariousThreadLocal;

/**
 * The NonStopBypass can be used to allow the current thread to ignore the NonStop timeout.
 *
 * @author Ludovic Orban
 */
public class NonStopBypass {

  private final static ThreadLocal<Boolean> bypassTl = new VicariousThreadLocal<Boolean>() {
    @Override
    protected Boolean initialValue() {
      return false;
    }
  };

  public static void setBypassEnabledForCurrentThread(boolean bypass) {
    bypassTl.set(bypass);
  }

  public static boolean isBypassEnabledForCurrentThread() {
    return bypassTl.get();
  }

}

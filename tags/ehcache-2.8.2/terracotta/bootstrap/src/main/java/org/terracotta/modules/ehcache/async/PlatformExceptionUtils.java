/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.async;

import org.terracotta.toolkit.rejoin.RejoinException;

public class PlatformExceptionUtils {

  public static boolean isTCNRE(Throwable th) {
    return th.getClass().getName().equals("com.tc.exception.TCNotRunningException");
  }

  public static boolean isRejoinException(Throwable th) {
    if (th instanceof RejoinException) return true;
    return false;
  }

  public static boolean shouldIgnore(Throwable th) {
    return isTCNRE(th) || isRejoinException(th);
  }

}

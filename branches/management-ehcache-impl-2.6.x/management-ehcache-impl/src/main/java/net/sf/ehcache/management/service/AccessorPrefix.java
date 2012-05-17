/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache.management.service;

/**
* @author brandony
*/
public enum AccessorPrefix {
  get, is, has;

  public static boolean isAccessor(String methodName) {
    for (AccessorPrefix prefix : AccessorPrefix.values()) {
      if (methodName.startsWith(prefix.toString())) return true;
    }
    return false;
  }

  public static String trimPrefix(String methodName) {
    String trimmed = null;

    for (AccessorPrefix prefix : AccessorPrefix.values()) {
      if (methodName.startsWith(prefix.toString())) {
        trimmed = methodName.substring(prefix.toString().length());
      }
    }

    return trimmed;
  }
}

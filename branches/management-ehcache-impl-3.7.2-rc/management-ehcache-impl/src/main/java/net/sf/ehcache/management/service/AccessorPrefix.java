/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache.management.service;

/**
 * A enumeration of expected class accessor prefixes.
 *
 * @author brandony
 */
public enum AccessorPrefix {
  get, is, has;

  /**
   * A method to determine whether or not the submitted method name matches the accessor pattern defined by this enumeration.
   *
   * @param methodName to be checked
   * @return true if accessor pattern is detected; false otherwise
   */
  public static boolean isAccessor(String methodName) {
    for (AccessorPrefix prefix : AccessorPrefix.values()) {
      if (methodName.startsWith(prefix.toString())) return true;
    }
    return false;
  }

  /**
   * A method to trim accessor methods of their accessor prefix for further processing.
   *
   * @param methodName to be trimmed
   * @return the trimmed method name
   */
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

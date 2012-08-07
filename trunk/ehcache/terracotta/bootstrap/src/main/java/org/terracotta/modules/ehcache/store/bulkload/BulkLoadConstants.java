/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store.bulkload;

import java.util.Properties;

public final class BulkLoadConstants {
  private static final String EHCACHE_LOGGING_ENABLED_PROPERTY                      = "ehcache.incoherent.logging";
  private static final String EHCACHE_LOCAL_BUFFER_PUTS_BATCH_BYTE_SIZE_PROPERTY    = "ehcache.incoherent.putsBatchByteSize";
  private static final String EHCACHE_LOCAL_BUFFER_PUTS_BATCH_TIME_MILLIS_PROPERTY  = "ehcache.incoherent.putsBatchTimeInMillis";
  private static final String EHCACHE_LOCAL_BUFFER_PUTS_THROTTLE_BYTE_SIZE_PROPERTY = "ehcache.incoherent.throttlePutByteSize";
  private static final String EHCACHE_BULKOPS_MAX_KB_SIZE_PROPERTY                  = "ehcache.bulkOps.maxKBSize";

  private static final int    ONE_KB                                                = 1024;
  private static final int    ONE_MB                                                = 1 * ONE_KB * ONE_KB;                       // 1MB
  private static final int    DEFAULT_LOCAL_BUFFER_PUTS_BATCH_BYTE_SIZE             = 5 * ONE_MB;                                // 5MB
  private static final int    DEFAULT_LOCAL_BUFFER_PUTS_THROTTLE_BYTE_SIZE          = 10 * ONE_MB;                               // 10MB
  private static final int    DEFAULT_LOCAL_BUFFER_PUTS_BATCH_TIME_MILLIS           = 600;
  private static final int    DEFAULT_EHCACHE_BULKOPS_MAX_KB_SIZE                   = ONE_KB;

  private static Properties   tcProperties                                          = new Properties();

  private BulkLoadConstants() {
    // private
  }

  private static boolean getTCPropBoolean(String propName, boolean defaultValue) {
    try {
      return Boolean.parseBoolean(tcProperties.getProperty(propName, String.valueOf(defaultValue)));
    } catch (Exception e) {
      // for unit-tests
      return defaultValue;
    }
  }

  private static int getTCPropInt(String propName, int defaultValue) {
    try {
      return Integer.parseInt(tcProperties.getProperty(propName, String.valueOf(defaultValue)));
    } catch (Exception e) {
      // for unit-tests
      return defaultValue;
    }
  }

  private static long getTCPropLong(String propName, long defaultValue) {
    try {
      return Long.parseLong(tcProperties.getProperty(propName, String.valueOf(defaultValue)));
    } catch (Exception e) {
      // for unit-tests
      return defaultValue;
    }
  }

  public static boolean isLoggingEnabled() {
    return getTCPropBoolean(EHCACHE_LOGGING_ENABLED_PROPERTY, false);
  }

  public static int getBatchedPutsBatchBytes() {
    return getTCPropInt(EHCACHE_LOCAL_BUFFER_PUTS_BATCH_BYTE_SIZE_PROPERTY, DEFAULT_LOCAL_BUFFER_PUTS_BATCH_BYTE_SIZE);
  }

  public static long getBatchedPutsBatchTimeMillis() {
    return getTCPropLong(EHCACHE_LOCAL_BUFFER_PUTS_BATCH_TIME_MILLIS_PROPERTY,
                         DEFAULT_LOCAL_BUFFER_PUTS_BATCH_TIME_MILLIS);
  }

  public static int getBatchedPutsThrottlePutsAtByteSize() {
    return getTCPropInt(EHCACHE_LOCAL_BUFFER_PUTS_THROTTLE_BYTE_SIZE_PROPERTY,
                        DEFAULT_LOCAL_BUFFER_PUTS_THROTTLE_BYTE_SIZE);
  }

  public static int getBulkOpsMaxKBSize() {
    return getTCPropInt(EHCACHE_BULKOPS_MAX_KB_SIZE_PROPERTY, DEFAULT_EHCACHE_BULKOPS_MAX_KB_SIZE);
  }
}

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store.bulkload;

public final class BulkLoadConstants {
  private static final String EHCACHE_LOGGING_ENABLED_PROPERTY                      = "com.tc.ehcache.incoherent.logging";
  private static final String EHCACHE_LOCAL_BUFFER_PUTS_BATCH_BYTE_SIZE_PROPERTY    = "com.tc.ehcache.incoherent.putsBatchByteSize";
  private static final String EHCACHE_LOCAL_BUFFER_PUTS_BATCH_TIME_MILLIS_PROPERTY  = "com.tc.ehcache.incoherent.putsBatchTimeInMillis";
  private static final String EHCACHE_LOCAL_BUFFER_PUTS_THROTTLE_BYTE_SIZE_PROPERTY = "com.tc.ehcache.incoherent.throttlePutByteSize";
  private static final String EHCACHE_BULKOPS_MAX_KB_SIZE_PROPERTY                  = "com.tc.ehcache.bulkOps.maxKBSize";

  private static final int    ONE_KB                                                = 1024;
  private static final int    ONE_MB                                                = 1 * ONE_KB * ONE_KB;                       // 1MB
  private static final int    DEFAULT_LOCAL_BUFFER_PUTS_BATCH_BYTE_SIZE             = 5 * ONE_MB;                                // 5MB
  private static final int    DEFAULT_LOCAL_BUFFER_PUTS_THROTTLE_BYTE_SIZE          = 10 * ONE_MB;                               // 10MB
  private static final int    DEFAULT_LOCAL_BUFFER_PUTS_BATCH_TIME_MILLIS           = 600;
  private static final int    DEFAULT_EHCACHE_BULKOPS_MAX_KB_SIZE                   = ONE_KB;

  private BulkLoadConstants() {
    // private
  }

  public static boolean isLoggingEnabled() {
    return Boolean.getBoolean(EHCACHE_LOGGING_ENABLED_PROPERTY);
  }

  public static int getBatchedPutsBatchBytes() {
    return Integer.getInteger(EHCACHE_LOCAL_BUFFER_PUTS_BATCH_BYTE_SIZE_PROPERTY,
                              DEFAULT_LOCAL_BUFFER_PUTS_BATCH_BYTE_SIZE);
  }

  public static long getBatchedPutsBatchTimeMillis() {
    return Long.getLong(EHCACHE_LOCAL_BUFFER_PUTS_BATCH_TIME_MILLIS_PROPERTY,
                         DEFAULT_LOCAL_BUFFER_PUTS_BATCH_TIME_MILLIS);
  }

  public static int getBatchedPutsThrottlePutsAtByteSize() {
    return Integer.getInteger(EHCACHE_LOCAL_BUFFER_PUTS_THROTTLE_BYTE_SIZE_PROPERTY,
                        DEFAULT_LOCAL_BUFFER_PUTS_THROTTLE_BYTE_SIZE);
  }

  public static int getBulkOpsMaxKBSize() {
    return Integer.getInteger(EHCACHE_BULKOPS_MAX_KB_SIZE_PROPERTY, DEFAULT_EHCACHE_BULKOPS_MAX_KB_SIZE);
  }
}

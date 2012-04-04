/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.cache;

import org.terracotta.cache.evictor.CapacityEvictionPolicyData;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.config.Configuration;

import java.io.Serializable;

/**
 * Inside a DistributedCache, the map values are wrapped in a TimestampedValue that combines the actual value with
 * information on created and last accessed times.
 */
public interface TimestampedValue<V> extends ExpirableEntry, Serializable {
  /**
   * Indicates the value will never expire (Integer.MAX_VALUE).
   */
  public static final int NEVER_EXPIRE = Integer.MAX_VALUE;

  /**
   * Determine when this value will expire based on the current configuration.
   * 
   * @param config The configuration
   * @return The timestamp (in seconds since the epoch) when this value will expire. May return {@link #NEVER_EXPIRE} if
   *         it will never expire.
   */
  int expiresAt(Configuration config);

  /**
   * Determine whether this value will be expired at time based on the config.
   * 
   * @param time The time in seconds since the epoch for which to check. Should be > 0.
   * @param config The cache configuration
   * @return True if expired
   */
  boolean isExpired(int time, Configuration config);

  /**
   * Get the actual value in the cache.
   * 
   * @return The value
   */
  V getValue();

  /**
   * Mark this value as being used at the specified time. This updates the value's last accessed time.
   * 
   * @param usedAt Current time in seconds since the epoch
   * @param lock The lock guarding this value, such as would be obtained by calling the underlying
   *        ConcurrentDistributedMap.createFinegrainedLock() method
   * @param config The cache configuration
   */
  void markUsed(int usedAt, ToolkitLock lock, Configuration config);

  /**
   * Retrieves the time when this value was created.
   * 
   * @return this value's creation time in seconds since epoch
   */
  int getCreateTime();

  /**
   * Retrieves the time when this value was last accessed.
   * 
   * @return this value's last access time in seconds since epoch
   */
  int getLastAccessedTime();

  /**
   * Sets this value's capacity eviction policy data.
   * 
   * @param capacityEvictionPolicyData the new data
   */
  void setCapacityEvictionPolicyData(CapacityEvictionPolicyData capacityEvictionPolicyData);

  /**
   * Retrieves this value's capacity eviction policy data.
   * 
   * @return this value's data
   */
  CapacityEvictionPolicyData getCapacityEvictionPolicyData();
}

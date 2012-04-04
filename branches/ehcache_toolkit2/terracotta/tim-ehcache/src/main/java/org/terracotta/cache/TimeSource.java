/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.cache;

/**
 * A source of time for the map when updating timestamps.
 */
public interface TimeSource {
  /**
   * Determine the current time, (like System.currentTimeMillis() but in seconds).
   * 
   * @return The current time in seconds since the epoch
   */
  int now();
}

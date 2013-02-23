/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.async;

public class DefaultAsyncConfig implements AsyncConfig {
  private final static AsyncConfig INSTANCE               = new DefaultAsyncConfig();

  public final static long         WORK_DELAY             = 1000;                    // 1 second
  public final static long         MAX_ALLOWED_FALLBEHIND = 2000;                    // 2 seconds
  public final static int          BATCH_SIZE             = 1;
  public final static boolean      BATCHING_ENABLED       = false;
  public final static boolean      SYNCHRONOUS_WRITE      = false;
  public final static int          RETRY_ATTEMPTS         = 0;
  public final static long         RETRY_ATTEMPT_DELAY    = 1000;                    // 1 second
  public final static int          RATE_LIMIT             = 0;
  public final static int          MAX_QUEUE_SIZE         = 0;

  /**
   * Return an {@code AsyncConfig} instance representing the default configuration.
   * 
   * @return the default configuration
   */
  public static AsyncConfig getInstance() {
    return INSTANCE;
  }

  protected DefaultAsyncConfig() {
    // private constructor for singleton
  }

  public long getWorkDelay() {
    return WORK_DELAY;
  }

  public long getMaxAllowedFallBehind() {
    return MAX_ALLOWED_FALLBEHIND;
  }

  public int getBatchSize() {
    return BATCH_SIZE;
  }

  public boolean isBatchingEnabled() {
    return BATCHING_ENABLED;
  }

  public boolean isSynchronousWrite() {
    return SYNCHRONOUS_WRITE;
  }

  public int getRetryAttempts() {
    return RETRY_ATTEMPTS;
  }

  public long getRetryAttemptDelay() {
    return RETRY_ATTEMPT_DELAY;
  }

  public int getRateLimit() {
    return RATE_LIMIT;
  }

  public int getMaxQueueSize() {
    return MAX_QUEUE_SIZE;
  }
}

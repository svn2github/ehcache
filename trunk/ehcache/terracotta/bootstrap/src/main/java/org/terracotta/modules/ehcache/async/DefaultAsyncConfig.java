/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.async;

import java.util.concurrent.TimeUnit;

public class DefaultAsyncConfig implements AsyncConfig {
  private final static AsyncConfig INSTANCE               = new DefaultAsyncConfig();

  public final static long         WORK_DELAY             = TimeUnit.SECONDS.toMillis(1L); // 1 second
  public final static long         MAX_ALLOWED_FALLBEHIND = TimeUnit.SECONDS.toMillis(2L); // 2 seconds
  public final static int          BATCH_SIZE             = 1;
  public final static boolean      BATCHING_ENABLED       = false;
  public final static boolean      SYNCHRONOUS_WRITE      = false;
  public final static int          RETRY_ATTEMPTS         = 0;
  public final static long         RETRY_ATTEMPT_DELAY    = TimeUnit.SECONDS.toMillis(1L); // 1 second
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

  @Override
  public long getWorkDelay() {
    return WORK_DELAY;
  }

  @Override
  public long getMaxAllowedFallBehind() {
    return MAX_ALLOWED_FALLBEHIND;
  }

  @Override
  public int getBatchSize() {
    return BATCH_SIZE;
  }

  @Override
  public boolean isBatchingEnabled() {
    return BATCHING_ENABLED;
  }

  @Override
  public boolean isSynchronousWrite() {
    return SYNCHRONOUS_WRITE;
  }

  @Override
  public int getRetryAttempts() {
    return RETRY_ATTEMPTS;
  }

  @Override
  public long getRetryAttemptDelay() {
    return RETRY_ATTEMPT_DELAY;
  }

  @Override
  public int getRateLimit() {
    return RATE_LIMIT;
  }

  @Override
  public int getMaxQueueSize() {
    return MAX_QUEUE_SIZE;
  }
}

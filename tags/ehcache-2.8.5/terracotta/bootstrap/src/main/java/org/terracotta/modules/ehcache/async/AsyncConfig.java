/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.async;

import java.io.Serializable;

public interface AsyncConfig extends Serializable {
  /**
   * Returns the minimum amount of time to wait between individual work cycles.
   * <p/>
   * This allows work to accumulate in the write behind queue and be processed more efficiently.
   * 
   * @return the work delay that should be used, in milliseconds
   */
  public long getWorkDelay();

  /**
   * The maximum amount of time that a queue is allowed to fall behind on the work that it's processing.
   * 
   * @return the maximum of time that the queue is allowed to fall behind, in milliseconds
   */
  public long getMaxAllowedFallBehind();

  /**
   * The number of items to include in each batch when batching is enabled. If there are less entries in the queue than
   * the batch size, the queue length size is used.
   * 
   * @return the amount of items to batch
   */
  public int getBatchSize();

  /**
   * Indicates whether to batch items. If set to {@code true}, {@link ItemProcessor#process(java.util.Collection)} will
   * be called rather than {@link ItemProcessor#process(Object)} being called for individual item. Resources such as
   * databases can perform more efficiently if updates are batched, thus reducing load.
   * 
   * @return {@code true} if items should be batched; {@code false} otherwise
   */
  public boolean isBatchingEnabled();

  /**
   * Perform all writes to the Terracotta backend in a synchronous fashion, hence increasing reliability but decreasing
   * performance.
   * 
   * @return {@code true} to enable synchronous writes; or {@code false} to perform the write asynchronously
   */
  public boolean isSynchronousWrite();

  /**
   * Retrieves the number of times the processing of an item is retried.
   * 
   * @return the number of tries before this pass is considered failed
   */
  public int getRetryAttempts();

  /**
   * Retrieves the number of milliseconds to wait before retrying a failed operation.
   * 
   * @return the delay in between retries, in milliseconds
   */
  public long getRetryAttemptDelay();

  /**
   * Sets the maximum number of operations to allow per second when {@link #isBatchingEnabled} is enabled.
   * 
   * @return the rate limit
   */
  public int getRateLimit();

  /**
   * The maximum size of items the Async coordinator can hold.
   * 
   * @return
   */
  public int getMaxQueueSize();

}

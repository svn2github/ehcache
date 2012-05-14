/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.async.errorhandlers;

import org.terracotta.modules.ehcache.async.ProcessingBucket;

/**
 * Instances of AsyncErrorHandler are used to provide custom exception handling for async work items.
 */
public interface AsyncErrorHandler {

  /**
   * Called when a work item throws during execution.
   * 
   * @param bucket the bucket executing the work item
   * @param exception the exception thrown
   */
  public void onError(ProcessingBucket bucket, Throwable exception);
}

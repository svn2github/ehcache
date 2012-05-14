/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.async.errorhandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.modules.ehcache.async.ProcessingBucket;

public class LoggingErrorHandler implements AsyncErrorHandler {
  private static final Logger              LOGGER   = LoggerFactory.getLogger(LoggingErrorHandler.class.getName());
  private static final LoggingErrorHandler INSTANCE = new LoggingErrorHandler();

  private LoggingErrorHandler() {
    // private constructor for singleton
  }

  public static LoggingErrorHandler getInstance() {
    return INSTANCE;
  }

  public void onError(final ProcessingBucket bucket, final Throwable exception) {
    LOGGER.error(bucket.getBucketName() + " " + exception);
  }
}

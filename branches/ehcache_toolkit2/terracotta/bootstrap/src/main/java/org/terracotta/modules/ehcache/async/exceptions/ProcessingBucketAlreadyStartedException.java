/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.async.exceptions;

public class ProcessingBucketAlreadyStartedException extends AsyncException {
  public ProcessingBucketAlreadyStartedException(final Thread t) {
    super("A thread with name " + t.getName() + " already exists and is still running");
  }
}

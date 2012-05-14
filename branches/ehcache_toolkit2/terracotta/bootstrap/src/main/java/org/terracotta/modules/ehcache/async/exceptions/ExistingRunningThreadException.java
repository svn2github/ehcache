/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.async.exceptions;

public class ExistingRunningThreadException extends AsyncException {
  public ExistingRunningThreadException(final Thread t) {
    super("A thread with name " + t.getName() + " already exists and is still running");
  }
}

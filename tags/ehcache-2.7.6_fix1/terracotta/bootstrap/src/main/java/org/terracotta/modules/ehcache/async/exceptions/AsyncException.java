/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.async.exceptions;

public class AsyncException extends Exception {

  public AsyncException(final String msg, final Throwable cause) {
    super(msg, cause);
  }

  public AsyncException(final String msg) {
    this(msg, null);
  }
}

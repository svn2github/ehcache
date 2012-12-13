/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.async.exceptions;


public class ProcessingException extends AsyncException {
  public ProcessingException(final String msg, final Throwable cause) {
    super(msg, cause);
  }
}

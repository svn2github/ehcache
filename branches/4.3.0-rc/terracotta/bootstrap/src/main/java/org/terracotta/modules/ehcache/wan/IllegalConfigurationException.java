/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.wan;

public class IllegalConfigurationException extends RuntimeException {

  public IllegalConfigurationException() {
  }

  public IllegalConfigurationException(final String message) {
    super(message);
  }

  public IllegalConfigurationException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public IllegalConfigurationException(final Throwable cause) {
    super(cause);
  }

}

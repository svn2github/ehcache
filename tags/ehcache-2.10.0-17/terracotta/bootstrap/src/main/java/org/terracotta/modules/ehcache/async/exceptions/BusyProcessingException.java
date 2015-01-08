/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.async.exceptions;

public class BusyProcessingException extends AsyncException {
  public BusyProcessingException() {
    super("Already busy processing.");
  }
}
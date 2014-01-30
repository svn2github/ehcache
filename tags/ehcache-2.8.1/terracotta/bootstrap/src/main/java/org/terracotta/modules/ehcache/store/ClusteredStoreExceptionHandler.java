/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

public interface ClusteredStoreExceptionHandler {

  public void handleException(Throwable t);

}

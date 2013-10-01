/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind;

import net.sf.ehcache.writer.writebehind.operations.KeyBasedOperation;

public class KeyBasedOperationWrapper implements KeyBasedOperation {
  private final Object key;
  private final long   creationTime;

  public KeyBasedOperationWrapper(Object key, long creationTime) {
    this.key = key;
    this.creationTime = creationTime;
  }

  public Object getKey() {
    return key;
  }

  public long getCreationTime() {
    return creationTime;
  }
}

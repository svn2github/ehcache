/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind;

import net.sf.ehcache.writer.writebehind.operations.KeyBasedOperation;

/**
 * An implementation of {@code KeyBasedOperation} that simply stores the values it has to return
 *
 * @author Geert Bevin
 * @version $Id$
 */
public class KeyBasedOperationWrapper implements KeyBasedOperation {
  private final Object key;
  private final long creationTime;

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

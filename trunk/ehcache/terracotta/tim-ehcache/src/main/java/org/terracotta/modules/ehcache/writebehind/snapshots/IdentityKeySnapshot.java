/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind.snapshots;

import org.terracotta.annotations.InstrumentedClass;
import org.terracotta.cache.serialization.SerializationStrategy;

@InstrumentedClass
public class IdentityKeySnapshot extends KeySnapshot {
  private final Object key;

  public IdentityKeySnapshot(Object key) {
    this.key = key;
  }

  @Override
  public Object getKey(SerializationStrategy strategy) {
    return key;
  }
}

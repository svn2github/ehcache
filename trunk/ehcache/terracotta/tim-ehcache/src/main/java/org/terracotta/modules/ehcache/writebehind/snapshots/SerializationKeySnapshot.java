/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind.snapshots;

import org.terracotta.cache.serialization.SerializationStrategy;

import java.io.IOException;

public class SerializationKeySnapshot extends KeySnapshot {
  private final byte[] keySerialized;
  private transient Object keyDeserialized;

  public SerializationKeySnapshot(SerializationStrategy strategy, Object key) throws IOException {
    this.keySerialized = strategy.serialize(key);
  }

  @Override
  public synchronized Object getKey(SerializationStrategy strategy) throws IOException, ClassNotFoundException {
    if (null == keyDeserialized) {
      keyDeserialized = strategy.deserialize(keySerialized);
    }

    return keyDeserialized;
  }
}

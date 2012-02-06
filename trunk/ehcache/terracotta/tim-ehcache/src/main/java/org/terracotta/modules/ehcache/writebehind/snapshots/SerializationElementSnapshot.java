/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind.snapshots;

import net.sf.ehcache.Element;

import org.terracotta.cache.serialization.SerializationStrategy;

import java.io.IOException;

public class SerializationElementSnapshot extends ElementSnapshot {
  private final byte[] keySerialized;
  private final byte[] value;
  private transient Object keyDeserialized;

  public SerializationElementSnapshot(SerializationStrategy strategy, Element element) throws IOException {
    super(element);

    this.keySerialized = strategy.serialize(element.getKey());
    this.value = strategy.serialize(element.getValue());
  }

  @Override
  public synchronized Object getKey(SerializationStrategy strategy) throws IOException, ClassNotFoundException {
    if (null == keyDeserialized) {
      keyDeserialized = strategy.deserialize(keySerialized);
    }

    return keyDeserialized;
  }

  @Override
  public Object getValue(SerializationStrategy strategy) throws IOException, ClassNotFoundException {
    return strategy.deserialize(value);
  }
}

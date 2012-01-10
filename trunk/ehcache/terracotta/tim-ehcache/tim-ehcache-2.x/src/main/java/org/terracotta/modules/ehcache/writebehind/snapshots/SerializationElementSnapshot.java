/**
 *  Copyright 2003-2009 Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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

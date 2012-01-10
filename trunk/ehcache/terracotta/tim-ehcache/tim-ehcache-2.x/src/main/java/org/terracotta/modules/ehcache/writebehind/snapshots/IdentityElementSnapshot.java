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

import org.terracotta.annotations.InstrumentedClass;
import org.terracotta.cache.serialization.SerializationStrategy;

@InstrumentedClass
public class IdentityElementSnapshot extends ElementSnapshot {
  private final Object key;
  private final Object value;

  public IdentityElementSnapshot(Element element) {
    super(element);

    this.key = element.getObjectKey();
    this.value = element.getObjectValue();
  }

  @Override
  public Object getKey(SerializationStrategy strategy) {
    return key;
  }

  @Override
  public Object getValue(SerializationStrategy strategy) {
    return value;
  }
}

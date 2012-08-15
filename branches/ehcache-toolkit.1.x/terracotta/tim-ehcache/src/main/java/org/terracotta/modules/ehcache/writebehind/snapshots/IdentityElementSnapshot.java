/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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

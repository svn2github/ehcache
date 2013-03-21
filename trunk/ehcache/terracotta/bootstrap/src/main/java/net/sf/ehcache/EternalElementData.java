/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package net.sf.ehcache;

import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Serialized representation of an eternal {@link Element}
 */
public class EternalElementData extends ElementData {

  public EternalElementData() {
    // for serialization
  }

  public EternalElementData(final Element element) {
    super(element);

  }

  @Override
  public Element createElement(final Object key) {
    Element element = new Element(key, value, version, creationTime, lastAccessTime, hitCount, cacheDefaultLifespan, 0,
                                  0, lastUpdateTime);
    return element;
  }


  @Override
  protected void writeAttributes(ObjectOutput oos) {
    // Do Nothing
  }


  @Override
  protected void readAttributes(ObjectInput in) {
    // Do Nothing
  }


}

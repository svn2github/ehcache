/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package net.sf.ehcache;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Serialized representation of non-eternal {@link Element}
 */
public class NonEternalElementData extends ElementData {
  private volatile int     timeToLive;
  private volatile int     timeToIdle;

  public NonEternalElementData() {
    // for serialization
  }

  public NonEternalElementData(final Element element) {
    super(element);
    timeToIdle = element.getTimeToIdle();
    timeToLive = element.getTimeToLive();
  }

  @Override
  public Element createElement(final Object key) {
    Element element = new Element(key, value, version, creationTime, lastAccessTime, hitCount, cacheDefaultLifespan,
                                  timeToLive, timeToIdle, lastUpdateTime);
    return element;
  }

  @Override
  protected void writeAttributes(ObjectOutput oos) throws IOException {
    oos.writeInt(timeToLive);
    oos.writeInt(timeToIdle);
  }

  @Override
  protected void readAttributes(ObjectInput in) throws IOException {
    timeToLive = in.readInt();
    timeToIdle = in.readInt();
  }

}

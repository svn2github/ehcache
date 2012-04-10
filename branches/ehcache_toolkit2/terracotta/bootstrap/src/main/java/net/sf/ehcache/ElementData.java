package net.sf.ehcache;
/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */


import net.sf.ehcache.util.TimeUtil;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * @author Nishant
 */
public class ElementData implements Serializable {
  private final Object  value;
  private final long    version;
  private final long    creationTime;
  private long          lastAccessTime;
  private final long    hitCount;
  private final boolean cacheDefaultLifespan;
  private final int     timeToLive;
  private final int     timeToIdle;
  private final long    lastUpdateTime;

  public ElementData(final Object value, final long version, final long creationTime, final long lastAccessTime,
                     final long hitCount, final boolean cacheDefaultLifespan, final int timeToLive,
                     final int timeToIdle, final long lastUpdateTime) {
    this.value = value;
    this.version = version;
    this.creationTime = creationTime;
    this.lastAccessTime = lastAccessTime;
    this.hitCount = hitCount;
    this.cacheDefaultLifespan = cacheDefaultLifespan;
    this.timeToLive = timeToLive;
    this.timeToIdle = timeToIdle;
    this.lastUpdateTime = lastUpdateTime;
  }

  public ElementData(final Element element) {
    this(element.getValue(), element.getVersion(), element.getCreationTime(), element.getLastAccessTime(), element
        .getHitCount(), element.isLifespanSet(), element.getTimeToLive(), element.getTimeToIdle(), element
        .getLastUpdateTime());
  }

  public Element createElement(final Object key) {
    return new Element(key, value, version, creationTime, lastAccessTime, hitCount, cacheDefaultLifespan, timeToLive,
                       timeToIdle, lastUpdateTime);
  }

  public Object getValue() {
    return value;
  }

  public int getLastAccessedTime() {
    return TimeUtil.toSecs(lastAccessTime);
  }

  protected void setLastAccessedTimeInternal(final int usedAtTime) {
    this.lastAccessTime = TimeUtil.toMillis(usedAtTime);
  }

  public int getCreateTime() {
    return TimeUtil.toSecs(creationTime);
  }

  public static ElementData create(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    ElementData element = new ElementData(ois.readObject(), ois.readLong(), ois.readLong(), ois.readLong(),
                                          ois.readLong(), ois.readBoolean(), ois.readInt(), ois.readInt(),
                                          ois.readLong());
    return element;
  }

  public void write(final ObjectOutputStream oos) throws IOException {
    oos.writeObject(value);
    oos.writeLong(version);
    oos.writeLong(creationTime);
    oos.writeLong(lastAccessTime);
    oos.writeLong(hitCount);
    oos.writeBoolean(cacheDefaultLifespan);
    oos.writeInt(timeToLive);
    oos.writeInt(timeToIdle);
    oos.writeLong(lastUpdateTime);
  }

}

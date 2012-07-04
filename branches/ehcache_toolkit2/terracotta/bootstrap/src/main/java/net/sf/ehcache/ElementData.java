package net.sf.ehcache;

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

import net.sf.ehcache.util.TimeUtil;

import org.terracotta.toolkit.internal.collections.TimestampedValue;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Serialized representation of net.sf.ehcache.Element
 * 
 * @author Nishant
 */
public class ElementData implements Externalizable, TimestampedValue {
  private volatile Object  value;
  private volatile long    version;
  private volatile long    creationTime;
  private volatile long    lastAccessTime;
  private volatile long    hitCount;
  private volatile boolean cacheDefaultLifespan;
  private volatile int     timeToLive;
  private volatile int     timeToIdle;
  private volatile long    lastUpdateTime;

  public ElementData() {
    // for serialization
  }

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
        .getHitCount(), element.usesCacheDefaultLifespan(), element.getTimeToLive(), element.getTimeToIdle(), element
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

  @Override
  public void writeExternal(ObjectOutput oos) throws IOException {
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

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    value = in.readObject();
    version = in.readLong();
    creationTime = in.readLong();
    lastAccessTime = in.readLong();
    hitCount = in.readLong();
    cacheDefaultLifespan = in.readBoolean();
    timeToLive = in.readInt();
    timeToIdle = in.readInt();
    lastUpdateTime = in.readLong();
  }

  @Override
  public void updateTimestamps(int createTime, int lastAccessedTime) {
    setLastAccessedTimeInternal(lastAccessedTime);
  }

}

package net.sf.ehcache;

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

import net.sf.ehcache.util.TimeUtil;

import org.terracotta.toolkit.internal.cache.TimestampedValue;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Serialized representation of net.sf.ehcache.Element
 * 
 * @author Nishant
 */
public abstract class ElementData implements Externalizable, TimestampedValue {
  protected volatile Object  value;
  protected volatile long    version;
  protected volatile long    creationTime;
  protected volatile long    lastAccessTime;
  protected volatile long    hitCount;
  protected volatile boolean cacheDefaultLifespan;
  protected volatile long    lastUpdateTime;

  public ElementData() {
    // for serialization
  }

  public ElementData(final Element element) {
    this.value = element.getValue();
    this.version = element.getVersion();
    this.creationTime = element.getCreationTime();
    this.lastAccessTime = element.getLastAccessTime();
    this.hitCount = element.getHitCount();
    this.cacheDefaultLifespan = element.usesCacheDefaultLifespan();
    this.lastUpdateTime = element.getLastUpdateTime();
  }

  /**
   * This method should return the {@link Element} object constructed from fields of this ElementData.
   * 
   * @param key the key which the returned {@link Element} object should have.
   * @return the Element object constructed from this ElementData.
   */
  public abstract Element createElement(final Object key);

  /**
   * The subclasses must implement this method to persist any subclass specific attribute while serialization <br>
   * It will be called when this object is being serialized.
   * 
   * @param oos the {@link ObjectOutput} object on which the attributes must be written
   */
  protected abstract void writeAttributes(ObjectOutput oos) throws IOException;

  /**
   * The subclasses must implement this method to read any subclass specific attributes while deserialization <br>
   * It will be called when this object is being deserialized.
   * 
   * @param in the {@link ObjectInput} object from which the attributes must be read.
   */
  protected abstract void readAttributes(ObjectInput in) throws IOException;


  @Override
  public void writeExternal(ObjectOutput oos) throws IOException {
    oos.writeObject(value);
    oos.writeLong(version);
    oos.writeLong(creationTime);
    oos.writeLong(lastAccessTime);
    oos.writeLong(hitCount);
    oos.writeBoolean(cacheDefaultLifespan);
    oos.writeLong(lastUpdateTime);

    writeAttributes(oos);
  }


  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    value = in.readObject();
    version = in.readLong();
    creationTime = in.readLong();
    lastAccessTime = in.readLong();
    hitCount = in.readLong();
    cacheDefaultLifespan = in.readBoolean();
    lastUpdateTime = in.readLong();
    
    readAttributes(in);
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
  public void updateTimestamps(int createTime, int lastAccessedTime) {
    setLastAccessedTimeInternal(lastAccessedTime);
  }

}

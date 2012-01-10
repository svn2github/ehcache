package net.sf.ehcache;

import net.sf.ehcache.util.TimeUtil;

import org.terracotta.cache.CacheConfig;
import org.terracotta.cache.ImmutableConfig;
import org.terracotta.cache.evictor.CapacityEvictionPolicyData;
import org.terracotta.cache.value.AbstractStatelessTimestampedValue;

import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * @author Alex Snaps
 */
public abstract class AbstractElementData extends AbstractStatelessTimestampedValue<Object> {
  protected final Object  value;
  protected final long    version;
  protected final long    creationTime;
  protected       long    lastAccessTime;
  protected final long    hitCount;
  protected final boolean cacheDefaultLifespan;
  protected final int     timeToLive;
  protected final int     timeToIdle;
  protected final long    lastUpdateTime;

  protected AbstractElementData(final Object value, final long version, final long creationTime, final long lastAccessTime,
                     final long hitCount, final boolean cacheDefaultLifespan,
                     final int timeToLive, final int timeToIdle, final long lastUpdateTime) {
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

  public abstract void setCapacityEvictionPolicyData(CapacityEvictionPolicyData capacityEvictionPolicyData);

  public abstract CapacityEvictionPolicyData getCapacityEvictionPolicyData();

  protected abstract CapacityEvictionPolicyData fastGetCapacityEvictionPolicyData();

  public Element createElement(final Object key) {
    return new Element(key, value, version, creationTime, lastAccessTime, hitCount, cacheDefaultLifespan,
                       timeToLive, timeToIdle, lastUpdateTime);
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
  public int expiresAt(CacheConfig config) {
    if (hasCustomLifespan()) {
      return customLifespanExpiresAt(config);
    } else {
      return super.expiresAt(config);
    }
  }

  private boolean hasCustomLifespan() {
    return !cacheDefaultLifespan;
  }

  private int customLifespanExpiresAt(CacheConfig config) {
    CacheConfig customConfig = new ImmutableConfig("custom", false, timeToIdle, timeToLive, false, 0, 0, 0, null, config.isServerMap());
    return super.expiresAt(customConfig);
  }
}

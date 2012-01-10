package net.sf.ehcache;

import org.terracotta.cache.evictor.CapacityEvictionPolicyData;

import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * @author Alex Snaps
 */
public class SerializationModeElementData extends AbstractElementData {

  public SerializationModeElementData(final Element element) {
    this(element.getObjectValue(), element.getVersion(), element.getCreationTime(), element.getLastAccessTime(),
         element.getHitCount(), element.usesCacheDefaultLifespan(), element.getTimeToLive(), element.getTimeToIdle(),
         element.getLastUpdateTime());
  }

  public SerializationModeElementData(final Object value, final long version, final long creationTime,
                                      final long lastAccessTime, final long hitCount,
                                      final boolean cacheDefaultLifespan, final int timeToLive, final int timeToIdle,
                                      final long lastUpdateTime) {
    super(value, version, creationTime, lastAccessTime, hitCount, cacheDefaultLifespan, timeToLive, timeToIdle,
          lastUpdateTime);
  }

  public static AbstractElementData create(final ObjectInputStream ois) throws IOException, ClassNotFoundException {
    AbstractElementData element = new SerializationModeElementData(ois.readObject(), ois.readLong(), ois.readLong(),
                                                                   ois.readLong(), ois.readLong(), ois.readBoolean(),
                                                                   ois.readInt(), ois.readInt(), ois.readLong());
    return element;
  }

  @Override
  public final void setCapacityEvictionPolicyData(final CapacityEvictionPolicyData capacityEvictionPolicyData) {
    throw new UnsupportedOperationException("Operation not supported in Serialization mode!");
  }

  @Override
  public final CapacityEvictionPolicyData getCapacityEvictionPolicyData() {
    throw new UnsupportedOperationException("Operation not supported in Serialization mode!");
  }

  @Override
  protected final CapacityEvictionPolicyData fastGetCapacityEvictionPolicyData() {
    throw new UnsupportedOperationException("Operation not supported in Serialization mode!");
  }
}

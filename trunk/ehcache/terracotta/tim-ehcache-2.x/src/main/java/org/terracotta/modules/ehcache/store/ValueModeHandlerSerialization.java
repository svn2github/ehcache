/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.AbstractElementData;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.SerializationModeElementData;
import net.sf.ehcache.util.TimeUtil;

import org.terracotta.bytecode.NotClearable;
import org.terracotta.cache.TimestampedValue;
import org.terracotta.cache.serialization.CustomLifespanSerializedEntry;
import org.terracotta.cache.serialization.SerializationStrategy3;
import org.terracotta.cache.serialization.SerializedEntry;
import org.terracotta.cache.serialization.SerializedEntryParameters;

class ValueModeHandlerSerialization implements ValueModeHandler, NotClearable {

  // clustered reference
  private final ClusteredStore                              store;
  private final SerializationStrategy3<AbstractElementData> serializationStrategy;

  // not clustered - reset via init()
  private transient volatile ClassLoader                    threadContextAwareClassLoader;

  private final boolean                                     copyOnRead;

  ValueModeHandlerSerialization(final ClusteredStore store, boolean copyOnRead, boolean compress) {
    this(store, copyOnRead, new ElementSerializationStrategy(compress));
  }

  ValueModeHandlerSerialization(final ClusteredStore store, boolean copyOnRead,
                                SerializationStrategy3<AbstractElementData> serializationStrategy) {
    this.store = store;
    this.copyOnRead = copyOnRead;
    this.serializationStrategy = serializationStrategy;
    init();
  }

  // onLoad method - used when clustered version of this object is faulted into another node
  private void init() {
    this.threadContextAwareClassLoader = new ThreadContextAwareClassLoader(
                                                                           ValueModeHandlerSerialization.class
                                                                               .getClassLoader());
    loadReferences();
  }

  public void loadReferences() {
    // access fields here to fault in from cluster, if not already
    serializationStrategy.getClass();
  }

  public Object createPortableKey(Object key) {
    return generateStringKeyFor(key);
  }

  public TimestampedValue createTimestampedValue(final Element element) {
    SerializationModeElementData value = new SerializationModeElementData(element);
    int createTime = TimeUtil.toSecs(element.getElementEvictionData().getCreationTime());

    byte[] data;
    try {
      data = serializationStrategy.serialize(value);
    } catch (Exception e) {
      throw new CacheException(e);
    }

    // we can store null in the entry if we're doing copy on read
    value = copyOnRead ? null : value;

    SerializedEntryParameters<SerializationModeElementData> params = new SerializedEntryParameters<SerializationModeElementData>();
    params.deserialized(value).createTime(createTime).lastAccessedTime(createTime).serialized(data);

    if (element.usesCacheDefaultLifespan()) {
      return new SerializedEntry<SerializationModeElementData>(params);
    } else {
      int tti = element.getTimeToIdle();
      int ttl = element.getTimeToLive();
      return new CustomLifespanSerializedEntry<SerializationModeElementData>(params, tti, ttl);
    }
  }

  public void processStoredValue(final TimestampedValue value) {
    if (!copyOnRead) {
      SerializedEntry serializedEntry = (SerializedEntry) value;
      try {
        // serializedEntry.getDeserializedValue(serializationStrategy, threadContextAwareClassLoader);
        serializedEntry.nullByteArray();
      } catch (Exception e) {
        throw new CacheException(e);
      }
    }
  }

  public Element createElement(final Object key, final TimestampedValue value) {
    if (null == value) { return null; }

    SerializedEntry<AbstractElementData> entry = (SerializedEntry<AbstractElementData>) value;
    AbstractElementData data;
    try {
      if (copyOnRead) {
        data = entry.getDeserializedValueCopy(serializationStrategy, threadContextAwareClassLoader);
      } else {
        data = entry.getDeserializedValue(serializationStrategy, threadContextAwareClassLoader);
      }
    } catch (Exception e) {
      throw new CacheException(e);
    }
    // recalculate local cache size after value has been deserialized
    boolean recalculateSize = entry.shouldRecalculateSize();
    if (recalculateSize) {
      store.getBackend().recalculateLocalCacheSize(key);
    }
    Element element = data.createElement(key);
    element.setElementEvictionData(new ClusteredElementEvictionData(store, value));
    return element;
  }

  /**
   * Generates a String key for the supplied object.
   */
  private String generateStringKeyFor(final Object obj) {
    try {
      return this.serializationStrategy.generateStringKeyFor(obj);
    } catch (Exception e) {
      throw new CacheException(e);
    }
  }

  public Object getRealKeyObject(Object portableKey) {
    return realKeyObjectFor(portableKey, false);
  }

  public Object localGetRealKeyObject(Object portableKey) {
    return realKeyObjectFor(portableKey, true);
  }

  private Object realKeyObjectFor(Object portableKey, boolean local) {
    try {
      if (local) {
        return this.serializationStrategy
            .localDeserializeStringKey((String) portableKey, threadContextAwareClassLoader);
      } else {
        return this.serializationStrategy.deserializeStringKey((String) portableKey, threadContextAwareClassLoader);
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new CacheException(e);
    }
  }

}

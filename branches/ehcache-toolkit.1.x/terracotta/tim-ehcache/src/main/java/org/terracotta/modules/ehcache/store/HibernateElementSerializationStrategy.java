/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import org.hibernate.cache.CacheKey;
import org.terracotta.collections.ConcurrentDistributedMap;
import org.terracotta.locking.LockType;
import org.terracotta.locking.TerracottaLock;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map.Entry;

/**
 * @author Chris Dennis
 */
public class HibernateElementSerializationStrategy extends ElementSerializationStrategy {

  private static final int                                NON_CACHE_KEY_MAPPING = -1;

  private final ConcurrentDistributedMap<String, Integer> entityRoleMappings    = new ConcurrentDistributedMap<String, Integer>();

  public HibernateElementSerializationStrategy(boolean compress) {
    super(compress);
  }

  /**
   * Special cases the CacheKey class in order to reduce the size of the generated Strings.
   */
  @Override
  protected void writeStringKey(final Object key, final ObjectOutputStream oos) throws IOException {
    if (key instanceof CacheKey) {
      CacheKey cacheKey = (CacheKey) key;
      oos.writeInt(getOrCreateMapping(cacheKey.getEntityOrRoleName()));
      oos.writeObject(cacheKey.getKey());
    } else {
      oos.writeInt(NON_CACHE_KEY_MAPPING);
      super.writeStringKey(key, oos);
    }
  }

  @Override
  protected Object readStringKey(final ObjectInputStream ois) throws IOException, ClassNotFoundException {
    int mapping = ois.readInt();
    if (mapping == NON_CACHE_KEY_MAPPING) {
      return super.readStringKey(ois);
    } else {
      Serializable innerKey = (Serializable) ois.readObject();
      return new CacheKey(innerKey, new UnknownHibernateType(), getEntityOrRoleName(mapping), null, null);
    }
  }

  private int getOrCreateMapping(final String entityOrRoleName) {
    Integer mapping = entityRoleMappings.unsafeGet(entityOrRoleName);

    if (mapping == null) {
      final TerracottaLock lock = new TerracottaLock(entityRoleMappings, LockType.WRITE);
      lock.lock();
      try {
        mapping = entityRoleMappings.get(entityOrRoleName);
        if (mapping == null) {
          mapping = Integer.valueOf(entityRoleMappings.size());
          Integer prev = entityRoleMappings.put(entityOrRoleName, mapping);
          if (null != prev) { throw new AssertionError("mapping exists for " + entityOrRoleName + "(" + prev + ")"); }
        }
      } finally {
        lock.unlock();
      }
    }

    return mapping.intValue();
  }

  private String getEntityOrRoleName(int encoding) {
    for (Entry<String, Integer> e : entityRoleMappings.entrySet()) {
      Integer value = e.getValue();
      if (value != null && value.intValue() == encoding) { return e.getKey(); }
    }
    return null;
  }
}

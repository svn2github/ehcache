/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Element;
import net.sf.ehcache.ElementEvictionData;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.util.TimeUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.cache.TimestampedValue;

public class ClusteredElementEvictionData implements ElementEvictionData {
  private static final Logger    LOG = LoggerFactory.getLogger(ClusteredElementEvictionData.class.getName());

  private final transient Store  store;
  private final TimestampedValue value;

  /**
   * Default constructor initializing the field to their empty values
   */
  public ClusteredElementEvictionData(final Store store, final TimestampedValue timestampedValue) {
    this.store = store;
    this.value = timestampedValue;
  }

  /**
   * {@inheritDoc}
   * <p/>
   * Doesn't do anything in this implementation.
   */
  public void setCreationTime(final long creationTime) {
    // no-op
  }

  /**
   * {@inheritDoc}
   */
  public long getCreationTime() {
    return TimeUtil.toMillis(value.getCreateTime());
  }

  /**
   * {@inheritDoc}
   */
  public long getLastAccessTime() {
    return TimeUtil.toMillis(value.getLastAccessedTime());
  }

  /**
   * {@inheritDoc}
   */
  public void updateLastAccessTime(final long time, final Element element) {
    setLastAccessTime(TimeUtil.toSecs(time), element, store);
  }

  /**
   * {@inheritDoc}
   */
  public void resetLastAccessTime(final Element element) {
    setLastAccessTime(value.getCreateTime(), element, store);
  }

  private void setLastAccessTime(int time, final Element element, final Store store) {
    if (null == store) { throw new IllegalArgumentException("store can't be null"); }
    if (!(store instanceof ClusteredStore)) { throw new IllegalArgumentException(
                                                                                 "store is expected to be a ClusteredStore"); }
    if (time < value.getCreateTime()) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Last access time " + time + " of element with key '" + element.getObjectKey()
                 + "' is earlier than its creation time " + value.getCreateTime()
                 + ". Setting it to the creation time.");
      }
      time = value.getCreateTime();
    }

    ClusteredStore clusteredStore = (ClusteredStore) store;
    ClusteredStoreBackend backend = clusteredStore.getBackend();
    value.markUsed(time, backend.createFinegrainedLock(clusteredStore.generatePortableKeyFor(element.getObjectKey())),
                   backend.getConfig());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final ElementEvictionData clone() throws CloneNotSupportedException {
    return (ClusteredElementEvictionData) super.clone();
  }

  /**
   * {@inheritDoc}
   */
  public boolean canParticipateInSerialization() {
    return true;
  }
}
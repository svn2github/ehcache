/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store.backend;

import org.terracotta.cache.TimestampedValue;
import org.terracotta.meta.MetaData;

public class ClusteredElement {
  private final Object           objectKey;
  private final Object           portableKey;
  private final MetaData         searchMetaData;
  private final TimestampedValue value;

  public ClusteredElement(Object objectKey, Object portableKey, MetaData searchMetaData, TimestampedValue value) {
    this.objectKey = objectKey;
    this.portableKey = portableKey;
    this.searchMetaData = searchMetaData;
    this.value = value;
  }

  public Object getObjectKey() {
    return objectKey;
  }

  public Object getPortableKey() {
    return portableKey;
  }

  public MetaData getSearchMetaData() {
    return searchMetaData;
  }

  public TimestampedValue getTimeStampedValue() {
    return value;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((objectKey == null) ? 0 : objectKey.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (!(obj instanceof ClusteredElement)) return false;
    ClusteredElement other = (ClusteredElement) obj;
    if (objectKey == null) {
      if (other.objectKey != null) return false;
    } else if (!objectKey.equals(other.objectKey)) return false;
    return true;
  }

}

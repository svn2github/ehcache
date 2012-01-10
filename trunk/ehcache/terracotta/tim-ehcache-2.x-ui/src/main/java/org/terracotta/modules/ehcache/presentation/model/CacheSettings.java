/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation.model;

public class CacheSettings extends MutableCacheSettings implements ICacheSettings {
  private final String  memoryStoreEvictionPolicy;
  private final boolean diskPersistent;
  private final boolean eternal;
  private final boolean overflowToDisk;

  public CacheSettings(ClusteredCacheModel cacheModel) {
    super(cacheModel);

    this.memoryStoreEvictionPolicy = cacheModel.getMemoryStoreEvictionPolicy();
    this.diskPersistent = cacheModel.isDiskPersistent();
    this.eternal = cacheModel.isEternal();
    this.overflowToDisk = cacheModel.isOverflowToDisk();
  }

  public String getMemoryStoreEvictionPolicy() {
    return memoryStoreEvictionPolicy;
  }

  public boolean isDiskPersistent() {
    return diskPersistent;
  }

  public boolean isEternal() {
    return eternal;
  }

  public boolean isOverflowToDisk() {
    return overflowToDisk;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + (diskPersistent ? 1231 : 1237);
    result = prime * result + (eternal ? 1231 : 1237);
    result = prime * result + ((memoryStoreEvictionPolicy == null) ? 0 : memoryStoreEvictionPolicy.hashCode());
    result = prime * result + (overflowToDisk ? 1231 : 1237);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) { return true; }
    if (!super.equals(obj)) { return false; }
    if (!(obj instanceof CacheSettings)) { return false; }
    CacheSettings other = (CacheSettings) obj;
    if (diskPersistent != other.diskPersistent) { return false; }
    if (eternal != other.eternal) { return false; }
    if (memoryStoreEvictionPolicy == null) {
      if (other.memoryStoreEvictionPolicy != null) { return false; }
    } else if (!memoryStoreEvictionPolicy.equals(other.memoryStoreEvictionPolicy)) { return false; }
    if (overflowToDisk != other.overflowToDisk) { return false; }
    return true;
  }
}

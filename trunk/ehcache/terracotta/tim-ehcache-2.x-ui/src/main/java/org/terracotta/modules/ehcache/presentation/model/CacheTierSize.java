/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation.model;

public class CacheTierSize {
  private final long maxEntriesLocalHeap;
  private final long localHeapSize;
  private final long localHeapMissRate;

  private final long maxEntriesLocalOffHeap;
  private final long localOffHeapSize;
  private final long localOffHeapMissRate;

  private final long maxEntriesLocalDisk;
  private final long localDiskSize;
  private final long localDiskMissRate;

  private final long maxBytesLocalHeap;
  private final long localHeapSizeInBytes;

  private final long maxBytesLocalOffHeap;
  private final long localOffHeapSizeInBytes;

  private final long maxBytesLocalDisk;
  private final long localDiskSizeInBytes;

  public CacheTierSize(long maxEntriesLocalHeap, long localHeapSize, long localOffHeapSize, long maxEntriesLocalDisk,
                       long localDiskSize, long maxBytesLocalHeap, long localHeapSizeInBytes,
                       long maxBytesLocalOffHeap, long localOffHeapSizeInBytes, long maxBytesLocalDisk,
                       long localDiskSizeInBytes, long localHeapMissRate, long localOffHeapMissRate,
                       long localDiskMissRate) {
    this.maxEntriesLocalHeap = maxEntriesLocalHeap;
    this.maxBytesLocalHeap = maxBytesLocalHeap;
    this.localHeapSize = localHeapSize;
    this.localHeapMissRate = localHeapMissRate;
    this.localHeapSizeInBytes = localHeapSizeInBytes;

    this.maxEntriesLocalOffHeap = 0; // some day
    this.maxBytesLocalOffHeap = maxBytesLocalOffHeap;
    this.localOffHeapSize = localOffHeapSize;
    this.localOffHeapMissRate = localOffHeapMissRate;
    this.localOffHeapSizeInBytes = localOffHeapSizeInBytes;

    this.maxEntriesLocalDisk = maxEntriesLocalDisk;
    this.maxBytesLocalDisk = maxBytesLocalDisk;
    this.localDiskSize = localDiskSize;
    this.localDiskMissRate = localDiskMissRate;
    this.localDiskSizeInBytes = localDiskSizeInBytes;
  }

  public long getMaxLocalHeapValue(boolean inBytes) {
    return inBytes ? getMaxBytesLocalHeap() : getMaxEntriesLocalHeap();
  }

  public long getMaxEntriesLocalHeap() {
    return maxEntriesLocalHeap;
  }

  public long getMaxBytesLocalHeap() {
    return maxBytesLocalHeap;
  }

  public long getLocalHeapValue(boolean inBytes) {
    return inBytes ? getLocalHeapSizeInBytes() : getLocalHeapSize();
  }

  public long getLocalHeapSize() {
    return localHeapSize;
  }

  public long getLocalHeapSizeInBytes() {
    return localHeapSizeInBytes;
  }

  public long getLocalHeapMissRate() {
    return localHeapMissRate;
  }

  public long getMaxLocalOffHeapValue(boolean inBytes) {
    return inBytes ? getMaxBytesLocalOffHeap() : getMaxEntriesLocalOffHeap();
  }

  public long getMaxEntriesLocalOffHeap() {
    return maxEntriesLocalOffHeap;
  }

  public long getMaxBytesLocalOffHeap() {
    return maxBytesLocalOffHeap;
  }

  public long getLocalOffHeapValue(boolean inBytes) {
    return inBytes ? getLocalOffHeapSizeInBytes() : getLocalOffHeapSize();
  }

  public long getLocalOffHeapSize() {
    return localOffHeapSize;
  }

  public long getLocalOffHeapSizeInBytes() {
    return localOffHeapSizeInBytes;
  }

  public long getLocalOffHeapMissRate() {
    return localOffHeapMissRate;
  }

  public long getMaxLocalDiskValue(boolean inBytes) {
    return inBytes ? getMaxBytesLocalDisk() : getMaxEntriesLocalDisk();
  }

  public long getMaxEntriesLocalDisk() {
    return maxEntriesLocalDisk;
  }

  public long getMaxBytesLocalDisk() {
    return maxBytesLocalDisk;
  }

  public long getLocalDiskValue(boolean inBytes) {
    return inBytes ? getLocalDiskSizeInBytes() : getLocalDiskSize();
  }

  public long getLocalDiskSize() {
    return localDiskSize;
  }

  public long getLocalDiskSizeInBytes() {
    return localDiskSizeInBytes;
  }

  public long getLocalDiskMissRate() {
    return localDiskMissRate;
  }

  public long entriesForTier(String tierName) {
    return valueForTier(false, tierName);
  }

  public long maxEntriesForTier(String tierName) {
    return maxForTier(false, tierName);
  }

  public long sizeInBytesForTier(String tierName) {
    return valueForTier(true, tierName);
  }

  public long valueForTier(boolean inBytes, String tierName) {
    if ("Local Heap".equals(tierName)) {
      return getLocalHeapValue(inBytes);
    } else if ("Local OffHeap".equals(tierName)) {
      return getLocalOffHeapValue(inBytes);
    } else if ("Local Disk".equals(tierName)) {
      return getLocalDiskValue(inBytes);
    } else if ("Remote".equals(tierName)) {
      long entries = getLocalDiskSize();
      if (inBytes) {
        long size = getLocalOffHeapSize();
        if (size > 0) {
          return (long) ((entries * getLocalOffHeapSizeInBytes()) / ((double) size));
        } else if ((size = getLocalHeapSize()) > 0) { return (long) ((entries * getLocalHeapSizeInBytes()) / ((double) size)); }
      } else {
        return entries;
      }
    }
    return 0;
  }

  public long maxBytesForTier(String tierName) {
    return maxForTier(true, tierName);
  }

  public long maxForTier(boolean inBytes, String tierName) {
    if ("Local Heap".equals(tierName)) {
      return getMaxLocalHeapValue(inBytes);
    } else if ("Local OffHeap".equals(tierName)) {
      return getMaxLocalOffHeapValue(inBytes);
    } else if ("Local Disk".equals(tierName)) {
      return getMaxLocalDiskValue(inBytes);
    } else if ("Remote".equals(tierName)) {
      long entries = getMaxEntriesLocalDisk();
      if (inBytes) {
        long size = getLocalOffHeapSize();
        if (size > 0) {
          return (long) ((entries * getLocalOffHeapSizeInBytes()) / ((double) size));
        } else if ((size = getLocalHeapSize()) > 0) { return (long) ((entries * getLocalHeapSizeInBytes()) / ((double) size)); }
      } else {
        return entries;
      }
    }
    return 0;
  }
}

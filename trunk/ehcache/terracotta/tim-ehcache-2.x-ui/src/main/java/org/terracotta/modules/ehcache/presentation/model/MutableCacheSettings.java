/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation.model;

public class MutableCacheSettings implements IMutableCacheSettings, Comparable {
  private final String cacheName;
  private final String shortName;
  private boolean      loggingEnabled;
  private long         maxEntriesLocalHeap;
  private long         maxEntriesLocalDisk;
  private String       maxBytesLocalHeapAsString;
  private final String maxBytesLocalOffHeapAsString;
  private String       maxBytesLocalDiskAsString;
  private long         maxBytesLocalHeap;
  private final long   maxBytesLocalOffHeap;
  private long         maxBytesLocalDisk;
  private long         timeToIdleSeconds;
  private long         timeToLiveSeconds;

  public MutableCacheSettings(SettingsCacheModel cacheModel) {
    this.cacheName = cacheModel.getCacheName();
    this.shortName = cacheModel.getShortName();
    this.maxEntriesLocalHeap = cacheModel.getMaxEntriesLocalHeap();
    this.maxEntriesLocalDisk = cacheModel.getMaxEntriesLocalDisk();
    this.maxBytesLocalHeapAsString = cacheModel.getMaxBytesLocalHeapAsString();
    this.maxBytesLocalOffHeapAsString = cacheModel.getMaxBytesLocalOffHeapAsString();
    this.maxBytesLocalDiskAsString = cacheModel.getMaxBytesLocalDiskAsString();
    this.maxBytesLocalHeap = cacheModel.getMaxBytesLocalHeap();
    this.maxBytesLocalOffHeap = cacheModel.getMaxBytesLocalOffHeap();
    this.maxBytesLocalDisk = cacheModel.getMaxBytesLocalDisk();
    this.timeToIdleSeconds = cacheModel.getTimeToIdleSeconds();
    this.timeToLiveSeconds = cacheModel.getTimeToLiveSeconds();
  }

  public String getCacheName() {
    return cacheName;
  }

  public String getShortName() {
    return shortName;
  }

  public void setMaxEntriesLocalHeap(long maxEntriesLocalHeap) {
    this.maxEntriesLocalHeap = maxEntriesLocalHeap;
  }

  public long getMaxEntriesLocalHeap() {
    return maxEntriesLocalHeap;
  }

  public void setMaxEntriesLocalDisk(long maxElementsOnDisk) {
    this.maxEntriesLocalDisk = maxElementsOnDisk;
  }

  public long getMaxEntriesLocalDisk() {
    return maxEntriesLocalDisk;
  }

  public void setMaxBytesLocalHeap(long maxBytesLocalHeap) {
    this.maxBytesLocalHeap = maxBytesLocalHeap;
  }

  public long getMaxBytesLocalHeap() {
    return maxBytesLocalHeap;
  }

  public void setMaxBytesLocalHeapAsString(String maxBytesLocalHeap) {
    this.maxBytesLocalHeapAsString = maxBytesLocalHeap;
  }

  public String getMaxBytesLocalHeapAsString() {
    return maxBytesLocalHeapAsString;
  }

  public long getMaxBytesLocalOffHeap() {
    return maxBytesLocalOffHeap;
  }

  public String getMaxBytesLocalOffHeapAsString() {
    return maxBytesLocalOffHeapAsString;
  }

  public void setMaxBytesLocalDisk(long maxBytesLocalDisk) {
    this.maxBytesLocalDisk = maxBytesLocalDisk;
  }

  public long getMaxBytesLocalDisk() {
    return maxBytesLocalDisk;
  }

  public void setMaxBytesLocalDiskAsString(String maxBytesLocalDisk) {
    this.maxBytesLocalDiskAsString = maxBytesLocalDisk;
  }

  public String getMaxBytesLocalDiskAsString() {
    return maxBytesLocalDiskAsString;
  }

  public void setTimeToIdleSeconds(long timeToIdleSeconds) {
    this.timeToIdleSeconds = timeToIdleSeconds;
  }

  public long getTimeToIdleSeconds() {
    return timeToIdleSeconds;
  }

  public void setTimeToLiveSeconds(long timeToLiveSeconds) {
    this.timeToLiveSeconds = timeToLiveSeconds;
  }

  public long getTimeToLiveSeconds() {
    return timeToLiveSeconds;
  }

  public boolean isLoggingEnabled() {
    return loggingEnabled;
  }

  public void setLoggingEnabled(boolean loggingEnabled) {
    this.loggingEnabled = loggingEnabled;
  }

  public int compareTo(Object o) {
    MutableCacheSettings other = (MutableCacheSettings) o;
    return getCacheName().compareTo(other.getCacheName());
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((cacheName == null) ? 0 : cacheName.hashCode());
    result = prime * result + (loggingEnabled ? 1231 : 1237);
    result = prime * result + (int) (maxEntriesLocalHeap ^ (maxEntriesLocalHeap >>> 32));
    result = prime * result + (int) (maxEntriesLocalDisk ^ (maxEntriesLocalDisk >>> 32));
    result = prime * result + (int) (maxBytesLocalHeap ^ (maxBytesLocalHeap >>> 32));
    result = prime * result + (int) (maxBytesLocalOffHeap ^ (maxBytesLocalOffHeap >>> 32));
    result = prime * result + (int) (maxBytesLocalDisk ^ (maxBytesLocalDisk >>> 32));
    result = prime * result + (int) (timeToIdleSeconds ^ (timeToIdleSeconds >>> 32));
    result = prime * result + (int) (timeToLiveSeconds ^ (timeToLiveSeconds >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    MutableCacheSettings other = (MutableCacheSettings) obj;
    if (cacheName == null) {
      if (other.cacheName != null) return false;
    } else if (!cacheName.equals(other.cacheName)) return false;
    if (loggingEnabled != other.loggingEnabled) return false;
    if (maxEntriesLocalHeap != other.maxEntriesLocalHeap) return false;
    if (maxEntriesLocalDisk != other.maxEntriesLocalDisk) return false;
    if (maxBytesLocalHeap != other.maxBytesLocalHeap) return false;
    if (maxBytesLocalOffHeap != other.maxBytesLocalOffHeap) return false;
    if (maxBytesLocalDisk != other.maxBytesLocalDisk) return false;
    if (timeToIdleSeconds != other.timeToIdleSeconds) return false;
    if (timeToLiveSeconds != other.timeToLiveSeconds) return false;
    return true;
  }
}

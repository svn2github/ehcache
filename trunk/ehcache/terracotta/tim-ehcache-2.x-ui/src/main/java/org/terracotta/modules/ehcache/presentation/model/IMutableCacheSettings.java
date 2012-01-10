/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation.model;

public interface IMutableCacheSettings {
  String getCacheName();

  String getShortName();

  boolean isLoggingEnabled();

  void setLoggingEnabled(boolean loggingEnabled);

  long getMaxEntriesLocalHeap();

  void setMaxEntriesLocalHeap(long maxEntriesLocalHeap);

  long getMaxEntriesLocalDisk();

  void setMaxEntriesLocalDisk(long maxEntriesLocalDisk);

  String getMaxBytesLocalHeapAsString();

  void setMaxBytesLocalHeapAsString(String maxBytesLocalHeap);

  long getMaxBytesLocalHeap();

  void setMaxBytesLocalHeap(long maxBytesLocalHeap);

  String getMaxBytesLocalOffHeapAsString();

  long getMaxBytesLocalOffHeap();

  String getMaxBytesLocalDiskAsString();

  void setMaxBytesLocalDiskAsString(String maxBytesLocalDisk);

  long getMaxBytesLocalDisk();

  void setMaxBytesLocalDisk(long maxBytesLocalDisk);

  long getTimeToIdleSeconds();

  void setTimeToIdleSeconds(long tti);

  long getTimeToLiveSeconds();

  void setTimeToLiveSeconds(long ttl);
}

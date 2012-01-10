/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation.model;

import org.terracotta.modules.ehcache.presentation.BaseEhcacheRuntimeStatsPanel;
import org.terracotta.modules.ehcache.presentation.EhcachePresentationUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This represents an Ehcache {@link net.sf.ehcache.statistics.LiveCacheStatistics} and is displayed in a
 * {@link BaseEhcacheRuntimeStatsPanel}. These values are mostly raw counters but there are some synthesized values such
 * as CacheHitRation, ShortName, and the GetTimes.
 */
public class CacheStatisticsModel {
  private final Map<String, Object> attributes;
  private final String              shortName;

  /*
   * These are the names of some of the attributes on {@link net.sf.ehcache.management.sampled.SampledCacheMBean},
   * specifically those that are provided by {@link net.sf.ehcache.statistics.LiveCacheStatistics}.
   */

  public static final String        CACHE_NAME                  = "CacheName";
  public static final String        CACHE_HIT_COUNT             = "CacheHitCount";
  public static final String        IN_MEMORY_HIT_COUNT         = "InMemoryHitCount";
  public static final String        OFF_HEAP_HIT_COUNT          = "OffHeapHitCount";
  public static final String        ON_DISK_HIT_COUNT           = "OnDiskHitCount";
  public static final String        CACHE_MISS_COUNT            = "CacheMissCount";
  public static final String        CACHE_MISS_COUNT_EXPIRED    = "CacheMissCountExpired";
  public static final String        IN_MEMORY_MISS_COUNT        = "InMemoryMissCount";
  public static final String        OFF_HEAP_MISS_COUNT         = "OffHeapMissCount";
  public static final String        ON_DISK_MISS_COUNT          = "OnDiskMissCount";
  public static final String        PUT_COUNT                   = "PutCount";
  public static final String        UPDATE_COUNT                = "UpdateCount";
  public static final String        EVICTED_COUNT               = "EvictedCount";
  public static final String        EXPIRED_COUNT               = "ExpiredCount";
  public static final String        REMOVED_COUNT               = "RemovedCount";
  public static final String        AVERAGE_GET_TIME_MILLIS     = "AverageGetTimeMillis";
  public static final String        MIN_GET_TIME_MILLIS         = "MinGetTimeMillis";
  public static final String        MAX_GET_TIME_MILLIS         = "MaxGetTimeMillis";
  public static final String        LOCAL_HEAP_SIZE             = "LocalHeapSize";
  public static final String        LOCAL_HEAP_SIZE_IN_BYTES    = "LocalHeapSizeInBytes";
  public static final String        LOCAL_OFFHEAP_SIZE          = "LocalOffHeapSize";
  public static final String        LOCAL_OFFHEAP_SIZE_IN_BYTES = "LocalOffHeapSizeInBytes";
  public static final String        LOCAL_DISK_SIZE             = "LocalDiskSize";
  public static final String        LOCAL_DISK_SIZE_IN_BYTES    = "LocalDiskSizeInBytes";
  public static final String        WRITER_QUEUE_LENGTH         = "WriterQueueLength";
  public static final String        XA_COMMIT_COUNT             = "XaCommitCount";
  public static final String        XA_ROLLBACK_COUNT           = "XaRollbackCount";

  /*
   * These are the names of attributes synthesized from the MBean attributes.
   */

  public static final String        SHORT_NAME                  = "ShortName";
  public static final String        CACHE_HIT_RATIO             = "CacheHitRatio";

  public static final String[]      MBEAN_ATTRS                 = { CACHE_NAME, CACHE_HIT_RATIO, CACHE_HIT_COUNT,
      IN_MEMORY_HIT_COUNT, OFF_HEAP_HIT_COUNT, ON_DISK_HIT_COUNT, CACHE_MISS_COUNT, CACHE_MISS_COUNT_EXPIRED,
      IN_MEMORY_MISS_COUNT, OFF_HEAP_MISS_COUNT, ON_DISK_MISS_COUNT, PUT_COUNT, UPDATE_COUNT, EVICTED_COUNT,
      EXPIRED_COUNT, REMOVED_COUNT, AVERAGE_GET_TIME_MILLIS, MIN_GET_TIME_MILLIS, MAX_GET_TIME_MILLIS, LOCAL_HEAP_SIZE,
      LOCAL_HEAP_SIZE_IN_BYTES, LOCAL_OFFHEAP_SIZE, LOCAL_OFFHEAP_SIZE_IN_BYTES, LOCAL_DISK_SIZE,
      LOCAL_DISK_SIZE_IN_BYTES, WRITER_QUEUE_LENGTH, XA_COMMIT_COUNT, XA_ROLLBACK_COUNT };

  public static final String[]      ATTRS                       = { CACHE_NAME, SHORT_NAME, CACHE_HIT_RATIO,
      CACHE_HIT_COUNT, IN_MEMORY_HIT_COUNT, OFF_HEAP_HIT_COUNT, ON_DISK_HIT_COUNT, CACHE_MISS_COUNT,
      CACHE_MISS_COUNT_EXPIRED, IN_MEMORY_MISS_COUNT, OFF_HEAP_MISS_COUNT, ON_DISK_MISS_COUNT, PUT_COUNT, UPDATE_COUNT,
      EVICTED_COUNT, EXPIRED_COUNT, REMOVED_COUNT, AVERAGE_GET_TIME_MILLIS, MIN_GET_TIME_MILLIS, MAX_GET_TIME_MILLIS,
      LOCAL_HEAP_SIZE, LOCAL_HEAP_SIZE_IN_BYTES, LOCAL_OFFHEAP_SIZE, LOCAL_OFFHEAP_SIZE_IN_BYTES, LOCAL_DISK_SIZE,
      LOCAL_DISK_SIZE_IN_BYTES, WRITER_QUEUE_LENGTH, XA_COMMIT_COUNT, XA_ROLLBACK_COUNT };

  public static final String[]      HEADERS                     = { "Cache", "Name", "Hit Ratio", "Hits",
      "InMemory Hits", "OffHeap Hits", "OnDisk Hits", "Misses", "Misses, Expired", "InMemory Misses", "OffHeap Misses",
      "OnDisk Misses", "Puts", "Updates", "Evicted", "Expired", "Removed", "Avg. Get Time (ms.)", "Min Get Time (ms.)",
      "Max Get Time (ms.)", "Local Heap Size", "Local Heap Bytes", "Local OffHeap Size", "Local OffHeap Bytes",
      "Local Disk Size", "Local Disk Bytes", "Writer Q Length", "XA Commits", "XA Rollbacks" };

  public CacheStatisticsModel(String cacheName) {
    this.attributes = new HashMap<String, Object>();
    this.attributes.put(CACHE_NAME, cacheName);
    this.shortName = EhcachePresentationUtils.determineShortName(cacheName);
  }

  public CacheStatisticsModel(Map<String, Object> attributes) {
    this.attributes = attributes;
    this.shortName = EhcachePresentationUtils.determineShortName(getCacheName());
    updateCacheHitRatio();
  }

  public static String[] attributes(String[] headers) {
    List<String> list = new ArrayList<String>();
    List<String> headerList = Arrays.asList(HEADERS);
    for (String header : headers) {
      int index = headerList.indexOf(header);
      if (index < ATTRS.length) {
        list.add(ATTRS[index]);
      }
    }
    return list.toArray(new String[0]);
  }

  public static String[] headersForAttributes(String[] attributes) {
    List<String> list = new ArrayList<String>();
    List<String> attrList = Arrays.asList(ATTRS);
    for (String attr : attributes) {
      int index = attrList.indexOf(attr);
      if (index > -1 && index < HEADERS.length) {
        list.add(HEADERS[index]);
      } else {
        list.add(attr);
      }
    }
    return list.toArray(new String[0]);
  }

  public void add(CacheStatisticsModel cacheStats) {
    attributes.put(CACHE_HIT_COUNT, getCacheHitCount() + cacheStats.getCacheHitCount());
    attributes.put(IN_MEMORY_HIT_COUNT, getInMemoryHitCount() + cacheStats.getInMemoryHitCount());
    attributes.put(OFF_HEAP_HIT_COUNT, getOffHeapHitCount() + cacheStats.getOffHeapHitCount());
    attributes.put(ON_DISK_HIT_COUNT, getOnDiskHitCount() + cacheStats.getOnDiskHitCount());
    attributes.put(CACHE_MISS_COUNT, getCacheMissCount() + cacheStats.getCacheMissCount());
    attributes.put(CACHE_MISS_COUNT_EXPIRED, getCacheMissCountExpired() + cacheStats.getCacheMissCountExpired());
    attributes.put(IN_MEMORY_MISS_COUNT, getInMemoryMissCount() + cacheStats.getInMemoryMissCount());
    attributes.put(OFF_HEAP_MISS_COUNT, getOffHeapMissCount() + cacheStats.getOffHeapMissCount());
    attributes.put(ON_DISK_MISS_COUNT, getOnDiskMissCount() + cacheStats.getOnDiskMissCount());
    attributes.put(PUT_COUNT, getPutCount() + cacheStats.getPutCount());
    attributes.put(UPDATE_COUNT, getUpdateCount() + cacheStats.getUpdateCount());
    attributes.put(EVICTED_COUNT, getEvictedCount() + cacheStats.getEvictedCount());
    attributes.put(EXPIRED_COUNT, getExpiredCount() + cacheStats.getExpiredCount());
    attributes.put(REMOVED_COUNT, getRemovedCount() + cacheStats.getRemovedCount());
    attributes.put(LOCAL_HEAP_SIZE, getLocalHeapSize() + cacheStats.getLocalHeapSize());
    attributes.put(LOCAL_HEAP_SIZE_IN_BYTES, getLocalHeapSizeInBytes() + cacheStats.getLocalHeapSizeInBytes());
    attributes.put(LOCAL_OFFHEAP_SIZE, getLocalOffHeapSize() + cacheStats.getLocalOffHeapSize());
    attributes.put(LOCAL_OFFHEAP_SIZE_IN_BYTES, getLocalOffHeapSizeInBytes() + cacheStats.getLocalOffHeapSizeInBytes());
    attributes.put(LOCAL_DISK_SIZE, getLocalDiskSize() + cacheStats.getLocalDiskSize());
    attributes.put(LOCAL_DISK_SIZE_IN_BYTES, getLocalDiskSizeInBytes() + cacheStats.getLocalDiskSizeInBytes());
    attributes.put(WRITER_QUEUE_LENGTH, getWriterQueueLength() + cacheStats.getWriterQueueLength());
    attributes.put(XA_COMMIT_COUNT, getXaCommitCount() + cacheStats.getXaCommitCount());
    attributes.put(XA_ROLLBACK_COUNT, getXaRollbackCount() + cacheStats.getXaRollbackCount());

    updateCacheHitRatio();
  }

  private void updateCacheHitRatio() {
    long hits = getCacheHitCount();
    long misses = getCacheMissCount();
    double tries = hits + misses;
    double cacheHitRatio = tries > 0 ? hits / tries : 0;
    attributes.put(CACHE_HIT_RATIO, Double.valueOf(cacheHitRatio));
  }

  public String getCacheName() {
    return (String) attributes.get(CACHE_NAME);
  }

  public String getShortName() {
    return shortName != null ? shortName : getCacheName();
  }

  public double getCacheHitRatio() {
    Double result = (Double) attributes.get(CACHE_HIT_RATIO);
    return result == null ? 0 : result;
  }

  public long getCacheHitCount() {
    Long result = (Long) attributes.get(CACHE_HIT_COUNT);
    return result == null ? 0 : result;
  }

  public long getInMemoryHitCount() {
    Long result = (Long) attributes.get(IN_MEMORY_HIT_COUNT);
    return result == null ? 0 : result;
  }

  public long getOffHeapHitCount() {
    Long result = (Long) attributes.get(OFF_HEAP_HIT_COUNT);
    return result == null ? 0 : result;
  }

  public long getOnDiskHitCount() {
    Long result = (Long) attributes.get(ON_DISK_HIT_COUNT);
    return result == null ? 0 : result;
  }

  public long getCacheMissCount() {
    Long result = (Long) attributes.get(CACHE_MISS_COUNT);
    return result == null ? 0 : result;
  }

  public long getCacheMissCountExpired() {
    Long result = (Long) attributes.get(CACHE_MISS_COUNT_EXPIRED);
    return result == null ? 0 : result;
  }

  public long getInMemoryMissCount() {
    Long result = (Long) attributes.get(IN_MEMORY_MISS_COUNT);
    return result == null ? 0 : result;
  }

  public long getOffHeapMissCount() {
    Long result = (Long) attributes.get(OFF_HEAP_MISS_COUNT);
    return result == null ? 0 : result;
  }

  public long getOnDiskMissCount() {
    Long result = (Long) attributes.get(ON_DISK_MISS_COUNT);
    return result == null ? 0 : result;
  }

  public long getPutCount() {
    Long result = (Long) attributes.get(PUT_COUNT);
    return result == null ? 0 : result;
  }

  public long getUpdateCount() {
    Long result = (Long) attributes.get(UPDATE_COUNT);
    return result == null ? 0 : result;
  }

  public long getEvictedCount() {
    Long result = (Long) attributes.get(EVICTED_COUNT);
    return result == null ? 0 : result;
  }

  public long getExpiredCount() {
    Long result = (Long) attributes.get(EXPIRED_COUNT);
    return result == null ? 0 : result;
  }

  public long getRemovedCount() {
    Long result = (Long) attributes.get(REMOVED_COUNT);
    return result == null ? 0 : result;
  }

  public float getAverageGetTimeMillis() {
    Float result = (Float) attributes.get(AVERAGE_GET_TIME_MILLIS);
    return result == null ? 0 : result;
  }

  public void setAverageGetTime(float averageGetTimeMillis) {
    attributes.put(AVERAGE_GET_TIME_MILLIS, Float.valueOf(averageGetTimeMillis));
  }

  public float getMinGetTimeMillis() {
    Number result = (Number) attributes.get(MIN_GET_TIME_MILLIS);
    return result == null ? 0 : result.floatValue();
  }

  public void setMinGetTime(float minGetTimeMillis) {
    attributes.put(MIN_GET_TIME_MILLIS, Float.valueOf(minGetTimeMillis));
  }

  public float getMaxGetTimeMillis() {
    Number result = (Number) attributes.get(MAX_GET_TIME_MILLIS);
    return result == null ? 0 : result.floatValue();
  }

  public void setMaxGetTime(float maxGetTimeMillis) {
    attributes.put(MAX_GET_TIME_MILLIS, Float.valueOf(maxGetTimeMillis));
  }

  public long getLocalHeapSize() {
    Long result = (Long) attributes.get(LOCAL_HEAP_SIZE);
    return result == null ? 0 : result;
  }

  public long getLocalHeapSizeInBytes() {
    Long result = (Long) attributes.get(LOCAL_HEAP_SIZE_IN_BYTES);
    return result == null ? 0 : result;
  }

  public long getLocalOffHeapSize() {
    Long result = (Long) attributes.get(LOCAL_OFFHEAP_SIZE);
    return result == null ? 0 : result;
  }

  public long getLocalOffHeapSizeInBytes() {
    Long result = (Long) attributes.get(LOCAL_OFFHEAP_SIZE_IN_BYTES);
    return result == null ? 0 : result;
  }

  public long getLocalDiskSize() {
    Long result = (Long) attributes.get(LOCAL_DISK_SIZE);
    return result == null ? 0 : result;
  }

  public long getLocalDiskSizeInBytes() {
    Long result = (Long) attributes.get(LOCAL_DISK_SIZE_IN_BYTES);
    return result == null ? 0 : result;
  }

  public long getWriterQueueLength() {
    Number result = (Number) attributes.get(WRITER_QUEUE_LENGTH);
    return result == null ? 0 : result.longValue();
  }

  public long getXaCommitCount() {
    Number result = (Number) attributes.get(XA_COMMIT_COUNT);
    return result == null ? 0 : result.longValue();
  }

  public long getXaRollbackCount() {
    Number result = (Number) attributes.get(XA_ROLLBACK_COUNT);
    return result == null ? 0 : result.longValue();
  }
}

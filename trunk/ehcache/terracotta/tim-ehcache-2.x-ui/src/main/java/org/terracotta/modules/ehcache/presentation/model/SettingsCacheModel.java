/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation.model;

import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.Notification;
import javax.management.ObjectName;
import javax.swing.table.TableModel;

public class SettingsCacheModel extends CacheModel implements ICacheSettings {
  private static final String LOGGING_ENABLED_PROP                   = "LoggingEnabled";
  private static final String MAX_ENTRIES_LOCAL_HEAP_PROP            = "MaxEntriesLocalHeap";
  private static final String MAX_ENTRIES_LOCAL_DISK_PROP            = "MaxEntriesLocalDisk";
  private static final String MAX_BYTES_LOCAL_HEAP_PROP              = "MaxBytesLocalHeap";
  private static final String MAX_BYTES_LOCAL_HEAP_AS_STRING_PROP    = "MaxBytesLocalHeapAsString";
  private static final String MAX_BYTES_LOCAL_OFFHEAP_PROP           = "MaxBytesLocalOffHeap";
  private static final String MAX_BYTES_LOCAL_OFFHEAP_AS_STRING_PROP = "MaxBytesLocalOffHeapAsString";
  private static final String MAX_BYTES_LOCAL_DISK_PROP              = "MaxBytesLocalDisk";
  private static final String MAX_BYTES_LOCAL_DISK_AS_STRING_PROP    = "MaxBytesLocalDiskAsString";
  private static final String TIME_TO_IDLE_SECONDS_PROP              = "TimeToIdleSeconds";
  private static final String TIME_TO_LIVE_SECONDS_PROP              = "TimeToLiveSeconds";
  private static final String MEMORY_STORE_EVICTION_POLICY_PROP      = "MemoryStoreEvictionPolicy";
  private static final String DISK_PERSISTENT_PROP                   = "DiskPersistent";
  private static final String ETERNAL_PROP                           = "Eternal";
  private static final String OVERFLOW_TO_DISK_PROP                  = "OverflowToDisk";

  static final String[]       MBEAN_ATTRS                            = { "CacheName", MAX_ENTRIES_LOCAL_HEAP_PROP,
      MAX_ENTRIES_LOCAL_DISK_PROP, MAX_BYTES_LOCAL_HEAP_AS_STRING_PROP, MAX_BYTES_LOCAL_HEAP_PROP,
      MAX_BYTES_LOCAL_OFFHEAP_AS_STRING_PROP, MAX_BYTES_LOCAL_OFFHEAP_PROP, MAX_BYTES_LOCAL_DISK_AS_STRING_PROP,
      MAX_BYTES_LOCAL_DISK_PROP, MEMORY_STORE_EVICTION_POLICY_PROP, TIME_TO_IDLE_SECONDS_PROP,
      TIME_TO_LIVE_SECONDS_PROP, DISK_PERSISTENT_PROP, ETERNAL_PROP, OVERFLOW_TO_DISK_PROP, LOGGING_ENABLED_PROP };

  public static String[]      ATTRS                                  = { "ShortName", MAX_ENTRIES_LOCAL_HEAP_PROP,
      MAX_ENTRIES_LOCAL_DISK_PROP, MAX_BYTES_LOCAL_HEAP_AS_STRING_PROP, MAX_BYTES_LOCAL_HEAP_PROP,
      MAX_BYTES_LOCAL_OFFHEAP_AS_STRING_PROP, MAX_BYTES_LOCAL_OFFHEAP_PROP, MAX_BYTES_LOCAL_DISK_AS_STRING_PROP,
      MAX_BYTES_LOCAL_DISK_PROP, MEMORY_STORE_EVICTION_POLICY_PROP, TIME_TO_IDLE_SECONDS_PROP,
      TIME_TO_LIVE_SECONDS_PROP                                     };

  public static String[]      HEADERS                                = { "Name", "Max Local Heap Entries",
      "Max Local Disk Entries", "Max Local Heap Bytes", "Max Local Disk Bytes", "Max Local OffHeap Bytes",
      "Eviction Policy", "Time-To-Idle", "Time-To-Live"             };

  private boolean             loggingEnabled;
  private long                maxEntriesLocalHeap;
  private long                maxEntriesLocalDisk;
  private String              maxBytesLocalHeapAsString;
  private String              maxBytesLocalOffHeapAsString;
  private String              maxBytesLocalDiskAsString;
  private long                maxBytesLocalHeap;
  private long                maxBytesLocalOffHeap;
  private long                maxBytesLocalDisk;
  private long                timeToIdleSeconds;
  private long                timeToLiveSeconds;
  private String              memoryStoreEvictionPolicy;
  private boolean             diskPersistent;
  private boolean             eternal;
  private boolean             overflowToDisk;

  public SettingsCacheModel(CacheManagerModel cacheManagerModel, String cacheName) {
    super(cacheManagerModel, cacheName);
  }

  public void setAttributes(Map<String, Object> attrs) {
    synchronized (this) {
      this.timeToLiveSeconds = longAttr(attrs, TIME_TO_LIVE_SECONDS_PROP);
      this.timeToIdleSeconds = longAttr(attrs, TIME_TO_IDLE_SECONDS_PROP);
      this.maxEntriesLocalHeap = longAttr(attrs, MAX_ENTRIES_LOCAL_HEAP_PROP);
      this.maxEntriesLocalDisk = longAttr(attrs, MAX_ENTRIES_LOCAL_DISK_PROP);
      this.maxBytesLocalHeapAsString = stringAttr(attrs, MAX_BYTES_LOCAL_HEAP_AS_STRING_PROP);
      this.maxBytesLocalOffHeapAsString = stringAttr(attrs, MAX_BYTES_LOCAL_OFFHEAP_AS_STRING_PROP);
      this.maxBytesLocalDiskAsString = stringAttr(attrs, MAX_BYTES_LOCAL_DISK_AS_STRING_PROP);
      this.maxBytesLocalHeap = longAttr(attrs, MAX_BYTES_LOCAL_HEAP_PROP);
      this.maxBytesLocalOffHeap = longAttr(attrs, MAX_BYTES_LOCAL_OFFHEAP_PROP);
      this.maxBytesLocalDisk = longAttr(attrs, MAX_BYTES_LOCAL_DISK_PROP);
      this.memoryStoreEvictionPolicy = stringAttr(attrs, MEMORY_STORE_EVICTION_POLICY_PROP);
      this.diskPersistent = booleanAttr(attrs, DISK_PERSISTENT_PROP);
      this.eternal = booleanAttr(attrs, ETERNAL_PROP);
      this.overflowToDisk = booleanAttr(attrs, OVERFLOW_TO_DISK_PROP);
      this.loggingEnabled = booleanAttr(attrs, LOGGING_ENABLED_PROP);
    }

    firePropertyChange(null, null, null);
  }

  @Override
  public Set<CacheModelInstance> cacheModelInstances() {
    return Collections.emptySet();
  }

  public void setLoggingEnabled(boolean enabled) {
    boolean oldLoggingEnabled = isLoggingEnabled();
    if (oldLoggingEnabled != enabled) {
      synchronized (this) {
        this.loggingEnabled = enabled;
      }
      _setAttribute(LOGGING_ENABLED_PROP, Boolean.valueOf(enabled));
      firePropertyChange(LOGGING_ENABLED_PROP, oldLoggingEnabled, enabled);
    }
  }

  public synchronized boolean isLoggingEnabled() {
    return loggingEnabled;
  }

  public synchronized long getMaxEntriesLocalHeap() {
    return maxEntriesLocalHeap;
  }

  public void setMaxEntriesLocalHeap(long maxEntries) {
    long oldMaxEntries = getMaxEntriesLocalHeap();
    if (oldMaxEntries != maxEntries) {
      synchronized (this) {
        this.maxEntriesLocalHeap = maxEntries;
      }
      _setAttribute(MAX_ENTRIES_LOCAL_HEAP_PROP, Long.valueOf(maxEntries));
      firePropertyChange(MAX_ENTRIES_LOCAL_HEAP_PROP, oldMaxEntries, maxEntries);
    }
  }

  public synchronized long getMaxEntriesLocalDisk() {
    return maxEntriesLocalDisk;
  }

  public void setMaxEntriesLocalDisk(long maxEntries) {
    long oldMaxEntries = getMaxEntriesLocalDisk();
    if (oldMaxEntries != maxEntries) {
      synchronized (this) {
        this.maxEntriesLocalDisk = maxEntries;
      }
      _setAttribute(MAX_ENTRIES_LOCAL_DISK_PROP, Long.valueOf(maxEntries));
      firePropertyChange(MAX_ENTRIES_LOCAL_DISK_PROP, oldMaxEntries, maxEntries);
    }
  }

  public synchronized String getMaxBytesLocalHeapAsString() {
    return maxBytesLocalHeapAsString;
  }

  public void setMaxBytesLocalHeapAsString(String maxBytes) {
    String oldMaxBytes = getMaxBytesLocalHeapAsString();
    if (!StringUtils.equals(oldMaxBytes, maxBytes)) {
      synchronized (this) {
        this.maxBytesLocalHeapAsString = maxBytes;
      }
      _setAttribute(MAX_BYTES_LOCAL_HEAP_AS_STRING_PROP, maxBytes);
      firePropertyChange(MAX_BYTES_LOCAL_HEAP_AS_STRING_PROP, oldMaxBytes, maxBytes);
    }
  }

  public synchronized String getMaxBytesLocalOffHeapAsString() {
    return maxBytesLocalOffHeapAsString;
  }

  public synchronized long getMaxBytesLocalOffHeap() {
    return maxBytesLocalOffHeap;
  }

  public synchronized long getMaxBytesLocalHeap() {
    return maxBytesLocalHeap;
  }

  public void setMaxBytesLocalHeap(long maxBytes) {
    long oldMaxBytes = getMaxBytesLocalHeap();
    if (oldMaxBytes != maxBytes) {
      synchronized (this) {
        this.maxBytesLocalHeap = maxBytes;
      }
      _setAttribute(MAX_BYTES_LOCAL_HEAP_PROP, maxBytes);
      firePropertyChange(MAX_BYTES_LOCAL_HEAP_PROP, oldMaxBytes, maxBytes);
    }
  }

  public synchronized String getMaxBytesLocalDiskAsString() {
    return maxBytesLocalDiskAsString;
  }

  public void setMaxBytesLocalDiskAsString(String maxBytes) {
    String oldMaxBytes = getMaxBytesLocalDiskAsString();
    if (!StringUtils.equals(oldMaxBytes, maxBytes)) {
      synchronized (this) {
        this.maxBytesLocalDiskAsString = maxBytes;
      }
      _setAttribute(MAX_BYTES_LOCAL_DISK_AS_STRING_PROP, maxBytes);
      firePropertyChange(MAX_BYTES_LOCAL_DISK_AS_STRING_PROP, oldMaxBytes, maxBytes);
    }
  }

  public synchronized long getMaxBytesLocalDisk() {
    return maxBytesLocalDisk;
  }

  public void setMaxBytesLocalDisk(long maxBytes) {
    long oldMaxBytes = getMaxBytesLocalDisk();
    if (oldMaxBytes != maxBytes) {
      synchronized (this) {
        this.maxBytesLocalDisk = maxBytes;
      }
      _setAttribute(MAX_BYTES_LOCAL_DISK_PROP, maxBytes);
      firePropertyChange(MAX_BYTES_LOCAL_DISK_PROP, oldMaxBytes, maxBytes);
    }
  }

  public synchronized long getTimeToIdleSeconds() {
    return timeToIdleSeconds;
  }

  public void setTimeToIdleSeconds(long tti) {
    long oldTTI = getTimeToIdleSeconds();
    if (oldTTI != tti) {
      synchronized (this) {
        this.timeToIdleSeconds = tti;
      }
      _setAttribute(TIME_TO_IDLE_SECONDS_PROP, Long.valueOf(tti));
      firePropertyChange(TIME_TO_IDLE_SECONDS_PROP, oldTTI, tti);
    }
  }

  public synchronized long getTimeToLiveSeconds() {
    return timeToLiveSeconds;
  }

  public void setTimeToLiveSeconds(long ttl) {
    long oldTTL = getTimeToLiveSeconds();
    if (oldTTL != ttl) {
      synchronized (this) {
        this.timeToLiveSeconds = ttl;
      }
      _setAttribute(TIME_TO_LIVE_SECONDS_PROP, Long.valueOf(ttl));
      firePropertyChange(TIME_TO_LIVE_SECONDS_PROP, oldTTL, ttl);
    }
  }

  public synchronized String getMemoryStoreEvictionPolicy() {
    return memoryStoreEvictionPolicy;
  }

  public void setMemoryStoreEvictionPolicy(String evictionPolicy) {
    String oldEvictionPolicy = getMemoryStoreEvictionPolicy();
    if (!StringUtils.equals(oldEvictionPolicy, evictionPolicy)) {
      synchronized (this) {
        this.memoryStoreEvictionPolicy = evictionPolicy;
      }
      _setAttribute(MEMORY_STORE_EVICTION_POLICY_PROP, evictionPolicy);
      firePropertyChange(MEMORY_STORE_EVICTION_POLICY_PROP, oldEvictionPolicy, evictionPolicy);
    }
  }

  public void setDiskPersistent(boolean diskPersistent) {
    boolean oldDiskPersistent = isDiskPersistent();
    if (oldDiskPersistent != diskPersistent) {
      synchronized (this) {
        this.diskPersistent = diskPersistent;
      }
      _setAttribute(DISK_PERSISTENT_PROP, Boolean.valueOf(diskPersistent));
      firePropertyChange(DISK_PERSISTENT_PROP, oldDiskPersistent, diskPersistent);
    }
  }

  public synchronized boolean isDiskPersistent() {
    return diskPersistent;
  }

  public void setEternal(boolean eternal) {
    boolean oldEternal = isEternal();
    if (oldEternal != eternal) {
      synchronized (this) {
        this.eternal = eternal;
      }
      _setAttribute(ETERNAL_PROP, Boolean.valueOf(eternal));
      firePropertyChange(ETERNAL_PROP, oldEternal, eternal);
    }
  }

  public synchronized boolean isEternal() {
    return eternal;
  }

  public void setOverflowToDisk(boolean overflowToDisk) {
    boolean oldOverflowToDisk = isOverflowToDisk();
    if (oldOverflowToDisk != overflowToDisk) {
      synchronized (this) {
        this.overflowToDisk = overflowToDisk;
      }
      _setAttribute(OVERFLOW_TO_DISK_PROP, Boolean.valueOf(overflowToDisk));
      firePropertyChange(OVERFLOW_TO_DISK_PROP, oldOverflowToDisk, overflowToDisk);
    }
  }

  public synchronized boolean isOverflowToDisk() {
    return overflowToDisk;
  }

  public boolean hasSizeBasedLimits() {
    return (getMaxBytesLocalHeap() + getMaxBytesLocalOffHeap() + getMaxBytesLocalDisk()) > 0;
  }

  @Override
  public void init() {
    addListeners();

    onSet.addAll(getActiveCacheModelBeans());
    ObjectName target = getRandomBean();
    if (target != null) {
      Map<String, Object> result = getAttributes(target, new HashSet(Arrays.asList(MBEAN_ATTRS)));
      setAttributes(result);
      for (ObjectName objectName : onSet) {
        addNotificationListener(objectName, this);
      }
    }
  }

  @Override
  protected Set<ObjectName> getActiveCacheModelBeans() {
    return new HashSet<ObjectName>();
  }

  @Override
  public void suspend() {
    /**/
  }

  @Override
  public void handleNotification(Notification notif, Object data) {
    String type = notif.getType();

    if ("CacheChanged".equals(type) || "CacheEnabled".equals(type)) {
      Map<String, Object> attrs = (Map<String, Object>) notif.getUserData();

      synchronized (this) {
        this.loggingEnabled = booleanAttr(attrs, LOGGING_ENABLED_PROP);
        this.timeToLiveSeconds = longAttr(attrs, TIME_TO_LIVE_SECONDS_PROP);
        this.timeToIdleSeconds = longAttr(attrs, TIME_TO_IDLE_SECONDS_PROP);
        this.maxEntriesLocalHeap = longAttr(attrs, MAX_ENTRIES_LOCAL_HEAP_PROP);
        this.maxEntriesLocalDisk = longAttr(attrs, MAX_ENTRIES_LOCAL_DISK_PROP);
        this.maxBytesLocalHeapAsString = stringAttr(attrs, MAX_BYTES_LOCAL_HEAP_AS_STRING_PROP);
        this.maxBytesLocalOffHeapAsString = stringAttr(attrs, MAX_BYTES_LOCAL_OFFHEAP_AS_STRING_PROP);
        this.maxBytesLocalDiskAsString = stringAttr(attrs, MAX_BYTES_LOCAL_DISK_AS_STRING_PROP);
        this.maxBytesLocalHeap = longAttr(attrs, MAX_BYTES_LOCAL_HEAP_PROP);
        this.maxBytesLocalOffHeap = longAttr(attrs, MAX_BYTES_LOCAL_OFFHEAP_PROP);
        this.maxBytesLocalDisk = longAttr(attrs, MAX_BYTES_LOCAL_DISK_PROP);
        this.memoryStoreEvictionPolicy = stringAttr(attrs, MEMORY_STORE_EVICTION_POLICY_PROP);
        this.diskPersistent = booleanAttr(attrs, DISK_PERSISTENT_PROP);
        this.eternal = booleanAttr(attrs, ETERNAL_PROP);
        this.overflowToDisk = booleanAttr(attrs, OVERFLOW_TO_DISK_PROP);
      }

      firePropertyChange(null, null, null);

      cacheModelChanged();
    }
  }

  protected void cacheModelChanged() {
    cacheManagerModel.cacheModelChanged(this);
  }

  @Override
  public int getStatisticsEnabledCount() {
    return cacheManagerModel.getStatisticsEnabledCount(this);
  }

  @Override
  public int getBulkLoadEnabledCount() {
    return cacheManagerModel.getBulkLoadEnabledCount(this);
  }

  @Override
  public int getEnabledCount() {
    return cacheManagerModel.getEnabledCount(this);
  }

  @Override
  public String generateActiveConfigDeclaration() {
    return cacheManagerModel.generateActiveConfigDeclaration(getCacheName());
  }

  @Override
  public int compareTo(Object o) {
    SettingsCacheModel other = (SettingsCacheModel) o;
    return getCacheName().compareTo(other.getCacheName());
  }

  public IMutableCacheSettings getCacheSettings() {
    return new MutableCacheSettings(this);
  }

  public void apply(IMutableCacheSettings settings) {
    setMaxEntriesLocalHeap(settings.getMaxEntriesLocalHeap());
    setMaxEntriesLocalDisk(settings.getMaxEntriesLocalDisk());
    setMaxBytesLocalHeapAsString(settings.getMaxBytesLocalHeapAsString());
    setMaxBytesLocalDiskAsString(settings.getMaxBytesLocalDiskAsString());
    setTimeToIdleSeconds(settings.getTimeToIdleSeconds());
    setTimeToLiveSeconds(settings.getTimeToLiveSeconds());
  }

  public TableModel executeQuery(String query) throws Exception {
    Object result = invokeOnce("executeQuery", new Object[] { query }, new String[] { "java.lang.String" });
    if (result instanceof Exception) { throw (Exception) result; }
    return (TableModel) result;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + (isDiskPersistent() ? 1231 : 1237);
    result = prime * result + (isEternal() ? 1231 : 1237);
    result = prime * result + (isLoggingEnabled() ? 1231 : 1237);
    result = prime * result + (int) getMaxEntriesLocalHeap();
    result = prime * result + (int) getMaxEntriesLocalHeap();
    result = prime * result + (int) getMaxEntriesLocalDisk();
    result = prime * result
             + ((getMemoryStoreEvictionPolicy() == null) ? 0 : getMemoryStoreEvictionPolicy().hashCode());
    result = prime * result + (isOverflowToDisk() ? 1231 : 1237);
    result = prime * result + (int) (getTimeToIdleSeconds() ^ (getTimeToIdleSeconds() >>> 32));
    result = prime * result + (int) (getTimeToLiveSeconds() ^ (getTimeToLiveSeconds() >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) { return true; }
    if (!super.equals(obj)) { return false; }
    if (!(obj instanceof SettingsCacheModel)) { return false; }
    SettingsCacheModel other = (SettingsCacheModel) obj;
    if (isDiskPersistent() != other.isDiskPersistent()) { return false; }
    if (isEternal() != other.isEternal()) { return false; }
    if (isLoggingEnabled() != other.isLoggingEnabled()) { return false; }
    if (getMaxEntriesLocalHeap() != other.getMaxEntriesLocalHeap()) { return false; }
    if (getMaxEntriesLocalDisk() != other.getMaxEntriesLocalDisk()) { return false; }
    if (getMemoryStoreEvictionPolicy() == null) {
      if (other.getMemoryStoreEvictionPolicy() != null) { return false; }
    } else if (!getMemoryStoreEvictionPolicy().equals(other.getMemoryStoreEvictionPolicy())) { return false; }
    if (isOverflowToDisk() != other.isOverflowToDisk()) { return false; }
    if (getTimeToIdleSeconds() != other.getTimeToIdleSeconds()) { return false; }
    if (getTimeToLiveSeconds() != other.getTimeToLiveSeconds()) { return false; }
    return true;
  }
}

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation.model;

import org.terracotta.modules.ehcache.presentation.EhcachePresentationUtils;

import com.tc.admin.model.IClient;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

public class CacheModelInstance implements NotificationListener, Comparable {
  private final CacheManagerInstance cacheManagerInstance;
  private final String               cacheName;
  private final String               shortName;
  private final ObjectName           beanName;

  private boolean                    enabled;
  private boolean                    bulkLoadEnabled;
  private boolean                    transactional;
  private String                     consistency;
  private boolean                    statisticsEnabled;
  private boolean                    terracottaClustered;
  private String                     pinnedToStore;
  private boolean                    loggingEnabled;
  private long                       maxEntriesLocalHeap;
  private long                       maxEntriesLocalDisk;
  private String                     maxBytesLocalHeapAsString;
  private String                     maxBytesLocalOffHeapAsString;
  private String                     maxBytesLocalDiskAsString;
  private long                       maxBytesLocalHeap;
  private long                       maxBytesLocalOffHeap;
  private long                       maxBytesLocalDisk;
  private long                       timeToIdleSeconds;
  private long                       timeToLiveSeconds;
  private String                     memoryStoreEvictionPolicy;
  private boolean                    diskPersistent;
  private boolean                    eternal;
  private boolean                    overflowToDisk;

  public static final String         SHORT_NAME_PROP                        = "ShortName";
  public static final String         CLIENT_NAME_PROP                       = "ClientName";
  public static final String         ENABLED_PROP                           = "Enabled";
  public static final String         BULK_LOAD_ENABLED_PROP                 = "NodeBulkLoadEnabled";
  public static final String         BULK_LOAD_ENABLED_DESC_PROP            = "BulkLoadEnabledDesc";
  public static final String         TRANSACTIONAL_PROP                     = "Transactional";
  public static final String         TERRACOTTA_CONSISTENCY_PROP            = "TerracottaConsistency";
  public static final String         TERRACOTTA_CLUSTERED_PROP              = "TerracottaClustered";
  public static final String         STATISTICS_ENABLED_PROP                = "StatisticsEnabled";
  public static final String         PINNED_TO_STORE_PROP                   = "PinnedToStore";
  public static final String         CONSISTENCY_PROP                       = "Consistency";
  public static final String         LOGGING_ENABLED_PROP                   = "LoggingEnabled";
  public static final String         MAX_ENTRIES_LOCAL_HEAP_PROP            = "MaxEntriesLocalHeap";
  public static final String         MAX_ENTRIES_LOCAL_DISK_PROP            = "MaxEntriesLocalDisk";
  public static final String         MAX_BYTES_LOCAL_HEAP_PROP              = "MaxBytesLocalHeap";
  public static final String         MAX_BYTES_LOCAL_HEAP_AS_STRING_PROP    = "MaxBytesLocalHeapAsString";
  public static final String         MAX_BYTES_LOCAL_OFFHEAP_PROP           = "MaxBytesLocalOffHeap";
  public static final String         MAX_BYTES_LOCAL_OFFHEAP_AS_STRING_PROP = "MaxBytesLocalOffHeapAsString";
  public static final String         MAX_BYTES_LOCAL_DISK_PROP              = "MaxBytesLocalDisk";
  public static final String         MAX_BYTES_LOCAL_DISK_AS_STRING_PROP    = "MaxBytesLocalDiskAsString";
  public static final String         TIME_TO_IDLE_SECONDS_PROP              = "TimeToIdleSeconds";
  public static final String         TIME_TO_LIVE_SECONDS_PROP              = "TimeToLiveSeconds";
  public static final String         MEMORY_STORE_EVICTION_POLICY_PROP      = "MemoryStoreEvictionPolicy";
  public static final String         DISK_PERSISTENT_PROP                   = "DiskPersistent";
  public static final String         ETERNAL_PROP                           = "Eternal";
  public static final String         OVERFLOW_TO_DISK_PROP                  = "OverflowToDisk";

  public static final String[]       MBEAN_ATTRS                            = { ENABLED_PROP,
      TERRACOTTA_CLUSTERED_PROP, TERRACOTTA_CONSISTENCY_PROP, BULK_LOAD_ENABLED_PROP, TRANSACTIONAL_PROP,
      STATISTICS_ENABLED_PROP, PINNED_TO_STORE_PROP, MAX_ENTRIES_LOCAL_HEAP_PROP, MAX_ENTRIES_LOCAL_DISK_PROP,
      MAX_BYTES_LOCAL_HEAP_AS_STRING_PROP, MAX_BYTES_LOCAL_HEAP_PROP, MAX_BYTES_LOCAL_OFFHEAP_AS_STRING_PROP,
      MAX_BYTES_LOCAL_OFFHEAP_PROP, MAX_BYTES_LOCAL_DISK_AS_STRING_PROP, MAX_BYTES_LOCAL_DISK_PROP,
      MEMORY_STORE_EVICTION_POLICY_PROP, TIME_TO_IDLE_SECONDS_PROP, TIME_TO_LIVE_SECONDS_PROP, DISK_PERSISTENT_PROP,
      ETERNAL_PROP, OVERFLOW_TO_DISK_PROP, LOGGING_ENABLED_PROP            };

  public static final Set<String>    MBEAN_ATTR_SET                         = new HashSet(Arrays.asList(MBEAN_ATTRS));

  public CacheModelInstance(CacheManagerInstance cacheManagerInstance, String cacheName, ObjectName beanName) {
    this.cacheManagerInstance = cacheManagerInstance;
    this.cacheName = cacheName;
    this.beanName = beanName;
    this.shortName = EhcachePresentationUtils.determineShortName(cacheName);

    cacheManagerInstance.addNotificationListener(beanName, this);
    setAttributes(cacheManagerInstance.getAttributes(beanName, MBEAN_ATTR_SET));

    CacheManagerModel cacheManagerModel = cacheManagerInstance.getCacheManagerModel();
    Boolean enabledPersistently = cacheManagerModel.isCachesEnabledPersistently();
    if (enabledPersistently != null) {
      setEnabled(enabledPersistently.booleanValue());
    }
    Boolean statsEnabledPersistently = cacheManagerModel.isStatisticsEnabledPersistently();
    if (statsEnabledPersistently != null) {
      setStatisticsEnabled(statsEnabledPersistently.booleanValue());
    }
    Boolean bulkLoadEnabledPersistently = cacheManagerModel.isBulkLoadEnabledPersistently();
    if (bulkLoadEnabledPersistently != null) {
      setBulkLoadEnabled(bulkLoadEnabledPersistently.booleanValue());
    }
  }

  public ObjectName getBeanName() {
    return beanName;
  }

  public IClient getClient() {
    return cacheManagerInstance.getClient();
  }

  public String getClientName() {
    return cacheManagerInstance.getClientName();
  }

  public void setAttributes(Map<String, Object> attrs) {
    synchronized (this) {
      this.enabled = booleanAttr(attrs, ENABLED_PROP);
      this.transactional = booleanAttr(attrs, TRANSACTIONAL_PROP);
      this.bulkLoadEnabled = booleanAttr(attrs, BULK_LOAD_ENABLED_PROP);
      this.consistency = stringAttr(attrs, TERRACOTTA_CONSISTENCY_PROP);
      this.terracottaClustered = booleanAttr(attrs, TERRACOTTA_CLUSTERED_PROP);
      this.statisticsEnabled = booleanAttr(attrs, STATISTICS_ENABLED_PROP);
      this.pinnedToStore = stringAttr(attrs, PINNED_TO_STORE_PROP);
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

  private static Boolean booleanAttr(Map<String, Object> attrs, String name) {
    Boolean result = null;
    try {
      result = (Boolean) attrs.get(name);
    } catch (Exception e) {
      /**/
    } finally {
      if (result == null) {
        result = Boolean.FALSE;
      }
    }
    return result;
  }

  private static String stringAttr(Map<String, Object> attrs, String name) {
    String result = null;
    try {
      Object val = attrs.get(name);
      if (val != null) {
        result = val.toString();
      }
    } catch (Exception e) {
      /**/
    } finally {
      if (result == null) {
        result = "";
      }
    }
    return result;
  }

  protected static Long longAttr(Map<String, Object> attrs, String name) {
    Long result = null;
    try {
      result = (Long) attrs.get(name);
    } catch (Exception e) {
      /**/
    } finally {
      if (result == null) {
        result = Long.valueOf(0);
      }
    }
    return result;
  }

  public CacheManagerInstance getCacheManagerInstance() {
    return cacheManagerInstance;
  }

  public CacheModel getCacheModel() {
    return getCacheManagerInstance().getCacheManagerModel().getCacheModel(getCacheName());
  }

  public String getCacheName() {
    return cacheName;
  }

  public String getShortName() {
    return shortName;
  }

  public synchronized boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    boolean oldEnabled = isEnabled();
    synchronized (this) {
      this.enabled = enabled;
      if (oldEnabled != enabled) {
        cacheManagerInstance.safeSetAttribute(beanName, ENABLED_PROP, Boolean.valueOf(enabled));
      }
    }
    firePropertyChange(ENABLED_PROP, oldEnabled, enabled);
  }

  public synchronized boolean isTransactional() {
    return transactional;
  }

  public synchronized boolean isBulkLoadEnabled() {
    return bulkLoadEnabled;
  }

  public Boolean isBulkLoadEnabledDesc() {
    return isTerracottaClustered() ? Boolean.valueOf(bulkLoadEnabled) : null;
  }

  public boolean isBulkLoadDisabled() {
    return !isBulkLoadEnabled();
  }

  public void setBulkLoadEnabled(boolean bulkLoadEnabled) {
    boolean oldIsBulkLoadEnabled = isBulkLoadEnabled();
    synchronized (this) {
      this.bulkLoadEnabled = bulkLoadEnabled;
      if (oldIsBulkLoadEnabled != bulkLoadEnabled) {
        setAttribute(BULK_LOAD_ENABLED_PROP, Boolean.valueOf(bulkLoadEnabled));
      }
    }
    firePropertyChange(BULK_LOAD_ENABLED_PROP, oldIsBulkLoadEnabled, bulkLoadEnabled);
  }

  public synchronized String getConsistency() {
    return isTerracottaClustered() ? consistency : "na";
  }

  public synchronized boolean isTerracottaClustered() {
    return terracottaClustered;
  }

  public synchronized boolean isStatisticsEnabled() {
    return statisticsEnabled;
  }

  public void setStatisticsEnabled(boolean enabled) {
    boolean oldEnabled = isStatisticsEnabled();
    synchronized (this) {
      this.statisticsEnabled = enabled;
      if (oldEnabled != enabled) {
        setAttribute(STATISTICS_ENABLED_PROP, Boolean.valueOf(enabled));
      }
    }
    firePropertyChange(STATISTICS_ENABLED_PROP, oldEnabled, enabled);
  }

  public boolean isPinned() {
    return !getPinnedToStore().equals("na");
  }

  public synchronized String getPinnedToStore() {
    return pinnedToStore;
  }

  public void setLoggingEnabled(boolean enabled) {
    boolean oldLoggingEnabled = isLoggingEnabled();
    if (oldLoggingEnabled != enabled) {
      synchronized (this) {
        this.loggingEnabled = enabled;
      }
      setAttribute(LOGGING_ENABLED_PROP, Boolean.valueOf(enabled));
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
      setAttribute(MAX_ENTRIES_LOCAL_HEAP_PROP, Long.valueOf(maxEntries));
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
      setAttribute(MAX_ENTRIES_LOCAL_DISK_PROP, Long.valueOf(maxEntries));
      firePropertyChange(MAX_ENTRIES_LOCAL_DISK_PROP, oldMaxEntries, maxEntries);
    }
  }

  public synchronized String getMaxBytesLocalHeapAsString() {
    return maxBytesLocalHeapAsString;
  }

  public void setMaxBytesLocalHeapAsString(String maxBytes) {
    String oldMaxBytes = getMaxBytesLocalHeapAsString();
    if (!oldMaxBytes.equals(maxBytes)) {
      synchronized (this) {
        this.maxBytesLocalHeapAsString = maxBytes;
      }
      setAttribute(MAX_BYTES_LOCAL_HEAP_AS_STRING_PROP, maxBytes);
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
      setAttribute(MAX_BYTES_LOCAL_HEAP_PROP, maxBytes);
      firePropertyChange(MAX_BYTES_LOCAL_HEAP_PROP, oldMaxBytes, maxBytes);
    }
  }

  public synchronized String getMaxBytesLocalDiskAsString() {
    return maxBytesLocalDiskAsString;
  }

  public void setMaxBytesLocalDiskAsString(String maxBytes) {
    String oldMaxBytes = getMaxBytesLocalDiskAsString();
    if (!oldMaxBytes.equals(maxBytes)) {
      synchronized (this) {
        this.maxBytesLocalDiskAsString = maxBytes;
      }
      setAttribute(MAX_BYTES_LOCAL_DISK_AS_STRING_PROP, maxBytes);
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
      setAttribute(MAX_BYTES_LOCAL_DISK_PROP, maxBytes);
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
      setAttribute(TIME_TO_IDLE_SECONDS_PROP, Long.valueOf(tti));
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
      setAttribute(TIME_TO_LIVE_SECONDS_PROP, Long.valueOf(ttl));
      firePropertyChange(TIME_TO_LIVE_SECONDS_PROP, oldTTL, ttl);
    }
  }

  public synchronized String getMemoryStoreEvictionPolicy() {
    return memoryStoreEvictionPolicy;
  }

  public void setMemoryStoreEvictionPolicy(String evictionPolicy) {
    String oldEvictionPolicy = getMemoryStoreEvictionPolicy();
    if (!oldEvictionPolicy.equals(evictionPolicy)) {
      synchronized (this) {
        this.memoryStoreEvictionPolicy = evictionPolicy;
      }
      setAttribute(MEMORY_STORE_EVICTION_POLICY_PROP, evictionPolicy);
      firePropertyChange(MEMORY_STORE_EVICTION_POLICY_PROP, oldEvictionPolicy, evictionPolicy);
    }
  }

  public void setDiskPersistent(boolean diskPersistent) {
    boolean oldDiskPersistent = isDiskPersistent();
    if (oldDiskPersistent != diskPersistent) {
      synchronized (this) {
        this.diskPersistent = diskPersistent;
      }
      setAttribute(DISK_PERSISTENT_PROP, Boolean.valueOf(diskPersistent));
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
      setAttribute(ETERNAL_PROP, Boolean.valueOf(eternal));
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
      setAttribute(OVERFLOW_TO_DISK_PROP, Boolean.valueOf(overflowToDisk));
      firePropertyChange(OVERFLOW_TO_DISK_PROP, oldOverflowToDisk, overflowToDisk);
    }
  }

  public synchronized boolean isOverflowToDisk() {
    return overflowToDisk;
  }

  public boolean hasSizeBasedLimits() {
    return (getMaxBytesLocalHeap() + getMaxBytesLocalOffHeap() + getMaxBytesLocalDisk()) > 0;
  }

  private void setAttribute(String attribute, Object value) {
    cacheManagerInstance.safeSetAttribute(beanName, attribute, value);
  }

  public CacheStatisticsModel getCacheStatistics() {
    return getCacheStatistics(CacheStatisticsModel.MBEAN_ATTRS);
  }

  public CacheStatisticsModel getCacheStatistics(String[] attributes) {
    CacheStatisticsModel result = null;
    Map<String, Object> attrMap = cacheManagerInstance.getAttributes(beanName, new HashSet(Arrays.asList(attributes)));
    if (attrMap != null && !attrMap.isEmpty()) {
      result = new CacheStatisticsModel(attrMap);
    }
    return result;
  }

  public void removeAll() {
    cacheManagerInstance.invokeOnce(beanName, "removeAll");
  }

  public String generateActiveConfigDeclaration() {
    Object result = cacheManagerInstance.generateActiveConfigDeclaration(getCacheName());
    return result != null ? result.toString() : "";
  }

  public int getSize() {
    Object o = cacheManagerInstance.getAttribute(getBeanName(), "Size");
    if (o instanceof Number) { return ((Number) o).intValue(); }
    return 0;
  }

  public void handleNotification(Notification notif, Object data) {
    if ("CacheChanged".equals(notif.getType())) {
      setAttributes((Map<String, Object>) notif.getUserData());
      cacheManagerInstance.cacheInstanceChanged(this);
    }
  }

  @Override
  public String toString() {
    return getCacheName() + " on node " + cacheManagerInstance.getClientName();

  }

  // PropertyChangeSupport

  protected PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

  public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
    if (listener != null && propertyChangeSupport != null) {
      propertyChangeSupport.removePropertyChangeListener(listener);
      propertyChangeSupport.addPropertyChangeListener(listener);
    }
  }

  public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
    if (listener != null && propertyChangeSupport != null) {
      propertyChangeSupport.removePropertyChangeListener(listener);
    }
  }

  public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
    PropertyChangeSupport pcs;
    synchronized (this) {
      pcs = propertyChangeSupport;
    }
    if (pcs != null) {
      pcs.firePropertyChange(propertyName, oldValue, newValue);
    }
  }

  public int compareTo(Object o) {
    CacheModelInstance other = (CacheModelInstance) o;
    return cacheName.compareTo(other.getCacheName());
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((beanName == null) ? 0 : beanName.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    CacheModelInstance other = (CacheModelInstance) obj;
    if (beanName == null) {
      if (other.beanName != null) return false;
    } else if (!beanName.equals(other.beanName)) return false;
    return true;
  }

  public void tearDown() {
    cacheManagerInstance.removeNotificationListener(beanName, this);
  }
}

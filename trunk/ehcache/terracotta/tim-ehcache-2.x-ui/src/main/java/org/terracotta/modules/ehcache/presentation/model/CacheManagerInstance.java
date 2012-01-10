/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation.model;

import org.terracotta.modules.ehcache.presentation.EhcacheStatsUtils;

import com.tc.admin.model.IClient;
import com.tc.admin.model.IServer;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.management.MBeanServerNotification;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.ObjectName;

public class CacheManagerInstance extends BaseMBeanModel implements PropertyChangeListener, Comparable {
  private final CacheManagerModel                             cacheManagerModel;
  private final IClient                                       client;
  private final ObjectName                                    beanName;
  private String[]                                            cacheNames;
  private final ConcurrentMap<ObjectName, CacheModelInstance> cacheModelInstanceMap;
  private Long                                                maxBytesLocalHeap;
  private String                                              maxBytesLocalHeapAsString;
  private Long                                                maxBytesLocalOffHeap;
  private String                                              maxBytesLocalOffHeapAsString;
  private Long                                                maxBytesLocalDisk;
  private String                                              maxBytesLocalDiskAsString;

  public static final String                                  CACHE_NAMES_ATTR                       = "CacheNames";
  public static final String                                  MAX_BYTES_LOCAL_HEAP_ATTR              = "MaxBytesLocalHeap";
  public static final String                                  MAX_BYTES_LOCAL_HEAP_AS_STRING_ATTR    = "MaxBytesLocalHeapAsString";
  public static final String                                  MAX_BYTES_LOCAL_OFFHEAP_ATTR           = "MaxBytesLocalOffHeap";
  public static final String                                  MAX_BYTES_LOCAL_OFFHEAP_AS_STRING_ATTR = "MaxBytesLocalOffHeapAsString";
  public static final String                                  MAX_BYTES_LOCAL_DISK_ATTR              = "MaxBytesLocalDisk";
  public static final String                                  MAX_BYTES_LOCAL_DISK_AS_STRING_ATTR    = "MaxBytesLocalDiskAsString";

  public static final String[]                                ATTRIBUTE_ARRAY                        = {
      CACHE_NAMES_ATTR, MAX_BYTES_LOCAL_HEAP_ATTR, MAX_BYTES_LOCAL_HEAP_AS_STRING_ATTR, MAX_BYTES_LOCAL_OFFHEAP_ATTR,
      MAX_BYTES_LOCAL_OFFHEAP_AS_STRING_ATTR, MAX_BYTES_LOCAL_DISK_ATTR, MAX_BYTES_LOCAL_DISK_AS_STRING_ATTR };
  public static final Set<String>                             ATTRIBUTE_SET                          = new HashSet<String>(
                                                                                                                           Arrays
                                                                                                                               .asList(ATTRIBUTE_ARRAY));

  public CacheManagerInstance(CacheManagerModel cacheManagerModel, IClient client, ObjectName beanName) {
    super(cacheManagerModel.getClusterModel());

    this.cacheManagerModel = cacheManagerModel;
    this.client = client;
    this.beanName = beanName;
    this.cacheModelInstanceMap = new ConcurrentHashMap<ObjectName, CacheModelInstance>();
    //
    // setAttributes(getAttributes(beanName, ATTRIBUTE_SET));
    // addNotificationListener(beanName, this);
  }

  public void setAttributes(Map<String, Object> attrs) {
    synchronized (this) {
      this.cacheNames = (String[]) attrs.get(CACHE_NAMES_ATTR);
      this.maxBytesLocalHeap = (Long) attrs.get(MAX_BYTES_LOCAL_HEAP_ATTR);
      this.maxBytesLocalHeapAsString = (String) attrs.get(MAX_BYTES_LOCAL_HEAP_AS_STRING_ATTR);
      this.maxBytesLocalOffHeap = (Long) attrs.get(MAX_BYTES_LOCAL_OFFHEAP_ATTR);
      this.maxBytesLocalOffHeapAsString = (String) attrs.get(MAX_BYTES_LOCAL_OFFHEAP_AS_STRING_ATTR);
      this.maxBytesLocalDisk = (Long) attrs.get(MAX_BYTES_LOCAL_DISK_ATTR);
      this.maxBytesLocalDiskAsString = (String) attrs.get(MAX_BYTES_LOCAL_DISK_AS_STRING_ATTR);
    }
  }

  public CacheManagerModel getCacheManagerModel() {
    return cacheManagerModel;
  }

  public ObjectName getBeanName() {
    return beanName;
  }

  public String getCacheManagerName() {
    return getCacheManagerModel().getName();
  }

  public String[] getCacheNames() {
    return cacheNames;
  }

  public Long getMaxLocalHeapValue(boolean inBytes) {
    return inBytes ? getMaxBytesLocalHeap() : getMaxEntriesLocalHeap();
  }

  public Long getMaxBytesLocalHeap() {
    return maxBytesLocalHeap != null ? maxBytesLocalHeap : Long.valueOf(0);
  }

  public String getMaxBytesLocalHeapAsString() {
    return maxBytesLocalHeapAsString;
  }

  public long getMaxEntriesLocalHeap() {
    long result = 0;
    for (CacheModelInstance cacheModelInstance : cacheModelInstances()) {
      SettingsCacheModel scm = getSettingsCacheModel(cacheModelInstance);
      result += scm.getMaxEntriesLocalHeap();
    }
    return result;
  }

  public Long getMaxLocalOffHeapValue(boolean inBytes) {
    return inBytes ? getMaxBytesLocalOffHeap() : getMaxEntriesLocalOffHeap();
  }

  public Long getMaxBytesLocalOffHeap() {
    return maxBytesLocalOffHeap != null ? maxBytesLocalOffHeap : Long.valueOf(0);
  }

  public String getMaxBytesLocalOffHeapAsString() {
    return maxBytesLocalOffHeapAsString;
  }

  public long getMaxEntriesLocalOffHeap() {
    return 0;
  }

  public Long getMaxLocalDiskValue(boolean inBytes) {
    return inBytes ? getMaxBytesLocalDisk() : getMaxEntriesLocalDisk();
  }

  public Long getMaxBytesLocalDisk() {
    return maxBytesLocalDisk != null ? maxBytesLocalDisk : Long.valueOf(0);
  }

  public String getMaxBytesLocalDiskAsString() {
    return maxBytesLocalDiskAsString;
  }

  public Long getMaxEntriesLocalDisk() {
    long result = 0;
    for (CacheModelInstance cacheModelInstance : cacheModelInstances()) {
      if (!cacheModelInstance.isTerracottaClustered()) {
        SettingsCacheModel scm = getSettingsCacheModel(cacheModelInstance);
        result += scm.getMaxEntriesLocalDisk();
      }
    }
    return result;
  }

  public Long getMaxRemoteValue(boolean inBytes) {
    return inBytes ? getMaxBytesRemote() : getMaxEntriesRemote();
  }

  public Long getMaxBytesRemote() {
    long result = 0;
    for (CacheModelInstance cacheModelInstance : cacheModelInstances()) {
      if (cacheModelInstance.isTerracottaClustered()) {
        SettingsCacheModel scm = getSettingsCacheModel(cacheModelInstance);
        result += scm.getMaxBytesLocalDisk();
      }
    }
    return result;
  }

  public Long getMaxEntriesRemote() {
    long result = 0;
    for (CacheModelInstance cacheModelInstance : cacheModelInstances()) {
      if (cacheModelInstance.isTerracottaClustered()) {
        SettingsCacheModel scm = getSettingsCacheModel(cacheModelInstance);
        result += scm.getMaxEntriesLocalDisk();
      }
    }
    return result;
  }

  public boolean hasSizeBasedPooling() {
    return (getMaxBytesLocalHeap() + getMaxBytesLocalOffHeap() + getMaxBytesLocalDisk()) > 0;
  }

  public boolean hasSizeBasedCache() {
    for (CacheModelInstance cmi : cacheModelInstances()) {
      if (getSettingsCacheModel(cmi).hasSizeBasedLimits()) { return true; }
    }
    return false;
  }

  public IClient getClient() {
    return client;
  }

  public String getClientName() {
    return client != null ? client.toString() : "";
  }

  public synchronized Set<ObjectName> cacheModelInstanceBeans() {
    return Collections.unmodifiableSet(cacheModelInstanceMap.keySet());
  }

  public synchronized Iterator<CacheModelInstance> cacheModelInstanceIter() {
    List<CacheModelInstance> l = new ArrayList(cacheModelInstanceMap.values());
    Collections.sort(l);
    return Collections.unmodifiableList(l).iterator();
  }

  public synchronized Set<CacheModelInstance> cacheModelInstances() {
    return new HashSet(cacheModelInstanceMap.values());
  }

  public synchronized CacheModelInstance getCacheModelInstance(String cacheName) {
    for (Iterator<CacheModelInstance> iter = cacheModelInstanceIter(); iter.hasNext();) {
      CacheModelInstance instance = iter.next();
      if (instance.getCacheName().equals(cacheName)) { return instance; }
    }
    return null;
  }

  public boolean hasCacheModelInstance(String cacheName) {
    return getCacheModelInstance(cacheName) != null;
  }

  private synchronized CacheModelInstance removeCacheModelInstance(ObjectName on) {
    CacheModelInstance prev = cacheModelInstanceMap.remove(on);
    if (prev != null) {
      onSet.remove(on);
      cacheManagerModel.deregisterCacheModelInstance(prev);
      fireCacheInstanceRemoved(prev);
      prev.removePropertyChangeListener(this);
      removeNotificationListener(on, this);
    }
    return prev;
  }

  private CacheModelInstance addCacheModelInstance(ObjectName on, CacheModelInstance cacheModelInstance) {
    CacheModelInstance prev = cacheModelInstanceMap.putIfAbsent(on, cacheModelInstance);
    if (prev == null) {
      onSet.add(on);
      cacheManagerModel.registerCacheModelInstance(cacheModelInstance);
      fireCacheInstanceAdded(cacheModelInstance);
      cacheModelInstance.addPropertyChangeListener(this);
      addNotificationListener(on, this);
    }
    return prev;
  }

  public void cacheInstanceChanged(CacheModelInstance cacheModelInstance) {
    fireCacheInstanceChanged(cacheModelInstance);
  }

  private boolean isCacheModelInstance(ObjectName on) {
    if (on == null) { return false; }

    return on.getKeyProperty("type").equals("SampledCache")
           && on.getKeyProperty("SampledCacheManager").equals(cacheManagerModel.getName())
           && on.getKeyProperty("node").equals(client.getRemoteAddress().replace(':', '/'))
           && !on.getKeyProperty("name").endsWith("org.hibernate.cache.UpdateTimestampsCache");
  }

  @Override
  public void handleNotification(Notification notif, Object data) {
    String type = notif.getType();
    if (notif instanceof MBeanServerNotification) {
      final MBeanServerNotification mbsn = (MBeanServerNotification) notif;
      final ObjectName on = mbsn.getMBeanName();

      if (type.equals(MBeanServerNotification.UNREGISTRATION_NOTIFICATION)) {
        if (isCacheModelInstance(on)) {
          CacheModelInstance cacheModelInstance = removeCacheModelInstance(on);
          if (cacheModelInstance != null) {
            cacheModelInstance.tearDown();
          }
        }
      } else if (type.equals(MBeanServerNotification.REGISTRATION_NOTIFICATION)) {
        if (isCacheModelInstance(on)) {
          String cacheName = on.getKeyProperty("name");
          if (!hasCacheModelInstance(cacheName)) {
            CacheModelInstance instance = new CacheModelInstance(this, cacheName, on);
            if (addCacheModelInstance(on, instance) != null) {
              instance.tearDown();
            }
          }
        }
      }
    } else {
      if ("CacheManagerChanged".equals(notif.getType())) {
        setAttributes((Map<String, Object>) notif.getUserData());
        fireCacheManagerInstanceChanged();
      }
    }
  }

  private Set<ObjectName> getActiveCacheModelBeans() {
    IServer activeCoord = getActiveCoordinator();
    if (activeCoord != null) {
      try {
        StringBuilder sb = new StringBuilder(EhcacheStatsUtils.SAMPLED_CACHE_BEAN_NAME_PREFIX)
            .append(",SampledCacheManager=").append(EhcacheStatsUtils.mbeanSafe(cacheManagerModel.getName()))
            .append(",node=").append(client.getRemoteAddress().replace(':', '/')).append(",*");
        return activeCoord.queryNames(new ObjectName(sb.toString()), null);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return Collections.emptySet();
  }

  @Override
  public void init() {
    addListeners();

    setAttributes(getAttributes(beanName, ATTRIBUTE_SET));
    addNotificationListener(beanName, this);

    onSet.addAll(getActiveCacheModelBeans());
    for (ObjectName on : onSet.toArray(new ObjectName[0])) {
      String cacheName = on.getKeyProperty("name");
      if (!cacheName.endsWith("org.hibernate.cache.UpdateTimestampsCache")) {
        if (!hasCacheModelInstance(cacheName)) {
          addCacheModelInstance(on, new CacheModelInstance(this, cacheName, on));
        }
      }
    }
  }

  public void addCacheManagerInstanceListener(CacheManagerInstanceListener listener) {
    listenerList.add(CacheManagerInstanceListener.class, listener);
  }

  public void removeCacheManagerInstanceListener(CacheManagerInstanceListener listener) {
    listenerList.remove(CacheManagerInstanceListener.class, listener);
  }

  protected void fireCacheManagerInstanceChanged() {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == CacheManagerInstanceListener.class) {
        ((CacheManagerInstanceListener) listeners[i + 1]).cacheManagerInstanceChanged(this);
      }
    }
  }

  protected void fireCacheInstanceAdded(CacheModelInstance instance) {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == CacheManagerInstanceListener.class) {
        ((CacheManagerInstanceListener) listeners[i + 1]).cacheModelInstanceAdded(instance);
      }
    }
  }

  protected void fireCacheInstanceRemoved(CacheModelInstance instance) {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == CacheManagerInstanceListener.class) {
        ((CacheManagerInstanceListener) listeners[i + 1]).cacheModelInstanceRemoved(instance);
      }
    }
  }

  protected void fireCacheInstanceChanged(CacheModelInstance instance) {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == CacheManagerInstanceListener.class) {
        ((CacheManagerInstanceListener) listeners[i + 1]).cacheModelInstanceChanged(instance);
      }
    }
  }

  @Override
  public void suspend() {
    /**/
  }

  public void propertyChange(PropertyChangeEvent evt) {
    String prop = evt.getPropertyName();
    if ("Enabled".equals(prop)) {
      /**/
    }
  }

  public int getInstanceCount() {
    return cacheModelInstanceMap.size();
  }

  public int getEnabledCount() {
    int result = 0;
    for (CacheModelInstance info : cacheModelInstanceMap.values()) {
      if (info.isEnabled()) {
        result++;
      }
    }
    return result;
  }

  public int getBulkLoadEnabledCount() {
    int result = 0;
    for (CacheModelInstance info : cacheModelInstanceMap.values()) {
      if (info.isBulkLoadEnabled()) {
        result++;
      }
    }
    return result;
  }

  public int getBulkLoadDisabledCount() {
    int result = 0;
    for (CacheModelInstance info : cacheModelInstanceMap.values()) {
      if (!info.isBulkLoadEnabled()) {
        result++;
      }
    }
    return result;
  }

  public int getTransactionalCount() {
    int result = 0;
    for (CacheModelInstance info : cacheModelInstanceMap.values()) {
      if (info.isTransactional()) {
        result++;
      }
    }
    return result;
  }

  public int getStatisticsEnabledCount() {
    int result = 0;
    for (CacheModelInstance info : cacheModelInstanceMap.values()) {
      if (info.isStatisticsEnabled()) {
        result++;
      }
    }
    return result;
  }

  public int getTerracottaClusteredInstanceCount() {
    int result = 0;
    for (CacheModelInstance info : cacheModelInstanceMap.values()) {
      if (info.isTerracottaClustered()) {
        result++;
      }
    }
    return result;
  }

  public void setCachesEnabled(boolean enabled) {
    safeSetAttribute(cacheModelInstanceBeans(), "Enabled", Boolean.valueOf(enabled));
  }

  public void setCachesBulkLoadEnabled(boolean bulkLoadEnabled) {
    safeSetAttribute(cacheModelInstanceBeans(), "NodeBulkLoadEnabled", Boolean.valueOf(bulkLoadEnabled));
  }

  public void setStatisticsEnabled(boolean enabled) {
    safeSetAttribute(cacheModelInstanceBeans(), "StatisticsEnabled", Boolean.valueOf(enabled));
  }

  public void clearAll() {
    invokeAll("clearAll");
  }

  public boolean isCacheTerracottaClustered(String cacheName) {
    CacheModelInstance cacheModelInstance = getCacheModelInstance(cacheName);
    if (cacheModelInstance != null) { return cacheModelInstance.isTerracottaClustered(); }
    return false;
  }

  public String generateActiveConfigDeclaration() {
    Object result = invokeOnce(beanName, "generateActiveConfigDeclaration");
    return result != null ? result.toString() : "";
  }

  public String generateActiveConfigDeclaration(String cacheName) {
    Object result = invokeOnce(beanName, "generateActiveConfigDeclaration", new Object[] { cacheName },
                               new String[] { "java.lang.String" });
    return result != null ? result.toString() : "";
  }

  public Map<CacheModelInstance, CacheTierSize> getSizes() {
    Map<CacheModelInstance, CacheTierSize> result = new TreeMap<CacheModelInstance, CacheTierSize>();
    Map<ObjectName, Map<String, Object>> attrs = getAttributes(new HashSet(
                                                                           Arrays
                                                                               .asList(new String[] {
                                                                                   CacheStatisticsModel.LOCAL_HEAP_SIZE,
                                                                                   CacheStatisticsModel.LOCAL_HEAP_SIZE_IN_BYTES,
                                                                                   CacheStatisticsModel.LOCAL_OFFHEAP_SIZE,
                                                                                   CacheStatisticsModel.LOCAL_OFFHEAP_SIZE_IN_BYTES,
                                                                                   CacheStatisticsModel.LOCAL_DISK_SIZE,
                                                                                   CacheStatisticsModel.LOCAL_DISK_SIZE_IN_BYTES,
                                                                                   CacheStatisticsModel.IN_MEMORY_MISS_COUNT,
                                                                                   CacheStatisticsModel.OFF_HEAP_MISS_COUNT,
                                                                                   CacheStatisticsModel.ON_DISK_MISS_COUNT,
                                                                                   "CacheInMemoryMissRate",
                                                                                   "CacheOffHeapMissRate",
                                                                                   "CacheOnDiskMissRate" })));
    for (Map.Entry<ObjectName, Map<String, Object>> entry : attrs.entrySet()) {
      Map<String, Object> value = entry.getValue();
      CacheModelInstance cmi = cacheModelInstanceMap.get(entry.getKey());
      if (cmi != null) {
        result.put(cmi, getTierSize(cmi, getSettingsCacheModel(cmi), value));
      }
    }
    return result;
  }

  public CacheTierSize getTierSize(CacheModelInstance cmi, SettingsCacheModel scm, Map<String, Object> sizes) {
    return new CacheTierSize(getMaxEntriesLocalHeap(), getLong(sizes, CacheStatisticsModel.LOCAL_HEAP_SIZE),
                             getLong(sizes, CacheStatisticsModel.LOCAL_OFFHEAP_SIZE), scm.getMaxEntriesLocalDisk(),
                             getLong(sizes, CacheStatisticsModel.LOCAL_DISK_SIZE), cmi.getMaxBytesLocalHeap(),
                             getLong(sizes, CacheStatisticsModel.LOCAL_HEAP_SIZE_IN_BYTES),
                             cmi.getMaxBytesLocalOffHeap(), getLong(sizes,
                                                                    CacheStatisticsModel.LOCAL_OFFHEAP_SIZE_IN_BYTES),
                             scm.getMaxBytesLocalDisk(), getLong(sizes, CacheStatisticsModel.LOCAL_DISK_SIZE_IN_BYTES),
                             getLong(sizes, "CacheInMemoryMissRate"), getLong(sizes, "CacheOffHeapMissRate"),
                             getLong(sizes, "CacheOnDiskMissRate"));
  }

  private static long getLong(Map<String, Object> sizes, String attr) {
    Long result = (Long) sizes.get(attr);
    return result != null ? result.longValue() : 0;
  }

  public SettingsCacheModel getSettingsCacheModel(String cacheName) throws MalformedObjectNameException,
      NullPointerException {
    return getSettingsCacheModel(cacheModelInstanceMap.get(new ObjectName(cacheName)));
  }

  public SettingsCacheModel getSettingsCacheModel(CacheModelInstance cmi) {
    if (cmi != null) {
      if (cmi.isTerracottaClustered()) {
        return getCacheManagerModel().getClusteredCacheModel(cmi.getCacheName());
      } else {
        return getCacheManagerModel().getStandaloneCacheModel(cmi);
      }
    }
    return null;
  }

  public int compareTo(Object o) {
    CacheManagerInstance other = (CacheManagerInstance) o;
    return getCacheManagerName().compareTo(other.getCacheManagerName());
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
    CacheManagerInstance other = (CacheManagerInstance) obj;
    if (beanName == null) {
      if (other.beanName != null) return false;
    } else if (!beanName.equals(other.beanName)) return false;
    return true;
  }

  @Override
  public String toString() {
    return getCacheManagerName() + " on node " + getClientName();
  }

  @Override
  public void tearDown() {
    removeListeners();

    for (CacheModelInstance cacheModelInstance : cacheModelInstanceMap.values()) {
      cacheModelInstance.tearDown();
    }
    cacheModelInstanceMap.clear();

    super.tearDown();
  }
}

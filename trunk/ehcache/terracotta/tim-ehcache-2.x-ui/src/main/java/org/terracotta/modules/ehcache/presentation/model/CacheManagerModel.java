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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.ObjectName;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

public class CacheManagerModel extends BaseMBeanModel implements PropertyChangeListener {
  private final EhcacheModel                                            ehcacheModel;
  private final String                                                  name;
  private final Set<ObjectName>                                         pendingClients;
  private final ConcurrentMap<ObjectName, CacheManagerInstance>         cacheManagerInstanceMap;
  private final ConcurrentMap<String, CacheModel>                       cacheModelMap;
  private final ConcurrentMap<String, ClusteredCacheModel>              clusteredCacheModelMap;
  private final ConcurrentMap<CacheModelInstance, StandaloneCacheModel> standaloneCacheModelMap;
  private Boolean                                                       cachesEnabledPersistently;
  private Boolean                                                       cachesBulkLoadEnabledPersistently;
  private Boolean                                                       statisticsEnabledPersistently;

  public CacheManagerModel(EhcacheModel ehCacheModel, String name) {
    super(ehCacheModel.getClusterModel());

    this.ehcacheModel = ehCacheModel;
    this.name = name;

    pendingClients = new HashSet<ObjectName>();
    cacheManagerInstanceMap = new ConcurrentHashMap<ObjectName, CacheManagerInstance>();
    cacheModelMap = new ConcurrentHashMap<String, CacheModel>();
    clusteredCacheModelMap = new ConcurrentHashMap<String, ClusteredCacheModel>();
    standaloneCacheModelMap = new ConcurrentHashMap<CacheModelInstance, StandaloneCacheModel>();
  }

  public boolean isEmpty() {
    return cacheManagerInstanceMap != null && cacheManagerInstanceMap.isEmpty();
  }

  public String getName() {
    return name;
  }

  public EhcacheModel getEhcacheModel() {
    return ehcacheModel;
  }

  public int getInstanceCount() {
    return cacheManagerInstanceMap != null ? cacheManagerInstanceMap.size() : 0;
  }

  public int getCacheModelCount() {
    return cacheModelMap != null ? cacheModelMap.size() : 0;
  }

  public Set<CacheModelInstance> allCacheModelInstances() {
    LinkedHashSet<CacheModelInstance> list = new LinkedHashSet<CacheModelInstance>();
    for (CacheModel cacheModel : cacheModels()) {
      list.addAll(cacheModel.cacheModelInstances());
    }
    return Collections.unmodifiableSet(list);
  }

  public Set<CacheModelInstance> cacheModelInstances(CacheModel cacheModel) {
    LinkedHashSet<CacheModelInstance> list = new LinkedHashSet<CacheModelInstance>();
    for (Iterator<CacheManagerInstance> iter = cacheManagerInstanceIterator(); iter.hasNext();) {
      CacheManagerInstance cmi = iter.next();
      CacheModelInstance cacheModelInstance = cmi.getCacheModelInstance(cacheModel.getCacheName());
      if (cacheModelInstance != null) {
        list.add(cacheModelInstance);
      }
    }
    return Collections.unmodifiableSet(list);
  }

  public Set<CacheModelInstance> clusteredCacheModelInstances(CacheModel cacheModel) {
    LinkedHashSet<CacheModelInstance> list = new LinkedHashSet<CacheModelInstance>();
    for (Iterator<CacheManagerInstance> iter = cacheManagerInstanceIterator(); iter.hasNext();) {
      CacheManagerInstance cmi = iter.next();
      CacheModelInstance cacheModelInstance = cmi.getCacheModelInstance(cacheModel.getCacheName());
      if (cacheModelInstance != null && cacheModelInstance.isTerracottaClustered()) {
        list.add(cacheModelInstance);
      }
    }
    return Collections.unmodifiableSet(list);
  }

  public CacheManagerInstance getInstance(IClient client) {
    if (client == null || cacheManagerInstanceMap == null) { return null; }

    String remoteAddress = client.getRemoteAddress().replace(':', '/');
    for (ObjectName on : cacheManagerInstanceMap.keySet()) {
      String node = on.getKeyProperty("node");
      if (remoteAddress.equals(node)) { return cacheManagerInstanceMap.get(on); }
    }

    return null;
  }

  public CacheModelInstance getCacheModelInstance(IClient client, String cacheName) {
    CacheManagerInstance cacheManagerInstance = getInstance(client);
    return cacheManagerInstance != null ? cacheManagerInstance.getCacheModelInstance(cacheName) : null;
  }

  public IClient getClient(ObjectName on) {
    if (on == null || clusterModel == null) { return null; }

    String node = on.getKeyProperty("node").replace('/', ':');
    IClient result = null;
    for (IClient client : clusterModel.getClients()) {
      if (client.getRemoteAddress().equals(node)) {
        result = client;
        break;
      }
    }
    return result;
  }

  @Override
  public void clientConnected(IClient client) {
    for (ObjectName on : pendingClients.toArray(new ObjectName[0])) {
      IClient theClient = getClient(on);
      if (theClient != null) {
        bindClientToCacheManagerInstance(theClient, on);
      }
    }
  }

  @Override
  public void clientDisconnected(IClient client) {/**/
  }

  // The ObjectName here represents a CacheManagerInstance
  private void addPendingClient(ObjectName on) {
    pendingClients.add(on);
  }

  private void bindClientToCacheManagerInstance(IClient client, ObjectName on) {
    CacheManagerInstance instance = new CacheManagerInstance(this, client, on);
    if (cacheManagerInstanceMap.putIfAbsent(on, instance) == null) {
      onSet.add(on);
      instance.startup();
      addNotificationListener(on, this);
      fireInstanceAdded(instance);
    } else {
      instance.tearDown();
    }
  }

  @Override
  public void init() {
    addListeners();

    onSet.addAll(getActiveCacheManagerBeans());
    for (ObjectName on : onSet.toArray(new ObjectName[0])) {
      IClient client = getClient(on);
      if (client != null) {
        bindClientToCacheManagerInstance(client, on);
      } else {
        addPendingClient(on);
      }
    }

    initCacheModelMap();

    if (getStatisticsEnabledCount() == getCacheModelInstanceCount()) {
      statisticsEnabledPersistently = Boolean.TRUE;
    }
  }

  @Override
  public void suspend() {
    for (ObjectName on : onSet.toArray(new ObjectName[0])) {
      CacheManagerInstance instance = cacheManagerInstanceMap.remove(on);
      if (instance != null) {
        fireInstanceRemoved(instance);
        instance.tearDown();
      }
    }

    for (CacheModel cacheModel : cacheModels()) {
      removeCacheModel(cacheModel.getCacheName());
    }

    onSet.clear();
  }

  private boolean testAddCacheModel(ObjectName on) {
    String cacheName = on.getKeyProperty("name");
    if (cacheName != null && !cacheName.endsWith("org.hibernate.cache.UpdateTimestampsCache")) {
      if (!hasCacheModel(cacheName)) {
        CacheModel cacheModel = new CacheModel(this, cacheName);
        CacheModel prev = addCacheModel(cacheModel);
        if (prev == null) {
          cacheModel.startup();
        }
        return prev == null;
      }
    }
    return false;
  }

  private void initCacheModelMap() {
    for (ObjectName objectName : getActiveCacheModelBeans()) {
      testAddCacheModel(objectName);
    }
  }

  public Set<ObjectName> allCacheManagerBeans() {
    if (onSet != null) { return new HashSet<ObjectName>(onSet); }
    return Collections.emptySet();
  }

  private Set<ObjectName> getActiveCacheManagerBeans() {
    IServer activeCoord = getActiveCoordinator();
    if (activeCoord != null) {
      try {
        StringBuilder sb = new StringBuilder(EhcacheStatsUtils.SAMPLED_CACHE_MANAGER_BEAN_NAME_PREFIX).append(",name=")
            .append(EhcacheStatsUtils.mbeanSafe(name)).append(",*");
        return activeCoord.queryNames(new ObjectName(sb.toString()), null);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return Collections.emptySet();
  }

  private Set<ObjectName> getActiveCacheModelBeans() {
    IServer activeCoord = getActiveCoordinator();
    if (activeCoord != null) {
      try {
        StringBuilder sb = new StringBuilder(EhcacheStatsUtils.SAMPLED_CACHE_BEAN_NAME_PREFIX)
            .append(",SampledCacheManager=").append(EhcacheStatsUtils.mbeanSafe(name)).append(",*");
        return activeCoord.queryNames(new ObjectName(sb.toString()), null);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return Collections.emptySet();
  }

  public Set<CacheModel> cacheModels() {
    return Collections.unmodifiableSet(new HashSet(cacheModelMap.values()));
  }

  public Iterator<CacheModel> cacheModelIterator() {
    return new TreeMap(cacheModelMap).values().iterator();
  }

  public Set<CacheManagerInstance> cacheManagerInstances() {
    return Collections.unmodifiableSet(new HashSet(cacheManagerInstanceMap.values()));
  }

  public Iterator<CacheManagerInstance> cacheManagerInstanceIterator() {
    List<CacheManagerInstance> l = new ArrayList(cacheManagerInstanceMap.values());
    Collections.sort(l);
    return l.iterator();
  }

  public CacheModel getCacheModel(String cacheName) {
    return cacheModelMap.get(cacheName);
  }

  public boolean hasCacheModel(String cacheName) {
    return getCacheModel(cacheName) != null;
  }

  public CacheModel removeCacheModel(String cacheName) {
    CacheModel prev = cacheModelMap.remove(cacheName);
    if (prev != null) {
      fireCacheModelRemoved(prev);
      prev.removePropertyChangeListener(this);
    }
    return prev;
  }

  public CacheModel addCacheModel(CacheModel cacheModel) {
    CacheModel prev = cacheModelMap.putIfAbsent(cacheModel.getCacheName(), cacheModel);
    if (prev == null) {
      cacheModel.addPropertyChangeListener(this);
      fireCacheModelAdded(cacheModel);
    }
    return prev;
  }

  public void cacheModelChanged(CacheModel cacheModel) {
    fireCacheModelChanged(cacheModel);
  }

  public void registerCacheModelInstance(CacheModelInstance cacheModelInstance) {
    if (cacheModelInstance != null) {
      String cacheName = cacheModelInstance.getCacheName();
      ObjectName beanName = cacheModelInstance.getBeanName();
      if (cacheModelInstance.isTerracottaClustered()) {
        ClusteredCacheModel cacheModel = getClusteredCacheModel(cacheName);
        if (cacheModel == null) {
          addClusteredCacheModel(cacheModel = new ClusteredCacheModel(this, cacheName, beanName));
        } else {
          cacheModel.addInstance(beanName);
        }
      } else {
        addStandaloneCacheModel(cacheModelInstance, new StandaloneCacheModel(this, cacheModelInstance));
      }
    }
  }

  public void deregisterCacheModelInstance(CacheModelInstance cacheModelInstance) {
    if (cacheModelInstance != null) {
      String cacheName = cacheModelInstance.getCacheName();
      if (cacheModelInstance.isTerracottaClustered()) {
        ClusteredCacheModel cacheModel = getClusteredCacheModel(cacheName);
        if (cacheModel != null) {
          cacheModel.removeInstance(cacheModelInstance.getBeanName());
          if (cacheModel.instanceCount() == 0) {
            removeClusteredCacheModel(cacheName);
          }
        }
      } else {
        removeStandaloneCacheModel(cacheModelInstance);
      }
    }
  }

  public StandaloneCacheModel removeStandaloneCacheModel(CacheModelInstance cacheModelInstance) {
    StandaloneCacheModel cacheModel = standaloneCacheModelMap.remove(cacheModelInstance);
    if (cacheModel != null) {
      cacheModel.removePropertyChangeListener(this);
      fireStandaloneCacheModelRemoved(cacheModel);
    }
    return cacheModel;
  }

  public StandaloneCacheModel addStandaloneCacheModel(CacheModelInstance cacheModelInstance,
                                                      StandaloneCacheModel cacheModel) {
    StandaloneCacheModel prev = standaloneCacheModelMap.putIfAbsent(cacheModelInstance, cacheModel);
    if (prev == null) {
      cacheModel.addPropertyChangeListener(this);
      fireStandaloneCacheModelAdded(cacheModel);
    }
    return prev;
  }

  public Map<CacheModelInstance, StandaloneCacheModel> standaloneCacheModels() {
    return new HashMap(standaloneCacheModelMap);
  }

  public CacheModelInstance cacheModelInstance(StandaloneCacheModel cacheModel) {
    for (Map.Entry<CacheModelInstance, StandaloneCacheModel> entry : standaloneCacheModelMap.entrySet()) {
      if (entry.getValue().equals(cacheModel)) { return entry.getKey(); }
    }
    return null;
  }

  public StandaloneCacheModel standaloneCacheModel(CacheModelInstance cacheModelInstance) {
    return standaloneCacheModelMap.get(cacheModelInstance);
  }

  public ClusteredCacheModel addClusteredCacheModel(ClusteredCacheModel cacheModel) {
    ClusteredCacheModel prev = clusteredCacheModelMap.putIfAbsent(cacheModel.getCacheName(), cacheModel);
    if (prev == null) {
      cacheModel.addPropertyChangeListener(this);
      fireClusteredCacheModelAdded(cacheModel);
    }
    return prev;
  }

  public ClusteredCacheModel removeClusteredCacheModel(String cacheName) {
    ClusteredCacheModel cacheModel = clusteredCacheModelMap.remove(cacheName);
    if (cacheModel != null) {
      cacheModel.removePropertyChangeListener(this);
      fireClusteredCacheModelRemoved(cacheModel);
    }
    return cacheModel;
  }

  public StandaloneCacheModel getStandaloneCacheModel(CacheModelInstance cacheModelInstance) {
    return standaloneCacheModelMap.get(cacheModelInstance);
  }

  public ClusteredCacheModel getClusteredCacheModel(String cacheName) {
    return clusteredCacheModelMap.get(cacheName);
  }

  public boolean hasClusteredCacheModel(String cacheName) {
    return getClusteredCacheModel(cacheName) != null;
  }

  public Set<ClusteredCacheModel> clusteredCacheModels() {
    return new HashSet(clusteredCacheModelMap.values());
  }

  public Iterator<ClusteredCacheModel> clusteredCacheModelIterator() {
    return new TreeMap(clusteredCacheModelMap).values().iterator();
  }

  public Map<CacheModelInstance, Object> invokeCacheModelInstances(Set<CacheModelInstance> targets, String operation) {
    IServer activeCoord = getActiveCoordinator();
    if (activeCoord != null) {
      Map<ObjectName, CacheModelInstance> on2cmi = new HashMap<ObjectName, CacheModelInstance>();
      for (CacheModelInstance cmi : targets) {
        on2cmi.put(cmi.getBeanName(), cmi);
      }
      Map<ObjectName, Object> response = invokeAll(on2cmi.keySet(), operation);
      Map<CacheModelInstance, Object> result = new HashMap<CacheModelInstance, Object>();
      for (Entry<ObjectName, Object> entry : response.entrySet()) {
        result.put(on2cmi.get(entry.getKey()), entry.getValue());
      }
      return result;
    }
    return Collections.emptyMap();
  }

  public Map<CacheModelInstance, Exception> setCacheModelInstanceAttribute(String attr,
                                                                           Map<CacheModelInstance, Object> attrMap)
      throws Exception {
    IServer activeCoord = getActiveCoordinator();
    if (activeCoord != null) {
      Map<ObjectName, CacheModelInstance> on2cmi = new HashMap<ObjectName, CacheModelInstance>();
      Map<ObjectName, Object> on2val = new HashMap<ObjectName, Object>();
      for (Entry<CacheModelInstance, Object> entry : attrMap.entrySet()) {
        CacheModelInstance cmi = entry.getKey();
        ObjectName on = cmi.getBeanName();
        on2cmi.put(on, cmi);
        on2val.put(on, entry.getValue());
      }
      Map<ObjectName, Exception> response = activeCoord.setAttribute(attr, on2val);
      Map<CacheModelInstance, Exception> result = new HashMap<CacheModelInstance, Exception>();
      for (Entry<ObjectName, Exception> entry : response.entrySet()) {
        result.put(on2cmi.get(entry.getKey()), entry.getValue());
      }
      return result;
    }
    return Collections.emptyMap();
  }

  public void clearStatistics() {
    invokeAll("clearStatistics");
  }

  public void setStatisticsEnabled(boolean enabled) {
    setStatisticsEnabled(enabled, false);
  }

  public void setStatisticsEnabled(boolean enabled, boolean persistent) {
    setStatisticsEnabledPersistently(enabled, persistent);
    safeSetAttribute("StatisticsEnabled", Boolean.valueOf(enabled));
  }

  public void setStatisticsEnabledPersistently(boolean enabled, boolean persistent) {
    if (persistent) {
      statisticsEnabledPersistently = Boolean.valueOf(enabled);
    } else {
      statisticsEnabledPersistently = null;
    }
  }

  public void setStatisticsEnabled(CacheModel cacheModel, boolean enabled) {
    safeSetAttribute(allCacheModelInstanceBeans(cacheModel), "StatisticsEnabled", Boolean.valueOf(enabled));
  }

  public Boolean isStatisticsEnabledPersistently() {
    return statisticsEnabledPersistently;
  }

  public void clearAllCaches() {
    invokeAll("clearAll");
  }

  public Set<ObjectName> allCacheModelInstanceBeans() {
    Set<ObjectName> result = new LinkedHashSet<ObjectName>();
    for (Iterator<CacheManagerInstance> iter = cacheManagerInstanceIterator(); iter.hasNext();) {
      result.addAll(iter.next().cacheModelInstanceBeans());
    }
    return Collections.unmodifiableSet(result);
  }

  public Set<ObjectName> allCacheModelInstanceBeans(CacheModel cacheModel) {
    Set<ObjectName> result = new LinkedHashSet<ObjectName>();
    for (Iterator<CacheManagerInstance> iter = cacheManagerInstanceIterator(); iter.hasNext();) {
      CacheManagerInstance cmi = iter.next();
      CacheModelInstance cacheModelInstance = cmi.getCacheModelInstance(cacheModel.getCacheName());
      if (cacheModelInstance != null) {
        result.add(cacheModelInstance.getBeanName());
      }
    }
    return Collections.unmodifiableSet(result);
  }

  public void setBulkLoadEnabled(boolean bulkLoadEnabled) {
    setBulkLoadEnabled(bulkLoadEnabled, false);
  }

  public void setBulkLoadEnabled(boolean bulkLoadEnabled, boolean persistent) {
    setCachesBulkLoadEnabledPersistently(bulkLoadEnabled, persistent);
    safeSetAttribute(allCacheModelInstanceBeans(), "NodeBulkLoadEnabled", Boolean.valueOf(bulkLoadEnabled));
  }

  public void setCachesBulkLoadEnabledPersistently(boolean bulkLoadEnabled, boolean persistent) {
    if (persistent) {
      cachesBulkLoadEnabledPersistently = Boolean.valueOf(bulkLoadEnabled);
    } else {
      cachesBulkLoadEnabledPersistently = null;
    }
  }

  public void setBulkLoadEnabled(CacheModel cacheModel, boolean bulkLoadEnabled) {
    safeSetAttribute(allCacheModelInstanceBeans(cacheModel), "NodeBulkLoadEnabled", Boolean.valueOf(bulkLoadEnabled));
  }

  public Boolean isBulkLoadEnabledPersistently() {
    return cachesBulkLoadEnabledPersistently;
  }

  public void setCachesEnabled(boolean enabled) {
    setCachesEnabled(enabled, false);
  }

  public void setCachesEnabled(boolean enabled, boolean persistent) {
    setCachesEnabledPersistently(enabled, persistent);
    safeSetAttribute("Enabled", Boolean.valueOf(enabled));
  }

  public void setCachesEnabledPersistently(boolean enabled, boolean persistent) {
    if (persistent) {
      cachesEnabledPersistently = Boolean.valueOf(enabled);
    } else {
      cachesEnabledPersistently = null;
    }
  }

  public Boolean isCachesEnabledPersistently() {
    return cachesEnabledPersistently;
  }

  public void setCachesEnabled(CacheModel cacheModel, boolean enabled) {
    safeSetAttribute(allCacheModelInstanceBeans(cacheModel), "Enabled", Boolean.valueOf(enabled));
  }

  public int getCacheModelInstanceCount() {
    int result = 0;
    for (Iterator<CacheManagerInstance> iter = cacheManagerInstanceIterator(); iter.hasNext();) {
      result += iter.next().getInstanceCount();
    }
    return result;
  }

  public int getCacheModelInstanceCount(CacheModel cacheModel) {
    int result = 0;
    for (Iterator<CacheManagerInstance> iter = cacheManagerInstanceIterator(); iter.hasNext();) {
      if (iter.next().getCacheModelInstance(cacheModel.getCacheName()) != null) {
        result++;
      }
    }
    return result;
  }

  public int getEnabledCount() {
    int result = 0;
    for (Iterator<CacheManagerInstance> iter = cacheManagerInstanceIterator(); iter.hasNext();) {
      result += iter.next().getEnabledCount();
    }
    return result;
  }

  public int getEnabledCount(CacheModel cacheModel) {
    int result = 0;
    for (Iterator<CacheManagerInstance> iter = cacheManagerInstanceIterator(); iter.hasNext();) {
      CacheModelInstance cacheModelInstance = iter.next().getCacheModelInstance(cacheModel.getCacheName());
      if (cacheModelInstance != null && cacheModelInstance.isEnabled()) {
        result++;
      }
    }
    return result;
  }

  public int getTerracottaClusteredInstanceCount(CacheModel cacheModel) {
    int result = 0;
    for (Iterator<CacheManagerInstance> iter = cacheManagerInstanceIterator(); iter.hasNext();) {
      CacheModelInstance cacheModelInstance = iter.next().getCacheModelInstance(cacheModel.getCacheName());
      if (cacheModelInstance != null && cacheModelInstance.isTerracottaClustered()) {
        result++;
      }
    }
    return result;
  }

  public int getTransactionalCount() {
    int result = 0;
    for (Iterator<CacheManagerInstance> iter = cacheManagerInstanceIterator(); iter.hasNext();) {
      result += iter.next().getTransactionalCount();
    }
    return result;
  }

  public int getTransactionalCount(CacheModel cacheModel) {
    int result = 0;
    for (Iterator<CacheManagerInstance> iter = cacheManagerInstanceIterator(); iter.hasNext();) {
      CacheModelInstance cacheModelInstance = iter.next().getCacheModelInstance(cacheModel.getCacheName());
      if (cacheModelInstance != null && cacheModelInstance.isTransactional()) {
        result++;
      }
    }
    return result;
  }

  public int getBulkLoadEnabledCount() {
    int result = 0;
    for (Iterator<CacheManagerInstance> iter = cacheManagerInstanceIterator(); iter.hasNext();) {
      result += iter.next().getBulkLoadEnabledCount();
    }
    return result;
  }

  public int getBulkLoadDisabledCount() {
    int result = 0;
    for (Iterator<CacheManagerInstance> iter = cacheManagerInstanceIterator(); iter.hasNext();) {
      result += iter.next().getBulkLoadDisabledCount();
    }
    return result;
  }

  public int getBulkLoadEnabledCount(CacheModel cacheModel) {
    int result = 0;
    for (Iterator<CacheManagerInstance> iter = cacheManagerInstanceIterator(); iter.hasNext();) {
      CacheModelInstance cacheModelInstance = iter.next().getCacheModelInstance(cacheModel.getCacheName());
      if (cacheModelInstance != null && cacheModelInstance.isBulkLoadEnabled()) {
        result++;
      }
    }
    return result;
  }

  public int getBulkLoadDisabledCount(CacheModel cacheModel) {
    int result = 0;
    for (Iterator<CacheManagerInstance> iter = cacheManagerInstanceIterator(); iter.hasNext();) {
      CacheModelInstance cacheModelInstance = iter.next().getCacheModelInstance(cacheModel.getCacheName());
      if (cacheModelInstance != null && !cacheModelInstance.isBulkLoadEnabled()) {
        result++;
      }
    }
    return result;
  }

  public int getStatisticsEnabledCount() {
    int result = 0;
    for (Iterator<CacheManagerInstance> iter = cacheManagerInstanceIterator(); iter.hasNext();) {
      result += iter.next().getStatisticsEnabledCount();
    }
    return result;
  }

  public int getStatisticsEnabledCount(CacheModel cacheModel) {
    int result = 0;
    for (Iterator<CacheManagerInstance> iter = cacheManagerInstanceIterator(); iter.hasNext();) {
      CacheModelInstance cacheModelInstance = iter.next().getCacheModelInstance(cacheModel.getCacheName());
      if (cacheModelInstance != null && cacheModelInstance.isStatisticsEnabled()) {
        result++;
      }
    }
    return result;
  }

  public boolean isCacheManagerInstance(ObjectName on) {
    if (on == null) { return false; }
    return on.getKeyProperty("type").equals("SampledCacheManager") && on.getKeyProperty("name").equals(name);
  }

  public static boolean isCacheManagerInstance(ObjectName on, String name) {
    if (on == null || name == null) { return false; }
    return on.getKeyProperty("type").equals("SampledCacheManager") && on.getKeyProperty("name").equals(name);
  }

  public boolean isCacheInstance(ObjectName on) {
    if (on == null) { return false; }
    return on.getKeyProperty("type").equals("SampledCache") && on.getKeyProperty("SampledCacheManager").equals(name);
  }

  @Override
  public void handleNotification(Notification notif, Object data) {
    String type = notif.getType();
    if (notif instanceof MBeanServerNotification) {
      final MBeanServerNotification mbsn = (MBeanServerNotification) notif;
      final ObjectName on = mbsn.getMBeanName();

      if (type.equals(MBeanServerNotification.UNREGISTRATION_NOTIFICATION)) {
        if (isCacheManagerInstance(on)) {
          onSet.remove(on);
          CacheManagerInstance instance = cacheManagerInstanceMap.remove(on);
          if (instance != null) {
            fireInstanceRemoved(instance);
            instance.tearDown();
          }
          if (cacheManagerInstanceMap.size() == 0) {
            CacheManagerModel cacheManagerModel = ehcacheModel.removeCacheManagerModel(name);
            if (cacheManagerModel != null) {
              ehcacheModel.fireCacheManagerRemoved(cacheManagerModel);
              cacheManagerModel.tearDown();
            }
          }
        }
      } else if (type.equals(MBeanServerNotification.REGISTRATION_NOTIFICATION)) {
        if (isCacheManagerInstance(on)) {
          IClient client = getClient(on);
          if (client != null) {
            bindClientToCacheManagerInstance(client, on);
          } else {
            addPendingClient(on);
          }
        } else if (isCacheInstance(on)) {
          testAddCacheModel(on);
        }
      }
    }
  }

  public void addCacheManagerModelListener(CacheManagerModelListener listener) {
    listenerList.remove(CacheManagerModelListener.class, listener);
    listenerList.add(CacheManagerModelListener.class, listener);
  }

  public void removeCacheManagerModelListener(CacheManagerModelListener listener) {
    listenerList.remove(CacheManagerModelListener.class, listener);
  }

  protected void fireInstanceAdded(CacheManagerInstance instance) {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == CacheManagerModelListener.class) {
        ((CacheManagerModelListener) listeners[i + 1]).instanceAdded(instance);
      }
    }
  }

  protected void fireInstanceRemoved(CacheManagerInstance instance) {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == CacheManagerModelListener.class) {
        ((CacheManagerModelListener) listeners[i + 1]).instanceRemoved(instance);
      }
    }
  }

  protected void fireCacheModelAdded(CacheModel cacheModel) {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == CacheManagerModelListener.class) {
        ((CacheManagerModelListener) listeners[i + 1]).cacheModelAdded(cacheModel);
      }
    }
  }

  protected void fireCacheModelRemoved(CacheModel cacheModel) {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == CacheManagerModelListener.class) {
        ((CacheManagerModelListener) listeners[i + 1]).cacheModelRemoved(cacheModel);
      }
    }
  }

  protected void fireCacheModelChanged(CacheModel cacheModel) {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == CacheManagerModelListener.class) {
        ((CacheManagerModelListener) listeners[i + 1]).cacheModelChanged(cacheModel);
      }
    }
  }

  protected void fireClusteredCacheModelAdded(ClusteredCacheModel cacheModel) {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == CacheManagerModelListener.class) {
        ((CacheManagerModelListener) listeners[i + 1]).clusteredCacheModelAdded(cacheModel);
      }
    }
  }

  protected void fireClusteredCacheModelRemoved(ClusteredCacheModel cacheModel) {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == CacheManagerModelListener.class) {
        ((CacheManagerModelListener) listeners[i + 1]).clusteredCacheModelRemoved(cacheModel);
      }
    }
  }

  protected void fireClusteredCacheModelChanged(ClusteredCacheModel cacheModel) {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == CacheManagerModelListener.class) {
        ((CacheManagerModelListener) listeners[i + 1]).clusteredCacheModelChanged(cacheModel);
      }
    }
  }

  protected void fireStandaloneCacheModelAdded(StandaloneCacheModel cacheModel) {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == CacheManagerModelListener.class) {
        ((CacheManagerModelListener) listeners[i + 1]).standaloneCacheModelAdded(cacheModel);
      }
    }
  }

  protected void fireStandaloneCacheModelRemoved(StandaloneCacheModel cacheModel) {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == CacheManagerModelListener.class) {
        ((CacheManagerModelListener) listeners[i + 1]).standaloneCacheModelRemoved(cacheModel);
      }
    }
  }

  protected void fireStandaloneCacheModelChanged(StandaloneCacheModel cacheModel) {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == CacheManagerModelListener.class) {
        ((CacheManagerModelListener) listeners[i + 1]).standaloneCacheModelChanged(cacheModel);
      }
    }
  }

  public String generateActiveConfigDeclaration() {
    Object result = invokeOnce("generateActiveConfigDeclaration");
    return result != null ? result.toString() : "";
  }

  public String generateActiveConfigDeclaration(String cacheName) {
    Object result = invokeOnce("generateActiveConfigDeclaration", new Object[] { cacheName },
                               new String[] { "java.lang.String" });
    return result != null ? result.toString() : "";
  }

  public TableModel executeQuery(String cacheName, String query) throws Exception {
    ClusteredCacheModel cacheModel = getClusteredCacheModel(cacheName);
    if (cacheModel != null) { return cacheModel.executeQuery(query); }
    return new DefaultTableModel();
  }

  public void propertyChange(PropertyChangeEvent evt) {
    String prop = evt.getPropertyName();
    if ("Enabled".equals(prop)) {
      /**/
    }
  }

  @Override
  public void tearDown() {
    removeListeners();

    for (CacheManagerInstance cacheManagerInstance : cacheManagerInstanceMap.values()) {
      cacheManagerInstance.tearDown();
    }

    super.tearDown();
  }
}

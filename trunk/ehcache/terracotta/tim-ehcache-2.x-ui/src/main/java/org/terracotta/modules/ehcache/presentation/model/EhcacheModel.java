/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation.model;

import org.terracotta.modules.ehcache.presentation.EhcacheStatsUtils;

import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.ObjectName;

public class EhcacheModel extends BaseMBeanModel implements PropertyChangeListener {
  private final ConcurrentMap<String, CacheManagerModel> cacheManagerMap;

  public static final Iterator                           EMPTY_ITERATOR = Collections.EMPTY_LIST.iterator();

  public EhcacheModel(IClusterModel clusterModel) {
    super(clusterModel);
    this.cacheManagerMap = new ConcurrentHashMap<String, CacheManagerModel>();
    clusterModel.addPropertyChangeListener(this);
  }

  public CacheManagerModel getCacheManagerModel(String cacheManagerName) {
    return cacheManagerMap.get(cacheManagerName);
  }

  public int getCacheManagerCount() {
    return cacheManagerMap.size();
  }

  public Iterator<CacheManagerModel> cacheManagers() {
    return cacheManagerMap.values().iterator();
  }

  public String[] getCacheManagerNames() {
    String[] result = cacheManagerMap.keySet().toArray(new String[cacheManagerMap.size()]);
    Arrays.sort(result);
    return result;
  }

  @Override
  public void init() {
    addListeners();

    onSet.addAll(getActiveCacheManagerBeans());
    for (ObjectName objectName : new HashSet<ObjectName>(onSet)) {
      testAddCacheManagerModel(objectName);
    }
  }

  public void propertyChange(PropertyChangeEvent evt) {
    String prop = evt.getPropertyName();
    if (IClusterModel.PROP_ACTIVE_COORDINATOR.equals(prop)) {
      IServer newActive = (IServer) evt.getNewValue();
      if (newActive != null) {
        suspend();
        init();
      }
    }
  }

  @Override
  public void suspend() {
    removeListeners();

    HashMap tmpMap = new HashMap(cacheManagerMap);
    cacheManagerMap.clear();
    for (Iterator<CacheManagerModel> iter = tmpMap.values().iterator(); iter.hasNext();) {
      CacheManagerModel cmm = iter.next();
      if (cmm != null) {
        fireCacheManagerRemoved(cmm);
        cmm.tearDown();
      }
    }
    tmpMap.clear();
  }

  @Override
  public void reset() {
    /**/
  }

  private Set<ObjectName> getActiveCacheManagerBeans() {
    return getActiveCacheManagerBeans(null);
  }

  private Set<ObjectName> getActiveCacheManagerBeans(String name) {
    IServer activeCoord = getActiveCoordinator();
    if (activeCoord != null) {
      try {
        StringBuilder sb = new StringBuilder(EhcacheStatsUtils.SAMPLED_CACHE_MANAGER_BEAN_NAME_PREFIX);
        if (name != null) {
          sb.append(",name=" + EhcacheStatsUtils.mbeanSafe(name));
        }
        sb.append(",*");
        return activeCoord.queryNames(new ObjectName(sb.toString()), null);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return Collections.emptySet();
  }

  public CacheManagerModel removeCacheManagerModel(String cacheManagerName) {
    return cacheManagerMap.remove(cacheManagerName);
  }

  private boolean testAddCacheManagerModel(ObjectName on) {
    onSet.add(on);
    if (cacheManagerMap != null) {
      String name = on.getKeyProperty("name");
      if (!cacheManagerMap.containsKey(name)) {
        CacheManagerModel cacheManagerModel = new CacheManagerModel(this, name);
        cacheManagerModel.startup();
        if (cacheManagerMap.putIfAbsent(name, cacheManagerModel) == null) {
          fireCacheManagerAdded(cacheManagerModel);
          return true;
        }
      }
    }
    return false;
  }

  private boolean testRemoveCacheManagerModel(ObjectName on) {
    onSet.remove(on);
    if (cacheManagerMap != null) {
      String name = on.getKeyProperty("name");
      CacheManagerModel cacheManagerModel;
      if ((cacheManagerModel = getCacheManagerModel(name)) != null && cacheManagerModel.isEmpty()) {
        if (cacheManagerMap.remove(name, cacheManagerModel)) {
          fireCacheManagerRemoved(cacheManagerModel);
          cacheManagerModel.tearDown();
          return true;
        }
      }
    }
    return false;
  }

  private boolean isCacheManagerBean(ObjectName on) {
    if (on == null) { return false; }
    return on.getKeyProperty("type").equals("SampledCacheManager");
  }

  @Override
  public void handleNotification(Notification notif, Object data) {
    String type = notif.getType();
    if (notif instanceof MBeanServerNotification) {
      final MBeanServerNotification mbsn = (MBeanServerNotification) notif;
      final ObjectName on = mbsn.getMBeanName();

      if (type.equals(MBeanServerNotification.REGISTRATION_NOTIFICATION)) {
        if (isCacheManagerBean(on)) {
          testAddCacheManagerModel(on);
        }
      } else if (type.equals(MBeanServerNotification.UNREGISTRATION_NOTIFICATION)) {
        if (isCacheManagerBean(on)) {
          testRemoveCacheManagerModel(on);
        }
      }
    }
  }

  public void addEhcacheModelListener(EhcacheModelListener listener) {
    listenerList.remove(EhcacheModelListener.class, listener);
    listenerList.add(EhcacheModelListener.class, listener);
  }

  public void removeEhcacheModelListener(EhcacheModelListener listener) {
    listenerList.remove(EhcacheModelListener.class, listener);
  }

  public void fireCacheManagerAdded(CacheManagerModel cacheManagerModel) {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == EhcacheModelListener.class) {
        ((EhcacheModelListener) listeners[i + 1]).cacheManagerModelAdded(cacheManagerModel);
      }
    }
  }

  public void fireCacheManagerRemoved(CacheManagerModel cacheManagerModel) {
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == EhcacheModelListener.class) {
        ((EhcacheModelListener) listeners[i + 1]).cacheManagerModelRemoved(cacheManagerModel);
      }
    }
  }

  @Override
  public void tearDown() {
    clusterModel.removePropertyChangeListener(this);
    reset();
    super.tearDown();
  }
}

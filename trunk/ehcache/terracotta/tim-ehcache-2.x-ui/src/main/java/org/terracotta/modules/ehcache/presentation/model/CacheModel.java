/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation.model;

import org.terracotta.modules.ehcache.presentation.EhcachePresentationUtils;
import org.terracotta.modules.ehcache.presentation.EhcacheStatsUtils;

import com.tc.admin.model.IClient;
import com.tc.admin.model.IServer;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.ObjectName;

public class CacheModel extends BaseMBeanModel implements Comparable {
  protected final CacheManagerModel cacheManagerModel;
  protected final String            cacheName;
  protected final String            shortName;

  public CacheModel(CacheManagerModel cacheManagerModel, String cacheName) {
    super(cacheManagerModel.getClusterModel());

    this.cacheManagerModel = cacheManagerModel;
    this.cacheName = cacheName;
    this.shortName = EhcachePresentationUtils.determineShortName(cacheName);
  }

  protected static Boolean booleanAttr(Map<String, Object> attrs, String name) {
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

  protected static Integer integerAttr(Map<String, Object> attrs, String name) {
    Integer result = null;
    try {
      result = (Integer) attrs.get(name);
    } catch (Exception e) {
      /**/
    } finally {
      if (result == null) {
        result = Integer.valueOf(0);
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

  protected static String stringAttr(Map<String, Object> attrs, String name) {
    String result = null;
    try {
      result = (String) attrs.get(name);
    } catch (Exception e) {
      /**/
    } finally {
      if (result == null) {
        result = "";
      }
    }
    return result;
  }

  public synchronized CacheManagerModel getCacheManagerModel() {
    return cacheManagerModel;
  }

  public Set<CacheModelInstance> cacheModelInstances() {
    return cacheManagerModel.cacheModelInstances(this);
  }

  public String getCacheName() {
    return cacheName;
  }

  public String getShortName() {
    return shortName;
  }

  public int getInstanceCount() {
    return onSet != null ? onSet.size() : 0;
  }

  protected void _setAttribute(String attribute, Object value) {
    ObjectName target;
    while ((target = getRandomBean()) != null) {
      safeSetAttribute(target, attribute, value);
      return;
    }
  }

  public CacheStatisticsModel getCacheStatistics(IClient client) {
    IServer activeCoord = getActiveCoordinator();
    if (activeCoord == null) { return null; }

    CacheStatisticsModel result = null;
    try {
      Map<ObjectName, Set<String>> request = new HashMap<ObjectName, Set<String>>();
      ObjectName templateName = EhcacheStatsUtils.getSampledCacheBeanName(cacheManagerModel.getName(), cacheName);
      templateName = client.getTunneledBeanName(templateName);
      request.put(templateName, new HashSet(Arrays.asList(CacheStatisticsModel.MBEAN_ATTRS)));
      Map<ObjectName, Map<String, Object>> resp = activeCoord
          .getAttributeMap(request, Long.MAX_VALUE, TimeUnit.SECONDS);
      Map<String, Object> attrMap = resp.get(templateName);
      if (attrMap != null && !attrMap.isEmpty()) {
        result = new CacheStatisticsModel(attrMap);
      }
    } catch (Exception e) {
      /* connection probably dropped */
    }
    return result;
  }

  public CacheStatisticsModel getAggregateCacheStatistics() {
    CacheStatisticsModel result = new CacheStatisticsModel(cacheName);
    Set<String> attrSet = new HashSet(Arrays.asList(CacheStatisticsModel.MBEAN_ATTRS));
    float avgGetTimeMillis = 0f;
    float minGetTimeMillis = 0f;
    float maxGetTimeMillis = 0f;
    int instances = 0;
    Map<ObjectName, Map<String, Object>> resp = getAttributes(attrSet);
    for (Entry<ObjectName, Map<String, Object>> entry : resp.entrySet()) {
      Map<String, Object> attrMap = entry.getValue();
      if (attrMap != null) {
        CacheStatisticsModel csm = new CacheStatisticsModel(attrMap);
        result.add(csm);
        avgGetTimeMillis += csm.getAverageGetTimeMillis();
        minGetTimeMillis += csm.getMinGetTimeMillis();
        maxGetTimeMillis += csm.getMaxGetTimeMillis();
        instances++;
      }
    }
    if (instances > 0) {
      result.setAverageGetTime(avgGetTimeMillis / instances);
      result.setMinGetTime(minGetTimeMillis / instances);
      result.setMaxGetTime(maxGetTimeMillis / instances);
    }
    return result;
  }

  /**
   * Removes all entries from this cache cluster-wide.
   */
  public void removeAll() {
    invokeAll("removeAll");
  }

  /**
   * Enables/disables the cache cluster-wide.
   */
  public void setEnabled(boolean enabled) {
    safeSetAttribute("Enabled", Boolean.valueOf(enabled));
  }

  /**
   * Enable/disable the cache bulk-load mode cluster-wide.
   */
  public void setBulkLoadEnabled(boolean bulkLoadEnabled) {
    safeSetAttribute("NodeBulkLoadEnabled", Boolean.valueOf(bulkLoadEnabled));
  }

  /**
   * Sets the cache statistics cluster-wide.
   */
  public void setStatisticsEnabled(boolean enabled) {
    safeSetAttribute("StatisticsEnabled", Boolean.valueOf(enabled));
  }

  @Override
  public void init() {
    addListeners();
    onSet.addAll(getActiveCacheModelBeans());
  }

  protected Set<ObjectName> getActiveCacheModelBeans() {
    IServer activeCoord = getActiveCoordinator();
    if (activeCoord != null) {
      try {
        StringBuilder sb = new StringBuilder(EhcacheStatsUtils.SAMPLED_CACHE_BEAN_NAME_PREFIX)
            .append(",SampledCacheManager=").append(EhcacheStatsUtils.mbeanSafe(cacheManagerModel.getName()))
            .append(",name=").append(cacheName).append(",*");
        return activeCoord.queryNames(new ObjectName(sb.toString()), null);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return Collections.emptySet();
  }

  @Override
  public void suspend() {
    /**/
  }

  @Override
  public void handleNotification(Notification notif, Object data) {
    String type = notif.getType();
    if (notif instanceof MBeanServerNotification) {
      final MBeanServerNotification mbsn = (MBeanServerNotification) notif;
      final ObjectName on = mbsn.getMBeanName();

      if (type.equals(MBeanServerNotification.UNREGISTRATION_NOTIFICATION)) {
        if (cacheManagerModel.isCacheInstance(on) && on.getKeyProperty("name").equals(cacheName)) {
          onSet.remove(on);
          if (onSet.isEmpty()) {
            CacheModel cacheModel = cacheManagerModel.removeCacheModel(cacheName);
            if (cacheModel != null) {
              cacheModel.tearDown();
            }
          }
        }
      } else if (type.equals(MBeanServerNotification.REGISTRATION_NOTIFICATION)) {
        if (cacheManagerModel.isCacheInstance(on) && on.getKeyProperty("name").equals(cacheName)) {
          onSet.add(on);
        }
      }
    }
  }

  public int getStatisticsEnabledCount() {
    return cacheManagerModel.getStatisticsEnabledCount(this);
  }

  public int getBulkLoadEnabledCount() {
    return cacheManagerModel.getBulkLoadEnabledCount(this);
  }

  public int getBulkLoadDisabledCount() {
    return cacheManagerModel.getBulkLoadDisabledCount(this);
  }

  public int getEnabledCount() {
    return cacheManagerModel.getEnabledCount(this);
  }

  public int getTransactionalCount() {
    return cacheManagerModel.getTransactionalCount(this);
  }

  public int getTerracottaClusteredInstanceCount() {
    return cacheManagerModel.getTerracottaClusteredInstanceCount(this);
  }

  public String generateActiveConfigDeclaration() {
    return cacheManagerModel.generateActiveConfigDeclaration(getCacheName());
  }

  // PropertyChangeSupport

  protected final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

  public void addPropertyChangeListener(PropertyChangeListener listener) {
    if (listener != null) {
      propertyChangeSupport.removePropertyChangeListener(listener);
      propertyChangeSupport.addPropertyChangeListener(listener);
    }
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
    if (listener != null) {
      propertyChangeSupport.removePropertyChangeListener(listener);
    }
  }

  public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
    propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
  }

  public int compareTo(Object o) {
    CacheModel other = (CacheModel) o;
    return getCacheName().compareTo(other.getCacheName());
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((cacheName == null) ? 0 : cacheName.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    CacheModel other = (CacheModel) obj;
    if (cacheName == null) {
      if (other.cacheName != null) return false;
    } else if (!cacheName.equals(other.cacheName)) return false;
    return true;
  }

  /**
   * Of course, this is wrong as it just grabs the first instance, which is fine if all the instances are
   * TerracottaClustered, otherwise we're grabbing a random, non-Clustered instance.
   */
  public int getSize() {
    for (CacheModelInstance cmi : cacheModelInstances()) {
      return cmi.getSize();
    }
    return 0;
  }

  @Override
  public String toString() {
    return cacheName;
  }

  @Override
  public void tearDown() {
    removeListeners();
    super.tearDown();
  }
}

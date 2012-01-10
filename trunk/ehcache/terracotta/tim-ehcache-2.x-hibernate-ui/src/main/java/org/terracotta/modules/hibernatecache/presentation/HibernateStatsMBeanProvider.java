/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.hibernatecache.presentation;

import net.sf.ehcache.hibernate.management.api.EhcacheHibernateMBean;

import org.terracotta.modules.hibernatecache.jmx.HibernateStatsUtils;

import com.tc.admin.AbstractClusterListener;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.openmbean.TabularData;

public class HibernateStatsMBeanProvider implements NotificationListener {
  private final IClusterModel                clusterModel;
  private final ObjectName                   templateName;
  private EhcacheHibernateMBean              currBean;
  private ObjectName                         currBeanName;
  private final NotificationListener         unregisterNotificationListener;

  private static final EhcacheHibernateMBean nullBean = new NullHibernateStatsMBean();

  public HibernateStatsMBeanProvider(final IClusterModel clusterModel, final String persistenceUnit) {
    this.clusterModel = clusterModel;
    this.currBean = nullBean;
    this.unregisterNotificationListener = new UnRegistrationNotificationListener();

    try {
      templateName = new ObjectName(HibernateStatsUtils.getHibernateStatsBeanName(persistenceUnit).getCanonicalName()
                                    + ",*");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    clusterModel.addPropertyChangeListener(new ClusterListener(clusterModel));
    if (clusterModel.isReady()) {
      init();
    }
  }

  private class ClusterListener extends AbstractClusterListener {
    private ClusterListener(IClusterModel clusterModel) {
      super(clusterModel);
    }

    @Override
    protected void handleReady() {
      IClusterModel theClusterModel = getClusterModel();
      if (theClusterModel == null) { return; }

      if (theClusterModel.isReady()) {
        init();
      }
    }
  }

  private void init() {
    addMBeanServerDelegateListener();
  }

  private void addMBeanServerDelegateListener() {
    IServer activeCoord = clusterModel.getActiveCoordinator();
    try {
      ObjectName on = new ObjectName("JMImplementation:type=MBeanServerDelegate");
      activeCoord.addNotificationListener(on, unregisterNotificationListener);
    } catch (Exception e) {
      /**/
    }
  }

  private synchronized void setCurrentBeanName(ObjectName on) {
    currBeanName = on;
  }

  private synchronized ObjectName getCurrentBeanName() {
    return currBeanName;
  }

  private class UnRegistrationNotificationListener implements NotificationListener {
    public void handleNotification(Notification notif, Object data) {
      String type = notif.getType();
      if (notif instanceof MBeanServerNotification) {
        final MBeanServerNotification mbsn = (MBeanServerNotification) notif;
        final ObjectName leaving = mbsn.getMBeanName();
        final ObjectName targetBeanName = getCurrentBeanName();

        if (type.equals(MBeanServerNotification.UNREGISTRATION_NOTIFICATION) && leaving.equals(targetBeanName)) {
          setCurrentBeanName(null);
          currBean = null;
        }
      }
    }
  }

  public EhcacheHibernateMBean getBean() {
    if (currBean != nullBean) { return currBean; }

    if (clusterModel.isReady()) {
      IServer activeCoord = clusterModel.getActiveCoordinator();
      try {
        Set<ObjectName> onSet = activeCoord.queryNames(templateName, null);
        Iterator<ObjectName> onIter = onSet.iterator();
        while (onIter.hasNext()) {
          ObjectName on = onIter.next();
          EhcacheHibernateMBean statsBean = activeCoord.getMBeanProxy(on, EhcacheHibernateMBean.class);
          try {
            activeCoord.addNotificationListener(on, this);
          } catch (Exception e) {
            continue;
          }
          setCurrentBeanName(on);
          return currBean = statsBean;
        }
      } catch (IOException ioe) {
        /**/
      }
    }
    return currBean = nullBean;
  }

  private static class NullHibernateStatsMBean implements EhcacheHibernateMBean {
    public void clearStats() {
      /**/
    }

    public void flushRegionCache(final String region) {
      /**/
    }

    public long getCacheHitCount() {
      return 0;
    }

    public long getCacheHitSample() {
      return 0;
    }

    public double getCacheHitRate() {
      return 0;
    }

    public long getCacheMissCount() {
      return 0;
    }

    public long getCacheMissSample() {
      return 0;
    }

    public double getCacheMissRate() {
      return 0;
    }

    public long getCachePutCount() {
      return 0;
    }

    public long getCachePutSample() {
      return 0;
    }

    public double getCachePutRate() {
      return 0;
    }

    public TabularData getCacheRegionStats() {
      return null;
    }

    public long getCloseStatementCount() {
      return 0;
    }

    public TabularData getCollectionStats() {
      return null;
    }

    public long getConnectCount() {
      return 0;
    }

    public TabularData getEntityStats() {
      return null;
    }

    public long getFlushCount() {
      return 0;
    }

    public long getOptimisticFailureCount() {
      return 0;
    }

    public long getPrepareStatementCount() {
      return 0;
    }

    public long getQueryExecutionCount() {
      return 0;
    }

    public long getQueryExecutionSample() {
      return 0;
    }

    public double getQueryExecutionRate() {
      return 0;
    }

    public TabularData getQueryStats() {
      return null;
    }

    public String getOriginalConfigDeclaration() {
      return null;
    }

    public String getOriginalConfigDeclaration(final String region) {
      return null;
    }

    public int getRegionCacheMaxTTISeconds(final String region) {
      return 0;
    }

    public int getRegionCacheMaxTTLSeconds(final String region) {
      return 0;
    }

    public int getRegionCacheOrphanEvictionPeriod(final String region) {
      return 0;
    }

    public long getSessionCloseCount() {
      return 0;
    }

    public long getSessionOpenCount() {
      return 0;
    }

    public long getSuccessfulTransactionCount() {
      return 0;
    }

    public long getTransactionCount() {
      return 0;
    }

    public boolean isRegionCacheEnabled(final String region) {
      return false;
    }

    public boolean isRegionCacheLoggingEnabled(final String region) {
      return false;
    }

    public boolean isRegionCacheOrphanEvictionEnabled(final String region) {
      return false;
    }

    public boolean isStatisticsEnabled() {
      return false;
    }

    public void setRegionCacheEnabled(final String region, final boolean flag) {
      /**/
    }

    public void setRegionCacheLoggingEnabled(final String region, final boolean loggingEnabled) {
      /**/
    }

    public void setRegionCacheMaxTTISeconds(final String region, final int maxTTISeconds) {
      /**/
    }

    public void setRegionCacheMaxTTLSeconds(final String region, final int maxTTLSeconds) {
      /**/
    }

    public void setStatisticsEnabled(final boolean flag) {
      /**/
    }

    public String[] getTerracottaHibernateCacheRegionNames() {
      return null;
    }

    public boolean isTerracottaHibernateCache(final String region) {
      return false;
    }

    public Map getRegionCacheAttributes(final String regionName) {
      return Collections.emptyMap();
    }

    public Map getRegionCacheAttributes() {
      return Collections.emptyMap();
    }

    public String generateActiveConfigDeclaration(final String region) {
      System.err.println("NullHibernateStatsMBean.generateActiveConfigDeclaration region=" + region);
      return "NullHibernateStatsMBean";
    }

    public String generateActiveConfigDeclaration() {
      System.err.println("NullHibernateStatsMBean.generateActiveConfigDeclaration");
      return "NullHibernateStatsMBean";
    }

    public void flushRegionCaches() {
      /**/
    }

    public boolean isRegionCachesEnabled() {
      return false;
    }

    public int getRegionCacheTargetMaxTotalCount(String region) {
      return 0;
    }

    public int getRegionCacheTargetMaxInMemoryCount(String region) {
      return 0;
    }

    public void setRegionCacheTargetMaxTotalCount(String region, int maxGlobalEntries) {
      /**/
    }

    public void setRegionCacheTargetMaxInMemoryCount(String region, int maxLocalEntries) {
      /**/
    }

    public Map<String, int[]> getRegionCacheSamples() {
      return null;
    }

    public void disableStats() {
      /**/
    }

    public void enableStats() {
      /**/
    }

    public boolean isHibernateStatisticsSupported() {
      return false;
    }

    public float getAverageGetTimeMillis(String region) {
      return 0;
    }

    public long getMaxGetTimeMillis() {
      return 0;
    }

    public long getMaxGetTimeMillis(String cacheName) {
      return 0;
    }

    public long getMinGetTimeMillis() {
      return 0;
    }

    public long getMinGetTimeMillis(String cacheName) {
      return 0;
    }

    public int getNumberOfElementsInMemory(String region) {
      return 0;
    }

    public int getNumberOfElementsOffHeap(String region) {
      return 0;
    }

    public int getNumberOfElementsOnDisk(String region) {
      return 0;
    }

    public void setRegionCachesEnabled(boolean enabled) {
      /**/
    }

    public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
      /**/
    }

    public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback)
        throws IllegalArgumentException {
      /**/
    }

    public MBeanNotificationInfo[] getNotificationInfo() {
      return null;
    }

    public void removeNotificationListener(NotificationListener listener) {
      /**/
    }
  }

  private final NotificationBroadcasterSupport broadcaster = new NotificationBroadcasterSupport();

  public void addNotificationListener(NotificationListener listener) {
    removeNotificationListener(listener);
    getBean(); // this smells: force the local notification listener to be added to bean.
    broadcaster.addNotificationListener(listener, null, null);
  }

  public void removeNotificationListener(NotificationListener listener) {
    try {
      broadcaster.removeNotificationListener(listener);
    } catch (ListenerNotFoundException lnfe) {
      /**/
    }
  }

  public void handleNotification(Notification notif, Object data) {
    broadcaster.sendNotification(notif);
  }
}

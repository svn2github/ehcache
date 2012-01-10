/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.hibernatecache.presentation;

import net.sf.ehcache.hibernate.management.api.EhcacheHibernateMBean;

import org.terracotta.modules.hibernatecache.jmx.CacheRegionStats;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.model.IClient;
import com.tc.admin.model.IClusterModel;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

public class ClientH2LCRuntimeStatsPanel extends BaseH2LCRuntimeStatsPanel {
  protected IClient client;

  public ClientH2LCRuntimeStatsPanel(ApplicationContext appContext, IClusterModel clusterModel, IClient client,
                                     String persistenceUnit) {
    super(appContext, clusterModel, persistenceUnit);
    this.client = client;
  }

  private class ClientRegionStatsWorker extends RegionStatsWorker {
    private ClientRegionStatsWorker() {
      super(new Callable<Map<String, ? extends CacheRegionStats>>() {
        public Map<String, ? extends CacheRegionStats> call() throws Exception {
          Map<String, CacheRegionStats> result = new HashMap<String, CacheRegionStats>();
          ObjectName on = client.getTunneledBeanName(statsBeanObjectName);
          EhcacheHibernateMBean statsBean = clusterModel.getActiveCoordinator()
              .getMBeanProxy(on, EhcacheHibernateMBean.class);
          TabularData td = statsBean.getCacheRegionStats();
          if (td != null) {
            Iterator iter = td.values().iterator();
            while (iter.hasNext()) {
              CompositeData cData = (CompositeData) iter.next();
              String region = (String) cData.get("region");
              if (!region.endsWith("org.hibernate.cache.UpdateTimestampsCache")) {
                CacheRegionStats cacheRegionStats = new CacheRegionStats(cData);
                result.put(cacheRegionStats.getRegion(), cacheRegionStats);
              }
            }
          }
          return result;
        }
      });
    }
  }

  @Override
  protected RegionStatsWorker createRegionStatsWorker() {
    return new ClientRegionStatsWorker();
  }
}

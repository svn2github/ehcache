/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.hibernatecache.presentation;

import org.terracotta.modules.hibernatecache.jmx.AggregateCacheRegionStats;
import org.terracotta.modules.hibernatecache.jmx.CacheRegionStats;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.XTable.BaseRenderer;
import com.tc.admin.model.IClient;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.swing.JComponent;
import javax.swing.SwingConstants;

public class AggregateH2LCRuntimeStatsPanel extends BaseH2LCRuntimeStatsPanel {
  public AggregateH2LCRuntimeStatsPanel(ApplicationContext appContext, IClusterModel clusterModel,
                                        String persistenceUnit) {
    super(appContext, clusterModel, persistenceUnit);
  }

  @Override
  protected JComponent createRegionTablePanel() {
    JComponent comp = super.createRegionTablePanel();
    regionTable.getColumnModel().getColumn(5).setCellRenderer(new DashRenderer());
    return comp;
  }

  private class DashRenderer extends BaseRenderer {
    @Override
    public void setValue(Object value) {
      label.setEnabled(false);
      label.setText("---");
      label.setHorizontalAlignment(SwingConstants.CENTER);
    }
  }

  private class AggregateRegionStatsWorker extends RegionStatsWorker {
    private AggregateRegionStatsWorker() {
      super(new Callable<Map<String, ? extends CacheRegionStats>>() {
        public Map<String, ? extends CacheRegionStats> call() throws Exception {
          Map<ObjectName, Set<String>> reqMap = new HashMap<ObjectName, Set<String>>();
          Set<String> attrSet = Collections.singleton(CACHE_REGION_STATS);
          Map<String, AggregateCacheRegionStats> result = new HashMap<String, AggregateCacheRegionStats>();
          IServer activeCoord = clusterModel.getActiveCoordinator();

          for (IClient client : clusterModel.getClients()) {
            ObjectName on = client.getTunneledBeanName(statsBeanObjectName);
            reqMap.put(on, attrSet);
          }
          Map<ObjectName, Map<String, Object>> resultMap = activeCoord.getAttributeMap(reqMap, Long.MAX_VALUE,
                                                                                       TimeUnit.SECONDS);
          if (resultMap != null) {
            Iterator<Map<String, Object>> valuesIter = resultMap.values().iterator();
            while (valuesIter.hasNext()) {
              Map<String, Object> valueMap = valuesIter.next();
              TabularData td = (TabularData) valueMap.get(CACHE_REGION_STATS);
              if (td != null) {
                Iterator iter = td.values().iterator();
                while (iter.hasNext()) {
                  CompositeData cData = (CompositeData) iter.next();
                  String region = (String) cData.get("region");
                  if (!region.endsWith("org.hibernate.cache.UpdateTimestampsCache")) {
                    CacheRegionStats cacheRegionStats = new CacheRegionStats(cData);
                    AggregateCacheRegionStats aggregateCacheRegionStats = result.get(region);
                    if (aggregateCacheRegionStats == null) {
                      result.put(region, aggregateCacheRegionStats = new AggregateCacheRegionStats(region));
                    }
                    aggregateCacheRegionStats.aggregate(cacheRegionStats);
                  }
                }
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
    return new AggregateRegionStatsWorker();
  }
}

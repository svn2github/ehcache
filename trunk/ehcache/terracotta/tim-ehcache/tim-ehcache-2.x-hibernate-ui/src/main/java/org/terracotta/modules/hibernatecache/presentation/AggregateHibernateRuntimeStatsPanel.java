/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.hibernatecache.presentation;

import org.terracotta.modules.hibernatecache.jmx.CollectionStats;
import org.terracotta.modules.hibernatecache.jmx.EntityStats;

import com.tc.admin.common.ApplicationContext;
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

public class AggregateHibernateRuntimeStatsPanel extends BaseHibernateRuntimeStatsPanel {
  public AggregateHibernateRuntimeStatsPanel(ApplicationContext appContext, IClusterModel clusterModel,
                                             String persistenceUnit) {
    super(appContext, clusterModel, persistenceUnit);
  }

  private class AggregateEntityStatsWorker extends EntityStatsWorker {
    private AggregateEntityStatsWorker() {
      super(new Callable<Map<String, EntityStats>>() {
        public Map<String, EntityStats> call() throws Exception {
          Map<ObjectName, Set<String>> reqMap = new HashMap<ObjectName, Set<String>>();
          Set<String> attrSet = Collections.singleton(ENTITY_STATS);
          Map<String, EntityStats> result = new HashMap<String, EntityStats>();
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
              TabularData td = (TabularData) valueMap.get(ENTITY_STATS);
              // null check since data can be null if the connection with the JMX server was severed in the meantime
              if (td != null) {
                Iterator iter = td.values().iterator();
                while (iter.hasNext()) {
                  EntityStats entityStats = new EntityStats((CompositeData) iter.next());
                  String name = entityStats.getName();
                  EntityStats aggregateEntityStats = result.get(name);
                  if (aggregateEntityStats == null) {
                    result.put(name, aggregateEntityStats = new EntityStats(name));
                  }
                  aggregateEntityStats.add(entityStats);
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
  protected EntityStatsWorker createEntityStatsWorker() {
    return new AggregateEntityStatsWorker();
  }

  private class AggregateCollectionStatsWorker extends CollectionStatsWorker {
    private AggregateCollectionStatsWorker() {
      super(new Callable<Map<String, CollectionStats>>() {
        public Map<String, CollectionStats> call() throws Exception {
          Map<ObjectName, Set<String>> reqMap = new HashMap<ObjectName, Set<String>>();
          Set<String> attrSet = Collections.singleton(COLLECTION_STATS);
          Map<String, CollectionStats> result = new HashMap<String, CollectionStats>();
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
              TabularData td = (TabularData) valueMap.get(COLLECTION_STATS);
              if (td != null) {
                Iterator iter = td.values().iterator();
                while (iter.hasNext()) {
                  CollectionStats collectionStats = new CollectionStats((CompositeData) iter.next());
                  String roleName = collectionStats.getRoleName();
                  CollectionStats aggregateCollectionStats = result.get(roleName);
                  if (aggregateCollectionStats == null) {
                    result.put(roleName, aggregateCollectionStats = new CollectionStats(roleName));
                  }
                  aggregateCollectionStats.add(collectionStats);
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
  protected CollectionStatsWorker createCollectionStatsWorker() {
    return new AggregateCollectionStatsWorker();
  }

  private class AggregateQueryStatsWorker extends QueryStatsWorker {
    private AggregateQueryStatsWorker() {
      super(new Callable<Map<String, QueryStats>>() {
        public Map<String, QueryStats> call() throws Exception {
          Map<ObjectName, Set<String>> reqMap = new HashMap<ObjectName, Set<String>>();
          Set<String> attrSet = Collections.singleton(QUERY_STATS);
          Map<String, QueryStats> result = new HashMap<String, QueryStats>();
          IServer activeCoord = clusterModel.getActiveCoordinator();

          for (IClient client : clusterModel.getClients()) {
            ObjectName on = client.getTunneledBeanName(statsBeanObjectName);
            reqMap.put(on, attrSet);
          }
          Map<ObjectName, Map<String, Object>> resultMap = activeCoord.getAttributeMap(reqMap, Long.MAX_VALUE,
                                                                                       TimeUnit.SECONDS);
          Iterator<Map<String, Object>> valuesIter = resultMap.values().iterator();
          while (valuesIter.hasNext()) {
            Map<String, Object> valueMap = valuesIter.next();
            TabularData td = (TabularData) valueMap.get(QUERY_STATS);
            if (td != null) {
              Iterator iter = td.values().iterator();
              while (iter.hasNext()) {
                QueryStats queryStats = new QueryStats((CompositeData) iter.next());
                String query = queryStats.getQuery();
                QueryStats aggregateQueryStats = result.get(query);
                if (aggregateQueryStats == null) {
                  result.put(query, aggregateQueryStats = new QueryStats(query));
                }
                aggregateQueryStats.add(queryStats);
              }
            }
          }
          return result;
        }
      });
    }
  }

  @Override
  protected QueryStatsWorker createQueryStatsWorker() {
    return new AggregateQueryStatsWorker();
  }
}

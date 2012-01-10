/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.hibernatecache.presentation;

import net.sf.ehcache.hibernate.management.api.EhcacheHibernateMBean;

import org.terracotta.modules.hibernatecache.jmx.CollectionStats;
import org.terracotta.modules.hibernatecache.jmx.EntityStats;

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

public class ClientHibernateRuntimeStatsPanel extends BaseHibernateRuntimeStatsPanel {
  protected final IClient client;

  public ClientHibernateRuntimeStatsPanel(ApplicationContext appContext, IClusterModel clusterModel, IClient client,
                                          String persistenceUnit) {
    super(appContext, clusterModel, persistenceUnit);
    this.client = client;
  }

  private class ClientEntityStatsWorker extends EntityStatsWorker {
    private ClientEntityStatsWorker() {
      super(new Callable<Map<String, EntityStats>>() {
        public Map<String, EntityStats> call() throws Exception {
          Map<String, EntityStats> result = new HashMap<String, EntityStats>();
          ObjectName on = client.getTunneledBeanName(statsBeanObjectName);
          EhcacheHibernateMBean statsBean = clusterModel.getActiveCoordinator()
              .getMBeanProxy(on, EhcacheHibernateMBean.class);
          TabularData td = statsBean.getEntityStats();
          if (td != null) {
            Iterator iter = td.values().iterator();
            while (iter.hasNext()) {
              EntityStats entityStats = new EntityStats((CompositeData) iter.next());
              result.put(entityStats.getName(), entityStats);
            }
          }
          return result;
        }
      });
    }
  }

  @Override
  protected EntityStatsWorker createEntityStatsWorker() {
    return new ClientEntityStatsWorker();
  }

  private class ClientCollectionStatsWorker extends CollectionStatsWorker {
    private ClientCollectionStatsWorker() {
      super(new Callable<Map<String, CollectionStats>>() {
        public Map<String, CollectionStats> call() throws Exception {
          Map<String, CollectionStats> result = new HashMap<String, CollectionStats>();
          ObjectName on = client.getTunneledBeanName(statsBeanObjectName);
          EhcacheHibernateMBean statsBean = clusterModel.getActiveCoordinator()
              .getMBeanProxy(on, EhcacheHibernateMBean.class);
          TabularData td = statsBean.getCollectionStats();
          if (td != null) {
            Iterator iter = td.values().iterator();
            while (iter.hasNext()) {
              CollectionStats collectionStats = new CollectionStats((CompositeData) iter.next());
              result.put(collectionStats.getRoleName(), collectionStats);
            }
          }
          return result;
        }
      });
    }
  }

  @Override
  protected CollectionStatsWorker createCollectionStatsWorker() {
    return new ClientCollectionStatsWorker();
  }

  private class ClientQueryStatsWorker extends QueryStatsWorker {
    private ClientQueryStatsWorker() {
      super(new Callable<Map<String, QueryStats>>() {
        public Map<String, QueryStats> call() throws Exception {
          Map<String, QueryStats> result = new HashMap<String, QueryStats>();
          ObjectName on = client.getTunneledBeanName(statsBeanObjectName);
          EhcacheHibernateMBean statsBean = clusterModel.getActiveCoordinator()
              .getMBeanProxy(on, EhcacheHibernateMBean.class);
          TabularData td = statsBean.getQueryStats();
          if (td != null) {
            Iterator iter = td.values().iterator();
            while (iter.hasNext()) {
              QueryStats queryStats = new QueryStats((CompositeData) iter.next());
              result.put(queryStats.getQuery(), queryStats);
            }
          }
          return result;
        }
      });
    }
  }

  @Override
  protected QueryStatsWorker createQueryStatsWorker() {
    return new ClientQueryStatsWorker();
  }
}

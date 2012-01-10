/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.hibernatecache.presentation;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.model.IClient;
import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.PolledAttribute;
import com.tc.admin.model.PolledAttributesResult;

import javax.management.ObjectName;
import javax.swing.SwingUtilities;

public class AggregateH2LCStatsChartPanel extends BaseH2LCStatsChartPanel {
  public AggregateH2LCStatsChartPanel(ApplicationContext appContext, IClusterModel clusterModel, String persistenceUnit) {
    super(appContext, clusterModel, persistenceUnit);
  }

  @Override
  public void attributesPolled(PolledAttributesResult result) {
    long executionCount = 0;
    long hitCount = 0;
    long putCount = 0;
    long missCount = 0;

    for (IClient client : clusterModel.getClients()) {
      ObjectName on = client.getTunneledBeanName(statsBeanObjectName);
      Number n;

      n = (Number) result.getPolledAttribute(client, on, DBSQL_EXECUTION_SAMPLE_ATTR);
      if (n != null) {
        executionCount += n.longValue();
      }

      n = (Number) result.getPolledAttribute(client, on, CACHE_HIT_SAMPLE_ATTR);
      if (n != null) {
        hitCount += n.longValue();
      }

      n = (Number) result.getPolledAttribute(client, on, CACHE_PUT_SAMPLE_ATTR);
      if (n != null) {
        putCount += n.longValue();
      }

      n = (Number) result.getPolledAttribute(client, on, CACHE_MISS_SAMPLE_ATTR);
      if (n != null) {
        missCount += n.longValue();
      }
    }

    final long theExecutionCount = executionCount;
    final long theHitCount = hitCount;
    final long thePutCount = putCount;
    final long theMissCount = missCount;
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        updateAllSeries(theExecutionCount, theHitCount, thePutCount, theMissCount);
      }
    });
  }

  @Override
  public void clientConnected(IClient client) {
    registerClientPolledAttributes(client);
  }

  private void registerClientPolledAttributes(IClient client) {
    ObjectName on = client.getTunneledBeanName(statsBeanObjectName);
    for (String attr : POLLED_ATTRS) {
      PolledAttribute pa = new PolledAttribute(on, attr);
      client.addPolledAttributeListener(pa, this);
    }
  }

  @Override
  public void clientDisconnected(IClient client) {
    ObjectName on = client.getTunneledBeanName(statsBeanObjectName);
    for (String attr : POLLED_ATTRS) {
      PolledAttribute pa = new PolledAttribute(on, attr);
      client.removePolledAttributeListener(pa, this);
    }
  }

  @Override
  protected void addPolledAttributeListener() {
    for (IClient client : clusterModel.getClients()) {
      registerClientPolledAttributes(client);
    }
  }

  @Override
  protected void removePolledAttributeListener() {
    /**/
  }
}

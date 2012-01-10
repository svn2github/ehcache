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

public class ClientH2LCStatsChartPanel extends BaseH2LCStatsChartPanel {
  protected final IClient client;

  public ClientH2LCStatsChartPanel(ApplicationContext appContext, IClusterModel clusterModel, IClient client,
                                   String persistenceUnit) {
    super(appContext, clusterModel, persistenceUnit);
    this.client = client;
  }

  @Override
  public void attributesPolled(PolledAttributesResult result) {
    long executionCount = 0;
    long hitCount = 0;
    long putCount = 0;
    long missCount = 0;

    ObjectName on = client.getTunneledBeanName(statsBeanObjectName);

    Number n = (Number) result.getPolledAttribute(client, on, DBSQL_EXECUTION_SAMPLE_ATTR);
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

  // TODO: add PollScope.ALL_CLIENTS

  @Override
  protected void addPolledAttributeListener() {
    try {
      ObjectName on = client.getTunneledBeanName(statsBeanObjectName);
      for (String attr : POLLED_ATTRS) {
        PolledAttribute pa = new PolledAttribute(on, attr);
        client.addPolledAttributeListener(pa, this);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void removePolledAttributeListener() {
    /**/
  }
}

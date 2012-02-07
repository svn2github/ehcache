/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.cluster;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.terracotta.AbstractTerracottaActivePassiveTestBase;

import org.terracotta.api.ClusteringToolkit;
import org.terracotta.ehcache.tests.ClientBase;

import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.test.config.model.TestConfig;

public class FailoverDuringPassiveSyncTest extends AbstractTerracottaActivePassiveTestBase {

  public FailoverDuringPassiveSyncTest(TestConfig testConfig) {
    super("failover-during-passive-sync-test.xml", testConfig, FailoverDuringPassiveSyncTestApp.class);
    testConfig.getGroupConfig().setMemberCount(3);

    testConfig.addTcProperty("seda." + ServerConfigurationContext.OBJECTS_SYNC_STAGE + ".sleepMs", "2000");
  }

  public static class FailoverDuringPassiveSyncTestApp extends ClientBase {

    public FailoverDuringPassiveSyncTestApp(String[] args) {
      super(args);
    }

    public static void main(String[] args) {
      new FailoverDuringPassiveSyncTestApp(args).run();
    }

    @Override
    protected void runTest(Cache cache, ClusteringToolkit clusteringToolkit) throws Throwable {
      getTestControlMbean().crashAllPassiveServers(0);
      for (int i = 0; i < 20000; i++) {
        cache.put(new Element("key-" + i, new byte[1024]));
      }

      info("Starting up the first passive.");
      getTestControlMbean().restartCrashedServer(0, 1);

      info("Waiting until the passive is synced up.");
      getTestControlMbean().waitUntilPassiveStandBy(0);

      info("Starting up the second passive.");
      getTestControlMbean().restartCrashedServer(0, 2);

      info("Sleeping for a short time to wait for the passive syncup to start.");
      Thread.sleep(15 * 1000);

      info("Killing the active so passive[1] can take over.");
      getTestControlMbean().crashActiveAndWaitForPassiveToTakeOver(0);

      info("Waiting for passive[2] to fully sync up.");
      getTestControlMbean().waitUntilPassiveStandBy(0);

      info("Stopping passive[1].");
      getTestControlMbean().crashActiveServer(0);

      for (int i = 0; i < 20000; i++) {
        assertNotNull(cache.get("key-" + i));
      }
    }

    private void info(String msg) {
      System.out.println(msg);
    }

  }

}

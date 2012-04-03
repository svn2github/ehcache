/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.cluster;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.terracotta.AbstractTerracottaActivePassiveTestBase;

import org.junit.Assert;
import org.terracotta.api.ClusteringToolkit;
import org.terracotta.ehcache.tests.ClientBase;

import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.test.config.model.TestConfig;
import com.tc.util.concurrent.ThreadUtil;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class FailoverToOutOfSyncPassivesTest extends AbstractTerracottaActivePassiveTestBase {

  public FailoverToOutOfSyncPassivesTest(TestConfig testConfig) {
    super("failover-during-passive-sync-test.xml", testConfig, FailoverToOutOfSyncPassivesTestApp.class);
    testConfig.getGroupConfig().setMemberCount(3);

    testConfig.addTcProperty("seda." + ServerConfigurationContext.OBJECTS_SYNC_STAGE + ".sleepMs", "2000");
  }

  public static class FailoverToOutOfSyncPassivesTestApp extends ClientBase {

    public FailoverToOutOfSyncPassivesTestApp(String[] args) {
      super(args);
    }

    public static void main(String[] args) {
      new FailoverToOutOfSyncPassivesTestApp(args).run();
    }

    @Override
    protected void runTest(final Cache cache, ClusteringToolkit clusteringToolkit) throws Throwable {
      getTestControlMbean().crashAllPassiveServers(0);

      info("Starting up the passives.");
      getTestControlMbean().restartCrashedServer(0, 1);
      getTestControlMbean().restartCrashedServer(0, 2);

      info("Waiting until the passives are synced up.");
      getTestControlMbean().waitUntilPassiveStandBy(0);

      final AtomicInteger keyIndex = new AtomicInteger();
      final AtomicBoolean running = new AtomicBoolean(true);
      Thread putter = new Thread(new Runnable() {
        public void run() {
          while (running.get()) {
            cache.put(new Element("key-" + keyIndex.incrementAndGet(), new byte[1024]));
          }
        }
      });
      putter.start();

      info("Wait for a bit to do a few puts.");
      ThreadUtil.reallySleep(15 * 1000);

      info("Killing the active so a passive can take over.");
      getTestControlMbean().crashActiveServer(0);

      running.set(false);
      putter.join();

      info("Killing active server.");
      getTestControlMbean().crashActiveServer(0);

      for (int i = 1; i < keyIndex.get(); i++) {
        Assert.assertNotNull(cache.get("key-" + i));
      }
    }

    private void info(String msg) {
      System.out.println(msg);
    }

  }

}

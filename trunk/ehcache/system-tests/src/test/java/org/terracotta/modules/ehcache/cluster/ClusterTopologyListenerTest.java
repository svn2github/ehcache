/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.cluster;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;

import com.tc.test.config.model.TestConfig;
import com.tc.util.runtime.Os;

public class ClusterTopologyListenerTest extends AbstractCacheTestBase {
  public static final int CLIENT_COUNT = 3;

  public ClusterTopologyListenerTest(TestConfig testConfig) {
    super(testConfig, TopologyL1Client.class, TopologyL1Client.class, TopologyL1Client.class);
    if (Os.isWindows()) {
      disableTest();
    }

    testConfig.getL2Config().setClientReconnectWindow(600);

    testConfig.addTcProperty("l2.l1reconnect.enabled", "true");
    testConfig.addTcProperty("l2.l1reconnect.timeout.millis", "3000");
  }

  @Override
  protected void startClients() throws Throwable {
    Thread[] threads = new Thread[getTestConfig().getClientConfig().getClientClasses().length];
    int i = 0;
    for (final Class client : getTestConfig().getClientConfig().getClientClasses()) {
      threads[i] = new Thread(new Runnable() {

        public void run() {
          try {
            runClient(client);
          } catch (Throwable e) {
            throw new AssertionError(e);
          }
        }
      }, "test client " + i);
      threads[i].start();
      i++;
      Thread.sleep(60 * 1000);
    }

    for (Thread th : threads) {
      th.join();
    }

  }

}

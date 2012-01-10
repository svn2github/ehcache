/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.servermap;

import org.terracotta.ehcache.tests.AbstractExpressCacheTest;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

public class LockGCNotFlushingEntriesTest extends AbstractExpressCacheTest {

  public LockGCNotFlushingEntriesTest() {
    super("/servermap/lock-gc-test.xml", LockGCNotFlushingEntriesTestClient.class);
  }

  @Override
  protected Class<?> getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractExpressCacheTest.App {

    public App(final String appId, final ApplicationConfig cfg, final ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      addClientJvmarg("-Dcom.tc.l1.lockmanager.timeout.interval=5000");
    }
  }

}

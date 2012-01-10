/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

public class SuspendResumeAtomikosXATest extends AbstractStandaloneCacheTest {

  public SuspendResumeAtomikosXATest() {
    super("basic-xa-test.xml", SuspendResumeClient.class);
    setParallelClients(false);
  }

  @Override
  protected Class getApplicationClass() {
    return SuspendResumeAtomikosXATest.App.class;
  }

  public static class App extends AbstractStandaloneCacheTest.AtomikosApp {

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

  }
}

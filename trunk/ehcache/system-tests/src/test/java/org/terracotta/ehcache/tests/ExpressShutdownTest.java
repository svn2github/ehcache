/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.runtime.Vm;

public class ExpressShutdownTest extends AbstractStandaloneCacheTest {

  public ExpressShutdownTest() {
    super("basic-cache-test.xml", ShutdownClient1.class, ShutdownClient2.class);
    setParallelClients(true);

    // JDK 1.5 perm gen collection is not reliable enough
    if (Vm.isJRockit() || Vm.isHotSpot() && Vm.isJDK15()) {
      disableTest();
    }
  }

  @Override
  protected Class getApplicationClass() {
    return ExpressShutdownApp.class;
  }

  public static class ExpressShutdownApp extends AbstractStandaloneCacheTest.App {
    public ExpressShutdownApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      if (Vm.isHotSpot()) {
        addClientJvmarg("-XX:MaxPermSize=64M");
        addClientJvmarg("-XX:+HeapDumpOnOutOfMemoryError");
        addClientJvmarg("-XX:SoftRefLRUPolicyMSPerMB=0");
      }
    }
  }

}

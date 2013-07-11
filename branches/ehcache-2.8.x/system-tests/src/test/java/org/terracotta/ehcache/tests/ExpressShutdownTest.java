/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.test.config.model.TestConfig;
import com.tc.util.runtime.Vm;

public class ExpressShutdownTest extends AbstractCacheTestBase {

  public ExpressShutdownTest(TestConfig testConfig) {
    super("basic-cache-test.xml", testConfig, ShutdownClient1.class, ShutdownClient2.class);
    testConfig.getClientConfig().setParallelClients(true);

    // JDK 1.5 perm gen collection is not reliable enough
    if (Vm.isJRockit() || Vm.isHotSpot() && Vm.isJDK15()) {
      disableTest();
    }

    if (Vm.isHotSpot()) {
      testConfig.getClientConfig().addExtraClientJvmArg("-XX:MaxPermSize=256M");
      testConfig.getClientConfig().addExtraClientJvmArg("-XX:+HeapDumpOnOutOfMemoryError");
      testConfig.getClientConfig().addExtraClientJvmArg("-XX:SoftRefLRUPolicyMSPerMB=0");
    }
  }
}

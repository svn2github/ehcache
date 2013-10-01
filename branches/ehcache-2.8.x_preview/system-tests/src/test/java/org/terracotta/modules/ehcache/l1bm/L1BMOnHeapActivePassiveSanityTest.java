/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.l1bm;

import net.sf.ehcache.terracotta.AbstractTerracottaActivePassiveTestBase;

import com.tc.test.config.model.ServerCrashMode;
import com.tc.test.config.model.TestConfig;

public class L1BMOnHeapActivePassiveSanityTest extends AbstractTerracottaActivePassiveTestBase {

  public L1BMOnHeapActivePassiveSanityTest(TestConfig testConfig) {
    super(testConfig, L1BMOnHeapBasicSanityTestApp.class, L1BMOnHeapBasicSanityTestApp.class);
    testConfig.getL2Config().setMaxHeap(1024);
    testConfig.getL2Config().setOffHeapEnabled(true);
    testConfig.getL2Config().setMaxOffHeapDataSize(1024);
    testConfig.addTcProperty("ehcache.evictor.logging.enabled", "true");
    testConfig.getCrashConfig().setCrashMode(ServerCrashMode.RANDOM_SERVER_CRASH);
    testConfig.getCrashConfig().setServerCrashWaitTimeInSec(30);
  }

}

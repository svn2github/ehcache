/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.l1bm;

import com.tc.test.MultipleServersCrashMode;
import com.tc.test.MultipleServersPersistenceMode;
import com.tc.test.MultipleServersSharedDataMode;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tctest.ActivePassiveTransparentTestBase;
import com.tctest.TransparentTestIface;

public class L1BMOnHeapActivePassiveSanityTest extends ActivePassiveTransparentTestBase {

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(L1BMOnHeapBasicSanityTestApp.NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  public void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    setupManager.setServerCount(2);
    setupManager.setServerCrashMode(MultipleServersCrashMode.RANDOM_SERVER_CRASH);
    setupManager.setServerCrashWaitTimeInSec(30);
    setupManager.setServerShareDataMode(MultipleServersSharedDataMode.NETWORK);
    setupManager.setServerPersistenceMode(MultipleServersPersistenceMode.PERMANENT_STORE);
  }

  @Override
  protected Class getApplicationClass() {
    return L1BMOnHeapBasicSanityTestApp.class;
  }
}

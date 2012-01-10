/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.l1bm;

import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.TransparentAppConfig;

public class L1BMEventualDCV2IdentityWithStatsTest extends TransparentTestBase {

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(L1BMOnHeapReadWriteTestApp.NODE_COUNT);
    t.initializeTestRunner();
    TransparentAppConfig cfg = t.getTransparentAppConfig();
    cfg.setAttribute(L1BMOnHeapReadWriteTestApp.CONSISTENCY, "EVENTUAL");
    cfg.setAttribute(L1BMOnHeapReadWriteTestApp.STORAGE_STRATEGY, "DCV2");
    cfg.setAttribute(L1BMOnHeapReadWriteTestApp.VALUE_MODE, "identity");
    cfg.setAttribute(L1BMOnHeapReadWriteTestApp.WITH_STATS, "false");
  }

  @Override
  protected Class getApplicationClass() {
    return L1BMOnHeapReadWriteTestApp.class;
  }
}

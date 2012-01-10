/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.l1bm;

import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;

public class L1BMOnHeapSanityTest extends TransparentTestBase {

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(L1BMOnHeapBasicSanityTestApp.NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return L1BMOnHeapBasicSanityTestApp.class;
  }
}

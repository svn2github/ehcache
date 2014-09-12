/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.txns;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.test.util.TestBaseUtil;

import bitronix.tm.TransactionManagerServices;

import com.tc.test.config.model.TestConfig;

import java.util.ArrayList;
import java.util.List;

public class TwoResourceBTMXATest extends AbstractCacheTestBase {

  public TwoResourceBTMXATest(TestConfig testConfig) {
    super("two-resource-xa-test.xml", testConfig, BTMTwoResourceTx1.class, BTMTwoResourceTx2.class);
    testConfig.getClientConfig().setParallelClients(true);

    // DEV-3930
    // disableAllUntil("2010-03-31");
  }

  @Override
  protected List<String> getExtraJars() {
    List<String> extraJars = new ArrayList<String>();
    extraJars.add(TestBaseUtil.jarFor(TransactionManagerServices.class));
    return extraJars;
  }

}

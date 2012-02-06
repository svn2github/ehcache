/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import org.terracotta.test.util.TestBaseUtil;

import bitronix.tm.TransactionManagerServices;

import com.tc.test.config.model.TestConfig;

import java.util.ArrayList;
import java.util.List;

public class TwoResourceSuspendResumeBTMXATest extends AbstractCacheTestBase {

  public TwoResourceSuspendResumeBTMXATest(TestConfig testConfig) {
    super("two-resource-xa-test.xml", testConfig, TwoResourceSuspendResumeBTMClient.class);
    testConfig.getClientConfig().setParallelClients(false);
  }

  @Override
  protected List<String> getExtraJars() {
    List<String> extraJars = new ArrayList<String>();
    extraJars.add(TestBaseUtil.jarFor(TransactionManagerServices.class));
    return extraJars;
  }

}

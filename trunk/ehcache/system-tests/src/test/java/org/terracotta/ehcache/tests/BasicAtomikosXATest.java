/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import org.terracotta.test.util.TestBaseUtil;

import com.atomikos.icatch.config.UserTransactionService;
import com.atomikos.icatch.config.imp.AbstractUserTransactionService;
import com.atomikos.icatch.jta.UserTransactionManager;
import com.tc.test.config.model.TestConfig;

import java.util.ArrayList;
import java.util.List;

public class BasicAtomikosXATest extends AbstractCacheTestBase {

  public BasicAtomikosXATest(TestConfig testConfig) {
    super("basic-xa-test.xml", testConfig, SimpleTx1.class, SimpleTx2.class);
  }

  @Override
  protected List<String> getExtraJars() {
    List<String> extraJars = new ArrayList<String>();
    extraJars.add(TestBaseUtil.jarFor(UserTransactionManager.class));
    extraJars.add(TestBaseUtil.jarFor(UserTransactionService.class));
    extraJars.add(TestBaseUtil.jarFor(AbstractUserTransactionService.class));
    extraJars.add(TestBaseUtil.jarFor(com.atomikos.diagnostics.Console.class));
    return extraJars;
  }

}

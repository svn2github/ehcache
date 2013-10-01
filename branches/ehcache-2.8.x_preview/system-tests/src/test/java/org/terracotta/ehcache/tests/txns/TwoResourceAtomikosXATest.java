/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.txns;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.test.util.TestBaseUtil;

import com.atomikos.icatch.config.UserTransactionService;
import com.atomikos.icatch.config.imp.AbstractUserTransactionService;
import com.atomikos.icatch.jta.UserTransactionManager;
import com.atomikos.util.Atomikos;
import com.tc.test.config.model.TestConfig;

import java.util.ArrayList;
import java.util.List;

public class TwoResourceAtomikosXATest extends AbstractCacheTestBase {

  public TwoResourceAtomikosXATest(TestConfig testConfig) throws Exception {
    super("two-resource-xa-test.xml", testConfig, TwoResourceTx1.class, TwoResourceTx2.class);
    testConfig.getClientConfig().setParallelClients(true);
  }

  @Override
  protected List<String> getExtraJars() {
    List<String> extraJars = new ArrayList<String>();
    extraJars.add(TestBaseUtil.jarFor(UserTransactionManager.class));
    extraJars.add(TestBaseUtil.jarFor(UserTransactionService.class));
    extraJars.add(TestBaseUtil.jarFor(AbstractUserTransactionService.class));
    extraJars.add(TestBaseUtil.jarFor(Atomikos.class));
    return extraJars;
  }

}

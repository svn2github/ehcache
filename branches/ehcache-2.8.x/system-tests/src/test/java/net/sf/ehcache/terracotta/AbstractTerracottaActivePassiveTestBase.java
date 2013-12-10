/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package net.sf.ehcache.terracotta;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;

import com.tc.test.config.model.ServerCrashMode;
import com.tc.test.config.model.TestConfig;

public class AbstractTerracottaActivePassiveTestBase extends AbstractCacheTestBase {

  public AbstractTerracottaActivePassiveTestBase(TestConfig testConfig, Class<? extends ClientBase>... c) {
    this("basic-cache-test.xml", testConfig, c);
  }

  public AbstractTerracottaActivePassiveTestBase(String ehcacheConfigPath, TestConfig testConfig,
                                                 Class<? extends ClientBase>... c) {
    super(ehcacheConfigPath, testConfig, c);

    testConfig.setRestartable(false);

    testConfig.getGroupConfig().setMemberCount(2);
    testConfig.getGroupConfig().setElectionTime(5);

    testConfig.getCrashConfig().setCrashMode(ServerCrashMode.NO_CRASH);
  }

}

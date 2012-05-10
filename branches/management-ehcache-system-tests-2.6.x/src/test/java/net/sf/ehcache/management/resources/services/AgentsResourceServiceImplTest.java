package net.sf.ehcache.management.resources.services;

import net.sf.ehcache.tests.AbstractEhcacheManagementTestBase;

import com.tc.test.config.model.TestConfig;

/**
 * @author Ludovic Orban
 */
public class AgentsResourceServiceImplTest extends AbstractEhcacheManagementTestBase {

  public AgentsResourceServiceImplTest(TestConfig testConfig) {
    super(testConfig);

    testConfig.setNumOfGroups(1);
    testConfig.getGroupConfig().setMemberCount(1);
    testConfig.getClientConfig().setClientClasses(AgentsResourceServiceImplTestClient.class, 1);
  }

}

package net.sf.ehcache.management.resources.services;

import net.sf.ehcache.tests.AbstractEhcacheManagementTestBase;

import com.tc.test.config.model.TestConfig;

/**
 * @author Ludovic Orban
 */
public class CacheManagersResourceServiceImplTest extends AbstractEhcacheManagementTestBase {

  public CacheManagersResourceServiceImplTest(TestConfig testConfig) {
    super(testConfig);

    testConfig.setNumOfGroups(1);
    testConfig.getGroupConfig().setMemberCount(1);
    testConfig.getClientConfig().setClientClasses(CacheManagersResourceServiceImplTestClient.class, 1);
  }

}

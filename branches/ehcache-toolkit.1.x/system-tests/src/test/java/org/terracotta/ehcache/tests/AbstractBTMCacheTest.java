/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import org.terracotta.test.util.TestBaseUtil;

import bitronix.tm.TransactionManagerServices;

import com.tc.test.config.model.TestConfig;

import java.util.ArrayList;
import java.util.List;

public class AbstractBTMCacheTest extends AbstractCacheTestBase {

  public AbstractBTMCacheTest(String ehcacheConfigPath, TestConfig testConfig, Class<? extends ClientBase>... c) {
    super(ehcacheConfigPath, testConfig, c);
  }

  public AbstractBTMCacheTest(String ehcacheConfigPath, TestConfig testConfig) {
    this(ehcacheConfigPath, testConfig, Client1.class, Client2.class);
  }

  @Override
  protected List<String> getExtraJars() {
    List<String> extraJars = new ArrayList<String>();
    extraJars.add(TestBaseUtil.jarFor(TransactionManagerServices.class));
    return extraJars;
  }
}

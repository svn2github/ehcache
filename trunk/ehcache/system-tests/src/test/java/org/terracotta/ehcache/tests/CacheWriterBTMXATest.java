/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import org.apache.derby.jdbc.EmbeddedXADataSource;
import org.hibernate.dialect.DerbyDialect;
import org.terracotta.test.util.TestBaseUtil;

import com.tc.test.config.model.TestConfig;

import java.util.List;

public class CacheWriterBTMXATest extends AbstractBTMCacheTest {

  public CacheWriterBTMXATest(TestConfig testConfig) {
    super("basic-xa-test.xml", testConfig, CacheWriterBTMTxClient.class);
    testConfig.getClientConfig().setParallelClients(false);
  }

  @Override
  protected List<String> getExtraJars() {
    List<String> extraJars = super.getExtraJars();
    extraJars.add(TestBaseUtil.jarFor(DerbyDialect.class));
    extraJars.add(TestBaseUtil.jarFor(EmbeddedXADataSource.class));
    return extraJars;
  }

}

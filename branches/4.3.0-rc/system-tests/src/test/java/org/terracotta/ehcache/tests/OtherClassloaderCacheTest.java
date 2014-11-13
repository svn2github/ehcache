/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import org.objectweb.asm.ClassWriter;
import org.terracotta.test.util.TestBaseUtil;

import com.tc.test.config.model.TestConfig;

public class OtherClassloaderCacheTest extends AbstractCacheTestBase {

  @Override
  protected String getTestDependencies() {
    return TestBaseUtil.jarFor(ClassWriter.class);
  }
  public OtherClassloaderCacheTest(TestConfig testConfig) {
    super("small-memory-cache-test.xml", testConfig, OtherClassloaderClient.class, ReaderClient.class);
    testConfig.getClientConfig().setParallelClients(false);
  }

}

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.test.config.model.TestConfig;

public class OtherClassLoaderEventTest extends AbstractCacheTestBase {

  public OtherClassLoaderEventTest(TestConfig testConfig) {
    super("other-class-loader-event-test.xml", testConfig, OtherClassLoaderEventClient1.class,
          OtherClassLoaderEventClient2.class);
    testConfig.getClientConfig().setParallelClients(true);
  }

}

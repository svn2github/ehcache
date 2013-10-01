/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.test.config.model.TestConfig;

public class UrlConfigStandaloneCacheTest extends AbstractCacheTestBase {

  public UrlConfigStandaloneCacheTest(TestConfig testConfig) {
    super("url-config-cache-test.xml", testConfig);
  }

  @Override
  protected void runClient(Class client) throws Throwable {
    writeXmlFileWithPort("url-config-cache-test-tc-config.xml", "tc-config-client.xml");
    super.runClient(client);
  }
}

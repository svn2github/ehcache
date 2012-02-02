/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.test.config.model.TestConfig;

public class XAResourceTest extends AbstractCacheTestBase {

  public XAResourceTest(TestConfig testConfig) {
    super("xaresource-test.xml", testConfig, BareXAResourceTx.class);
  }
}

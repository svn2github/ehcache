package org.terracotta.ehcache.tests.servermap;

import org.terracotta.ehcache.tests.AbstractExpressCacheTest;

public class ServerMapClearExpressTest extends AbstractExpressCacheTest {

  public ServerMapClearExpressTest() {
    super("/servermap/servermap-clear-test.xml", ServerMapClearExpressTestClient1.class,
          ServerMapClearExpressTestClient2.class);
    super.setParallelClients(true);
  }

}

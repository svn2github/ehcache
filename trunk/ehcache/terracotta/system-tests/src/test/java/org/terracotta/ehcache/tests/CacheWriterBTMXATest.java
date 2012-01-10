/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import org.apache.derby.jdbc.EmbeddedXADataSource;
import org.hibernate.dialect.DerbyDialect;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

import java.util.List;

public class CacheWriterBTMXATest extends AbstractStandaloneCacheTest {

  public CacheWriterBTMXATest() {
    super("basic-xa-test.xml", CacheWriterBTMTxClient.class);
    setParallelClients(false);
  }

  @Override
  protected Class getApplicationClass() {
    return CacheWriterBTMXATest.App.class;
  }

  public static class App extends AbstractStandaloneCacheTest.BTMApp {

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected List<String> getExtraJars() {
      List<String> extraJars = super.getExtraJars();
      extraJars.add(jarFor(DerbyDialect.class));
      extraJars.add(jarFor(EmbeddedXADataSource.class));
      return extraJars;
    }

  }
}

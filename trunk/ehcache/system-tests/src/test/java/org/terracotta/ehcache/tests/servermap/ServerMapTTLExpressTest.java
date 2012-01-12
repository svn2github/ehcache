/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.servermap;

import org.terracotta.ehcache.tests.AbstractExpressCacheTest;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

import java.util.Iterator;
import java.util.List;

public class ServerMapTTLExpressTest extends AbstractExpressCacheTest {

  public ServerMapTTLExpressTest() {
    super("/servermap/basic-servermap-cache-test.xml", ServerMapTTLExpressTestClient.class);
  }

  @Override
  protected synchronized void setupConfigLogDataStatisticsPaths(TestConfigurationSetupManagerFactory out)
      throws ConfigurationSetupException {
    super.setupConfigLogDataStatisticsPaths(out);

    out.setGCEnabled(true);
    out.setGCIntervalInSec(60);
    out.setGCVerbose(true);

    out.addTcPropertyToConfig("ehcache.evictor.logging.enabled", "true");
  }

  @Override
  protected Class<?> getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractExpressCacheTest.App {

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void addTestTcPropertiesFile(final List<String> jvmArgs) {
      // do not add tc properties file for this test
    }

    @Override
    protected void configureClientExtraJVMArgs(final List<String> jvmArgs) {
      super.configureClientExtraJVMArgs(jvmArgs);
      final Iterator<String> iter = jvmArgs.iterator();
      while (iter.hasNext()) {
        final String prop = iter.next();
        if (prop.contains("ehcache.storageStrategy.dcv2.localcache.enabled")) {
          // remove it and always disable localcache for this test
          iter.remove();
        }
      }
      // always disable local cache
      jvmArgs.add("-Dcom.tc.ehcache.storageStrategy.dcv2.localcache.enabled=false");
    }

  }

}
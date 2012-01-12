/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.servermap;

import org.terracotta.ehcache.tests.AbstractExpressCacheTest;

import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ServerMapL1EvictionOffHeapDestroyExpressTest extends AbstractExpressCacheTest {

  public ServerMapL1EvictionOffHeapDestroyExpressTest() {

    super("/servermap/servermap-l1-eviction-destroy-off-heap-test.xml",
          ServerMapL1EvictionOffHeapDestroyExpressTestClient.class);
  }

  @Override
  protected void setExtraJvmArgs(ArrayList jvmArgs) {

    // adds server side jvm args to encourage faulting from on heap L2
    super.setExtraJvmArgs(jvmArgs);

    // shut off inline dgc
    jvmArgs.add("-Dcom.tc.l2.objectmanager.dgc.inline.enabled=false");

    // set low threshold for eviction from on-heap to cause more faulting
    jvmArgs.add("-Dcom.tc.l2.cachemanager.criticalThreshold = 10");

    jvmArgs.add("-Dcom.tc.l2.cachemanager.threshold = 5");

    // enable fault & flush logging
    jvmArgs.add("-Dcom.tc.l2.objectmanager.flush.logging.enabled=true");
    jvmArgs.add("-Dcom.tc.l2.objectmanager.fault.logging.enabled=true");

    jvmArgs.add("-Xmx256m");

  }

  @Override
  protected void setupConfig(TestConfigurationSetupManagerFactory configFactory) {
    super.setupConfig(configFactory);
    configFactory.setGCEnabled(false);
  }

  @Override
  protected Class<?> getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractExpressCacheTest.App {

    public App(final String appId, final ApplicationConfig cfg, final ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      addClientJvmarg("-Dcom.tc.l1.lockmanager.timeout.interval=600000");
      // ehcache evictor logging not needed for this test
      addClientJvmarg("-Dcom.tc.ehcache.evictor.logging.enabled=false");
      addClientJvmarg("-XX:+HeapDumpOnOutOfMemoryError");
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
          // remove it and disable local cache for this test
          iter.remove();
        }
      }
      // disable local cache to cause reads/writes to go to server
      jvmArgs.add("-Dcom.tc.ehcache.storageStrategy.dcv2.localcache.enabled=false");
    }

  }

}

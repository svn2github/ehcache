package org.terracotta.ehcache.tests.servermap;

import org.terracotta.ehcache.tests.AbstractExpressCacheTest;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

import java.util.ArrayList;

public class ServerMapL2EvictionReachesL1Test extends AbstractExpressCacheTest {

  public ServerMapL2EvictionReachesL1Test() {
    super("/servermap/servermap-l2-eviction-reaches-l1-test.xml", ServerMapL2EvictionReachesL1TestClient.class);
    super.setParallelClients(true);
  }

  @Override
  protected Class<?> getApplicationClass() {
    return ServerMapL2EvictionReachesL1App.class;
  }

  @Override
  protected void setExtraJvmArgs(ArrayList jvmArgs) {
    super.setExtraJvmArgs(jvmArgs);
    jvmArgs.add("-Dcom.tc.ehcache.evictor.logging.enabled=true");
  }

  @Override
  protected synchronized void setupConfigLogDataStatisticsPaths(TestConfigurationSetupManagerFactory out)
      throws ConfigurationSetupException {
    super.setupConfigLogDataStatisticsPaths(out);

    out.setGCEnabled(true);
    out.setGCIntervalInSec(60);
    out.setGCVerbose(true);
  }

  public static class ServerMapL2EvictionReachesL1App extends App {

    public ServerMapL2EvictionReachesL1App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      addClientJvmarg("-Dcom.tc.l1.cachemanager.enabled=false");
      addClientJvmarg("-Dcom.tc.ehcache.evictor.logging.enabled=true");
      addClientJvmarg("-Dcom.tc.l1.lockmanager.timeout.interval=60000");
    }

  }

}

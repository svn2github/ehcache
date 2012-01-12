package org.terracotta.ehcache.tests;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

/**
 * @author Alex Snaps
 */
public class ClusterCacheEventsRejoinEnabledTest extends AbstractStandaloneCacheTest {

  public ClusterCacheEventsRejoinEnabledTest() {
    super("basic-rejoin-cache-test.xml", ClusterCacheEventsRejoinEnabledClient.class,
          ClusterCacheEventsRejoinEnabledClient.class);
    setParallelClients(true);
  }

  @Override
  protected Class getApplicationClass() {
    return ClusterEventsRejoinEnabledTest.App.class;
  }

  public static class App extends AbstractStandaloneCacheTest.App {
    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }
  }

}

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

/**
 *
 * @author cdennis
 */
public class DiskPersistentStandaloneCacheTest extends AbstractStandaloneCacheTest {

  public DiskPersistentStandaloneCacheTest() {
    super("disk-persistent-cache-test.xml");
  }

  @Override
  protected Class getApplicationClass() {
    return DiskPersistentStandaloneCacheTest.App.class;
  }

  public static class App extends AbstractStandaloneCacheTest.App {
    public App(final String appId, final ApplicationConfig cfg, final ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void evaluateClientOutput(final String clientName, final int exitCode, final String clientOutput) throws AssertionError {
      if ((exitCode == 0) || !clientOutput.contains("InvalidConfigurationException")) {
        throw new AssertionError("Exit Code " + exitCode + "\n" + clientOutput);
      }
    }
  }
}

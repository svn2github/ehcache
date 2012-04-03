/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.event;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;

import com.tc.test.config.model.TestConfig;

import java.util.Set;

import junit.framework.Assert;

public class ClusteredEventsLocalTest extends AbstractCacheTestBase {

  public ClusteredEventsLocalTest(TestConfig testConfig) {
    super("clustered-events-test.xml", testConfig, App.class, App.class, App.class, App.class, App.class);
    testConfig.addTcProperty("ehcache.clusteredStore.checkContainsKeyOnPut", "true");
  }

  public static class App extends ClientBase {

    public App(String[] args) {
      super("testLocal", args);
    }

    @Override
    protected void runTest(Cache cache, Toolkit clusteringToolkit) throws Throwable {
      final int index = waitForAllClients();

      Assert.assertEquals(0, cache.getSize());

      waitForAllClients();

      cache.put(new Element("key" + index, "value" + index));
      cache.put(new Element("key" + index, "valueUpdated" + index));
      cache.remove("key" + index);

      waitForAllClients();

      cache.removeAll();

      waitForAllClients();

      Thread.sleep(10000);

      EhcacheTerracottaEventListener listener = null;
      Set<CacheEventListener> listeners = cache.getCacheEventNotificationService().getCacheEventListeners();
      for (CacheEventListener l : listeners) {
        if (l instanceof EhcacheTerracottaEventListener) {
          listener = (EhcacheTerracottaEventListener) l;
          break;
        }
      }

      Assert.assertNotNull(listener);

      Assert.assertEquals(1, listener.getPut().size());
      Assert.assertEquals(1, listener.getUpdate().size());
      Assert.assertEquals(1, listener.getRemove().size());
      Assert.assertEquals(1, listener.getRemoveAll());
    }

  }

}

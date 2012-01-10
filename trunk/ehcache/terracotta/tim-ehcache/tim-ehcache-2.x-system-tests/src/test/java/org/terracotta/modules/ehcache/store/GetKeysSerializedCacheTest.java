/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.terracotta.modules.BasicTimInfo;
import org.terracotta.modules.TimInfo;

/**
 *
 * @author Chris Dennis
 */
public class GetKeysSerializedCacheTest extends TransparentTestBase {

  private static final int NODE_COUNT = 2;

  @Override
  public void doSetUp(final TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    private final CyclicBarrier barrier = new CyclicBarrier(getParticipantCount());

    public App(final String appId, final ApplicationConfig cfg, final ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void runTest() throws Throwable {
      final int index = barrier.await();

      CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/basic-cache-test.xml"));

      Cache cache = cacheManager.getCache("test");

      Assert.assertEquals(0, cache.getSize());

      barrier.await();

      if (index == 0) {
        cache.put(new Element(new KeyType("key"), "value"));
      }

      barrier.await();

      String value = (String) cache.get(new KeyType("key")).getObjectValue();
      Assert.assertEquals(1, cache.getSize());
      Assert.assertEquals("value", value);

      barrier.await();

      List keys = cache.getKeys();
      Assert.assertEquals(1, keys.size());
      Object k = keys.iterator().next();
      Assert.assertTrue(k instanceof KeyType);
      Assert.assertEquals(new KeyType("key"), k);

      barrier.await();
    }

    public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
      config.getOrCreateSpec(App.class.getName()).addRoot("barrier", "barrier");

      String module_name = "tim-ehcache-2.x";
      TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
      config.addModule(timInfo.artifactId(), timInfo.version());
    }
  }

  public static class KeyType implements Serializable {

    private final String string;

    KeyType(String string) {
      this.string = string;
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof KeyType && ((KeyType) o).string.equals(string);
    }

    @Override
    public int hashCode() {
      return string.hashCode();
    }
  }
}

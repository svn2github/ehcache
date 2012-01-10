/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.terracotta.modules.BasicTimInfo;
import org.terracotta.modules.TimInfo;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;

public class PrimitiveClassTest extends TransparentTestBase {

  private static final int NODE_COUNT = 3;

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    private final CyclicBarrier barrier = new CyclicBarrier(getParticipantCount());

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void runTest() throws Throwable {
      final int index = barrier.await();

      CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/primitive-class-test.xml"));

      Cache cache = cacheManager.getCache("test");

      Set<Class<?>> types = new HashSet<Class<?>>();
      types.add(Void.TYPE);
      types.add(Boolean.TYPE);
      types.add(Byte.TYPE);
      types.add(Character.TYPE);
      types.add(Double.TYPE);
      types.add(Float.TYPE);
      types.add(Integer.TYPE);
      types.add(Long.TYPE);
      types.add(Short.TYPE);

      if (index == 0) {
        for (Class<?> c : types) {
          cache.put(new Element(c, c));
        }
      }

      barrier.await();

      for (Class<?> c : types) {
        assertEquals(c, cache.get(c).getObjectValue());
        assertEquals(c, cache.get(c).getObjectKey());
      }

      Set<Class<?>> copy = new HashSet<Class<?>>(types);
      for (Object o : cache.getKeys()) {
        boolean removed = copy.remove(o);
        if (!removed) { throw new AssertionError("did not remove: " + o); }
      }

      assertEquals(copy.toString(), 0, copy.size());

    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.getOrCreateSpec(App.class.getName()).addRoot("barrier", "barrier");

      String module_name = "tim-ehcache-2.x";
      TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
      config.addModule(timInfo.artifactId(), timInfo.version());
    }
  }

}

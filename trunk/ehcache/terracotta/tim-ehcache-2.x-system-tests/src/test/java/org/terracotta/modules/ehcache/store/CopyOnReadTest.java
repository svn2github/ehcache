/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.ValueMode;

import org.apache.commons.io.IOUtils;
import org.terracotta.modules.BasicTimInfo;
import org.terracotta.modules.TimInfo;

import com.tc.object.bytecode.hook.impl.ClassProcessorHelper;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

public class CopyOnReadTest extends TransparentTestBase {

  private static final int NODE_COUNT = 1;

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

    public App(final String appId, final ApplicationConfig cfg, final ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void runTest() throws Throwable {
      Configuration cacheManagerConfig = new Configuration();
      CacheConfiguration config = new CacheConfiguration();
      config.setName("test");
      config.setEternal(true);
      config.setOverflowToDisk(false);
      config.setOverflowToOffHeap(false);
      config.setMaxEntriesLocalHeap(0);
      config.setCopyOnRead(true);

      TerracottaConfiguration tcConfig = new TerracottaConfiguration();
      tcConfig.valueMode(ValueMode.SERIALIZATION);
      config.terracotta(tcConfig);

      cacheManagerConfig.addCache(config);

      CacheManager cacheManager = new CacheManager(cacheManagerConfig);

      Cache cache = cacheManager.getCache("test");

      Loader loader1 = new Loader("1");
      Loader loader2 = new Loader("2");

      // hack-tastic! Since system test framework picks manager based on TCCL we need to register these new loaders
      ClassProcessorHelper.setContext(loader1, ClassProcessorHelper.getContext(getClass().getClassLoader()));
      ClassProcessorHelper.setContext(loader2, ClassProcessorHelper.getContext(getClass().getClassLoader()));

      cache.put(new Element("key", loader1.loadClass(ValueHolder.class.getName()).newInstance()));

      Thread.currentThread().setContextClassLoader(loader1);
      Object value = cache.get("key").getObjectValue();
      // read should be done with TCCL
      assertEquals(loader1, value.getClass().getClassLoader());
      // repeated read should get a different object
      assertNotSame(value, cache.get("key").getObjectValue());

      Thread.currentThread().setContextClassLoader(loader2);
      // read with TCCL set to loader2 should get value defined in loader2
      value = cache.get("key").getObjectValue();
      assertEquals(loader2, value.getClass().getClassLoader());
    }

    public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
      String module_name = "tim-ehcache-2.x";
      TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
      config.addModule(timInfo.artifactId(), timInfo.version());
    }
  }

  public static class ValueHolder implements Serializable {
    private volatile String data;

    public ValueHolder() {
      //
    }

    public String getData() {
      return data;
    }

    public void setData(String data) {
      this.data = data;
    }
  }

  private static class Loader extends ClassLoader {
    private final String id;

    Loader(String id) {
      super(null);
      this.id = id;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
      if (name.equals(ValueHolder.class.getName())) {
        InputStream in = getClass().getClassLoader().getResourceAsStream(name.replace('.', '/').concat(".class"));
        byte[] b;
        try {
          b = IOUtils.toByteArray(in);
        } catch (IOException e) {
          throw new ClassNotFoundException(name, e);
        }
        return defineClass(name, b, 0, b.length);
      }

      return super.loadClass(name);
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + "(" + id + ")";
    }

  }

}

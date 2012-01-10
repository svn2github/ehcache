package net.sf.ehcache.terracotta;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.CacheStoreHelper;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.store.TerracottaStore;

import org.terracotta.modules.BasicTimInfo;
import org.terracotta.modules.TimInfo;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

/**
 * @author Alex Snaps
 */
public class BootstrapCacheTest extends TransparentTestBase {

  private static final int NODE_COUNT        = 3;

  private static final int ELEMENTS_PER_NODE = 10;

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

    private final CyclicBarrier barrier = new CyclicBarrier(NODE_COUNT);

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void runTest() throws Throwable {
      final int index = barrier.await();

      Assert.assertEquals(0, KeySnapshotter.getKnownCacheManagers().size());
      CacheManager cacheManager = createNewCacheManager(index);
      Cache cache = cacheManager.getCache("test");

      barrier.await();
      Thread.sleep(TimeUnit.SECONDS.toMillis(8));
      for (int i = 0; i < ELEMENTS_PER_NODE; i++) {
        cache.put(new Element("key" + i + "_node" + index, "value for key"));
      }
      System.out.println(new Date() + " ==> node" + index + " put 10 elements in the cache");
      barrier.await();

      if (index == 0) {
        Assert.assertEquals(ELEMENTS_PER_NODE * NODE_COUNT, cache.getSize());
      }
      Assert.assertEquals(1, KeySnapshotter.getKnownCacheManagers().size());
      Assert.assertEquals(true, KeySnapshotter.getKnownCacheManagers().contains(cacheManager));
      Thread.sleep(TimeUnit.SECONDS.toMillis(10));
      System.out.println(new Date() + " ==> node" + index + " CacheManager shutdown1...");
      cacheManager.shutdown();
      barrier.await();
      cacheManager = createNewCacheManager(index);
      cache = cacheManager.getCache("test");

      final KeySnapshotter keySnapshotter = ((TerracottaBootstrapCacheLoader) cache.getBootstrapCacheLoader())
          .getKeySnapshotter();
      keySnapshotter.doSnapshot();

      final Store store = new CacheStoreHelper(cache).getStore();
      final Set localKeys = ((TerracottaStore) store).getLocalKeys();
      for (Object localKey : localKeys) {
        final String key = (String) localKey;
        Assert.assertEquals("_node" + index, key.substring(key.indexOf("_"), key.length()));
      }
      Assert.assertEquals("For node" + index, ELEMENTS_PER_NODE, localKeys.size());
      barrier.await();
      gc();
      Assert.assertEquals("For node " + index, 1, KeySnapshotter.getKnownCacheManagers().size());
      Assert.assertEquals("For node " + index, true, KeySnapshotter.getKnownCacheManagers().contains(cacheManager));
      barrier.await();
      cacheManager.shutdown();
      System.out.println(new Date() + " ==> node" + index + " CacheManager shutdown2...");
    }

    private void gc() throws InterruptedException {
      System.gc();
      Thread.sleep(2000);
      System.gc();
      System.gc();
    }

    private CacheManager createNewCacheManager(final int index) {
      final Configuration configuration = new Configuration();
      configuration
          .addCache(new CacheConfiguration("test", 20)
              .eternal(true)
              .terracotta(new TerracottaConfiguration().clustered(true)
                              .consistency(TerracottaConfiguration.Consistency.STRONG))
              .bootstrapCacheLoaderFactory(new CacheConfiguration.BootstrapCacheLoaderFactoryConfiguration()
                                               .className(net.sf.ehcache.terracotta.TerracottaBootstrapCacheLoaderFactory.class
                                                              .getName())
                                               .properties("bootstrapAsynchronously=false" + ",directory=dumps_node"
                                                               + index + ",interval=15" + ",immediateShutdown=true"
                                                               + ",doKeySnapshot=true"
                                                               + ",doKeySnapshotOnDedicatedThread=false")
                                               .propertySeparator(",")));
      return new CacheManager(configuration);
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.getOrCreateSpec(App.class.getName()).addRoot("barrier", "barrier").addRoot("nodes", "nodes");

      String module_name = "tim-ehcache-2.x";
      TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
      config.addModule(timInfo.artifactId(), timInfo.version());
    }
  }

}

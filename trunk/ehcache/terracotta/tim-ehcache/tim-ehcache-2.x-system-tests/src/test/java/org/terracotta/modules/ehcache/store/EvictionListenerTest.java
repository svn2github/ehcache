/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;

import org.apache.commons.io.IOUtils;
import org.terracotta.modules.BasicTimInfo;
import org.terracotta.modules.TimInfo;

import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.config.schema.setup.TestConfigurationSetupManagerFactory;
import com.tc.config.schema.test.L2ConfigBuilder;
import com.tc.config.schema.test.TerracottaConfigBuilder;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.StandardDSOClientConfigHelperImpl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tctest.GCTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;

public class EvictionListenerTest extends GCTestBase {

  private static final int NODE_COUNT = 2;

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  public void setUp() throws Exception {
    PortChooser pc = new PortChooser();
    int port = pc.chooseRandomPort();
    int adminPort = pc.chooseRandomPort();
    int groupPort = pc.chooseRandomPort();
    File configFile = getTempFile("tc-config.xml");
    writeConfigFile(port, adminPort, configFile);
    TestConfigurationSetupManagerFactory factory = new TestConfigurationSetupManagerFactory(
                                                                                            TestConfigurationSetupManagerFactory.MODE_CENTRALIZED_CONFIG,
                                                                                            null,
                                                                                            new FatalIllegalConfigurationChangeHandler());

    L1ConfigurationSetupManager manager = factory.getL1TVSConfigurationSetupManager();
    setUpControlledServer(factory, new StandardDSOClientConfigHelperImpl(manager), port, adminPort, groupPort,
                          configFile.getAbsolutePath());
    doSetUp(this);
  }

  private synchronized void writeConfigFile(int dsoPort, int adminPort, File configFile) {
    try {
      TerracottaConfigBuilder builder = createConfig(dsoPort, adminPort);
      FileOutputStream out = new FileOutputStream(configFile);
      IOUtils.write(builder.toString(), out);
      out.close();
    } catch (Exception e) {
      throw Assert.failure("Can't create config file", e);
    }
  }

  private TerracottaConfigBuilder createConfig(int port, int adminPort) {
    TerracottaConfigBuilder out = new TerracottaConfigBuilder();

    out.getServers().getL2s()[0].setDSOPort(port);
    out.getServers().getL2s()[0].setJMXPort(adminPort);
    out.getServers().getL2s()[0].setPersistenceMode(L2ConfigBuilder.PERSISTENCE_MODE_PERMANENT_STORE);

    out.getServers().getL2s()[0].setGCEnabled(true);
    out.getServers().getL2s()[0].setGCInterval(10);
    out.getServers().getL2s()[0].setGCVerbose(true);

    return out;
  }

  @Override
  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp implements CacheEventListener {

    private final CyclicBarrier barrier      = new CyclicBarrier(getParticipantCount());
    private final AtomicLong    evictedCount = new AtomicLong(0);

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void runTest() throws Throwable {
      final int index = barrier.await();

      CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/evict-cache-test.xml"));

      Cache cache = cacheManager.getCache("test");

      cache.getCacheEventNotificationService().registerListener(this);

      // XXX: assert that the cache is clustered via methods on cache config (when methods exist)
      System.err.println(cache);

      Assert.assertEquals(0, cache.getSize());

      barrier.await();

      int numOfElements = 1000;

      if (index == 0) {
        for (int i = 0; i < numOfElements; i++) {
          cache.put(new Element(i, "value"));
        }
      }
      barrier.await();

      Thread.sleep(30 * 1000);

      while (cache.getSize() >= 600) {
        Thread.sleep(1000);
        System.out.println("XXXX client" + index + " size: " + cache.getSize());
      }

      System.out.println("XXXX client" + index + " final size: " + cache.getSize());
      long evictedElements = numOfElements - cache.getSize();

      barrier.await();

      Thread.sleep(30 * 1000);
      System.out.println("XXXX client" + index + ": "
                         + cache.getCacheEventNotificationService().getElementsEvictedCounter());

      this.evictedCount.addAndGet(cache.getCacheEventNotificationService().getElementsEvictedCounter());

      barrier.await();

      Assert.assertEquals("XXXX client " + index + " failed.", evictedElements, this.evictedCount.get());
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.getOrCreateSpec(App.class.getName()).addRoot("barrier", "barrier");
      config.getOrCreateSpec(App.class.getName()).addRoot("evictedCount", "evictedCount");

      String module_name = "tim-ehcache-2.x";
      TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
      config.addModule(timInfo.artifactId(), timInfo.version());
    }

    public void dispose() {
      // don't care
    }

    public void notifyElementEvicted(Ehcache cache, Element element) {
      System.out.println("Element [" + element + "] evicted");
    }

    public void notifyElementExpired(Ehcache cache, Element element) {
      // don't care
    }

    public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
      // don't care
    }

    public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
      // don't care
    }

    public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
      // don't care
    }

    public void notifyRemoveAll(Ehcache cache) {
      // don't care
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
      return super.clone();
    }
  }

}

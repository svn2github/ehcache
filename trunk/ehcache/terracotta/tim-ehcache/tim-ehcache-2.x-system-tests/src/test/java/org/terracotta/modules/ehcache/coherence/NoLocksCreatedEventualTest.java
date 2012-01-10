/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.coherence;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.terracotta.modules.BasicTimInfo;
import org.terracotta.modules.TimInfo;

import com.tc.management.beans.L2MBeanNames;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.locks.LockID;
import com.tc.objectserver.locks.LockMBean;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.stats.api.DSOMBean;
import com.tc.test.JMXUtils;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractTransparentApp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

import junit.framework.Assert;

/**
 * @author Abhishek Sanoujam
 */
public class NoLocksCreatedEventualTest extends TransparentTestBase {

  private static final int NODE_COUNT = 1;

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractTransparentApp {

    private final ApplicationConfig appConfig;

    public App(final String appId, final ApplicationConfig cfg, final ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      appConfig = cfg;
    }

    public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
      String testClass = App.class.getName();
      config.addIncludePattern(testClass + "$*", false, false, true);

      String moduleName = "tim-ehcache-2.x";
      TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", moduleName);
      config.addModule(timInfo.artifactId(), timInfo.version());
    }

    public void run() {
      DSOClientMBeanCoordinator coordinator = new DSOClientMBeanCoordinator();
      coordinator.startDSOClientMBeanCoordinator();

      CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/cache-coherence-test.xml"));
      Cache cache = cacheManager.getCache("non-strict-Cache");

      int initialLocks = coordinator.getLocks().size();
      for (int i = 0; i < 1000; i++) {
        cache.put(new Element("key" + i, "value" + i));
      }

      for (int i = 0; i < 5000; i++) {
        Element element = cache.get("key" + (i % 1000));
        Assert.assertNotNull(element);
        Assert.assertEquals("key" + (i % 1000), element.getKey());
        Assert.assertEquals("value" + (i % 1000), element.getValue());
      }

      Assert.assertEquals("No lock should have been created ", 0, (coordinator.getLocks().size() - initialLocks));

      for (int i = 0; i < 100; i++) {
        cache.acquireReadLockOnKey("key" + i);
      }
      Assert.assertEquals(100, (coordinator.getLocks().size() - initialLocks));

      for (int i = 100; i < 200; i++) {
        cache.acquireWriteLockOnKey("key" + i);
      }
      Assert.assertEquals(200, (coordinator.getLocks().size() - initialLocks));

      for (int i = 0; i < 5000; i++) {
        Element element = cache.get("key" + (i % 1000));
        Assert.assertNotNull(element);
        Assert.assertEquals("key" + (i % 1000), element.getKey());
        Assert.assertEquals("value" + (i % 1000), element.getValue());
      }
      Assert.assertEquals(1000, (coordinator.getLocks().size() - initialLocks));

    }

    private class DSOClientMBeanCoordinator {

      private DSOMBean              dsoMBean;
      private MBeanServerConnection mbsc;

      public void startDSOClientMBeanCoordinator() {
        try {
          JMXConnector jmxc = JMXUtils.getJMXConnector("localhost", Integer.valueOf(appConfig
              .getAttribute(ApplicationConfig.JMXPORT_KEY)));
          mbsc = jmxc.getMBeanServerConnection();
        } catch (IOException e) {
          throw new AssertionError(e);
        }
        dsoMBean = MBeanServerInvocationHandler.newProxyInstance(mbsc, L2MBeanNames.DSO, DSOMBean.class, false);
      }

      public List<LockID> getLocks() {
        ArrayList<LockID> rv = new ArrayList<LockID>();
        LockMBean[] locks = dsoMBean.getLocks();
        for (LockMBean lockMBean : locks) {
          rv.add(lockMBean.getLockID());
        }
        return rv;
      }
    }
  }

}

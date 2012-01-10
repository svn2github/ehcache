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
import com.tc.util.Assert;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractTransparentApp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

/**
 * @author Abhishek Sanoujam
 */
public class NoLocksCreatedTest extends TransparentTestBase {

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

      List<LockID> locks = coordinator.getLocks();
      System.out.println("Initial locks: " + locks);
      int initial = locks.size();
      CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/cache-coherence-test.xml"));
      Cache cache = cacheManager.getCache("strictCache");
      cache.setNodeCoherent(false);
      Assert.assertEquals(false, cache.isNodeCoherent());
      Assert.assertEquals(false, cache.isClusterCoherent());

      System.out.println("Adding 1000 elements in INCOHERENT mode...");
      long now = System.currentTimeMillis();
      for (int i = 0; i < 1000; i++) {
        cache.put(new Element("key" + i, "value" + i));
      }
      long t2 = System.currentTimeMillis();
      cache.setNodeCoherent(true);
      cache.waitUntilClusterCoherent();
      long incoherentPutTime = System.currentTimeMillis() - now;
      System.out.println("Time taken for 1000 puts: " + incoherentPutTime + " msecs. Time for setting node coherent: "
                         + (System.currentTimeMillis() - t2) + " msecs");
      Assert.assertTrue(cache.isNodeCoherent());
      Assert.assertTrue(cache.isClusterCoherent());

      List<LockID> locksAfterPuts = coordinator.getLocks();
      System.out.println("Number of locks:- initialCount: " + initial + " after INCOHERENT puts: "
                         + locksAfterPuts.size());
      // System.out.println("After put locks: " + locksAfterPuts);
      // we do create some CONCURRENT TXN locks, assert not more that 50 or so more locks created
      Assert.assertTrue((initial + 50) > locksAfterPuts.size());

      initial = locksAfterPuts.size();
      // get in incoherent mode
      cache.setNodeCoherent(false);
      now = System.currentTimeMillis();
      for (int i = 0; i < 5000; i++) {
        cache.get("key" + (i % 1000));
      }
      long incoherentGetTime = System.currentTimeMillis() - now;
      System.out.println("Time taken for 5000 INCOHERENT gets: " + incoherentGetTime + " msecs");
      List<LockID> locksAfterGets = coordinator.getLocks();
      System.out.println("Number of locks:- Before: " + initial + " After INCOHERENT gets: " + locksAfterGets.size());
      // System.out.println("Locks after INCOHERENT gets: " + locksAfterGets);
      // no locks should be created still
      Assert.assertEquals("No extra locks should have been created in INCOHERENT gets", initial, locksAfterGets.size());
      cache.setNodeCoherent(true);
      cache.waitUntilClusterCoherent();
      Assert.assertTrue(cache.isNodeCoherent());
      Assert.assertTrue(cache.isClusterCoherent());

      // do some gets in coherent mode,
      now = System.currentTimeMillis();
      for (int i = 0; i < 5000; i++) {
        cache.get("key" + (i % 1000));
      }
      long coherentGetTime = System.currentTimeMillis() - now;
      System.out.println("Time taken for 5000 COHERENT gets: " + coherentGetTime + " msecs");
      locksAfterGets = coordinator.getLocks();
      System.out.println("Number of locks:- Before: " + initial + " After COHERENT gets: " + locksAfterGets.size());
      // no locks should be created still
      Assert.assertEquals("Extra locks should have been created in COHERENT gets", initial + 1000,
                          locksAfterGets.size());

      // assert puts in coherent mode also creates locks
      initial = locksAfterGets.size();
      Assert.assertTrue(cache.isNodeCoherent());
      Assert.assertTrue(cache.isClusterCoherent());
      System.out.println("Adding 1000 elements in COHERENT mode...");
      now = System.currentTimeMillis();
      for (int i = 0; i < 1000; i++) {
        cache.put(new Element("new-key" + i, "value" + i));
      }
      long coherentPutTime = System.currentTimeMillis() - now;
      System.out.println("Time taken for 1000 puts(): " + coherentPutTime + " msecs");
      Assert.assertTrue(cache.isNodeCoherent());
      Assert.assertTrue(cache.isClusterCoherent());
      locksAfterPuts = coordinator.getLocks();
      System.out.println("Number of locks:- initialCount: " + initial + " after COHERENT puts: "
                         + locksAfterPuts.size());
      // System.out.println("After put locks: " + locksAfterPuts);
      // we do create some CONCURRENT TXN locks, assert not more that 50 or so more locks created
      Assert.assertEquals("Coherent puts should have created locks", initial + 1000, locksAfterPuts.size());
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

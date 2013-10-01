/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.coherence;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.test.util.JMXUtils;

import com.tc.management.beans.L2MBeanNames;
import com.tc.object.locks.LockID;
import com.tc.objectserver.locks.LockMBean;
import com.tc.properties.TCPropertiesConsts;
import com.tc.stats.api.DSOMBean;
import com.tc.test.config.model.TestConfig;

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
public class NoLocksCreatedTest extends AbstractCacheTestBase {

  public NoLocksCreatedTest(TestConfig testConfig) {
    super("cache-coherence-test.xml", testConfig, App.class);
    testConfig.addTcProperty(TCPropertiesConsts.L1_LOCKMANAGER_TIMEOUT_INTERVAL, "9000000");
  }

  public static class App extends ClientBase {

    public App(String[] args) {
      super("strictCache", args);
    }

    public static void main(String[] args) {
      new App(args).run();
    }

    @Override
    protected void runTest(Cache cache, Toolkit clusteringToolkit) throws Throwable {
      DSOClientMBeanCoordinator coordinator = new DSOClientMBeanCoordinator();
      coordinator.startDSOClientMBeanCoordinator();

      List<LockID> locks = coordinator.getLocks();
      System.out.println("Initial locks: " + locks);
      int initial = locks.size();
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
          JMXConnector jmxc = JMXUtils.getJMXConnector("localhost",
                                                       getTestControlMbean().getGroupsData()[0].getJmxPort(0));
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

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.coherence;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.api.ClusteringToolkit;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.test.util.JMXUtils;
import org.terracotta.test.util.TestBaseUtil;

import com.tc.management.TerracottaMBean;
import com.tc.management.beans.L2MBeanNames;
import com.tc.object.locks.LockID;
import com.tc.objectserver.locks.LockMBean;
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
public class NoLocksCreatedEventualTest extends AbstractCacheTestBase {

  public NoLocksCreatedEventualTest(TestConfig testConfig) {
    super("cache-coherence-test.xml", testConfig, App.class);
  }

  @Override
  protected String createClassPath(Class client) throws IOException {
    String classPath = super.createClassPath(client);
    return addToClasspath(classPath, TestBaseUtil.jarFor(TerracottaMBean.class));
  }

  public static class App extends ClientBase {

    public App(String[] args) {
      super("non-strict-Cache", args);
    }

    @Override
    protected void runTest(Cache cache, ClusteringToolkit clusteringToolkit) throws Throwable {
      DSOClientMBeanCoordinator coordinator = new DSOClientMBeanCoordinator();
      coordinator.startDSOClientMBeanCoordinator();

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

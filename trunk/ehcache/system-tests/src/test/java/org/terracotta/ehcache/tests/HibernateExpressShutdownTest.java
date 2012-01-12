/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import org.apache.derby.drda.NetworkServerControl;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.runtime.Vm;
import com.tctest.TransparentTestIface;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class HibernateExpressShutdownTest extends AbstractStandaloneCacheTest {
  private NetworkServerControl derbyServer;

  public HibernateExpressShutdownTest() {
    super("hibernate-shutdown-test.xml", HibernateShutdownClient1.class, HibernateShutdownClient2.class);
    setParallelClients(true);

    // JDK 1.5 perm gen collection is not reliable enough
    if (Vm.isJRockit() || Vm.isHotSpot() && Vm.isJDK15()) {
      disableTest();
    }
  }

  @Override
  protected Class getApplicationClass() {
    return App.class;
  }

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    super.doSetUp(t);
    startDerby();
  }

  private void startDerby() throws Exception {
    File derbyWorkDir = new File("derbydb", HibernateExpressShutdownTest.class.getSimpleName() + "-"
                                            + System.currentTimeMillis());
    if (!derbyWorkDir.exists() && !derbyWorkDir.mkdirs()) { throw new RuntimeException("Can't create derby work dir "
                                                                                       + derbyWorkDir.getAbsolutePath()); }
    System.setProperty("derby.system.home", derbyWorkDir.getAbsolutePath());
    derbyServer = new NetworkServerControl();
    derbyServer.start(new PrintWriter(System.out));
    int tries = 0;
    while (tries < 5) {
      try {
        Thread.sleep(500);
        derbyServer.ping();
        break;
      } catch (Exception e) {
        tries++;
      }
    }
    if (tries == 5) { throw new Exception("Failed to start Derby!"); }
  }

  @Override
  protected void tearDown() throws Exception {
    if (derbyServer != null) {
      derbyServer.shutdown();
    }
    super.tearDown();
  }

  public static class App extends AbstractStandaloneCacheTest.App {

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      if (Vm.isHotSpot()) {
        addClientJvmarg("-XX:MaxPermSize=96M");
        addClientJvmarg("-XX:+HeapDumpOnOutOfMemoryError");
        addClientJvmarg("-XX:SoftRefLRUPolicyMSPerMB=0");
      }
    }

    @Override
    protected List<String> getExtraJars() {
      List<String> extraJars = new ArrayList<String>();
      extraJars.add(jarFor(org.hibernate.SessionFactory.class));
      extraJars.add(jarFor(org.apache.commons.collections.Buffer.class));
      extraJars.add(jarFor(org.apache.derby.jdbc.ClientDriver.class));
      extraJars.add(jarFor(org.dom4j.Node.class));
      extraJars.add(jarFor(antlr.Tool.class));
      extraJars.add(jarFor(javassist.util.proxy.ProxyFactory.class));
      return extraJars;
    }
  }
}

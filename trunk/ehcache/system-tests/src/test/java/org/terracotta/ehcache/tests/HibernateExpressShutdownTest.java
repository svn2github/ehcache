/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import org.apache.derby.drda.NetworkServerControl;
import org.terracotta.test.util.TestBaseUtil;

import com.tc.test.config.model.TestConfig;
import com.tc.util.runtime.Vm;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class HibernateExpressShutdownTest extends AbstractCacheTestBase {
  private NetworkServerControl derbyServer;

  public HibernateExpressShutdownTest(TestConfig testConfig) {
    super("hibernate-shutdown-test.xml", testConfig, HibernateShutdownClient1.class, HibernateShutdownClient2.class);
    testConfig.getClientConfig().setParallelClients(true);

    // JDK 1.5 perm gen collection is not reliable enough
    if (Vm.isJRockit() || Vm.isHotSpot() && Vm.isJDK15()) {
      disableTest();
    }

    if (Vm.isHotSpot()) {
      getTestConfig().getClientConfig().addExtraClientJvmArg("-XX:MaxPermSize=96M");
      getTestConfig().getClientConfig().addExtraClientJvmArg("-XX:+HeapDumpOnOutOfMemoryError");
      getTestConfig().getClientConfig().addExtraClientJvmArg("-XX:SoftRefLRUPolicyMSPerMB=0");
    }
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
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
  public void tearDown() throws Exception {
    if (derbyServer != null) {
      derbyServer.shutdown();
    }
    super.tearDown();
  }

  @Override
  protected List<String> getExtraJars() {
    List<String> extraJars = new ArrayList<String>();
    extraJars.add(TestBaseUtil.jarFor(org.hibernate.SessionFactory.class));
    extraJars.add(TestBaseUtil.jarFor(org.apache.commons.collections.map.LRUMap.class));
    extraJars.add(TestBaseUtil.jarFor(org.apache.derby.jdbc.ClientDriver.class));
    extraJars.add(TestBaseUtil.jarFor(org.dom4j.Node.class));
    extraJars.add(TestBaseUtil.jarFor(antlr.Tool.class));
    extraJars.add(TestBaseUtil.jarFor(javassist.util.proxy.ProxyFactory.class));
    return extraJars;
  }
}

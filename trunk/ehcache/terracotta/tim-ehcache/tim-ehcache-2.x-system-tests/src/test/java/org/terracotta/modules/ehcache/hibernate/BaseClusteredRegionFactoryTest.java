/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.ehcache.hibernate;

import org.apache.commons.logging.LogFactory;
import org.apache.derby.drda.NetworkServerControl;
import org.apache.log4j.Logger;
import org.terracotta.modules.BasicTimInfo;
import org.terracotta.modules.TimInfo;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.AppServerInfo;
import com.tc.test.TestConfigObject;
import com.tc.test.server.appserver.deployment.AbstractTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.WARBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.util.TcConfigBuilder;

import java.io.File;
import java.io.PrintWriter;

import javax.transaction.Transaction;

import junit.framework.Assert;

public abstract class BaseClusteredRegionFactoryTest extends AbstractTwoServerDeploymentTest {

  private static final TimInfo TIM_EHCACHE_20 = new BasicTimInfo("tim-ehcache-2.x");

  public BaseClusteredRegionFactoryTest() {
    timebombTest("2012-01-21");
  }

  @Override
  public boolean shouldDisable() {
    return super.shouldDisable() || (appServerInfo().getId() == AppServerInfo.WASCE);
  }

  public void testHibernateCacheProvider() throws Exception {
    WebConversation conversation = new WebConversation();

    WebResponse response1 = hibernateRequest(server0, "server=server0", conversation);
    assertEquals("OK", response1.getText().trim());

    WebResponse response2 = hibernateRequest(server1, "server=server1", conversation);
    assertEquals("OK", response2.getText().trim());
  }

  private WebResponse hibernateRequest(WebApplicationServer server, String params, WebConversation con)
      throws Exception {
    return server.ping("/test/HibernateCacheTestServlet?" + params, con);
  }

  public static abstract class BaseClusteredCacheProviderTestSetup extends TwoServerTestSetup {

    private NetworkServerControl derbyServer;
    private final Class          testClass;

    protected BaseClusteredCacheProviderTestSetup(Class testClass, String configFile) {
      super(testClass, configFile, "test");
      this.testClass = testClass;
    }

    @Override
    protected final void configureWar(DeploymentBuilder builder) {
      builder.addDirectoryOrJARContainingClass(org.hibernate.SessionFactory.class); // hibernate*.jar
      builder.addDirectoryOrJARContainingClass(org.apache.commons.collections.Buffer.class); //
      builder.addDirectoryOrJARContainingClass(org.apache.derby.jdbc.ClientDriver.class); // derby*.jar
      builder.addDirectoryOrJARContainingClass(org.dom4j.Node.class); // domj4*.jar
      builder.addDirectoryOrJARContainingClass(antlr.Tool.class); // antlr*.jar
      builder.addDirectoryOrJARContainingClass(Assert.class);

      builder.addDirectoryOrJARContainingClass(classFor("javassist.util.proxy.ProxyFactory")); // java-assist
      builder.addDirectoryOrJARContainingClass(classFor("org.slf4j.LoggerFactory")); // sl4fj (api)
      builder.addDirectoryOrJARContainingClass(classFor("org.slf4j.impl.StaticLoggerBinder")); // sl4fj (impl)

      // EhCache Hibernate Cache
      builder.addDirectoryOrJARContainingClass(net.sf.ehcache.hibernate.EhCacheRegionFactory.class);
      builder
          .addDirectoryOrJARContainingClass(org.terracotta.modules.ehcache.store.TerracottaClusteredInstanceFactory.class);
      builder.addDirectoryOrJARContainingClass(org.terracotta.cache.DistributedCache.class);
      builder.addDirectoryOrJARContainingClass(org.terracotta.collections.ConcurrentDistributedMap.class);
      builder.addDirectoryOrJARContainingClass(org.terracotta.async.AsyncCoordinator.class);

      if (appServerInfo().getId() != AppServerInfo.JBOSS) {
        builder.addDirectoryOrJARContainingClass(Logger.class); // log4j
        builder.addDirectoryOrJARContainingClass(LogFactory.class); // common-loggings
      }

      builder.addResource("/hibernate-config/appserver/", "jboss-web.xml", "WEB-INF");
      builder.addResource("/hibernate-config/appserver/", "weblogic.xml", "WEB-INF");

      customizeWar(builder);

      builder.addServlet("HibernateCacheTestServlet", "/HibernateCacheTestServlet/*", getServletClass(), null, true);
    }

    private Class classFor(String className) {
      try {
        return Class.forName(className);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    protected void configureTcConfig(TcConfigBuilder clientConfig) {
      clientConfig.addModule(TIM_EHCACHE_20.artifactId(), TIM_EHCACHE_20.version());
    }

    private void addPackageToAppServerClassPath(Class clazz) {
      String path = WARBuilder.calculatePathToClass(clazz).getFile().getAbsolutePath();
      TestConfigObject.getInstance().addToAppServerClassPath(path);
    }

    @Override
    public final void setUp() throws Exception {
      // Tomcat is not a full J2EE application-server - we have to manually add the JTA classes to its classpath.
      if (appServerInfo().getId() == AppServerInfo.TOMCAT || appServerInfo().getId() == AppServerInfo.JETTY) {
        addPackageToAppServerClassPath(Transaction.class);
      }

      // To debug servlets:
      // System.setProperty("com.tc.test.server.appserver.deployment.GenericServer.ENABLE_DEBUGGER", "true");
      File derbyWorkDir = new File("derbydb", testClass.getSimpleName() + "-" + System.currentTimeMillis());
      if (!derbyWorkDir.exists() && !derbyWorkDir.mkdirs()) { throw new RuntimeException("Can't create derby work dir "
                                                                                         + derbyWorkDir
                                                                                             .getAbsolutePath()); }
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

      super.setUp();
    }

    @Override
    public final void tearDown() throws Exception {
      super.tearDown();
      derbyServer.shutdown();
    }

    protected abstract void customizeWar(DeploymentBuilder builder);

    protected abstract Class getServletClass();
  }
}

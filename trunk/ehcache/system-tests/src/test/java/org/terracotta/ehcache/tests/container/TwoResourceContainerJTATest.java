/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.container;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.tc.test.AppServerInfo;
import com.tc.test.server.appserver.deployment.AbstractStandaloneTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;

import junit.framework.Test;

public class TwoResourceContainerJTATest extends AbstractStandaloneTwoServerDeploymentTest {

  private static final String CONTEXT = "TwoResourceContainerJTATest";

  public TwoResourceContainerJTATest() {
    if (appServerInfo().getId() == AppServerInfo.JETTY || appServerInfo().getId() == AppServerInfo.TOMCAT
        || appServerInfo().getId() == AppServerInfo.WEBSPHERE) {
      // Jetty and Tomcat have no TM and we know the Websphere one is not compatible
      disableTest();
    }
  }

  public static Test suite() {
    return new TwoResourceContainerJTATestSetup();
  }

  public void testBasics() throws Exception {
    System.out.println("Running test");
    WebClient conversation = new WebClient();

    // do insert on server0
    WebResponse response1 = request(server0, "cmd=insert", conversation);
    assertEquals("OK", response1.getContentAsString().trim());

    // do query on server1
    response1 = request(server1, "cmd=query", conversation);
    assertEquals("OK", response1.getContentAsString().trim());
    System.out.println("Test finished");
  }

  private WebResponse request(WebApplicationServer server, String params, WebClient con) throws Exception {
    return server.ping("/" + CONTEXT + "/TwoResourceJTATestServlet?" + params, con).getWebResponse();
  }

  private static class TwoResourceContainerJTATestSetup extends AbstractStandaloneContainerJTATestSetup {

    public TwoResourceContainerJTATestSetup() {
      super(TwoResourceContainerJTATest.class, "two-resource-xa-appserver-test.xml", CONTEXT);
    }

    @Override
    protected void configureWar(DeploymentBuilder builder) {
      super.configureWar(builder);
      builder.addServlet("TwoResourceJTATestServlet", "/TwoResourceJTATestServlet/*", TwoResourceJTATestServlet.class,
                         null, false);
    }

  }

}

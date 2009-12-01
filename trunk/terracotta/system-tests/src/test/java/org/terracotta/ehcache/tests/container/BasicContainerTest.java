/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.container;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.server.appserver.deployment.AbstractStandaloneTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;

import junit.framework.Test;

public class BasicContainerTest extends AbstractStandaloneTwoServerDeploymentTest {

  private static final String CONTEXT = "BasicContainerTest";

  public static Test suite() {
    return new BasicContainerTestSetup();
  }

  public void testBasics() throws Exception {
    System.out.println("Running test");
    WebConversation conversation = new WebConversation();

    // do insert on server0
    WebResponse response1 = request(server0, "cmd=insert", conversation);
    assertEquals("OK", response1.getText().trim());

    // do query on server1
    response1 = request(server1, "cmd=query", conversation);
    assertEquals("OK", response1.getText().trim());
    System.out.println("Test finished");
  }

  private WebResponse request(WebApplicationServer server, String params, WebConversation con) throws Exception {
    return server.ping("/" + CONTEXT + "/BasicTestServlet?" + params, con);
  }

  private static class BasicContainerTestSetup extends AbstractStandaloneContainerTestSetup {

    public BasicContainerTestSetup() {
      super(BasicContainerTest.class, "basic-cache-test.xml", CONTEXT);
    }

    @Override
    protected void configureWar(DeploymentBuilder builder) {
      super.configureWar(builder);
      builder.addServlet("BasicTestServlet", "/BasicTestServlet/*", BasicTestServlet.class, null, false);
    }

  }

}

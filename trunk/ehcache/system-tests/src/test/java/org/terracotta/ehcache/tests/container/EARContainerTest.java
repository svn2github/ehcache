/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.container;

import net.sf.ehcache.Ehcache;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.taskdefs.Ear;
import org.apache.tools.ant.types.FileSet;
import org.codehaus.cargo.util.AntUtils;
import org.junit.Assert;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;
import org.terracotta.test.util.TestBaseUtil;
import org.terracotta.toolkit.Toolkit;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.tc.test.AppServerInfo;
import com.tc.test.TestConfigObject;
import com.tc.test.server.appserver.deployment.AbstractDeploymentTestCase;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.EARDeployment;
import com.tc.test.server.appserver.deployment.FileSystemPath;
import com.tc.test.server.appserver.deployment.ServerTestSetup;
import com.tc.test.server.appserver.deployment.WARBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;

public class EARContainerTest extends AbstractDeploymentTestCase {

  private static final String CONTEXT        = "BasicContainerTest";
  private static final String ehcacheConfig  = "basic-cache-test.xml";
  private static final String resourceAppXml = "application.xml";

  public static Test suite() {
    return new ServerTestSetup(EARContainerTest.class);
  }

  public EARContainerTest() {

    switch (appServerInfo().getId()) {
      case AppServerInfo.JETTY:
      case AppServerInfo.TOMCAT:
      case AppServerInfo.WASCE:
        disableTest();
        break;
      case AppServerInfo.JBOSS:
        if (Integer.valueOf(appServerInfo().getMajor()) < 6) {
          disableTest();
        }
        break;
      case AppServerInfo.GLASSFISH:
        if (appServerInfo().getMajor().equals("v1")) {
          disableTest();
        }
        break;
      case AppServerInfo.WEBSPHERE:
        if (Integer.valueOf(appServerInfo().getMajor()) < 7) {
          disableTest();
        }
        break;
      case AppServerInfo.WEBLOGIC:
        if (Integer.valueOf(appServerInfo().getMajor()) < 10) {
          disableTest();
        }
        break;
    }
  }

  private File getEhcacheConfig(String resourceConfig, int port) throws Exception {
    InputStream in = null;
    FileOutputStream out = null;

    try {
      in = getClass().getClassLoader().getResourceAsStream(resourceConfig);
      File rv = new File(getTempDirectory(), "ehcache.xml");
      out = new FileOutputStream(rv);
      String template = IOUtils.toString(in);
      String config = template.replace("PORT", String.valueOf(port));
      out.write(config.getBytes());
      return rv;
    } finally {
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(out);
    }
  }

  private File getApplicationXml(String resource, Set<String> earLibs) throws Exception {
    String modulesSection = "";
    for (String lib : earLibs) {
      String module = new File(lib).getName();
      if (appServerInfo().toString().startsWith("weblogic-12")) {
        module = "APP-INF/lib/" + module;
      }
      modulesSection += "  <module>\n " + "    <java>" + module + "</java>\n" + "  </module>\n";
    }
    InputStream in = null;
    FileOutputStream out = null;

    try {
      in = getClass().getClassLoader().getResourceAsStream(resource);
      File rv = new File(getTempDirectory(), "application.xml");
      out = new FileOutputStream(rv);
      String template = IOUtils.toString(in);
      String modulesText = modulesSection;
      if (appServerInfo().getId() == AppServerInfo.WEBSPHERE || appServerInfo().toString().startsWith("jboss-7")) {
        modulesText = "";
      }
      String appXml = template.replace("TOOLKIT_MODULE", modulesText);
      out.write(appXml.getBytes());
      return rv;
    } finally {
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(out);
    }
  }

  private File makeEar() throws Exception {
    DeploymentBuilder builder = new WARBuilder(CONTEXT + ".war", getTempDirectory(), TestConfigObject.getInstance(),
                                               false);
    builder.addDirectoryOrJARContainingClass(Assert.class); // junit
    builder.addServlet("BasicTestServlet", "/BasicTestServlet/*", BasicTestServlet.class, null, false);
    builder.addFileAsResource(getEhcacheConfig(ehcacheConfig, getServerManager().getServerTcConfig().getTsaPort()),
                              "WEB-INF/classes/");

    Set<String> earLibs = new HashSet<String>();
    earLibs.addAll(TestBaseUtil.getEhcacheDependencies(Ehcache.class));
    earLibs.addAll(TestBaseUtil.getToolkitRuntimeDependencies(Toolkit.class));
    earLibs.add(WARBuilder.calculatePathToClass(LoggerFactory.class).toString()); // slf4j-api
    earLibs.add(WARBuilder.calculatePathToClass(StaticLoggerBinder.class).toString()); // slf4j-log4j
    earLibs.add(WARBuilder.calculatePathToClass(org.apache.log4j.LogManager.class).toString()); // log4j

    File appXmlFile = getApplicationXml(resourceAppXml, earLibs);

    File earDir = new File(getTempDirectory(), "ear");
    earDir.mkdirs();

    String libDirName = "lib";
    if (appServerInfo().getId() == AppServerInfo.WEBLOGIC) {
      libDirName = "APP-INF/lib";
    }

    File lib = new File(earDir, libDirName);
    lib.mkdirs();

    FileUtils.copyFileToDirectory(builder.makeDeployment().getFileSystemPath().getFile(), earDir);

    for (String earLib : earLibs) {
      if (appServerInfo().getId() == AppServerInfo.WEBLOGIC || appServerInfo().getId() == AppServerInfo.WEBSPHERE
          || (appServerInfo().toString().startsWith("jboss-7"))) {
        FileUtils.copyFileToDirectory(new File(earLib), lib);
      } else {
        FileUtils.copyFileToDirectory(new File(earLib), earDir);
      }
    }

    FileSet fileSet = new FileSet();
    fileSet.setDir(earDir);
    fileSet.setIncludes("**/*.war,**/*.jar,**/lib/**,**/APP-INF/**,");

    File earFile = new File(earDir, "myapp.ear");

    Ear earTask = (Ear) new AntUtils().createAntTask("ear");
    earTask.setDestFile(earFile);
    earTask.setAppxml(appXmlFile);
    earTask.addFileset(fileSet);
    earTask.execute();

    return earFile;
  }

  public void testEar() throws Exception {
    File earFile = makeEar();
    File warDir = new File(getServerManager().getSandbox(), "war");
    warDir.mkdirs();
    if (appServerInfo().getId() == AppServerInfo.WEBSPHERE) {
      FileUtils.copyFileToDirectory(earFile, warDir);
    }

    EARDeployment ear = new EARDeployment(new FileSystemPath(earFile));
    WebApplicationServer server0 = getServerManager().makeWebApplicationServerNoDso();
    server0.addEarDeployment(ear);
    server0.start();

    // do insert on server0
    WebClient conversation = new WebClient();
    WebResponse response1 = request(server0, "cmd=insert", conversation);
    assertEquals("OK", response1.getContentAsString().trim());

  }

  private WebResponse request(WebApplicationServer server, String params, WebClient con) throws Exception {
    return server.ping("/" + CONTEXT + "/BasicTestServlet?" + params, con);
  }

}

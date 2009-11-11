/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All rights
 * reserved.
 */
package org.terracotta.ehcache.tests;

import net.sf.ehcache.CacheManager;

import org.apache.commons.io.IOUtils;

import com.tc.process.Exec;
import com.tc.process.Exec.Result;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.text.Banner;
import com.tc.util.runtime.Os;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public abstract class AbstractStandaloneCacheTest extends TransparentTestBase {

  public static final Date     ALL_TESTS_PASS_BY = new Date(109, Calendar.SEPTEMBER, 28);

  private static final boolean DEBUG_CLIENTS     = false;
  private static final String  SEP               = File.pathSeparator;
  private static final int     NODE_COUNT        = 1;

  private final String         configFile;

  AbstractStandaloneCacheTest(String configFile) {
    this.configFile = configFile;
  }

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.getTransparentAppConfig().setAttribute("PORT", new Integer(getDsoPort()));
    t.getTransparentAppConfig().setAttribute("TEMP", getTempDirectory());
    t.getTransparentAppConfig().setAttribute("CONFIG", configFile);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return AbstractStandaloneCacheTest.App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    private final File    tempDir;
    private final Integer port;
    private final String  configFile;
    private String        extraClientJvmargs = "";

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      this.tempDir = (File) cfg.getAttributeObject("TEMP");
      this.port = (Integer) cfg.getAttributeObject("PORT");
      this.configFile = cfg.getAttribute("CONFIG");
    }

    protected void evaluateClientOutput(String clientName, int exitCode, String clientOutput) throws AssertionError {
      if ((exitCode != 0) || !clientOutput.trim().contains("[PASS: " + clientName + "]")) { throw new AssertionError(clientOutput); }
    }

    @Override
    protected void runTest() throws Throwable {
      runClient(Client1.class);
      runClient(Client2.class);
    }

    protected void runClient(Class client) throws Throwable {
      runClient(client, true);
    }

    protected void runClient(Class client, boolean withStandaloneJar) throws Throwable {
      String test = jarFor(client);
      String standalone = jarFor(net.sf.ehcache.terracotta.StandaloneTerracottaStoreFactory.class);
      String ehcache = jarFor(CacheManager.class);
      String slf4jApi = jarFor(org.slf4j.LoggerFactory.class);
      String slf4jBinder = jarFor(org.slf4j.impl.StaticLoggerBinder.class);

      List<String> cmd = new ArrayList<String>();
      cmd.add(Exec.getJavaExecutable());

      if (DEBUG_CLIENTS) {
        int debugPort = 8000;
        cmd.add("-agentlib:jdwp=transport=dt_socket,suspend=y,server=y,address=" + debugPort);
        Banner.infoBanner("waiting for debugger to attach on port " + debugPort);
      }

      cmd.add("-Xms128m");
      cmd.add("-Xmx128m");
      cmd.add(extraClientJvmargs);

      cmd.add("-cp");
      if (withStandaloneJar) {
        cmd.add(makeClasspath(writeEhcacheConfigWithPort(configFile), test, standalone, ehcache, slf4jApi, slf4jBinder));
      } else {
        cmd.add(makeClasspath(writeEhcacheConfigWithPort(configFile), test, ehcache, slf4jApi, slf4jBinder));
      }
      cmd.add(client.getName());

      System.err.println("Starting client with cmd: " + cmd);
      File output = new File(tempDir, client.getSimpleName() + ".log");

      Result result = Exec.execute(cmd.toArray(new String[cmd.size()]), output.getAbsolutePath());

      evaluateClientOutput(client.getName(), result.getExitCode(), getFileContents(output));
    }

    /**
     * Read the ehcache.xml file as a resource, replace PORT token with appropriate port, write the ehcache.xml file back out to the temp
     * dir, and return the resulting resource directory.
     */
    private String writeEhcacheConfigWithPort(String resourcePath) throws IOException {
      return writeXmlFileWithPort(resourcePath, "ehcache-config.xml");
    }

    protected String writeXmlFileWithPort(String resourcePath, String outputName) throws IOException {
      // Slurp resourcePath file
      InputStream is = this.getClass().getClassLoader().getResourceAsStream(resourcePath);
      List<String> lines = IOUtils.readLines(is);

      // Replace PORT token
      for (int i = 0; i < lines.size(); i++) {
        String line = lines.get(i);
        lines.set(i, line.replace("PORT", Integer.toString(this.port)).replace("CONFIG", configFile).replace("TEMP",
                                                                                                             tempDir.getAbsolutePath()));
      }

      // Write
      File outputFile = new File(tempDir, outputName);
      FileOutputStream fos = new FileOutputStream(outputFile);
      IOUtils.writeLines(lines, IOUtils.LINE_SEPARATOR, fos);
      return tempDir.getAbsolutePath();
    }

    private String getFileContents(File f) throws IOException {
      FileInputStream in = null;
      try {
        in = new FileInputStream(f);
        return IOUtils.toString(in);
      } finally {
        IOUtils.closeQuietly(in);
      }
    }

    private String makeClasspath(String... jars) {
      String cp = "";
      for (String jar : jars) {
        cp += SEP + jar;
      }

      return cp;
    }
    
    public void addClientExtraJavaOps(String jvmarg) {
      extraClientJvmargs += " " + jvmarg;
    }

  }

  public static String jarFor(Class c) {
    ProtectionDomain protectionDomain = c.getProtectionDomain();
    CodeSource codeSource = protectionDomain.getCodeSource();
    URL url = codeSource.getLocation();
    String path = url.getPath();
    if (Os.isWindows() && path.startsWith("/")) {
      path = path.substring(1);
    }
    return URLDecoder.decode(path);
  }
}

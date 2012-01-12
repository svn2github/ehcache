/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.terracotta.StandaloneTerracottaClusteredInstanceFactory;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassWriter;
import org.terracotta.ehcache.tests.mbean.JMXUtils;
import org.terracotta.express.ClientFactory;

import bitronix.tm.TransactionManagerServices;

import com.atomikos.icatch.config.UserTransactionService;
import com.atomikos.icatch.config.imp.AbstractUserTransactionService;
import com.atomikos.icatch.jta.UserTransactionManager;
import com.tc.lcp.LinkedJavaProcess;
import com.tc.process.Exec;
import com.tc.process.Exec.Result;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.text.Banner;
import com.tc.util.runtime.Os;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.transaction.TransactionManager;

public abstract class AbstractStandaloneCacheTest extends TransparentTestBase {

  public static final Date     ALL_TESTS_PASS_BY = new Date(109, Calendar.SEPTEMBER, 28);

  private static final boolean DEBUG_CLIENTS     = Boolean.getBoolean("standalone.client.debug");
  private static final String  SEP               = File.pathSeparator;
  private static final int     NODE_COUNT        = 1;

  private final String         configFile;
  private final String         terracottaUrl;

  private final Class<?>[]     classes;
  private boolean              parallelClients   = false;

  public AbstractStandaloneCacheTest(String configFile) {
    this(configFile, "localhost:PORT", Client1.class, Client2.class);
  }

  public AbstractStandaloneCacheTest(String configFile, Class<?>... c) {
    this(configFile, "localhost:PORT", c);
  }

  public AbstractStandaloneCacheTest(String configFile, String terracottaUrl, Class<?>... c) {
    this.configFile = configFile;
    this.terracottaUrl = terracottaUrl;
    this.classes = c;
  }

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.getTransparentAppConfig().setAttribute("PORT", new Integer(getDsoPort()));
    t.getTransparentAppConfig().setAttribute("TEMP", getTempDirectory());
    t.getTransparentAppConfig().setAttribute("CONFIG", configFile);
    t.getTransparentAppConfig().setAttribute("TC_CONFIG_URL", terracottaUrl);
    t.getTransparentAppConfig().setAttribute("CLASSES", classes);
    t.getTransparentAppConfig().setAttribute("PARALLEL", Boolean.valueOf(parallelClients));
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return AbstractStandaloneCacheTest.App.class;
  }

  protected void setParallelClients(boolean parallelClients) {
    this.parallelClients = parallelClients;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    private final File       tempDir;
    private final Integer    port;
    private final String     configFile;
    private final String     terracottaUrl;
    private final Class<?>[] classes;
    private final boolean    parallel;
    private final Runner[]   runners;
    private final List       extraClientJvmargs = new ArrayList();

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      this.tempDir = (File) cfg.getAttributeObject("TEMP");
      this.port = (Integer) cfg.getAttributeObject("PORT");
      this.configFile = cfg.getAttribute("CONFIG");
      this.terracottaUrl = cfg.getAttribute("TC_CONFIG_URL");
      this.classes = (Class<?>[]) cfg.getAttributeObject("CLASSES");
      this.parallel = ((Boolean) cfg.getAttributeObject("PARALLEL")).booleanValue();
      this.runners = parallel ? new Runner[classes.length] : new Runner[] {};
    }

    protected void evaluateClientOutput(String clientName, int exitCode, File output) throws Throwable {
      if ((exitCode != 0)) { throw new AssertionError("Client " + clientName + " exited with exit code: " + exitCode); }

      FileReader fr = null;
      try {
        fr = new FileReader(output);
        BufferedReader reader = new BufferedReader(fr);
        String st = "";
        while ((st = reader.readLine()) != null) {
          if (st.contains("[PASS: " + clientName + "]")) return;
        }
        throw new AssertionError("Client " + clientName + " did not pass");
      } catch (Exception e) {
        throw new AssertionError(e);
      } finally {
        try {
          fr.close();
        } catch (Exception e) {
          //
        }
      }
    }

    @Override
    protected void runTest() throws Throwable {
      int index = 0;
      for (Class<?> c : classes) {
        if (!parallel) {
          runClient(c);
        } else {
          Runner runner = new Runner(c);
          runners[index++] = runner;
          runner.start();
        }
      }

      for (Runner runner : runners) {
        runner.finish();
      }
    }

    protected int getPort() {
      return this.port;
    }

    private final AtomicInteger clientIndex = new AtomicInteger(1);

    protected void runClient(Class client) throws Throwable {
      runClient(client, true);
    }

    protected void runClient(Class client, boolean withStandaloneJar) throws Throwable {
      runClient(client, withStandaloneJar, client.getSimpleName(), Collections.EMPTY_LIST);
    }

    protected void runClient(Class client, boolean withStandaloneJar, String clientName, List<String> extraClientArgs)
        throws Throwable {
      String test = jarFor(client);
      String standalone = jarFor(StandaloneTerracottaClusteredInstanceFactory.class);
      String ehcache = jarFor(CacheManager.class);
      String slf4jApi = jarFor(org.slf4j.LoggerFactory.class);
      String slf4jBinder = jarFor(org.slf4j.impl.StaticLoggerBinder.class);
      String log4j = jarFor(org.apache.log4j.LogManager.class);
      String cLogging = jarFor(org.apache.commons.logging.LogFactory.class);
      String asm = jarFor(ClassWriter.class); // needed for OtherClassloaderClient
      String jta = jarFor(TransactionManager.class);
      String oswego = jarFor(EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap.class);
      String junit = jarFor(org.junit.Assert.class);
      String expressRuntime = jarFor(ClientFactory.class);
      String linkedChild = jarFor(LinkedJavaProcess.class);
      String jmxUtil = jarFor(JMXUtils.class);

      List<String> jvmArgs = new ArrayList<String>();
      if (DEBUG_CLIENTS) {
        int debugPort = 9000 + (clientIndex.getAndIncrement());
        jvmArgs.add("-agentlib:jdwp=transport=dt_socket,suspend=y,server=y,address=" + debugPort);
        Banner.infoBanner("waiting for debugger to attach on port " + debugPort);
      }

      clientJVMMemorySettings(jvmArgs);
      jvmArgs.addAll(extraClientJvmargs);
      addTestTcPropertiesFile(jvmArgs);

      String classpath;
      if (withStandaloneJar) {
        classpath = makeClasspath(writeEhcacheConfigWithPort(configFile),
                                  writeXmlFileWithPort("log4j.xml", "log4j.xml"), test, junit, standalone,
                                  expressRuntime, ehcache, slf4jApi, slf4jBinder, log4j, cLogging, asm, jta, oswego,
                                  linkedChild, jmxUtil);
      } else {
        classpath = makeClasspath(writeEhcacheConfigWithPort(configFile),
                                  writeXmlFileWithPort("log4j.xml", "log4j.xml"), test, junit, expressRuntime, ehcache,
                                  slf4jApi, slf4jBinder, log4j, jta, cLogging, oswego, linkedChild, jmxUtil);
      }

      // do this last
      configureClientExtraJVMArgs(jvmArgs);
      checkDuplicateJVMArgs(jvmArgs);

      List<String> arguments = new ArrayList<String>();
      arguments.add(terracottaUrl.replace("PORT", Integer.toString(this.port)));
      arguments.addAll(extraClientArgs);

      LinkedJavaProcess clientProcess = new LinkedJavaProcess(client.getName(), arguments, jvmArgs);
      clientProcess.setClasspath(classpath);

      System.err.println("Starting client with jvmArgs: " + jvmArgs);
      System.err.println("LinkedJavaProcess arguments: " + arguments);

      String workDirPath = tempDir + "/" + clientName;
      File workDir;
      synchronized (AbstractStandaloneCacheTest.class) {
        workDir = new File(workDirPath);
        int index = 0;
        while (workDir.exists()) {
          String newWorkDirPath = workDirPath + "-" + index;
          System.err.println("Work directory already exists, trying: " + newWorkDirPath);
          workDir = new File(newWorkDirPath);
          index++;
        }
        if (!workDir.mkdirs()) { throw new AssertionError("Could not create " + workDir); }
      }
      File output = new File(workDir, clientName + ".log");

      System.out.println("client output file: " + output.getAbsolutePath());
      System.out.println("working directory: " + workDir.getAbsolutePath());

      clientProcess.setDirectory(workDir);

      clientProcess.start();
      Result result = Exec.execute(clientProcess, clientProcess.getCommand(), output.getAbsolutePath(), null, workDir);

      evaluateClientOutput(client.getName(), result.getExitCode(), output);
    }

    private void checkDuplicateJVMArgs(List<String> cmd) {
      Map<String, Set<String>> map = new HashMap<String, Set<String>>();
      for (String arg : cmd) {
        if (arg.indexOf("=") > 0) {
          String key = arg.substring(0, arg.indexOf("="));
          String value = arg.substring(arg.indexOf("=") + 1);
          Set<String> values = map.get(key);
          if (values == null) {
            values = new HashSet<String>();
            map.put(key, values);
          }
          values.add(value);
        }
      }
      System.out.println("Using client JVM args:");
      Set<String> ambiguousArgs = new HashSet<String>();
      for (Entry<String, Set<String>> entry : map.entrySet()) {
        System.out.println(entry.getKey() + " = " + entry.getValue());
        if (entry.getValue().size() != 1) {
          ambiguousArgs.add(entry.getKey());
        }
      }
      if (ambiguousArgs.size() > 0) { throw new AssertionError(
                                                               "Some JVM args have ambiguous values, correct your test to use only one value for the args: "
                                                                   + ambiguousArgs); }
    }

    protected void clientJVMMemorySettings(List<String> cmd) {
      cmd.add("-Xms128m");
      cmd.add("-Xmx128m");
    }

    protected void configureClientExtraJVMArgs(List<String> jvmArgs) {
      //
    }

    protected void addTestTcPropertiesFile(List<String> jvmArgs) {
      URL url = getClass().getResource("/com/tc/properties/tests.properties");
      if (url == null) {
        // System.err.println("\n\n ##### No tests.properties defined for this module \n\n");
        return;
      }
      String pathToTestTcProperties = url.getPath();
      if (pathToTestTcProperties == null || pathToTestTcProperties.equals("")) {
        // System.err.println("\n\n ##### No path to tests.properties defined \n\n");
        return;
      }
      // System.err.println("\n\n ##### -Dcom.tc.properties=" + pathToTestTcProperties + "\n\n");
      jvmArgs.add("-Dcom.tc.properties=" + pathToTestTcProperties);
    }

    /**
     * Read the ehcache.xml file as a resource, replace PORT token with appropriate port, write the ehcache.xml file
     * back out to the temp dir, and return the resulting resource directory.
     */
    protected String writeEhcacheConfigWithPort(String resourcePath) throws IOException {
      return writeXmlFileWithPort(resourcePath, "ehcache-config.xml");
    }

    protected String writeXmlFileWithPort(String resourcePath, String outputName) throws IOException {
      // Slurp resourcePath file
      resourcePath = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
      System.out.println("RESOURCE PATH: " + resourcePath);
      InputStream is = this.getClass().getResourceAsStream(resourcePath);
      List<String> lines = IOUtils.readLines(is);

      // Replace PORT token
      for (int i = 0; i < lines.size(); i++) {
        String line = lines.get(i);
        lines.set(i,
                  line.replace("PORT", Integer.toString(this.port)).replace("CONFIG", configFile)
                      .replace("TEMP", tempDir.getAbsolutePath()));
      }

      // Write
      File outputFile = new File(tempDir, outputName);
      FileOutputStream fos = new FileOutputStream(outputFile);
      IOUtils.writeLines(lines, IOUtils.LINE_SEPARATOR, fos);
      return tempDir.getAbsolutePath();
    }

    protected String makeClasspath(String... jars) {
      String cp = "";
      for (String jar : jars) {
        cp += SEP + jar;
      }

      for (String extra : getExtraJars()) {
        cp += SEP + extra;
      }

      return cp;
    }

    protected List<String> getExtraJars() {
      return Collections.emptyList();
    }

    public void addClientJvmarg(String jvmarg) {
      extraClientJvmargs.add(jvmarg);
    }

    protected class Runner extends Thread {

      private final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
      private final Class                      clientClass;
      private final String                     clientName;
      private final List<String>               extraClientArgs;

      public Runner(Class clientClass) {
        this(clientClass, clientClass.getSimpleName());
      }

      public Runner(Class clientClass, String clientName) {
        this.clientClass = clientClass;
        this.clientName = clientName;
        this.extraClientArgs = new ArrayList<String>();
      }

      @Override
      public void run() {
        try {
          runClient(clientClass, true, clientName, extraClientArgs);
        } catch (Throwable t) {
          error.set(t);
        }
      }

      public void finish() throws Throwable {
        join();
        Throwable t = error.get();
        if (t != null) throw t;
      }

      public void addClientArg(String arg) {
        extraClientArgs.add(arg);
      }
    }

  }

  public static class BTMApp extends AbstractStandaloneCacheTest.App {

    public BTMApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void runClient(Class client) throws Throwable {
      super.runClient(client, true);
    }

    @Override
    protected List<String> getExtraJars() {
      List<String> extraJars = new ArrayList<String>();
      extraJars.add(jarFor(TransactionManagerServices.class));
      return extraJars;
    }
  }

  public static class AtomikosApp extends AbstractStandaloneCacheTest.App {

    public AtomikosApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void runClient(Class client) throws Throwable {
      super.runClient(client, true);
    }

    @Override
    protected List<String> getExtraJars() {
      List<String> extraJars = new ArrayList<String>();
      extraJars.add(jarFor(UserTransactionManager.class));
      extraJars.add(jarFor(UserTransactionService.class));
      extraJars.add(jarFor(AbstractUserTransactionService.class));
      extraJars.add(jarFor(com.atomikos.diagnostics.Console.class));
      return extraJars;
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

  @Override
  protected boolean useExternalProcess() {
    return true;
  }
}

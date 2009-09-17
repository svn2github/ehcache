/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.agent;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.terracotta.agent.loader.Handler;
import org.terracotta.agent.loader.Jar;
import org.terracotta.agent.loader.JarManager;
import org.terracotta.agent.loader.Util;

import com.tc.aspectwerkz.transform.InstrumentationContext;
import com.tc.bundles.Repository;
import com.tc.bundles.VirtualTimRepository;
import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.L1TVSConfigurationSetupManager;
import com.tc.config.schema.setup.StandardTVSConfigurationSetupManagerFactory;
import com.tc.logging.NullTCLogger;
import com.tc.logging.TCLogger;
import com.tc.object.bytecode.hook.impl.DefaultWeavingStrategy;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.StandardDSOClientConfigHelperImpl;
import com.tc.object.logging.NullInstrumentationLogger;
import com.tc.plugins.ModulesLoader;
import com.terracottatech.config.Client;
import com.terracottatech.config.Module;
import com.terracottatech.config.Modules;
import com.terracottatech.config.TcConfigDocument;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Agent {

  private static final String           SIGAR_LIB_PATH_PROPERTY_NAME = "org.hyperic.sigar.path";

  private static final String           TIM_LIST                     = "/META-INF/terracotta/timlist";

  private static final AtomicBoolean    ACTIVATED                    = new AtomicBoolean(false);
  private static final List<Jar>        L1_JARS                      = Collections
                                                                         .synchronizedList(new ArrayList<Jar>());
  private static final List<Jar>        TIM_JARS                     = Collections
                                                                         .synchronizedList(new ArrayList<Jar>());
  private static final Map<String, URL> VIRTUAL_TIM_JARS             = new ConcurrentHashMap<String, URL>();

  private static final JarManager       JAR_MANAGER                  = new JarManager();

  public static Map<String, URL> getVirtualTIMs() {
    return Collections.unmodifiableMap(VIRTUAL_TIM_JARS);
  }

  public static URL[] getL1Jars() {
    return toURLs(L1_JARS);
  }

  public static URL[] getTIMJars() {
    return toURLs(TIM_JARS);
  }

  public static boolean wasActivated() {
    return ACTIVATED.get();
  }

  public static void premain(String agentArgs, Instrumentation inst) {
    // record that the agent was in fact started by the VM (and only once!)
    if (!ACTIVATED.compareAndSet(false, true)) { throw new AssertionError("activated more than once"); }

    System.setProperty("tc.active", "true");
    System.setProperty("tc.dso.globalmode", "false");

    URL source = Agent.class.getProtectionDomain().getCodeSource().getLocation();

    ZipInputStream agentJar = null;
    try {
      File sigarTmpDir = createTempDir("tmpSigarJars");
      agentJar = new ZipInputStream(source.openStream());
      for (ZipEntry entry = agentJar.getNextEntry(); entry != null; entry = agentJar.getNextEntry()) {
        if (entry.getName().startsWith("L1") && entry.getName().endsWith(".jar")) {
          URL l1Jar = new URL("jar:" + source.toExternalForm() + "!/" + entry.getName());
          L1_JARS.add(JAR_MANAGER.getOrCreate(l1Jar.toExternalForm(), l1Jar));
        } else if (entry.getName().startsWith("TIMs") && entry.getName().endsWith(".jar")) {
          String baseJar = baseName(entry);
          URL timJarUrl = new URL("jar:" + source.toExternalForm() + "!/" + entry.getName());
          Jar timJar = JAR_MANAGER.getOrCreate(timJarUrl.toExternalForm(), timJarUrl);
          TIM_JARS.add(timJar);
          VIRTUAL_TIM_JARS.put(baseJar, newTcJarUrl(timJarUrl));
        }
        // extract to tmp dir if sigar file
        if (entry.getName().toLowerCase().contains("sigar")) {
          handleSigarZipEntry(agentJar, entry, sigarTmpDir);
        }
      }
      System.setProperty(SIGAR_LIB_PATH_PROPERTY_NAME, sigarTmpDir.getAbsolutePath());
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    } finally {
      if (agentJar != null) {
        try {
          agentJar.close();
        } catch (IOException ioe) {
          // ignore
        }
      }
    }

    if (L1_JARS.isEmpty()) { throw new AssertionError("No terracotta L1 libraries found in agent"); }

    InputStream in = Agent.class.getResourceAsStream(TIM_LIST);
    if (in == null) {
      System.err.println("Missing resource: " + TIM_LIST);
      System.exit(1);
    }

    String[] timsToLoad;
    try {
      timsToLoad = readTimList(in);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    try {
      ClassLoader l1Loader = newL1Loader();
      Class<?> bootClass = l1Loader.loadClass(Boot.class.getName());
      Constructor<?> cstr = bootClass.getConstructor(String[].class, Map.class);
      Object boot = cstr.newInstance(timsToLoad, VIRTUAL_TIM_JARS);
      inst.addTransformer((ClassFileTransformer) boot);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static URL newTcJarUrl(URL embedded) throws MalformedURLException {
    return new URL(Handler.TC_JAR_PROTOCOL, "", -1, Handler.TAG + embedded.toExternalForm() + Handler.TAG + "/",
                   new Handler(JAR_MANAGER));
  }

  private static ClassLoader newL1Loader() {
    Map<String, byte[]> extraClasses = new HashMap<String, byte[]>();
    extraClasses.put(Boot.class.getName(), getBootClassBytes());

    ClassLoader loader = new AgentL1Loader(getL1Jars(), null, extraClasses);
    return loader;
  }

  private static byte[] getBootClassBytes() {
    ClassLoader loader = Agent.class.getClassLoader();
    String res = Boot.class.getName().replace('.', '/').concat(".class");
    try {
      return Util.extract(loader.getResourceAsStream(res));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void handleSigarZipEntry(ZipInputStream agentJar, ZipEntry entry, File sigarTmpDir) throws IOException {
    // extract only if this is for the current platform
    if (entry.getName().contains(baseLibraryName())) {
      extractSigarZipEntry(agentJar, entry, sigarTmpDir);
    }
  }

  private static void extractSigarZipEntry(ZipInputStream jar, ZipEntry entry, File outputDir) throws IOException {
    byte[] content = getCurrentZipEntry(jar);
    String outName = baseName(entry);

    // name is like libsigar-universal-macosx-1.6.3.dylib
    // need to strip off the version
    // Using sigar normally doesn't need to strip off the version,
    // current L1Loader does not work well with Sigar's way of loading
    // and fails to look up version of sigar based on the jar from where Sigar classes were loaded (by L1Loader)
    // see org.hyperic.jni.ArchLoader.findJarPath(String libName, boolean isRequired)
    // see org.hyperic.jni.ArchLoader.isJarURL(URL url) (which fails to set the sigar version)
    int index = outName.lastIndexOf('-');
    if (index > 0) {
      String ext = outName.substring(outName.lastIndexOf('.'));
      outName = outName.substring(0, index) + ext;
    }

    // dump the content at outputDir/outName
    File outFile = new File(outputDir, outName);
    writeFile(outFile, content);
    outFile.deleteOnExit();
  }

  private static void writeFile(File file, byte[] contents) throws IOException {
    FileOutputStream out = null;

    try {
      out = new FileOutputStream(file);
      out.write(contents);
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (IOException ioe) {
          // ignore
        }
      }
    }
  }

  private static byte[] getCurrentZipEntry(ZipInputStream zis) throws IOException {
    byte[] buf = new byte[1024];
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    int n;
    while ((n = zis.read(buf, 0, 1024)) > -1) {
      bout.write(buf, 0, n);
    }
    bout.close();
    return bout.toByteArray();
  }

  private static File createTempDir(String prefix) throws IOException {
    final File tempDir = File.createTempFile(prefix, Long.toString(System.nanoTime()));
    if (!(tempDir.delete())) { throw new IOException("Could not delete temp file: " + tempDir.getAbsolutePath()); }
    if (!(tempDir.mkdir())) { throw new IOException("Could not create temp directory: " + tempDir.getAbsolutePath()); }
    tempDir.deleteOnExit();
    return tempDir;
  }

  private static String[] readTimList(InputStream in) throws IOException {
    List<String> lines = new ArrayList<String>();
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(in));

      String line;
      while ((line = reader.readLine()) != null) {
        lines.add(line);
      }
      return lines.toArray(new String[lines.size()]);
    } finally {
      try {
        in.close();
      } catch (IOException ioe) {
        // ignore
      }
    }
  }

  private static String baseName(ZipEntry entry) {
    return new File(entry.getName()).getName();
  }

  private static URL[] toURLs(List<Jar> jars) {
    Jar[] jarArray = jars.toArray(new Jar[] {});
    URL[] urls = new URL[jarArray.length];
    for (int i = 0; i < jarArray.length; i++) {
      try {
        urls[i] = newTcJarUrl(jarArray[i].getSource());
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    }

    return urls;
  }

  // Adapted from Sigar's ArchName.java
  private static String baseLibraryName() {
    String name = System.getProperty("os.name");
    String arch = System.getProperty("os.arch");
    String version = System.getProperty("os.version");
    String majorVersion = version.substring(0, 1); // 4.x, 5.x, etc.

    StringBuffer buf = new StringBuffer();

    if (arch.endsWith("86")) {
      arch = "x86";
    }

    if (name.equals("Linux")) {
      buf.append(arch).append("-linux");
    } else if (name.indexOf("Windows") > -1) {
      buf.append(arch).append("-winnt");
    } else if (name.equals("SunOS")) {
      if (arch.startsWith("sparcv") && "64".equals(System.getProperty("sun.arch.data.model"))) {
        arch = "sparc64";
      }
      buf.append(arch).append("-solaris");
    } else if (name.equals("HP-UX")) {
      if (arch.startsWith("IA64")) {
        arch = "ia64";
      } else {
        arch = "pa";
      }
      if (version.indexOf("11") > -1) {
        buf.append(arch).append("-hpux-11");
      }
    } else if (name.equals("AIX")) {
      buf.append("ppc-aix-").append(majorVersion);
    } else if (name.equals("Mac OS X")) {
      buf.append("universal-macosx");
    } else if (name.equals("FreeBSD")) {
      // none of the 4,5,6 major versions are binary compatible
      buf.append(arch).append("-freebsd-").append(majorVersion);
    } else if (name.equals("OpenBSD")) {
      buf.append(arch).append("-openbsd-").append(majorVersion);
    } else if (name.equals("NetBSD")) {
      buf.append(arch).append("-netbsd-").append(majorVersion);
    } else if (name.equals("OSF1")) {
      buf.append("alpha-osf1-").append(majorVersion);
    } else if (name.equals("NetWare")) {
      buf.append("x86-netware-").append(majorVersion);
    }

    if (buf.length() == 0) {
      return null;
    } else {
      String prefix = "libsigar-";
      if (name.startsWith("Windows")) {
        prefix = "sigar-";
      }
      return prefix + buf.toString();
    }
  }

  public static class Boot implements ClassFileTransformer {

    private final DefaultWeavingStrategy weavingStrategy;
    private final DSOClientConfigHelper  clientConfig;

    public Boot(String[] tims, Map<String, URL> virtualTimJars) throws Exception {
      Collection<Repository> repo = new ArrayList<Repository>();
      repo.add(new VirtualTimRepository(virtualTimJars));
      clientConfig = new StandardDSOClientConfigHelperImpl(createConfigSetupManager(tims), false);
      ModulesLoader.initModules(clientConfig, null, true, repo);

      // XXX: use a non-null instrumentation logger backed by JDK logging?
      weavingStrategy = new DefaultWeavingStrategy(clientConfig, new NullInstrumentationLogger(), true);
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
      try {
        return doTransform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
      } catch (Throwable t) {
        // exceptions thrown from this method are squelched, so at least print them
        t.printStackTrace();
        return null;
      }
    }

    private byte[] doTransform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                               ProtectionDomain protectionDomain, byte[] classfileBuffer) {
      if (loader instanceof AgentL1Loader) { return null; }
      if (loader == null || className.startsWith("java.") || className.startsWith("javax.")) { return null; }

      className = className.replace('/', '.');

      InstrumentationContext context = new InstrumentationContext(className, classfileBuffer, loader);
      weavingStrategy.transform(className, context);
      return context.getCurrentBytecode();
    }

    private File buildTcConfigFile(String[] tims) throws Exception {
      TcConfigDocument doc = TcConfigDocument.Factory.newInstance();
      TcConfig tcConfig = doc.addNewTcConfig();
      Client clients = tcConfig.addNewClients();
      Modules modules = clients.addNewModules();

      for (String tim : tims) {
        Module module = modules.addNewModule();
        module.setName(tim);
      }

      File tmpFile = File.createTempFile("tc-config", ".xml");
      tmpFile.deleteOnExit();
      writeFile(tmpFile, doc.toString().getBytes());

      return tmpFile;
    }

    private L1TVSConfigurationSetupManager createConfigSetupManager(String[] tims) throws Exception {
      File tcConfigFile = buildTcConfigFile(tims);

      Option configFileOption = new Option("f", "config", true, "configuration file (optional)");
      configFileOption.setArgName("file-or-URL");
      configFileOption.setType(String.class);
      configFileOption.setRequired(false);

      Options options = new Options();
      options.addOption(configFileOption);

      String[] newArgs = new String[2];
      newArgs[newArgs.length - 2] = "-f";
      newArgs[newArgs.length - 1] = tcConfigFile.getAbsolutePath();
      CommandLine cmdLine = new PosixParser().parse(options, newArgs);

      StandardTVSConfigurationSetupManagerFactory factory;
      factory = new StandardTVSConfigurationSetupManagerFactory(cmdLine, false,
                                                                new FatalIllegalConfigurationChangeHandler());

      // XXX: use JDK logger here?
      TCLogger logger = new NullTCLogger();
      L1TVSConfigurationSetupManager setupManager = factory.createL1TVSConfigurationSetupManager(logger);

      // okay to try to delete temp file now
      tcConfigFile.delete();

      return setupManager;
    }
  }

}

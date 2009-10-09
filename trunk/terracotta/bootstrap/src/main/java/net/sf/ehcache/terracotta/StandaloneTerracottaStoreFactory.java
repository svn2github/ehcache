package net.sf.ehcache.terracotta;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.TerracottaConfigConfiguration;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.store.StoreFactory;

import org.terracotta.agent.loader.Handler;
import org.terracotta.agent.loader.Jar;
import org.terracotta.agent.loader.JarManager;
import org.terracotta.agent.loader.Util;

import com.tc.object.bytecode.ManagerUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class StandaloneTerracottaStoreFactory implements StoreFactory {

  private static final String SIGAR_LIB_PATH_PROPERTY_NAME = "org.hyperic.sigar.path";

  private static String       computedBaseLibraryName;

  private final JarManager    jarManager                   = new JarManager();
  private final StoreFactory  realFactory;

  public StandaloneTerracottaStoreFactory(final TerracottaConfigConfiguration terracottaConfig) {
    testForBootJar();

    System.setProperty("tc.active", "true");
    System.setProperty("tc.dso.globalmode", "false");

    URL source = getClass().getProtectionDomain().getCodeSource().getLocation();
    if (!source.toExternalForm().endsWith(".jar")) {
      // code source may return upto the class and not the containing jar
      String extForm = source.toExternalForm();
      if (extForm.startsWith("jar:") && extForm.endsWith(".class") && extForm.contains(".jar!")) {
        // rip the jar protocol, use the jar directly
        extForm = extForm.substring(4, extForm.lastIndexOf(".jar!")) + ".jar";
        try {
          source = new URL(extForm);
        } catch (MalformedURLException e) {
          throw new CacheException(e);
        }
      }
    }
    URL bootJarUrl = null;
    List<Jar> l1Jars = Collections.synchronizedList(new ArrayList<Jar>());
    List<Jar> timJars = Collections.synchronizedList(new ArrayList<Jar>());
    Map<String, URL> virtualTimJars = new ConcurrentHashMap<String, URL>();

    ZipInputStream standaloneJar = null;
    try {

      File sigarTmpDir = createTempDir("tmpSigarJars");
      standaloneJar = new ZipInputStream(source.openStream());
      for (ZipEntry entry = standaloneJar.getNextEntry(); entry != null; entry = standaloneJar.getNextEntry()) {
        if (entry.getName().startsWith("L1") && entry.getName().endsWith(".jar")) {
          URL l1Jar = new URL("jar:" + source.toExternalForm() + "!/" + entry.getName());
          l1Jars.add(jarManager.getOrCreate(l1Jar.toExternalForm(), l1Jar));
        } else if (entry.getName().startsWith("TIMs") && entry.getName().endsWith(".jar")) {
          String baseJar = baseName(entry);
          URL timJarUrl = new URL("jar:" + source.toExternalForm() + "!/" + entry.getName());
          Jar timJar = jarManager.getOrCreate(timJarUrl.toExternalForm(), timJarUrl);
          timJars.add(timJar);
          virtualTimJars.put(baseJar, newTcJarUrl(timJarUrl));
        } else if (entry.getName().equals("dso-boot.jar")) {
          bootJarUrl = new URL("jar:" + source.toExternalForm() + "!/" + entry.getName());
          jarManager.getOrCreate(bootJarUrl.toExternalForm(), bootJarUrl);
        } else if (entry.getName().equals("exported-classes.jar")) {
          URL exports = new URL("jar:" + source.toExternalForm() + "!/" + entry.getName());
          timJars.add(jarManager.getOrCreate(exports.toExternalForm(), exports));
        }

        // extract to tmp dir if sigar file
        if (entry.getName().toLowerCase().contains("sigar")) {
          handleSigarZipEntry(standaloneJar, entry, sigarTmpDir);
        }
      }
      System.setProperty(SIGAR_LIB_PATH_PROPERTY_NAME, sigarTmpDir.getAbsolutePath());
    } catch (IOException ioe) {
      throw new CacheException(ioe);
    } finally {
      if (standaloneJar != null) {
        try {
          standaloneJar.close();
        } catch (IOException ioe) {
          // ignore
        }
      }
    }

    final boolean isURLConfig = terracottaConfig.isUrlConfig();
    String tcConfig = null;
    if (isURLConfig) {
      tcConfig = terracottaConfig.getUrl();
    } else {
      tcConfig = terracottaConfig.getEmbeddedConfig();
    }

    // depending on how things get factored this might be a jar resource like it is in the hibernate agent
    String[] timsToLoad = new String[] { "tim-ehcache-1.7" };

    ClassLoader bootJarLoader = new URLClassLoader(new URL[] { newTcJarUrl(bootJarUrl) }, null);

    ClassLoader newL1Loader = newL1Loader(l1Jars, bootJarLoader);
    AppLevelTIMLoader appLevelTimLoader = new AppLevelTIMLoader(toURLs(timJars), bootJarLoader, getClass()
        .getClassLoader());
    try {
      Class boot = newL1Loader.loadClass(StandaloneL1Boot.class.getName());
      Constructor<?> cstr = boot.getConstructor(String[].class, Map.class, String.class, Boolean.TYPE,
                                                ClassLoader.class);
      Callable<ClassFileTransformer> call = (Callable<ClassFileTransformer>) cstr.newInstance(timsToLoad,
                                                                                              virtualTimJars, tcConfig,
                                                                                              isURLConfig,
                                                                                              appLevelTimLoader);
      ClassFileTransformer dsoContext = call.call();
      appLevelTimLoader.setTransformer(dsoContext);

      Class factoryClass = appLevelTimLoader.loadClass("org.terracotta.modules.ehcache.store.TerracottaStoreFactory");
      Constructor factoryClassConstructor = factoryClass
          .getConstructor(new Class[] { TerracottaConfigConfiguration.class });
      realFactory = (StoreFactory) factoryClassConstructor.newInstance(terracottaConfig);
    } catch (Exception e) {
      throw new CacheException(e);
    }
  }

  private static void handleSigarZipEntry(final ZipInputStream agentJar, final ZipEntry entry, final File sigarTmpDir)
      throws IOException {
    // extract only if this is for the current platform
    if (entry.getName().contains(baseLibraryName())) {
      extractSigarZipEntry(agentJar, entry, sigarTmpDir);
    }
  }

  private static void extractSigarZipEntry(final ZipInputStream jar, final ZipEntry entry, final File outputDir)
      throws IOException {
    byte[] content = getCurrentZipEntry(jar);
    String outName = baseName(entry);

    // no need to strip off the version (like in hibernate-agent)

    // dump the content at outputDir/outName
    File outFile = new File(outputDir, outName);
    writeFile(outFile, content);
    outFile.deleteOnExit();
  }

  private static void writeFile(final File file, final byte[] contents) throws IOException {
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

  private static byte[] getCurrentZipEntry(final ZipInputStream zis) throws IOException {
    byte[] buf = new byte[1024];
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    int n;
    while ((n = zis.read(buf, 0, 1024)) > -1) {
      bout.write(buf, 0, n);
    }
    bout.close();
    return bout.toByteArray();
  }

  private static File createTempDir(final String prefix) throws IOException {
    final File tempDir = File.createTempFile(prefix, Long.toString(System.nanoTime()));
    if (!(tempDir.delete())) { throw new IOException("Could not delete temp file: " + tempDir.getAbsolutePath()); }
    if (!(tempDir.mkdir())) { throw new IOException("Could not create temp directory: " + tempDir.getAbsolutePath()); }
    tempDir.deleteOnExit();
    return tempDir;
  }

  public Store create(final Ehcache cache) {
    return realFactory.create(cache);
  }

  private static void testForBootJar() {
    try {
      Class c = Class.forName(ManagerUtil.class.getName());
      if (c.getClassLoader() == null) {
        // ManagerUtil is in standalone hibernate jar so it might be visible to us here, but it should not be defined by
        // the null loader in either case
        throw new CacheException(
                                 "The Terracotta dso-boot.jar is specified via -Xbootclasspath. This is not a correct configuration, please remove it");
      }

    } catch (ClassNotFoundException cnfe) {
      // expected
    } catch (NoClassDefFoundError ncdfe) {
      // expected
    }
  }

  private ClassLoader newL1Loader(final List<Jar> l1Jars, final ClassLoader bootJarLoader) {
    Map<String, byte[]> extraClasses = new HashMap<String, byte[]>();
    extraClasses.put(StandaloneL1Boot.class.getName(), getBootClassBytes());

    ClassLoader loader = new L1Loader(toURLs(l1Jars), bootJarLoader, extraClasses);
    return loader;

  }

  private URL[] toURLs(final List<Jar> jars) {
    Jar[] jarArray = jars.toArray(new Jar[] {});
    URL[] urls = new URL[jarArray.length];
    for (int i = 0; i < jarArray.length; i++) {
      urls[i] = newTcJarUrl(jarArray[i].getSource());
    }

    return urls;
  }

  private byte[] getBootClassBytes() {
    ClassLoader loader = getClass().getClassLoader();
    String res = StandaloneL1Boot.class.getName().replace('.', '/').concat(".class");
    try {
      return Util.extract(loader.getResourceAsStream(res));
    } catch (IOException e) {
      throw new CacheException(e);
    }
  }

  private static String baseName(final ZipEntry entry) {
    return new File(entry.getName()).getName();
  }

  private URL newTcJarUrl(final URL embedded) {
    try {
      return new URL(Handler.TC_JAR_PROTOCOL, "", -1, Handler.TAG + embedded.toExternalForm() + Handler.TAG + "/",
                     new Handler(jarManager));
    } catch (MalformedURLException e) {
      throw new CacheException(e);
    }
  }

  // Adapted from Sigar's ArchName.java
  private static String baseLibraryName() {
    if (computedBaseLibraryName != null) return computedBaseLibraryName;

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
      computedBaseLibraryName = prefix + buf.toString();
      return computedBaseLibraryName;
    }
  }

}

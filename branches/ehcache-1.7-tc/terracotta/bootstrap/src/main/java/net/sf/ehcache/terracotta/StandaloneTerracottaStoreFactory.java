package net.sf.ehcache.terracotta;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.store.StoreFactory;

import org.terracotta.agent.loader.Handler;
import org.terracotta.agent.loader.Jar;
import org.terracotta.agent.loader.JarManager;
import org.terracotta.agent.loader.Util;

import com.tc.object.bytecode.ManagerUtil;

import java.io.File;
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

  private final JarManager   jarManager = new JarManager();
  private final StoreFactory realFactory;

  public StandaloneTerracottaStoreFactory() {
    testForBootJar();

    System.setProperty("tc.active", "true");
    System.setProperty("tc.dso.globalmode", "false");

    URL source = getClass().getProtectionDomain().getCodeSource().getLocation();
    URL bootJarUrl = null;
    List<Jar> l1Jars = Collections.synchronizedList(new ArrayList<Jar>());
    List<Jar> timJars = Collections.synchronizedList(new ArrayList<Jar>());
    Map<String, URL> virtualTimJars = new ConcurrentHashMap<String, URL>();

    ZipInputStream standaloneJar = null;
    try {
      // XXX: SIGAR STUFF!!!!

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
      }
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

    final String tcConfig;
    final boolean isURLConfig = true; // XXX: cache.getCacheConfiguration().isTerracottaConfigURL();
    // XXX: String tcConfig = cache.getCacheConfiguration().getTerracottaConfig();
    if (System.getProperty("HACK-DSO-PORT") != null) {
      // system tests are doing this now
      tcConfig = "localhost:" + System.getProperty("HACK-DSO-PORT");
    } else {
      tcConfig = "localhost:9510";
    }

    // depending on how things get factored this might be a jar resource like it is in the hibernate agent
    String[] timsToLoad = new String[] { "tim-ehcache-store" };

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

      realFactory = (StoreFactory) appLevelTimLoader
          .loadClass("org.terracotta.modules.ehcache.store.TerracottaStoreFactory").newInstance();
    } catch (Exception e) {
      throw new CacheException(e);
    }
  }

  public Store create(Ehcache cache) {
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

  private ClassLoader newL1Loader(List<Jar> l1Jars, ClassLoader bootJarLoader) {
    Map<String, byte[]> extraClasses = new HashMap<String, byte[]>();
    extraClasses.put(StandaloneL1Boot.class.getName(), getBootClassBytes());

    ClassLoader loader = new L1Loader(toURLs(l1Jars), bootJarLoader, extraClasses);
    return loader;

  }

  private URL[] toURLs(List<Jar> jars) {
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

  private static String baseName(ZipEntry entry) {
    return new File(entry.getName()).getName();
  }

  private URL newTcJarUrl(URL embedded) {
    try {
      return new URL(Handler.TC_JAR_PROTOCOL, "", -1, Handler.TAG + embedded.toExternalForm() + Handler.TAG + "/",
                     new Handler(jarManager));
    } catch (MalformedURLException e) {
      throw new CacheException(e);
    }
  }

}

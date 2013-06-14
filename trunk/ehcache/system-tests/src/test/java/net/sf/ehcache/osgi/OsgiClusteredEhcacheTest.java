/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package net.sf.ehcache.osgi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.bootDelegationPackages;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.terracotta.test.OsgiUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * @author hhuynh
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class OsgiClusteredEhcacheTest {
  private int tsaPort;

  @Before
  public void before() throws Exception {

    URLClassLoader urlClassLoader = new URLClassLoader(toUrls(System.getProperty("maven.test.class.path"))
        .toArray(new URL[0]));

    // PortChooser
    Class<?> portChooserClass = urlClassLoader.loadClass("com.tc.util.PortChooser");
    Object portChooser = portChooserClass.newInstance();
    Method chooseRandomPort = portChooserClass.getMethod("chooseRandomPort", (Class<?>[]) null);
    tsaPort = ((Integer) chooseRandomPort.invoke(portChooser, (Object[]) null)).intValue();
    int jmxPort = ((Integer) chooseRandomPort.invoke(portChooser, (Object[]) null)).intValue();

    // construct ExtraProcessServerControl(String host, int tsaPort, int adminPort, String configFileLoc, boolean
    // mergeOutput)
    // and then call start()
    Class<?> serverControlClass = urlClassLoader.loadClass("com.tc.objectserver.control.ExtraProcessServerControl");
    Constructor<?> constructor = serverControlClass.getConstructor(String.class, int.class, int.class, String.class,
                                                                   boolean.class);
    Object serverControl = constructor.newInstance("localhost", tsaPort, jmxPort, getDefaultTcConfig(tsaPort, jmxPort),
                                                   true);
    Method startMethod = serverControlClass.getMethod("start", (Class<?>[]) null);
    startMethod.invoke(serverControl, (Object[]) null);
  }

  private String getDefaultTcConfig(int tsaport, int jmxPort) throws Exception {
    String config = readResourceAsString("/net/sf/ehcache/osgi/default-tc-config.xml");
    config = config.replace("TSAPORT", String.valueOf(tsaport));
    config = config.replace("JMXPORT", String.valueOf(jmxPort));
    File configFile = new File("temp/OsgiClusteredEhcacheTest/tc-config.xml");
    configFile.getParentFile().mkdirs();
    PrintWriter writer = new PrintWriter(configFile);
    try {
      writer.println(config);
      return configFile.getCanonicalPath();
    } finally {
      writer.close();
    }
  }

  private String readResourceAsString(String resource) throws Exception {
    String newline = System.getProperty("line.separator");
    InputStream configStream = OsgiClusteredEhcacheTest.class.getResourceAsStream(resource);
    try {
      StringBuilder sb = new StringBuilder();
      BufferedReader reader = new BufferedReader(new InputStreamReader(configStream));
      String line = null;
      while ((line = reader.readLine()) != null) {
        sb.append(line).append(newline);
      }
      return sb.toString();
    } finally {
      if (configStream != null) {
        configStream.close();
      }
    }
  }

  private List<URL> toUrls(String classpath) throws Exception {
    List<URL> elements = new ArrayList<URL>();
    if (classpath.contains("surefirebooter")) {
      JarFile surefireBooter = new JarFile(classpath);
      Manifest manifest = surefireBooter.getManifest();
      classpath = manifest.getMainAttributes().getValue("Class-path");
      surefireBooter.close();
      for (String urlElement : classpath.split(" ")) {
        elements.add(new URI(urlElement).toURL());
      }
    } else {
      for (String path : classpath.split(File.pathSeparator)) {
        elements.add(new File(path).toURI().toURL());
      }
    }
    return elements;
  }

  @org.ops4j.pax.exam.Configuration
  public Option[] config() {
    return options(bootDelegationPackages("sun.*,javax.naming,javax.naming.spi,javax.naming.event,javax.management,javax.net.ssl,javax.management.remote.misc"),
                   OsgiUtil.commonOptions(),
                   OsgiUtil.getMavenBundle("org.terracotta", "terracotta-toolkit-runtime-ee",
                                           "terracotta-toolkit-runtime"),
                   OsgiUtil.getMavenBundle("net.sf.ehcache", "ehcache-ee", "ehcache"),
                   systemProperty("tc.base-dir").value(System.getProperty("tc.base-dir")),
                   systemProperty("tc.dso.globalmode").value(System.getProperty("tc.dso.globalmode")),
                   systemProperty("tc.tests.info.property-files")
                       .value(System.getProperty("tc.tests.info.property-files")), systemProperty("com.tc.properties")
                       .value(System.getProperty("com.tc.properties")),
                   systemProperty("maven.test.class.path").value(System.getProperty("java.class.path")));
  }

  @Test
  public void testClusteredCache() throws Exception {
    Configuration configuration = new Configuration()
        .terracotta(new TerracottaClientConfiguration().url("localhost:" + tsaPort))
        .defaultCache(new CacheConfiguration("defaultCache", 100))
        .cache(new CacheConfiguration("clusteredCache", 100).timeToIdleSeconds(5).timeToLiveSeconds(120)
                   .terracotta(new TerracottaConfiguration()));
    CacheManager manager = new CacheManager(configuration);
    Cache cache = manager.getCache("clusteredCache");
    Element element = new Element("key1", "value1");
    cache.put(element);
    Element element1 = cache.get("key1");
    assertEquals("value1", element1.getObjectValue());
    assertEquals(1, cache.getSize());
    assertTrue(cache.isTerracottaClustered());
  }
}

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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.terracotta.test.OsgiUtil;

import java.io.File;
import java.net.URLClassLoader;

/**
 * @author hhuynh
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class OsgiClusteredEhcacheTest {
  private int     tsaPort;
  private Object  l2ServerControl;

  @Rule
  public TestName testName = new TestName();

  @Before
  public void before() throws Exception {

    URLClassLoader urlClassLoader = new URLClassLoader(OsgiUtil.toUrls(System.getProperty("maven.test.class.path")));

    // randomize ports
    Object portChooser = OsgiUtil.getPortChooser(urlClassLoader);
    tsaPort = OsgiUtil.chooseRandomPort(portChooser);
    int jmxPort = OsgiUtil.chooseRandomPort(portChooser);

    // start L2
    l2ServerControl = OsgiUtil
        .getL2ServerControl(urlClassLoader, "localhost", tsaPort, jmxPort, new File("temp/OsgiClusteredEhcacheTest/"
                                                                                    + testName.getMethodName()));
    OsgiUtil.startL2(l2ServerControl);
  }

  @After
  public void after() throws Exception {
    OsgiUtil.stopL2(l2ServerControl);
  }

  @org.ops4j.pax.exam.Configuration
  public Option[] config() {
    return options(bootDelegationPackages("sun.*,javax.naming,javax.naming.spi,javax.naming.event,javax.management,javax.net.ssl,javax.management.remote.misc"),
                   OsgiUtil.commonOptions(), OsgiUtil.getMavenBundle("org.terracotta", "terracotta-toolkit-runtime-ee",
                                                                     "terracotta-toolkit-runtime"), OsgiUtil
                       .getMavenBundle("net.sf.ehcache", "ehcache-ee", "ehcache"),
                   OsgiUtil.propagateSystemTestsProps(),
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

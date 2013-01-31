package net.sf.ehcache.osgi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.bootDelegationPackages;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.osgi.util.TestUtil;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.exam.util.PathUtils;

/**
 * 
 * 
 * @author hhuynh
 *
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class ClusteredEhcacheTest {
  
  @Configuration
  public Option[] config() {
    return options(
        bootDelegationPackages("sun.*,javax.naming,javax.naming.spi,javax.naming.event,javax.management,javax.net.ssl,javax.management.remote.misc"),
        TestUtil.commonOptions(),
        mavenBundle("org.terracotta", "terracotta-toolkit-runtime-ee").versionAsInProject(),
        mavenBundle("net.sf.ehcache", "ehcache-ee").versionAsInProject(),
        systemProperty("com.tc.productkey.path").value(
            PathUtils.getBaseDir() + "/src/test/resources/enterprise-suite-license.key"));
  }

  @Test
  public void testClusteredCache() throws Exception {
    CacheManager manager = new CacheManager(
        ClusteredEhcacheTest.class.getResource("/clustered-ehcache.xml"));
    Cache cache = manager.getCache("sampleCache2");
    Element element = new Element("key1", "value1");
    cache.put(element);
    Element element1 = cache.get("key1");
    assertEquals("value1", element1.getObjectValue());
    assertEquals(1, cache.getSize());
    assertTrue(cache.isTerracottaClustered());
  }

}
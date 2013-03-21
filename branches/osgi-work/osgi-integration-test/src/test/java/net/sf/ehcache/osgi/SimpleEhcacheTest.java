package net.sf.ehcache.osgi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.bootDelegationPackages;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.osgi.util.TestUtil;
import net.sf.ehcache.util.ProductInfo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.exam.util.PathUtils;
import org.terracotta.context.ContextManager;

/**
 * Test a simple BigMemory usage with BM Go license key. The product name should
 * include "BigMemory" and not "Ehcache"
 * 
 * 
 * @author hhuynh
 * 
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class SimpleEhcacheTest {

  @Configuration
  public Option[] config() {
    return options(
        bootDelegationPackages("javax.xml.transform,org.w3c.dom"), // need this for REST agent test
        mavenBundle("org.terracotta.bigmemory", "bigmemory").versionAsInProject(),
        mavenBundle("net.sf.ehcache", "ehcache-ee").versionAsInProject(),
        TestUtil.commonOptions(),
        systemProperty("com.tc.productkey.path").value(
            PathUtils.getBaseDir() + "/src/test/resources/bigmemorygo-license.key"));
  }

  @Test
  public void testSimpleCache() throws Exception {
    ProductInfo pi = new ProductInfo();
    assertEquals("BigMemory", pi.getName());
    CacheManager manager = new CacheManager(
        SimpleEhcacheTest.class.getResource("/simple-ehcache.xml"));
    try {
      Cache cache = manager.getCache("sampleCache1");
      Element element = new Element("key1", "value1");
      cache.put(element);
      Element element1 = cache.get("key1");
      assertEquals("value1", element1.getObjectValue());
      assertEquals(1, cache.getSize());
    } finally {
      if (manager != null) {
        manager.shutdown();
      }
    }
  }

  @Test
  public void testUsingNonExportedClass() {
    try {
      ContextManager cm = new ContextManager();
      fail("Expected class not found exception");
    } catch (Throwable e) {
      // expected
    }
  }

  @Test
  public void testRestAgent() throws Exception {
    CacheManager manager = new CacheManager(
        SimpleEhcacheTest.class.getResource("/rest-enabled-ehcache.xml"));
    InputStream in = null;
    try {
      Cache testCache = manager.getCache("testCache");
      testCache.put(new Element("k", "v"));
      assertEquals(1, testCache.getSize());
      URL url = new URL("http://localhost:9888/tc-management-api/agents");
      in = url.openStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(in));
      String line = null;
      StringBuilder sb = new StringBuilder();
      while ((line = reader.readLine()) != null) {
        sb.append(line.trim()).append("\n");
      }
      System.out.println("Rest response: " + sb);
      assertTrue(sb.toString().contains("\"agentId\":\"embedded\""));
    } finally {
      if (manager != null) {
        manager.shutdown();
      }
      if (in != null) {
        in.close();
      }
    }
  }
}
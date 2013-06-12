/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package net.sf.ehcache.osgi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.bootDelegationPackages;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.terracotta.context.ContextManager;
import org.terracotta.test.OsgiUtil;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Test a simple BigMemory usage with BM Go license key. The product name should include "BigMemory" and not "Ehcache"
 * NOTE: this test only works in fullmode
 * 
 * @author hhuynh
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class SimpleOsgiTest {

  @Configuration
  public Option[] config() {
    return options(bootDelegationPackages("javax.xml.transform,org.w3c.dom,javax.xml.bind,com.sun.xml.internal.bind.v2,javax.xml.bind.annotation"),
                   // need this for REST agent test
                   mavenBundle("net.sf.ehcache", "ehcache").versionAsInProject(), OsgiUtil.commonOptions());
  }

  @Test
  public void testSimpleCache() throws Exception {
    CacheManager manager = new CacheManager(SimpleOsgiTest.class.getResource("/net/sf/ehcache/osgi/simple-ehcache.xml"));
    try {
      Cache cache = manager.getCache("sampleCache1");
      Element element = new Element("key1", "value1");
      cache.put(element);
      Element element1 = cache.get("key1");
      assertEquals("value1", element1.getObjectValue());
      assertEquals(1, cache.getSize());
    } finally {
      manager.shutdown();
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
    System.getProperties().list(System.out);
    CacheManager manager = new CacheManager(
                                            SimpleOsgiTest.class
                                                .getResource("/net/sf/ehcache/osgi/rest-enabled-ehcache.xml"));
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
      manager.shutdown();
      if (in != null) {
        in.close();
      }
    }
  }
}
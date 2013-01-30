package net.sf.ehcache.osgi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.workingDirectory;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.util.ProductInfo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.terracotta.context.ContextManager;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class SimpleEhcacheTest {

  @Configuration
  public Option[] config() {
    return options(
        mavenBundle("org.terracotta.license", "license-bundle").versionAsInProject().noStart(),
        mavenBundle("org.terracotta.bigmemory", "bigmemory").versionAsInProject(),
        mavenBundle("net.sf.ehcache", "ehcache-ee").versionAsInProject(), 
        junitBundles(), 
        workingDirectory("target/pax-exam"), 
        cleanCaches());
  }

  @Test
  public void testSimpleCache() throws Exception {
    System.out.println("XXX: " + new ProductInfo());
    CacheManager manager = new CacheManager(
        SimpleEhcacheTest.class.getResource("/simple-ehcache.xml"));
    Cache cache = manager.getCache("sampleCache1");
    Element element = new Element("key1", "value1");
    cache.put(element);
    Element element1 = cache.get("key1");
    assertEquals("value1", element1.getObjectValue());
    assertEquals(1, cache.getSize());
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
}
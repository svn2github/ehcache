package net.sf.ehcache.osgi;

import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import net.sf.ehcache.osgi.util.TestUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.exam.util.PathUtils;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitFactory;
import org.terracotta.toolkit.collections.ToolkitMap;

/**
 * 
 * @author hhuynh
 *
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class SimpleToolkitTest {
  private Toolkit toolkit;

  @Configuration
  public Option[] config() {
    return options(
        mavenBundle("org.terracotta", "terracotta-toolkit-runtime-ee").versionAsInProject(),
        TestUtil.commonOptions(),
        systemProperty("com.tc.productkey.path").value(
            PathUtils.getBaseDir() + "/src/test/resources/enterprise-suite-license.key"));
  }
  
  @Before
  public void setUp() throws Exception {
    toolkit = ToolkitFactory.createToolkit("toolkit:terracotta://localhost:9510");
  }

  @After
  public void tearDown() throws Exception {
    if (toolkit != null) {
      toolkit.shutdown();
    }
  }
  
  @Test
  public void testToolkitMap() throws Exception {
    ToolkitMap<String, String> map = toolkit.getMap("myMap", String.class, String.class);
    map.put("k", "v");
    assertEquals(1, map.size());
    assertEquals("v", map.get("k"));
  }
}
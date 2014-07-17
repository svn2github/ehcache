package org.terracotta.ehcache.tests.loader;

import net.sf.ehcache.CacheManager;

import org.objectweb.asm.ClassWriter;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.test.util.TestBaseUtil;
import org.terracotta.toolkit.Toolkit;

import com.tc.test.config.model.TestConfig;

import java.io.File;
import java.util.Arrays;

public class LoaderTest extends AbstractCacheTestBase {

  private static final String EHCACHE_XML = "small-memory-cache-test.xml";

  public LoaderTest(TestConfig testConfig) {
    super(EHCACHE_XML, testConfig);
  }

  @Override
  protected String createClassPath(Class client) {
    return "";
  }

  @Override
  protected void startClients() throws Throwable {

    StringBuilder sb = new StringBuilder();

    sb.append(writeEhcacheConfigWithPort(EHCACHE_XML)).append(File.pathSeparator);
    sb.append(writeXmlFileWithPort("log4j.xml", "log4j.xml")).append(File.pathSeparator);
    sb.append(TestBaseUtil.jarFor(LoaderClient.class)).append(File.pathSeparator);
    sb.append(TestBaseUtil.jarFor(CacheManager.class)).append(File.pathSeparator);
    sb.append(TestBaseUtil.jarFor(org.slf4j.LoggerFactory.class)).append(File.pathSeparator);
    sb.append(TestBaseUtil.jarFor(org.slf4j.impl.StaticLoggerBinder.class)).append(File.pathSeparator);
    sb.append(TestBaseUtil.jarFor(org.apache.log4j.LogManager.class)).append(File.pathSeparator);
    sb.append(TestBaseUtil.jarFor(org.apache.commons.logging.LogFactory.class)).append(File.pathSeparator);
    sb.append(TestBaseUtil.jarFor(ClassWriter.class)).append(File.pathSeparator); // needed for OtherClassloaderClient
    sb.append(TestBaseUtil.jarFor(org.junit.Assert.class)).append(File.pathSeparator);
    sb.append(TestBaseUtil.jarFor(Toolkit.class));

    runClient(LoaderClient.class, LoaderClient.class.getSimpleName(), Arrays.asList(sb.toString()));
  }
}

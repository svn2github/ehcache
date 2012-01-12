package org.terracotta.ehcache.tests.loader;

import net.sf.ehcache.CacheManager;

import org.objectweb.asm.ClassWriter;
import org.terracotta.ehcache.tests.AbstractStandaloneCacheTest;
import org.terracotta.express.ClientFactory;

import com.tc.lcp.LinkedJavaProcess;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

import java.io.File;
import java.util.Arrays;

public class LoaderTest extends AbstractStandaloneCacheTest {

  private static final String EHCACHE_XML = "small-memory-cache-test.xml";

  public LoaderTest() {
    super(EHCACHE_XML, LoaderClient.class);
  }

  @Override
  protected Class getApplicationClass() {
    return LoaderTest.App.class;
  }

  public static class App extends AbstractStandaloneCacheTest.App {

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected String makeClasspath(String... jars) {
      String cp = "";
      for (String jar : jars) {
        if (jar.endsWith(".jar")) continue;
        cp += File.pathSeparator + jar;
      }

      // the linked-child-process lib still is necessary
      cp += File.pathSeparator + jarFor(LinkedJavaProcess.class);

      for (String extra : getExtraJars()) {
        cp += File.pathSeparator + extra;
      }

      return cp;
    }

    @Override
    protected void runClient(Class client, boolean withStandaloneJar) throws Throwable {
      StringBuilder sb = new StringBuilder();

      sb.append(writeEhcacheConfigWithPort(EHCACHE_XML)).append(File.pathSeparator);
      sb.append(writeXmlFileWithPort("log4j.xml", "log4j.xml")).append(File.pathSeparator);
      sb.append(jarFor(client)).append(File.pathSeparator);
      sb.append(jarFor(Class.forName("net.sf.ehcache.terracotta.StandaloneTerracottaClusteredInstanceFactory")))
          .append(File.pathSeparator);
      sb.append(jarFor(CacheManager.class)).append(File.pathSeparator);
      sb.append(jarFor(org.slf4j.LoggerFactory.class)).append(File.pathSeparator);
      sb.append(jarFor(org.slf4j.impl.StaticLoggerBinder.class)).append(File.pathSeparator);
      sb.append(jarFor(org.apache.log4j.LogManager.class)).append(File.pathSeparator);
      sb.append(jarFor(org.apache.commons.logging.LogFactory.class)).append(File.pathSeparator);
      sb.append(jarFor(ClassWriter.class)).append(File.pathSeparator); // needed for OtherClassloaderClient
      sb.append(jarFor(EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap.class)).append(File.pathSeparator);
      sb.append(jarFor(org.junit.Assert.class)).append(File.pathSeparator);
      sb.append(jarFor(ClientFactory.class));

      runClient(client, withStandaloneJar, client.getSimpleName(), Arrays.asList(sb.toString()));
    }

  }
}

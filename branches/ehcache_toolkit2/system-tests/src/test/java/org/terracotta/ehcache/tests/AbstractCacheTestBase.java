/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import net.sf.ehcache.CacheManager;

import org.apache.commons.cli.ParseException;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassWriter;
import org.terracotta.test.util.JMXUtils;
import org.terracotta.test.util.TestBaseUtil;
import org.terracotta.tests.base.AbstractClientBase;
import org.terracotta.tests.base.AbstractTestBase;
import org.terracotta.toolkit.client.TerracottaClient;

import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.test.config.model.TestConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.transaction.TransactionManager;

public class AbstractCacheTestBase extends AbstractTestBase {
  protected final String    ehcacheConfigPath;
  private final Set<String> writtenXmls = new HashSet<String>();

  public AbstractCacheTestBase(TestConfig testConfig, Class<? extends ClientBase>... c) {
    this("basic-cache-test.xml", testConfig, c);
    // This will disable all Cache Tests until ehcache becomes compatible with toolkit2.0
    if (isDisabled()) {
      disableTest();
    }
  }

  public AbstractCacheTestBase(final String ehcacheConfigPath, TestConfig testConfig) {
    this(ehcacheConfigPath, testConfig, Client1.class, Client2.class);
    testConfig.getClientConfig().setParallelClients(false);
    // This will disable all Cache Tests until ehcache becomes compatible with toolkit2.0
    if (isDisabled()) {
      disableTest();
    }
  }

  public AbstractCacheTestBase(final String ehcacheConfigPath, TestConfig testConfig,
                               Class<? extends AbstractClientBase>... c) {
    super(testConfig);
    this.ehcacheConfigPath = ehcacheConfigPath;
    testConfig.getClientConfig().setClientClasses(c);
    // This will disable all Cache Tests until ehcache becomes compatible with toolkit2.0
    if (isDisabled()) {
      disableTest();
    }
  }

  @Override
  protected String createClassPath(Class client) throws IOException {
    String ehcache = TestBaseUtil.jarFor(CacheManager.class);
    String slf4jApi = TestBaseUtil.jarFor(org.slf4j.LoggerFactory.class);
    String slf4jBinder = TestBaseUtil.jarFor(org.slf4j.impl.StaticLoggerBinder.class);
    String cLogging = TestBaseUtil.jarFor(org.apache.commons.logging.LogFactory.class);
    String asm = TestBaseUtil.jarFor(ClassWriter.class); // needed for OtherClassloaderClient
    String jta = TestBaseUtil.jarFor(TransactionManager.class);
    String oswego = TestBaseUtil.jarFor(EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap.class);
    String expressRuntime = TestBaseUtil.jarFor(TerracottaClient.class);
    String parseException = TestBaseUtil.jarFor(ParseException.class); // apache commons
    String httpMethod = TestBaseUtil.jarFor(HttpMethod.class);
    String decoder = TestBaseUtil.jarFor(DecoderException.class);
    String jmxUtils = TestBaseUtil.jarFor(JMXUtils.class);
    String clientBase = TestBaseUtil.jarFor(ClientBase.class);

    // TODO: get rid of this
    String mbeanSereverProxy = TestBaseUtil.jarFor(MBeanServerInvocationProxy.class);

    String classpath = "";
    classpath = makeClasspath(writeEhcacheConfigWithPort(ehcacheConfigPath),
                              writeXmlFileWithPort("log4j.xml", "log4j.xml"), expressRuntime, ehcache, slf4jApi,
                              slf4jBinder, cLogging, asm, jta, oswego, parseException, httpMethod, decoder, jmxUtils,
                              clientBase, mbeanSereverProxy);

    return classpath;
  }

  /**
   * Read the ehcache.xml file as a resource, replace PORT token with appropriate port, write the ehcache.xml file back
   * out to the temp dir, and return the resulting resource directory.
   */
  protected String writeEhcacheConfigWithPortAndNameSuffix(String resourcePath, String nameSuffix) throws IOException {
    return writeXmlFileWithPort(resourcePath, "ehcache-config.xml", nameSuffix);
  }

  protected String writeEhcacheConfigWithPort(String resourcePath) throws IOException {
    return writeXmlFileWithPort(resourcePath, "ehcache-config.xml");
  }

  @SuppressWarnings("unchecked")
  protected String writeXmlFileWithPort(String resourcePath, String outputName) throws IOException {
    return writeXmlFileWithPort(resourcePath, outputName, null);
  }

  @SuppressWarnings("unchecked")
  protected String writeXmlFileWithPort(String resourcePath, String outputName, String nameSuffix) throws IOException {
    if (nameSuffix != null && outputName.indexOf(".xml") > 0) {
      outputName = outputName.substring(0, outputName.indexOf(".xml")) + "-" + nameSuffix + ".xml";
    }
    if (!writtenXmls.add(outputName)) {
      System.out.println("OUTPUT FILE: " + outputName + " already written. Skipping...");
      return tempDir.getAbsolutePath();
    }
    resourcePath = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
    // Slurp resourcePath file
    System.out.println("RESOURCE PATH: " + resourcePath);

    InputStream is = this.getClass().getResourceAsStream(resourcePath);

    List<String> lines = IOUtils.readLines(is);

    // Replace PORT token
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      line = line.replace("localhost:PORT", getTerracottaURL());
      line = line.replace("CONFIG", ehcacheConfigPath);
      line = line.replace("TEMP", tempDir.getAbsolutePath());
      line = line.replace("TERRACOTTA_URL", getTerracottaURL());

      String nameSuffixReplaceValue = nameSuffix == null ? "" : "-" + nameSuffix;
      line = line.replace("__NAME_SUFFIX__", nameSuffixReplaceValue);
      lines.set(i, line);
    }

    // Write
    File outputFile = new File(tempDir, outputName);
    FileOutputStream fos = new FileOutputStream(outputFile);
    IOUtils.writeLines(lines, IOUtils.LINE_SEPARATOR, fos);
    return tempDir.getAbsolutePath();
  }

  public String getEhcacheConfigPath() {
    return ehcacheConfigPath;
  }

  protected boolean isDisabled() {
    return true;
  }

}

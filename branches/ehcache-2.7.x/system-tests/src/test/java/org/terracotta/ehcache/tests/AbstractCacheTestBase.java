/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import net.sf.ehcache.CacheManager;

import org.apache.commons.io.IOUtils;
import org.terracotta.test.util.TestBaseUtil;
import org.terracotta.tests.base.AbstractClientBase;
import org.terracotta.tests.base.AbstractTestBase;
import org.terracotta.toolkit.ToolkitFactory;

import com.tc.l2.L2DebugLogging.LogLevel;
import com.tc.management.beans.L2MBeanNames;
import com.tc.test.config.model.TestConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.transaction.TransactionManager;

public class AbstractCacheTestBase extends AbstractTestBase {
  protected final String    ehcacheConfigPath;
  private final Set<String> writtenXmls = new HashSet<String>();

  public AbstractCacheTestBase(TestConfig testConfig, Class<? extends AbstractClientBase>... c) {
    this("basic-cache-test.xml", testConfig, c);
  }

  public AbstractCacheTestBase(final String ehcacheConfigPath, TestConfig testConfig) {
    this(ehcacheConfigPath, testConfig, Client1.class, Client2.class);
    testConfig.getClientConfig().setParallelClients(false);
  }

  public AbstractCacheTestBase(final String ehcacheConfigPath, TestConfig testConfig,
                               Class<? extends AbstractClientBase>... c) {
    super(testConfig);
    this.ehcacheConfigPath = ehcacheConfigPath;
    testConfig.getL2Config().setMaxHeap(1024);
    testConfig.getL2Config().setMinHeap(1024);
    testConfig.getClientConfig().setClientClasses(c);
  }

  @Override
  protected String createClassPath(Class client) throws IOException {
    List<String> toolkitRuntime = TestBaseUtil.getToolkitRuntimeDependencies(ToolkitFactory.class);
    List<String> ehcache = TestBaseUtil.getEhcacheDependencies(CacheManager.class);
    List<String> ehcacheExpress = new ArrayList<String>();
    ehcacheExpress.addAll(toolkitRuntime);
    ehcacheExpress.addAll(ehcache);

    String slf4jApi = TestBaseUtil.jarFor(org.slf4j.LoggerFactory.class);
    String slf4jBinder = TestBaseUtil.jarFor(org.slf4j.impl.StaticLoggerBinder.class);
    String l2Mbean = TestBaseUtil.jarFor(L2MBeanNames.class);
    String jta = TestBaseUtil.jarFor(TransactionManager.class);
    String expressRuntime = TestBaseUtil.jarFor(ToolkitFactory.class);
    String clientBase = TestBaseUtil.jarFor(ClientBase.class);

    String classpath = makeClasspath(ehcacheExpress, writeEhcacheConfigWithPort(ehcacheConfigPath),
                                     writeXmlFileWithPort("log4j.xml", "log4j.xml"), expressRuntime, jta, slf4jApi,
                                     slf4jBinder, clientBase, l2Mbean);

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
  protected synchronized String writeXmlFileWithPort(String resourcePath, String outputName, String nameSuffix)
      throws IOException {
    if (nameSuffix != null && outputName.indexOf(".xml") > 0) {
      outputName = outputName.substring(0, outputName.indexOf(".xml")) + "-" + nameSuffix + ".xml";
    }
    if (!writtenXmls.add(outputName)) {
      System.out.println("OUTPUT FILE: " + outputName + " already written. Skipping...");
      return tempDir.getAbsolutePath();
    }
    resourcePath = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
    // Slurp resourcePath file
    System.out.println("RESOURCE PATH: " + resourcePath + ", Output name: " + outputName);

    InputStream is = this.getClass().getResourceAsStream(resourcePath);

    List<String> lines = IOUtils.readLines(is);

    final boolean isStandAloneTest = getTestConfig().isStandAloneTest();
    // Replace PORT token
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      if (!isStandAloneTest) {
        line = line.replace("localhost:PORT", getTerracottaURL());
        line = line.replace("CONFIG", ehcacheConfigPath);
        line = line.replace("TEMP", tempDir.getAbsolutePath());
        line = line.replace("TERRACOTTA_URL", getTerracottaURL());
        line = line.replace("PORT", Integer.toString(getGroupsData()[0].getTsaPort(0)));

        String nameSuffixReplaceValue = nameSuffix == null ? "" : "-" + nameSuffix;
        line = line.replace("__NAME_SUFFIX__", nameSuffixReplaceValue);
      } else {
        if (line.contains("terracotta")) {
          line = "";
        }
      }
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

  public void enableNonStopDebugLogs() {
    configureTCLogging("com.terracotta.toolkit.nonstop.NonStopManagerImpl", LogLevel.DEBUG);
    configureTCLogging("com.terracotta.toolkit.NonStopInitializationService", LogLevel.DEBUG);
    configureTCLogging("com.terracotta.toolkit.NonStopClusterInfo", LogLevel.DEBUG);
    configureTCLogging("com.terracotta.toolkit.nonstop.AbstractToolkitObjectLookupAsync", LogLevel.DEBUG);
    configureTCLogging("com.terracotta.toolkit.nonstop.NonStopInvocationHandler", LogLevel.DEBUG);
  }

}

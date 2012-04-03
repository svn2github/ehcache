/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import org.apache.commons.io.IOUtils;

import com.tc.test.config.model.TestConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class UrlConfigStandaloneCacheTest extends AbstractCacheTestBase {

  public UrlConfigStandaloneCacheTest(TestConfig testConfig) {
    super("url-config-cache-test.xml", testConfig);
  }

  @Override
  protected void runClient(Class client) throws Throwable {
    writeXmlFileWithPort("url-config-cache-test-tc-config.xml", "ehcache-config.xml");
    super.runClient(client);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected String writeXmlFileWithPort(String resourcePath, String outputName, String nameSuffix) throws IOException {
    if (nameSuffix != null && outputName.indexOf(".xml") > 0) {
      outputName = outputName.substring(0, outputName.indexOf(".xml")) + "-" + nameSuffix + ".xml";
    }
    resourcePath = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
    // Slurp resourcePath file
    System.out.println("RESOURCE PATH: " + resourcePath);

    InputStream is = this.getClass().getResourceAsStream(resourcePath);

    List<String> lines = IOUtils.readLines(is);

    // Replace PORT token
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      line = line.replace("PORT", Integer.toString(getGroupsData()[0].getDsoPort(0)));
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
}

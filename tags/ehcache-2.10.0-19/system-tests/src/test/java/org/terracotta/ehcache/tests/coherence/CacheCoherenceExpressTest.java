/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests.coherence;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;

import com.tc.test.config.model.TestConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class CacheCoherenceExpressTest extends AbstractCacheTestBase {

  public static final int CLIENT_COUNT = 3;

  public CacheCoherenceExpressTest(TestConfig testConfig) {
    super("cache-coherence-test.xml", testConfig, CacheCoherenceExpressClient.class, CacheCoherenceExpressClient.class,
          CacheCoherenceExpressClient.class);
    testConfig.addTcProperty("ehcache.evictor.logging.enabled", "true");
  }

  @Override
  protected void evaluateClientOutput(String clientName, int exitCode, File clientOutput) throws Throwable {
    super.evaluateClientOutput(clientName, exitCode, clientOutput);

    FileReader fr = null;
    try {
      fr = new FileReader(clientOutput);
      BufferedReader reader = new BufferedReader(fr);
      String st = "";
      while ((st = reader.readLine()) != null) {
        if (st.contains(CacheCoherenceExpressClient.PASS_OUTPUT)) return;
      }
      throw new AssertionError("Client exited without pass output string: " + CacheCoherenceExpressClient.PASS_OUTPUT);
    } catch (Exception e) {
      throw new AssertionError(e);
    } finally {
      try {
        fr.close();
      } catch (Exception e) {
        //
      }
    }
  }
}

/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.test.config.model.TestConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class ExpiryListenerTest extends AbstractCacheTestBase {

  public ExpiryListenerTest(TestConfig testConfig) {
    // assume the 'test' cache TTL is 3s
    super("evict-cache-test.xml", testConfig, ExpiryListenerClient1.class, ExpiryListenerClient2.class);
    testConfig.getClientConfig().setParallelClients(false);
    timebombTest("2013-04-25");
  }

  @Override
  protected void evaluateClientOutput(String clientName, int exitCode, File output) throws Throwable {
    if ((exitCode != 0)) { throw new AssertionError("Client " + clientName + " exited with exit code: " + exitCode); }

    if (!ExpiryListenerClient1.class.getName().equals(clientName)) return;

    FileReader fr = null;
    try {
      fr = new FileReader(output);
      BufferedReader reader = new BufferedReader(fr);
      String st = "";
      while ((st = reader.readLine()) != null) {
        if (st.contains("Got evicted")) return;
      }
      throw new AssertionError("Expecting eviction notice from client " + clientName);
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

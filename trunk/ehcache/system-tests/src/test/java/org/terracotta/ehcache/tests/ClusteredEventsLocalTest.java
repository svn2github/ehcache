/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.test.config.model.TestConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class ClusteredEventsLocalTest extends AbstractCacheTestBase {

  public ClusteredEventsLocalTest(TestConfig testConfig) {
    super("clustered-events-test.xml", testConfig, ClusteredEventsLocalClient1.class, ClusteredEventsLocalClient2.class);
  }

  @Override
  protected void evaluateClientOutput(String clientName, int exitCode, File clientOutput) throws Throwable {
    super.evaluateClientOutput(clientName, exitCode, clientOutput);

    boolean notifedKey1 = false;
    boolean notifedKey2 = false;
    FileReader fr = null;
    try {
      fr = new FileReader(clientOutput);
      BufferedReader reader = new BufferedReader(fr);
      String st = "";
      while ((st = reader.readLine()) != null) {
        if (st.contains("[notifyElementPut key1, value1]")) notifedKey1 = true;
        if (st.contains("[notifyElementPut key2, value2]")) notifedKey2 = true;
      }
    } catch (Exception e) {
      throw new AssertionError(e);
    } finally {
      try {
        fr.close();
      } catch (Exception e) {
        //
      }
    }

    if (ClusteredEventsLocalClient1.class.getName().equals(clientName) && (!notifedKey1 || notifedKey2)) { throw new AssertionError(
                                                                                                                                    clientName); }

    if (ClusteredEventsLocalClient2.class.getName().equals(clientName) && (notifedKey1 || !notifedKey2)) { throw new AssertionError(
                                                                                                                                    clientName); }

  }
}

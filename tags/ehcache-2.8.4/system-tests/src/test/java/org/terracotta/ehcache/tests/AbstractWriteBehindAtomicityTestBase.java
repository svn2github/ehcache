/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import org.terracotta.tests.base.AbstractClientBase;

import com.tc.test.config.model.TestConfig;
import com.tc.util.Assert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public abstract class AbstractWriteBehindAtomicityTestBase extends AbstractCacheTestBase {

  public AbstractWriteBehindAtomicityTestBase(final String ehcacheConfigPath, TestConfig testConfig,
                                              Class<? extends AbstractClientBase>... c) {
    super(ehcacheConfigPath, testConfig, c);
    testConfig.getClientConfig().getBytemanConfig().setScript("/byteman/writeBehindAtomicity.btm");
    // disableTest();
  }

  // 1) Begin putWithWriter
  // 2) lock() putWithWriter
  // 3) Begin Transaction
  // 4) Commit Transaction
  // 5) unlock() putWithWriter
  // 6) Done putWithWriter
  @Override
  protected void evaluateClientOutput(String clientName, int exitCode, File output) throws Throwable {
    super.evaluateClientOutput(clientName, exitCode, output);
    int txnCount = 0;
    boolean underExplicitLock = false;
    FileReader fr = null;
    BufferedReader reader = null;
    try {
      fr = new FileReader(output);
      reader = new BufferedReader(fr);
      String st = "";
      while ((st = reader.readLine()) != null) {
        // only check for main thread
        if (st.contains("main")) {
          if (st.contains("BEGINOPERATION")) {
            Assert.assertEquals(false, underExplicitLock);
            underExplicitLock = true;
          } else if (st.contains("COMMITTRANSACTION") && underExplicitLock) {
            txnCount++;
            Assert.assertEquals(txnCount, 1);
          } else if (st.contains("ENDOPERATION")) {
            Assert.assertEquals(true, underExplicitLock);
            underExplicitLock = false;
            Assert.assertEquals(txnCount, 1);
            txnCount = 0;
          }
        }
      }
    } catch (Exception e) {
      throw new AssertionError(e);
    } finally {
      try {
        fr.close();
        reader.close();
      } catch (Exception e) {
        //
      }
    }

    Assert.assertEquals(false, underExplicitLock);
    Assert.assertEquals(txnCount, 0);
  }

}
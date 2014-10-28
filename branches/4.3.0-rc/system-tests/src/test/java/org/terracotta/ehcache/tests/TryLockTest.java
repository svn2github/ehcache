/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;

import com.tc.test.config.model.TestConfig;

// test for ENG-680, should be enabled when implemented
public class TryLockTest extends AbstractCacheTestBase {

  public TryLockTest(TestConfig testConfig) {
    super(testConfig, TryLockTestClient.class, TryLockTestClient.class);
    disableTest();
  }

  public static class TryLockTestClient extends ClientBase {

    public TryLockTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void runTest(Cache cache, Toolkit myToolkit) throws Throwable {
      ToolkitReadWriteLock myLock = myToolkit.getReadWriteLock("abc");
      int index = waitForAllClients();

      if (index == 0) {
        myLock.writeLock().lock();
        myLock.writeLock().unlock();
      }

      waitForAllClients();

      if (index != 0) {
        assertTrue(myLock.readLock().tryLock());
        // assertTrue(myLock.readLock().tryLock(2, TimeUnit.SECONDS)); - this works
      }

      waitForAllClients();
    }
  }
}

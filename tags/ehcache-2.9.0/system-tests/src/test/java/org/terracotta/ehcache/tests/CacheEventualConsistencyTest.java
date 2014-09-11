/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.junit.Assert;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitFactory;
import org.terracotta.toolkit.ToolkitInstantiationException;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.concurrent.atomic.ToolkitAtomicLong;

import com.tc.test.config.model.TestConfig;

import java.util.Calendar;
import java.util.Properties;
import java.util.concurrent.BrokenBarrierException;

public class CacheEventualConsistencyTest extends AbstractCacheTestBase {


  public CacheEventualConsistencyTest(TestConfig testConfig) {
    super("eventual-cache-explicit-locking-test.xml", testConfig, TestClient.class, TestClient.class);
    testConfig.addTcProperty("seda.receive_invalidate_objects_stage.sleepMs", "60000");

  }

  public static class TestClient extends ClientBase {

    private static final String QWERTY         = "qwerty";
    private static final String INTIAL_VALUE   = "init_";
    private static final String PREFIX_COUNTER = "_updated";
    private static final String BARRIER_NAME   = "eventual-cache-explicit-locking-barrier";
    private static final int    CLIENT_COUNT   = 2;
    private static final int    UPDATE_LIMIT   = 6;                                        // Test Case Runs
    private static final int    NUM_ELEMENTS   = 2;                                        // Number of keys updated

    public TestClient(String[] args) {
      super("eventualConsistencyCache", args);
    }

    @Override
    protected Toolkit createToolkit() {
      try {
        Properties properties = new Properties();
        properties.put("rejoin", true);
        properties.put("nonstop-terracotta", false);
        return ToolkitFactory.createToolkit(getTerracottaTypeSubType() + getTerracottaUrl(), properties);
      } catch (ToolkitInstantiationException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    protected void runTest(Cache cache, Toolkit toolkit) throws Throwable {
      try {
        Toolkit internaltoolkit = createToolkit();
        ToolkitBarrier barrier = internaltoolkit.getBarrier(BARRIER_NAME, CLIENT_COUNT);
        int index = barrier.await();
        if (index == 0) {
          System.err.println("Client: " + index + " doing eventual puts...");
          doEventualPuts(cache, INTIAL_VALUE);
        }
        barrier.await();
        if (index > 0) {
          System.err.println("Client: " + index + " doing eventual gets");
          doEventualGets(cache);
        }
        barrier.await();
        passForEventualCache(cache, barrier, index, internaltoolkit);
      } finally {
        System.err.println("Text Completed");
      }
    }

    private void passForEventualCache(Cache cache, ToolkitBarrier barrier, int index, Toolkit internaltoolkit)
        throws InterruptedException,
        BrokenBarrierException {
      long runCounter = 0;
      long runsLimit = UPDATE_LIMIT;
      runCounter = runsForEventualPutsNGets(cache, barrier, index, internaltoolkit, runCounter, runsLimit);
      runsLimit = UPDATE_LIMIT * 2;
      runCounter = runsForStrongPutNEventualGets(cache, barrier, index, internaltoolkit, runCounter, runsLimit);
      runsLimit = UPDATE_LIMIT * 3;
      // runCounter = runsForEventualPutsNStrongGets(cache, barrier, index, internaltoolkit, runCounter, runsLimit);
      // runsLimit = UPDATE_LIMIT * 4;
      runCounter = runsForStrongPutsNStrongGets(cache, barrier, index, runCounter, runsLimit);
    }

    private long runsForStrongPutsNStrongGets(Cache cache, ToolkitBarrier barrier, int index, long runCounter,
                                              long runsLimit) throws InterruptedException, BrokenBarrierException {
      while (runCounter < runsLimit) {
        testExplicitStrongPutsNGets(cache, barrier, index, "#" + (++runCounter) + PREFIX_COUNTER);
      }
      return runCounter;
    }

    private long runsForEventualPutsNStrongGets(Cache cache, ToolkitBarrier barrier, int index,
                                                Toolkit internaltoolkit, long runCounter, long runsLimit)
        throws InterruptedException,
        BrokenBarrierException {
      testStrongGets(cache, barrier, index, "#" + (++runCounter) + PREFIX_COUNTER, internaltoolkit);
      if (index > 0) {
        assertOnEventualFailures(internaltoolkit);
      }
      barrier.await();
      return runCounter;
    }

    private long runsForStrongPutNEventualGets(Cache cache, ToolkitBarrier barrier, int index, Toolkit internaltoolkit,
                                               long runCounter, long runsLimit) throws InterruptedException,
        BrokenBarrierException {
      testStrongPuts(cache, barrier, index, "#" + (++runCounter) + PREFIX_COUNTER, internaltoolkit);
      if (index > 0) {
        assertOnEventualFailures(internaltoolkit);
      }
      barrier.await();
      return runCounter;
    }

    private long runsForEventualPutsNGets(Cache cache, ToolkitBarrier barrier, int index, Toolkit internaltoolkit,
                                          long runCounter, long runsLimit) throws InterruptedException,
        BrokenBarrierException {
      testEventualPutsNGets(cache, barrier, index, "#" + (++runCounter) + PREFIX_COUNTER, internaltoolkit);
      if (index > 0) {
        assertOnEventualFailures(internaltoolkit);
      }
      barrier.await();
      return runCounter;
    }


    private void testExplicitStrongPutsNGets(Cache cache, ToolkitBarrier barrier, int index, final String prefix)
        throws InterruptedException, BrokenBarrierException {
      if (index == 0) {
        System.err.println("Client: " + index + " doing eventual puts...");
        doExplicitStrongPuts(cache, prefix);
      }
      barrier.await();
      if (index > 0) {
        doExplicitStrongGets(cache, prefix);
      }
      barrier.await();
    }

    private void testStrongPuts(Cache cache, ToolkitBarrier barrier, int index, final String prefix,
                                Toolkit toolkitInternal)
        throws InterruptedException, BrokenBarrierException {
      if (index == 0) {
        System.err.println("Client: " + index + " doing eventual puts...");
        doExplicitStrongPuts(cache, prefix);
      }
      barrier.await();
      if (index > 0) {
        failEventualGets(cache, prefix, toolkitInternal);
      }
      barrier.await();
    }

    private void testStrongGets(Cache cache, ToolkitBarrier barrier, int index, final String prefix,
                                Toolkit internaltoolkit)
        throws InterruptedException, BrokenBarrierException {
      if (index == 0) {
        logTimeStart();
        System.err.println("Client: " + index + " doing eventual puts...");
        doEventualPuts(cache, prefix);
        logTimeEnd();
      }
      barrier.await();
      if (index > 0) {
        logTimeStart();
        failExplicitStrongGets(cache, prefix, internaltoolkit);
        logTimeEnd();
      }
      barrier.await();
    }

    private void logTimeStart() {
      System.out.println("XS:-" + Calendar.getInstance().getTime());
    }

    private void logTimeEnd() {
      System.out.println("XE:-" + Calendar.getInstance().getTime());
    }

    private void testEventualPutsNGets(Cache cache, ToolkitBarrier barrier, int index, final String prefix,
                                       Toolkit internaltoolkit)
        throws InterruptedException, BrokenBarrierException {
      if (index == 0) {
        System.err.println("Client: " + index + " doing eventual puts..." + prefix);
        doEventualPuts(cache, prefix);
      }
      barrier.await();
      if (index > 0) {
        System.err.println("Client: " + index + " doing eventual gets..." + prefix);
        failEventualGets(cache, prefix, internaltoolkit);
      }
      barrier.await();
    }

    private void doEventualPuts(Cache cache, final String prefix) {
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        cache.put(getElement(i, prefix));
      }
    }

    private void doEventualGets(Cache cache) {
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        cache.get(getKey(i));
      }
    }

    private void doExplicitStrongPuts(Cache cache, final String prefix) {
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        cache.acquireWriteLockOnKey(getKey(i));
        try {
          cache.put(getElement(i, prefix));
        } finally {
          cache.releaseWriteLockOnKey(getKey(i));
        }
      }
    }

    private void doExplicitStrongGets(Cache cache, final String prefix) {
      boolean result = true;
      for (int i = NUM_ELEMENTS - 1; i >= 0; i--) {
        cache.acquireReadLockOnKey(getKey(i));
        try {
          Element element = cache.get(getKey(i));
          boolean eq = getValue(i, prefix).equals(element.getObjectValue());
          result = result ? eq : result;
        } finally {
          cache.releaseReadLockOnKey(getKey(i));
        }
      }
      Assert.assertTrue(result);
    }

    // ** Always called by Client with index > 0;
    private void failExplicitStrongGets(Cache cache, final String prefix, Toolkit internaltoolkit) {
      boolean result = true;
      for (int i = NUM_ELEMENTS - 1; i >= 0; i--) {
        cache.acquireReadLockOnKey(getKey(i));
        try {
          Element element = cache.get(getKey(i));
          boolean eq = getValue(i, prefix).equals(element.getObjectValue());
          if (!eq) {
            System.out.println("HHHH Failed");
          }
          result = result ? eq : result;
        } finally {
          cache.releaseReadLockOnKey(getKey(i));
        }
      }
      ToolkitAtomicLong atomicLong = internaltoolkit.getAtomicLong(QWERTY);
      if (!result) {
        atomicLong.incrementAndGet();
      }
    }

    // ** Always called by Client with index > 0;
    private void failEventualGets(Cache cache, final String prefix, Toolkit internaltoolkit) {
      boolean result = true;
      for (int i = NUM_ELEMENTS - 1; i >= 0; i--) {
        Element element = cache.get(getKey(i));
        boolean eq = getValue(i, prefix).equals(element.getObjectValue());
        result = result ? eq : result;
      }
      ToolkitAtomicLong atomicLong = internaltoolkit.getAtomicLong(QWERTY);
      if (!result) {
        atomicLong.incrementAndGet();
      }
    }

    private void assertOnEventualFailures(Toolkit internaltoolkit) {
      ToolkitAtomicLong atomicLong = internaltoolkit.getAtomicLong(QWERTY);
      long evenutalRuns = atomicLong.longValue();
      Assert.assertTrue(evenutalRuns > 0);
      atomicLong.set(0);
    }

    private Element getElement(int i, String valPrefix) {
      return new Element(getKey(i), getValue(i, valPrefix));
    }

    private String getValue(int i, String valPrefix) {
      return valPrefix + "val_" + i;
    }

    private String getKey(int i) {
      return "key_" + i;
    }

  }


}

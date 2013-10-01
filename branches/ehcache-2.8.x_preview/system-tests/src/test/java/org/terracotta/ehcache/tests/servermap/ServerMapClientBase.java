/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.servermap;

import net.sf.ehcache.Cache;

import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.ehcache.tests.mbean.DSOMBean;
import org.terracotta.ehcache.tests.mbean.DSOMBeanController;

public abstract class ServerMapClientBase extends ClientBase {

  public ServerMapClientBase(String cacheName, String args[]) {
    super(cacheName, args);
  }

  protected static void assertTrue(boolean condition) {
    assertTrue("Assertion failed", condition);
  }

  protected static void assertTrue(String message, boolean condition) {
    if (!condition) { throw new AssertionError(message); }
  }

  protected void assertNull(Object obj) throws AssertionError {
    if (obj != null) { throw new AssertionError("expected null value!"); }
  }

  protected void assertNotNull(Object obj) throws AssertionError {
    if (obj == null) { throw new AssertionError("expected not null value!"); }
  }

  protected void fail(String message) {
    throw new AssertionError(message);
  }

  protected static void assertEquals(Object expected, Object actual) {
    if (null == expected || null == actual) {
      if (expected != null) {
        throw new AssertionError("Expected [" + expected + "], was null");
      } else if (actual != null) { throw new AssertionError("Expected null, was [" + actual + "]"); }
    } else if (!expected.equals(actual)) { throw new AssertionError("Expected [" + expected + "], was [" + actual + "]"); }
  }

  protected void assertRange(int min, int max, Cache cache) throws InterruptedException {
    DSOMBean dsoMBean = new DSOMBeanController("localhost", getTestControlMbean().getGroupsData()[0].getJmxPort(0));
    assertRange(min, max, cache, dsoMBean);
  }

  private void assertRange(int min, int max, Cache cache, DSOMBean dsoMBean) throws InterruptedException {
    long actual = cache.getSize();
    boolean rangeMatching = min <= actual && actual <= max;
    int count = 0;
    while (count < 10) {
      try {
        assertTrue("assert range failed: min: " + min + " max: " + max + " actual: " + actual, rangeMatching);
        return;
      } catch (Throwable t) {
        System.err.println("Count=" + count);
        System.err.println("Got Exception " + t.getMessage());
        System.err.println("Calling System gc");

        for (int i = 0; i < 10; i++) {
          System.gc();
        }

        sleep(10000);
        count++;
      }
      actual = cache.getSize();
      rangeMatching = min <= actual && actual <= max;
    }

    if (!rangeMatching) {
      try {
        dsoMBean.dumpClusterState();
      } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException("not able to take clusterDump: " + e.getMessage());
      }
      Thread.sleep(1 * 60 * 1000);
    }
    assertTrue("assert range failed: min: " + min + " max: " + max + " actual: " + actual, rangeMatching);
  }

  private void sleep(long millis) {
    try {
      System.err.println("Sleeping for " + millis + " millis");
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      // nothing to do, just ignore
    }
  }
}

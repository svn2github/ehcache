/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.terracotta.api.ClusteringToolkit;
import org.terracotta.api.TerracottaClient;

public abstract class ClientBase {

  private final String name;
  private final String terracottaUrl;
  
  private CacheManager cacheManager;
  private TerracottaClient terracottaClient;
  
  public ClientBase(String cacheName, String args[]) {
    this.name = cacheName;
    this.terracottaUrl = args[0];
  }

  public final void run() {
    try {
      test(setupCache(), getClusteringToolkit());
      pass();
      System.exit(0);
    } catch (Throwable t) {
      t.printStackTrace();
      System.exit(1);
    }
  }

  protected Cache setupCache() {
    cacheManager = new CacheManager(Client1.class.getResourceAsStream("/ehcache-config.xml"));
    return cacheManager.getCache(name);
  }
  
  public CacheManager getCacheManager() {
    return cacheManager;
  }
  
  protected ClusteringToolkit getClusteringToolkit() {
    return getTerracottaClient().getToolkit();
  }

  public synchronized TerracottaClient getTerracottaClient() {
    if (terracottaClient == null) {
      terracottaClient = new TerracottaClient(terracottaUrl);
    }
    return terracottaClient;
  }

  public synchronized void clearTerracottaClient() {
    terracottaClient = null;
    cacheManager = null;
  }

  protected abstract void test(Cache cache, ClusteringToolkit toolkit) throws Throwable;

  protected void pass() {
    System.out.println("[PASS: " + getClass().getName() + "]");
  }

  protected void assertTrue(boolean condition) {
    assertTrue("Assertion failed", condition);
  }

  protected void assertTrue(String message, boolean condition) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }

  protected void assertEquals(Object expected, Object actual) {
    if (null == expected || null == actual) {
      if (expected != null) {
        throw new AssertionError("Expected [" + expected + "], was null");
      } else if (actual != null) {
        throw new AssertionError("Expected null, was [" + actual + "]");
      }
    } else if (!expected.equals(actual)) {
      throw new AssertionError("Expected [" + expected + "], was [" + actual + "]");
    }
  }
}

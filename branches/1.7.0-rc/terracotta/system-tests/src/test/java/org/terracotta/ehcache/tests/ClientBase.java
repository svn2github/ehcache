/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

public abstract class ClientBase {

  private final String name;
  
  public ClientBase(String cacheName, String args[]) {
    this.name = cacheName;
  }

  public final void run() {
    try {
      test(setupCache());
      pass();
    } catch (Throwable t) {
      t.printStackTrace();
      System.exit(1);
    }
  }

  private Cache setupCache() {
    CacheManager mgr = new CacheManager(Client1.class.getResourceAsStream("/ehcache-config.xml"));
    return mgr.getCache(name);
  }

  protected abstract void test(Cache cache) throws Throwable;

  protected void pass() {
    System.err.println("[PASS: " + getClass().getName() + "]");
    System.exit(0);
  }

}

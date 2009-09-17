/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

public abstract class ClientBase {

  static {
    // XXX: remove me!!!
    System.setProperty("tc-ehcache-clustered", "true");
  }

  public ClientBase(String args[]) {
    // XXX: remove me!!
    System.setProperty("HACK-DSO-PORT", args[0]);
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
    CacheManager mgr = new CacheManager(Client1.class.getResourceAsStream("/basic-cache-test.xml"));
    return mgr.getCache("test");
  }

  protected abstract void test(Cache cache) throws Throwable;

  protected void pass() {
    System.err.println("[PASS: " + getClass().getName() + "]");
    System.exit(0);
  }

}

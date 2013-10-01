/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.toolkit.Toolkit;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Chris Dennis
 */
public class GetKeysClient extends ClientBase {

  public static void main(String[] args) {
    new GetKeysClient(args).run();
  }

  public GetKeysClient(String[] args) {
    super("test", args);
  }

  @Override
  protected void runTest(Cache cache, Toolkit toolkit) {
    cache.put(new Element(new Date(), "now"));

    List keys = cache.getKeys();
    boolean interrupted = false;
    try {
      long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
      while (System.nanoTime() < end && keys.isEmpty()) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          interrupted = true;
        }
        keys = cache.getKeys();
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
    if (keys.isEmpty()) {
      throw new AssertionError();
    }
    for (Object key : keys) {
      if (!(key instanceof Date)) {
        throw new AssertionError("Expected Date type for key");
      }
    }
  }
}

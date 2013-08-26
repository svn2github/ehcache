/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.otherclassloader;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.NotificationScope;

import java.io.ByteArrayInputStream;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

public class Client implements Runnable, CacheEventListener {

  private final AtomicInteger putEvents = new AtomicInteger();
  private final String        config;
  private final CyclicBarrier barrier;

  public Client(String config, CyclicBarrier barrier) {
    this.config = config;
    this.barrier = barrier;
  }

  public void run() {
    try {
      run0();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void run0() throws Exception {
    CacheManager cm = CacheManager.create(new ByteArrayInputStream(config.getBytes()));

    cm.getCache("test").getCacheEventNotificationService().registerListener(this, NotificationScope.ALL);

    barrier.await();

    long end = System.currentTimeMillis() + 60000L;
    while (System.currentTimeMillis() < end) {
      int puts = putEvents.get();
      System.err.println("puts: " + puts);
      if (puts == 1) { return; }
      if (puts > 1) { throw new AssertionError(puts); }
      Thread.sleep(500);
    }

    throw new AssertionError("never saw event");
  }

  public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
    //
  }

  public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
    System.err.println("TCCL: " + Thread.currentThread().getContextClassLoader());
    System.err.println("put(" + element.getValue() + ") with loader " + element.getValue().getClass().getClassLoader());

    if (element.getValue().getClass().getClassLoader() != getClass().getClassLoader()) { throw new AssertionError(); }

    // do this after the assertions (so test will fail)
    putEvents.incrementAndGet();
  }

  public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
    //
  }

  public void notifyElementExpired(Ehcache cache, Element element) {
    //
  }

  public void notifyElementEvicted(Ehcache cache, Element element) {
    //
  }

  public void notifyRemoveAll(Ehcache cache) {
    //
  }

  public void dispose() {
    //
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    throw new CloneNotSupportedException();
  }

}

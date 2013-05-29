package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;
import org.terracotta.test.util.WaitUtil;
import org.terracotta.toolkit.Toolkit;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.Assert;

public class ExpiryListenerClient1 extends ClientBase implements CacheEventListener {

  private final AtomicLong localExpiredCount = new AtomicLong();

  public ExpiryListenerClient1(String[] args) {
    super("test", args);
  }

  public static void main(String[] args) {
    new ExpiryListenerClient1(args).run();
  }

  @Override
  protected void runTest(final Cache cache, final Toolkit toolkit) throws Throwable {
    cache.getCacheEventNotificationService().registerListener(this);
    cache.put(new Element("key", "value"));
    // assume the TTL of the cache is set to 3s
    System.out.println("TTL value of the cache: " + cache.getCacheConfiguration().getTimeToLiveSeconds());
    Assert.assertEquals(3, cache.getCacheConfiguration().getTimeToLiveSeconds());

    WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return cache.getSize() == 0 && localExpiredCount.get() > 0;
      }
    });
    // assert eviction has already occurred
    Assert.assertEquals(0, cache.getSize());
    Assert.assertTrue(localExpiredCount.get() > 0);
  }

  public void dispose() {
    //
  }

  public void notifyElementEvicted(Ehcache cache, Element element) {
    //
  }

  public void notifyElementExpired(Ehcache cache, Element element) {
    localExpiredCount.incrementAndGet();
    Assert.assertNotNull(element.getKey());
    Assert.assertNull(element.getValue());
    System.out.println("Got evicted: " + element);
  }

  public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
    //
  }

  public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
    //
  }

  public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
    //
  }

  public void notifyRemoveAll(Ehcache cache) {
    //
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }
}
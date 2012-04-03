package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;

import org.terracotta.toolkit.Toolkit;

import junit.framework.Assert;

public class ExpiryListenerClient1 extends ClientBase implements CacheEventListener {

  public ExpiryListenerClient1(String[] args) {
    super("test", args);
  }

  public static void main(String[] args) {
    new ExpiryListenerClient1(args).run();
  }

  @Override
  protected void runTest(Cache cache, Toolkit toolkit) throws Throwable {
    cache.getCacheEventNotificationService().registerListener(this);
    cache.put(new Element("key", "value"));
    // assume the TTL of the cache is set to 3s
    System.out.println("TTL value of the cache: " + cache.getCacheConfiguration().getTimeToLiveSeconds());
    Assert.assertEquals(3, cache.getCacheConfiguration().getTimeToLiveSeconds());

    int tries = 0;
    do {
      Thread.sleep(15 * 1000);
      tries++;
      System.out.println("XXX Cache size: " + cache.getSize() + ", tries: " + tries);
      for (Object k : cache.getKeys()) {
        System.err.println("XXX key = " + k + "; value: " + cache.get(k));
      }
    } while (cache.getSize() > 0);

    // assert eviction has already occurred
    Assert.assertEquals(1, cache.getCacheEventNotificationService().getElementsExpiredCounter());
    Assert.assertEquals(0, cache.getSize());
  }

  public void dispose() {
    //
  }

  public void notifyElementEvicted(Ehcache cache, Element element) {
    //
  }

  public void notifyElementExpired(Ehcache cache, Element element) {
    Assert.assertNotNull(element.getKey());
    Assert.assertNotNull(element.getValue());
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
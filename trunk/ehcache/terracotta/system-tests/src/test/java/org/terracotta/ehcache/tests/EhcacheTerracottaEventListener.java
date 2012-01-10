package org.terracotta.ehcache.tests;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;

public class EhcacheTerracottaEventListener implements CacheEventListener {
  private void printThread() {
    System.out.println("Current thread: " + Thread.currentThread().getName());
  }

  public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
    System.out.println("[notifyElementRemoved " + element + "]");
    printThread();
  }

  public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
    System.out.println("[notifyElementPut " + element.getObjectKey() + ", " + element.getValue() + "]");
    printThread();
  }

  public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
    System.out.println("[notifyElementUpdated " + element.getObjectKey() + ", " + element.getValue() + "]");
    printThread();
  }

  public void notifyElementExpired(Ehcache cache, Element element) {
    System.out.println("[notifyElementExpired " + element.getObjectKey() + ", " + element.getValue() + "]");
    printThread();
  }

  public void notifyElementEvicted(Ehcache cache, Element element) {
    System.out.println("[notifyElementEvicted " + element.getObjectKey() + ", " + element.getValue() + "]");
    printThread();
  }

  public void notifyRemoveAll(Ehcache cache) {
    System.out.println("[notifyRemoveAll]");
    printThread();
  }

  public void dispose() {
    // no-op
  }

  @Override
  public EhcacheTerracottaEventListener clone() {
    throw new UnsupportedOperationException();
  }
}

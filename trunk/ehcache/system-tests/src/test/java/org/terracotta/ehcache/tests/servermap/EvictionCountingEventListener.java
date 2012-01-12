/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.servermap;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;

import org.terracotta.util.ClusteredAtomicLong;

public class EvictionCountingEventListener implements CacheEventListener {

  private final ClusteredAtomicLong count;

  public EvictionCountingEventListener(ClusteredAtomicLong clusteredAtomicLong) {
    this.count = clusteredAtomicLong;
  }

  public void notifyElementEvicted(Ehcache cache, Element element) {
    long val = count.incrementAndGet();
    if (val % 100 == 0) {
      System.out.println("EvictionListener: number of elements evicted till now: " + val);
    }
  }

  public void dispose() {
    //
  }

  public void notifyElementExpired(Ehcache cache, Element element) {
    //
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

  public long getEvictedCount() {
    return this.count.get();
  }

}

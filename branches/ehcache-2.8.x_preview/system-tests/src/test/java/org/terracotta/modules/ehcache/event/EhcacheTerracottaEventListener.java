/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.event;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class EhcacheTerracottaEventListener implements CacheEventListener {
  private final List<Element> put       = new CopyOnWriteArrayList<Element>();
  private final List<Element> update    = new CopyOnWriteArrayList<Element>();
  private final List<Element> remove    = new CopyOnWriteArrayList<Element>();
  private final List<Element> expired   = new CopyOnWriteArrayList<Element>();
  private final List<Element> evicted   = new CopyOnWriteArrayList<Element>();
  private int                 removeAll = 0;

  public List<Element> getPut() {
    return put;
  }

  public List<Element> getUpdate() {
    return update;
  }

  public List<Element> getRemove() {
    return remove;
  }

  public List<Element> getExpired() {
    return expired;
  }

  public List<Element> getEvicted() {
    return evicted;
  }

  public int getRemoveAll() {
    return removeAll;
  }

  public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
    put.add(element);
  }

  public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
    update.add(element);
  }

  public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
    remove.add(element);
  }

  public void notifyElementExpired(Ehcache cache, Element element) {
    expired.add(element);
  }

  public void notifyElementEvicted(Ehcache cache, Element element) {
    evicted.add(element);
  }

  public void notifyRemoveAll(Ehcache cache) {
    removeAll++;
  }

  public void dispose() {
    // no-op
  }

  @Override
  public EhcacheTerracottaEventListener clone() {
    throw new UnsupportedOperationException();
  }
}

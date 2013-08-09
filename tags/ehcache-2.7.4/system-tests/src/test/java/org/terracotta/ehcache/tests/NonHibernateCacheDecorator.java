package org.terracotta.ehcache.tests;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.EhcacheDecoratorAdapter;

import java.io.Serializable;

/**
 * This decorator class intercepts cache gets and acts as a cache read through during or before a cache load and acts as a system of record
 * after the cache has been loaded. Cache read through: get the object from the cache, if not in cache, get object from database, load in to
 * cache and return object. System of Record (SOR): only get the object from the cache.
 * 
 * @author sdalto2
 * 
 */
public class NonHibernateCacheDecorator extends EhcacheDecoratorAdapter {

  private Ehcache cache;
  public NonHibernateCacheDecorator(Ehcache cache) {
    super(cache);
    this.cache=cache;
  }

  /**
   * This get method will always call Element get(Serializable key) So a serializable key is mandatory.
   */
  @Override
  public Element get(Object key) throws IllegalStateException, CacheException {
    return get((Serializable) key);
  }

  @Override
  public Element get(Serializable key) throws IllegalStateException, CacheException {
    return new Element(1,"dummy");
  }
  @Override
  public void removeAll(boolean doNotNotifyListeners) throws IllegalStateException, CacheException {
	  cache.removeAll(doNotNotifyListeners);
  }

}
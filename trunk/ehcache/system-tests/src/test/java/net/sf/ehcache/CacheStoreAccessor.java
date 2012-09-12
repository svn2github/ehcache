/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package net.sf.ehcache;

import net.sf.ehcache.store.Store;

public class CacheStoreAccessor {

  private final Cache cache;

  public CacheStoreAccessor(Cache cache) {
    this.cache = cache;
  }

  public Store getStore() {
    return cache.getStore();
  }

  public static CacheStoreAccessor newCacheStoreAccessor(Cache cache) {
    return new CacheStoreAccessor(cache);
  }

}

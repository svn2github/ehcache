/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheAccessor;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.store.Store;

import org.terracotta.toolkit.Toolkit;

public class ToolkitClientAccessor {

  public static Toolkit getInternalToolkitClient(Cache cache) {
    CacheConfiguration cacheConfiguration = cache.getCacheConfiguration();
    if (cacheConfiguration.isTerracottaClustered()) {
      CacheAccessor storeAccessor = CacheAccessor.newCacheAccessor(cache);
      Store store = null;
      store = storeAccessor.getStore();
      Object internalContext = store.getInternalContext();
      if (internalContext instanceof ToolkitLookup) {
        return ((ToolkitLookup) internalContext).getToolkit();
      } else {
        throw new AssertionError("Internal context of cache '" + cache.getName() + "' is not of type ToolkitLookup");
      }
    } else {
      throw new AssertionError("Toolkit can only be looked up for Clustered Caches - unclustered ehcache name: "
                               + cache.getName());
    }
  }

}

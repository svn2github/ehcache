/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheAccessor;
import net.sf.ehcache.constructs.nonstop.store.NonstopStore;
import net.sf.ehcache.store.TerracottaStore;

import org.terracotta.toolkit.Toolkit;

public class ToolkitClientAccessor {

  public static Toolkit getInternalToolkitClient(Cache cache) {
    if (cache.getCacheConfiguration().isTerracottaClustered()) {
      CacheAccessor storeAccessor = CacheAccessor.newCacheAccessor(cache);
      TerracottaStore store = (TerracottaStore) storeAccessor.getStore();
      if (store instanceof NonstopStore) {
        store = storeAccessor.getNonstopActiveDelegateHolder().getUnderlyingTerracottaStore();
      }
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

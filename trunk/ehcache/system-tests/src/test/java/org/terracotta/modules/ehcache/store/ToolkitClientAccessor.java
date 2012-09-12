/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheStoreAccessor;
import net.sf.ehcache.store.Store;

import org.terracotta.toolkit.Toolkit;

public class ToolkitClientAccessor {

  public static Toolkit getInternalToolkitClient(Cache cache) {
    CacheStoreAccessor storeAccessor = CacheStoreAccessor.newCacheStoreAccessor(cache);
    Store store = storeAccessor.getStore();
    if (store instanceof ClusteredStore) {
      return ((ClusteredStore) store).getInternalToolkit();
    } else {
      // Return null for non-clustered stores
      return null;
    }
  }

}

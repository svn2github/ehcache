/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation.model;

import java.util.EventListener;

public interface CacheManagerModelListener extends EventListener {
  void instanceAdded(CacheManagerInstance instance);

  void instanceRemoved(CacheManagerInstance instance);

  void cacheModelAdded(CacheModel cacheModel);

  void cacheModelRemoved(CacheModel cacheModel);

  void cacheModelChanged(CacheModel cacheModel);

  void clusteredCacheModelAdded(ClusteredCacheModel cacheModel);

  void clusteredCacheModelRemoved(ClusteredCacheModel cacheModel);

  void clusteredCacheModelChanged(ClusteredCacheModel cacheModel);

  void standaloneCacheModelAdded(StandaloneCacheModel cacheModel);

  void standaloneCacheModelRemoved(StandaloneCacheModel cacheModel);

  void standaloneCacheModelChanged(StandaloneCacheModel cacheModel);
}

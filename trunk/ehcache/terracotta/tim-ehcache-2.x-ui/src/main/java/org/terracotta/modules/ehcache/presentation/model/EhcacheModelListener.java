/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation.model;

import java.util.EventListener;

public interface EhcacheModelListener extends EventListener {
  void cacheManagerModelAdded(CacheManagerModel cacheManagerModel);

  void cacheManagerModelRemoved(CacheManagerModel cacheManagerModel);
}

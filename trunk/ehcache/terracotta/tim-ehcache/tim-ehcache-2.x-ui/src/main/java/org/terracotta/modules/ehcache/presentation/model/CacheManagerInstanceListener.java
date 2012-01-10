/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation.model;

import java.util.EventListener;

public interface CacheManagerInstanceListener extends EventListener {
  void cacheManagerInstanceChanged(CacheManagerInstance cacheManagerInstance);

  void cacheModelInstanceAdded(CacheModelInstance cacheModelInstance);

  void cacheModelInstanceRemoved(CacheModelInstance cacheModelInstance);

  void cacheModelInstanceChanged(CacheModelInstance cacheModelInstance);
}

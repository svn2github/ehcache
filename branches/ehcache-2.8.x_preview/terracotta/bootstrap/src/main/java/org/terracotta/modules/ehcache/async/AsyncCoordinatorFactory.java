/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.async;

import net.sf.ehcache.Ehcache;

public interface AsyncCoordinatorFactory {
  AsyncCoordinator getOrCreateAsyncCoordinator(final Ehcache cache, final AsyncConfig config);

  boolean destroy(String cacheManagerName, String cacheName);
}

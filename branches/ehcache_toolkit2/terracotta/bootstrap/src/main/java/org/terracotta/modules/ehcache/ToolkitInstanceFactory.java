/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache;

import net.sf.ehcache.Ehcache;

import org.terracotta.modules.ehcache.store.CacheConfigChangeNotificationMsg;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.events.ToolkitNotifier;
import org.terracotta.toolkit.internal.collections.ToolkitCacheWithMetadata;

import java.io.Serializable;

/**
 * Factory used for creating {@link Toolkit} instances used for implementing clustered ehcache
 */
public interface ToolkitInstanceFactory {

  Toolkit getToolkit();

  String getFullyQualifiedCacheName(Ehcache cache);

  ToolkitCacheWithMetadata<Object, Serializable> getOrCreateToolkitCache(Ehcache cache);

  ToolkitNotifier<CacheConfigChangeNotificationMsg> getOrCreateConfigChangeNotifier(Ehcache cache);

}

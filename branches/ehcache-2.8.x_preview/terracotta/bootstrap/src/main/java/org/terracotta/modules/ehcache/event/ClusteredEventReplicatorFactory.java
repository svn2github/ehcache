/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.event;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.event.CacheEventListener;

import org.terracotta.modules.ehcache.ToolkitInstanceFactory;

import java.util.HashMap;
import java.util.Map;

public class ClusteredEventReplicatorFactory {
  private final Map<String, CacheEventListener> eventReplicators = new HashMap<String, CacheEventListener>();
  private final ToolkitInstanceFactory          toolkitInstanceFactory;

  public ClusteredEventReplicatorFactory(ToolkitInstanceFactory toolkitInstanceFactory) {
    this.toolkitInstanceFactory = toolkitInstanceFactory;
  }

  public synchronized CacheEventListener getOrCreateClusteredEventReplicator(Ehcache cache) {
    String fullyQualifiedCacheName = toolkitInstanceFactory.getFullyQualifiedCacheName(cache);
    CacheEventListener replicator = eventReplicators.get(fullyQualifiedCacheName);
    if (replicator == null) {
      NonstopConfiguration nonStopConfiguration = cache.getCacheConfiguration().getTerracottaConfiguration()
          .getNonstopConfiguration();
      CacheEventListener clusteredEventReplicator = new ClusteredEventReplicator(
                                                                                 cache,
                                                                                 fullyQualifiedCacheName,
                                                                                 toolkitInstanceFactory
                                                                                     .getOrCreateCacheEventNotifier(cache),
                                                                                 this);
      replicator = new NonStopEventReplicator(clusteredEventReplicator, toolkitInstanceFactory, nonStopConfiguration);
      eventReplicators.put(fullyQualifiedCacheName, replicator);
    }
    return replicator;
  }

  public synchronized void disposeClusteredEventReplicator(String fullyQualifiedCacheName) {
    eventReplicators.remove(fullyQualifiedCacheName);
  }

}

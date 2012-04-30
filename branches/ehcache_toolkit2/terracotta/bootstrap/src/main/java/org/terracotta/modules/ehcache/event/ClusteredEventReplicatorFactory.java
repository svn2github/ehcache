/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.event;

import net.sf.ehcache.Ehcache;

import org.terracotta.modules.ehcache.ToolkitInstanceFactory;

import java.util.HashMap;
import java.util.Map;

public class ClusteredEventReplicatorFactory {
  private final Map<String, ClusteredEventReplicator> eventReplicators = new HashMap<String, ClusteredEventReplicator>();

  public synchronized ClusteredEventReplicator getOrCreateClusteredEventReplicator(ToolkitInstanceFactory toolkitInstanceFactory,
                                                                                   Ehcache cache) {
    String fullyQualifiedCacheName = toolkitInstanceFactory.getFullyQualifiedCacheName(cache);
    ClusteredEventReplicator replicator = eventReplicators.get(fullyQualifiedCacheName);
    if (replicator == null) {
      replicator = new ClusteredEventReplicator(cache, fullyQualifiedCacheName,
                                                toolkitInstanceFactory.getOrCreateCacheEventNotifier(cache), this);
      eventReplicators.put(fullyQualifiedCacheName, replicator);
    }
    return replicator;
  }

  public synchronized void disposeClusteredEventReplicator(String fullyQualifiedCacheName) {
    eventReplicators.remove(fullyQualifiedCacheName);
  }

}

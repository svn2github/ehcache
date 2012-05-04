/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.transaction;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.transaction.SoftLockFactory;

import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.modules.ehcache.transaction.state.EhcacheTxnsClusteredStateFacade;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SoftLockFactoryProvider {
  private final ConcurrentMap<String, SoftLockFactory> softLockFactories = new ConcurrentHashMap<String, SoftLockFactory>();
  private final EhcacheTxnsClusteredStateFacade        ehcacheTxnsClusteredFacade;
  private final ToolkitInstanceFactory                 toolkitInstanceFactory;

  public SoftLockFactoryProvider(final EhcacheTxnsClusteredStateFacade ehcacheTxnsClusteredFacade,
                                 ToolkitInstanceFactory toolkitInstanceFactory) {
    this.ehcacheTxnsClusteredFacade = ehcacheTxnsClusteredFacade;
    this.toolkitInstanceFactory = toolkitInstanceFactory;
  }

  public SoftLockFactory getOrCreateClusteredSoftLockFactory(Ehcache cache) {
    String name = toolkitInstanceFactory.getFullyQualifiedCacheName(cache);
    SoftLockFactory softLockFactory = softLockFactories.get(name);
    if (softLockFactory == null) {
      softLockFactory = new ReadCommittedClusteredSoftLockFactory(ehcacheTxnsClusteredFacade, cache.getCacheManager()
          .getName(), cache.getName());
      SoftLockFactory old = softLockFactories.putIfAbsent(name, softLockFactory);
      if (old == null) {
        // Put successful add a Cache Event Listener.
        cache.getCacheEventNotificationService().registerListener(new EventListener(name));
      } else {
        softLockFactory = old;
      }
    }
    return softLockFactory;
  }

  private void disposeSoftLockFactory(String fullyQualifiedCacheName) {
    softLockFactories.remove(fullyQualifiedCacheName);
  }

  private class EventListener implements CacheEventListener {
    private final String fullyQualifiedCacheName;

    private EventListener(String fullyQualifiedCacheName) {
      this.fullyQualifiedCacheName = fullyQualifiedCacheName;
    }

    @Override
    public void dispose() {
      disposeSoftLockFactory(fullyQualifiedCacheName);
    }

    @Override
    public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
      // DO Nothing
    }

    @Override
    public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
      // DO Nothing
    }

    @Override
    public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
      // DO Nothing
    }

    @Override
    public void notifyElementExpired(Ehcache cache, Element element) {
      // DO Nothing
    }

    @Override
    public void notifyElementEvicted(Ehcache cache, Element element) {
      // DO Nothing
    }

    @Override
    public void notifyRemoveAll(Ehcache cache) {
      // DO Nothing
    }

    @Override
    public EventListener clone() throws CloneNotSupportedException {
      return (EventListener) super.clone();
    }

  }

}

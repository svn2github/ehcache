/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.transaction;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.transaction.SoftLockManager;

import org.terracotta.modules.ehcache.ToolkitInstanceFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SoftLockManagerProvider {
  private final ConcurrentMap<String, SoftLockManager> softLockFactories = new ConcurrentHashMap<String, SoftLockManager>();
  private final ToolkitInstanceFactory                 toolkitInstanceFactory;

  public SoftLockManagerProvider(
                                 ToolkitInstanceFactory toolkitInstanceFactory) {
    this.toolkitInstanceFactory = toolkitInstanceFactory;
  }

  public SoftLockManager getOrCreateClusteredSoftLockFactory(Ehcache cache) {
    String name = toolkitInstanceFactory.getFullyQualifiedCacheName(cache);
    SoftLockManager softLockFactory = softLockFactories.get(name);
    if (softLockFactory == null) {
      softLockFactory = new ReadCommittedClusteredSoftLockFactory(toolkitInstanceFactory, cache.getCacheManager()
          .getName(), cache.getName());
      SoftLockManager old = softLockFactories.putIfAbsent(name, softLockFactory);
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

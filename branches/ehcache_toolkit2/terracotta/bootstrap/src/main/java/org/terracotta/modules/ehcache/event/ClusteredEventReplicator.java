/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.event;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.RegisteredEventListeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.modules.ehcache.event.CacheEventNotificationMsg.EventType;
import org.terracotta.toolkit.events.ToolkitNotificationEvent;
import org.terracotta.toolkit.events.ToolkitNotificationListener;
import org.terracotta.toolkit.events.ToolkitNotifier;

public class ClusteredEventReplicator implements CacheEventListener {
  private static final Logger                              LOG = LoggerFactory
                                                                   .getLogger(ClusteredEventReplicator.class);
  private final ToolkitNotifier<CacheEventNotificationMsg> toolkitNotifier;
  private final String                                     fullyQualifiedEhcacheName;
  private final Ehcache                                    ecache;
  private final ClusteredEventReplicatorFactory            factory;
  private final ToolkitListener                            toolkitListener;

  public ClusteredEventReplicator(Ehcache cache, String fullyQualifiedEhcacheName,
                                  ToolkitNotifier<CacheEventNotificationMsg> toolkitNotifier,
                                  ClusteredEventReplicatorFactory factory) {
    this.fullyQualifiedEhcacheName = fullyQualifiedEhcacheName;
    this.ecache = cache;
    this.toolkitNotifier = toolkitNotifier;
    toolkitListener = new ToolkitListener();
    this.toolkitNotifier.addNotificationListener(toolkitListener);
    this.factory = factory;
  }

  @Override
  public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
    sendEvent(CacheEventNotificationMsg.EventType.ELEMENT_REMOVED, element);
  }

  @Override
  public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
    sendEvent(CacheEventNotificationMsg.EventType.ELEMENT_PUT, element);
  }

  @Override
  public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
    sendEvent(CacheEventNotificationMsg.EventType.ELEMENT_UPDATED, element);
  }

  @Override
  public void notifyElementExpired(Ehcache cache, Element element) {
    sendEvent(CacheEventNotificationMsg.EventType.ELEMENT_EXPIRED, element);
  }

  @Override
  public void notifyElementEvicted(Ehcache cache, Element element) {
    sendEvent(CacheEventNotificationMsg.EventType.ELEMENT_EVICTED, element);
  }

  @Override
  public void notifyRemoveAll(Ehcache cache) {
    sendEvent(CacheEventNotificationMsg.EventType.REMOVEALL, null);
  }

  @Override
  public void dispose() {
    // dispose means dispose the listener for this node locally.
    // No need to propagate this event in cluster.
    toolkitNotifier.removeNotificationListener(toolkitListener);
    factory.disposeClusteredEventReplicator(fullyQualifiedEhcacheName);

  }

  @Override
  public ClusteredEventReplicator clone() throws CloneNotSupportedException {
    return (ClusteredEventReplicator) super.clone();
  }

  private void sendEvent(EventType eventType, Element element) {
    toolkitNotifier.notifyListeners(new CacheEventNotificationMsg(fullyQualifiedEhcacheName, eventType, element));
  }

  private class ToolkitListener implements ToolkitNotificationListener {
    @Override
    public void onNotification(ToolkitNotificationEvent event) {
      if (shouldProcessNotification(event)) {
        processEventNotification((CacheEventNotificationMsg) event.getMessage());
      } else {
        LOG.warn("Ignoring uninterested notification - " + event);
      }
    }


    private void processEventNotification(CacheEventNotificationMsg msg) {
      RegisteredEventListeners notificationService = ecache.getCacheEventNotificationService();
      switch (msg.getToolkitEventType()) {
        case ELEMENT_REMOVED:
          notificationService.notifyElementRemoved(msg.getElement(), true);
          break;
        case ELEMENT_PUT:
          notificationService.notifyElementPut(msg.getElement(), true);
          break;
        case ELEMENT_UPDATED:
          notificationService.notifyElementUpdated(msg.getElement(), true);
          break;
        case ELEMENT_EXPIRED:
          notificationService.notifyElementExpiry(msg.getElement(), true);
          break;
        case ELEMENT_EVICTED:
          notificationService.notifyElementEvicted(msg.getElement(), true);
          break;
        case REMOVEALL:
          notificationService.notifyRemoveAll(true);
          break;
      }
    }

    private boolean shouldProcessNotification(ToolkitNotificationEvent event) {
      return event.getMessage() instanceof CacheEventNotificationMsg
             && ((CacheEventNotificationMsg) event.getMessage()).getFullyQualifiedEhcacheName()
                 .equals(fullyQualifiedEhcacheName);
    }

  }

}

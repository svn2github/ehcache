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
import org.terracotta.toolkit.cluster.ClusterNode;
import org.terracotta.toolkit.events.ToolkitNotificationListener;
import org.terracotta.toolkit.events.ToolkitNotifier;

import java.io.Serializable;

public class ClusteredEventReplicator implements CacheEventListener,
    ToolkitNotificationListener<CacheEventNotificationMsg> {
  private static final Logger                              LOG = LoggerFactory
                                                                   .getLogger(ClusteredEventReplicator.class);
  private final ToolkitNotifier<CacheEventNotificationMsg> toolkitNotifier;
  private final String                                     fullyQualifiedEhcacheName;
  private final Ehcache                                    cache2;

  public ClusteredEventReplicator(Ehcache cache, String fullyQualifiedEhcacheName,
                                  ToolkitNotifier<CacheEventNotificationMsg> toolkitNotifier) {
    this.fullyQualifiedEhcacheName = fullyQualifiedEhcacheName;
    this.cache2 = cache;
    this.toolkitNotifier = toolkitNotifier;
    this.toolkitNotifier.addNotificationListener(this);
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
    sendEvent(CacheEventNotificationMsg.EventType.DISPOSE, null);
  }

  @Override
  public ClusteredEventReplicator clone() throws CloneNotSupportedException {
    return (ClusteredEventReplicator) super.clone();
  }

  private void sendEvent(EventType eventType, Element element) {
    toolkitNotifier.notifyListeners(new CacheEventNotificationMsg(fullyQualifiedEhcacheName, eventType, element));
  }

  @Override
  public void onNotification(ToolkitNotifier<CacheEventNotificationMsg> notifierParam, ClusterNode remoteNode,
                             CacheEventNotificationMsg msg) {
    if (shouldProcessNotification(notifierParam, remoteNode, msg)) {
      processEventNotification(msg);
    } else {
      LOG.warn("Ignoring uninterested notification - notifier: " + notifierParam + ", remoteNode: " + remoteNode
               + ", msg: " + msg);
    }

  }

  private void processEventNotification(CacheEventNotificationMsg msg) {
    RegisteredEventListeners notificationService = cache2.getCacheEventNotificationService();
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
      case DISPOSE:
        notificationService.dispose();
        break;
    }
  }

  private boolean shouldProcessNotification(ToolkitNotifier notifierParam, ClusterNode remoteNode, Serializable msg) {
    return toolkitNotifier == notifierParam && msg instanceof CacheEventNotificationMsg
           && ((CacheEventNotificationMsg) msg).getFullyQualifiedEhcacheName().equals(fullyQualifiedEhcacheName);
  }

}

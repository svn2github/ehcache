/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.event;

import net.sf.ehcache.Element;

import java.io.Serializable;

public class CacheEventNotificationMsg implements Serializable {

  public enum EventType {
    ELEMENT_REMOVED, ELEMENT_PUT, ELEMENT_UPDATED, ELEMENT_EXPIRED, ELEMENT_EVICTED, REMOVEALL
  }

  private final String    fullyQualifiedEhcacheName;
  private final EventType toolkitEventType;
  private final Element   element;

  public CacheEventNotificationMsg(String fullyQualifiedEhcacheName, EventType toolkitEventType, Element element) {
    super();
    this.fullyQualifiedEhcacheName = fullyQualifiedEhcacheName;
    this.toolkitEventType = toolkitEventType;
    this.element = element;
  }

  public String getFullyQualifiedEhcacheName() {
    return fullyQualifiedEhcacheName;
  }

  public EventType getToolkitEventType() {
    return toolkitEventType;
  }

  public Element getElement() {
    return element;
  }

  @Override
  public String toString() {
    return "CacheEventNotificationMsg [fullyQualifiedEhcacheName=" + fullyQualifiedEhcacheName + ", toolkitEventtype="
           + toolkitEventType + ", element=" + element + "]";
  }

}

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import java.io.Serializable;

public class CacheConfigChangeNotificationMsg implements Serializable {
  private final String       fullyQualifiedEhcacheName;
  private final String       toolkitConfigName;
  private final Serializable newValue;

  public CacheConfigChangeNotificationMsg(String fullyQualifiedCacheName, String configName, Serializable newValue) {
    this.fullyQualifiedEhcacheName = fullyQualifiedCacheName;
    this.toolkitConfigName = configName;
    this.newValue = newValue;
  }

  public String getToolkitConfigName() {
    return toolkitConfigName;
  }

  public Serializable getNewValue() {
    return newValue;
  }

  public String getFullyQualifiedEhcacheName() {
    return fullyQualifiedEhcacheName;
  }

  @Override
  public String toString() {
    return "CacheConfigChangeNotificationMsg [fullyQualifiedEhcacheName=" + fullyQualifiedEhcacheName
           + ", toolkitConfigName=" + toolkitConfigName + ", newValue=" + newValue + "]";
  }

}

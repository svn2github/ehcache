/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package net.sf.ehcache.management.resource;

import java.util.Map;

/**
 * @author brandony
 */
public class CacheStatisticSampleEntity extends AbstractCacheEntity{

  private String statName;

  private Map<Long, Long> statValueByTimeMillis;

  public String getStatName() {
    return statName;
  }

  public void setStatName(String statName) {
    this.statName = statName;
  }

  public Map<Long, Long> getStatValueByTimeMillis() {
    return statValueByTimeMillis;
  }

  public void setStatValueByTimeMillis(Map<Long, Long> statValueByTimeMillis) {
    this.statValueByTimeMillis = statValueByTimeMillis;
  }
}

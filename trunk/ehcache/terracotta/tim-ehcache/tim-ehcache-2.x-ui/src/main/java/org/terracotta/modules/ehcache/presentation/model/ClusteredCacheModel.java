/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.management.ObjectName;

public class ClusteredCacheModel extends SettingsCacheModel {
  public ClusteredCacheModel(CacheManagerModel cacheManagerModel, String cacheName, ObjectName beanName) {
    super(cacheManagerModel, cacheName);
    addInstance(beanName);
  }

  @Override
  public Set<CacheModelInstance> cacheModelInstances() {
    return cacheManagerModel.clusteredCacheModelInstances(this);
  }

  public void addInstance(ObjectName on) {
    onSet.add(on);
    if (onSet.size() == 1) {
      setAttributes(getAttributes(on, new HashSet(Arrays.asList(MBEAN_ATTRS))));
    }
    addNotificationListener(on, this);
  }

  public void removeInstance(ObjectName on) {
    onSet.remove(on);
  }

  public int instanceCount() {
    return beanCount();
  }

  @Override
  public int getStatisticsEnabledCount() {
    return cacheManagerModel.getStatisticsEnabledCount(this);
  }

  @Override
  public int getBulkLoadEnabledCount() {
    return cacheManagerModel.getBulkLoadEnabledCount(this);
  }

  @Override
  public int getEnabledCount() {
    return cacheManagerModel.getEnabledCount(this);
  }

  @Override
  public String generateActiveConfigDeclaration() {
    return cacheManagerModel.generateActiveConfigDeclaration(getCacheName());
  }

  @Override
  protected void cacheModelChanged() {
    super.cacheModelChanged();
    cacheManagerModel.fireClusteredCacheModelChanged(this);
  }
}

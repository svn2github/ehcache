/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.management.ObjectName;

public class StandaloneCacheModel extends SettingsCacheModel {
  private final ObjectName         beanName;
  private final CacheModelInstance cacheModelInstance;

  public StandaloneCacheModel(CacheManagerModel cacheManagerModel, CacheModelInstance cacheModelInstance) {
    super(cacheManagerModel, cacheModelInstance.getCacheName());
    this.cacheModelInstance = cacheModelInstance;
    this.beanName = cacheModelInstance.getBeanName();
    setAttributes(getAttributes(beanName, new HashSet(Arrays.asList(MBEAN_ATTRS))));
    addNotificationListener(beanName, this);
  }

  @Override
  public ObjectName getRandomBean() {
    return beanName;
  }

  @Override
  public int beanCount() {
    return 1;
  }

  @Override
  public Set<CacheModelInstance> cacheModelInstances() {
    return Collections.singleton(cacheModelInstance);
  }

  public CacheModelInstance cacheModelInstance() {
    return cacheModelInstance;
  }

  @Override
  protected Set<ObjectName> getActiveCacheModelBeans() {
    return Collections.singleton(beanName);
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
    cacheManagerModel.fireStandaloneCacheModelChanged(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((beanName == null) ? 0 : beanName.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) { return true; }
    if (!super.equals(obj)) { return false; }
    if (!(obj instanceof StandaloneCacheModel)) { return false; }
    StandaloneCacheModel other = (StandaloneCacheModel) obj;
    if (beanName == null) {
      if (other.beanName != null) { return false; }
    } else if (!beanName.equals(other.beanName)) { return false; }
    return true;
  }
}

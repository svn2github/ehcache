/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation;

import org.terracotta.modules.ehcache.presentation.model.CacheManagerInstance;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerModel;
import org.terracotta.modules.ehcache.presentation.model.CacheModel;
import org.terracotta.modules.ehcache.presentation.model.CacheModelInstance;

import java.util.Map;

public interface ManageChannel {
  public boolean isEnabled(CacheModelInstance cacheModelInstance);

  public boolean isEnabled(CacheManagerInstance cacheManagerInstance);

  public boolean isEnabled(CacheModel cacheModel);

  public boolean getValue(CacheModelInstance cacheModelInstance);

  public boolean getValue(CacheManagerInstance cacheManagerInstance);

  public boolean getValue(CacheModel cacheModel);

  public boolean getValue(CacheManagerModel theCacheManagerModel);

  public void setValue(CacheManagerModel cacheManagerModel, boolean value, boolean applyToNewcomers);

  public void setNodeViewValues(Map<CacheManagerInstance, Boolean> cacheManagerInstances,
                                Map<CacheModelInstance, Boolean> cacheModelInstances);

  public void setCacheViewValues(Map<CacheModel, Boolean> cacheModels,
                                 Map<CacheModelInstance, Boolean> cacheModelInstances);

}

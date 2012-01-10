/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation;

import org.terracotta.modules.ehcache.presentation.TopologyPanel.Mode;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerInstance;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerModel;
import org.terracotta.modules.ehcache.presentation.model.CacheModel;
import org.terracotta.modules.ehcache.presentation.model.CacheModelInstance;

import com.tc.admin.common.ApplicationContext;

import java.awt.Frame;
import java.util.Map;

public class ManageStatisticsMessage extends ManageMessage {
  public ManageStatisticsMessage(Frame frame, ApplicationContext appContext, CacheManagerModel cacheManagerModel) {
    this(frame, appContext, cacheManagerModel, Mode.CACHE_MANAGER);
  }

  public ManageStatisticsMessage(Frame frame, ApplicationContext appContext, CacheManagerModel cacheManagerModel,
                                 Mode mode) {
    super(frame, appContext, cacheManagerModel, "Manage Cache Statistics",
          "The selected caches will begin gathering statistics.", mode);
  }

  @Override
  public boolean getValue(CacheModelInstance cacheModelInstance) {
    return cacheModelInstance.isStatisticsEnabled();
  }

  @Override
  public boolean getValue(CacheManagerInstance cacheManagerInstance) {
    int instanceCount = cacheManagerInstance.getInstanceCount();
    return instanceCount > 0 && (cacheManagerInstance.getStatisticsEnabledCount() == instanceCount);
  }

  @Override
  public boolean getValue(CacheManagerModel theCacheManagerModel) {
    return theCacheManagerModel.getStatisticsEnabledCount() == theCacheManagerModel.getCacheModelCount();
  }

  @Override
  public void setValue(CacheManagerModel cacheManagerModel, boolean value, boolean applyToNewcomers) {
    String prefix = value ? "Enabling" : "Disabling";
    final String msg = prefix + " all cache statistics for '" + cacheManagerModel.getName() + "'...";
    cacheManagerModel.setStatisticsEnabledPersistently(value, applyToNewcomers);
    setValueImpl(cacheManagerModel, CacheModelInstance.STATISTICS_ENABLED_PROP, value, msg);
  }

  @Override
  public boolean getValue(CacheModel cacheModel) {
    CacheManagerModel cacheManagerModel = cacheModel.getCacheManagerModel();
    return cacheManagerModel.getStatisticsEnabledCount(cacheModel) == cacheManagerModel
        .getCacheModelInstanceCount(cacheModel);
  }

  @Override
  public void setNodeViewValues(final Map<CacheManagerInstance, Boolean> cacheManagerInstances,
                                final Map<CacheModelInstance, Boolean> cacheModelInstances) {
    final String msg = "Setting cache statistics...";
    setNodeViewValuesImpl(cacheManagerInstances, cacheModelInstances, CacheModelInstance.STATISTICS_ENABLED_PROP, msg);
  }

  @Override
  public void setCacheViewValues(final Map<CacheModel, Boolean> cacheModels,
                                 final Map<CacheModelInstance, Boolean> cacheModelInstances) {
    final String msg = "Setting cache statistics...";
    setCacheViewValuesImpl(cacheModels, cacheModelInstances, CacheModelInstance.STATISTICS_ENABLED_PROP, msg);
  }
}

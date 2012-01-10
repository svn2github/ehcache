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

public class ManageEnablementMessage extends ManageMessage {
  public ManageEnablementMessage(Frame frame, ApplicationContext appContext, CacheManagerModel cacheManagerModel) {
    this(frame, appContext, cacheManagerModel, Mode.CACHE_MANAGER);
  }

  public ManageEnablementMessage(Frame frame, ApplicationContext appContext, CacheManagerModel cacheManagerModel,
                                 Mode mode) {
    super(frame, appContext, cacheManagerModel, "Manage Active Caches", "The selected caches will enabled.", mode);
  }

  @Override
  public boolean getValue(CacheModelInstance cacheModelInstance) {
    return cacheModelInstance.isEnabled();
  }

  @Override
  public boolean getValue(CacheManagerInstance cacheManagerInstance) {
    return cacheManagerInstance.getEnabledCount() == cacheManagerInstance.getInstanceCount();
  }

  @Override
  public boolean getValue(CacheManagerModel theCacheManagerModel) {
    return theCacheManagerModel.getEnabledCount() == theCacheManagerModel.getCacheModelCount();
  }

  @Override
  public void setValue(CacheManagerModel cacheManagerModel, boolean value, boolean applyToNewcomers) {
    String prefix = value ? "Enabling" : "Disabling";
    final String msg = prefix + " all caches contained by '" + cacheManagerModel.getName() + "'...";
    cacheManagerModel.setCachesBulkLoadEnabledPersistently(value, applyToNewcomers);
    setValueImpl(cacheManagerModel, CacheModelInstance.ENABLED_PROP, value, msg);
  }

  @Override
  public boolean getValue(CacheModel cacheModel) {
    CacheManagerModel cacheManagerModel = cacheModel.getCacheManagerModel();
    return cacheManagerModel.getEnabledCount(cacheModel) == cacheManagerModel.getCacheModelInstanceCount(cacheModel);
  }

  @Override
  public void setNodeViewValues(final Map<CacheManagerInstance, Boolean> cacheManagerInstances,
                                final Map<CacheModelInstance, Boolean> cacheModelInstances) {
    final String msg = "Changing cache enablement...";
    setNodeViewValuesImpl(cacheManagerInstances, cacheModelInstances, CacheModelInstance.ENABLED_PROP, msg);
  }

  @Override
  public void setCacheViewValues(final Map<CacheModel, Boolean> cacheModels,
                                 final Map<CacheModelInstance, Boolean> cacheModelInstances) {
    final String msg = "Changing cache enablement...";
    setCacheViewValuesImpl(cacheModels, cacheModelInstances, CacheModelInstance.ENABLED_PROP, msg);
  }
}

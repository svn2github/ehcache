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

public class ManageBulkLoadingMessage extends ManageMessage {
  private static final String OVERVIEW_MSG = "<html>The selected caches will be put into bulk-load mode.<br>"
                                             + " Note: transactional caches cannot be put into bulk-load mode.</html>";

  public ManageBulkLoadingMessage(Frame frame, ApplicationContext appContext, CacheManagerModel cacheManagerModel,
                                  Mode mode) {
    super(frame, appContext, cacheManagerModel, "Manage BulkLoad Mode", OVERVIEW_MSG, mode);
    boolean haveTransactionalCaches = cacheManagerModel.getTransactionalCount() > 0;
    selectAllToggle.setEnabled(!haveTransactionalCaches);
  }

  @Override
  public boolean getValue(CacheModelInstance cacheModelInstance) {
    return cacheModelInstance.isBulkLoadEnabled();
  }

  @Override
  public boolean getValue(CacheManagerInstance cacheManagerInstance) {
    return cacheManagerInstance.getBulkLoadEnabledCount() == cacheManagerInstance.getInstanceCount();
  }

  @Override
  public boolean getValue(CacheManagerModel theCacheManagerModel) {
    return theCacheManagerModel.getBulkLoadEnabledCount() == theCacheManagerModel.getCacheModelCount();
  }

  @Override
  public void setValue(CacheManagerModel cacheManagerModel, boolean value, boolean applyToNewcomers) {
    String enablingMsg = "Putting CacheManager '" + cacheManagerModel.getName() + "' into bulk-load mode...";
    String disablingMsg = "Removing CacheManager '" + cacheManagerModel.getName() + "' from bulk-load mode...";
    final String msg = value ? enablingMsg : disablingMsg;
    cacheManagerModel.setCachesBulkLoadEnabledPersistently(value, applyToNewcomers);
    setValueImpl(cacheManagerModel, CacheModelInstance.BULK_LOAD_ENABLED_PROP, value, msg);
  }

  @Override
  public boolean getValue(CacheModel cacheModel) {
    CacheManagerModel cacheManagerModel = cacheModel.getCacheManagerModel();
    return cacheManagerModel.getBulkLoadEnabledCount(cacheModel) == cacheManagerModel
        .getCacheModelInstanceCount(cacheModel);
  }

  @Override
  public boolean isEnabled(CacheModelInstance cacheModelInstance) {
    return !cacheModelInstance.isTransactional();
  }

  @Override
  public boolean isEnabled(CacheModel cacheModel) {
    return cacheModel.getTransactionalCount() == 0;
  }

  @Override
  public boolean isEnabled(CacheManagerInstance cacheManageInstance) {
    return cacheManageInstance.getTransactionalCount() == 0;
  }

  @Override
  public void setNodeViewValues(final Map<CacheManagerInstance, Boolean> cacheManagerInstances,
                                final Map<CacheModelInstance, Boolean> cacheModelInstances) {
    final String msg = "Updating bulk-load modes...";
    setNodeViewValuesImpl(cacheManagerInstances, cacheModelInstances, CacheModelInstance.BULK_LOAD_ENABLED_PROP, msg);
  }

  @Override
  public void setCacheViewValues(final Map<CacheModel, Boolean> cacheModels,
                                 final Map<CacheModelInstance, Boolean> cacheModelInstances) {
    final String msg = "Updating bulk-load modes...";
    setCacheViewValuesImpl(cacheModels, cacheModelInstances, CacheModelInstance.BULK_LOAD_ENABLED_PROP, msg);
  }
}

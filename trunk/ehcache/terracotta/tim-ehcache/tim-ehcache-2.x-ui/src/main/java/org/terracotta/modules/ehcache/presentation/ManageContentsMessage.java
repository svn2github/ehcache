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
import com.tc.admin.common.XContainer;
import com.tc.util.concurrent.ThreadUtil;

import java.awt.Frame;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ManageContentsMessage extends ManageMessage {
  private static final String OVERVIEW_MSG = "<html>The selected caches will have their contents cleared.<br>"
                                             + " Note: transactional caches cannot be cleared over JMX.</html>";

  public ManageContentsMessage(Frame frame, ApplicationContext appContext, CacheManagerModel cacheManagerModel) {
    this(frame, appContext, cacheManagerModel, Mode.CACHE_MANAGER);
  }

  public ManageContentsMessage(Frame frame, ApplicationContext appContext, CacheManagerModel cacheManagerModel,
                               Mode mode) {
    super(frame, appContext, cacheManagerModel, "Clear Cache Contents", OVERVIEW_MSG, mode);
  }

  @Override
  protected void createApplyToNewcomersToggle(XContainer panel) {/**/
  }

  @Override
  protected void setApplyToNewcomersToggleVisible(boolean visible) {
    /**/
  }

  @Override
  public boolean getValue(CacheModelInstance cacheModelInstance) {
    return false;
  }

  @Override
  public boolean getValue(CacheManagerInstance cacheManagerInstance) {
    return false;
  }

  @Override
  public boolean getValue(CacheManagerModel theCacheManagerModel) {
    return false;
  }

  @Override
  public void setValue(final CacheManagerModel cacheManagerModel, boolean value, boolean applyToNewcomers) {
    if (value) {
      final String msg = "Clearing all cache contents for '" + cacheManagerModel.getName() + "'...";
      showWaitDialog(msg, new Runnable() {
        public void run() {
          long startTime = System.currentTimeMillis();
          Set<CacheModelInstance> cmiSet = cacheManagerModel.allCacheModelInstances();
          if (cmiSet.size() > 0) {
            try {
              Map<CacheModelInstance, Object> result = cacheManagerModel.invokeCacheModelInstances(cmiSet, "removeAll");
              if (result != null && result.size() > 0) {
                Map<CacheModelInstance, Exception> errors = new HashMap<CacheModelInstance, Exception>();
                for (Entry<CacheModelInstance, Object> entry : result.entrySet()) {
                  Object val = entry.getValue();
                  if (val instanceof Exception) {
                    errors.put(entry.getKey(), (Exception) val);
                  }
                }
                if (errors.size() > 0) {
                  reportCacheModelInstanceErrors(errors);
                  return;
                }
              }
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
          long timeTaken = System.currentTimeMillis() - startTime;
          if (timeTaken < MIN_WAIT_DIALOG_TIME) {
            ThreadUtil.reallySleep(MIN_WAIT_DIALOG_TIME - timeTaken);
          }
          hideWaitDialog(msg);
        }
      });
    }
  }

  @Override
  public boolean getValue(CacheModel cacheModel) {
    return false;
  }

  // @Override
  // public boolean isEnabled(CacheModelInstance cacheModelInstance) {
  // return !cacheModelInstance.isTransactional();
  // }
  //
  // @Override
  // public boolean isEnabled(CacheModel cacheModel) {
  // return cacheModel.getTransactionalCount() == 0;
  // }
  //
  // @Override
  // public boolean isEnabled(CacheManagerInstance cacheManageInstance) {
  // return cacheManageInstance.getTransactionalCount() == 0;
  // }

  @Override
  public void setNodeViewValues(final Map<CacheManagerInstance, Boolean> cacheManagerInstances,
                                final Map<CacheModelInstance, Boolean> cacheModelInstances) {
    final String msg = "Clearing cache contents...";
    showWaitDialog(msg, new Runnable() {
      public void run() {
        long startTime = System.currentTimeMillis();
        Set<CacheModelInstance> cmiSet = new HashSet<CacheModelInstance>();
        for (CacheManagerInstance cacheManagerInstance : cacheManagerInstances.keySet()) {
          Object val = cacheManagerInstances.get(cacheManagerInstance);
          if (val.equals(Boolean.TRUE)) {
            cmiSet.addAll(cacheManagerInstance.cacheModelInstances());
          }
        }
        for (CacheModelInstance cacheModelInstance : cacheModelInstances.keySet()) {
          Object val = cacheModelInstances.get(cacheModelInstance);
          if (val.equals(Boolean.TRUE)) {
            cmiSet.add(cacheModelInstance);
          }
        }
        if (cmiSet.size() > 0) {
          try {
            Map<CacheModelInstance, Object> result = getCacheManagerModel().invokeCacheModelInstances(cmiSet,
                                                                                                      "removeAll");
            if (result != null && result.size() > 0) {
              Map<CacheModelInstance, Exception> errors = new HashMap<CacheModelInstance, Exception>();
              for (Entry<CacheModelInstance, Object> entry : result.entrySet()) {
                Object val = entry.getValue();
                if (val instanceof Exception) {
                  errors.put(entry.getKey(), (Exception) val);
                }
              }
              if (errors.size() > 0) {
                reportCacheModelInstanceErrors(errors);
                return;
              }
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
        long timeTaken = System.currentTimeMillis() - startTime;
        if (timeTaken < MIN_WAIT_DIALOG_TIME) {
          ThreadUtil.reallySleep(MIN_WAIT_DIALOG_TIME - timeTaken);
        }
        hideWaitDialog(msg);
      }
    });
  }

  @Override
  public void setCacheViewValues(final Map<CacheModel, Boolean> cacheModels,
                                 final Map<CacheModelInstance, Boolean> cacheModelInstances) {
    final String msg = "Clearing cache contents...";
    showWaitDialog(msg, new Runnable() {
      public void run() {
        long startTime = System.currentTimeMillis();
        CacheManagerModel cacheManagerModel = getCacheManagerModel();
        Set<CacheModelInstance> cmiSet = new HashSet<CacheModelInstance>();
        for (CacheModel cacheModel : cacheModels.keySet()) {
          Object val = cacheModels.get(cacheModel);
          if (val.equals(Boolean.TRUE)) {
            cmiSet.addAll(cacheModel.cacheModelInstances());
          }
        }
        for (CacheModelInstance cacheModelInstance : cacheModelInstances.keySet()) {
          Object val = cacheModelInstances.get(cacheModelInstance);
          if (val.equals(Boolean.TRUE)) {
            cmiSet.add(cacheModelInstance);
          }
        }
        if (cmiSet.size() > 0) {
          try {
            Map<CacheModelInstance, Object> result = cacheManagerModel.invokeCacheModelInstances(cmiSet, "removeAll");
            if (result != null && result.size() > 0) {
              Map<CacheModelInstance, Exception> errors = new HashMap<CacheModelInstance, Exception>();
              for (Entry<CacheModelInstance, Object> entry : result.entrySet()) {
                Object val = entry.getValue();
                if (val instanceof Exception) {
                  errors.put(entry.getKey(), (Exception) val);
                }
              }
              if (errors.size() > 0) {
                reportCacheModelInstanceErrors(errors);
                return;
              }
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
        long timeTaken = System.currentTimeMillis() - startTime;
        if (timeTaken < MIN_WAIT_DIALOG_TIME) {
          ThreadUtil.reallySleep(MIN_WAIT_DIALOG_TIME - timeTaken);
        }
        hideWaitDialog(msg);
      }
    });
  }
}

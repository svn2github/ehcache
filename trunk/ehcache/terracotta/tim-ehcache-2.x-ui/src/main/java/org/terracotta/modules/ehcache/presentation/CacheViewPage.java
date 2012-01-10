package org.terracotta.modules.ehcache.presentation;

import org.terracotta.modules.ehcache.presentation.model.CacheManagerModel;
import org.terracotta.modules.ehcache.presentation.model.CacheModel;
import org.terracotta.modules.ehcache.presentation.model.CacheModelInstance;

import com.tc.admin.common.XRootNode;
import com.tc.admin.common.XTreeNode;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

public class CacheViewPage extends AbstractViewPage {
  private final Map<CacheModel, CacheModelControl>                 cacheModelMap;
  private final Map<CacheModelInstance, CacheModelInstanceControl> cacheModelInstanceMap;
  private final CacheModelControlListener                          cacheModelControlListener;
  private final CacheModelInstanceControlListener                  cacheModelInstanceControlListener;

  public CacheViewPage(final ManageChannel channel, final CacheManagerModel cacheManagerModel) {
    super(channel, cacheManagerModel);

    cacheModelMap = new HashMap<CacheModel, CacheModelControl>();
    cacheModelInstanceMap = new HashMap<CacheModelInstance, CacheModelInstanceControl>();

    cacheModelControlListener = new CacheModelControlListener();
    cacheModelInstanceControlListener = new CacheModelInstanceControlListener();

    initialize();

    cacheManagerModel.addCacheManagerModelListener(this);
    cacheManagerModel.getEhcacheModel().addEhcacheModelListener(this);
  }

  @Override
  public Map<CacheModelInstance, CacheModelInstanceControl> getSelection() {
    return new HashMap(cacheModelInstanceMap);
  }

  @Override
  public void setSelection(Map<CacheModelInstance, CacheModelInstanceControl> selection) {
    if (selection != null) {
      for (Entry<CacheModelInstance, CacheModelInstanceControl> entry : selection.entrySet()) {
        CacheModelInstanceControl cmic = cacheModelInstanceMap.get(entry.getKey());
        if (cmic != null) {
          cmic.setSelected(entry.getValue().isSelected());
        }
      }
    }
  }

  @Override
  public void initialize() {
    XRootNode rootNode = (XRootNode) treeModel.getRoot();
    rootNode.tearDownChildren();
    treeModel.nodeStructureChanged(rootNode);

    cacheModelMap.clear();
    cacheModelInstanceMap.clear();

    int x = 0;
    for (Iterator<CacheModel> cacheModelIter = cacheManagerModel.cacheModelIterator(); cacheModelIter.hasNext();) {
      CacheModel cacheModel = cacheModelIter.next();
      CacheModelControl cmc = new CacheModelControl(cacheModel) {
        @Override
        public void setSelectedAndChildren(boolean selected) {
          setSelected(selected);
          for (CacheModelInstance cacheModelInstance : cacheManagerModel.cacheModelInstances(cacheModel)) {
            cacheModelInstanceMap.get(cacheModelInstance).setSelected(selected);
          }
        }
      };
      XTreeNode parentNode = new XTreeNode(cmc);
      treeModel.insertNodeInto(parentNode, rootNode, x++);
      cmc.addActionListener(cacheModelControlListener);
      cacheModelMap.put(cacheModel, cmc);

      int y = 0;
      for (CacheModelInstance cacheModelInstance : cacheManagerModel.cacheModelInstances(cacheModel)) {
        String clientName = cacheModelInstance.getClientName();
        CacheModelInstanceControl cacheModelInstanceControl = new CacheModelInstanceControl(clientName,
                                                                                            cacheModelInstance);
        XTreeNode xtn = new XTreeNode(cacheModelInstanceControl);
        xtn.setIcon(cacheModelInstanceControl.getIcon());
        treeModel.insertNodeInto(xtn, parentNode, y++);
        cacheModelInstanceControl.addActionListener(cacheModelInstanceControlListener);
        cacheModelInstanceMap.put(cacheModelInstance, cacheModelInstanceControl);
        cacheModelInstance.getCacheManagerInstance().addCacheManagerInstanceListener(this);
      }
    }
    tree.expandAll();

    handleAllSelector();
  }

  @Override
  public void selectAll(boolean select) {
    for (CacheModelControl cacheModelControl : cacheModelMap.values()) {
      cacheModelControl.setSelectedAndChildren(select);
    }
    updateAllNodes();
    handleAllSelector();
  }

  @Override
  public void apply(Boolean applyToNewcomers) {
    boolean cmValue = channel.getValue(cacheManagerModel);
    if ((isAllSelected() && !cmValue) || (isAllDeselected() && cmValue)) {
      channel.setValue(cacheManagerModel, !cmValue, applyToNewcomers != null ? applyToNewcomers.booleanValue() : false);
    } else {
      Map<CacheModel, Boolean> cacheModels = new HashMap<CacheModel, Boolean>();
      Map<CacheModelInstance, Boolean> cacheModelInstances = new HashMap<CacheModelInstance, Boolean>();
      for (CacheModel cacheModel : cacheModelMap.keySet()) {
        CacheModelControl cmc = cacheModelMap.get(cacheModel);
        if (cmc.isSelected() && !channel.getValue(cacheModel)) {
          cacheModels.put(cacheModel, Boolean.TRUE);
        } else {
          for (CacheModelInstance cmi : cacheModelInstanceMap.keySet()) {
            CacheModelInstanceControl cmic = cacheModelInstanceMap.get(cmi);
            if (cmic.isSelected() != channel.getValue(cmi)) {
              cacheModelInstances.put(cmi, Boolean.valueOf(cmic.isSelected()));
            }
          }
        }
        channel.setCacheViewValues(cacheModels, cacheModelInstances);
      }
    }
  }

  @Override
  public void handleAllSelector() {
    boolean allSelected = true;
    for (CacheModel cacheModel : cacheModelMap.keySet()) {
      CacheModelControl cmc = cacheModelMap.get(cacheModel);
      if (!cmc.isSelected()) {
        allSelected = false;
        break;
      }
    }
    if ((isAllSelected = allSelected) == true) {
      isAllDeselected = false;
      isSomeSelected = true;
      firePropertyChange(ALL_SELECTED_PROP, allSelected, !allSelected);
    } else {
      boolean allDeselected = true;
      for (CacheModelInstanceControl cmic : cacheModelInstanceMap.values()) {
        if (cmic.isSelected()) {
          allDeselected = false;
          break;
        }
      }
      if ((isAllDeselected = allDeselected) == true) {
        isSomeSelected = isAllSelected = false;
        firePropertyChange(ALL_DESELECTED_PROP, allDeselected, !allDeselected);
      } else {
        isSomeSelected = true;
        isAllSelected = isAllDeselected = false;
        firePropertyChange(SOME_SELECTED_PROP, true, !true);
      }
    }
  }

  private class CacheModelControlListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      CacheModelControl cmc = (CacheModelControl) e.getSource();
      CacheModel cacheModel = cmc.getCacheModel();
      boolean selected = cmc.isSelected();
      for (CacheModelInstance cmi : cacheModelInstanceMap.keySet()) {
        if (cmi.getCacheName().equals(cacheModel.getCacheName())) {
          CacheModelInstanceControl cmic = cacheModelInstanceMap.get(cmi);
          if (cmic != null && selected != cmic.isSelected()) {
            cmic.setSelectedQuietly(selected);
          }
        }
      }
      updateAllNodes();
      handleAllSelector();
    }
  }

  private class CacheModelInstanceControlListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      CacheModelInstanceControl cmic = (CacheModelInstanceControl) e.getSource();
      boolean selected = cmic.isSelected();
      CacheModelInstance cmi = cmic.getCacheModelInstance();
      CacheModel cacheModel = cacheManagerModel.getCacheModel(cmi.getCacheName());
      CacheModelControl cmc = cacheModelMap.get(cacheModel);
      if (!selected && cmc.isSelected()) {
        cmc.setSelectedQuietly(false);
      } else if (selected && !cmc.isSelected()) {
        boolean allEnabled = true;
        for (CacheModelInstance cacheModelInstance2 : cacheManagerModel.cacheModelInstances(cacheModel)) {
          cmic = cacheModelInstanceMap.get(cacheModelInstance2);
          if (!cmic.isSelected()) {
            allEnabled = false;
            break;
          }
        }
        cmc.setSelectedQuietly(allEnabled);
      }
      updateAllNodes();
      handleAllSelector();
    }
  }

  @Override
  public void tearDown() {
    cacheModelMap.clear();
    cacheModelInstanceMap.clear();

    super.tearDown();
  }
}

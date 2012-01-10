/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation;

import org.terracotta.modules.ehcache.presentation.model.CacheManagerInstance;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerModel;
import org.terracotta.modules.ehcache.presentation.model.CacheModelInstance;

import com.tc.admin.common.XRootNode;
import com.tc.admin.common.XTreeNode;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class NodeViewPage extends AbstractViewPage {
  private final Map<CacheManagerInstance, CacheManagerInstanceControl> cacheManagerInstanceMap;
  private final Map<CacheModelInstance, CacheModelInstanceControl>     cacheModelInstanceMap;

  private final CacheManagerInstanceControlListener                    cacheManagerInstanceControlListener;
  private final CacheModelInstanceControlListener                      cacheModelInstanceControlListener;

  public NodeViewPage(ManageChannel channel, CacheManagerModel cacheManagerModel) {
    super(channel, cacheManagerModel);

    cacheManagerInstanceMap = new HashMap<CacheManagerInstance, CacheManagerInstanceControl>();
    cacheModelInstanceMap = new HashMap<CacheModelInstance, CacheModelInstanceControl>();

    cacheManagerInstanceControlListener = new CacheManagerInstanceControlListener();
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

    cacheManagerInstanceMap.clear();
    cacheModelInstanceMap.clear();

    int x = 0;
    for (Iterator<CacheManagerInstance> cacheManagerInstanceIter = cacheManagerModel.cacheManagerInstanceIterator(); cacheManagerInstanceIter
        .hasNext();) {
      CacheManagerInstance cacheManagerInstance = cacheManagerInstanceIter.next();
      CacheManagerInstanceControl cmic = new CacheManagerInstanceControl(cacheManagerInstance) {
        @Override
        public void setSelectedAndChildren(boolean selected) {
          setSelected(selected);
          for (Iterator<CacheModelInstance> iter = cacheManagerInstance.cacheModelInstanceIter(); iter.hasNext();) {
            cacheModelInstanceMap.get(iter.next()).setSelected(selected);
          }
        }
      };
      XTreeNode parentNode = new XTreeNode(cmic);
      treeModel.insertNodeInto(parentNode, rootNode, x++);
      cmic.addActionListener(cacheManagerInstanceControlListener);
      cacheManagerInstanceMap.put(cacheManagerInstance, cmic);
      cacheManagerInstance.addCacheManagerInstanceListener(this);

      int y = 0;
      for (Iterator<CacheModelInstance> cacheModelInstanceIter = cacheManagerInstance.cacheModelInstanceIter(); cacheModelInstanceIter
          .hasNext();) {
        CacheModelInstance cacheModelInstance = cacheModelInstanceIter.next();
        CacheModelInstanceControl cacheModelInstanceControl = new CacheModelInstanceControl(cacheModelInstance);
        XTreeNode xtn = new XTreeNode(cacheModelInstanceControl);
        xtn.setIcon(cacheModelInstanceControl.getIcon());
        treeModel.insertNodeInto(xtn, parentNode, y++);
        cacheModelInstanceControl.addActionListener(cacheModelInstanceControlListener);
        cacheModelInstanceMap.put(cacheModelInstance, cacheModelInstanceControl);
      }
    }
    tree.expandAll();

    handleAllSelector();
  }

  @Override
  public void selectAll(boolean select) {
    for (CacheManagerInstanceControl cacheManagerInstanceControl : cacheManagerInstanceMap.values()) {
      cacheManagerInstanceControl.setSelectedAndChildren(select);
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
      Map<CacheManagerInstance, Boolean> cacheManagerInstances = new HashMap<CacheManagerInstance, Boolean>();
      Map<CacheModelInstance, Boolean> cacheModelInstances = new HashMap<CacheModelInstance, Boolean>();
      for (CacheManagerInstance cacheManagerInstance : cacheManagerInstanceMap.keySet()) {
        CacheManagerInstanceControl cacheManagerInstanceControl = cacheManagerInstanceMap.get(cacheManagerInstance);
        if (cacheManagerInstanceControl.isSelected() && !channel.getValue(cacheManagerInstance)) {
          cacheManagerInstances.put(cacheManagerInstance, Boolean.TRUE);
        } else {
          for (CacheModelInstance cacheModelInstance : cacheModelInstanceMap.keySet()) {
            CacheModelInstanceControl cacheModelInstanceControl = cacheModelInstanceMap.get(cacheModelInstance);
            if (cacheModelInstanceControl.isSelected() != channel.getValue(cacheModelInstance)) {
              cacheModelInstances.put(cacheModelInstance, Boolean.valueOf(cacheModelInstanceControl.isSelected()));
            }
          }
        }
        channel.setNodeViewValues(cacheManagerInstances, cacheModelInstances);
      }
    }
  }

  @Override
  public void handleAllSelector() {
    boolean allSelected = true;
    for (CacheManagerInstance cacheManagerInstance : cacheManagerInstanceMap.keySet()) {
      CacheManagerInstanceControl cacheManagerInstanceControl = cacheManagerInstanceMap.get(cacheManagerInstance);
      if (!cacheManagerInstanceControl.isSelected()) {
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
      for (CacheModelInstanceControl cacheModelInstanceControl : cacheModelInstanceMap.values()) {
        if (cacheModelInstanceControl.isSelected()) {
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

  private class CacheManagerInstanceControlListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      CacheManagerInstanceControl cmic = (CacheManagerInstanceControl) e.getSource();
      CacheManagerInstance cacheManagerInstance = cmic.getCacheManagerInstance();
      boolean selected = cmic.isSelected();
      for (Iterator<CacheModelInstance> iter = cacheManagerInstance.cacheModelInstanceIter(); iter.hasNext();) {
        CacheModelInstanceControl cacheModelInstanceControl = cacheModelInstanceMap.get(iter.next());
        if (cacheModelInstanceControl != null && selected != cacheModelInstanceControl.isSelected()) {
          cacheModelInstanceControl.setSelectedQuietly(selected);
        }
      }
      updateAllNodes();
      handleAllSelector();
    }
  }

  private class CacheModelInstanceControlListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      CacheModelInstanceControl cacheModelInstanceControl = (CacheModelInstanceControl) e.getSource();
      boolean selected = cacheModelInstanceControl.isSelected();
      CacheModelInstance cacheModelInstance = cacheModelInstanceControl.getCacheModelInstance();
      CacheManagerInstance cacheManagerInstance = cacheModelInstance.getCacheManagerInstance();
      CacheManagerInstanceControl parentControl = cacheManagerInstanceMap.get(cacheManagerInstance);
      if (!selected && parentControl.isSelected()) {
        parentControl.setSelectedQuietly(false);
      } else if (selected && !parentControl.isSelected()) {
        boolean allEnabled = true;
        for (Iterator<CacheModelInstance> iter = cacheManagerInstance.cacheModelInstanceIter(); iter.hasNext();) {
          if (!cacheModelInstanceMap.get(iter.next()).isSelected()) {
            allEnabled = false;
            break;
          }
        }
        parentControl.setSelectedQuietly(allEnabled);
      }
      updateAllNodes();
      handleAllSelector();
    }
  }

  @Override
  public void tearDown() {
    cacheManagerInstanceMap.clear();
    cacheModelInstanceMap.clear();

    super.tearDown();
  }
}

package org.terracotta.modules.ehcache.presentation;

import static org.terracotta.modules.ehcache.presentation.EhcachePresentationUtils.CLUSTERED_ICON;
import static org.terracotta.modules.ehcache.presentation.EhcachePresentationUtils.NON_CLUSTERED_ICON;

import org.terracotta.modules.ehcache.presentation.model.CacheManagerInstance;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerInstanceListener;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerModel;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerModelListener;
import org.terracotta.modules.ehcache.presentation.model.CacheModel;
import org.terracotta.modules.ehcache.presentation.model.CacheModelInstance;
import org.terracotta.modules.ehcache.presentation.model.ClusteredCacheModel;
import org.terracotta.modules.ehcache.presentation.model.EhcacheModelListener;
import org.terracotta.modules.ehcache.presentation.model.StandaloneCacheModel;

import com.tc.admin.common.XContainer;
import com.tc.admin.common.XRootNode;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XTree;
import com.tc.admin.common.XTreeModel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.ResourceBundle;

import javax.swing.Icon;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public abstract class AbstractViewPage extends XContainer implements CacheManagerModelListener,
    CacheManagerInstanceListener, EhcacheModelListener, Runnable {
  protected ManageChannel               channel;
  protected CacheManagerModel           cacheManagerModel;
  protected SelectionModelTreeModel     treeModel;
  protected XTree                       tree;
  protected boolean                     isAllSelected;
  protected boolean                     isAllDeselected;
  protected boolean                     isSomeSelected;

  protected static final int            CONTROL_OFFSET      = 20;

  protected static final Insets         CONTROL_MARGIN      = new Insets(1, 1, 1, 1);

  public static final String            ALL_SELECTED_PROP   = "ALL_SELECTED";
  public static final String            ALL_DESELECTED_PROP = "ALL_DESELECTED";
  public static final String            SOME_SELECTED_PROP  = "SOME_SELECTED";

  protected static final ResourceBundle bundle              = ResourceBundle.getBundle(EhcacheResourceBundle.class
                                                                .getName());

  protected AbstractViewPage(ManageChannel channel, CacheManagerModel cacheManagerModel) {
    super(new BorderLayout());
    this.channel = channel;
    this.cacheManagerModel = cacheManagerModel;
    treeModel = new SelectionModelTreeModel();
    tree = new XTree(treeModel);
    XScrollPane scroller = new XScrollPane(tree);
    scroller.setPreferredSize(new Dimension(325, 275));
    add(scroller);
    // tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    tree.setSelectionModel(null);
    tree.setCellRenderer(new CheckBoxNodeRenderer());
    tree.setCellEditor(new CheckBoxNodeEditor(tree));
    tree.setEditable(true);

    tree.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        int selRow = tree.getRowForLocation(e.getX(), e.getY());
        TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
        if (selRow != -1) {
          if (e.getClickCount() == 1) {
            tree.startEditingAtPath(selPath);
            DefaultMutableTreeNode aNode = (DefaultMutableTreeNode) selPath.getLastPathComponent();
            Object userObject = aNode.getUserObject();
            if (userObject instanceof SelectionModel) {
              SelectionModel selectionModel = (SelectionModel) userObject;
              treeModel.valueForPathChanged(selPath, Boolean.valueOf(!selectionModel.isSelected()));
              tree.stopEditing();
            }
          }
        }
      }
    });
  }

  protected static class SelectionModelTreeModel extends XTreeModel {
    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
      DefaultMutableTreeNode aNode = (DefaultMutableTreeNode) path.getLastPathComponent();
      Object userObject = aNode.getUserObject();
      if (userObject instanceof SelectionModel) {
        ((SelectionModel) userObject).setSelected(((Boolean) newValue).booleanValue());
      } else {
        aNode.setUserObject(newValue);
      }
      nodeChanged(aNode);
    }
  }

  public abstract void initialize();

  public abstract void selectAll(boolean select);

  public abstract void apply(Boolean applyToNewcomers);

  public abstract void handleAllSelector();

  public abstract Map<CacheModelInstance, CacheModelInstanceControl> getSelection();

  public abstract void setSelection(Map<CacheModelInstance, CacheModelInstanceControl> selection);

  public boolean isAllSelected() {
    return isAllSelected;
  }

  public boolean isAllDeselected() {
    return isAllDeselected;
  }

  public boolean isSomeSelected() {
    return isSomeSelected;
  }

  protected void updateAllNodes() {
    treeModel.nodesChanged((TreeNode) treeModel.getRoot(), null);
  }

  private abstract class BaseSelectionModel implements SelectionModel {
    private String                  text;
    private Icon                    icon;
    private boolean                 selected;
    private boolean                 enabled;
    private final EventListenerList eventListenerList;

    BaseSelectionModel(String text, boolean selected, boolean enabled) {
      this.text = text;
      this.selected = selected;
      this.enabled = enabled;
      this.eventListenerList = new EventListenerList();
    }

    BaseSelectionModel(String text, boolean selected) {
      this(text, selected, true);
    }

    public synchronized void setText(String text) {
      this.text = text;
    }

    public synchronized String getText() {
      return text;
    }

    public synchronized void setIcon(Icon icon) {
      this.icon = icon;
    }

    public synchronized Icon getIcon() {
      return icon;
    }

    public void setSelected(boolean selected) {
      if (isEnabled()) {
        setSelectedQuietly(selected);
        fireActionPerformed();
      }
    }

    public void setSelectedQuietly(boolean selected) {
      if (isEnabled()) {
        synchronized (this) {
          this.selected = selected;
        }
      }
    }

    public synchronized boolean isSelected() {
      return selected;
    }

    public synchronized void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public synchronized boolean isEnabled() {
      return enabled;
    }

    public void addActionListener(ActionListener l) {
      eventListenerList.add(ActionListener.class, l);
    }

    public void removeActionListener(ActionListener l) {
      eventListenerList.remove(ActionListener.class, l);
    }

    protected void fireActionPerformed() {
      Object[] listeners = eventListenerList.getListenerList();
      ActionEvent e = null;
      for (int i = listeners.length - 2; i >= 0; i -= 2) {
        if (listeners[i] == ActionListener.class) {
          if (e == null) {
            e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, getText(), System.currentTimeMillis(), 0);
          }
          ((ActionListener) listeners[i + 1]).actionPerformed(e);
        }
      }
    }
  }

  protected abstract class CacheManagerInstanceControl extends BaseSelectionModel {
    protected final CacheManagerInstance cacheManagerInstance;

    public CacheManagerInstanceControl(CacheManagerInstance cacheManagerInstance) {
      super(cacheManagerInstance.getClientName(), channel.getValue(cacheManagerInstance));
      setEnabled(channel.isEnabled(cacheManagerInstance));
      this.cacheManagerInstance = cacheManagerInstance;
    }

    public CacheManagerInstance getCacheManagerInstance() {
      return cacheManagerInstance;
    }

    public abstract void setSelectedAndChildren(boolean selected);
  }

  protected class CacheModelInstanceControl extends BaseSelectionModel {
    protected final CacheModelInstance cacheModelInstance;

    public CacheModelInstanceControl(CacheModelInstance cacheModelInstance) {
      super(cacheModelInstance.getCacheName(), channel.getValue(cacheModelInstance));
      setIcon(cacheModelInstance.isTerracottaClustered() ? CLUSTERED_ICON : NON_CLUSTERED_ICON);
      setEnabled(channel.isEnabled(cacheModelInstance));
      this.cacheModelInstance = cacheModelInstance;
    }

    public CacheModelInstanceControl(String moniker, CacheModelInstance cacheModelInstance) {
      this(cacheModelInstance);
      setText(moniker);
    }

    public CacheModelInstance getCacheModelInstance() {
      return cacheModelInstance;
    }
  }

  protected abstract class CacheModelControl extends BaseSelectionModel {
    protected final CacheModel cacheModel;

    public CacheModelControl(CacheModel cacheModel) {
      super(cacheModel.getCacheName(), channel.getValue(cacheModel));
      setEnabled(channel.isEnabled(cacheModel));
      this.cacheModel = cacheModel;
    }

    public CacheModel getCacheModel() {
      return cacheModel;
    }

    public abstract void setSelectedAndChildren(boolean selected);
  }

  public void cacheManagerModelAdded(CacheManagerModel theCacheManagerModel) {
    /**/
  }

  public void cacheManagerModelRemoved(CacheManagerModel theCacheManagerModel) {
    if (theCacheManagerModel == cacheManagerModel) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          setVisible(false);
        }
      });
    }
  }

  public void instanceAdded(CacheManagerInstance instance) {
    instance.addCacheManagerInstanceListener(this);
    SwingUtilities.invokeLater(this);
  }

  public void instanceRemoved(CacheManagerInstance instance) {
    instance.removeCacheManagerInstanceListener(this);
    SwingUtilities.invokeLater(this);
  }

  public void cacheModelAdded(CacheModel cacheModel) {
    SwingUtilities.invokeLater(this);
  }

  public void cacheModelRemoved(CacheModel cacheModel) {
    SwingUtilities.invokeLater(this);
  }

  public void cacheModelChanged(CacheModel cacheModel) {/**/
  }

  public void cacheManagerInstanceChanged(CacheManagerInstance cacheManagerInstance) {
    /**/
  }

  public void cacheModelInstanceAdded(CacheModelInstance cacheModelInstance) {
    SwingUtilities.invokeLater(this);
  }

  public void cacheModelInstanceRemoved(CacheModelInstance cacheModelInstance) {
    SwingUtilities.invokeLater(this);
  }

  public void cacheModelInstanceChanged(CacheModelInstance cacheModelInstance) {/**/
  }

  public void clusteredCacheModelAdded(ClusteredCacheModel cacheModel) {
    /**/
  }

  public void clusteredCacheModelRemoved(ClusteredCacheModel cacheModel) {
    /**/
  }

  public void clusteredCacheModelChanged(ClusteredCacheModel cacheModel) {
    /**/
  }

  public void standaloneCacheModelAdded(StandaloneCacheModel cacheModel) {
    /**/
  }

  public void standaloneCacheModelRemoved(StandaloneCacheModel cacheModel) {
    /**/
  }

  public void standaloneCacheModelChanged(StandaloneCacheModel cacheModel) {
    /**/
  }

  public void run() {
    initialize();
  }

  @Override
  public void tearDown() {
    cacheManagerModel.removeCacheManagerModelListener(this);
    cacheManagerModel.getEhcacheModel().removeEhcacheModelListener(this);

    ((XRootNode) treeModel.getRoot()).tearDownChildren();

    super.tearDown();
  }
}

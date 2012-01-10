/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation;

import org.terracotta.modules.ehcache.presentation.model.CacheManagerModel;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.XCheckBox;
import com.tc.admin.common.XTable;
import com.tc.admin.common.XTable.BaseRenderer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.Callable;

import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public abstract class TopologyPanelPage extends BaseClusterModelPanel {
  public TopologyPanelPage(ApplicationContext appContext, CacheManagerModel cacheManagerModel) {
    super(appContext, cacheManagerModel.getClusterModel());
  }

  protected abstract BasicWorker<Void> createClearCachesWorker(JPopupMenu popupMenu);

  protected abstract BasicWorker<Void> createEnableCachesWorker(JPopupMenu popupMenu, boolean enable, boolean flush);

  protected abstract BasicWorker<Void> createStatisticsControlWorker(JPopupMenu popupMenu, boolean enable);

  protected abstract BasicWorker<Void> createBulkLoadControlWorker(JPopupMenu popupMenu, boolean bulkLoadEnabled);

  protected abstract Callable<String> createConfigurationGenerator(JPopupMenu popupMenu);

  public abstract void updateActions(XTable table);

  protected class PopupMenuAdapter implements PopupMenuListener {
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
      /**/
    }

    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
      /**/
    }

    public void popupMenuCanceled(PopupMenuEvent e) {
      /**/
    }
  }

  protected static class ModeRenderer extends BaseRenderer {
    @Override
    public void setValue(Object value) {
      if (!(value instanceof Boolean)) {
        setText(value != null ? value.toString() : "na");
        return;
      }
      boolean isBulkloading = ((Boolean) value).booleanValue();
      label.setText(isBulkloading ? "Bulkloading" : "Normal");
    }
  }

  protected static class SelectAllToggleListener implements ActionListener {
    private final XTable table;

    protected SelectAllToggleListener(XTable table) {
      this.table = table;
    }

    public void actionPerformed(ActionEvent e) {
      XCheckBox toggle = (XCheckBox) e.getSource();
      if (toggle.isSelected()) {
        table.selectAll();
      } else {
        table.clearSelection();
      }
    }
  }
}

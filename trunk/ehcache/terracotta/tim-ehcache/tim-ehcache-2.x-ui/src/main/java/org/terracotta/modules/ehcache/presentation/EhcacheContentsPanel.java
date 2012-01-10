/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.presentation;

import org.terracotta.modules.ehcache.presentation.model.CacheManagerInstance;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerModel;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerModelListener;
import org.terracotta.modules.ehcache.presentation.model.CacheModel;
import org.terracotta.modules.ehcache.presentation.model.CacheModelInstance;
import org.terracotta.modules.ehcache.presentation.model.ClusteredCacheModel;
import org.terracotta.modules.ehcache.presentation.model.StandaloneCacheModel;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.ExceptionHelper;
import com.tc.admin.common.XButton;
import com.tc.admin.common.XCheckBox;
import com.tc.admin.common.XComboBox;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XTable;
import com.tc.admin.common.XTextField;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Callable;

import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

public class EhcacheContentsPanel extends BaseClusterModelPanel implements CacheManagerModelListener, ClipboardOwner {
  private final CacheManagerModel cacheManagerModel;

  private XComboBox               clusteredCacheSelector;
  private XButton                 searchButton;
  private XTextField              searchField;
  private XLabel                  contentsHeader;
  private XContainer              resultsPanel;
  private XTable                  resultsTable;
  private XContainer              topPanel;
  private XContainer              clusteredPanel;
  private XContainer              standalonePanel;
  private XComboBox               cacheManagerInstanceSelector;
  private XComboBox               standaloneCacheSelector;
  private XCheckBox               clusteredCacheToggle;
  private XCheckBox               standaloneCacheToggle;
  private Object                  clusteredConstraint;
  private Object                  standaloneConstraint;

  private static final TableModel EMPTY_TABLE_MODEL = new DefaultTableModel();

  private final static Icon       WAIT_ICON         = new ImageIcon(
                                                                    EhcacheContentsPanel.class
                                                                        .getResource("/com/tc/admin/icons/wait.gif"));
  private final static Icon       EMPTY_WAIT_ICON   = new ImageIcon(
                                                                    EhcacheContentsPanel.class
                                                                        .getResource("/com/tc/admin/icons/transparent16x16.png"));

  public EhcacheContentsPanel(ApplicationContext appContext, CacheManagerModel cacheManagerModel) {
    super(appContext, cacheManagerModel.getClusterModel());
    this.cacheManagerModel = cacheManagerModel;
  }

  @Override
  protected void init() {
    DefaultComboBoxModel comboModel = new DefaultComboBoxModel();
    for (ClusteredCacheModel cacheModel : cacheManagerModel.clusteredCacheModels()) {
      comboModel.addElement(cacheModel);
    }
    clusteredCacheSelector.setModel(comboModel);
    updateCacheManagerSelector();
    evaluateVisibility();
    cacheManagerModel.addCacheManagerModelListener(this);
  }

  private void updateCacheManagerSelector() {
    int selIndex = cacheManagerInstanceSelector.getSelectedIndex();
    Map<CacheModelInstance, StandaloneCacheModel> standaloneCacheModels = cacheManagerModel.standaloneCacheModels();
    Set<CacheManagerInstance> cmis = new HashSet<CacheManagerInstance>();
    for (CacheModelInstance cacheModelInstance : standaloneCacheModels.keySet()) {
      cmis.add(cacheModelInstance.getCacheManagerInstance());
    }
    cacheManagerInstanceSelector.setModel(new DefaultComboBoxModel(cmis.toArray(new CacheManagerInstance[0])));
    if (selIndex != -1 && selIndex < cacheManagerInstanceSelector.getItemCount()) {
      cacheManagerInstanceSelector.setSelectedIndex(selIndex);
    }
  }

  private void evaluateVisibility() {
    Set<ClusteredCacheModel> clusteredCacheModels = cacheManagerModel.clusteredCacheModels();
    boolean haveClustered = clusteredCacheModels.size() > 0;
    if (haveClustered && clusteredPanel.getParent() == null) {
      topPanel.add(clusteredPanel, clusteredConstraint);
    } else if (!haveClustered && clusteredPanel.getParent() != null) {
      topPanel.remove(clusteredPanel);
    }

    Map<CacheModelInstance, StandaloneCacheModel> standaloneCacheModels = cacheManagerModel.standaloneCacheModels();
    boolean haveStandalone = standaloneCacheModels.size() > 0;
    if (haveStandalone && standalonePanel.getParent() == null) {
      topPanel.add(standalonePanel, standaloneConstraint);
      cacheManagerInstanceSelector.setSelectedIndex(0);
    } else if (!haveStandalone && standalonePanel.getParent() != null) {
      topPanel.remove(standalonePanel);
    }

    if (haveClustered) {
      setClusteredCachesEnabled(true);
    } else if (haveStandalone) {
      setStandaloneCachesEnabled(true);
    }

    topPanel.revalidate();
    topPanel.repaint();
  }

  private void setClusteredCachesEnabled(boolean enabled) {
    clusteredCacheToggle.setSelected(enabled);
    clusteredCacheSelector.setEnabled(enabled);

    standaloneCacheToggle.setSelected(!enabled);
    cacheManagerInstanceSelector.setEnabled(!enabled);
    standaloneCacheSelector.setEnabled(!enabled);
  }

  private void setStandaloneCachesEnabled(boolean enabled) {
    standaloneCacheToggle.setSelected(enabled);
    cacheManagerInstanceSelector.setEnabled(enabled);
    standaloneCacheSelector.setEnabled(enabled);

    clusteredCacheToggle.setSelected(!enabled);
    clusteredCacheSelector.setEnabled(!enabled);
  }

  @Override
  public XContainer createMainPanel() {
    XContainer panel = new XContainer(new BorderLayout());
    panel.add(topPanel = createTopPanel(), BorderLayout.NORTH);
    panel.add(createCenterPanel());
    return panel;
  }

  protected XContainer createClusteredPanel() {
    XContainer result = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.gridx = gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;

    result.add(clusteredCacheToggle = new XCheckBox("Clustered Caches:"), gbc);
    clusteredCacheToggle.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        boolean selected = clusteredCacheToggle.isSelected();
        clusteredCacheSelector.setEnabled(selected);

        standaloneCacheToggle.setSelected(!selected);
        cacheManagerInstanceSelector.setEnabled(!selected);
        standaloneCacheSelector.setEnabled(!selected);
      }
    });
    gbc.gridx++;
    result.add(clusteredCacheSelector = new XComboBox(), gbc);

    return result;
  }

  protected XContainer createStandalonePanel() {
    XContainer result = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.gridx = gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;

    result.add(standaloneCacheToggle = new XCheckBox("Standalone Caches:"), gbc);
    standaloneCacheToggle.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        boolean selected = standaloneCacheToggle.isSelected();
        cacheManagerInstanceSelector.setEnabled(selected);
        standaloneCacheSelector.setEnabled(selected);

        clusteredCacheToggle.setSelected(!selected);
        clusteredCacheSelector.setEnabled(!selected);
      }
    });
    gbc.gridx++;
    result.add(cacheManagerInstanceSelector = new XComboBox(), gbc);
    cacheManagerInstanceSelector.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Set<StandaloneCacheModel> set = new HashSet<StandaloneCacheModel>();
        CacheManagerInstance cacheManagerInstance = (CacheManagerInstance) cacheManagerInstanceSelector
            .getSelectedItem();
        for (CacheModelInstance cacheModelInstance : cacheManagerInstance.cacheModelInstances()) {
          StandaloneCacheModel cacheModel = cacheManagerModel.standaloneCacheModel(cacheModelInstance);
          if (cacheModel != null) {
            set.add(cacheModel);
          }
        }
        DefaultComboBoxModel comboModel = new DefaultComboBoxModel(set.toArray(new StandaloneCacheModel[0]));
        standaloneCacheSelector.setModel(comboModel);
      }
    });
    gbc.gridx++;
    result.add(standaloneCacheSelector = new XComboBox(), gbc);
    standaloneCacheSelector.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        /**/
      }
    });
    gbc.gridx++;
    // filler
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    result.add(new XLabel(), gbc);

    return result;
  }

  protected XContainer createTopPanel() {
    XContainer result = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.gridx = gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.WEST;

    clusteredConstraint = gbc.clone();
    gbc.gridx++;
    standaloneConstraint = gbc.clone();

    clusteredPanel = createClusteredPanel();
    standalonePanel = createStandalonePanel();

    return result;
  }

  protected XContainer createCenterPanel() {
    XContainer top = new XContainer(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.anchor = GridBagConstraints.WEST;

    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    top.add(searchButton = new XButton(bundle.getString("search")), gbc);
    searchButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        findElements();
      }
    });
    gbc.gridx++;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    top.add(searchField = new XTextField(), gbc);
    searchField.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        findElements();
      }
    });

    resultsPanel = new XContainer(new BorderLayout());
    resultsTable = new XTable();
    resultsPanel.add(new XScrollPane(resultsTable));
    trySetAutoCreateRowSorter(resultsTable);
    XContainer contentsPanel = new XContainer(new BorderLayout());
    XContainer headerPanel = new XContainer(new GridBagLayout());
    gbc.gridx = gbc.gridy = 0;
    gbc.insets = new Insets(1, 1, 1, 1);
    headerPanel.add(contentsHeader = new XLabel(bundle.getString("no.elements"), EMPTY_WAIT_ICON), gbc);
    gbc.gridx++;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    headerPanel.add(new XLabel(), gbc);
    contentsPanel.add(headerPanel, BorderLayout.NORTH);
    contentsPanel.add(new XScrollPane(resultsPanel));

    XContainer result = new XContainer(new BorderLayout());
    result.add(top, BorderLayout.NORTH);
    result.add(contentsPanel, BorderLayout.CENTER);

    return result;
  }

  private static void trySetAutoCreateRowSorter(JTable table) {
    Method m;
    try {
      m = table.getClass().getMethod("setAutoCreateRowSorter", new Class[] { Boolean.TYPE });
      m.invoke(table, Boolean.TRUE);
    } catch (SecurityException e) {
      /**/
    } catch (NoSuchMethodException e) {
      /**/
    } catch (IllegalArgumentException e) {
      /**/
    } catch (IllegalAccessException e) {
      /**/
    } catch (InvocationTargetException e) {
      /**/
    }
  }

  private void findElements() {
    String query = searchField.getText().trim();
    if (query.length() > 0) {
      appContext.submit(new ContentsRetrievalWorker(query));
    }
  }

  private static class ContentsRetrievalResult {
    TableModel data;
    long       totalElements;
    long       elapsedMillis;
  }

  private class ContentsRetrievalWorker extends BasicWorker<ContentsRetrievalResult> {
    private ContentsRetrievalWorker(final String query) {
      super(new Callable<ContentsRetrievalResult>() {
        public ContentsRetrievalResult call() throws Exception {
          ContentsRetrievalResult result = null;
          long startTime = System.currentTimeMillis();
          if (clusteredCacheToggle.isSelected()) {
            CacheModel selectedCacheModel = (CacheModel) clusteredCacheSelector.getSelectedItem();
            String cacheName = selectedCacheModel.getCacheName();
            result = new ContentsRetrievalResult();
            result.data = cacheManagerModel.executeQuery(cacheName, query);
            result.totalElements = selectedCacheModel.getSize();
          } else {
            StandaloneCacheModel cacheModel = (StandaloneCacheModel) standaloneCacheSelector.getSelectedItem();
            result = new ContentsRetrievalResult();
            result.data = cacheModel.executeQuery(query);
            result.totalElements = cacheModel.getSize();
          }
          result.elapsedMillis = System.currentTimeMillis() - startTime;
          return result;
        }
      });
      clearResults();
      searchButton.setEnabled(false);
      searchField.setEnabled(false);
      contentsHeader.setText("Retrieving...");
      contentsHeader.setIcon(WAIT_ICON);
    }

    @Override
    protected void finished() {
      Exception e = getException();
      if (e != null) {
        Throwable cause = ExceptionHelper.getRootCause(e);
        contentsHeader.setText(cause.toString());
        contentsHeader.setIcon(EMPTY_WAIT_ICON);
        showError("Retrieving search results", cause);
        appContext.log(cause);
      } else {
        ContentsRetrievalResult result = getResult();
        resultsTable.setModel(convertTableModel(result.data));
        updateHeader(result);
      }
      searchButton.setEnabled(true);
      searchField.setEnabled(true);
    }
  }

  private static TableModel convertTableModel(TableModel tableModel) {
    if (tableModel.getRowCount() > 0) {
      Class[] columnTypes = new Class[tableModel.getColumnCount()];
      for (int i = 0; i < columnTypes.length; i++) {
        Object value = tableModel.getValueAt(0, i);
        columnTypes[i] = value.getClass();
      }
      return new WrappingTableModel(tableModel, columnTypes);
    }
    return tableModel;
  }

  private static class WrappingTableModel extends DefaultTableModel {
    private final Class[] columnClasses;

    WrappingTableModel(TableModel tableModel, Class[] columnClasses) {
      this.columnClasses = columnClasses;
      DefaultTableModel dtm = (DefaultTableModel) tableModel;
      Vector columnIds = new Vector();
      for (int i = 0; i < columnClasses.length; i++) {
        columnIds.add(tableModel.getColumnName(i));
      }
      setDataVector((Vector) dtm.getDataVector().clone(), columnIds);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      return columnClasses[columnIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return false;
    }
  }

  private void updateHeader(ContentsRetrievalResult result) {
    String text;
    int returnedRows = result.data.getRowCount();
    if (returnedRows <= 0) {
      text = bundle.getString("no.elements");
    } else if (returnedRows == result.totalElements) {
      text = MessageFormat.format(bundle.getString("retrieved.all.elements"), result.totalElements,
                                  result.elapsedMillis);
    } else {
      String elements = bundle.getString(returnedRows == 1 ? "element" : "elements");
      text = MessageFormat.format(bundle.getString("retrieved.some.elements"), returnedRows, elements,
                                  result.totalElements, result.elapsedMillis);
    }
    contentsHeader.setText(text);
    contentsHeader.setIcon(EMPTY_WAIT_ICON);
  }

  private void clearResults() {
    resultsPanel.setBorder(null);
    resultsTable.setModel(EMPTY_TABLE_MODEL);
  }

  public void lostOwnership(Clipboard clipboard, Transferable contents) {
    /**/
  }

  public void cacheModelAdded(final CacheModel cacheModel) {
    /**/
  }

  public void cacheModelRemoved(final CacheModel cacheModel) {
    /**/
  }

  public void cacheModelChanged(final CacheModel cacheModel) {
    /**/
  }

  private void updateClusteredCacheSelector() {
    int index = clusteredCacheSelector.getSelectedIndex();
    List<ClusteredCacheModel> list = new ArrayList<ClusteredCacheModel>(cacheManagerModel.clusteredCacheModels());
    ClusteredCacheModel[] a = list.toArray(new ClusteredCacheModel[0]);
    Arrays.sort(a);
    clusteredCacheSelector.setModel(new DefaultComboBoxModel(a));
    if (index != -1 && index < clusteredCacheSelector.getItemCount()) {
      clusteredCacheSelector.setSelectedIndex(index);
    }
  }

  public void clusteredCacheModelAdded(ClusteredCacheModel cacheModel) {
    updateClusteredCacheSelector();
    evaluateVisibility();
  }

  public void clusteredCacheModelRemoved(ClusteredCacheModel cacheModel) {
    updateClusteredCacheSelector();
    evaluateVisibility();
  }

  public void clusteredCacheModelChanged(ClusteredCacheModel cacheModel) {
    /**/
  }

  public void standaloneCacheModelAdded(StandaloneCacheModel cacheModel) {
    updateCacheManagerSelector();
    evaluateVisibility();
  }

  public void standaloneCacheModelRemoved(StandaloneCacheModel cacheModel) {
    updateCacheManagerSelector();
    evaluateVisibility();
  }

  public void standaloneCacheModelChanged(StandaloneCacheModel cacheModel) {
    /**/
  }

  public void instanceAdded(CacheManagerInstance instance) {
    /**/
  }

  public void instanceRemoved(CacheManagerInstance instance) {
    /**/
  }

  @Override
  public void tearDown() {
    cacheManagerModel.removeCacheManagerModelListener(this);
    super.tearDown();
  }
}

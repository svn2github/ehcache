/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.ehcache.presentation;

import static org.terracotta.modules.ehcache.presentation.EhcachePresentationUtils.CLEAR_STATS_ICON;
import static org.terracotta.modules.ehcache.presentation.model.CacheStatisticsModel.AVERAGE_GET_TIME_MILLIS;
import static org.terracotta.modules.ehcache.presentation.model.CacheStatisticsModel.CACHE_HIT_COUNT;
import static org.terracotta.modules.ehcache.presentation.model.CacheStatisticsModel.CACHE_HIT_RATIO;
import static org.terracotta.modules.ehcache.presentation.model.CacheStatisticsModel.CACHE_MISS_COUNT;
import static org.terracotta.modules.ehcache.presentation.model.CacheStatisticsModel.CACHE_MISS_COUNT_EXPIRED;
import static org.terracotta.modules.ehcache.presentation.model.CacheStatisticsModel.CACHE_NAME;
import static org.terracotta.modules.ehcache.presentation.model.CacheStatisticsModel.EVICTED_COUNT;
import static org.terracotta.modules.ehcache.presentation.model.CacheStatisticsModel.EXPIRED_COUNT;
import static org.terracotta.modules.ehcache.presentation.model.CacheStatisticsModel.IN_MEMORY_HIT_COUNT;
import static org.terracotta.modules.ehcache.presentation.model.CacheStatisticsModel.IN_MEMORY_MISS_COUNT;
import static org.terracotta.modules.ehcache.presentation.model.CacheStatisticsModel.LOCAL_DISK_SIZE;
import static org.terracotta.modules.ehcache.presentation.model.CacheStatisticsModel.LOCAL_DISK_SIZE_IN_BYTES;
import static org.terracotta.modules.ehcache.presentation.model.CacheStatisticsModel.LOCAL_HEAP_SIZE;
import static org.terracotta.modules.ehcache.presentation.model.CacheStatisticsModel.LOCAL_HEAP_SIZE_IN_BYTES;
import static org.terracotta.modules.ehcache.presentation.model.CacheStatisticsModel.LOCAL_OFFHEAP_SIZE;
import static org.terracotta.modules.ehcache.presentation.model.CacheStatisticsModel.LOCAL_OFFHEAP_SIZE_IN_BYTES;
import static org.terracotta.modules.ehcache.presentation.model.CacheStatisticsModel.MAX_GET_TIME_MILLIS;
import static org.terracotta.modules.ehcache.presentation.model.CacheStatisticsModel.MIN_GET_TIME_MILLIS;
import static org.terracotta.modules.ehcache.presentation.model.CacheStatisticsModel.OFF_HEAP_HIT_COUNT;
import static org.terracotta.modules.ehcache.presentation.model.CacheStatisticsModel.OFF_HEAP_MISS_COUNT;
import static org.terracotta.modules.ehcache.presentation.model.CacheStatisticsModel.ON_DISK_HIT_COUNT;
import static org.terracotta.modules.ehcache.presentation.model.CacheStatisticsModel.ON_DISK_MISS_COUNT;
import static org.terracotta.modules.ehcache.presentation.model.CacheStatisticsModel.PUT_COUNT;
import static org.terracotta.modules.ehcache.presentation.model.CacheStatisticsModel.REMOVED_COUNT;
import static org.terracotta.modules.ehcache.presentation.model.CacheStatisticsModel.SHORT_NAME;
import static org.terracotta.modules.ehcache.presentation.model.CacheStatisticsModel.UPDATE_COUNT;
import static org.terracotta.modules.ehcache.presentation.model.CacheStatisticsModel.WRITER_QUEUE_LENGTH;
import static org.terracotta.modules.ehcache.presentation.model.CacheStatisticsModel.XA_COMMIT_COUNT;
import static org.terracotta.modules.ehcache.presentation.model.CacheStatisticsModel.XA_ROLLBACK_COUNT;

import org.apache.commons.lang.StringUtils;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerInstance;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerModel;
import org.terracotta.modules.ehcache.presentation.model.CacheManagerModelListener;
import org.terracotta.modules.ehcache.presentation.model.CacheModel;
import org.terracotta.modules.ehcache.presentation.model.CacheStatisticsModel;
import org.terracotta.modules.ehcache.presentation.model.ClusteredCacheModel;
import org.terracotta.modules.ehcache.presentation.model.StandaloneCacheModel;

import com.tc.admin.common.ApplicationContext;
import com.tc.admin.common.BasicWorker;
import com.tc.admin.common.ExceptionHelper;
import com.tc.admin.common.LinkButton;
import com.tc.admin.common.SyncHTMLEditorKit;
import com.tc.admin.common.XAbstractAction;
import com.tc.admin.common.XButton;
import com.tc.admin.common.XCheckBox;
import com.tc.admin.common.XContainer;
import com.tc.admin.common.XLabel;
import com.tc.admin.common.XObjectTable;
import com.tc.admin.common.XObjectTableModel;
import com.tc.admin.common.XScrollPane;
import com.tc.admin.common.XSplitPane;
import com.tc.admin.common.XTable.BaseRenderer;
import com.tc.admin.common.XTable.PercentRenderer;
import com.tc.admin.common.XTextPane;
import com.tc.admin.model.IClient;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

/**
 * Abstract base class for {@link AggregateEhcacheRuntimeStatsPanel} and {@link ClientEhCacheRuntimeStatsPanel} that
 * shows a table of cache statistics from {@link CacheStatisticsModel}.
 */
public abstract class BaseEhcacheRuntimeStatsPanel extends BaseClusterModelPanel implements HierarchyListener,
    ListSelectionListener, CacheManagerModelListener {
  protected final CacheManagerModel          cacheManagerModel;

  protected XObjectTable                     cacheTable;
  protected CacheStatisticsTableModel        cacheTableModel;
  protected CacheModel                       selectedCacheModel;

  private RefreshAction                      refreshStatisticsAction;
  private ClearStatisticsAction              clearStatisticsAction;

  protected String[]                         DEFAULT_COLUMNS           = { SHORT_NAME, CACHE_HIT_RATIO,
      AVERAGE_GET_TIME_MILLIS, LOCAL_HEAP_SIZE, LOCAL_DISK_SIZE       };
  protected String[]                         selectedColumns;
  protected final Preferences                visibleColumnsPrefs;
  protected final static String              VISIBLE_COLUMNS_PREFS_KEY = "VisibleColumns";
  protected final static Map<String, String> COLUMN_HEADER_TIP_MAP     = new HashMap<String, String>();
  protected final static Map<String, String> CUSTOMIZE_DESCRIPTION_MAP = new HashMap<String, String>();

  static {
    COLUMN_HEADER_TIP_MAP.put(CACHE_NAME, "<html>Full cache name</html>");
    CUSTOMIZE_DESCRIPTION_MAP.put(CACHE_NAME, "<html>The full cache name, as specified in the configuration<html/>");

    COLUMN_HEADER_TIP_MAP.put(SHORT_NAME, "<html>Short cache name</html>");
    CUSTOMIZE_DESCRIPTION_MAP
        .put(SHORT_NAME,
             "<html>The, possibly shortened, cache name.<p>If the name appears to be a fully-qualified Java class"
                 + " name, it will be shorted to include only the first letter of all non-final components:"
                 + "<p><code>com.myco.util.HelperClass --> c.m.u.HelperClass</code><html/>");

    COLUMN_HEADER_TIP_MAP.put(CACHE_HIT_RATIO, "<html>Proportion of gets that resulted in cache hits</html>");
    CUSTOMIZE_DESCRIPTION_MAP
        .put(CACHE_HIT_RATIO,
             "<html>The proportion of gets that resulted in cache hits.<p><code>hitRatio = hits/(hits+misses)</code></html>");

    COLUMN_HEADER_TIP_MAP.put(CACHE_HIT_COUNT, "<html>Number of gets that returned a non-null result</html>");
    CUSTOMIZE_DESCRIPTION_MAP
        .put(CACHE_HIT_COUNT,
             "<html>The total number of gets that returned a non-null result across all active caching tiers.</html>");

    COLUMN_HEADER_TIP_MAP.put(IN_MEMORY_HIT_COUNT,
                              "<html>Number of gets that returned a non-null result<br>from local memory</html>");
    CUSTOMIZE_DESCRIPTION_MAP
        .put(IN_MEMORY_HIT_COUNT,
             "<html>The number of gets that returned a non-null result from the cache's local memory tier.</html>");

    COLUMN_HEADER_TIP_MAP
        .put(OFF_HEAP_HIT_COUNT,
             "<html>Number of gets that returned a non-null result<br>from the local off-heap memory</html>");
    CUSTOMIZE_DESCRIPTION_MAP
        .put(OFF_HEAP_HIT_COUNT,
             "<html>The number of gets that returned a non-null result from local off-heap memory."
                 + "<p>The local off-heap memory feature is only available in the Terracotta Enterprise Edition.</html>");

    COLUMN_HEADER_TIP_MAP.put(ON_DISK_HIT_COUNT,
                              "<html>Number of gets that returned a non-null result from the local disk"
                                  + "<br>or the Terracotta Server Array</html>");
    CUSTOMIZE_DESCRIPTION_MAP.put(ON_DISK_HIT_COUNT,
                                  "<html>The number of gets that returned a non-null result from either the cache's local disk store"
                                      + " or from the Terracotta Server Array.</html>");

    COLUMN_HEADER_TIP_MAP.put(CACHE_MISS_COUNT, "<html>Number of gets that returned a null result</html>");
    CUSTOMIZE_DESCRIPTION_MAP
        .put(CACHE_MISS_COUNT,
             "<html>The number of gets that returned a null result from each of the active caching tiers.</html>");

    COLUMN_HEADER_TIP_MAP.put(CACHE_MISS_COUNT_EXPIRED,
                              "<html>Number of gets that returned a non-null result that was already expired</html>");
    CUSTOMIZE_DESCRIPTION_MAP
        .put(CACHE_MISS_COUNT_EXPIRED,
             "<html>The number of gets that returned a non-null result that was already expired due to TTI/TTL.</html>");

    COLUMN_HEADER_TIP_MAP.put(IN_MEMORY_MISS_COUNT,
                              "<html>Number of gets that returned a null result from memory</html>");
    CUSTOMIZE_DESCRIPTION_MAP
        .put(IN_MEMORY_MISS_COUNT,
             "<html>The number of gets that returned a null result from the cache's local memory store.</html>");

    COLUMN_HEADER_TIP_MAP.put(OFF_HEAP_MISS_COUNT, "<html>Number of gets that returned a null result<br>"
                                                   + "from the Terracotta Server Array's off-heap memory</html>");
    CUSTOMIZE_DESCRIPTION_MAP
        .put(OFF_HEAP_MISS_COUNT,
             "<html>The number of gets that returned a null result from the Terracotta Server Array's off-heap memory store."
                 + "<p>The TSA off-heap memory feature is only available in the Terracotta Enterprise Edition.</html>");

    COLUMN_HEADER_TIP_MAP.put(ON_DISK_MISS_COUNT, "<html>Number of gets that returned a null result from disk</html>");
    CUSTOMIZE_DESCRIPTION_MAP.put(ON_DISK_MISS_COUNT,
                                  "<html>The number of gets that returned a null result from either the cache's local"
                                      + " disk store or the Terracotta Server Array's memory or persistent "
                                      + "storage but not off-heap.</html>");

    COLUMN_HEADER_TIP_MAP.put(PUT_COUNT, "<html>Number of puts to the cache</html>");
    CUSTOMIZE_DESCRIPTION_MAP.put(PUT_COUNT,
                                  "<html>The number of puts to the cache.<p>An item put to the cache resides in one or more cache tiers,"
                                      + " depending on how the cache has been configured.</html>");

    COLUMN_HEADER_TIP_MAP.put(UPDATE_COUNT, "<html>Number of puts to existing elements in the cache</html>");
    CUSTOMIZE_DESCRIPTION_MAP.put(UPDATE_COUNT, "<html>The number of puts to existing elements in the cache.</html>");

    COLUMN_HEADER_TIP_MAP
        .put(EVICTED_COUNT,
             "<html>Number of elements that have been removed from<br>the cache due to space limitations</html>");
    CUSTOMIZE_DESCRIPTION_MAP
        .put(EVICTED_COUNT,
             "<html>The number of elements that have been removed from the cache due to space limitations."
                 + "<p>Eviction differs from expiration in that elements are expired from the cache "
                 + "due to the configured time-to-live (TTL) or time-to-expire (TTI).</html>");

    COLUMN_HEADER_TIP_MAP
        .put(EXPIRED_COUNT, "<html>Number of elements that have been removed from<br>the cache due to TTI/TTL</html>");
    CUSTOMIZE_DESCRIPTION_MAP
        .put(EXPIRED_COUNT,
             "<html>The number of elements that have been removed from the cache due to the configured "
                 + "time-to-live (TTL) or time-to-expire (TTI)."
                 + "<p>Expiration differs from eviction in that elements are evicted due to space limitations.</html>");

    COLUMN_HEADER_TIP_MAP.put(REMOVED_COUNT,
                              "<html>Number of elements that have been manually<br>removed from the cache</html>");
    CUSTOMIZE_DESCRIPTION_MAP
        .put(REMOVED_COUNT,
             "<html>The number of elements that have been manually removed from the cache."
                 + "<p>Clearing the cache, either programatically or via the JMX interface, is not consider for this metric.</html>");

    COLUMN_HEADER_TIP_MAP.put(AVERAGE_GET_TIME_MILLIS,
                              "<html>Average cost in milliseconds to access an element in the cache</html>");
    CUSTOMIZE_DESCRIPTION_MAP.put(AVERAGE_GET_TIME_MILLIS,
                                  "<html>The average cost in milliseconds to attempt to access an element in the cache,"
                                      + " regardless of whether or not a non-null value was returned.</html>");

    COLUMN_HEADER_TIP_MAP.put(MIN_GET_TIME_MILLIS,
                              "<html>Minimum cost in milliseconds to access an element in the cache</html>");
    CUSTOMIZE_DESCRIPTION_MAP.put(MIN_GET_TIME_MILLIS,
                                  "<html>The minimum cost in milliseconds to attempt to access an element in the cache,"
                                      + " regardless of whether or not a non-null value was returned.</html>");

    COLUMN_HEADER_TIP_MAP.put(MAX_GET_TIME_MILLIS,
                              "<html>Maximum cost in milliseconds to access an element in the cache</html>");
    CUSTOMIZE_DESCRIPTION_MAP.put(MAX_GET_TIME_MILLIS,
                                  "<html>The maximun cost in milliseconds to attempt to access an element in the cache,"
                                      + " regardless of whether or not a non-null value was returned.</html>");

    COLUMN_HEADER_TIP_MAP.put(LOCAL_HEAP_SIZE, "<html>Number of elements held in cache's local memory store</html>");
    CUSTOMIZE_DESCRIPTION_MAP.put(LOCAL_HEAP_SIZE,
                                  "<html>The number of elements held in the cache's local memory store.</html>");

    COLUMN_HEADER_TIP_MAP.put(LOCAL_HEAP_SIZE_IN_BYTES,
                              "<html>Number of bytes held in cache's local memory store</html>");
    CUSTOMIZE_DESCRIPTION_MAP.put(LOCAL_HEAP_SIZE_IN_BYTES,
                                  "<html>The number of bytes held in the cache's local memory store.</html>");

    COLUMN_HEADER_TIP_MAP.put(LOCAL_OFFHEAP_SIZE,
                              "<html>Number of elements held in cache's local offheap memory store</html>");
    CUSTOMIZE_DESCRIPTION_MAP
        .put(LOCAL_OFFHEAP_SIZE, "<html>The number of elements held in the cache's local offheap memory store.</html>");

    COLUMN_HEADER_TIP_MAP.put(LOCAL_OFFHEAP_SIZE_IN_BYTES,
                              "<html>Number of bytes held in cache's local offheap memory store</html>");
    CUSTOMIZE_DESCRIPTION_MAP.put(LOCAL_OFFHEAP_SIZE_IN_BYTES,
                                  "<html>The number of bytes held in the cache's local offheap memory store.</html>");

    COLUMN_HEADER_TIP_MAP.put(LOCAL_DISK_SIZE, "<html>Number of elements residing in cache's local disk store</html>");
    CUSTOMIZE_DESCRIPTION_MAP.put(LOCAL_DISK_SIZE,
                                  "<html>The number of elements residing in the cache's local disk store</html>");

    COLUMN_HEADER_TIP_MAP.put(LOCAL_DISK_SIZE_IN_BYTES,
                              "<html>Number of bytes residing in cache's local disk store</html>");
    CUSTOMIZE_DESCRIPTION_MAP.put(LOCAL_DISK_SIZE_IN_BYTES,
                                  "<html>The number of bytes residing in the cache's local disk store.</html>");

    COLUMN_HEADER_TIP_MAP.put(XA_COMMIT_COUNT,
                              "<html>Number of times the cache has taken part in an XAResource commit</html>");
    CUSTOMIZE_DESCRIPTION_MAP.put(XA_COMMIT_COUNT,
                                  "<html>The number of times the cache has taken part in an XAResource commit.</html>");

    COLUMN_HEADER_TIP_MAP.put(XA_ROLLBACK_COUNT,
                              "<html>Number of times the cache has taken part in an XAResource rollback</html>");
    CUSTOMIZE_DESCRIPTION_MAP
        .put(XA_ROLLBACK_COUNT, "<html>The number of times the cache has taken part in an XAResource rollback.</html>");

    COLUMN_HEADER_TIP_MAP.put(WRITER_QUEUE_LENGTH,
                              "<html>Number of elements queued for write-through/write-behind to the database</html>");
    CUSTOMIZE_DESCRIPTION_MAP
        .put(WRITER_QUEUE_LENGTH,
             "<html>The number of elements queued for write-through/write-behind to the database.</html>");
  }

  private final static TableModel            EMPTY_TABLE_MODEL         = new XObjectTableModel();

  protected int                              sortColumn                = -1;
  protected int                              sortDirection             = -1;

  private JButton                            selectColumnsButton;

  public BaseEhcacheRuntimeStatsPanel(ApplicationContext appContext, CacheManagerModel cacheManagerModel) {
    super(appContext, cacheManagerModel.getClusterModel());
    this.cacheManagerModel = cacheManagerModel;
    this.visibleColumnsPrefs = appContext.getPrefs().node(BaseEhcacheRuntimeStatsPanel.class.getName());
  }

  @Override
  public void setup() {
    cacheManagerModel.addCacheManagerModelListener(this);
    super.setup();
  }

  protected CacheManagerModel getCacheManagerModel() {
    return cacheManagerModel;
  }

  private int indexOf(CacheModel cacheModel) {
    int rowCount = cacheTableModel.getRowCount();
    for (int i = 0; i < rowCount; i++) {
      CacheStatisticsModel cacheStatisticsModel = (CacheStatisticsModel) cacheTableModel.getObjectAt(i);
      if (cacheStatisticsModel.getCacheName().equals(cacheModel.getCacheName())) { return i; }
    }
    return -1;
  }

  @Override
  public void init() {
    updateStats();
  }

  @Override
  public void suspend() {
    /**/
  }

  protected XContainer createTopPanel() {
    XContainer panel = new XContainer(new BorderLayout());
    selectColumnsButton = LinkButton.makeLink("Customize Columns...", new CustomizeColumnsHandler());
    panel.add(selectColumnsButton, BorderLayout.EAST);
    panel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    return panel;
  }

  protected class CustomizeColumnsMessage extends XContainer implements MouseListener, FocusListener {
    private final XCheckBox[] cbArray;
    private final XLabel      textHeader;
    private final XTextPane   textPane;
    private final XCheckBox   selectToggle;

    protected CustomizeColumnsMessage() {
      super(new BorderLayout());

      List<String> currentlySelectedColumns = Arrays.asList(getEffectiveTableColumns());
      XContainer controlsPanel = new XContainer(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.weightx = 1.0;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.gridx = gbc.gridy = 0;
      cbArray = new XCheckBox[CacheStatisticsModel.ATTRS.length];
      for (int i = 0; i < CacheStatisticsModel.ATTRS.length; i++) {
        XCheckBox cb = new XCheckBox(CacheStatisticsModel.HEADERS[i]);
        controlsPanel.add(cb, gbc);
        cb.setName(CacheStatisticsModel.ATTRS[i]);
        cb.setSelected(currentlySelectedColumns.contains(CacheStatisticsModel.ATTRS[i]));
        cb.addMouseListener(this);
        cb.addFocusListener(this);
        cbArray[i] = cb;
        gbc.gridy++;
      }
      XContainer bottomPanel = new XContainer(new FlowLayout());
      XButton resetButton = new XButton("Reset to defaults");
      bottomPanel.add(resetButton);
      resetButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          handleResetToggle();
        }
      });
      bottomPanel.add(selectToggle = new XCheckBox("Select/De-select"));
      selectToggle.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          handleSelectToggle();
        }
      });
      XContainer textPanel = new XContainer(new BorderLayout());
      textPanel.add(textHeader = new XLabel(), BorderLayout.NORTH);
      textHeader.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
      textPanel.add(new XScrollPane(textPane = new XTextPane()), BorderLayout.CENTER);
      XScrollPane controlScroller = new XScrollPane(controlsPanel);
      controlScroller.setPreferredSize(new Dimension(200, 300));
      XSplitPane centerPanel = new XSplitPane(JSplitPane.HORIZONTAL_SPLIT, controlScroller, textPanel);
      textPane.setPreferredSize(new Dimension(320, 250));
      textPane.setEditable(false);
      textPane.setEditorKit(new SyncHTMLEditorKit());
      textPane.setBackground(Color.WHITE);
      centerPanel.setDefaultDividerLocation(0.35);
      add(centerPanel);
      add(bottomPanel, BorderLayout.SOUTH);
    }

    private void handleResetToggle() {
      List<String> defaultsList = Arrays.asList(DEFAULT_COLUMNS);
      for (int i = 0; i < CacheStatisticsModel.ATTRS.length; i++) {
        cbArray[i].setSelected(defaultsList.contains(CacheStatisticsModel.ATTRS[i]));
      }
    }

    private void handleSelectToggle() {
      for (XCheckBox cb : cbArray) {
        cb.setSelected(selectToggle.isSelected());
      }
    }

    public void mouseClicked(MouseEvent e) {
      /**/
    }

    public void mousePressed(MouseEvent e) {
      /**/
    }

    public void mouseReleased(MouseEvent e) {
      /**/
    }

    public void mouseEntered(MouseEvent e) {
      ((XCheckBox) e.getSource()).requestFocus();
    }

    public void mouseExited(MouseEvent e) {
      /**/
    }

    public void focusGained(FocusEvent e) {
      XCheckBox cb = (XCheckBox) e.getSource();
      String name = cb.getName();
      textHeader.setText(cb.getText());
      String description = CUSTOMIZE_DESCRIPTION_MAP.get(name);
      if (description == null) {
        description = COLUMN_HEADER_TIP_MAP.get(name);
      }
      textPane.setText(description);
    }

    public void focusLost(FocusEvent e) {
      textPane.setText("");
    }

    void acceptResults() {
      List<String> selected = new ArrayList<String>();
      for (int i = 0; i < CacheStatisticsModel.ATTRS.length; i++) {
        if (cbArray[i].isSelected()) {
          selected.add(CacheStatisticsModel.ATTRS[i]);
        }
      }
      setSelectedTableColumns(selected.toArray(new String[0]));
      updateStats();
    }
  }

  protected class CustomizeColumnsHandler implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      CustomizeColumnsMessage message = new CustomizeColumnsMessage();
      int result = JOptionPane.showConfirmDialog(selectColumnsButton, message, "Customize Columns",
                                                 JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
      if (result == JOptionPane.OK_OPTION) {
        message.acceptResults();
      }
    }
  }

  @Override
  protected XContainer createMainPanel() {
    XContainer panel = new XContainer(new BorderLayout());

    panel.add(createTopPanel(), BorderLayout.NORTH);
    cacheTable = new XObjectTable(cacheTableModel = new CacheStatisticsTableModel(getEffectiveTableColumns())) {
      @Override
      public String getToolTipText(MouseEvent me) {
        int hitRowIndex = rowAtPoint(me.getPoint());
        int hitColIndex = columnAtPoint(me.getPoint());
        if (hitRowIndex != -1) {
          if (hitColIndex == 0) {
            CacheStatisticsModel crs = (CacheStatisticsModel) cacheTableModel.getObjectAt(hitRowIndex);
            return crs.getCacheName();
          } else if (hitColIndex > 1) {
            int sum = 0;
            for (int i = 0; i < cacheTableModel.getRowCount(); i++) {
              Number n = (Number) cacheTableModel.getValueAt(i, hitColIndex);
              sum += n.intValue();
            }
            return Integer.toString(sum) + " Total " + cacheTableModel.getColumnName(hitColIndex);
          }
        }
        return super.getToolTipText(me);
      }

      @Override
      protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel) {
          @Override
          public String getToolTipText(MouseEvent me) {
            int column;
            if ((column = columnAtPoint(me.getPoint())) != -1) {
              TableColumn aColumn = columnModel.getColumn(column);
              return COLUMN_HEADER_TIP_MAP.get(aColumn.getIdentifier());
            }
            return null;
          }
        };
      }
    };
    cacheTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    cacheTable.addHierarchyListener(this);
    cacheTable.getSelectionModel().addListSelectionListener(this);
    panel.add(new XScrollPane(cacheTable), BorderLayout.CENTER);

    panel.add(createBottomPanel(), BorderLayout.SOUTH);

    return panel;
  }

  protected JComponent createBottomPanel() {
    EhcacheToolBar bottomPanel = new EhcacheToolBar();
    bottomPanel.add(refreshStatisticsAction = new RefreshAction());
    bottomPanel.add(clearStatisticsAction = new ClearStatisticsAction());
    return bottomPanel;
  }

  private class RefreshAction extends XAbstractAction {
    private RefreshAction() {
      super(bundle.getString("refresh"), new ImageIcon(
                                                       RefreshAction.class
                                                           .getResource("/com/tc/admin/icons/refresh.gif")));
    }

    public void actionPerformed(final ActionEvent ae) {
      updateStats();
    }
  }

  private class ClearStatisticsAction extends XAbstractAction {
    private ClearStatisticsAction() {
      super(bundle.getString("clear.all.statistics"), CLEAR_STATS_ICON);
    }

    public void actionPerformed(final ActionEvent ae) {
      queryClearAllStats();
    }
  }

  public void hierarchyChanged(final HierarchyEvent e) {
    XObjectTable table = (XObjectTable) e.getComponent();
    if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
      if (table.isShowing()) {
        if (table == cacheTable) {
          updateStats();
        }
      }
    }
  }

  public void valueChanged(ListSelectionEvent e) {
    if (!e.getValueIsAdjusting()) {
      CacheModel cacheModel = null;
      int row = cacheTable.getSelectedRow();
      if (row != -1) {
        CacheStatisticsModel cacheStatisticsModel = (CacheStatisticsModel) cacheTableModel.getObjectAt(row);
        cacheModel = cacheManagerModel.getCacheModel(cacheStatisticsModel.getCacheName());
      }
      setSelectedCacheModel(cacheModel);
    }
  }

  public void setSelectedCacheModel(CacheModel cacheModel) {
    CacheModel oldSelectedCacheModel = this.selectedCacheModel;
    this.selectedCacheModel = cacheModel;
    if (cacheModel != null) {
      for (int i = 0; i < cacheTableModel.getRowCount(); i++) {
        CacheStatisticsModel cacheStatisticsModel = (CacheStatisticsModel) cacheTableModel.getObjectAt(i);
        if (cacheModel.getCacheName().equals(cacheStatisticsModel.getCacheName())) {
          cacheTable.setSelectedRow(i);
          break;
        }
      }
    }
    if (oldSelectedCacheModel != cacheModel) {
      firePropertyChange("SelectedCacheModel", oldSelectedCacheModel, cacheModel);
    }
  }

  private void queryClearAllStats() {
    XLabel msg = new XLabel(bundle.getString("clear.all.counters.confirm"));
    Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
    int answer = JOptionPane.showConfirmDialog(this, msg, frame.getTitle(), JOptionPane.OK_CANCEL_OPTION);
    if (answer == JOptionPane.OK_OPTION) {
      appContext.submit(new ClearAllStatsWorker());
    }
  }

  private class ClearAllStatsWorker extends BasicWorker<Void> {
    private ClearAllStatsWorker() {
      super(new Callable<Void>() {
        public Void call() throws Exception {
          clearAllStats();
          return null;
        }
      });
      clearStatisticsAction.setEnabled(false);
    }

    @Override
    protected void finished() {
      Exception e = getException();
      if (e != null) {
        Throwable rootCause = ExceptionHelper.getRootCause(e);
        if (!(rootCause instanceof IOException)) {
          appContext.log(e);
        }
      } else {
        updateStats();
      }
      clearStatisticsAction.setEnabled(true);
    }
  }

  protected void setSelectedTableColumns(String[] selectedColumns) {
    this.selectedColumns = selectedColumns;
    if (selectedColumns != null) {
      visibleColumnsPrefs.put(VISIBLE_COLUMNS_PREFS_KEY, StringUtils.join(selectedColumns, ","));
    } else {
      visibleColumnsPrefs.remove(VISIBLE_COLUMNS_PREFS_KEY);
    }
  }

  protected String[] getSelectedTableColumns() {
    return selectedColumns;
  }

  protected String[] getEffectiveTableColumns() {
    return selectedColumns != null ? selectedColumns : DEFAULT_COLUMNS;
  }

  protected class TableModelWorker extends BasicWorker<CacheStatisticsTableModel> {
    protected int selectedRow;

    protected TableModelWorker(final Callable<CacheStatisticsTableModel> callable) {
      super(callable);
      selectedRow = cacheTable.getSelectedRow();
    }

    @Override
    protected void finished() {
      Exception e = getException();
      if (e != null) {
        appContext.log(ExceptionHelper.getRootCause(e));
      } else {
        setCacheStatisticsTableModel(getResult());
        if (selectedRow != -1) {
          cacheTable.setSelectedRow(selectedRow);
        } else if (selectedCacheModel != null) {
          selectedRow = indexOf(selectedCacheModel);
          if (selectedRow != -1) {
            cacheTable.setSelectedRow(selectedRow);
          }
        }
      }
      refreshStatisticsAction.setEnabled(true);
    }
  }

  protected abstract TableModelWorker createTableModelWorker();

  protected void setCacheStatisticsTableModel(CacheStatisticsTableModel tableModel) {
    cacheTable.setModel(cacheTableModel = tableModel);
    sortTable(cacheTable);

    try {
      int i = cacheTable.getColumnModel().getColumnIndex(CACHE_HIT_RATIO);
      cacheTable.getColumnModel().getColumn(i).setCellRenderer(new PercentRenderer());
    } catch (IllegalArgumentException iae) {/**/
    }

    try {
      int i = cacheTable.getColumnModel().getColumnIndex(AVERAGE_GET_TIME_MILLIS);
      cacheTable.getColumnModel().getColumn(i).setCellRenderer(new LatencyRenderer());
    } catch (IllegalArgumentException iae) {/**/
    }
  }

  public static class LatencyRenderer extends BaseRenderer {
    public LatencyRenderer() {
      super("#,##0.000;(#,##0.000)");
      label.setHorizontalAlignment(SwingConstants.RIGHT);
    }
  }

  public void cacheModelAdded(CacheModel cacheModel) {/**/
  }

  public void cacheModelRemoved(CacheModel cacheModel) {/**/
  }

  public void cacheModelChanged(CacheModel cacheModel) {/**/
  }

  public void instanceAdded(CacheManagerInstance instance) {
    /**/
  }

  public void instanceRemoved(CacheManagerInstance instance) {
    /**/
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

  @Override
  public void clientConnected(IClient client) {
    /**/
  }

  @Override
  public void clientDisconnected(IClient client) {
    /**/
  }

  protected void updateStats() {
    if (isShowing()) {
      cacheTable.setModel(EMPTY_TABLE_MODEL);
      refreshStatisticsAction.setEnabled(false);
      String vcp = visibleColumnsPrefs.get(VISIBLE_COLUMNS_PREFS_KEY, null);
      String[] vca;
      if (vcp == null) {
        vca = DEFAULT_COLUMNS;
      } else {
        vca = StringUtils.split(vcp, ",");
      }
      setSelectedTableColumns(vca);
      appContext.execute(createTableModelWorker());
    }
  }

  private void clearAllStats() {
    try {
      cacheManagerModel.clearStatistics();
    } catch (Exception e) {
      appContext.log(e);
    }
  }

  private void sortTable(final XObjectTable table) {
    if (sortColumn == -1) {
      sortColumn = table.getSortColumn();
      sortDirection = table.getSortDirection();
    }

    table.setSortColumn(sortColumn);
    table.setSortDirection(sortDirection);

    table.sort();
    ((XObjectTableModel) table.getModel()).fireTableDataChanged();
  }

  @Override
  public void tearDown() {
    cacheManagerModel.removeCacheManagerModelListener(this);
    super.tearDown();
  }
}

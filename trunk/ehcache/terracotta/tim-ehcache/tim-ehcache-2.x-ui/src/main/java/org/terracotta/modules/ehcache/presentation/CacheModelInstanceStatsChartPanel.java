/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.ehcache.presentation;

import org.terracotta.modules.ehcache.presentation.model.CacheModelInstance;

import com.tc.admin.common.ApplicationContext;

import javax.management.ObjectName;
import javax.swing.Icon;

public class CacheModelInstanceStatsChartPanel extends ClientEhcacheStatsChartPanel {
  private final CacheModelInstance cacheModelInstance;

  public CacheModelInstanceStatsChartPanel(ApplicationContext appContext, CacheModelInstance cacheModelInstance) {
    super(appContext, cacheModelInstance.getCacheManagerInstance().getCacheManagerModel(), cacheModelInstance
        .getCacheManagerInstance().getClient());
    this.cacheModelInstance = cacheModelInstance;
  }

  @Override
  protected ObjectName getBeanName() {
    return cacheModelInstance.getBeanName();
  }

  @Override
  protected void updateSummary() {
    String text = null;
    Icon icon = null;
    if (!cacheModelInstance.isStatisticsEnabled()) {
      text = "This cache instance does not have statistics enabled.";
      icon = EhcachePresentationUtils.ALERT_ICON;
    }
    summaryLabel.setText(text);
    summaryLabel.setIcon(icon);
    summaryLabel.setVisible(text != null);
    manageStatsButton.setVisible(text != null);
  }
}

/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.hibernatecache.presentation;

import com.tc.util.runtime.Os;

import java.awt.Insets;
import java.util.ListResourceBundle;
import java.util.ResourceBundle;

import javax.swing.UIManager;

public class HibernateResourceBundle extends ListResourceBundle {
  private static final Insets buttonSmallMargin;

  static {
    Insets insets = null;
    if (Os.isMac()) {
      insets = new Insets(2, 5, 2, 5);
    } else {
      insets = UIManager.getInsets("Button.margin");
    }
    buttonSmallMargin = insets;
  }

  public HibernateResourceBundle() {
    super();
    setParent(ResourceBundle.getBundle("com.tc.admin.AdminClientBundle"));
  }

  @Override
  public Object[][] getContents() {
    return new Object[][] {
        { "hibernate", "Hibernate" },
        { "no.session-factories.msg", "There are currently no active Hibernate session factories." },
        { "second-level.cache", "Second-Level Cache" },
        { "select.feature", "Select Feature:" },
        { "sql.execution.rate", "DB SQL Execution Rate" },
        { "cache.hit.ratio", "Cache Hit Ratio" },
        { "cache.hit.rate", "Cache Hit Rate" },
        { "cache.read.rate", "Cache Read Rate" },
        { "cache.put.rate", "Cache Put Rate" },
        { "cache.miss.rate", "Cache Miss Rate" },
        { "misses", "Misses" },
        { "hits", "Hits" },
        { "puts", "Puts" },
        { "current.value", "< Current Value" },
        { "local.maximum.value", "Local Maximum Value >" },
        { "cluster.stats", "Cluster stats" },
        { "evict.all.entries.in.region", "Evict All Entries in Region" },
        {
            "evict.all.entries.in.region.confirm",
            "<html><p>Evicting all entries in the region will remove all cached entries from the<br>region <code>{0}</code>.<br>Proceed?</p></html>" },
        { "evict.all.entries", "Evict All Entries" },
        { "flush.cache.confirm",
            "<html><p>Evicting all entries will remove all cached entries from the cache.  Proceed?</p></html>" },
        { "region.operations", "Region Operations" },
        { "region.settings", "Region Settings" },
        { "cache.operations", "Cache Operations" },
        { "advanced.config", "Advanced..." },
        { "advanced.cache.settings", "Advanced Cache Settings" },
        { "tti", "Time to idle:" },
        { "ttl", "Time to live:" },
        { "target.max.total.count", "Target max total count:" },
        { "target.max.in-memory.count", "Target max in-memory count:" },
        { "region.caching.enabled", "Caching enabled:" },
        { "enable.region", "Enable Region" },
        { "disable.region", "Disable Region" },
        { "enable.region.confirm", "<html><p>Enable the cache region {0}?</p></html>" },
        { "disable.region.confirm", "<html><p>Disable the cache region <code>{0}</code>?</p></html>" },
        { "enable.cache", "Enable All Cache Regions" },
        { "disable.cache", "Disable All Cache Regions" },
        { "enable.all.cache.regions.confirm", "<html><p>Enable all cache regions?</p></html>" },
        { "disable.all.cache.regions.confirm", "<html><p>Disable all cache regions?</p></html>" },
        { "logging.enabled", "Logging enabled" },
        { "overview", "Overview" },
        { "cache.regions", "Cache Regions" },
        { "runtime.statistics", "Runtime Statistics" },
        { "cache", "Cache" },
        { "regions", "Regions" },
        { "entities", "Entities" },
        { "collections", "Collections" },
        { "queries", "Queries" },
        { "generate.configuration", "Generate Cache Configuration..." },
        { "regions.summary.format", "Caching {0} of {1} cacheable regions." },
        { "on", "On" },
        { "off", "Off" },
        { "persistence.unit", "Persistence Unit:" },
        { "enable.stats", "Enable Stats" },
        { "disable.stats", "Disable Stats" },
        {
            "enable.stats.confirm",
            "<html><p>Note that statistics gathering entails some performance cost.<br><br>Enable statistics gathering for all cache regions?</p></html>" },
        { "disable.stats.confirm", "Disable statistics gathering for all cache regions?" },
        { "clear.all.stats", "Clear Stats" },
        { "clear.all.counters.confirm",
            "<html><p>This will clear global counters for both Hibernate and Hibernate Second-Level Cache. Proceed?</p></html>" },
        { "cache.enabled", "Cache enabled:" }, { "global.cache.performance", "Global Cache Performance" },
        { "per-region.cache.performance", "Per-Region Cache Performance" }, { "configuration", "Configuration" },
        { "statistics", "Statistics" }, { "busy", "Busy..." }, { "wait", "Wait..." },
        { "button.small.margin", buttonSmallMargin } };
  }
}

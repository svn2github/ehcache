/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package org.terracotta.modules.ehcache.presentation;

import com.tc.util.runtime.Os;

import java.awt.Insets;
import java.util.ListResourceBundle;
import java.util.ResourceBundle;

import javax.swing.UIManager;

public class EhcacheResourceBundle extends ListResourceBundle {
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

  public EhcacheResourceBundle() {
    super();
    setParent(ResourceBundle.getBundle("com.tc.admin.AdminClientBundle"));
  }

  @Override
  public Object[][] getContents() {
    return new Object[][] {
        { "Ehcache", "Ehcache" },
        { "cache.manager", "Cache Manager:" },
        { "no.cache-managers.msg", "There are currently no active CacheManagers." },
        { "second-level.cache", "Second-Level Cache" },
        { "select.feature", "Select Feature:" },
        { "cache.hit.ratio", "Cache Hit Ratio" },
        { "cache.hit.rate", "Cache Hit Rate" },
        { "cache.in-memory.hit.rate", "Cache InMemory Hit Rate" },
        { "cache.off-heap.hit.rate", "Cache OffHeap Hit Rate" },
        { "cache.on-disk.hit.rate", "Cache OnDisk Hit Rate" },
        { "cache.miss.rate", "Cache Miss Rate" },
        { "cache.in-memory.miss.rate", "Cache InMemory Miss Rate" },
        { "cache.off-heap.miss.rate", "Cache OffHeap Miss Rate" },
        { "cache.on-disk.miss.rate", "Cache OnDisk Miss Rate" },
        { "cache.put.rate", "Cache Put Rate" },
        { "cache.eviction.rate", "Cache Eviction Rate" },
        { "cache.expiration.rate", "Cache Expiration Rate" },
        { "cache.update.rate", "Cache Update Rate" },
        { "cache.remove.rate", "Cache Remove Rate" },
        { "cache.search.rate", "Cache Search Rate" },
        { "cache.average.search.time", "Cache Average Search Time" },
        { "transaction.commit.rate", "Tx Commit Rate" },
        { "transaction.rollback.rate", "Tx Rollback Rate" },
        { "writer.queue.length", "Writer Queue Length" },
        { "writer.max.queue.size", "Writer Max Queue Size" },
        { "misses", "Misses" },
        { "hits", "Hits" },
        { "puts", "Puts" },
        { "current.value", "< Current Value" },
        { "local.maximum.value", "Local Maximum Value >" },
        { "cluster.stats", "Cluster stats" },
        { "evict.all.entries.in.cache", "Evict All Entries in Cache" },
        { "evict.all.entries.in.cache.tip", "Evict entries from this cache cluster-wide" },
        {
            "evict.all.entries.in.cache.confirm",
            "<html><p>Evicting all entries in the cache will remove all cached entries from the<br>cache <code>{0}</code>.<br>Proceed?</p></html>" },
        { "clear.all.caches", "Clear All Caches" },
        { "clear.caches", "Clear Caches" },
        { "clear.selected.caches.confirm", "<html><p>Proceed with clearing all caches?</p></html>" },
        { "clear.caches.tip", "Clear cache contents on selected cluster nodes" },
        { "cache.operations", "Cache Operations" },
        { "cache.settings", "Cache Settings" },
        { "tti", "Time to idle:" },
        { "ttl", "Time to live:" },
        { "target.max.total.count", "Max elements on disk:" },
        { "target.max.in-memory.count", "Max elements in memory:" },
        { "caching.enabled", "Caching enabled:" },
        { "enable.cache", "Enable Cache" },
        { "enable.caches.tip", "Enable caches on selected cluster nodes" },
        { "disable.cache", "Disable Cache" },
        { "disable.caches.tip", "Disable caches on selected cluster nodes" },
        { "enable.cache.confirm", "Enable the cache ''{0}''?" },
        { "disable.cache.confirm", "Disable the cache ''{0}''?" },
        { "enable.all.caches", "Enable All Caches" },
        { "disable.all.caches", "Disable All Caches" },
        { "enable.caches", "Enable Caches" },
        { "disable.caches", "Disable Caches" },
        { "enable.selected.caches.confirm", "Enable selected caches?" },
        { "disable.selected.caches.confirm",
            "<html><p>Please confirm that you would like to disable the selected caches?</p></html>" },
        { "logging.enabled", "Logging enabled" },
        { "overview", "Overview" },
        { "contents", "Contents" },
        { "search", "Search: " },
        { "no.elements", "No elements" },
        { "element", "element" },
        { "elements", "elements" },
        { "retrieved.some.elements", "Retrieved {0} {1} from a total of {2} in {3} ms." },
        { "retrieved.all.elements", "Retrieved all {0} elements in {1} ms." },
        { "retrieval.limit", "Limited to a batch size of: " },
        { "get.elements", "Get Elements" },
        { "retrieving.elements", "Retrieving elements..." },
        { "performance", "Performance" },
        { "caches", "Caches" },
        { "runtime.statistics", "Runtime Statistics" },
        { "cache", "Cache" },
        { "caches.summary.format",
            "<html><p>Clustering {0} of {1} enabled caches from a total of {2}.<br>Statistics gathering is <b>{3}</b>.</p></html>" },
        { "on", "On" },
        { "off", "Off" },
        { "enable.statistics", "Enable Statistics" },
        { "enable.statistics.tip", "Enable statistics for caches on selected cluster nodes" },
        { "disable.statistics", "Disable Statistics" },
        { "disable.statistics.tip", "Disable statistics for caches on selected cluster nodes" },
        {
            "enable.selected.statistics.confirm",
            "<html><p>Note that statistics gathering entails some performance cost.<br><br>Enable statistics gathering for selected caches?</p></html>" },
        { "disable.selected.statistics.confirm", "Disable statistics gathering for selected caches?" },
        { "clear.all.statistics", "Clear Statistics" },
        { "clear.all.counters.confirm",
            "<html><p>This will clear statistics for each cache across the cluster. Proceed?</p></html>" },
        { "cache.enabled", "Cache enabled:" },
        { "global.cache.performance", "Global Cache Performance" },
        { "generate.configuration", "Generate Cache Configuration..." },
        { "per-cache.performance", "Per-Cache Performance" },
        { "configuration", "Configuration" },
        { "statistics", "Statistics" },
        { "sizing", "Sizing" },
        { "busy", "Busy..." },
        { "wait", "Wait..." },
        { "button.small.margin", buttonSmallMargin },
        { "enable.bulkload.confirm", "Put the selected caches into bulk-load mode?" },
        { "disable.bulkload.confirm", "Remove the selected caches from bulk-load mode?" },
        { "enable.bulkload", "Enable BulkLoad Mode" },
        { "enable.bulkload.tip", "Enable bulk-load mode on selected cluster nodes" },
        { "disable.bulkload", "Disable BulkLoad Mode" },
        { "disable.bulkload.tip", "Disable bulk-load mode on selected cluster nodes" },
        { "enable.cache.bulkload.confirm", "Put cache {0} into bulk-load mode cluster-wide?" },
        { "disable.cache.bulkload.confirm", "Remove cache {0} from bulk-load mode cluster-wide?" },
        { "node.instance.detail", "Summary of CacheManager {0} on node {1}" },
        { "cache.detail", "Instances of Cache {0}" },
        { "cache-manager.residence.summary", " has {0} clustered instances containing {1} total cache instances" },
        { "node.summary", "Instances of CacheManager {0}" },
        { "cache.summary", "Cache Summary" },
        { "cache-manager.not.resident.on.client", "This client is not hosting the selected CacheManager" },
        { "query.enable.all.statistics",
            "<html><p>Some caches currently have statistics gathering disabled.<br>Enable statistics for all caches?</p></html>" },
        { "Set Cache Statistics", "Set Cache Statistics" }, { "Set Active Caches", "Set Active Caches" },
        { "Clear Cache Contents", "Clear Cache Contents" }, { "Cache Configuration...", "Cache Configuration..." },
        { "Manage Cache Configuration", "Manage Cache Configuration" },
        { "Clear Cache Contents...", "Clear Cache Contents..." }, { "Cache BulkLoading...", "Cache BulkLoading..." },
        { "Cache Statistics...", "Cache Statistics..." }, { "Manage Active Caches...", "Manage Active Caches..." },
        { "overview.cacheModelAdded", "Added Ehcache ''{0}'' to CacheManager ''{1}''" },
        { "overview.cacheModelRemoved", "Removed Ehcache ''{0}'' from CacheManager ''{1}''" },
        { "overview.cacheModelChanged", "Ehcache ''{0}'' from CacheManager ''{1}'' has changed" },
        { "contents.tableHeaders", new String[] { "Key", "Value" } } };
  }
}

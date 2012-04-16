/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.terracotta.ClusteredInstanceFactory;
import net.sf.ehcache.transaction.SoftLockFactory;
import net.sf.ehcache.transaction.TransactionIDFactory;
import net.sf.ehcache.writer.writebehind.WriteBehind;

import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.modules.ehcache.ToolkitInstanceFactoryImpl;
import org.terracotta.modules.ehcache.event.ClusteredEventReplicator;
import org.terracotta.modules.ehcache.event.TerracottaTopologyImpl;

public class TerracottaClusteredInstanceFactory implements ClusteredInstanceFactory {

  public static final String             DEFAULT_CACHE_MANAGER_NAME = "__DEFAULT__";

  private final TerracottaTopologyImpl   topology;
  protected final ToolkitInstanceFactory toolkitInstanceFactory;

  public TerracottaClusteredInstanceFactory(TerracottaClientConfiguration terracottaClientConfiguration) {
    toolkitInstanceFactory = createToolkitInstanceFactory(terracottaClientConfiguration);
    topology = new TerracottaTopologyImpl(toolkitInstanceFactory.getToolkit().getClusterInfo());
  }

  protected ToolkitInstanceFactory createToolkitInstanceFactory(TerracottaClientConfiguration terracottaClientConfiguration) {
    return new ToolkitInstanceFactoryImpl(terracottaClientConfiguration);
  }

  @Override
  public Store createStore(Ehcache cache) {
    return newStore(cache);
  }

  protected ClusteredStore newStore(Ehcache cache) {
    return new ClusteredStore(toolkitInstanceFactory, cache);
  }

  public CacheCluster getTopology() {
    return topology;
  }

  @Override
  public WriteBehind createWriteBehind(Ehcache cache) {
    return null;
  }

  @Override
  public CacheEventListener createEventReplicator(Ehcache cache) {
    return new ClusteredEventReplicator(cache, toolkitInstanceFactory.getFullyQualifiedCacheName(cache),
                                        toolkitInstanceFactory.getOrCreateCacheEventNotifier(cache));
  }

  @Override
  public String getUUID() {
    return null;
  }

  @Override
  public void shutdown() {
    //
  }

  @Override
  public TransactionIDFactory createTransactionIDFactory(String uuid, String cacheManagerName) {
    return null;
  }

  @Override
  public SoftLockFactory getOrCreateSoftLockFactory(Ehcache cache) {
    return null;
  }

}

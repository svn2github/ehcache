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
import org.terracotta.modules.ehcache.event.TerracottaTopologyImpl;
import org.terracotta.toolkit.client.TerracottaClientStaticFactory;
import org.terracotta.toolkit.client.ToolkitClient;

public class TerracottaClusteredInstanceFactory implements ClusteredInstanceFactory {

  public static final String               DEFAULT_CACHE_MANAGER_NAME = "__DEFAULT__";

  protected final ToolkitClient            toolkitClient;
  private final TerracottaTopologyImpl     topology;
  private final ToolkitInstanceFactoryImpl toolkitInstanceFactory;

  public TerracottaClusteredInstanceFactory(TerracottaClientConfiguration tcClientConfig) {
    toolkitClient = createTerracottaClient(tcClientConfig);
    toolkitInstanceFactory = new ToolkitInstanceFactoryImpl(toolkitClient.getToolkit());
    topology = new TerracottaTopologyImpl(toolkitClient.getToolkit().getClusterInfo());
  }

  private static ToolkitClient createTerracottaClient(TerracottaClientConfiguration tcClientConfig) {
    if (!tcClientConfig.isUrlConfig()) {
      // TODO: is this to be supported?
      throw new IllegalArgumentException("Embedded tc-config no longer supported");
    }
    if (tcClientConfig.isRejoin()) {
      return TerracottaClientStaticFactory.getFactory().createDedicatedClient(tcClientConfig.getUrl());
    } else {
      return TerracottaClientStaticFactory.getFactory().getOrCreateClient(tcClientConfig.getUrl());
    }
  }

  protected ToolkitInstanceFactory getToolkitInstanceFactory() {
    return toolkitInstanceFactory;
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
    return null;
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

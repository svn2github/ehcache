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

import org.terracotta.modules.ehcache.event.TerracottaTopologyImpl;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.client.TerracottaClientStaticFactory;
import org.terracotta.toolkit.client.ToolkitClient;

public class TerracottaClusteredInstanceFactory implements ClusteredInstanceFactory {

  private static final String          PREFIX                     = "org.terracotta.modules.ehcache.";
  public static final String           DEFAULT_CACHE_MANAGER_NAME = "__DEFAULT__";

  protected final ToolkitClient        toolkitClient;
  private final TerracottaTopologyImpl topology;

  public TerracottaClusteredInstanceFactory(TerracottaClientConfiguration tcClientConfig) {
    if (!tcClientConfig.isUrlConfig()) {
      // TODO: is this to be supported?
      throw new IllegalArgumentException("Embedded tc-config no longer supported");
    }
    if (tcClientConfig.isRejoin()) {
      toolkitClient = TerracottaClientStaticFactory.getFactory().createDedicatedClient(tcClientConfig.getUrl());
    } else {
      toolkitClient = TerracottaClientStaticFactory.getFactory().getOrCreateClient(tcClientConfig.getUrl());
    }
    topology = new TerracottaTopologyImpl(toolkitClient.getToolkit().getClusterInfo());
  }

  @Override
  public Store createStore(Ehcache cache) {
    return newStore(toolkitClient.getToolkit(), cache);
  }

  protected ClusteredStore newStore(Toolkit toolkit, Ehcache cache) {
    return new ClusteredStore(toolkit, cache);
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

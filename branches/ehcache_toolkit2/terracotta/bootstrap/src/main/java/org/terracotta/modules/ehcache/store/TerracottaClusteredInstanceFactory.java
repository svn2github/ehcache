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
import net.sf.ehcache.util.ProductInfo;
import net.sf.ehcache.writer.writebehind.WriteBehind;

import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.modules.ehcache.ToolkitInstanceFactoryImpl;
import org.terracotta.modules.ehcache.event.ClusteredEventReplicatorFactory;
import org.terracotta.modules.ehcache.event.TerracottaTopologyImpl;
import org.terracotta.modules.ehcache.transaction.ClusteredTransactionIDFactory;
import org.terracotta.modules.ehcache.transaction.ReadCommittedClusteredSoftLockFactory;
import org.terracotta.modules.ehcache.transaction.state.EhcacheTxnsClusteredStateFacade;
import org.terracotta.modules.ehcache.transaction.state.EhcacheTxnsClusteredStateFacadeImpl;
import org.terracotta.toolkit.ToolkitLogger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TerracottaClusteredInstanceFactory implements ClusteredInstanceFactory {

  public static final String                           DEFAULT_CACHE_MANAGER_NAME = "__DEFAULT__";

  protected final ToolkitInstanceFactory               toolkitInstanceFactory;

  // private final fields
  private final TerracottaTopologyImpl                 topology;
  private final ClusteredEventReplicatorFactory        clusteredEventReplicatorFactory;
  private final EhcacheTxnsClusteredStateFacade        ehcacheTxnsClusteredFacade;
  private final ConcurrentMap<String, SoftLockFactory> softLockFactories;

  public TerracottaClusteredInstanceFactory(TerracottaClientConfiguration terracottaClientConfiguration) {
    toolkitInstanceFactory = createToolkitInstanceFactory(terracottaClientConfiguration);
    topology = new TerracottaTopologyImpl(toolkitInstanceFactory.getToolkit().getClusterInfo());
    clusteredEventReplicatorFactory = new ClusteredEventReplicatorFactory();
    ehcacheTxnsClusteredFacade = new EhcacheTxnsClusteredStateFacadeImpl(toolkitInstanceFactory);
    softLockFactories = new ConcurrentHashMap<String, SoftLockFactory>();
    logEhcacheBuildInfo();
  }

  private void logEhcacheBuildInfo() {
    final ProductInfo ehcacheCoreProductInfo = new ProductInfo();
    ToolkitLogger logger = toolkitInstanceFactory.getToolkit().getLogger(TerracottaClusteredInstanceFactory.class
                                                                             .getName());
    logger.info(ehcacheCoreProductInfo.toString());
  }

  protected ToolkitInstanceFactory createToolkitInstanceFactory(TerracottaClientConfiguration terracottaClientConfiguration) {
    return new ToolkitInstanceFactoryImpl(terracottaClientConfiguration);
  }

  @Override
  public final Store createStore(Ehcache cache) {
    return new ClusteredSafeStore(newStore(cache));
  }

  /**
   * Override to use different implementations
   */
  protected ClusteredStore newStore(Ehcache cache) {
    return new ClusteredStore(toolkitInstanceFactory, cache);
  }

  public CacheCluster getTopology() {
    return topology;
  }

  @Override
  public WriteBehind createWriteBehind(Ehcache cache) {
    // TODO:
    return null;
  }

  @Override
  public synchronized CacheEventListener createEventReplicator(Ehcache cache) {
    return clusteredEventReplicatorFactory.getOrCreateClusteredEventReplicator(toolkitInstanceFactory, cache);
  }

  /**
   * This is used by SampledMBeanRegistrationProvider to generate a JMX MBean ObjectName containing the client's uuid so
   * that it can be associated with the correct connection when tunneled to the L2.
   */
  @Override
  public String getUUID() {
    return toolkitInstanceFactory.getToolkit().getClusterInfo().getUniversallyUniqueClientID();
  }

  @Override
  public void shutdown() {
    toolkitInstanceFactory.shutdown();
  }

  @Override
  public TransactionIDFactory createTransactionIDFactory(String uuid, String cacheManagerName) {
    return new ClusteredTransactionIDFactory(ehcacheTxnsClusteredFacade, uuid, cacheManagerName);
  }

  @Override
  public SoftLockFactory getOrCreateSoftLockFactory(Ehcache cache) {
    String name = toolkitInstanceFactory.getFullyQualifiedCacheName(cache);
    SoftLockFactory softLockFactory = softLockFactories.get(name);
    if (softLockFactory == null) {
      softLockFactory = new ReadCommittedClusteredSoftLockFactory(ehcacheTxnsClusteredFacade, cache.getCacheManager()
          .getName(), cache.getName());
      SoftLockFactory old = softLockFactories.putIfAbsent(name, softLockFactory);
      if (old != null) {
        softLockFactory = old;
      }
    }
    return softLockFactory;
  }

}

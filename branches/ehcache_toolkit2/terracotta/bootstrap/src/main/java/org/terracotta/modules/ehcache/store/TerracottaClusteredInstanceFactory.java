/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.config.CacheWriterConfiguration;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.terracotta.ClusteredInstanceFactory;
import net.sf.ehcache.transaction.SoftLockFactory;
import net.sf.ehcache.transaction.TransactionIDFactory;
import net.sf.ehcache.util.ProductInfo;
import net.sf.ehcache.writer.writebehind.WriteBehind;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.modules.ehcache.ToolkitInstanceFactoryImpl;
import org.terracotta.modules.ehcache.async.AsyncConfig;
import org.terracotta.modules.ehcache.async.AsyncCoordinator;
import org.terracotta.modules.ehcache.async.AsyncCoordinatorImpl;
import org.terracotta.modules.ehcache.event.ClusteredEventReplicatorFactory;
import org.terracotta.modules.ehcache.event.FireRejoinOperatorEventClusterListener;
import org.terracotta.modules.ehcache.event.TerracottaTopologyImpl;
import org.terracotta.modules.ehcache.transaction.ClusteredTransactionIDFactory;
import org.terracotta.modules.ehcache.transaction.SoftLockFactoryProvider;
import org.terracotta.modules.ehcache.transaction.state.EhcacheTxnsClusteredStateFacade;
import org.terracotta.modules.ehcache.transaction.state.EhcacheTxnsClusteredStateFacadeImpl;
import org.terracotta.modules.ehcache.writebehind.AsyncWriteBehind;
import org.terracotta.modules.ehcache.writebehind.WriteBehindAsyncConfig;
import org.terracotta.toolkit.ToolkitLogger;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;

import java.util.LinkedList;

public class TerracottaClusteredInstanceFactory implements ClusteredInstanceFactory {

  public static final Logger                    LOGGER                     = LoggerFactory
                                                                               .getLogger(TerracottaClusteredInstanceFactory.class);
  public static final String                    DEFAULT_CACHE_MANAGER_NAME = "__DEFAULT__";

  protected final ToolkitInstanceFactory        toolkitInstanceFactory;
  private static final String                   DEFAULT_ASYNC_NAME         = "__DEFAULT__ASYNC__";

  // private final fields
  private final CacheCluster                    topology;
  private final ClusteredEventReplicatorFactory clusteredEventReplicatorFactory;
  private final EhcacheTxnsClusteredStateFacade ehcacheTxnsClusteredFacade;
  private final SoftLockFactoryProvider         softLockFactoryProvider;

  public TerracottaClusteredInstanceFactory(TerracottaClientConfiguration terracottaClientConfiguration) {
    toolkitInstanceFactory = createToolkitInstanceFactory(terracottaClientConfiguration);
    topology = createTopology(toolkitInstanceFactory);
    clusteredEventReplicatorFactory = new ClusteredEventReplicatorFactory(toolkitInstanceFactory);
    ehcacheTxnsClusteredFacade = new EhcacheTxnsClusteredStateFacadeImpl(toolkitInstanceFactory);
    softLockFactoryProvider = new SoftLockFactoryProvider(ehcacheTxnsClusteredFacade, toolkitInstanceFactory);
    logEhcacheBuildInfo();
  }

  private static CacheCluster createTopology(ToolkitInstanceFactory factory) {
    TerracottaTopologyImpl cacheCluster = new TerracottaTopologyImpl(factory.getToolkit().getClusterInfo());
    try {
      cacheCluster.addTopologyListener(new FireRejoinOperatorEventClusterListener(factory));
    } catch (Exception e) {
      LOGGER.warn("Unable to register: " + FireRejoinOperatorEventClusterListener.class.getName(), e);
    }
    return cacheCluster;
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
    final CacheWriterConfiguration config = cache.getCacheConfiguration().getCacheWriterConfiguration();
    final AsyncConfig asyncConfig = new WriteBehindAsyncConfig(config.getMinWriteDelay() * 1000,
                                                               config.getMaxWriteDelay() * 1000,
                                                               config.getWriteBatching(), config.getWriteBatchSize(),
                                                               cache.getCacheConfiguration()
                                                                   .getTerracottaConfiguration().isSynchronousWrites(),
                                                               config.getRetryAttempts(),
                                                               config.getRetryAttemptDelaySeconds() * 1000,
                                                               config.getRateLimitPerSecond(),
                                                               config.getWriteBehindMaxQueueSize());

    final AsyncCoordinator asyncCoordinator = getAsyncCoordinator(DEFAULT_ASYNC_NAME, cache, asyncConfig);
    return new AsyncWriteBehind(asyncCoordinator, cache);
  }

  private AsyncCoordinator getAsyncCoordinator(final String asyncName, final Ehcache cache, final AsyncConfig config) {
    final String fullAsyncName = toolkitInstanceFactory.getFullAsyncName(cache, asyncName);
    final ToolkitMap<String, AsyncConfig> configMap = toolkitInstanceFactory.getOrCreateAsyncConfigMap();
    final ToolkitMap<String, LinkedList<String>> listNamesMap = toolkitInstanceFactory.getOrCreateAsyncListNamesMap();
    LinkedList<String> nameList = new LinkedList<String>();
    String nodeId = getCurrentNodeId();
    String nameListKey = toolkitInstanceFactory.getAsyncNameListKey(fullAsyncName, nodeId);
    ToolkitLock toolkitLock = toolkitInstanceFactory.getAsyncWriteLock();
    // AsyncCoordinator async = null;
    toolkitLock.lock();
    try {
      AsyncConfig oldConfig = configMap.putIfAbsent(fullAsyncName, config);
      if (oldConfig != null && !oldConfig.equals(config)) { throw new IllegalArgumentException(
                                                                                               "can not get AsyncCoordinator for same name but different configs.\nExisting config\n"
                                                                                                   + oldConfig
                                                                                                   + "\nNew Config\n"
                                                                                                   + config); }

      LinkedList<String> oldList = listNamesMap.putIfAbsent(nameListKey, nameList);
      if (oldList != null) { throw new IllegalArgumentException("can not get AsyncCoordinator more that once for node "
                                                                + nodeId); }
      return new AsyncCoordinatorImpl(fullAsyncName, nameListKey, config, toolkitInstanceFactory);
      // return async;
    } finally {
      toolkitLock.unlock();
    }
  }

  private String getCurrentNodeId() {
    return toolkitInstanceFactory.getToolkit().getClusterInfo().waitUntilNodeJoinsCluster().getId();
  }

  @Override
  public synchronized CacheEventListener createEventReplicator(Ehcache cache) {
    return clusteredEventReplicatorFactory.getOrCreateClusteredEventReplicator(cache);
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
    return softLockFactoryProvider.getOrCreateClusteredSoftLockFactory(cache);
  }

}

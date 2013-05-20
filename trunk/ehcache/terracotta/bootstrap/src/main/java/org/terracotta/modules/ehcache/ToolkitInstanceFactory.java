/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.transaction.Decision;
import net.sf.ehcache.transaction.TransactionID;
import org.terracotta.modules.ehcache.async.AsyncConfig;
import org.terracotta.modules.ehcache.collections.SerializedToolkitCache;
import org.terracotta.modules.ehcache.event.CacheDisposalNotification;
import org.terracotta.modules.ehcache.event.CacheEventNotificationMsg;
import org.terracotta.modules.ehcache.store.CacheConfigChangeNotificationMsg;
import org.terracotta.modules.ehcache.transaction.ClusteredSoftLockIDKey;
import org.terracotta.modules.ehcache.transaction.SerializedReadCommittedClusteredSoftLock;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.events.ToolkitNotifier;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;

import java.io.Serializable;
import java.util.Set;

/**
 * Factory used for creating {@link Toolkit} instances used for implementing clustered ehcache
 */
public interface ToolkitInstanceFactory {

  /**
   * Returns the toolkit associated with this factory
   */
  Toolkit getToolkit();

  /**
   * Returns a fully qualified name for the cache
   */
  String getFullyQualifiedCacheName(Ehcache cache);

  /**
   * Returns the backend {@link ToolkitCacheInternal} to be used for the cache
   */
  ToolkitCacheInternal<String, Serializable> getOrCreateToolkitCache(Ehcache cache);

  /**
   * Returns a {@link ToolkitNotifier} for the cache for notifying {@link CacheConfigChangeNotificationMsg} across the
   * cluster
   */
  ToolkitNotifier<CacheConfigChangeNotificationMsg> getOrCreateConfigChangeNotifier(Ehcache cache);

  /**
   * Returns a {@link ToolkitReadWriteLock} for protecting the cache's store cluster wide
   */
  ToolkitLock getOrCreateStoreLock(Ehcache cache);

  ToolkitMap<String, AsyncConfig> getOrCreateAsyncConfigMap();

  ToolkitMap<String, Set<String>> getOrCreateAsyncListNamesMap(String fullAsyncName);

  /**
   * Returns a {@link ToolkitNotifier} for the cachse to notify {@link CacheEventNotificationMsg} across the cluster
   */
  ToolkitNotifier<CacheEventNotificationMsg> getOrCreateCacheEventNotifier(Ehcache cache);

  /**
   * Returns a {@link ToolkitCache} for storing serialized extractors for the cache
   * 
   * @throws UnsupportedOperationException if search is not supported
   */
  ToolkitMap<String, AttributeExtractor> getOrCreateExtractorsMap(Ehcache cache);

  /**
   * Destorys any clustered state associated with the given cache.
   *
   * @param cacheManagerName
   * @param cacheName
   */
  boolean destroy(final String cacheManagerName, final String cacheName);

  /**
   * Shutdown
   */
  void shutdown();

  /**
   * Return the map used for storing commit state of ehcache transactions
   */
  SerializedToolkitCache<TransactionID, Decision> getOrCreateTransactionCommitStateMap(String cacheManagerName);

  SerializedToolkitCache<ClusteredSoftLockIDKey, SerializedReadCommittedClusteredSoftLock> getOrCreateAllSoftLockMap(String cacheManagerName,
                                                                                                                     String cacheName);

  ToolkitMap<SerializedReadCommittedClusteredSoftLock, Integer> getOrCreateNewSoftLocksSet(String cacheManagerName,
                                                                                  String cacheName);

  ToolkitMap<String, Serializable> getOrCreateClusteredStoreConfigMap(String cacheManagerName, String cacheName);

  ToolkitLock getSoftLockWriteLock(String cacheManagerName, String cacheName, TransactionID transactionID, Object key);

  ToolkitReadWriteLock getSoftLockFreezeLock(String cacheManagerName, String cacheName, TransactionID transactionID,
                                             Object key);

  ToolkitReadWriteLock getSoftLockNotifierLock(String cacheManagerName, String cacheName, TransactionID transactionID,
                                               Object key);

  void removeNonStopConfigforCache(Ehcache cache);

  ToolkitLock getLockForCache(Ehcache cache, String lockName);

  ToolkitNotifier<CacheDisposalNotification> getOrCreateCacheDisposalNotifier(Ehcache cache);
}

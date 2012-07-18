/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.transaction.Decision;
import net.sf.ehcache.transaction.TransactionID;

import org.terracotta.modules.ehcache.async.AsyncConfig;
import org.terracotta.modules.ehcache.collections.SerializedToolkitCache;
import org.terracotta.modules.ehcache.event.CacheEventNotificationMsg;
import org.terracotta.modules.ehcache.store.CacheConfigChangeNotificationMsg;
import org.terracotta.modules.ehcache.txn.ClusteredSoftLockIDKey;
import org.terracotta.modules.ehcache.txn.SerializedReadCommittedClusteredSoftLock;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.collections.ToolkitCache;
import org.terracotta.toolkit.collections.ToolkitList;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.events.ToolkitNotifier;
import org.terracotta.toolkit.internal.collections.ToolkitCacheWithMetadata;

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
   * Returns the backend {@link ToolkitCacheWithMetadata} to be used for the cache
   */
  ToolkitCacheWithMetadata<String, Serializable> getOrCreateToolkitCache(Ehcache cache);

  /**
   * Returns a {@link ToolkitNotifier} for the cache for notifying {@link CacheConfigChangeNotificationMsg} across the
   * cluster
   */
  ToolkitNotifier<CacheConfigChangeNotificationMsg> getOrCreateConfigChangeNotifier(Ehcache cache);

  /**
   * Returns a {@link ToolkitCache} for storing search attribute types
   * 
   * @throws UnsupportedOperationException if search is not supported
   */
  ToolkitCache<String, String> getOrCreateSearchAttributeTypesMap(Ehcache cache);

  /**
   * Returns a {@link ToolkitReadWriteLock} for protecting the cache's store cluster wide
   */
  ToolkitReadWriteLock getOrCreateStoreLock(Ehcache cache);

  ToolkitCache<String, AsyncConfig> getOrCreateAsyncConfigMap();

  ToolkitCache<String, Set<String>> getOrCreateAsyncListNamesMap(String fullAsyncName);

  String getFullAsyncName(Ehcache cache);

  ToolkitLock getAsyncWriteLock();

  /**
   * Returns a {@link ToolkitNotifier} for the cachse to notify {@link CacheEventNotificationMsg} across the cluster
   */
  ToolkitNotifier<CacheEventNotificationMsg> getOrCreateCacheEventNotifier(Ehcache cache);

  /**
   * Returns a {@link ToolkitCache} for storing serialized extractors for the cache
   * 
   * @throws UnsupportedOperationException if search is not supported
   */
  ToolkitCache<String, byte[]> getOrCreateSerializedExtractorsMap(Ehcache cache);

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

  ToolkitList<SerializedReadCommittedClusteredSoftLock> getOrCreateNewSoftLocksSet(String cacheManagerName,
                                                                                   String cacheName);

  ToolkitCache<String, Serializable> getOrCreateClusteredStoreConfigMap(String cacheManagerName, String cacheName);

  ToolkitLock getSoftLockWriteLock(String cacheManagerName, String cacheName, TransactionID transactionID, Object key);

  ToolkitReadWriteLock getSoftLockFreezeLock(String cacheManagerName, String cacheName, TransactionID transactionID,
                                             Object key);

  ToolkitReadWriteLock getSoftLockNotifierLock(String cacheManagerName, String cacheName, TransactionID transactionID,
                                               Object key);

}

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package net.sf.ehcache.terracotta;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.transaction.SoftLockFactory;
import net.sf.ehcache.transaction.TransactionIDFactory;
import net.sf.ehcache.writer.writebehind.WriteBehind;

public class StandaloneTerracottaClusteredInstanceFactory implements ClusteredInstanceFactory {

  private final ClusteredInstanceFactory realFactory;

  public StandaloneTerracottaClusteredInstanceFactory(final TerracottaClientConfiguration terracottaConfig) {
    // TODO: create realFactory
    realFactory = null;
  }

  public Store createStore(final Ehcache cache) {
    return realFactory.createStore(cache);
  }

  public WriteBehind createWriteBehind(final Ehcache cache) {
    return realFactory.createWriteBehind(cache);
  }

  public CacheEventListener createEventReplicator(Ehcache cache) {
    return realFactory.createEventReplicator(cache);
  }

  public String getUUID() {
    return realFactory.getUUID();
  }

  public CacheCluster getTopology() {
    return realFactory.getTopology();
  }

  /**
   * Shutdown the associated express client.
   * <p>
   * We leave the handling of the shared L1 messiness to the Client object.
   */
  public void shutdown() {
    try {
      realFactory.shutdown();
    } finally {
      // TODO: client.shutdown();
    }
  }

  public TransactionIDFactory createTransactionIDFactory(String uuid, String cacheManagerName) {
    return realFactory.createTransactionIDFactory(uuid, cacheManagerName);
  }

  public SoftLockFactory getOrCreateSoftLockFactory(Ehcache cache) {
    return realFactory.getOrCreateSoftLockFactory(cache);
  }
}

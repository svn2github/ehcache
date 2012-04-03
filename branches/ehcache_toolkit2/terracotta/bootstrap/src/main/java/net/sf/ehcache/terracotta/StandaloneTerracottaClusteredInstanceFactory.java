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

import org.terracotta.modules.ehcache.store.TerracottaClusteredInstanceFactory;

public class StandaloneTerracottaClusteredInstanceFactory implements ClusteredInstanceFactory {

  private final ClusteredInstanceFactory realFactory;

  public StandaloneTerracottaClusteredInstanceFactory(final TerracottaClientConfiguration terracottaConfig) {
    realFactory = new TerracottaClusteredInstanceFactory(terracottaConfig);
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

  public void shutdown() {
    realFactory.shutdown();
  }

  public TransactionIDFactory createTransactionIDFactory(String uuid, String cacheManagerName) {
    return realFactory.createTransactionIDFactory(uuid, cacheManagerName);
  }

  public SoftLockFactory getOrCreateSoftLockFactory(Ehcache cache) {
    return realFactory.getOrCreateSoftLockFactory(cache);
  }
}

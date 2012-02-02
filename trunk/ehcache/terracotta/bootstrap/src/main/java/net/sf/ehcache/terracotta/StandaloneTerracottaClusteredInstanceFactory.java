/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package net.sf.ehcache.terracotta;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.transaction.SoftLockFactory;
import net.sf.ehcache.transaction.TransactionIDFactory;
import net.sf.ehcache.writer.writebehind.WriteBehind;

import org.terracotta.express.Client;
import org.terracotta.express.ClientFactory;
import org.terracotta.express.ClientFactoryExtras;

public class StandaloneTerracottaClusteredInstanceFactory implements ClusteredInstanceFactory {

  private final Client                   client;
  private final ClusteredInstanceFactory realFactory;

  public StandaloneTerracottaClusteredInstanceFactory(final TerracottaClientConfiguration terracottaConfig) {
    final boolean isURLConfig = terracottaConfig.isUrlConfig();
    String tcConfig = null;
    if (isURLConfig) {
      tcConfig = terracottaConfig.getUrl();
    } else {
      tcConfig = terracottaConfig.getEmbeddedConfig();
    }

    if (terracottaConfig.isRejoin()) {
      client = ClientFactoryExtras.createDedicatedRejoinClient(tcConfig, isURLConfig, new Class[] { getClass() });
    } else {
      client = ClientFactory.getOrCreateClient(tcConfig, isURLConfig, new Class[] { getClass() });
    }

    try {
      realFactory = client.instantiate("org.terracotta.modules.ehcache.store.TerracottaClusteredInstanceFactory",
                                       new Class[] { TerracottaClientConfiguration.class },
                                       new Object[] { terracottaConfig });
    } catch (Exception e) {
      throw new CacheException("Unable to create Terracotta client", e);
    }
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
      client.shutdown();
    }
  }

  public TransactionIDFactory createTransactionIDFactory(String uuid) {
    return realFactory.createTransactionIDFactory(uuid);
  }

  public SoftLockFactory getOrCreateSoftLockFactory(Ehcache cache) {
    return realFactory.getOrCreateSoftLockFactory(cache);
  }
}

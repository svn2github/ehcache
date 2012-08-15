/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache;

import org.terracotta.modules.configuration.BundleContext;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;

public class EhcacheModuleActivator extends TerracottaConfiguratorModule {

  @Override
  protected void addInstrumentation(BundleContext context) {
    super.addInstrumentation(context);

    // mbeans
    config.addTunneledMBeanDomain("net.sf.ehcache");
    config.addTunneledMBeanDomain("net.sf.ehcache.hibernate");

    // includes
    config.addIncludePattern("org.terracotta.modules.ehcache.store.ElementSerializationStrategy");
    config.addIncludePattern("org.terracotta.modules.ehcache.store.HibernateElementSerializationStrategy");
    config.addIncludePattern("org.terracotta.modules.ehcache.store.ClusteredStore", true);
    config.addIncludePattern("org.terracotta.modules.ehcache.store.ClusteredStoreBackendImpl", true);
    config.addIncludePattern("org.terracotta.modules.ehcache.store.SearchAttributeTypes", true);
    config.addIncludePattern("org.terracotta.modules.ehcache.store.EnterpriseClusteredStore", true);
    config.addIncludePattern("org.terracotta.modules.ehcache.store.ValueModeHandlerIdentity");
    config.addIncludePattern("org.terracotta.modules.ehcache.store.ValueModeHandlerSoftLockAwareSerialization");
    config.addIncludePattern("org.terracotta.modules.ehcache.store.ValueModeHandlerSerialization", true, "init");
    config.addIncludePattern("org.terracotta.modules.ehcache.store.ValueModeHandlerHibernate");
    config.addIncludePattern("net.sf.ehcache.AbstractElementData");
    config.addIncludePattern("net.sf.ehcache.SerializationModeElementData");
    config.addIncludePattern("net.sf.ehcache.IdentityModeElementData", true);
    config.addIncludePattern("org.terracotta.modules.ehcache.coherence.IncoherentNodesSet", true, "init");
    config.addIncludePattern("org.terracotta.modules.ehcache.event.ClusteredEventReplicator", true, "initializeOnLoad");
    config.addIncludePattern("org.terracotta.modules.ehcache.transaction.ClusteredTransactionID", true);
    config.addIncludePattern("org.terracotta.modules.ehcache.transaction.ReadCommittedClusteredSoftLock", true);
    config.addIncludePattern("org.terracotta.modules.ehcache.transaction.ReadCommittedClusteredSoftLockFactory", true);
    config.addIncludePattern("org.terracotta.modules.ehcache.transaction.ClusteredSoftLockID", true);
    config.addIncludePattern("org.terracotta.modules.ehcache.transaction.xa.XidClustered", true);
    config.addIncludePattern("org.terracotta.modules.ehcache.transaction.xa.ClusteredXidTransactionID", true);
    config.addIncludePattern("org.terracotta.modules.ehcache.store.servermap.ServerMapLocalStoreFactoryImpl", true);
    config.addIncludePattern("org.terracotta.modules.ehcache.writebehind.snapshots.SerializationKeySnapshot", true);
    config.addIncludePattern("org.terracotta.modules.ehcache.writebehind.snapshots.SerializationElementSnapshot", true);

    // locks
    config.addWriteAutolock("* org.terracotta.modules.ehcache.coherence.IncoherentNodesSet.removeIncoherentNode*(..)");
    config.addWriteAutolock("* org.terracotta.modules.ehcache.coherence.IncoherentNodesSet.gotoIncoherentMode*(..)");
    config.addWriteAutolock("* org.terracotta.modules.ehcache.coherence.IncoherentNodesSet.cleanIncoherentNodes*(..)");
    config.addWriteAutolock("* org.terracotta.modules.ehcache.coherence.IncoherentNodesSet.queryIsNodeCoherent*(..)");
    config
        .addWriteAutolock("* org.terracotta.modules.ehcache.coherence.IncoherentNodesSet.waitUntilClusterCoherent*(..)");
    config
        .addWriteAutolock("* org.terracotta.modules.ehcache.transaction.ReadCommittedClusteredSoftLock.isExpired*(..)");
    config
        .addWriteAutolock("* org.terracotta.modules.ehcache.transaction.ReadCommittedClusteredSoftLockFactory.collectExpiredTransactionIDs*(..)");

    config.addReadAutoLock("* org.terracotta.modules.ehcache.coherence.IncoherentNodesSet.isClusterCoherent*(..)");
    config.addReadAutoLock("* org.terracotta.modules.ehcache.coherence.IncoherentNodesSet.isFirstIncoherent*(..)");

    // DMI
    config.addDistributedMethod("void org.terracotta.modules.ehcache.event.ClusteredEventReplicator.dmiNotify*(..)");
    config.addDistributedMethod("void org.terracotta.modules.ehcache.store.ClusteredStore.fireClusterCoherent*(..)");
    config.addDistributedMethod("void org.terracotta.modules.ehcache.store.ClusteredStore.configChanged*(..)");
  }
}

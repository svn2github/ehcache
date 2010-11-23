/**
 *  Copyright 2003-2010 Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache.terracotta;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.cluster.ClusterNode;
import net.sf.ehcache.cluster.ClusterTopologyListener;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.InvalidConfigurationException;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.StorageStrategy;
import net.sf.ehcache.terracotta.TerracottaClusteredInstanceHelper.TerracottaRuntimeType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class encapsulating the idea of a Terracotta client. Provides access to the {@link ClusteredInstanceFactory} for the cluster
 *
 * @author Abhishek Sanoujam
 *
 */
public class TerracottaClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerracottaClient.class);

    private final UUID uuid = UUID.randomUUID();
    private final RejoinStatus rejoinStatus = new RejoinStatus();
    private final TerracottaClientConfiguration terracottaClientConfiguration;
    private volatile ClusteredInstanceFactory clusteredInstanceFactory;
    private final TerracottaCacheCluster cacheCluster = new TerracottaCacheCluster();
    private final TerracottaClientRejoinListener rejoinListener;

    private volatile Thread rejoinThread;

    /**
     * Constructor accepting the {@link TerracottaClientRejoinListener} and the {@link TerracottaClientConfiguration}
     *
     * @param rejoinAction
     * @param terracottaClientConfiguration
     */
    public TerracottaClient(TerracottaClientRejoinListener rejoinAction, TerracottaClientConfiguration terracottaClientConfiguration) {
        this.rejoinListener = rejoinAction;
        this.terracottaClientConfiguration = terracottaClientConfiguration;
        if (terracottaClientConfiguration != null) {
            terracottaClientConfiguration.freezeConfig();
        }
        if (isRejoinEnabled()) {
            TerracottaRuntimeType type = TerracottaClusteredInstanceHelper.getInstance().getTerracottaRuntimeTypeOrNull();
            if (type == null) {
                throw new InvalidConfigurationException(
                        "Terracotta Rejoin is enabled but can't determine Terracotta Runtime. You are probably missing Terracotta jar(s).");
            }
            if (type != TerracottaRuntimeType.EnterpriseExpress && type != TerracottaRuntimeType.Express) {
                throw new InvalidConfigurationException("Rejoin can be enabled only in Terracotta Express mode.");
            }
        }
    }

    /**
     * Returns the default {@link StorageStrategy} type for the current Terracotta runtime.
     *
     * @return the default {@link StorageStrategy} type for the current Terracotta runtime.
     */
    public static StorageStrategy getTerracottaDefaultStrategyForCurrentRuntime() {
        return TerracottaClusteredInstanceHelper.getInstance().getDefaultStorageStrategyForCurrentRuntime();
    }

    /*
     * --------- THIS METHOD IS NOT FOR PUBLIC USE ----------
     * private method, used in unit-tests using reflection
     *
     * @param testHelper the mock TerracottaClusteredInstanceHelper for testing
     */
    private static void setTestMode(TerracottaClusteredInstanceHelper testHelper) {
        try {
            Method method = TerracottaClusteredInstanceHelper.class.getDeclaredMethod("setTestMode",
                    TerracottaClusteredInstanceHelper.class);
            method.setAccessible(true);
            method.invoke(null, testHelper);
        } catch (Exception e) {
            // just print a stack trace and ignore
            e.printStackTrace();
        }
    }

    /**
     * Returns the {@link ClusteredInstanceFactory} associated with this client
     *
     * @return The ClusteredInstanceFactory
     */
    public ClusteredInstanceFactory getClusteredInstanceFactory() {
        waitUntilRejoinComplete();
        return clusteredInstanceFactory;
    }

    /**
     * Returns true if the clusteredInstanceFactory was created, otherwise returns false.
     * Multiple threads calling this method block and only one of them creates the factory.
     *
     * @param cacheConfigs
     * @return true if the clusteredInstanceFactory was created, otherwise returns false
     */
    public boolean createClusteredInstanceFactory(Map<String, CacheConfiguration> cacheConfigs) {
        waitUntilRejoinComplete();
        if (clusteredInstanceFactory != null) {
            return false;
        }
        final boolean created;
        synchronized (this) {
            if (clusteredInstanceFactory == null) {
                clusteredInstanceFactory = createNewClusteredInstanceFactory(cacheConfigs);
                created = true;
            } else {
                created = false;
            }
        }
        return created;
    }

    /**
     * Get the {@link CacheCluster} associated with this client
     *
     * @return the {@link CacheCluster} associated with this client
     */
    public TerracottaCacheCluster getCacheCluster() {
        waitUntilRejoinComplete();
        if (clusteredInstanceFactory == null) {
            throw new CacheException("Cannot get CacheCluster as ClusteredInstanceFactory has not been initialized yet.");
        }
        return cacheCluster;
    }

    /**
     * Returns the unique id associated with this client
     *
     * @return the unique id associated with this client
     */
    public String getUUID() {
        return uuid.toString();
    }

    /**
     * Shuts down the client
     */
    public synchronized void shutdown() {
        waitUntilRejoinComplete();
        if (clusteredInstanceFactory != null) {
            clusteredInstanceFactory.shutdown();
        }
    }

    private synchronized ClusteredInstanceFactory createNewClusteredInstanceFactory(Map<String, CacheConfiguration> cacheConfigs) {
        if (clusteredInstanceFactory != null) {
            // shut down the old factory
            clusteredInstanceFactory.shutdown();
        }
        ClusteredInstanceFactory factory = TerracottaClusteredInstanceHelper.getInstance().newClusteredInstanceFactory(cacheConfigs,
                terracottaClientConfiguration);
        CacheCluster underlyingCacheCluster = factory.getTopology();
        // set up listener so that rejoin can happen upon nodeLeft
        if (isRejoinEnabled()) {
            underlyingCacheCluster.addTopologyListener(new NodeLeftListener(this, underlyingCacheCluster.waitUntilNodeJoinsCluster()));
        }

        // set up the cacheCluster with the new underlying cache cluster
        cacheCluster.setUnderlyingCacheCluster(underlyingCacheCluster);

        return new ClusteredInstanceFactoryWrapper(this, factory);
    }

    /**
     * Rejoins the cluster
     */
    private void rejoinCluster(ClusterNode oldNode) {
        if (!isRejoinEnabled()) {
            return;
        }
        synchronized (this) {
            waitUntilRejoinComplete();
            rejoinStatus.rejoinStarted();
            rejoinThread = new Thread(new RejoinAction(oldNode), "Rejoin Thread");
            rejoinThread.start();
        }
    }

    private boolean isRejoinEnabled() {
        return terracottaClientConfiguration != null && terracottaClientConfiguration.isRejoin();
    }

    private void waitUntilRejoinComplete() {
        if (Thread.currentThread() == rejoinThread) {
            // rejoin thread does not wait
            return;
        }
        if (isRejoinEnabled() && rejoinStatus.isRejoinInProgress()) {
            rejoinStatus.waitUntilRejoinComplete();
        }
    }

    /**
     * Rejoin Action runnable
     */
    private class RejoinAction implements Runnable {

        private final ClusterNode oldNode;

        public RejoinAction(ClusterNode oldNode) {
            this.oldNode = oldNode;
        }

        public void run() {
            rejoinListener.clusterRejoinStarted();
            clusteredInstanceFactory = createNewClusteredInstanceFactory(Collections.EMPTY_MAP);
            // now reinitialize all existing caches with the new instance factory, outside lock
            rejoinListener.clusterRejoinComplete();
            rejoinStatus.rejoinComplete();

            // now fire the clusterRejoined event
            cacheCluster.fireNodeRejoinedEvent(oldNode, cacheCluster.getCurrentNode());

        }
    }

    /**
     *
     * A {@link ClusterTopologyListener} that listens for node left event for a node
     *
     * @author Abhishek Sanoujam
     *
     */
    private static class NodeLeftListener implements ClusterTopologyListener {

        private final ClusterNode currentNode;
        private final TerracottaClient client;

        /**
         * Constructor accepting the client and the node to listen for
         */
        public NodeLeftListener(TerracottaClient client, ClusterNode currentNode) {
            this.client = client;
            this.currentNode = currentNode;
        }

        /**
         * {@inheritDoc}
         */
        public void nodeLeft(ClusterNode node) {
            if (node.equals(currentNode)) {
                LOGGER.info("ClusterNode [id=" + node.getId() + "] left the cluster, rejoining cluster.");
                client.rejoinCluster(node);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void clusterOffline(ClusterNode node) {
            // no-op
        }

        /**
         * {@inheritDoc}
         */
        public void clusterOnline(ClusterNode node) {
            // no-op
        }

        /**
         * {@inheritDoc}
         */
        public void nodeJoined(ClusterNode node) {
            // no-op
        }

        /**
         * {@inheritDoc}
         */
        public void clusterRejoined(ClusterNode oldNode, ClusterNode newNode) {
            // no-op
        }

    }

    /**
     * Private class maintaining the rejoin state of the client
     *
     * @author Abhishek Sanoujam
     *
     */
    private static class RejoinStatus {

        /**
         * Rejoin state enum
         *
         * @author Abhishek Sanoujam
         *
         */
        enum RejoinState {
            IN_PROGRESS, NOT_IN_PROGRESS;
        }

        private volatile RejoinState state = RejoinState.NOT_IN_PROGRESS;

        /**
         * Returns true if rejoin is in progress
         *
         * @return true if rejoin is in progress
         */
        public boolean isRejoinInProgress() {
            return state == RejoinState.IN_PROGRESS;
        }

        /**
         * Waits until rejoin is complete if in progress
         */
        public synchronized void waitUntilRejoinComplete() {
            boolean interrupted = false;
            while (state == RejoinState.IN_PROGRESS) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }

        /**
         * Set the status to rejoin in progress
         */
        public synchronized void rejoinStarted() {
            state = RejoinState.IN_PROGRESS;
            notifyAll();
        }

        /**
         * Set the rejoin status to not in progress
         */
        public synchronized void rejoinComplete() {
            state = RejoinState.NOT_IN_PROGRESS;
            notifyAll();
        }

    }

}

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
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
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
    private static final int REJOIN_SLEEP_MILLIS_ON_EXCEPTION = Integer.getInteger("net.sf.ehcache.rejoin.sleepMillisOnException", 5000);

    private final TerracottaClientConfiguration terracottaClientConfiguration;
    private volatile ClusteredInstanceFactory clusteredInstanceFactory;
    private final TerracottaCacheCluster cacheCluster = new TerracottaCacheCluster();
    private final RejoinWorker rejoinWorker = new RejoinWorker();
    private final TerracottaClientRejoinListener rejoinListener;

    /**
     * Constructor accepting the {@link TerracottaClientRejoinListener} and the {@link TerracottaClientConfiguration}
     *
     * @param cacheManager
     * @param rejoinAction
     * @param terracottaClientConfiguration
     */
    public TerracottaClient(CacheManager cacheManager, TerracottaClientRejoinListener rejoinAction,
            TerracottaClientConfiguration terracottaClientConfiguration) {
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
                throw new InvalidConfigurationException("Rejoin cannot be used in Terracotta DSO mode.");
            }
            Thread rejoinThread = new Thread(rejoinWorker, "Rejoin Worker Thread [cacheManager: " + cacheManager.getName() + "]");
            rejoinThread.setDaemon(true);
            rejoinThread.start();
        }
    }

    /**
     * Returns the default {@link StorageStrategy} type for the current Terracotta runtime.
     * @param cacheConfiguration the cache's configuration
     *
     * @return the default {@link StorageStrategy} type for the current Terracotta runtime.
     */
    public static StorageStrategy getTerracottaDefaultStrategyForCurrentRuntime(final CacheConfiguration cacheConfiguration) {
        return TerracottaClusteredInstanceHelper.getInstance().getDefaultStorageStrategyForCurrentRuntime(cacheConfiguration);
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
        rejoinWorker.waitUntilRejoinComplete();
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
        rejoinWorker.waitUntilRejoinComplete();
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
        rejoinWorker.waitUntilRejoinComplete();
        if (clusteredInstanceFactory == null) {
            throw new CacheException("Cannot get CacheCluster as ClusteredInstanceFactory has not been initialized yet.");
        }
        return cacheCluster;
    }

    /**
     * Shuts down the client
     */
    public synchronized void shutdown() {
        rejoinWorker.waitUntilRejoinComplete();
        if (clusteredInstanceFactory != null) {
            clusteredInstanceFactory.shutdown();
        }
        rejoinWorker.shutdown();
    }

    private synchronized ClusteredInstanceFactory createNewClusteredInstanceFactory(Map<String, CacheConfiguration> cacheConfigs) {
        if (clusteredInstanceFactory != null) {
            LOGGER.info("Shutting down old ClusteredInstanceFactory...");
            // shut down the old factory
            clusteredInstanceFactory.shutdown();
        }
        LOGGER.info("Creating new ClusteredInstanceFactory");
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
        rejoinWorker.startRejoin(oldNode);
    }

    private boolean isRejoinEnabled() {
        return terracottaClientConfiguration != null && terracottaClientConfiguration.isRejoin();
    }

    /**
     * Private class responsible for carrying out rejoin
     *
     * @author Abhishek Sanoujam
     *
     */
    private class RejoinWorker implements Runnable {

        private final Object rejoinSync = new Object();
        private final RejoinStatus rejoinStatus = new RejoinStatus();
        private final AtomicInteger rejoinCount = new AtomicInteger();
        private final RejoinRequestHolder rejoinRequestHolder = new RejoinRequestHolder();
        private volatile boolean shutdown;
        private volatile Thread rejoinThread;

        public void run() {
            rejoinThread = Thread.currentThread();
            while (!shutdown) {
                waitUntilRejoinRequested();
                if (shutdown) {
                    break;
                }
                boolean rejoined = false;
                final RejoinRequest rejoinRequest = rejoinRequestHolder.consume();
                while (!rejoined) {
                    try {
                        doRejoin(rejoinRequest);
                        rejoined = true;
                    } catch (Exception e) {
                        LOGGER.warn("Caught exception while trying to rejoin cluster", e);
                        LOGGER.info("Trying to rejoin again in 5 secs...");
                        sleep(REJOIN_SLEEP_MILLIS_ON_EXCEPTION);
                    }
                }
            }
        }

        private void sleep(long sleepMillis) {
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e1) {
                // ignore
            }
        }

        public void shutdown() {
            synchronized (rejoinSync) {
                shutdown = true;
                rejoinSync.notifyAll();
            }
        }

        private void doRejoin(RejoinRequest rejoinRequest) {
            if (rejoinRequest == null) {
                return;
            }
            final ClusterNode oldNodeReference = rejoinRequest.getRejoinOldNode();
            rejoinStatus.rejoinStarted();
            int rejoinNumber = rejoinCount.incrementAndGet();
            LOGGER.info("Starting Terracotta Rejoin (as client id: " + (oldNodeReference == null ? "null" : oldNodeReference.getId())
                    + " left the cluster) [rejoin count = " + rejoinNumber + "] ... ");
            rejoinListener.clusterRejoinStarted();
            clusteredInstanceFactory = createNewClusteredInstanceFactory(Collections.EMPTY_MAP);
            // now reinitialize all existing caches with the new instance factory, outside lock
            rejoinListener.clusterRejoinComplete();
            // now fire the clusterRejoined event
            fireClusterRejoinedEvent(oldNodeReference);
            LOGGER.info("Rejoin Complete [rejoin count = " + rejoinNumber + "]");
            rejoinStatus.rejoinComplete();
        }

        private void fireClusterRejoinedEvent(final ClusterNode oldNodeReference) {
            try {
                cacheCluster.fireNodeRejoinedEvent(oldNodeReference, cacheCluster.getCurrentNode());
            } catch (Throwable e) {
                LOGGER.error("Caught exception while firing rejoin event", e);
            }
        }

        private void waitUntilRejoinRequested() {
            synchronized (rejoinSync) {
                while (!rejoinRequestHolder.isRejoinRequested()) {
                    if (shutdown) {
                        break;
                    }
                    try {
                        rejoinSync.wait();
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
            }
        }

        public void startRejoin(ClusterNode oldNode) {
            synchronized (rejoinSync) {
                rejoinRequestHolder.addRejoinRequest(oldNode);
                rejoinSync.notifyAll();
            }
        }

        private void waitUntilRejoinComplete() {
            if (rejoinThread == Thread.currentThread()) {
                return;
            }
            if (isRejoinEnabled()) {
                rejoinStatus.waitUntilRejoinComplete();
            }
        }
    }

    /**
     * Private class maintaining rejoin requests
     *
     * @author Abhishek Sanoujam
     *
     */
    private static class RejoinRequestHolder {
        private RejoinRequest outstandingRequest;

        public synchronized void addRejoinRequest(ClusterNode oldNode) {
            // will hold only one pending rejoin
            outstandingRequest = new RejoinRequest(oldNode);
        }

        public synchronized RejoinRequest consume() {
            if (outstandingRequest == null) {
                return null;
            }
            RejoinRequest rv = outstandingRequest;
            outstandingRequest = null;
            return rv;
        }

        public synchronized boolean isRejoinRequested() {
            return outstandingRequest != null;
        }
    }

    /**
     * Private class - Rejoin request bean
     *
     * @author Abhishek Sanoujam
     *
     */
    private static class RejoinRequest {
        private final ClusterNode oldNode;

        public RejoinRequest(ClusterNode oldNode) {
            this.oldNode = oldNode;
        }

        public ClusterNode getRejoinOldNode() {
            return oldNode;
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

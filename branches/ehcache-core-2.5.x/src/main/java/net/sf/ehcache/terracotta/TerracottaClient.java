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

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.cluster.ClusterNode;
import net.sf.ehcache.cluster.ClusterTopologyListener;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.InvalidConfigurationException;
import net.sf.ehcache.config.MemoryUnit;
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
    private volatile ClusteredInstanceFactoryWrapper clusteredInstanceFactory;
    private final TerracottaCacheCluster cacheCluster = new TerracottaCacheCluster();
    private final RejoinWorker rejoinWorker = new RejoinWorker();
    private final TerracottaClientRejoinListener rejoinListener;
    private final CacheManager cacheManager;
    private ExecutorService l1TerminatorThreadPool;

    /**
     * Constructor accepting the {@link TerracottaClientRejoinListener} and the {@link TerracottaClientConfiguration}
     *
     * @param cacheManager
     * @param rejoinAction
     * @param terracottaClientConfiguration
     */
    public TerracottaClient(CacheManager cacheManager, TerracottaClientRejoinListener rejoinAction,
            TerracottaClientConfiguration terracottaClientConfiguration) {
        this.cacheManager = cacheManager;
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
     *
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
        rejoinWorker.shutdown();
        if (clusteredInstanceFactory != null) {
            shutdownClusteredInstanceFactoryWrapper(clusteredInstanceFactory);
        }
    }

    private void shutdownClusteredInstanceFactoryWrapper(ClusteredInstanceFactoryWrapper clusteredInstanceFactory) {
        clusteredInstanceFactory.getActualFactory().getTopology().getTopologyListeners().clear();
        clusteredInstanceFactory.shutdown();
    }

    private synchronized ClusteredInstanceFactoryWrapper createNewClusteredInstanceFactory(Map<String, CacheConfiguration> cacheConfigs) {
        // shut down the old factory
        if (clusteredInstanceFactory != null) {
            info("Shutting down old ClusteredInstanceFactory...");
            shutdownClusteredInstanceFactoryWrapper(clusteredInstanceFactory);
        }
        info("Creating new ClusteredInstanceFactory");
        ClusteredInstanceFactory factory;
        CacheCluster underlyingCacheCluster = null;
        try {
            factory = TerracottaClusteredInstanceHelper.getInstance().newClusteredInstanceFactory(cacheConfigs,
                    terracottaClientConfiguration);
            underlyingCacheCluster = factory.getTopology();
        } finally {
            // always set up listener so that rejoin can happen upon nodeLeft
            if (isRejoinEnabled()) {
                if (underlyingCacheCluster != null) {
                    underlyingCacheCluster.addTopologyListener(new NodeLeftListener(this, underlyingCacheCluster
                            .waitUntilNodeJoinsCluster()));
                } else {
                    warn("Unable to register node left listener for rejoin");
                }
            }
        }

        if (!rejoinWorker.isRejoinInProgress()) {
            // set up the cacheCluster with the new underlying cache cluster if rejoin is not in progress
            // else defer until rejoin is complete (to have node joined, online fired just before rejoin event)
            cacheCluster.setUnderlyingCacheCluster(underlyingCacheCluster);
        }

        return new ClusteredInstanceFactoryWrapper(this, factory);
    }

    /**
     * Block thread until rejoin is complete
     */
    protected void waitUntilRejoinComplete() {
        rejoinWorker.waitUntilRejoinComplete();
    }

    private synchronized ExecutorService getL1TerminatorThreadPool() {
        if (l1TerminatorThreadPool == null) {
            l1TerminatorThreadPool = Executors.newCachedThreadPool(new ThreadFactory() {
                private final ThreadGroup threadGroup = new ThreadGroup("Rejoin Terminator Thread Group");

                public Thread newThread(Runnable runnable) {
                    Thread t = new Thread(threadGroup, runnable, "L1 Terminator");
                    t.setDaemon(true);
                    return t;
                }
            });
        }
        return l1TerminatorThreadPool;
    }

    /**
     * Rejoins the cluster
     */
    private void rejoinCluster(final ClusterNode oldNode) {
        if (!isRejoinEnabled()) {
            return;
        }
        final Runnable rejoinRunnable = new Runnable() {
            public void run() {
                if (rejoinWorker.isRejoinInProgress()) {
                    debug("Current node (" + oldNode.getId() + ") left before rejoin could complete, force terminating current client");
                    if (clusteredInstanceFactory != null) {
                        // if the rejoin thread is stuck in terracotta stack, this will make the rejoin thread come out with
                        // TCNotRunningException
                        info("Shutting down old client");
                        shutdownClusteredInstanceFactoryWrapper(clusteredInstanceFactory);
                        clusteredInstanceFactory = null;
                    } else {
                        warn("Current node (" + oldNode.getId() + ") left before rejoin could complete, but previous client is null");
                    }
                    // now interrupt the thread
                    // this will interrupt the rejoin thread if its still stuck after L1 has been shutdown
                    debug("Interrupting rejoin thread");
                    rejoinWorker.rejoinThread.interrupt();
                }
                debug("Going to initiate rejoin");
                // initiate the rejoin
                rejoinWorker.startRejoin(oldNode);
            }

        };
        if (rejoinWorker.isRejoinInProgress()) {
            // if another rejoin was already in progress
            // run in another thread, so that this thread (a thread from the L1) can just go back
            // also mark that its forced shutdown first
            rejoinWorker.setForcedShutdown();
            getL1TerminatorThreadPool().execute(rejoinRunnable);
        } else {
            // no need to run in separate thread as this is just initiating the rejoin
            rejoinRunnable.run();
        }
    }

    private boolean isRejoinEnabled() {
        return terracottaClientConfiguration != null && terracottaClientConfiguration.isRejoin();
    }

    private void info(String msg) {
        info(msg, null);
    }

    private void info(String msg, Throwable t) {
        if (t == null) {
            LOGGER.info(getLogPrefix() + msg);
        } else {
            LOGGER.info(getLogPrefix() + msg, t);
        }
    }

    private String getLogPrefix() {
        return "Thread [" + Thread.currentThread().getName() + "] [cacheManager: " + getCacheManagerName() + "]: ";
    }

    private void debug(String msg) {
        LOGGER.debug(getLogPrefix() + msg);
    }

    private void warn(String msg) {
        LOGGER.warn(getLogPrefix() + msg);
    }

    private String getCacheManagerName() {
        if (cacheManager.isNamed()) {
            return "'" + cacheManager.getName() + "'";
        } else {
            return "no name";
        }
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
        private volatile boolean forcedShutdown;

        public void run() {
            rejoinThread = Thread.currentThread();
            while (!shutdown) {
                waitUntilRejoinRequested();
                if (shutdown || isJVMShuttingDown()) {
                    break;
                }
                boolean rejoined = false;
                final RejoinRequest rejoinRequest = rejoinRequestHolder.consume();
                debug("Going to start rejoin for request: " + rejoinRequest);
                while (!rejoined) {
                    try {
                        doRejoin(rejoinRequest);
                        rejoined = true;
                    } catch (Exception e) {
                        boolean forced = getAndClearForcedShutdown();
                        if (forced) {
                            info("Client was shutdown forcefully before rejoin completed", e);
                            break;
                        }
                        LOGGER.warn("Caught exception while trying to rejoin cluster", e);
                        info("Trying to rejoin again in " + REJOIN_SLEEP_MILLIS_ON_EXCEPTION + " msecs...");
                        sleep(REJOIN_SLEEP_MILLIS_ON_EXCEPTION);
                    }
                }
            }
        }

        public synchronized boolean getAndClearForcedShutdown() {
            boolean rv = forcedShutdown;
            forcedShutdown = false;
            return rv;
        }

        public synchronized void setForcedShutdown() {
            forcedShutdown = true;
        }

        public boolean isRejoinInProgress() {
            return rejoinStatus.isRejoinInProgress();
        }

        public synchronized boolean isJVMShuttingDown() {
            try {
                // Detect whether the JVM is going down by adding a shutdown hook, if it's shutting down
                // we should get an IllegalStateException.
                Thread jvmShutdownCheckThread = new Thread();
                Runtime.getRuntime().addShutdownHook(jvmShutdownCheckThread);
                Runtime.getRuntime().removeShutdownHook(jvmShutdownCheckThread);
                return false;
            } catch (IllegalStateException e) {
                return true;
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
            // copy the disconnected ClusterNode to prevent keeping a ref to it for the whole duration of
            // the rejoin and allow the GC to immediately clean up the disconnected L1.
            final ClusterNode oldNodeReference = new DisconnectedClusterNode(rejoinRequest.getRejoinOldNode());
            rejoinStatus.rejoinStarted();
            if (Thread.currentThread().isInterrupted()) {
                // clear interrupt status if set
                info("Clearing interrupt state of rejoin thread");
                Thread.currentThread().interrupted();
            }
            int rejoinNumber = rejoinCount.incrementAndGet();
            info("Starting Terracotta Rejoin (as client id: " + (oldNodeReference == null ? "null" : oldNodeReference.getId())
                    + " left the cluster) [rejoin count = " + rejoinNumber + "] ... ");
            rejoinListener.clusterRejoinStarted();
            clusteredInstanceFactory = createNewClusteredInstanceFactory(Collections.<String, CacheConfiguration>emptyMap());
            // now reinitialize all existing caches with the new instance factory, outside lock
            rejoinListener.clusterRejoinComplete();
            // now fire the clusterRejoined event
            fireClusterRejoinedEvent(oldNodeReference);
            info("Rejoin Complete [rejoin count = " + rejoinNumber + "]");
            rejoinStatus.rejoinComplete();
        }

        private void fireClusterRejoinedEvent(final ClusterNode oldNodeReference) {
            // set up the cacheCluster with the new underlying cache cluster (to fire node joined and online events)
            cacheCluster.setUnderlyingCacheCluster(clusteredInstanceFactory.getActualFactory().getTopology());
            // add another listener here to fire the rejoin event only after receiving node joined and online
            final CountDownLatch latch = new CountDownLatch(2);
            FireRejoinEventListener fireRejoinEventListener = new FireRejoinEventListener(clusteredInstanceFactory.getActualFactory()
                    .getTopology().waitUntilNodeJoinsCluster(), latch);
            clusteredInstanceFactory.getActualFactory().getTopology().addTopologyListener(fireRejoinEventListener);

            waitUntilLatchOpen(latch);
            try {
                cacheCluster.fireNodeRejoinedEvent(oldNodeReference, cacheCluster.getCurrentNode());
            } catch (Throwable e) {
                LOGGER.error("Caught exception while firing rejoin event", e);
            }
            clusteredInstanceFactory.getActualFactory().getTopology().removeTopologyListener(fireRejoinEventListener);
        }

        private void waitUntilLatchOpen(CountDownLatch latch) {
            boolean done = false;
            do {
                try {
                    latch.await();
                    done = true;
                } catch (InterruptedException e) {
                    if (forcedShutdown) {
                        throw new CacheException(e);
                    } else {
                        LOGGER.info("Ignoring interrupted exception while waiting for latch");
                    }
                }
            } while (!done);
        }

        private void waitUntilRejoinRequested() {
            String message = "Rejoin worker waiting until rejoin requested";
            List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
            for (MemoryPoolMXBean memoryPoolMXBean : memoryPoolMXBeans) {
                String name = memoryPoolMXBean.getName();
                if (!name.contains("Perm Gen")) {
                    continue;
                }
                MemoryUsage usage = memoryPoolMXBean.getUsage();
                message += " (" + name + " : " + MemoryUnit.BYTES.toMegaBytes(usage.getUsed()) + "M / "
                           + MemoryUnit.BYTES.toMegaBytes(usage.getMax()) + "M)";
            }
            info(message + "...");

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

        @Override
        public String toString() {
            return "RejoinRequest [oldNode=" + oldNode.getId() + "]";
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
            client.info("Registered interest for rejoin, current node: " + currentNode.getId());
        }

        /**
         * {@inheritDoc}
         */
        public void nodeLeft(ClusterNode node) {
            client.info("ClusterNode [id=" + node.getId() + "] left the cluster (currentNode=" + currentNode.getId() + ")");
            if (node.equals(currentNode)) {
                client.rejoinCluster(node);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void clusterOffline(ClusterNode node) {
            client.info("ClusterNode [id=" + node.getId() + "] went offline (currentNode=" + currentNode.getId() + ")");
        }

        /**
         * {@inheritDoc}
         */
        public void clusterOnline(ClusterNode node) {
            client.info("ClusterNode [id=" + node.getId() + "] became online (currentNode=" + currentNode.getId() + ")");
        }

        /**
         * {@inheritDoc}
         */
        public void nodeJoined(ClusterNode node) {
            client.info("ClusterNode [id=" + node.getId() + "] joined the cluster (currentNode=" + currentNode.getId() + ")");
        }

        /**
         * {@inheritDoc}
         */
        public void clusterRejoined(ClusterNode oldNode, ClusterNode newNode) {
            client.info("ClusterNode [id=" + oldNode.getId() + "] rejoined cluster as ClusterNode [id=" + newNode.getId()
                    + "] (currentNode=" + currentNode.getId() + ")");
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

    /**
     * Event listener that counts down on receiving node join and online event
     *
     * @author Abhishek Sanoujam
     *
     */
    private static class FireRejoinEventListener implements ClusterTopologyListener {

        private final CountDownLatch latch;
        private final ClusterNode currentNode;

        /**
         * Constructor
         *
         * @param clusterNode
         * @param latch
         */
        public FireRejoinEventListener(ClusterNode currentNode, CountDownLatch latch) {
            this.currentNode = currentNode;
            this.latch = latch;
        }

        /**
         * {@inheritDoc}
         */
        public void nodeJoined(ClusterNode node) {
            if (node.equals(currentNode)) {
                latch.countDown();
            }
        }

        /**
         * {@inheritDoc}
         */
        public void clusterOnline(ClusterNode node) {
            if (node.equals(currentNode)) {
                latch.countDown();
            }
        }

        /**
         * {@inheritDoc}
         */
        public void nodeLeft(ClusterNode node) {
            // no-op
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
        public void clusterRejoined(ClusterNode oldNode, ClusterNode newNode) {
            // no-op
        }

    }

}

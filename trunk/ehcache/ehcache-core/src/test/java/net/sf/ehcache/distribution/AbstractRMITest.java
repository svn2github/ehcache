/**
 *  Copyright Terracotta, Inc.
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

package net.sf.ehcache.distribution;

import static net.sf.ehcache.util.RetryAssert.assertBy;
import static net.sf.ehcache.util.RetryAssert.sizeOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.rmi.server.RMISocketFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import net.sf.ehcache.Cache;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.IntegrationTest;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;

import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Category(IntegrationTest.class)
public abstract class AbstractRMITest {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractRMITest.class);

    protected static Configuration getConfiguration(String fileName) {
        return ConfigurationFactory.parseConfiguration(new File(fileName));
    }

    @BeforeClass
    public static void installRMISocketFactory() {
        RMISocketFactory current = RMISocketFactory.getSocketFactory();
        if (current == null) {
            current = RMISocketFactory.getDefaultSocketFactory();
        }
        assertNotNull(current);
        try {
            RMISocketFactory.setSocketFactory(new SocketReusingRMISocketFactory(current));
            LOG.info("Installed the SO_REUSEADDR setting socket factory");
        } catch (IOException e) {
            LOG.warn("Couldn't register the SO_REUSEADDR setting socket factory", e);
        }
    }

    @BeforeClass
    public static void checkActiveThreads() {
        assertThat(getActiveReplicationThreads(), IsEmptyCollection.<Thread>empty());
    }

    protected static Set<Thread> getActiveReplicationThreads() {
        Set<Thread> threads = new HashSet<Thread>();
        for (Thread thread : JVMUtil.enumerateThreads()) {
            if (thread.getName().equals("Replication Thread")) {
                threads.add(thread);
            }
        }
        return threads;
    }
    
    @Before
    public void setupMulticastTiming() {
        MulticastKeepaliveHeartbeatSender.setHeartBeatInterval(1000);
        MulticastKeepaliveHeartbeatSender.setHeartBeatStaleTime(30000);
    }

    protected static final void setHeapDumpOnOutOfMemoryError(boolean value) {
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName beanName = ObjectName.getInstance("com.sun.management:type=HotSpotDiagnostic");
            Object vmOption = server.invoke(beanName, "setVMOption", new Object[] { "HeapDumpOnOutOfMemoryError", Boolean.toString(value) },
                                                                     new String[] { "java.lang.String", "java.lang.String" });
            LOG.info("Set HeapDumpOnOutOfMemoryError to: " + value);
        } catch (Throwable t) {
            LOG.info("Set HeapDumpOnOutOfMemoryError to: " + value + " - failed", t);
        }
    }

    protected static Collection<Throwable> runTasks(Collection<Callable<Void>> tasks) {
        final long endTime = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        final Collection<Throwable> errors = new ArrayList<Throwable>();

        // Spin up the threads
        Collection<Thread> threads = new ArrayList<Thread>(tasks.size());
        for (final Callable<Void> task : tasks) {
            Assert.assertNotNull(task);
            threads.add(new Thread() {
                @Override
                public void run() {
                    try {
                        // Run the thread until the given end time
                        while (System.nanoTime() < endTime) {
                            task.call();
                        }
                    } catch (Throwable t) {
                        // Hang on to any errors
                        errors.add(t);
                    }
                }

            });
        }

        for (Thread t : threads) {
            t.start();
        }

        boolean interrupted = false;
        try {
            for (Thread t : threads) {
                while (t.isAlive()) {
                    try {
                        t.join();
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }

        return errors;
    }

    /**
     * Wait for all caches to have a full set of peers in each manager.
     * <p>
     * This method will hang if all managers don't share a common set of replicated caches.
     */
    protected static void waitForClusterMembership(int time, TimeUnit unit, final List<CacheManager> managers) {
        waitForClusterMembership(time, unit, getAllReplicatedCacheNames(managers.get(0)), managers);
    }

    /**
     * Wait for the given caches to have a full set of peers in each manager.
     * <p>
     * Any other caches in these managers may or may not be fully announced throughout the cluster.
     */
    protected static void waitForClusterMembership(int time, TimeUnit unit, final Collection<String> cacheNames, final CacheManager ... managers) {
        waitForClusterMembership(time, unit, cacheNames, Arrays.asList(managers));
    }

    /**
     * Wait for the given caches to have a full set of peers in each manager.
     * <p>
     * Any other caches in these managers may or may not be fully announced throughout the cluster.
     */
    protected static void waitForClusterMembership(int time, TimeUnit unit, final Collection<String> cacheNames, final List<CacheManager> managers) {
        assertBy(time, unit, new Callable<Integer>() {

            public Integer call() throws Exception {
                Integer minimumPeers = null;
                for (CacheManager manager : managers) {
                    CacheManagerPeerProvider peerProvider = manager.getCacheManagerPeerProvider("RMI");
                    for (String cacheName : cacheNames) {
                        List<CachePeer> peers = peerProvider.listRemoteCachePeers(manager.getEhcache(cacheName));
                        for (CachePeer peer : peers) {
                            String hostName = peer.getUrlBase().substring(2).split(":")[0];
                            InetAddress host = InetAddress.getByName(hostName);
                            NetworkInterface iface = NetworkInterface.getByInetAddress(host);
                            if (iface == null) {
                              throw new AssertionError("Cache peer is not local: " + peer.getUrl());
                            }
                        }
                        int peerCount = peers.size();
                        if (minimumPeers == null || peerCount < minimumPeers) {
                            minimumPeers = peerCount;
                        }
                    }
                }
                if (minimumPeers == null) {
                    return 0;
                } else {
                    return minimumPeers + 1;
                }
            }
        }, is(managers.size()));
    }

    protected static void emptyCaches(int time, TimeUnit unit, List<CacheManager> members) {
        emptyCaches(time, unit, getAllReplicatedCacheNames(members.get(0)), members);
    }

    protected static void emptyCaches(final int time, final TimeUnit unit, Collection<String> required, final List<CacheManager> members) {
        List<Callable<Void>> cacheEmptyTasks = new ArrayList<Callable<Void>>();
        for (String cache : required) {
            final String cacheName = cache;
            cacheEmptyTasks.add(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    for (CacheManager manager : members) {
                        manager.getCache(cacheName).put(new Element("setup", "setup"), true);
                    }

                    members.get(0).getCache(cacheName).removeAll();
                    for (CacheManager manager : members.subList(1, members.size())) {
                        assertBy(time, unit, sizeOf(manager.getCache(cacheName)), is(0));
                    }
                    return null;
                }
            });
        }

        ExecutorService executor = Executors.newCachedThreadPool();
        try {
            for (Future<Void> result : executor.invokeAll(cacheEmptyTasks)) {
                result.get();
            }
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        } catch (ExecutionException e) {
            throw new AssertionError(e);
        } finally {
            executor.shutdown();
        }
    }

    private static Collection<String> getAllReplicatedCacheNames(CacheManager manager) {
        Collection<String> replicatedCaches = new ArrayList<String>();
        for (String name : manager.getCacheNames()) {
            Cache cache = manager.getCache(name);
            if (cache.getCacheEventNotificationService().hasCacheReplicators()) {
                replicatedCaches.add(name);
            }
        }
        return replicatedCaches;
    }

    protected static List<CacheManager> startupManagers(List<Configuration> configurations) {
        List<Callable<CacheManager>> nodeStartupTasks = new ArrayList<Callable<CacheManager>>();
        for (Configuration config : configurations) {
            final Configuration configuration = config;
            nodeStartupTasks.add(new Callable<CacheManager>() {
                @Override
                public CacheManager call() throws Exception {
                    return new CacheManager(configuration);
                }
            });
        }

        ExecutorService clusterStarter = Executors.newCachedThreadPool();
        try {
            List<CacheManager> managers = new ArrayList<CacheManager>();
            try {
                for (Future<CacheManager> result : clusterStarter.invokeAll(nodeStartupTasks)) {
                    managers.add(result.get());
                }
            } catch (InterruptedException e) {
                throw new AssertionError(e);
            } catch (ExecutionException e) {
                throw new AssertionError(e);
            }
            return managers;
        } finally {
            clusterStarter.shutdown();
        }
    }
}

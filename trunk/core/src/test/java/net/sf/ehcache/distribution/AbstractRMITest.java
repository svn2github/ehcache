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

package net.sf.ehcache.distribution;

import static net.sf.ehcache.util.RetryAssert.assertBy;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.rmi.server.RMISocketFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import net.sf.ehcache.CacheManager;

import org.hamcrest.collection.IsEmptyCollection;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRMITest {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractRMITest.class);

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

    protected static void waitForClusterMembership(int time, TimeUnit unit, final Collection<String> cacheNames, final CacheManager ... managers) {
        assertBy(time, unit, new Callable<Integer>() {

            public Integer call() throws Exception {
                Integer minimumPeers = null;
                for (CacheManager manager : managers) {
                    CacheManagerPeerProvider peerProvider = manager.getCacheManagerPeerProvider("RMI");
                    for (String cacheName : cacheNames) {
                        int peers = peerProvider.listRemoteCachePeers(manager.getEhcache(cacheName)).size();
                        if (minimumPeers == null || peers < minimumPeers) {
                            minimumPeers = peers;
                        }
                    }
                }
                if (minimumPeers == null) {
                    return 0;
                } else {
                    return minimumPeers + 1;
                }
            }
        }, is(managers.length));
    }
}

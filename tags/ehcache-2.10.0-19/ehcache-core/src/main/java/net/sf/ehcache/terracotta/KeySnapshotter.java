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

package net.sf.ehcache.terracotta;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.CacheStoreHelper;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.store.TerracottaStore;
import net.sf.ehcache.util.WeakIdentityConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A class that will snapshot the local keySet of a Terracotta clustered cache to disk
 *
 * @author Alex Snaps
 */
class KeySnapshotter implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(KeySnapshotter.class.getName());
    private static final int POOL_SIZE = Integer.getInteger("net.sf.ehcache.terracotta.KeySnapshotter.threadPoolSize", 10);

    private static final WeakIdentityConcurrentMap<CacheManager, ScheduledExecutorService> INSTANCES =
        new WeakIdentityConcurrentMap<CacheManager, ScheduledExecutorService>(
            new WeakIdentityConcurrentMap.CleanUpTask<ScheduledExecutorService>() {
                public void cleanUp(final ScheduledExecutorService executor) {
                    executor.shutdownNow();
                }
            });

    private final String cacheName;
    private volatile TerracottaStore tcStore;
    private final RotatingSnapshotFile rotatingWriter;
    private final Thread thread;

    private volatile Runnable onSnapshot;
    private final ScheduledFuture<?> scheduledFuture;

    /**
     * Default Constructor
     *
     * @param cache                          the Terracotta clustered Cache to snapshot
     * @param interval                       the interval to do the snapshots on
     * @param doKeySnapshotOnDedicatedThread whether the snapshots have to be done on a dedicated thread
     * @param rotatingWriter                 the RotatingSnapshotFile to write to
     * @throws IllegalArgumentException if interval is less than or equal to zero
     */
    KeySnapshotter(final Ehcache cache, final long interval,
                   final boolean doKeySnapshotOnDedicatedThread,
                   final RotatingSnapshotFile rotatingWriter)
        throws IllegalArgumentException {
        final Store store = new CacheStoreHelper((Cache)cache).getStore();
        if (!(store instanceof TerracottaStore)) {
            throw new IllegalArgumentException("Cache '" + cache.getName() + "' isn't backed by a " + TerracottaStore.class.getSimpleName()
                                               + " but uses a " + store.getClass().getName() + " instead");
        }

        if (interval <= 0) {
            throw new IllegalArgumentException("Interval needs to be a positive & non-zero value");
        }

        if (rotatingWriter == null) {
            throw new NullPointerException();
        }

        this.cacheName = cache.getName();
        this.rotatingWriter = rotatingWriter;
        this.tcStore = (TerracottaStore)store;

        if (doKeySnapshotOnDedicatedThread) {
            scheduledFuture = null;
            thread = new SnapShottingThread(this, interval, "KeySnapshotter for cache " + cacheName);
            thread.start();
        } else {
            scheduledFuture = getScheduledExecutorService(cache.getCacheManager())
                .scheduleWithFixedDelay(this, interval, interval, TimeUnit.SECONDS);
            thread = null;
        }
    }

    private ScheduledExecutorService getScheduledExecutorService(final CacheManager cacheManager) {
        ScheduledExecutorService scheduledExecutorService = INSTANCES.get(cacheManager);
        if (scheduledExecutorService == null) {
            scheduledExecutorService = new ScheduledThreadPoolExecutor(POOL_SIZE);
            final ScheduledExecutorService previous = INSTANCES.putIfAbsent(cacheManager, scheduledExecutorService);
            if (previous != null) {
                scheduledExecutorService.shutdownNow();
                scheduledExecutorService = previous;
            }
        }
        return scheduledExecutorService;
    }

    /**
     * Shuts down the writer thread and cleans up resources
     *
     * @param immediately whether to leave the writer finish or shut down immediately
     */
    void dispose(boolean immediately) {
        if (thread != null) {
            rotatingWriter.setShutdownOnThreadInterrupted(immediately);
            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            scheduledFuture.cancel(immediately);
        }
        tcStore = null;
    }

    /**
     * {@inheritDoc}
     */
    public void run() {
        try {
            INSTANCES.cleanUp();
            rotatingWriter.writeAll(tcStore.getLocalKeys());
            onSnapshot();
        } catch (Throwable e) {
            LOG.error("Couldn't snapshot local keySet for Cache {}", cacheName, e);
        }
    }

    private void onSnapshot() {
        if (onSnapshot != null) {
            try {
                onSnapshot.run();
            } catch (Exception e) {
                LOG.warn("Error occurred in onSnapshot callback", e);
            }
        }
    }

    /**
     * Accessor to all known cacheManagers (which are also bound to a ScheduledExecutorService)
     *
     * @return the collection of known CacheManagers
     */
    static Collection<CacheManager> getKnownCacheManagers() {
        return INSTANCES.keySet();
    }

    /**
     * Calling this method will result in a snapshot being taken or wait for the one in progress to finish
     *
     * @throws IOException On exception being thrown while doing the snapshot
     */
    void doSnapshot() throws IOException {
        rotatingWriter.snapshotNowOrWaitForCurrentToFinish(tcStore.getLocalKeys());
        onSnapshot();
    }

    /**
     * Let register a Runnable that will be called on every snapshot happening
     * @param onSnapshot the runnable
     */
    void setOnSnapshot(final Runnable onSnapshot) {
        this.onSnapshot = onSnapshot;
    }

    /**
     * Returns the name of the underlying cache for which this snapshots
     * @return The name of the cache
     */
    public String getCacheName() {
        return cacheName;
    }

    /**
     * Thread doing background snapshots of the local key set
     */
    private static class SnapShottingThread extends Thread {

        private long lastRun;
        private final long interval;

        public SnapShottingThread(final Runnable runnable, final long interval, final String threadName) {
            super(runnable, threadName);
            this.interval = interval;
            lastRun = System.currentTimeMillis();
            this.setDaemon(true);
        }

        @Override
        public void run() {
            while (!isInterrupted()) {
                final long nextTime = lastRun + TimeUnit.SECONDS.toMillis(interval);
                final long now = System.currentTimeMillis();
                if (nextTime <= now) {
                    super.run();
                    lastRun = System.currentTimeMillis();
                } else {
                    try {
                        sleep(nextTime - now);
                    } catch (InterruptedException e) {
                        interrupt();
                    }
                }
            }
        }
    }
}

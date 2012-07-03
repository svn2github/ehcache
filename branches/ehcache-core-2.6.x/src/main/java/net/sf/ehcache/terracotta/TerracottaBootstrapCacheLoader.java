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

import java.io.IOException;
import java.util.Set;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.DiskStorePathManager;
import net.sf.ehcache.Disposable;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Status;
import net.sf.ehcache.distribution.RemoteCacheException;
import net.sf.ehcache.store.MemoryLimitedCacheLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link net.sf.ehcache.bootstrap.BootstrapCacheLoader} that will load Elements into a Terracotta clustered cache, based on a previously
 * snapshotted key set. It is also responsible to create snapshot files to disk
 *
 * @author Alex Snaps
 */
public class TerracottaBootstrapCacheLoader extends MemoryLimitedCacheLoader implements Disposable {

    /**
     * The default interval in seconds, between each snapshot
     */
    public static final long DEFAULT_INTERVAL = 10 * 60;
    /**
     * The default on whether to do the snapshot on a dedicated thread or using the CacheManager's
     * {@link java.util.concurrent.ScheduledExecutorService}
     */
    public static final boolean DEFAULT_DEDICATED_THREAD = false;

    private static final Logger LOG = LoggerFactory.getLogger(TerracottaBootstrapCacheLoader.class);

    private final boolean aSynchronous;
    private final boolean doKeySnapshot;
    private final boolean doKeySnapshotOnDedicatedThread;
    private final long interval;
    private final DiskStorePathManager diskStorePathManager;

    private volatile KeySnapshotter keySnapshotter;
    private volatile boolean immediateShutdown;
    private volatile boolean doKeySnapshotOnDispose;

    private TerracottaBootstrapCacheLoader(final boolean doKeySnapshot, final boolean aSynchronous, final String directory,
            final long interval, final boolean doKeySnapshotOnDedicatedThread) {
        this.aSynchronous = aSynchronous;
        this.doKeySnapshot = doKeySnapshot;
        this.doKeySnapshotOnDedicatedThread = doKeySnapshotOnDedicatedThread;
        this.interval = interval;
        this.diskStorePathManager = directory != null ? new DiskStorePathManager(directory) : null;
    }

    /**
     * Constructor
     *
     * @param asynchronous do the loading asynchronously, or synchronously
     * @param directory the directory to read snapshot files from, and write them to
     * @param doKeySnapshots Whether to do keysnapshotting
     */
    public TerracottaBootstrapCacheLoader(final boolean asynchronous, String directory, boolean doKeySnapshots) {
        this(doKeySnapshots, asynchronous, directory, DEFAULT_INTERVAL, DEFAULT_DEDICATED_THREAD);
    }

    /**
     * Constructor
     *
     * @param asynchronous do the loading asynchronously, or synchronously
     * @param directory the directory to read snapshot files from, and write them to
     * @param interval the interval in seconds at which the snapshots of the local key set has to occur
     */
    public TerracottaBootstrapCacheLoader(final boolean asynchronous, String directory, long interval) {
        this(asynchronous, directory, interval, false);
    }

    /**
     * Constructor
     *
     * @param asynchronous do the loading asynchronously, or synchronously
     * @param directory the directory to read snapshot files from, and write them to
     * @param interval the interval in seconds at which the snapshots of the local key set has to occur
     * @param onDedicatedThread whether to do the snapshot on a dedicated thread or using the CacheManager's
     *            {@link java.util.concurrent.ScheduledExecutorService ScheduledExecutorService}
     */
    public TerracottaBootstrapCacheLoader(final boolean asynchronous, String directory, long interval, boolean onDedicatedThread) {
        this(true, asynchronous, directory, interval, onDedicatedThread);
    }

    /**
     * Whether the on going keysnapshot will finish before the instance is disposed
     *
     * @return true if disposable is immediate
     * @see Disposable
     */
    public boolean isImmediateShutdown() {
        return immediateShutdown;
    }

    /**
     * Sets whether the disposal of the instance will let the potential current key set being written to disk finish, or whether the
     * shutdown will be immediate
     *
     * @param immediateShutdown true if immediate, false to let the snapshot finish
     */
    public void setImmediateShutdown(final boolean immediateShutdown) {
        this.immediateShutdown = immediateShutdown;
    }

    /**
     * {@inheritDoc}
     */
    public void load(final Ehcache cache) throws CacheException {
        if (!cache.getCacheConfiguration().isTerracottaClustered()) {
            LOG.error("You're trying to bootstrap a non Terracotta clustered cache with a TerracottaBootstrapCacheLoader! Cache "
                    + "'{}' will not be bootstrapped and no keySet snapshot will be recorded...", cache.getName());
            return;
        }

        if (cache.getStatus() != Status.STATUS_ALIVE) {
            throw new CacheException("Cache '" + cache.getName() + "' isn't alive yet: " + cache.getStatus());
        }

        if (isAsynchronous()) {
            BootstrapThread thread = new BootstrapThread(cache);
            thread.start();
        } else {
            doLoad(cache);
        }
    }

    private void doLoad(final Ehcache cache) {
        CacheManager manager = cache.getCacheManager();
        if (manager == null) {
            throw new CacheException("Cache must belong to a cache manager to bootstrap");
        }

        DiskStorePathManager pathManager = diskStorePathManager != null ? diskStorePathManager : cache.getCacheManager()
                .getDiskStorePathManager();

        final RotatingSnapshotFile snapshotFile = new RotatingSnapshotFile(pathManager, cache.getName());
        try {
            final Set<Object> keys = snapshotFile.readAll();
            int loaded = 0;
            for (Object key : keys) {
                if (isInMemoryLimitReached(cache, loaded)) {
                    break;
                }
                cache.get(key);
                loaded++;
            }
            LOG.info("Finished loading {} keys (of {} on disk) from previous snapshot for Cache '{}'",
                    new Object[] {Integer.valueOf(loaded), keys.size(), cache.getName()});
        } catch (IOException e) {
            LOG.error("Couldn't load keySet for Cache '{}'", cache.getName(), e);
        }

        if (doKeySnapshot) {
            keySnapshotter = new KeySnapshotter(cache, interval, doKeySnapshotOnDedicatedThread, snapshotFile);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAsynchronous() {
        return aSynchronous;
    }

    /**
     * Will shut the keysnapshot thread and other resources down.
     * If a snapshot is currently in progress, the method will either shutdown immediately or let the snapshot finish
     * based on the configured {@link #setImmediateShutdown(boolean)} value
     */
    public void dispose() {
        if (keySnapshotter != null) {
            if (doKeySnapshotOnDispose) {
                try {
                    keySnapshotter.doSnapshot();
                } catch (IOException e) {
                    LOG.error("Error writing local key set for Cache '{}'", keySnapshotter.getCacheName(), e);
                }
            } else {
                keySnapshotter.dispose(immediateShutdown);
            }
        }
        if (diskStorePathManager != null) {
            diskStorePathManager.releaseLock();
        }
    }

    /**
     * Calling this method will result in a snapshot being taken or wait for the one in progress to finish
     *
     * @throws IOException On exception being thrown while doing the snapshot
     */
    public void doLocalKeySnapshot() throws IOException {
        keySnapshotter.doSnapshot();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * Accessor to the associated {@link KeySnapshotter}
     *
     * @return the {@link KeySnapshotter} used by this loader instance
     */
    KeySnapshotter getKeySnapshotter() {
        return keySnapshotter;
    }

    /**
     * Configures the Loader to take a snapshot when it is being disposed
     *
     * @param doKeySnapshotOnDispose whether to snapshot on loader disposal
     */
    public void setSnapshotOnDispose(final boolean doKeySnapshotOnDispose) {
        this.doKeySnapshotOnDispose = doKeySnapshotOnDispose;
    }

    /**
     * A background daemon thread that asynchronously calls doLoad
     */
    private final class BootstrapThread extends Thread {
        private Ehcache cache;

        public BootstrapThread(Ehcache cache) {
            super("Bootstrap Thread for cache " + cache.getName());
            this.cache = cache;
            setDaemon(true);
            setPriority(Thread.NORM_PRIORITY);
        }

        /**
         * RemoteDebugger thread method.
         */
        @Override
        public final void run() {
            try {
                doLoad(cache);
            } catch (RemoteCacheException e) {
                LOG.warn("Error asynchronously performing bootstrap. The cause was: " + e.getMessage(), e);
            }
            cache = null;
        }
    }
}

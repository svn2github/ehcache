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

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.bootstrap.BootstrapCacheLoader;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads Elements from a random Cache Peer
 *
 * @author Greg Luck
 * @version $Id$
 */
public class RMIBootstrapCacheLoader implements BootstrapCacheLoader, Cloneable {

    private static final int ONE_SECOND = 1000;

    private static final Logger LOG = LoggerFactory.getLogger(RMIBootstrapCacheLoader.class.getName());

    /**
     * Whether to load asynchronously
     */
    protected boolean asynchronous;

    /**
     * The maximum serialized size of the elements to request from a remote cache peer during bootstrap.
     */
    protected int maximumChunkSizeBytes;

    /**
     * Creates a boostrap cache loader that will work with RMI based distribution
     *
     * @param asynchronous Whether to load asynchronously
     */
    public RMIBootstrapCacheLoader(boolean asynchronous, int maximumChunkSize) {
        this.asynchronous = asynchronous;
        this.maximumChunkSizeBytes = maximumChunkSize;
    }


    /**
     * Bootstraps the cache from a random CachePeer. Requests are done in chunks estimated at 5MB Serializable
     * size. This balances memory use on each end and network performance.
     *
     * @throws RemoteCacheException
     *          if anything goes wrong with the remote call
     */
    public void load(Ehcache cache) throws RemoteCacheException {
        if (asynchronous) {
            BootstrapThread bootstrapThread = new BootstrapThread(cache);
            bootstrapThread.start();
        } else {
            doLoad(cache);
        }
    }

    /**
     * @return true if this bootstrap loader is asynchronous
     */
    public boolean isAsynchronous() {
        return asynchronous;
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
        public final void run() {
            try {
                doLoad(cache);
            } catch (RemoteCacheException e) {
                LOG.warn("Error asynchronously performing bootstrap. The cause was: " + e.getMessage(), e);
            } finally {
                cache = null;
            }

        }

    }


    /**
     * Bootstraps the cache from a random CachePeer. Requests are done in chunks estimated at 5MB Serializable
     * size. This balances memory use on each end and network performance.
     * <p/>
     * Bootstrapping requires the establishment of a cluster. This can be instantaneous for manually configued
     * clusters or may take a number of seconds for multicast ones. This method waits up to 11 seconds for a cluster
     * to form.
     *
     * @throws RemoteCacheException
     *          if anything goes wrong with the remote call
     */
    public void doLoad(Ehcache cache) throws RemoteCacheException {

        List cachePeers = acquireCachePeers(cache);
        if (cachePeers == null || cachePeers.size() == 0) {
            LOG.debug("Empty list of cache peers for cache " + cache.getName() + ". No cache peer to bootstrap from.");
            return;
        }
        Random random = new Random();
        int randomPeerNumber = random.nextInt(cachePeers.size());
        CachePeer cachePeer = (CachePeer) cachePeers.get(randomPeerNumber);
        LOG.debug("Bootstrapping " + cache.getName() + " from " + cachePeer);

        try {

            //Estimate element size
            Element sampleElement = null;
            List keys = cachePeer.getKeys();
            for (int i = 0; i < keys.size(); i++) {
                Serializable key = (Serializable) keys.get(i);
                sampleElement = cachePeer.getQuiet(key);
                if (sampleElement != null && sampleElement.getSerializedSize() != 0) {
                    break;
                }
            }
            if (sampleElement == null) {
                LOG.debug("All cache peer elements were either null or empty. Nothing to bootstrap from. Cache was "
                        + cache.getName() + ". Cache peer was " + cachePeer);
                return;
            }
            long size = sampleElement.getSerializedSize();
            int chunkSize = (int) (maximumChunkSizeBytes / size);

            List requestChunk = new ArrayList();
            for (int i = 0; i < keys.size(); i++) {
                Serializable serializable = (Serializable) keys.get(i);
                requestChunk.add(serializable);
                if (requestChunk.size() == chunkSize) {
                    fetchAndPutElements(cache, requestChunk, cachePeer);
                    requestChunk.clear();
                }
            }
            //get leftovers
            fetchAndPutElements(cache, requestChunk, cachePeer);
            LOG.debug("Bootstrap of " + cache.getName() + " from " + cachePeer + " finished. "
                    + keys.size() + " keys requested.");
        } catch (Throwable t) {
            throw new RemoteCacheException("Error bootstrapping from remote peer. Message was: " + t.getMessage(), t);
        }
    }

    /**
     * Acquires the cache peers for this cache.
     *
     * @param cache
     */
    protected List acquireCachePeers(Ehcache cache) {

        long timeForClusterToForm = 0;
        CacheManagerPeerProvider cacheManagerPeerProvider = cache.getCacheManager().getCacheManagerPeerProvider("RMI");
        if (cacheManagerPeerProvider != null) {
            timeForClusterToForm = cacheManagerPeerProvider.getTimeForClusterToForm();
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Attempting to acquire cache peers for cache " + cache.getName()
                    + " to bootstrap from. Will wait up to " + timeForClusterToForm + "ms for cache to join cluster.");
        }
        List cachePeers = null;
        for (int i = 0; i <= timeForClusterToForm; i = i + ONE_SECOND) {
            cachePeers = listRemoteCachePeers(cache);
            if (cachePeers == null) {
                break;
            }
            if (cachePeers.size() > 0) {
                break;
            }
            try {
                Thread.sleep(ONE_SECOND);
            } catch (InterruptedException e) {
                LOG.debug("doLoad for " + cache.getName() + " interrupted.");
            }
        }

            LOG.debug("cache peers: {}", cachePeers);
        return cachePeers;
    }

    /**
     * Fetches a chunk of elements from a remote cache peer
     *
     * @param cache        the cache to put elements in
     * @param requestChunk the chunk of keys to request
     * @param cachePeer    the peer to fetch from
     * @throws java.rmi.RemoteException
     */
    protected void fetchAndPutElements(Ehcache cache, List requestChunk, CachePeer cachePeer) throws RemoteException {
        List receivedChunk = cachePeer.getElements(requestChunk);
        for (int i = 0; i < receivedChunk.size(); i++) {
            Element element = (Element) receivedChunk.get(i);
            // element could be expired at the peer
            if (element != null) {
                cache.put(element, true);
            }
        }
    }

    /**
     * Package protected List of cache peers
     *
     * @param cache
     */
    protected List listRemoteCachePeers(Ehcache cache) {
        CacheManagerPeerProvider provider = cache.getCacheManager().getCacheManagerPeerProvider("RMI");
        if (provider == null) {
            return null;
        } else {
            return provider.listRemoteCachePeers(cache);
        }

    }

    /**
     * Gets the maximum chunk size
     */
    public int getMaximumChunkSizeBytes() {
        return maximumChunkSizeBytes;
    }

    /**
     * Clones this loader
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        //checkstyle
        return new RMIBootstrapCacheLoader(asynchronous, maximumChunkSizeBytes);
    }

}

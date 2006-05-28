/**
 *  Copyright 2003-2006 Greg Luck
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Loads Elements from a random Cache Peer
 * @author Greg Luck
 * @version $Id$
 */
public class RMIBootstrapCacheLoader implements BootstrapCacheLoader {

    private static final Log LOG = LogFactory.getLog(RMIBootstrapCacheLoader.class.getName());

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
     * @throws net.sf.ehcache.distribution.RemoteCacheException if anything goes wrong with the remote call
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
            super("Bootstrap Thread");
            this.cache = cache;
            setDaemon(true);
            setPriority(2);
        }

        /**
         * Main thread method.
         */
        public final void run() {
            try {
                doLoad(cache);
            } catch (RemoteCacheException e) {
                LOG.debug("Error asynchronously performing bootstrap. The cause was: " + e.getMessage());
            } finally {
                cache = null;
            }

        }

    }



    /**
     * Bootstraps the cache from a random CachePeer. Requests are done in chunks estimated at 5MB Serializable
     * size. This balances memory use on each end and network performance.
     *
     * @throws net.sf.ehcache.distribution.RemoteCacheException if anything goes wrong with the remote call
     */
    public void doLoad(Ehcache cache) throws RemoteCacheException {
        List cachePeers = listRemoteCachePeers(cache);
        if (cachePeers == null || cachePeers.size() == 0) {
            LOG.info("Empty list of cache peers. No cache peer to bootstrap from.");
            return;
        }
        Random random = new Random();
        int randomPeerNumber = random.nextInt(cachePeers.size());
        CachePeer cachePeer = (CachePeer) cachePeers.get(randomPeerNumber);
        LOG.info("Bootstrapping from " + cachePeer);

        try {

            List keys = cachePeer.getKeys();

            //todo first element may be null
            Element firstElement = cachePeer.getQuiet((Serializable) keys.get(0));
            long size = firstElement.getSerializedSize();
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
            LOG.info("Bootstrapping from " + cachePeer + " finished.");

        } catch (Throwable t) {
            throw new RemoteCacheException("Error bootstrapping from remote peer. Message was: " + t.getMessage(), t);
        }
    }

    /**
     * Fetches a chunk of elements from a remote cache peer
     * @param cache the cache to put elements in
     * @param requestChunk the chunk of keys to request
     * @param cachePeer the peer to fetch from
     * @throws java.rmi.RemoteException
     */
    protected void fetchAndPutElements(Ehcache cache, List requestChunk, CachePeer cachePeer) throws RemoteException {
        List receivedChunk = cachePeer.getElements(requestChunk);
        for (int i = 0; i < receivedChunk.size(); i++) {
            Element element = (Element) receivedChunk.get(i);
            cache.put(element, true);
        }
    }

    /**
     * Package protected List of cache peers
     *
     * @param cache
     */
    protected List listRemoteCachePeers(Ehcache cache) {
        CacheManagerPeerProvider provider = cache.getCacheManager().getCachePeerProvider();
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

}

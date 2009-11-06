/**
 *  Copyright 2003-2009 Terracotta, Inc.
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


package net.sf.ehcache.distribution.jgroups;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.bootstrap.BootstrapCacheLoader;
import net.sf.ehcache.distribution.CacheManagerPeerProvider;
import net.sf.ehcache.distribution.CachePeer;
import net.sf.ehcache.distribution.RemoteCacheException;

import org.jgroups.stack.IpAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads Elements from a random Cache Peer
 *
 * @author Greg Luck
 * @version $Id$
 */
public class JGroupsBootstrapCacheLoader implements BootstrapCacheLoader {

    private static final int ONE_SECOND = 1000;

    private static final Logger LOG = LoggerFactory.getLogger(JGroupsBootstrapCacheLoader.class.getName());

    private static final int WAIT_FOR_RESPONSE = 3000;

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
    public JGroupsBootstrapCacheLoader(boolean asynchronous, int maximumChunkSize) {
        this.asynchronous = asynchronous;
        this.maximumChunkSizeBytes = maximumChunkSize;
    }

    /**
     * Bootstraps the cache from a random CachePeer. Requests are done in chunks estimated at 5MB Serializable size.
     * This balances memory use on each end and network performance.
     *
     * @throws RemoteCacheException if anything goes wrong with the remote call
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
         * thread method.
         */
        public final void run() {
            try {
                doLoad(cache);
            } catch (RemoteCacheException e) {
                LOG.warn("Error asynchronously performing bootstrap. The cause was: {}", e.getMessage(), e);
            } finally {
                cache = null;
            }

        }

    }

    /**
     * Bootstraps the cache from a random CachePeer. Requests are done in chunks estimated at 5MB Serializable size.
     * This balances memory use on each end and network performance.
     * <p/>
     * Bootstrapping requires the establishment of a cluster. This can be instantaneous for manually configued clusters
     * or may take a number of seconds for multicast ones. This method waits up to 11 seconds for a cluster to form.
     *
     * @throws RemoteCacheException if anything goes wrong with the remote call
     */
    public void doLoad(Ehcache cache) throws RemoteCacheException {
        JGroupManager jGroupManager = null;

        List cachePeers = acquireCachePeers(cache);
        if (cachePeers == null || cachePeers.size() == 0) {
            LOG.info("Empty list of cache peers for cache {}. No cache peer to bootstrap from.", cache.getName());
            return;
        }


        jGroupManager = (JGroupManager) cachePeers.get(0);
        IpAddress localAddress = (IpAddress) jGroupManager.getBusLocalAddress();
        if (LOG.isDebugEnabled()) {
            LOG.debug("({}) localAddress: {}", cache.getName(), localAddress);
        }
        List<IpAddress> addresses = buildCachePeerAddressList(cache, jGroupManager, localAddress);


        if (addresses == null || addresses.size() == 0) {
            LOG.info("This is the first node to start: no cache bootstrap for {}", cache.getName());
            return;
        }

        IpAddress address = null;
        Random random = new Random();

        while (addresses.size() > 0 && (address == null || cache.getSize() == 0)) {
            int randomPeerNumber = random.nextInt(addresses.size());
            address = addresses.get(randomPeerNumber);
            addresses.remove(randomPeerNumber);
            JGroupEventMessage event =
                    new JGroupEventMessage(JGroupEventMessage.ASK_FOR_BOOTSTRAP, localAddress, null, cache, cache.getName());
            if (LOG.isDebugEnabled()) {
                LOG.debug("contact {} to boot cache {}", address, cache.getName());
            }
            List events = new ArrayList();
            events.add(event);
            try {
                jGroupManager.send(address, events);
                try {
                    Thread.sleep(WAIT_FOR_RESPONSE);
                } catch (InterruptedException e) {
                    LOG.error("InterruptedException", e);
                }
            } catch (RemoteException e1) {
                LOG.error("error calling {}", address, e1);
            }
        }

        if (cache.getSize() == 0) {
            LOG.warn("Cache failed to bootstrap from its peers: " + cache.getName());
        } else {
            LOG.info("Bootstrap for cache {} has loaded {} elements", cache.getName(), cache.getSize());
        }


    }

    private List<IpAddress> buildCachePeerAddressList(Ehcache cache, JGroupManager jGroupManager, IpAddress localAddress) {
        List members = jGroupManager.getBusMembership();
        List<IpAddress> addresses = new ArrayList<IpAddress>();
        for (int i = 0; i < members.size(); i++) {
            IpAddress member = (IpAddress) members.get(i);
            if (LOG.isDebugEnabled()) {
                LOG.debug("({}) member {}: {}{}",
                        new Object[] {cache.getName(), i, member.getIpAddress(), member.equals(localAddress) ? " ***" : "" });
            }
            if (!member.equals(localAddress)) {
                addresses.add(member);
            }
        }
        return addresses;
    }

    /**
     * Acquires the cache peers for this cache.
     *
     * @param cache
     */
    protected List acquireCachePeers(Ehcache cache) {

        long timeForClusterToForm = 0;
        CacheManagerPeerProvider cacheManagerPeerProvider = cache.getCacheManager().getCacheManagerPeerProvider("JGroups");
        if (cacheManagerPeerProvider != null) {
            timeForClusterToForm = cacheManagerPeerProvider.getTimeForClusterToForm();
        }
        if (LOG.isInfoEnabled()) {
            LOG.info("Attempting to acquire cache peers for cache {}"
                    + " to bootstrap from. Will wait up to "
                    + "{}ms for cache to join cluster.", cache.getName(), timeForClusterToForm);
        }
        List cachePeers = null;
        for (int i = 0; i <= timeForClusterToForm; i = i + ONE_SECOND) {
            cachePeers = listRemoteCachePeers(cache);
            /*
             * if (cachePeers == null) { break; } if (cachePeers.size() > 0) { break; }
             */
            LOG.info("waiting...");
            try {
                Thread.sleep(ONE_SECOND);
            } catch (InterruptedException e) {
                LOG.info("doLoad for {} interrupted.", cache.getName());
            }
        }
        if (LOG.isInfoEnabled()) {
            LOG.info("cache peers: {}", cachePeers.size());
        }
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
        CacheManagerPeerProvider provider = cache.getCacheManager().getCacheManagerPeerProvider("JGroups");
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
    public Object clone() throws CloneNotSupportedException {
        // checkstyle
        return new JGroupsBootstrapCacheLoader(asynchronous, maximumChunkSizeBytes);
    }

}

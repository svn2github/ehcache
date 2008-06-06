/**
 *  Copyright 2003-2007 Luck Consulting Pty Ltd
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

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * A peer provider which discovers peers using Multicast.
 * <p/>
 * Hosts can be in three different levels of conformance with the Multicast specification (RFC1112), according to the requirements they meet.
 * <ol>
 * <li>Level 0 is the "no support for IP Multicasting" level. Lots of hosts and routers in the Internet are in this state,
 * as multicast support is not mandatory in IPv4 (it is, however, in IPv6).
 * Not too much explanation is needed here: hosts in this level can neither send nor receive multicast packets.
 * They must ignore the ones sent by other multicast capable hosts.
 * <li>Level 1 is the "support for sending but not receiving multicast IP datagrams" level.
 * Thus, note that it is not necessary to join a multicast group to be able to send datagrams to it.
 * Very few additions are needed in the IP module to make a "Level 0" host "Level 1-compliant".
 * <li>Level 2 is the "full support for IP multicasting" level.
 * Level 2 hosts must be able to both send and receive multicast traffic.
 * They must know the way to join and leave multicast groups and to propagate this information to multicast routers.
 * Thus, they must include an Internet Group Management Protocol (IGMP) implementation in their TCP/IP stack.
 * </ol>
 * <p/>
 * The list of CachePeers is maintained via heartbeats. rmiUrls are looked up using RMI and converted to CachePeers on
 * registration. On lookup any stale references are removed.
 *
 * @author Greg Luck
 * @version $Id$
 */
public final class MulticastRMICacheManagerPeerProvider extends RMICacheManagerPeerProvider implements CacheManagerPeerProvider {

    /**
     * One tenth of a second, in ms
     */
    protected static final int SHORT_DELAY = 100;

    private static final Log LOG = LogFactory.getLog(MulticastRMICacheManagerPeerProvider.class.getName());


    private final MulticastKeepaliveHeartbeatReceiver heartBeatReceiver;
    private final MulticastKeepaliveHeartbeatSender heartBeatSender;

    /**
     * Creates and starts a multicast peer provider
     *
     * @param groupMulticastAddress 224.0.0.1 to 239.255.255.255 e.g. 230.0.0.1
     * @param groupMulticastPort    1025 to 65536 e.g. 4446
     */
    public MulticastRMICacheManagerPeerProvider(CacheManager cacheManager, InetAddress groupMulticastAddress,
                                                Integer groupMulticastPort, Integer timeToLive) {
        super(cacheManager);
        heartBeatReceiver = new MulticastKeepaliveHeartbeatReceiver(this, groupMulticastAddress, groupMulticastPort);
        heartBeatSender =
                new MulticastKeepaliveHeartbeatSender(cacheManager, groupMulticastAddress, groupMulticastPort, timeToLive);
    }

    /**
     * {@inheritDoc}
     */
    public final void init() throws CacheException {
        try {
            heartBeatReceiver.init();
            heartBeatSender.init();
        } catch (IOException exception) {
            LOG.error("Error starting heartbeat. Error was: " + exception.getMessage(), exception);
            throw new CacheException(exception.getMessage());
        }
    }

    /**
     * Register a new peer, but only if the peer is new, otherwise the last seen timestamp is updated.
     * <p/>
     * This method is thread-safe. It relies on peerUrls being a synchronizedMap
     *
     * @param rmiUrl
     */
    public final void registerPeer(String rmiUrl) {
        try {
            CachePeerEntry cachePeerEntry = (CachePeerEntry) peerUrls.get(rmiUrl);
            if (cachePeerEntry == null || stale(cachePeerEntry.date)) {
                //can take seconds if there is a problem
                CachePeer cachePeer = lookupRemoteCachePeer(rmiUrl);
                cachePeerEntry = new CachePeerEntry(cachePeer, new Date());
                //synchronized due to peerUrls being a synchronizedMap
                peerUrls.put(rmiUrl, cachePeerEntry);
            } else {
                cachePeerEntry.date = new Date();
            }
        } catch (IOException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Unable to lookup remote cache peer for " + rmiUrl + ". Removing from peer list. Cause was: "
                        + e.getMessage());
            }
            unregisterPeer(rmiUrl);
        } catch (NotBoundException e) {
            peerUrls.remove(rmiUrl);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Unable to lookup remote cache peer for " + rmiUrl + ". Removing from peer list. Cause was: "
                        + e.getMessage());
            }
        } catch (Throwable t) {
            LOG.error("Unable to lookup remote cache peer for " + rmiUrl
                    + ". Cause was not due to an IOException or NotBoundException which will occur in normal operation:" +
                    " " + t.getMessage());
        }
    }

    /**
     * @return a list of {@link CachePeer} peers, excluding the local peer.
     */
    public final synchronized List listRemoteCachePeers(Ehcache cache) throws CacheException {
        List remoteCachePeers = new ArrayList();
        List staleList = new ArrayList();
        synchronized (peerUrls) {
            for (Iterator iterator = peerUrls.keySet().iterator(); iterator.hasNext();) {
                String rmiUrl = (String) iterator.next();
                String rmiUrlCacheName = extractCacheName(rmiUrl);
                try {
                    if (!rmiUrlCacheName.equals(cache.getName())) {
                        continue;
                    }
                    CachePeerEntry cachePeerEntry = (CachePeerEntry) peerUrls.get(rmiUrl);
                    Date date = cachePeerEntry.date;
                    if (!stale(date)) {
                        CachePeer cachePeer = cachePeerEntry.cachePeer;
                        remoteCachePeers.add(cachePeer);
                    } else {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("rmiUrl " + rmiUrl + " is stale. Either the remote peer is shutdown or the " +
                                    "network connectivity has been interrupted. Will be removed from list of remote cache peers");
                        }
                        staleList.add(rmiUrl);
                    }
                } catch (Exception exception) {
                    LOG.error(exception.getMessage(), exception);
                    throw new CacheException("Unable to list remote cache peers. Error was " + exception.getMessage());
                }
            }
            //Must remove entries after we have finished iterating over them
            for (int i = 0; i < staleList.size(); i++) {
                String rmiUrl = (String) staleList.get(i);
                peerUrls.remove(rmiUrl);
            }
        }
        return remoteCachePeers;
    }


    /**
     * Shutdown the heartbeat
     */
    public final void dispose() {
        heartBeatSender.dispose();
        heartBeatReceiver.dispose();
    }

    /**
     * Time for a cluster to form. This varies considerably, depending on the implementation.
     *
     * @return the time in ms, for a cluster to form
     */
    public long getTimeForClusterToForm() {
        return getStaleTime();
    }

    /**
     * The time after which an unrefreshed peer provider entry is considered stale.
     */
    protected long getStaleTime() {
        return MulticastKeepaliveHeartbeatSender.getHeartBeatInterval() * 2 + SHORT_DELAY;
    }

    /**
     * Whether the entry should be considered stale.
     * This will depend on the type of RMICacheManagerPeerProvider.
     * This method should be overridden for implementations that go stale based on date
     *
     * @param date the date the entry was created
     * @return true if stale
     */
    protected final boolean stale(Date date) {
        long now = System.currentTimeMillis();
        return date.getTime() < (now - getStaleTime());
    }


    /**
     * Entry containing a looked up CachePeer and date
     */
    protected static final class CachePeerEntry {

        private final CachePeer cachePeer;
        private Date date;

        /**
         * Constructor
         *
         * @param cachePeer the cache peer part of this entry
         * @param date      the date part of this entry
         */
        public CachePeerEntry(CachePeer cachePeer, Date date) {
            this.cachePeer = cachePeer;
            this.date = date;
        }

        /**
         * @return the cache peer part of this entry
         */
        public final CachePeer getCachePeer() {
            return cachePeer;
        }


        /**
         * @return the date part of this entry
         */
        public final Date getDate() {
            return date;
        }

    }

    /**
     * @return the MulticastKeepaliveHeartbeatReceiver
     */
    public MulticastKeepaliveHeartbeatReceiver getHeartBeatReceiver() {
        return heartBeatReceiver;
    }

    /**
     * @return the MulticastKeepaliveHeartbeatSender
     */
    public MulticastKeepaliveHeartbeatSender getHeartBeatSender() {
        return heartBeatSender;
    }
}

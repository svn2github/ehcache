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

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Cache;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
 *
 * @author Greg Luck
 * @version $Id$
 */
public class MulticastRMICacheManagerPeerProvider extends RMICacheManagerPeerProvider implements CacheManagerPeerProvider {

    /**
     * The number of ms until the peer is considered to be offline. Once offline it will not be sent
     * notifications.
     */
    public static final int STALE_PEER_TIME_MS = 11000;

    private static final Log LOG = LogFactory.getLog(MulticastRMICacheManagerPeerProvider.class.getName());


    private MulticastKeepaliveHeartbeatReceiver heartBeatReceiver;
    private MulticastKeepaliveHeartbeatSender heartBeatSender;

    /**
     * Creates and starts a multicast peer provider
     *
     * @param groupMulticastAddress 224.0.0.1 to 239.255.255.255 e.g. 230.0.0.1
     * @param groupMulticastPort    1025 to 65536 e.g. 4446
     */
    public MulticastRMICacheManagerPeerProvider(CacheManager cacheManager, InetAddress groupMulticastAddress,
                                                Integer groupMulticastPort) {
        super(cacheManager);
        heartBeatReceiver = new MulticastKeepaliveHeartbeatReceiver(this, groupMulticastAddress, groupMulticastPort);
        heartBeatSender = new MulticastKeepaliveHeartbeatSender(cacheManager, groupMulticastAddress, groupMulticastPort);
    }

    /**
     * {@inheritDoc}
     */
    public void init() throws CacheException {
        try {
            heartBeatReceiver.init();
            heartBeatSender.init();
        } catch (IOException exception) {
            LOG.error("Error starting heartbeat. Error was: " + exception.getMessage(), exception);
            throw new CacheException(exception.getMessage());
        }
    }

    /**
     * Register a new peer
     *
     * @param rmiUrl
     */
    public synchronized void registerPeer(String rmiUrl) {
        CachePeer cachePeer = null;
        try {
            cachePeer = lookupRemoteCachePeer(rmiUrl);
            CachePeerEntry cachePeerEntry = new CachePeerEntry(cachePeer, new Date());
            peerUrls.put(rmiUrl, cachePeerEntry);
        } catch (Exception e) {
            LOG.error("Unable to lookup remote cache peer for " + rmiUrl + ". Cause was: " + e.getMessage());
        }
    }

    /**
     * @return a list of {@link CachePeer} peers, excluding the local peer.
     */
    public synchronized List listRemoteCachePeers(Cache cache) throws CacheException {
        List remoteCachePeers = new ArrayList();
        List staleList = new ArrayList();
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
                    if (LOG.isInfoEnabled()) {
                        LOG.info("rmiUrl " + rmiUrl + " is stale. Either the remote peer is shutdown or the " +
                                "network connectivity has been interrupted. Will be removed from list of remote cache peers");
                    }
                    staleList.add(rmiUrl);
                }
            } catch (Exception exception) {
                LOG.error(exception.getMessage(), exception);
                throw new CacheException("Unable to list remote cache peers. Error was " + exception.getMessage());
            }
        }

        //Remove any stale remote peers. Must be done here to avoid concurrent modification exception.
        for (int i = 0; i < staleList.size(); i++) {
            String rmiUrl = (String) staleList.get(i);
            peerUrls.remove(rmiUrl);
        }
        return remoteCachePeers;
    }


    /**
     * Shutdown the heartbeat
     */
    public void dispose() {
        heartBeatSender.dispose();
        heartBeatReceiver.dispose();
    }

    /**
     * Whether the entry should be considered stale.
     * This will depend on the type of RMICacheManagerPeerProvider.
     * This method should be overridden for implementations that go stale based on date
     *
     * @param date the date the entry was created
     * @return true if stale
     */
    protected boolean stale(Date date) {
        long now = System.currentTimeMillis();
        return date.getTime() < (now - STALE_PEER_TIME_MS);
    }


    /**
     * Entry containing a looked up CachePeer and date
     */
    protected class CachePeerEntry {

        private CachePeer cachePeer;
        private Date date;

        /**
         * Constructor
         * @param cachePeer the cache peer part of this entry
         * @param date the date part of this entry
         */
        public CachePeerEntry(CachePeer cachePeer, Date date) {
            this.cachePeer = cachePeer;
            this.date = date;
        }

        /**
         * @return the cache peer part of this entry
         */
        public CachePeer getCachePeer() {
            return cachePeer;
        }


        /**
         * @return the date part of this entry
         */
        public Date getDate() {
            return date;
        }

    }

}

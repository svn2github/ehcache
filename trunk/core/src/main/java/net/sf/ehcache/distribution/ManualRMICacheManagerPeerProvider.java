/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
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
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Status;


import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * A provider of Peer RMI addresses based off manual configuration.
 * <p/>
 * Because there is no monitoring of whether a peer is actually there, the list of peers is dynamically
 * looked up and verified each time a lookup request is made.
 * <p/>
 *
 * @author Greg Luck
 * @version $Id$
 */
public final class ManualRMICacheManagerPeerProvider extends RMICacheManagerPeerProvider {

    private static final Logger LOG = Logger.getLogger(ManualRMICacheManagerPeerProvider.class.getName());

    /**
     * Empty constructor.
     */
    public ManualRMICacheManagerPeerProvider() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    public final void init() {
        //nothing to do here
    }

    /**
     * Time for a cluster to form. This varies considerably, depending on the implementation.
     *
     * @return the time in ms, for a cluster to form
     */
    public long getTimeForClusterToForm() {
        return 0;
    }

    /**
     * Register a new peer.
     *
     * @param rmiUrl
     */
    public final synchronized void registerPeer(String rmiUrl) {
        peerUrls.put(rmiUrl, new Date());
    }


    /**
     * @return a list of {@link CachePeer} peers, excluding the local peer.
     */
    public final synchronized List listRemoteCachePeers(Ehcache cache) throws CacheException {
        List remoteCachePeers = new ArrayList();
        List staleList = new ArrayList();
        for (Iterator iterator = peerUrls.keySet().iterator(); iterator.hasNext();) {
            String rmiUrl = (String) iterator.next();
            String rmiUrlCacheName = extractCacheName(rmiUrl);

            if (!rmiUrlCacheName.equals(cache.getName())) {
                continue;
            }
            Date date = (Date) peerUrls.get(rmiUrl);
            if (!stale(date)) {
                CachePeer cachePeer = null;
                try {
                    cachePeer = lookupRemoteCachePeer(rmiUrl);
                    remoteCachePeers.add(cachePeer);
                } catch (Exception e) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.log(Level.FINE, "Looking up rmiUrl " + rmiUrl + " through exception " + e.getMessage()
                                + ". This may be normal if a node has gone offline. Or it may indicate network connectivity"
                                + " difficulties", e);
                    }
                }
            } else {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("rmiUrl " + rmiUrl + " should never be stale for a manually configured cluster.");
                }
                staleList.add(rmiUrl);
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
     * Whether the entry should be considered stale.
     * <p/>
     * Manual RMICacheManagerProviders use a static list of urls and are therefore never stale.
     *
     * @param date the date the entry was created
     * @return true if stale
     */
    protected final boolean stale(Date date) {
        return false;
    }

}

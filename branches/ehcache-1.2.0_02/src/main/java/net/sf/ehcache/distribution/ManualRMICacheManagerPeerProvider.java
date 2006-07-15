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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * A provider of Peer RMI addresses based off manual configuration.
 * <p/>
 * Because there is no monitoring of whether a peer is actually there, the list of peers is dynamically
 * looked up and verified each time a lookup request is made.
 * <p/>
 * @author Greg Luck
 * @version $Id$
 */
public final class ManualRMICacheManagerPeerProvider extends RMICacheManagerPeerProvider {

    private static final Log LOG = LogFactory.getLog(ManualRMICacheManagerPeerProvider.class.getName());

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
    public final synchronized List listRemoteCachePeers(Cache cache) throws CacheException {
        List remoteCachePeers = new ArrayList();
        List staleList = new ArrayList();
        for (Iterator iterator = peerUrls.keySet().iterator(); iterator.hasNext();) {
            String rmiUrl = (String) iterator.next();
            String rmiUrlCacheName = extractCacheName(rmiUrl);
            try {
                if (!rmiUrlCacheName.equals(cache.getName())) {
                    continue;
                }
                Date date = (Date) peerUrls.get(rmiUrl);
                if (!stale(date)) {
                    CachePeer cachePeer = lookupRemoteCachePeer(rmiUrl);
                    remoteCachePeers.add(cachePeer);
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("rmiUrl " + rmiUrl + " is stale. Either the remote peer is shutdown or the " +
                                "network connectivity has been interrupted. Will be removed from list of remote cache peers");
                    }
                    staleList.add(rmiUrl);
                }
            } catch (NotBoundException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("No cache peer bound to URL at " + rmiUrl
                            + ". It must have disappeared since the last heartbeat");
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

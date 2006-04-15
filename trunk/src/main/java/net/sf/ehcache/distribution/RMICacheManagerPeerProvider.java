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
import net.sf.ehcache.CacheManager;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A provider of Peer RMI addresses.
 *
 * @author Greg Luck
 * @version $Id$
 */
public class RMICacheManagerPeerProvider implements CacheManagerPeerProvider {

    private static final Log LOG = LogFactory.getLog(RMICacheManagerPeerProvider.class.getName());

    /**
     * Contains a RMI URLs of the form: "//" + hostName + ":" + port + "/" + cacheName;
     */
    protected Map peerUrls = Collections.synchronizedMap(new HashMap());
    private CacheManager cacheManager;

    /**
     * Empty constructor
     */
    public RMICacheManagerPeerProvider() {
        //nothing to do
    }


    /**
     * Constructor
     *
     * @param cacheManager
     */
    public RMICacheManagerPeerProvider(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }


    /**
     * {@inheritDoc}
     */
    public void init() {
        //nothing required for this implementation
    }

    /**
     * Register a new peer
     *
     * @param rmiUrl
     */
    public synchronized void registerPeer(String rmiUrl) {
        peerUrls.put(rmiUrl, new Date());
    }

    private String extractCacheName(String rmiUrl) {
        return rmiUrl.substring(rmiUrl.lastIndexOf('/') + 1);
    }

    /**
     * Unregisters a peer
     *
     * @param rmiUrl
     */
    public synchronized void unregisterPeer(String rmiUrl) {
        peerUrls.remove(rmiUrl);
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
     * Whether the entry should be considered stale. This will depend on the type of RMICacheManagerPeerProvider.
     * <p/>
     * This method should be overridden for implementations that go stale based on date
     *
     * @param date the date the entry was created
     * @return true if stale
     */
    protected boolean stale(Date date) {
        return false;
    }


    /**
     * The use of one-time registry creation and Naming.rebind should mean we can create as many listeneres as we like.
     * They will simply replace the ones that were there.
     */
    public static CachePeer lookupRemoteCachePeer(String url) throws MalformedURLException, NotBoundException, RemoteException {
        return (CachePeer) Naming.lookup(url);
    }

    /**
     * Providers may be doing all sorts of exotic things and need to be able to clean up on dispose.
     *
     * @throws net.sf.ehcache.CacheException
     */
    public void dispose() throws CacheException {
        //nothing to do.
    }

    /**
     * The cacheManager this provider is bound to
     */
    public CacheManager getCacheManager() {
        return cacheManager;
    }

}

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
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A provider of Peer RMI addresses.
 *
 * @author Greg Luck
 * @version $Id$
 */
public abstract class RMICacheManagerPeerProvider implements CacheManagerPeerProvider {

    private static final Logger LOG = LoggerFactory.getLogger(RMICacheManagerPeerProvider.class.getName());

    /**
     * Contains a RMI URLs of the form: "//" + hostName + ":" + port + "/" + cacheName;
     */
    protected final Map peerUrls = Collections.synchronizedMap(new HashMap());

    /**
     * The CacheManager this peer provider is associated with.
     */
    protected CacheManager cacheManager;


    /**
     * Constructor
     *
     * @param cacheManager
     */
    public RMICacheManagerPeerProvider(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Empty constructor
     */
    public RMICacheManagerPeerProvider() {
        //nothing to do
    }


    /**
     * {@inheritDoc}
     */
    public abstract void init();


    /**
     * Register a new peer
     *
     * @param rmiUrl
     */
    public abstract void registerPeer(String rmiUrl);



    /**
     * Gets the cache name out of the url
     * @param rmiUrl
     * @return the cache name as it would appear in ehcache.xml
     */
    static String extractCacheName(String rmiUrl) {
        return rmiUrl.substring(rmiUrl.lastIndexOf('/') + 1);
    }

    /**
     * Unregisters a peer
     *
     * @param rmiUrl
     */
    public final synchronized void unregisterPeer(String rmiUrl) {
        peerUrls.remove(rmiUrl);
    }

    /**
     * @return a list of {@link net.sf.ehcache.distribution.CachePeer} peers for the given cache, excluding the local peer.
     */
    public abstract List listRemoteCachePeers(Ehcache cache) throws CacheException;

    /**
     * Whether the entry should be considered stale. This will depend on the type of RMICacheManagerPeerProvider.
     * <p/>
     * @param date the date the entry was created
     * @return true if stale
     */
    protected abstract boolean stale(Date date);


    /**
     * The use of one-time registry creation and Naming.rebind should mean we can create as many listeneres as we like.
     * They will simply replace the ones that were there.
     */
    public CachePeer lookupRemoteCachePeer(String url) throws MalformedURLException, NotBoundException, RemoteException {
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
    public final CacheManager getCacheManager() {
        return cacheManager;
    }

    /**
     * The replication scheme. Each peer provider has a scheme name, which can be used to specify
     * the scheme for replication and bootstrap purposes. Each <code>CacheReplicator</code> should lookup
     * the provider for its scheme type during replication. Similarly a <code>BootstrapCacheLoader</code>
     * should also look up the provider for its scheme.
     * <p/>
     * @since 1.6 introduced to permit multiple distribution schemes to be used in the same CacheManager
     * @return the well-known scheme name, which is determined by the replication provider author.
     */
    public String getScheme() {
        return "RMI";
    }
}

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

package net.sf.ehcache.distribution;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;

import java.util.List;

/**
 * Provides a discovery service to locate {@link CachePeer} RMI listener peers for a Cache.
 * @author Greg Luck
 * @version $Id$
 */
public interface CacheManagerPeerProvider {

    /**
     * Register a new peer.
     * @param rmiUrl
     */
    void registerPeer(String rmiUrl);

    /**
     * Unregisters a peer.
     *
     * @param rmiUrl
     */
    void unregisterPeer(String rmiUrl);

    /**
     * @return a list of {@link CachePeer} peers for the given cache, excluding the local peer.
     */
    List listRemoteCachePeers(Ehcache cache) throws CacheException;

    /**
     * Notifies providers to initialise themselves.
     * @throws CacheException
     */
    void init();


    /**
     * Providers may be doing all sorts of exotic things and need to be able to clean up on dispose.
     * @throws CacheException
     */
    void dispose() throws CacheException;

    /**
     * Time for a cluster to form. This varies considerably, depending on the implementation.
     * @return the time in ms, for a cluster to form
     */
    long getTimeForClusterToForm();


    /**
     * The replication scheme. Each peer provider has a scheme name, which can be used to specify
     * the scheme for replication and bootstrap purposes. Each <code>CacheReplicator</code> should lookup
     * the provider for its scheme type during replication. Similarly a <code>BootstrapCacheLoader</code>
     * should also look up the provider for its scheme.
     * <p/>
     * @since 1.6 introduced to permit multiple distribution schemes to be used in the same CacheManager
     * @return the well-known scheme name, which is determined by the replication provider author.
     */
    String getScheme();

}

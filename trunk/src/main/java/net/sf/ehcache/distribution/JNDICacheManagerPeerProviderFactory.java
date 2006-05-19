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


import net.sf.ehcache.CacheException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Properties;
import java.util.StringTokenizer;


/**
 * A factory to build CacheManagerPeerProvider based on RMI and JNDI.
 *
 * @author Andy McNutt
 * @author Greg Luck
 * @version $Id$
 * todo simplify
 */
public class JNDICacheManagerPeerProviderFactory extends RMICacheManagerPeerProviderFactory {
    
    private static final Log LOG = LogFactory.getLog(JNDICacheManagerPeerProviderFactory.class.getName());

    private static final String JNDI_URLS = "jndiUrls";
    private static final String STASH_CONTEXTS = "stashContexts";
    private static final String STASH_REMOTE_CACHE_PEERS = "stashRemoteCachePeers";


    /**
     * peerDiscovery=manual, 
     * jndiUrls=//hostname:port/cacheName //hostname:port/cacheName
     * The jndiUrls are in the format expected by the implementation
     * of your Context.INITIAL_CONTEXT_FACTORY.
     */
    protected CacheManagerPeerProvider createManuallyConfiguredCachePeerProvider(
            Properties properties) {
        String urls = extractAndLogProperty(JNDI_URLS, properties);
        if (urls == null || urls.length() == 0) {
            throw new CacheException(JNDI_URLS 
                    + " must be specified when peerDiscovery is manual");
        }
        urls = urls.trim();
        StringTokenizer stringTokenizer = new StringTokenizer(
                urls, PayloadUtil.URL_DELIMITER);
        String stashContexts = extractAndLogProperty(STASH_CONTEXTS, properties);
        String stashRemoteCachePeers = extractAndLogProperty(
                STASH_REMOTE_CACHE_PEERS, properties);
        boolean isStashContexts = (stashContexts == null || Boolean.valueOf(stashContexts).booleanValue());
        boolean isStashRemoteCachePeers = (stashRemoteCachePeers == null ||
                Boolean.valueOf(stashRemoteCachePeers).booleanValue());
        JNDICacheManagerPeerProvider jndiPeerProvider = 
                new JNDICacheManagerPeerProvider(isStashContexts, 
                isStashRemoteCachePeers);
        while (stringTokenizer.hasMoreTokens()) {
            String jndiUrl = stringTokenizer.nextToken();
            jndiUrl = jndiUrl.trim();
            jndiPeerProvider.registerPeer(jndiUrl);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Registering peer " + jndiUrl);
            }
        }
        return jndiPeerProvider;
    }

}

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

import java.util.Properties;
import java.util.StringTokenizer;
import java.net.InetAddress;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Builds a factory based on RMI
 * @author Greg Luck
 * @version $Id$
 */
public class RMICacheManagerPeerProviderFactory extends CacheManagerPeerProviderFactory {

    private static final Log LOG = LogFactory.getLog(RMICacheManagerPeerProviderFactory.class.getName());

    private static final String PEER_DISCOVERY = "peerDiscovery";
    private static final String AUTOMATIC_PEER_DISCOVERY = "automatic";
    private static final String MANUALLY_CONFIGURED_PEER_DISCOVERY = "manual";
    private static final String RMI_URLS = "rmiUrls";
    private static final String MULTICAST_GROUP_PORT = "multicastGroupPort";
    private static final String MULTICAST_GROUP_ADDRESS = "multicastGroupAddress";


    /**
     * @param properties implementation specific properties. These are configured as comma
     *                   separated name value pairs in ehcache.xml
     */
    public CacheManagerPeerProvider createCachePeerProvider(CacheManager cacheManager, Properties properties)
    throws CacheException {
        String peerDiscovery = extractAndLogProperty(PEER_DISCOVERY, properties);
        if (peerDiscovery.equalsIgnoreCase(AUTOMATIC_PEER_DISCOVERY)) {
            try {
                return createAutomaticallyConfiguredCachePeerProvider(cacheManager, properties);
            } catch (IOException e) {
                throw new CacheException("Could not create CacheManagerPeerProvider. Initial cause was " + e.getMessage(), e);
            }
        } else if (peerDiscovery.equalsIgnoreCase(MANUALLY_CONFIGURED_PEER_DISCOVERY)) {
            return createManuallyConfiguredCachePeerProvider(properties);
        } else {
            return null;
        }
    }


    /**
     * peerDiscovery=manual, rmiUrls=//hostname:port/cacheName //hostname:port/cacheName //hostname:port/cacheName
     */
    protected CacheManagerPeerProvider createManuallyConfiguredCachePeerProvider(Properties properties) {
        String rmiUrls = extractAndLogProperty(RMI_URLS, properties);
        if (rmiUrls == null || rmiUrls.length() == 0) {
            LOG.info("Starting manual peer provider with empty list of peers. No replication will occur unless peers are added.");
            rmiUrls = new String();
        }
        rmiUrls = rmiUrls.trim();
        StringTokenizer stringTokenizer = new StringTokenizer(rmiUrls, PayloadUtil.URL_DELIMITER);
        RMICacheManagerPeerProvider rmiPeerProvider = new ManualRMICacheManagerPeerProvider();
        while (stringTokenizer.hasMoreTokens()) {
            String rmiUrl = stringTokenizer.nextToken();
            rmiUrl = rmiUrl.trim();
            rmiPeerProvider.registerPeer(rmiUrl);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Registering peer " + rmiUrl);
            }
        }
        return rmiPeerProvider;
    }

    /**
     * peerDiscovery=automatic, multicastGroupAddress=230.0.0.1, multicastGroupPort=4446
     */
    protected CacheManagerPeerProvider createAutomaticallyConfiguredCachePeerProvider(CacheManager cacheManager,
                                                                             Properties properties) throws IOException {
        String groupAddressString = extractAndLogProperty(MULTICAST_GROUP_ADDRESS, properties);
        InetAddress groupAddress = InetAddress.getByName(groupAddressString);
        String multicastPortString = extractAndLogProperty(MULTICAST_GROUP_PORT, properties);
        Integer multicastPort = new Integer(multicastPortString);
        return new MulticastRMICacheManagerPeerProvider(cacheManager, groupAddress, multicastPort);
    }


    /**
     * @return null is their is not property for the key
     */
    protected String extractAndLogProperty(String name, Properties properties) {
        String foundValue = (String) properties.get(name);
        if (LOG.isDebugEnabled()) {
            LOG.debug(new StringBuffer().append("Value found for ").append(name).append(": ")
                    .append(foundValue).toString());
        }
        return foundValue;

    }


}

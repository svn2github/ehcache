/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 - 2004 Greg Luck.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by Greg Luck
 *       (http://sourceforge.net/users/gregluck) and contributors.
 *       See http://sourceforge.net/project/memberlist.php?group_id=93232
 *       for a list of contributors"
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "EHCache" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For written
 *    permission, please contact Greg Luck (gregluck at users.sourceforge.net).
 *
 * 5. Products derived from this software may not be called "EHCache"
 *    nor may "EHCache" appear in their names without prior written
 *    permission of Greg Luck.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL GREG LUCK OR OTHER
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by contributors
 * individuals on behalf of the EHCache project.  For more
 * information on EHCache, please see <http://ehcache.sourceforge.net/>.
 *
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
 * @version $Id: RMICacheManagerPeerProviderFactory.java,v 1.1 2006/03/09 06:38:19 gregluck Exp $
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
                throw new CacheException("Could not create CacheManagerPeerProvider. Error was " + e.getMessage());
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
    private CacheManagerPeerProvider createManuallyConfiguredCachePeerProvider(Properties properties) {
        String rmiUrls = extractAndLogProperty(RMI_URLS, properties);
        if (rmiUrls == null || rmiUrls.length() == 0) {
            throw new CacheException("rmiUrls must be specified when peerDiscovery is manual");
        }
        rmiUrls = rmiUrls.trim();
        StringTokenizer stringTokenizer = new StringTokenizer(rmiUrls, PayloadUtil.URL_DELIMITER);
        RMICacheManagerPeerProvider rmiPeerProvider = new RMICacheManagerPeerProvider();
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
    private CacheManagerPeerProvider createAutomaticallyConfiguredCachePeerProvider(CacheManager cacheManager,
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
    private String extractAndLogProperty(String name, Properties properties) {
        String foundValue = (String) properties.get(name);
        if (LOG.isDebugEnabled()) {
            LOG.debug(new StringBuffer().append("Value found for ").append(name).append(": ")
                    .append(foundValue).toString());
        }
        return foundValue;

    }


}

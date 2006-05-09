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
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.util.PropertyUtil;

import java.net.UnknownHostException;
import java.util.Properties;

/**
 * Builds a listener based on RMI.
 * <p/>
 * Expected configuration line:
 * <p/>
 * <code>
 * &lt;cachePeerListenerFactory class="net.sf.ehcache.distribution.RMICacheManagerPeerListenerFactory"
 * properties="hostName=localhost, port=5000" /&gt;
 * </code>
 * @author Greg Luck
 * @version $Id$
 */
public final class RMICacheManagerPeerListenerFactory extends CacheManagerPeerListenerFactory {

    /**
     * The default timeout for cache replication for a single replication action.
     * This may need to be increased for large data transfers.
     */
    public static final Integer DEFAULT_SOCKET_TIMEOUT_MILLIS = new Integer(120000);

    private static final String HOSTNAME = "hostName";
    private static final String PORT = "port";
    private static final String SOCKET_TIMEOUT_MILLIS = "socketTimeoutMillis";

    /**
     * @param properties implementation specific properties. These are configured as comma
     *                   separated name value pairs in ehcache.xml
     */
    public final CacheManagerPeerListener createCachePeerListener(CacheManager cacheManager, Properties properties)
            throws CacheException {
        String hostName = PropertyUtil.extractAndLogProperty(HOSTNAME, properties);
        String portString = PropertyUtil.extractAndLogProperty(PORT, properties);
        Integer port = null;
        if (portString != null && portString.length() != 0) {
            port = new Integer(portString);
        } else {
            port = new Integer(0);
        }
        String socketTimeoutMillisString = PropertyUtil.extractAndLogProperty(SOCKET_TIMEOUT_MILLIS, properties);
        Integer socketTimeoutMillis;
        if (socketTimeoutMillisString == null || socketTimeoutMillisString.length() == 0) {
            socketTimeoutMillis = DEFAULT_SOCKET_TIMEOUT_MILLIS;
        } else {
            socketTimeoutMillis = new Integer(socketTimeoutMillisString);
        }
        try {
            return new RMICacheManagerPeerListener(hostName, port, cacheManager, socketTimeoutMillis);
        } catch (UnknownHostException e) {
            throw new CacheException("Unable to create CacheManagerPeerListener. Initial cause was " + e.getMessage(), e);
        }
    }
}

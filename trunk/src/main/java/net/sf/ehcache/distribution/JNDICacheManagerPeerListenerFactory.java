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

import java.net.UnknownHostException;

/**
 * Builds a listener based on JNDI and RMI.
 * <p/>
 * Expected configuration line:
 * <p/>
 * <code>
 * <cachePeerListenerFactory class="net.sf.ehcache.distribution.JNDICacheManagerPeerListenerFactory"
 * properties="hostName=localhost, port=5000" />
 * </code>
 * @author Andy McNutt
 * @author Greg Luck
 * @version $Id$
 */
public class JNDICacheManagerPeerListenerFactory extends RMICacheManagerPeerListenerFactory {

    /**
     * A template method to actually create the factory
     *
     * @param hostName
     * @param port
     * @param cacheManager
     * @param socketTimeoutMillis
     * @return a crate CacheManagerPeerListener
     */
    protected CacheManagerPeerListener doCreateCachePeerListener(String hostName, Integer port, CacheManager cacheManager,
                                                                 Integer socketTimeoutMillis) {
         try {
            return new JNDICacheManagerPeerListener(hostName, port, cacheManager, socketTimeoutMillis);
        } catch (UnknownHostException e) {
            throw new CacheException("Unable to create CacheManagerPeerListener. Error was " + e.getMessage(), e);
        }
    }
}

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

package net.sf.ehcache.distribution.jms;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.distribution.CacheManagerPeerListener;
import net.sf.ehcache.distribution.CacheManagerPeerListenerFactory;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Greg Luck
 */
public class JMSCacheManagerPeerListenerFactory extends
        CacheManagerPeerListenerFactory {

    private static final Logger LOG = Logger.getLogger(JMSCacheManagerPeerListenerFactory.class.getName());

    /**
     * 
     * @param cacheManager the CacheManager instance connected to this peer provider
     * @param properties implementation specific properties. These are configured as comma
     * separated name value pairs in ehcache.xml
     * @return
     */
    @Override
    public CacheManagerPeerListener createCachePeerListener(
            CacheManager cacheManager, Properties properties) {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("createCachePeerListener ( cacheManager = " + cacheManager + ", properties = " + properties + " ) called ");
        }

        return null;
    }

}

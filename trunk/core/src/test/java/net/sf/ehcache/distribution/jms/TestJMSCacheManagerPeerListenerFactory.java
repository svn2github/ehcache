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

import net.sf.ehcache.distribution.CacheManagerPeerProviderFactory;
import net.sf.ehcache.distribution.CacheManagerPeerProvider;
import net.sf.ehcache.distribution.CacheManagerPeerListenerFactory;
import net.sf.ehcache.distribution.CacheManagerPeerListener;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.CacheException;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import java.util.Properties;

import com.sun.messaging.ConnectionConfiguration;

/**
 * Test provider using Sun Open Message queue dev mq
 *
 * @author Greg Luck
 * @version $Id$
 */
public class TestJMSCacheManagerPeerListenerFactory extends CacheManagerPeerListenerFactory {


    /**
     * Creates a peer provider.
     *
     * @param cacheManager the CacheManager instance connected to this peer provider
     * @param properties   implementation specific properties. These are configured as comma
     *                     separated name value pairs in ehcache.xml
     *
     * transactional=true|false the default is false
     * acknowledgementMode=AUTO_ACKNOWLEDGE|CLIENT_ACKNOWLEDGE|DUPS_OK_ACKNOWLEDGE|NO_ACKNOWLEDGE
     *
     * @return a constructed CacheManagerPeerProvider
     */
    public CacheManagerPeerListener createCachePeerListener(CacheManager cacheManager, Properties properties) {

        com.sun.messaging.ConnectionFactory factory = new com.sun.messaging.ConnectionFactory();
        try {
            factory.setProperty(ConnectionConfiguration.imqAddressList, "localhost:7676");
            factory.setProperty(ConnectionConfiguration.imqReconnectEnabled, "true");
        } catch (JMSException e) {
            throw new CacheException("Exception creating JMS Connection Factory", e);
        }

        return new JMSCacheManagerPeerListener(cacheManager, factory);

    }

}

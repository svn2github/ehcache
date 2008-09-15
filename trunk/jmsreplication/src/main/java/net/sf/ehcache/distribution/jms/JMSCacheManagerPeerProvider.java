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

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.distribution.CacheManagerPeerProvider;
import net.sf.ehcache.distribution.CachePeer;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author benoit.perroud@elca.ch
 * @author Greg Luck
 */
public class JMSCacheManagerPeerProvider implements CacheManagerPeerProvider {

    private static final Logger LOG = Logger.getLogger(JMSCacheManagerPeerProvider.class.getName());

    private CacheManager cacheManager;
    private List<CachePeer> remoteCachePeers = new ArrayList<CachePeer>();

    private MessageConsumer messageConsumer;
    private MessageProducer messageProducer;
    private Session producerSession;


    /**
     *
     * @param cacheManager
     * @param messageConsumer
     * @param messageProducer
     * @param producerSession
     */
    public JMSCacheManagerPeerProvider(CacheManager cacheManager, MessageConsumer messageConsumer,
                                       MessageProducer messageProducer, Session producerSession) {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("JMSCacheManagerPeerProvider constructor ( cacheManager = "
                    + cacheManager + ", messageConsumer = " + messageConsumer
                    + ", messageProducer = " + messageProducer + ", producerSession = " + producerSession
                    + " ) called");
        }

        this.cacheManager = cacheManager;
        this.messageConsumer = messageConsumer;
        this.messageProducer = messageProducer;
        this.producerSession = producerSession;
    }

    /**
     * Providers may be doing all sorts of exotic things and need to be able to clean up on dispose.
     * @throws CacheException
     */
    public void dispose() throws CacheException {

        try {
            messageConsumer.close();
            producerSession.close();
            messageProducer.close();
        } catch (JMSException e) {
            throw new CacheException(e.getMessage(), e);
        }

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("dispose ( ) called ");
        }

    }

    /**
     * Time for a cluster to form. This varies considerably, depending on the implementation.
     * @return the time in ms, for a cluster to form
     */
    public long getTimeForClusterToForm() {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("getTimeForClusterToForm ( ) called ");
        }

        return 0;
    }

    /**
     * Notifies providers to initialise themselves.
     * @throws CacheException
     */
    public void init() {

        LOG.fine("init ( ) called ");

        JMSCachePeer peer = new JMSCachePeer(cacheManager, messageProducer, producerSession);
        remoteCachePeers.add(peer);
        try {
            messageConsumer.setMessageListener(peer);
        } catch (JMSException e) {
            LOG.log(Level.SEVERE, "Cannot register " + peer + " as messageListener", e);
        }

    }

    /**
     * @return a list of {@link CachePeer} peers for the given cache, excluding the local peer.
     */
    public List<CachePeer> listRemoteCachePeers(Ehcache cache) throws CacheException {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("listRemoteCachePeers ( cache = " + cache + " ) called ");
        }

        return remoteCachePeers;
    }

    /**
     * Register a new peer.
     * @param rmiUrl
     */
    public void registerPeer(String rmiUrl) {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("registerPeer ( rmiUrl = " + rmiUrl + ") called ");
        }

    }

    /**
     * Unregisters a peer.
     *
     * @param rmiUrl
     */
    public void unregisterPeer(String rmiUrl) {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("unregisterPeer ( rmiUrl = " + rmiUrl + " ) called ");
        }

    }
}

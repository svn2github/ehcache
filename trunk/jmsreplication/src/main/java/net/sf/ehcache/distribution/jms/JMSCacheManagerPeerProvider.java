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
import static net.sf.ehcache.distribution.jms.JMSUtil.CACHE_MANAGER_UID;
import static net.sf.ehcache.distribution.jms.JMSUtil.localCacheManagerUid;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates a single instance of JMSCachePeer which does not publishing and subscribing to a single topic
 * for the CacheManager
 *
 * @author benoit.perroud@elca.ch
 * @author Greg Luck
 */
public class JMSCacheManagerPeerProvider implements CacheManagerPeerProvider {

    private static final Logger LOG = Logger.getLogger(JMSCacheManagerPeerProvider.class.getName());

    private CacheManager cacheManager;
    private List<CachePeer> remoteCachePeers = new ArrayList<CachePeer>();


    private TopicConnection replicationTopicConnection;
    private Topic replicationTopic;
    private QueueConnection getQueueConnection;
    private Queue getQueue;
    private AcknowledgementMode acknowledgementMode;
    private QueueReceiver getQueueRequestReceiver;
    private TopicSession topicPublisherSession;
    private TopicPublisher topicPublisher;
    private TopicSubscriber topicSubscriber;
    private QueueSession getQueueSession;
    private JMSCachePeer cachePeer;
    private boolean listenToTopic;


    /**
     * Constructor
     * @param cacheManager
     * @param replicationTopicConnection
     * @param replicationTopic
     * @param getQueueConnection
     * @param getQueue
     * @param acknowledgementMode
     * @param listenToTopic whether this provider should listen to events made to the JMS topic
     */
    public JMSCacheManagerPeerProvider(CacheManager cacheManager,
                                       TopicConnection replicationTopicConnection,
                                       Topic replicationTopic,
                                       QueueConnection getQueueConnection,
                                       Queue getQueue,
                                       AcknowledgementMode acknowledgementMode,
                                       boolean listenToTopic) {


        this.cacheManager = cacheManager;
        this.replicationTopicConnection = replicationTopicConnection;
        this.replicationTopic = replicationTopic;
        this.getQueueConnection = getQueueConnection;
        this.getQueue = getQueue;
        this.acknowledgementMode = acknowledgementMode;
        this.listenToTopic = listenToTopic;
    }


    /**
     * Time for a cluster to form. This varies considerably, depending on the implementation.
     *
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
     * <p/>
     *
     *
     * @throws CacheException
     */
    public void init() {

        try {

            topicPublisherSession = replicationTopicConnection.createTopicSession(false, acknowledgementMode.toInt());
            replicationTopicConnection.setExceptionListener(new ExceptionListener() {

                public void onException(JMSException e) {
                    LOG.log(Level.SEVERE, "Exception on replication Connection: " + e.getMessage(), e);
                }
            });

            topicPublisher = topicPublisherSession.createPublisher(replicationTopic);

            if (listenToTopic) {

                LOG.fine("Listening for message on topic " + replicationTopic.getTopicName());
                //ignore messages we have sent. The third parameter is noLocal, which means do not deliver back to the sender
                //on the same connection
                TopicSession topicSubscriberSession = replicationTopicConnection.createTopicSession(false, acknowledgementMode.toInt());
                topicSubscriber = topicSubscriberSession.createSubscriber(replicationTopic, null, true);
                replicationTopicConnection.start();
            }


            //noLocal is only supported in the JMS spec for topics. We need to use a message selector
            //on the queue to achieve the same effect.
            getQueueSession = getQueueConnection.createQueueSession(false, acknowledgementMode.toInt());
            String messageSelector = CACHE_MANAGER_UID + " <> " + localCacheManagerUid(cacheManager);
            getQueueRequestReceiver = getQueueSession.createReceiver(getQueue,  messageSelector);



            getQueueConnection.start();


        } catch (JMSException e) {
            throw new CacheException("Exception while creating JMS connections: " + e.getMessage(), e);
        }


        cachePeer = new JMSCachePeer(cacheManager, topicPublisher, topicPublisherSession, getQueueSession);

        remoteCachePeers.add(cachePeer);
        try {
            if (listenToTopic) {
                topicSubscriber.setMessageListener(cachePeer);
            }
            getQueueRequestReceiver.setMessageListener(cachePeer);
        } catch (JMSException e) {
            LOG.log(Level.SEVERE, "Cannot register " + cachePeer + " as messageListener", e);
        }


    }


    /**
     * Providers may be doing all sorts of exotic things and need to be able to clean up on dispose.
     *
     * @throws CacheException
     */
    public void dispose() throws CacheException {

        LOG.fine("JMSCacheManagerPeerProvider for CacheManager " + cacheManager.getName() + " being disposed.");

        try {

            cachePeer.dispose();

            topicPublisher.close();
            if (listenToTopic) {
                topicSubscriber.close();
                replicationTopicConnection.stop();
            }
            topicPublisherSession.close();
            replicationTopicConnection.close();

            getQueueRequestReceiver.close();
            getQueueSession.close();
            getQueueConnection.close();

        } catch (JMSException e) {
            LOG.severe(e.getMessage());
            throw new CacheException(e.getMessage(), e);
        }


    }


    /**
     * @return a list of {@link CachePeer} peers for the given cache, excluding the local peer.
     */
    public List<CachePeer> listRemoteCachePeers(Ehcache cache) throws CacheException {
        return remoteCachePeers;
    }


    /**
     * Register a new peer.
     *
     * @param rmiUrl
     */
    public void registerPeer(String rmiUrl) {
        throw new CacheException("Not implemented for JMS");
    }

    /**
     * Unregisters a peer.
     *
     * @param rmiUrl
     */
    public void unregisterPeer(String rmiUrl) {
        throw new CacheException("Not implemented for JMS");
    }
}

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
import net.sf.ehcache.CacheException;
import net.sf.ehcache.distribution.CacheManagerPeerProvider;
import net.sf.ehcache.distribution.CacheManagerPeerProviderFactory;
import net.sf.ehcache.util.PropertyUtil;

import javax.jms.JMSException;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.QueueConnectionFactory;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.naming.Context;
import javax.naming.NamingException;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * @author benoit.perroud@elca.ch
 * @author Greg Luck
 */
public class JMSCacheManagerPeerProviderFactory extends CacheManagerPeerProviderFactory {


    private boolean useJMSCacheLoader;

    private static final Logger LOG = Logger.getLogger(JMSCacheManagerPeerProviderFactory.class.getName());


    /**
     * @param cacheManager the CacheManager instance connected to this peer provider
     * @param properties   implementation specific properties. These are configured as comma
     *                     separated name value pairs in ehcache.xml
     * @return a provider, already connected to the message queue
     */
    @Override
    public CacheManagerPeerProvider createCachePeerProvider(CacheManager cacheManager, Properties properties) {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("createCachePeerProvider ( cacheManager = " + cacheManager
                    + ", properties = " + properties + " ) called ");
        }


        String securityPrincipalName = PropertyUtil.extractAndLogProperty(JMSConfiguration.SECURITY_PRINCIPAL_NAME, properties);
        String securityCredentials = PropertyUtil.extractAndLogProperty(JMSConfiguration.SECURITY_CREDENTIALS, properties);
        String initialContextFactoryName = PropertyUtil.extractAndLogProperty(JMSConfiguration.INITIAL_CONTEXT_FACTORY_NAME, properties);
        String urlPkgPrefixes = PropertyUtil.extractAndLogProperty(JMSConfiguration.URL_PKG_PREFIXES, properties);
        String providerURL = PropertyUtil.extractAndLogProperty(JMSConfiguration.PROVIDER_URL, properties);
        String replicationTopicBindingName = PropertyUtil.extractAndLogProperty(JMSConfiguration.REPLICATION_TOPIC_BINDING_NAME, properties);
        String getQueueBindingName = PropertyUtil.extractAndLogProperty(JMSConfiguration.GET_QUEUE_BINDING_NAME, properties);
        String getQueueConnectionFactoryBindingName = PropertyUtil.extractAndLogProperty(JMSConfiguration.GET_QUEUE_CONNECTION_FACTORY_BINDING_NAME, properties);
        String topicConnectionFactoryBindingName = PropertyUtil.extractAndLogProperty(JMSConfiguration.TOPIC_CONNECTION_FACTORY_BINDING_NAME, properties);
        String userName = PropertyUtil.extractAndLogProperty(JMSConfiguration.USERNAME, properties);
        String password = PropertyUtil.extractAndLogProperty(JMSConfiguration.PASSWORD, properties);
        String acknowledgementMode = PropertyUtil.extractAndLogProperty(JMSConfiguration.ACKNOWLEDGEMENT_MODE, properties);
        AcknowledgementMode effectiveAcknowledgementMode = AcknowledgementMode.forString(acknowledgementMode);
        LOG.fine("Creating TopicSession in " + effectiveAcknowledgementMode.name() + " mode.");

        validateJMSCacheLoaderConfiguration(getQueueBindingName, getQueueConnectionFactoryBindingName);

        Context context = null;

        TopicConnection replicationTopicConnection;
        QueueConnection getQueueConnection;

        TopicConnectionFactory topicConnectionFactory;
        Topic replicationTopic;
        QueueConnectionFactory queueConnectionFactory;
        Queue getQueue;

        try {

            context = JMSConfiguration.createInitialContext(securityPrincipalName, securityCredentials, initialContextFactoryName,
                    urlPkgPrefixes, providerURL, replicationTopicBindingName, topicConnectionFactoryBindingName,
                    getQueueBindingName, getQueueConnectionFactoryBindingName);


            topicConnectionFactory = (TopicConnectionFactory) JMSConfiguration.lookup(context, topicConnectionFactoryBindingName);
            replicationTopic = (Topic) JMSConfiguration.lookup(context, replicationTopicBindingName);

            queueConnectionFactory = (QueueConnectionFactory) JMSConfiguration.lookup(context, getQueueConnectionFactoryBindingName);
            getQueue = (Queue) JMSConfiguration.lookup(context, getQueueBindingName);

            JMSConfiguration.closeContext(context);
        } catch (NamingException ne) {
            throw new CacheException("NamingException " + ne.getMessage(), ne);
        }

        try {
            replicationTopicConnection = createTopicConnection(userName, password, topicConnectionFactory);
            getQueueConnection = createQueueConnection(userName, password, queueConnectionFactory);
        } catch (JMSException e) {
            throw new CacheException("Problem creating connections: " + e.getMessage(), e);
        }

        return new JMSCacheManagerPeerProvider(cacheManager, replicationTopicConnection, replicationTopic,
                getQueueConnection, getQueue, effectiveAcknowledgementMode);
    }

    private void validateJMSCacheLoaderConfiguration(String getQueueBindingName, String getQueueConnectionFactoryBindingName) {
        if (getQueueConnectionFactoryBindingName != null || getQueueBindingName != null) {
            useJMSCacheLoader = true;
        }
        if (getQueueConnectionFactoryBindingName != null && getQueueBindingName == null) {
            throw new CacheException("The 'getQueueBindingName is null'. Please configure.");
        }
        if (getQueueConnectionFactoryBindingName == null && getQueueBindingName != null) {
            throw new CacheException("The 'getQueueConnectionFactoryBindingName' is null. Please configure.");
        }
    }

    private TopicConnection createTopicConnection(String userName, String password,
                                                  TopicConnectionFactory topicConnectionFactory) throws JMSException {

        TopicConnection topicConnection;
        if (userName != null) {
            topicConnection = topicConnectionFactory.createTopicConnection(userName, password);
        } else {
            topicConnection = topicConnectionFactory.createTopicConnection();
        }
        return topicConnection;
    }

    private QueueConnection createQueueConnection(String userName, String password,
                                                  QueueConnectionFactory queueConnectionFactory) throws JMSException {
        QueueConnection queueConnection;
        if (userName != null) {
            queueConnection = queueConnectionFactory.createQueueConnection(userName, password);
        } else {
            queueConnection = queueConnectionFactory.createQueueConnection();
        }
        return queueConnection;
    }

}

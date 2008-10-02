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
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.jms.QueueConnectionFactory;
import javax.jms.Queue;
import javax.jms.QueueSession;
import javax.jms.QueueSender;
import javax.jms.QueueConnection;
import javax.jms.ConnectionConsumer;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
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

    /**
     * Enables acknowledgement mode to be specifiec
     */
    public enum AcknowledgementMode {

        AUTO_ACKNOWLEDGE(Session.AUTO_ACKNOWLEDGE),
        //CLIENT_ACKNOWLEDGE(Session.CLIENT_ACKNOWLEDGE), not supported
        DUPS_OK_ACKNOWLEDGE(Session.DUPS_OK_ACKNOWLEDGE),
        SESSION_TRANSACTED(Session.SESSION_TRANSACTED);

        private int mode;

        public static AcknowledgementMode forString(String value) {
            for (AcknowledgementMode mode : values()) {
                if (mode.name().equals(value)) {
                    return mode;
                }
            }
            return DUPS_OK_ACKNOWLEDGE;
        }

        private AcknowledgementMode(int mode) {
            this.mode = mode;
        }

        public int toInt() {
            return mode;
        }
    }

    /**
     * Configuration string
     */
    protected static final String PROVIDER_URL = "providerURL";

    /**
     * Configuration string
     */
    protected static final String REPLICATION_TOPIC_BINDING_NAME = "replicationTopicBindingName";

    /**
     * The JNDI binding name for the queue name used to do gets
     */
    protected static final String GET_QUEUE_BINDING_NAME = "getQueueBingingName";

    /**
     * Configuration string
     */
    protected static final String TOPIC_CONNECTION_FACTORY_BINDING_NAME = "topicConnectionFactoryBindingName";

    /**
     * Configuration string
     */
    protected static final String GET_QUEUE_CONNECTION_FACTORY_BINDING_NAME = "getQueueConnectionFactoryBindingName";

    /**
     * Configuration string
     */
    protected static final String USERNAME = "userName";

    /**
     * Configuration string
     */
    protected static final String PASSWORD = "password";

    private static final Logger LOG = Logger.getLogger(JMSCacheManagerPeerProviderFactory.class.getName());

    private static final String SECURITY_PRINCIPAL_NAME = "securityPrincipalName";
    private static final String SECURITY_CREDENTIALS = "securityCredentials";
    private static final String INITIAL_CONTEXT_FACTORY_NAME = "initialContextFactoryName";
    private static final String URL_PKG_PREFIXES = "urlPkgPrefixes";
    private static final String ACKNOWLEDGEMENT_MODE = "acknowledgementMode";


    /**
     * @param cacheManager the CacheManager instance connected to this peer provider
     * @param properties   implementation specific properties. These are configured as comma
     *                     separated name value pairs in ehcache.xml
     * @return a provider, already connected to the message queue
     */
    @Override
    public CacheManagerPeerProvider createCachePeerProvider(
            CacheManager cacheManager, Properties properties) {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("createCachePeerProvider ( cacheManager = " + cacheManager
                    + ", properties = " + properties + " ) called ");
        }


        String securityPrincipalName = PropertyUtil.extractAndLogProperty(SECURITY_PRINCIPAL_NAME, properties);
        String securityCredentials = PropertyUtil.extractAndLogProperty(SECURITY_CREDENTIALS, properties);
        String initialContextFactoryName = PropertyUtil.extractAndLogProperty(INITIAL_CONTEXT_FACTORY_NAME, properties);
        String urlPkgPrefixes = PropertyUtil.extractAndLogProperty(URL_PKG_PREFIXES, properties);
        String providerURL = PropertyUtil.extractAndLogProperty(PROVIDER_URL, properties);
        String replicationTopicBindingName = PropertyUtil.extractAndLogProperty(REPLICATION_TOPIC_BINDING_NAME, properties);
        String getQueueBindingName = PropertyUtil.extractAndLogProperty(GET_QUEUE_BINDING_NAME, properties);
        String getQueueConnectionFactoryBindingName = PropertyUtil.extractAndLogProperty(GET_QUEUE_CONNECTION_FACTORY_BINDING_NAME, properties);
        String topicConnectionFactoryBindingName = PropertyUtil.extractAndLogProperty(TOPIC_CONNECTION_FACTORY_BINDING_NAME, properties);
        String userName = PropertyUtil.extractAndLogProperty(USERNAME, properties);
        String password = PropertyUtil.extractAndLogProperty(PASSWORD, properties);
        String acknowledgementMode = PropertyUtil.extractAndLogProperty(ACKNOWLEDGEMENT_MODE, properties);

        validateJSMCacheLoaderConfiguration(getQueueBindingName, getQueueConnectionFactoryBindingName);

        Context context = createInitialContext(securityPrincipalName, securityCredentials, initialContextFactoryName,
                urlPkgPrefixes, providerURL, replicationTopicBindingName, topicConnectionFactoryBindingName,
                getQueueBindingName, getQueueConnectionFactoryBindingName);


        TopicConnectionFactory topicConnectionFactory;
        Topic topic;
        QueueConnectionFactory queueConnectionFactory;
        Queue queue;

        try {

            context = createInitialContext(securityPrincipalName, securityCredentials, initialContextFactoryName,
                    urlPkgPrefixes, providerURL, replicationTopicBindingName, topicConnectionFactoryBindingName,
                    getQueueBindingName, getQueueConnectionFactoryBindingName);


            topicConnectionFactory = (TopicConnectionFactory) lookup(context, topicConnectionFactoryBindingName);
            topic = (Topic) lookup(context, replicationTopicBindingName);
            queueConnectionFactory = (QueueConnectionFactory) lookup(context, getQueueConnectionFactoryBindingName);
            queue = (Queue) lookup(context, getQueueBindingName);

        } catch (NamingException ne) {

            throw new CacheException("NamingException " + ne.getMessage(), ne);
        }

        TopicSession topicPublisherSession;
        QueueSession getQueueSession;
        TopicPublisher topicPublisher;
        QueueSender getQueueSender;
        ConnectionConsumer connectionConsumer;
        TopicSubscriber topicSubscriber;
        try {
            TopicConnection topicConnection = createTopicConnection(userName, password, topicConnectionFactory);

            AcknowledgementMode effectiveAcknowledgementMode = AcknowledgementMode.forString(acknowledgementMode);
            LOG.fine("Creating TopicSession in " + effectiveAcknowledgementMode.name() + " mode.");
            topicPublisherSession = topicConnection.createTopicSession(false, effectiveAcknowledgementMode.toInt());
            TopicSession topicSubscriberSession = topicConnection.createTopicSession(false, effectiveAcknowledgementMode.toInt());

            topicPublisher = topicPublisherSession.createPublisher(topic);

            //ignore messages we have sent. The third parameter is noLocal, which means do not deliver back to the sender
            //on the same connection
            topicSubscriber = topicSubscriberSession.createSubscriber(topic, null, true);

            topicConnection.start();

            QueueConnection queueConnection = createQueueConnection(userName, password, queueConnectionFactory);

            effectiveAcknowledgementMode = AcknowledgementMode.forString(acknowledgementMode);
            LOG.fine("Creating QueueSession in " + effectiveAcknowledgementMode.name() + " mode.");
            getQueueSession = queueConnection.createQueueSession(false, effectiveAcknowledgementMode.toInt());
            connectionConsumer = queueConnection.createConnectionConsumer(queue, null, null, 1);
            getQueueSender = getQueueSession.createSender(queue);

            //ignore messages we have sent. The third parameter is noLocal, which means do not deliver back to the sender
            //on the same connection
//            topicSubscriber = connectionConsumer.createSubscriber(topic, null, true);

            queueConnection.start();




        } catch (JMSException e) {
            throw new CacheException("Exception while creating JMS connections: " + e.getMessage(), e);
        }

        try {
            if (context != null) {
                context.close();
            }
        } catch (NamingException e) {
            throw new CacheException("Exception while closing context", e);
        }
        return new JMSCacheManagerPeerProvider(cacheManager, topicSubscriber, topicPublisher, topicPublisherSession,
                                                getQueueSession, connectionConsumer, getQueueSender);
    }

    private void validateJSMCacheLoaderConfiguration(String getQueueBindingName, String getQueueConnectionFactoryBindingName) {
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

    private Context createInitialContext(String securityPrincipalName,
                                         String securityCredentials,
                                         String initialContextFactoryName,
                                         String urlPkgPrefixes,
                                         String providerURL,
                                         String replicationTopicBindingName,
                                         String topicConnectionFactoryBindingName,
                                         String getQueueBindingName,
                                         String getQueueConnectionFactoryBindingName) {
        Context context;
        LOG.fine("Getting initial context.");

        Properties env = new Properties();

        env.put(TOPIC_CONNECTION_FACTORY_BINDING_NAME, topicConnectionFactoryBindingName);
        env.put(REPLICATION_TOPIC_BINDING_NAME, replicationTopicBindingName);


        env.put(GET_QUEUE_CONNECTION_FACTORY_BINDING_NAME, getQueueConnectionFactoryBindingName);
        env.put(GET_QUEUE_BINDING_NAME, getQueueBindingName);

        env.put(Context.PROVIDER_URL, providerURL);

        if (initialContextFactoryName != null) {
            env.put(Context.INITIAL_CONTEXT_FACTORY, initialContextFactoryName);
            if (urlPkgPrefixes != null) {
                env.put(Context.URL_PKG_PREFIXES, urlPkgPrefixes);
            }
        }

        if (securityPrincipalName != null) {
            env.put(Context.SECURITY_PRINCIPAL, securityPrincipalName);
            if (securityCredentials != null) {
                env.put(Context.SECURITY_CREDENTIALS,
                        securityCredentials);
            } else {
                LOG.warning("You have set SecurityPrincipalName option but not the "
                        + "SecurityCredentials. This is likely to cause problems.");
            }
        }
        try {
            context = new InitialContext(env);
        } catch (NamingException ne) {

            throw new CacheException("NamingException " + ne.getMessage(), ne);
        }
        return context;
    }

    /**
     * Looks up an object in a JNDI Context
     *
     * @param ctx  the context to check
     * @param name the object name
     * @return the object or null if not found
     * @throws NamingException if an exception happens on lookup
     */
    protected Object lookup(Context ctx, String name) throws NamingException {
        try {
            LOG.fine("Looking up " + name);
            return ctx.lookup(name);
        } catch (NameNotFoundException e) {
            LOG.log(Level.SEVERE, "Could not find name [" + name + "].");
            throw e;
        }
    }
}

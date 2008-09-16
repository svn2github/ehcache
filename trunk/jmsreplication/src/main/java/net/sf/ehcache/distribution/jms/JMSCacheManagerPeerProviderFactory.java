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

    /**
     * Enables acknowledgement mode to be specifiec
     */
    public enum AcknowledgeMode {

        AUTO_ACKNOWLEDGE(Session.AUTO_ACKNOWLEDGE),
        CLIENT_ACKNOWLEDGE(Session.CLIENT_ACKNOWLEDGE),
        DUPS_OK_ACKNOWLEDGE(Session.DUPS_OK_ACKNOWLEDGE),
        SESSION_TRANSACTED(Session.SESSION_TRANSACTED);

        private int mode;

        public AcknowledgeMode forString(String value) {
            for (AcknowledgeMode mode : values()) {
                if (mode.name().equals(value)) {
                    return mode;
                }
            }
            return DUPS_OK_ACKNOWLEDGE;
        }

        private AcknowledgeMode(int mode) {
            this.mode = mode;
        }

        public int toInt() {
            return mode;
        }
    }

    /**
     * Configuration string
     */
    protected static final String PROVIDERURL = "providerURL";

    /**
     * Configuration string
     */
    protected static final String TOPICBINDINGNAME = "topicBindingName";

    /**
     * Configuration string
     */
    protected static final String TOPICCONNECTIONFACTORYBINDINGNAME = "topicConnectionFactoryBindingName";

    /**
     * Configuration string
     */
    protected static final String USERNAME = "userName";

    /**
     * Configuration string
     */
    protected static final String PASSWORD = "password";

    private static final Logger LOG = Logger.getLogger(JMSCacheManagerPeerProviderFactory.class.getName());

    private static final String SECURITYPINCIPALNAME = "securityPrincipalName";
    private static final String SECURITYCREDENTIALS = "securityCredentials";
    private static final String INITIALCONTEXTFACTORYNAME = "initialContextFactoryName";
    private static final String URLPKGPREFIXES = "urlPkgPrefixes";


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


        String securityPrincipalName = PropertyUtil.extractAndLogProperty(SECURITYPINCIPALNAME, properties);
        String securityCredentials = PropertyUtil.extractAndLogProperty(SECURITYCREDENTIALS, properties);
        String initialContextFactoryName = PropertyUtil.extractAndLogProperty(INITIALCONTEXTFACTORYNAME, properties);
        String urlPkgPrefixes = PropertyUtil.extractAndLogProperty(URLPKGPREFIXES, properties);
        String providerURL = PropertyUtil.extractAndLogProperty(PROVIDERURL, properties);
        String topicBindingName = PropertyUtil.extractAndLogProperty(TOPICBINDINGNAME, properties);
        String topicConnectionFactoryBindingName = PropertyUtil.extractAndLogProperty(TOPICCONNECTIONFACTORYBINDINGNAME, properties);
        String userName = PropertyUtil.extractAndLogProperty(USERNAME, properties);
        String password = PropertyUtil.extractAndLogProperty(PASSWORD, properties);


        Context context = null;
        Topic topic = null;

        TopicConnectionFactory topicConnectionFactory;
        try {

            context = createInitialContext(securityPrincipalName, securityCredentials, initialContextFactoryName,
                    urlPkgPrefixes, providerURL, topicBindingName, topicConnectionFactoryBindingName);


            LOG.fine("Looking up [" + topicConnectionFactoryBindingName + "]");
            topicConnectionFactory = (TopicConnectionFactory) lookup(context,
                    topicConnectionFactoryBindingName);

            LOG.fine("Looking up topic name [" + topicBindingName + "].");
            topic = (Topic) lookup(context, topicBindingName);

        } catch (NamingException ne) {

            throw new CacheException("NamingException " + topicConnectionFactoryBindingName, ne);
        }

        TopicSession topicPublisherSession;
        TopicPublisher topicPublisher;
        TopicSubscriber topicSubscriber;
        try {
            TopicConnection topicConnection = createTopicConnection(userName, password, topicConnectionFactory);

            LOG.fine("Creating TopicSessions, non-transactional, in AUTO_ACKNOWLEDGE mode.");
            topicPublisherSession = topicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
            TopicSession topicSubscriberSession = topicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

            LOG.fine("Creating TopicPublisher.");
            topicPublisher = topicPublisherSession.createPublisher(topic);

            LOG.fine("Creating TopicSubscriber.");
            topicSubscriber = topicSubscriberSession.createSubscriber(topic);

            LOG.fine("Starting TopicConnection.");
            topicConnection.start();

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
        return new JMSCacheManagerPeerProvider(cacheManager, topicSubscriber, topicPublisher, topicPublisherSession);
    }

    private TopicConnection createTopicConnection(String userName, String password,
                                                  TopicConnectionFactory topicConnectionFactory) throws JMSException {

        LOG.fine("About to create TopicConnection.");
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
                                         String topicBindingName,
                                         String topicConnectionFactoryBindingName) throws NamingException {
        Context context;
        LOG.fine("Getting initial context.");

        Properties env = new Properties();

        env.put(TOPICCONNECTIONFACTORYBINDINGNAME, topicConnectionFactoryBindingName);
        env.put(TOPICBINDINGNAME, topicBindingName);
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

        context = new InitialContext(env);
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

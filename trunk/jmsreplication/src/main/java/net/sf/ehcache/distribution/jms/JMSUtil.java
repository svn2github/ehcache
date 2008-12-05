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
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.CacheManager;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.NameNotFoundException;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Configuration file strings used by both the JMS replication and loader.
 *
 * @author Greg Luck
 */
public final class JMSUtil {

    /***/
    public static final String PROVIDER_URL = "providerURL";

    /***/
    public static final String REPLICATION_TOPIC_BINDING_NAME = "replicationTopicBindingName";

    /***/
    public static final String GET_QUEUE_BINDING_NAME = "getQueueBindingName";

    /***/
    public static final String TOPIC_CONNECTION_FACTORY_BINDING_NAME = "replicationTopicConnectionFactoryBindingName";

    /***/
    public static final String GET_QUEUE_CONNECTION_FACTORY_BINDING_NAME = "getQueueConnectionFactoryBindingName";

    /***/
    public static final String USERNAME = "userName";

    /***/
    public static final String PASSWORD = "password";

    /***/
    public static final String SECURITY_PRINCIPAL_NAME = "securityPrincipalName";

    /***/
    public static final String SECURITY_CREDENTIALS = "securityCredentials";

    /***/
    public static final String INITIAL_CONTEXT_FACTORY_NAME = "initialContextFactoryName";

    /***/
    public static final String URL_PKG_PREFIXES = "urlPkgPrefixes";

    /***/
    public static final String ACKNOWLEDGEMENT_MODE = "acknowledgementMode";

    /***/
    public static final String TIMEOUT_MILLIS = "timeoutMillis";

    /***/
    public static final String DEFAULT_LOADER_ARGUMENT = "defaultLoaderArgument";

    /***/
    public static final int MAX_PRIORITY = 9;

    /***/
    public static final String CACHE_MANAGER_UID = "cacheManagerUniqueId";

    /***/
    public static final String LISTEN_TO_TOPIC = "listenToTopic";


    private static final Logger LOG = Logger.getLogger(JMSUtil.class.getName());

    private JMSUtil() {
        //Utility class
    }

    /**
     * Creates a JNDI initial context.
     *
     * @param initialContextFactoryName   (mandatory) - the name of the factory used to create the message queue initial context.
     * @param providerURL                 (mandatory) - the JNDI configuration information for the service provider to use.
     * @param getQueueConnectionFactoryBindingName
     *                                    (mandatory) - the JNDI binding name for the QueueConnectionFactory
     * @param replicationTopicBindingName (mandatory) - the JNDI binding name for the topic name used for replication
     * @param replicationTopicConnectionFactoryBindingName
     *                                    (mandatory) - the JNDI binding name for the replication TopicConnectionFactory
     * @param getQueueBindingName         (mandatory) - the JNDI binding name for the queue name used to do make requests.
     * @param securityPrincipalName       the JNDI java.naming.security.principal
     * @param securityCredentials         the JNDI java.naming.security.credentials
     * @param urlPkgPrefixes              the JNDI java.naming.factory.url.pkgs
     *                                    AUTO_ACKNOWLEDGE, DUPS_OK_ACKNOWLEDGE and SESSION_TRANSACTED.
     *                                    The default is AUTO_ACKNOWLEDGE.
     * @return a context, ready for lookups
     */
    public static Context createInitialContext(String securityPrincipalName,
                                               String securityCredentials,
                                               String initialContextFactoryName,
                                               String urlPkgPrefixes,
                                               String providerURL,
                                               String replicationTopicBindingName,
                                               String replicationTopicConnectionFactoryBindingName,
                                               String getQueueBindingName,
                                               String getQueueConnectionFactoryBindingName) {
        Context context;

        Properties env = new Properties();

        if (replicationTopicConnectionFactoryBindingName != null) {
            env.put(TOPIC_CONNECTION_FACTORY_BINDING_NAME, replicationTopicConnectionFactoryBindingName);
        }
        if (replicationTopicBindingName != null) {
            env.put(REPLICATION_TOPIC_BINDING_NAME, replicationTopicBindingName);
        }


        if (getQueueConnectionFactoryBindingName != null) {
            env.put(GET_QUEUE_CONNECTION_FACTORY_BINDING_NAME, getQueueConnectionFactoryBindingName);
        }
        if (getQueueBindingName != null) {
            env.put(GET_QUEUE_BINDING_NAME, getQueueBindingName);
        }

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
     * @throws javax.naming.NamingException if an exception happens on lookup
     */
    public static Object lookup(Context ctx, String name) throws NamingException {
        try {
            LOG.fine("Looking up " + name);
            return ctx.lookup(name);
        } catch (NameNotFoundException e) {
            LOG.log(Level.SEVERE, "Could not find name [" + name + "].");
            throw e;
        }
    }

    /**
     * Closes the JNDI context.
     *
     * @param context the context to cose
     */
    public static void closeContext(Context context) {
        try {
            if (context != null) {
                context.close();
            }
        } catch (NamingException e) {
            throw new CacheException("Exception while closing context", e);
        }
    }

    /**
     * Returns a unique ID for a CacheManager. This method always returns the same value
     * for the life of a CacheManager instance.
     *
     * @param cache the CacheManager is discovered through a cache
     * @return an identifier for the local CacheManager
     */
    public static int localCacheManagerUid(Ehcache cache) {
        return localCacheManagerUid(cache.getCacheManager());
    }


    /**
     * Returns a unique ID for a CacheManager. This method always returns the same value
     * for the life of a CacheManager instance.
     *
     * @param cacheManager the CacheManager of interest
     * @return an identifier for the local CacheManager
     */
    public static int localCacheManagerUid(CacheManager cacheManager) {
        return cacheManager.hashCode();
    }
}

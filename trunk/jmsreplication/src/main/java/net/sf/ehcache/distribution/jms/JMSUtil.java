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
 */
public class JMSUtil {

    private static final Logger LOG = Logger.getLogger(JMSUtil.class.getName());

    static final String PROVIDER_URL = "providerURL";
    static final String REPLICATION_TOPIC_BINDING_NAME = "replicationTopicBindingName";
    static final String GET_QUEUE_BINDING_NAME = "getQueueBindingName";
    static final String TOPIC_CONNECTION_FACTORY_BINDING_NAME = "topicConnectionFactoryBindingName";
    static final String GET_QUEUE_CONNECTION_FACTORY_BINDING_NAME = "getQueueConnectionFactoryBindingName";
    static final String USERNAME = "userName";
    static final String PASSWORD = "password";
    static final String SECURITY_PRINCIPAL_NAME = "securityPrincipalName";
    static final String SECURITY_CREDENTIALS = "securityCredentials";
    static final String INITIAL_CONTEXT_FACTORY_NAME = "initialContextFactoryName";
    static final String URL_PKG_PREFIXES = "urlPkgPrefixes";
    static final String ACKNOWLEDGEMENT_MODE = "acknowledgementMode";
    static final String TIMEOUT_MILLIS = "timeoutMillis";
    static final String DEFAULT_LOADER_ARGUMENT = "defaultLoaderArgument";
    static final int MAX_PRIORITY = 9;
    static final String CACHE_MANAGER_UID = "cacheManagerUniqueId";

    public static Context createInitialContext(String securityPrincipalName,
                                         String securityCredentials,
                                         String initialContextFactoryName,
                                         String urlPkgPrefixes,
                                         String providerURL,
                                         String replicationTopicBindingName,
                                         String topicConnectionFactoryBindingName,
                                         String getQueueBindingName,
                                         String getQueueConnectionFactoryBindingName) {
        Context context;

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
     * @param cache the CacheManager is discovered through a cache
     * @return an identifier for the local CacheManager
     */
    public static int localCacheManagerUid(Ehcache cache) {
        return localCacheManagerUid(cache.getCacheManager());
    }


    /**
     * Returns a unique ID for a CacheManager. This method always returns the same value
     * for the life of a CacheManager instance.
     * @param cacheManager the CacheManager of interest
     * @return an identifier for the local CacheManager
     */
    public static int localCacheManagerUid(CacheManager cacheManager) {
        return cacheManager.hashCode();
    }
}

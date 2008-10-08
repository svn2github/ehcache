package net.sf.ehcache.distribution.jms;

import net.sf.ehcache.CacheException;

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
public class JMSConfiguration {

    private static final Logger LOG = Logger.getLogger(JMSConfiguration.class.getName());

    /**
     * Configuration string
     */
    static final String PROVIDER_URL = "providerURL";
    /**
     * Configuration string
     */
    static final String REPLICATION_TOPIC_BINDING_NAME = "replicationTopicBindingName";
    /**
     * The JNDI binding name for the queue name used to do gets
     */
    static final String GET_QUEUE_BINDING_NAME = "getQueueBingingName";
    /**
     * Configuration string
     */
    static final String TOPIC_CONNECTION_FACTORY_BINDING_NAME = "topicConnectionFactoryBindingName";
    /**
     * Configuration string
     */
    static final String GET_QUEUE_CONNECTION_FACTORY_BINDING_NAME = "getQueueConnectionFactoryBindingName";
    /**
     * Configuration string
     */
    static final String USERNAME = "userName";
    /**
     * Configuration string
     */
    static final String PASSWORD = "password";
    static final String SECURITY_PRINCIPAL_NAME = "securityPrincipalName";
    static final String SECURITY_CREDENTIALS = "securityCredentials";
    static final String INITIAL_CONTEXT_FACTORY_NAME = "initialContextFactoryName";
    static final String URL_PKG_PREFIXES = "urlPkgPrefixes";
    static final String ACKNOWLEDGEMENT_MODE = "acknowledgementMode";

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
}

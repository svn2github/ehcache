package net.sf.ehcache.distribution.jms;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import static net.sf.ehcache.distribution.jms.JMSConfiguration.SECURITY_PRINCIPAL_NAME;
import static net.sf.ehcache.distribution.jms.JMSConfiguration.SECURITY_CREDENTIALS;
import static net.sf.ehcache.distribution.jms.JMSConfiguration.INITIAL_CONTEXT_FACTORY_NAME;
import static net.sf.ehcache.distribution.jms.JMSConfiguration.URL_PKG_PREFIXES;
import static net.sf.ehcache.distribution.jms.JMSConfiguration.PROVIDER_URL;
import static net.sf.ehcache.distribution.jms.JMSConfiguration.REPLICATION_TOPIC_BINDING_NAME;
import static net.sf.ehcache.distribution.jms.JMSConfiguration.GET_QUEUE_BINDING_NAME;
import static net.sf.ehcache.distribution.jms.JMSConfiguration.GET_QUEUE_CONNECTION_FACTORY_BINDING_NAME;
import static net.sf.ehcache.distribution.jms.JMSConfiguration.TOPIC_CONNECTION_FACTORY_BINDING_NAME;
import static net.sf.ehcache.distribution.jms.JMSConfiguration.USERNAME;
import static net.sf.ehcache.distribution.jms.JMSConfiguration.PASSWORD;
import static net.sf.ehcache.distribution.jms.JMSConfiguration.ACKNOWLEDGEMENT_MODE;
import static net.sf.ehcache.distribution.jms.JMSConfiguration.TIMEOUT_MILLIS;
import net.sf.ehcache.loader.CacheLoaderFactory;
import net.sf.ehcache.util.PropertyUtil;
import net.sf.jsr107cache.CacheLoader;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.naming.Context;
import javax.naming.NamingException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * A factory to create JMSCacheLoaders.
 *
 *
 * @author Greg Luck
 *
 */
public class JMSCacheLoaderFactory extends CacheLoaderFactory {

    private static final Logger LOG = Logger.getLogger(JMSCacheLoaderFactory.class.getName());
    private static final int DEFAULT_TIMEOUT_INTERVAL_MILLIS = 30000;

    /**
     * Creates a CacheLoader using the JSR107 creational mechanism.
     * This method is called from {@link net.sf.ehcache.jcache.JCacheFactory}
     *
     * @param environment the same environment passed into {@link net.sf.ehcache.jcache.JCacheFactory}.
     *                    This factory can extract any properties it needs from the environment.
     * @return a constructed CacheLoader
     */
    public CacheLoader createCacheLoader(Map environment) {
        throw new CacheException("Use the factory method which takes a cache parameter");
    }

    /**
     * Creates a CacheLoader using the Ehcache configuration mechanism at the time the associated cache
     * is created.
     *
     * @param properties implementation specific properties. These are configured as comma
     *                   separated name value pairs in ehcache.xml
     * @param cache the cache this loader is bound to
     * @return a constructed CacheLoader
     */
    public net.sf.ehcache.loader.CacheLoader createCacheLoader(Ehcache cache, Properties properties) {


        String securityPrincipalName = PropertyUtil.extractAndLogProperty(SECURITY_PRINCIPAL_NAME, properties);
        String securityCredentials = PropertyUtil.extractAndLogProperty(SECURITY_CREDENTIALS, properties);
        String initialContextFactoryName = PropertyUtil.extractAndLogProperty(INITIAL_CONTEXT_FACTORY_NAME, properties);
        String urlPkgPrefixes = PropertyUtil.extractAndLogProperty(URL_PKG_PREFIXES, properties);
        String providerURL = PropertyUtil.extractAndLogProperty(PROVIDER_URL, properties);
        String replicationTopicBindingName = PropertyUtil.extractAndLogProperty(REPLICATION_TOPIC_BINDING_NAME, properties);
        String getQueueConnectionFactoryBindingName = PropertyUtil.extractAndLogProperty(GET_QUEUE_CONNECTION_FACTORY_BINDING_NAME, properties);
        if (getQueueConnectionFactoryBindingName == null ) {
            throw new CacheException("getQueueConnectionFactoryBindingName is not configured.");
        }
        String getQueueBindingName = PropertyUtil.extractAndLogProperty(GET_QUEUE_BINDING_NAME, properties);
        if (getQueueBindingName == null ) {
            throw new CacheException("getQueueBindingName is not configured.");
        }
        String topicConnectionFactoryBindingName = PropertyUtil.extractAndLogProperty(TOPIC_CONNECTION_FACTORY_BINDING_NAME, properties);
        String userName = PropertyUtil.extractAndLogProperty(USERNAME, properties);
        String password = PropertyUtil.extractAndLogProperty(PASSWORD, properties);
        String acknowledgementMode = PropertyUtil.extractAndLogProperty(ACKNOWLEDGEMENT_MODE, properties);


        if (getQueueBindingName == null ) {
            throw new CacheException("getQueueBindingName is not configured.");
        }

        int timeoutMillis = extractTimeoutMillis(properties);

        AcknowledgementMode effectiveAcknowledgementMode = AcknowledgementMode.forString(acknowledgementMode);


        Context context = null;

        QueueConnection getQueueConnection;
        QueueConnectionFactory queueConnectionFactory;
        Queue getQueue;

        try {

            context = JMSConfiguration.createInitialContext(securityPrincipalName, securityCredentials, initialContextFactoryName,
                    urlPkgPrefixes, providerURL, replicationTopicBindingName, topicConnectionFactoryBindingName,
                    getQueueBindingName, getQueueConnectionFactoryBindingName);


            queueConnectionFactory = (QueueConnectionFactory) JMSConfiguration.lookup(context, getQueueConnectionFactoryBindingName);
            getQueue = (Queue) JMSConfiguration.lookup(context, getQueueBindingName);

            JMSConfiguration.closeContext(context);
        } catch (NamingException ne) {
            throw new CacheException("NamingException " + ne.getMessage(), ne);
        }

        try {
            getQueueConnection = createQueueConnection(userName, password, queueConnectionFactory);
        } catch (JMSException e) {
            throw new CacheException("Problem creating connections: " + e.getMessage(), e);
        }

        return new JMSCacheLoader(cache, getQueueConnection, getQueue, effectiveAcknowledgementMode, timeoutMillis);
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


    /**
     * Extracts the value of timeoutMillis. Sets it to 30000ms if
     * either not set or there is a problem parsing the number
     * @param properties
     */
    protected int extractTimeoutMillis(Properties properties) {
        int timeoutMillis = 0;
        String timeoutMillisString =
                PropertyUtil.extractAndLogProperty(TIMEOUT_MILLIS, properties);
        if (timeoutMillisString != null) {
            try {
                timeoutMillis = Integer.parseInt(timeoutMillisString);
            } catch (NumberFormatException e) {
                LOG.warning("Number format exception trying to set asynchronousReplicationIntervalMillis. " +
                        "Using the default instead. String value was: '" + timeoutMillisString + "'");
                timeoutMillis = DEFAULT_TIMEOUT_INTERVAL_MILLIS;
            }
        } else {
            timeoutMillis = DEFAULT_TIMEOUT_INTERVAL_MILLIS;
        }
        return timeoutMillis;
    }

}

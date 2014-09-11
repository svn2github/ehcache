package net.sf.ehcache.distribution.jms;

import net.sf.ehcache.event.CacheEventListenerFactory;
import net.sf.ehcache.event.CacheEventListener;

import java.util.Properties;

/**
 * @author Greg Luck
 * @version $Id$
 */
public class TestJMSCacheReplicatorFactory extends CacheEventListenerFactory {

    /**
     * Create a <code>CacheEventListener</code>
     *
     * @param properties implementation specific properties. These are configured as comma
     *                   separated name value pairs in ehcache.xml
     * known properties:
     * connectionFactoryClass e.g. com.sun.messaging.ConnectionFactory
     *
     *
     * @return a constructed CacheEventListener
     */
    public CacheEventListener createCacheEventListener(Properties properties) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}

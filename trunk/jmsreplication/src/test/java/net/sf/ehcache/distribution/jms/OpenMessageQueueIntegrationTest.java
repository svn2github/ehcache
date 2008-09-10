package net.sf.ehcache.distribution.jms;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import com.sun.messaging.ConnectionConfiguration;

import javax.jms.JMSException;
import javax.jms.Connection;

import net.sf.ehcache.CacheException;

/**
 * @author Greg Luck
 * @version $Id$
 */
public class OpenMessageQueueIntegrationTest {


    /**
     * Tests a manual cluster partitioning technique based on key hashing
     * i.e. assuming 5 clusters behind a load balancer named cluster1, cluster2...
     * Each cluster could have 2 or more ehcache servers in it responding to queries and distributing amongst other nodes in their cluster.
     */
    @Test
    public void testHashing() {

        String[] cacheservers = new String[]{"cacheserver0.company.com", "cacheserver1.company.com", "cacheserver2.company.com", "cacheserver3.company.com",
                "cacheserver4.company.com", "cacheserver5.company.com"};

        net.sf.ehcache.Element element = new net.sf.ehcache.Element("dfadfasdfadsfa", "some value");
        Object key = element.getKey();
        int hash = Math.abs(key.hashCode());
        int cacheserverIndex = hash % cacheservers.length;

        String cacheserverToUseForKey = cacheservers[cacheserverIndex];

        assertEquals("cacheserver4.company.com", cacheserverToUseForKey);
    }

    /**
     * Can we create a connection to Open Message Queue
     */
    @Test
    public void testConnectionFactory() throws JMSException {
        com.sun.messaging.ConnectionFactory factory = new com.sun.messaging.ConnectionFactory();
        factory.setProperty(ConnectionConfiguration.imqAddressList, "localhost:7676");
        factory.setProperty(ConnectionConfiguration.imqReconnectEnabled, "true");
        Connection myConnection = factory.createConnection();
        assertNotNull(myConnection);
    }

}

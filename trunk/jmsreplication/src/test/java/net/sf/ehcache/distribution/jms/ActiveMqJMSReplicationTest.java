package net.sf.ehcache.distribution.jms;

import org.junit.Test;

/**
 * Run the tests using Active MQ
 * @author Greg Luck
 */
public class ActiveMqJMSReplicationTest extends AbstractJMSReplicationTest {

    protected String getConfigurationFile() {
        return "distribution/jms/ehcache-distributed-jms-activemq.xml";
    }

    /**
     * Uses the JMSCacheLoader.
     * <p/>
     * We put an item in cache1, which does not replicate.
     * <p/>
     * We then do a get on cache2, which has a JMSCacheLoader which should ask the cluster for the answer.
     * If a cache does not have an element it should leave the message on the queue for the next node to process.
     */
    @Override
    @Test
    public void testGet() throws InterruptedException {
        super.testGet();
    }

}

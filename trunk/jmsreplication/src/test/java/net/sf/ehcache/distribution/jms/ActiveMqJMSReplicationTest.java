package net.sf.ehcache.distribution.jms;

/**
 * Run the tests using Active MQ
 * @author Greg Luck
 */
public class ActiveMqJMSReplicationTest extends JMSReplicationTest {

    protected String getConfigurationFile() {
        return "distribution/jms/ehcache-distributed-jms-activemq.xml";
    }

}

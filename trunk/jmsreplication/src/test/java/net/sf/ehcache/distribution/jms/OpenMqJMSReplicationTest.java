package net.sf.ehcache.distribution.jms;

import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import com.sun.messaging.ConnectionConfiguration;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.ObjectMessage;
import javax.jms.Destination;
import javax.jms.TopicConnection;
import javax.jms.TopicSession;
import javax.jms.Topic;
import javax.jms.TopicPublisher;

import net.sf.ehcache.Element;
import net.sf.ehcache.Cache;
import static net.sf.ehcache.distribution.jms.JMSEventMessage.ACTION_PROPERTY;
import static net.sf.ehcache.distribution.jms.JMSEventMessage.CACHE_NAME_PROPERTY;
import static net.sf.ehcache.distribution.jms.JMSEventMessage.MIME_TYPE_PROPERTY;
import static net.sf.ehcache.distribution.jms.JMSEventMessage.KEY_PROPERTY;

/**
 * Run the tests using Open MQ
 * The create_administered_objects needs to have been run first
 *
 * @author Greg Luck
 */
public class OpenMqJMSReplicationTest extends AbstractJMSReplicationTest {

    protected String getConfigurationFile() {
        return "distribution/jms/ehcache-distributed-jms-openmq.xml";
    }

    @Test
    public void testPutAndRemove() throws InterruptedException {
        super.testPutAndRemove();
    }


    /**
     * No cache name or mime type set.
     */
    @Test
    public void testNonCachePublisherPropertiesNotSet() throws JMSException, InterruptedException {

        Element payload = new Element("1234", "dog");
        TopicConnection connection = getMQConnection();
        connection.start();

        TopicSession publisherSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
        ObjectMessage message = publisherSession.createObjectMessage(payload);
        Topic topic = publisherSession.createTopic("EhcacheTopicDest");
        TopicPublisher publisher = publisherSession.createPublisher(topic);
        publisher.send(message);

        Thread.sleep(100);

        Cache cache = manager1.getCache(cacheName);
        Element receivedElement = cache.get("1234");

        //ignored because no MimeType
        assertNull(receivedElement);
    }

    /**
     * 
     * @throws JMSException
     * @throws InterruptedException
     */
    @Test
    public void testNonCachePublisherElementMessagePut() throws JMSException, InterruptedException {

        Element payload = new Element("1234", "dog");
        TopicConnection connection = getMQConnection();
        connection.start();

        TopicSession publisherSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        ObjectMessage message = publisherSession.createObjectMessage(payload);
        message.setStringProperty(ACTION_PROPERTY, JMSEventMessage.Action.PUT.name());
        message.setStringProperty(CACHE_NAME_PROPERTY, "sampleCacheAsync");
        //don't set. Should work.
        //message.setStringProperty(MIME_TYPE_PROPERTY, null);
        //should work. Key should be ignored when sending an element.
        message.setStringProperty(KEY_PROPERTY, "ignore");


        Topic topic = publisherSession.createTopic("EhcacheTopicDest");
        TopicPublisher publisher = publisherSession.createPublisher(topic);
        publisher.send(message);

        connection.stop();

        Thread.sleep(100);


        assertEquals(payload, manager1.getCache("sampleCacheAsync").get("1234"));
        assertEquals(payload, manager2.getCache("sampleCacheAsync").get("1234"));
    }

    /**
     * Does not work if do not set key
     */
    @Test
    public void testNonCachePublisherElementMessageRemoveNoKey() throws JMSException, InterruptedException {

        //make sure there is an element
        testNonCachePublisherElementMessagePut();
        TopicConnection connection = getMQConnection();
        connection.start();

        TopicSession publisherSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        ObjectMessage message = publisherSession.createObjectMessage();
        message.setStringProperty(ACTION_PROPERTY, JMSEventMessage.Action.REMOVE.name());
        message.setStringProperty(CACHE_NAME_PROPERTY, "sampleCacheAsync");
        //don't set. Should work.
        //message.setStringProperty(MIME_TYPE_PROPERTY, null);
        //should work. Key should be ignored when sending an element.

        //won't work because key not set
//        message.setStringProperty(KEY_PROPERTY, "1234");


        Topic topic = publisherSession.createTopic("EhcacheTopicDest");
        TopicPublisher publisher = publisherSession.createPublisher(topic);
        publisher.send(message);

        connection.stop();

        Thread.sleep(100);


        assertNotNull(manager1.getCache("sampleCacheAsync").get("1234"));
        assertNotNull(manager2.getCache("sampleCacheAsync").get("1234"));
    }


    @Test
    public void testNonCachePublisherMessageRemove() throws JMSException, InterruptedException {

        //make sure there is an element
        testNonCachePublisherElementMessagePut();
        TopicConnection connection = getMQConnection();
        connection.start();

        TopicSession publisherSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        ObjectMessage message = publisherSession.createObjectMessage();
        message.setStringProperty(ACTION_PROPERTY, JMSEventMessage.Action.REMOVE.name());
        message.setStringProperty(CACHE_NAME_PROPERTY, "sampleCacheAsync");
        //don't set. Should work.
        //message.setStringProperty(MIME_TYPE_PROPERTY, null);
        //should work. Key should be ignored when sending an element.

        message.setStringProperty(KEY_PROPERTY, "1234");


        Topic topic = publisherSession.createTopic("EhcacheTopicDest");
        TopicPublisher publisher = publisherSession.createPublisher(topic);
        publisher.send(message);

        connection.stop();

        Thread.sleep(100);


        assertEquals(null, manager1.getCache("sampleCacheAsync").get("1234"));
        assertEquals(null, manager2.getCache("sampleCacheAsync").get("1234"));
    }


    /**
     * Use the property key even if an element is sent with remove
     */
    @Test
    public void testNonCachePublisherElementMessageRemove() throws JMSException, InterruptedException {

        //make sure there is an element
        testNonCachePublisherElementMessagePut();
        TopicConnection connection = getMQConnection();
        connection.start();

        TopicSession publisherSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        ObjectMessage message = publisherSession.createObjectMessage(new Element("ignored", "dog"));
        message.setStringProperty(ACTION_PROPERTY, JMSEventMessage.Action.REMOVE.name());
        message.setStringProperty(CACHE_NAME_PROPERTY, "sampleCacheAsync");
        //don't set. Should work.
        //message.setStringProperty(MIME_TYPE_PROPERTY, null);
        //should work. Key should be ignored when sending an element.

        message.setStringProperty(KEY_PROPERTY, "1234");


        Topic topic = publisherSession.createTopic("EhcacheTopicDest");
        TopicPublisher publisher = publisherSession.createPublisher(topic);
        publisher.send(message);

        connection.stop();

        Thread.sleep(100);


        assertEquals(null, manager1.getCache("sampleCacheAsync").get("1234"));
        assertEquals(null, manager2.getCache("sampleCacheAsync").get("1234"));
    }

    @Test
    public void testNonCachePublisherMessageRemoveAll() throws JMSException, InterruptedException {

        //make sure there is an element
        testNonCachePublisherElementMessagePut();
        TopicConnection connection = getMQConnection();
        connection.start();

        TopicSession publisherSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        ObjectMessage message = publisherSession.createObjectMessage();
        message.setStringProperty(ACTION_PROPERTY, JMSEventMessage.Action.REMOVE_ALL.name());
        message.setStringProperty(CACHE_NAME_PROPERTY, "sampleCacheAsync");
        //don't set. Should work.
        //message.setStringProperty(MIME_TYPE_PROPERTY, null);
        //should work. Key should be ignored when sending an element.

        message.setStringProperty(KEY_PROPERTY, "1234");


        Topic topic = publisherSession.createTopic("EhcacheTopicDest");
        TopicPublisher publisher = publisherSession.createPublisher(topic);
        publisher.send(message);

        connection.stop();

        Thread.sleep(100);


        assertEquals(null, manager1.getCache("sampleCacheAsync").get("1234"));
        assertEquals(null, manager2.getCache("sampleCacheAsync").get("1234"));
    }

    /**
     * Gets a connection without using JNDI, so that it is fully independent.
     * @throws JMSException
     */
    private TopicConnection getMQConnection() throws JMSException {
        com.sun.messaging.ConnectionFactory factory = new com.sun.messaging.ConnectionFactory();
        factory.setProperty(ConnectionConfiguration.imqAddressList, "localhost:7676");
        factory.setProperty(ConnectionConfiguration.imqReconnectEnabled, "true");
        TopicConnection myConnection = factory.createTopicConnection();
        assertNotNull(myConnection);
        return myConnection;
    }


}
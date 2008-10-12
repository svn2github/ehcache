package net.sf.ehcache.distribution.jms;

import com.sun.messaging.ConnectionConfiguration;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.MimeTypeByteArray;
import net.sf.ehcache.util.ClassLoaderUtil;
import static net.sf.ehcache.distribution.jms.JMSEventMessage.ACTION_PROPERTY;
import static net.sf.ehcache.distribution.jms.JMSEventMessage.CACHE_NAME_PROPERTY;
import static net.sf.ehcache.distribution.jms.JMSEventMessage.KEY_PROPERTY;
import static net.sf.ehcache.distribution.jms.JMSEventMessage.MIME_TYPE_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;

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



    /**
     * Manual test.
     * <p/>
     * todo - not recovering after message queue is restarted
     * <p/>
     * INFO: Responder: manager1 JMSCachePeer
     * net.sf.ehcache.CacheException: Exception on load for key 1
     * at net.sf.ehcache.Cache.getWithLoader(Cache.java:864)
     * at net.sf.ehcache.distribution.jms.AbstractJMSReplicationTest.testGetMessageQueueFailure(AbstractJMSReplicationTest.java:584)
     * at net.sf.ehcache.distribution.jms.OpenMqJMSReplicationTest.testGetMessageQueueFailure(OpenMqJMSReplicationTest.java:59)
     * at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
     * at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
     * at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
     * at java.lang.reflect.Method.invoke(Method.java:585)
     * at org.junit.internal.runners.TestMethodRunner.executeMethodBody(TestMethodRunner.java:99)
     * at org.junit.internal.runners.TestMethodRunner.runUnprotected(TestMethodRunner.java:81)
     * at org.junit.internal.runners.BeforeAndAfterRunner.runProtected(BeforeAndAfterRunner.java:34)
     * at org.junit.internal.runners.TestMethodRunner.runMethod(TestMethodRunner.java:75)
     * at org.junit.internal.runners.TestMethodRunner.run(TestMethodRunner.java:45)
     * at org.junit.internal.runners.TestClassMethodsRunner.invokeTestMethod(TestClassMethodsRunner.java:71)
     * at org.junit.internal.runners.TestClassMethodsRunner.run(TestClassMethodsRunner.java:35)
     * at org.junit.internal.runners.TestClassRunner$1.runUnprotected(TestClassRunner.java:42)
     * at org.junit.internal.runners.BeforeAndAfterRunner.runProtected(BeforeAndAfterRunner.java:34)
     * at org.junit.internal.runners.TestClassRunner.run(TestClassRunner.java:52)
     * at com.intellij.rt.junit4.Junit4TestMethodAdapter.run(Junit4TestMethodAdapter.java:53)
     * at junit.textui.TestRunner.doRun(TestRunner.java:115)
     * at com.intellij.rt.execution.junit.IdeaTestRunner.doRun(IdeaTestRunner.java:94)
     * at junit.textui.TestRunner.doRun(TestRunner.java:108)
     * at com.intellij.rt.execution.junit.IdeaTestRunner.startRunnerWithArgs(IdeaTestRunner.java:22)
     * at com.intellij.rt.execution.junit.JUnitStarter.prepareStreamsAndStart(JUnitStarter.java:118)
     * at com.intellij.rt.execution.junit.JUnitStarter.main(JUnitStarter.java:40)
     * Caused by: java.util.concurrent.ExecutionException: net.sf.ehcache.CacheException: Problem during load. Load will not be completed. Cause was null
     * at java.util.concurrent.FutureTask$Sync.innerGet(FutureTask.java:205)
     * at java.util.concurrent.FutureTask.get(FutureTask.java:80)
     * at net.sf.ehcache.Cache.getWithLoader(Cache.java:862)
     * ... 23 more
     * Caused by: net.sf.ehcache.CacheException: Problem during load. Load will not be completed. Cause was null
     * at net.sf.ehcache.Cache$1.run(Cache.java:2230)
     * at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:417)
     * at java.util.concurrent.FutureTask$Sync.innerRun(FutureTask.java:269)
     * at java.util.concurrent.FutureTask.run(FutureTask.java:123)
     * at java.util.concurrent.ThreadPoolExecutor$Worker.runTask(ThreadPoolExecutor.java:650)
     * at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:675)
     * at java.lang.Thread.run(Thread.java:613)
     * Caused by: java.lang.AssertionError: The load request received an uncorrelated request. Request ID was ID:417-192.168.1.101(ab:4b:8:f5:7c:f3)-50440-1223728658817
     * at net.sf.ehcache.distribution.jms.JMSCacheLoader.load(JMSCacheLoader.java:130)
     * at net.sf.ehcache.distribution.jms.JMSCacheLoader.load(JMSCacheLoader.java:78)
     * at net.sf.ehcache.Cache.loadWithRegisteredLoaders(Cache.java:2242)
     * at net.sf.ehcache.Cache.access$100(Cache.java:74)
     * at net.sf.ehcache.Cache$1.run(Cache.java:2214)
     * ... 6 more
     * Oct 11, 2008 10:37:42 PM net.sf.ehcache.distribution.jms.JMSCachePeer onMessage
     */
    @Override
    //@Test
    public void testGetMessageQueueFailure() throws InterruptedException {
        super.testGetMessageQueueFailure(); 
    }


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

    @Test
    public void testNonCachePublisherObjectMessagePut() throws JMSException, InterruptedException {

        String payload = "this is an object";
        TopicConnection connection = getMQConnection();
        connection.start();

        TopicSession publisherSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        ObjectMessage message = publisherSession.createObjectMessage(payload);
        message.setStringProperty(ACTION_PROPERTY, JMSEventMessage.Action.PUT.name());
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


        assertEquals(payload, manager1.getCache("sampleCacheAsync").get("1234").getObjectValue());
        assertEquals(payload, manager2.getCache("sampleCacheAsync").get("1234").getObjectValue());
    }


    @Test
    public void testNonCachePublisherByteMessagePut() throws JMSException, InterruptedException {

        byte[] bytes = new byte[]{0x34, (byte) 0xe3, (byte) 0x88};
        TopicConnection connection = getMQConnection();
        connection.start();

        TopicSession publisherSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        BytesMessage message = publisherSession.createBytesMessage();
        message.writeBytes(bytes);
        message.setStringProperty(ACTION_PROPERTY, JMSEventMessage.Action.PUT.name());
        message.setStringProperty(CACHE_NAME_PROPERTY, "sampleCacheAsync");
        message.setStringProperty(MIME_TYPE_PROPERTY, "application/x-greg");
        message.setStringProperty(KEY_PROPERTY, "1234");


        Topic topic = publisherSession.createTopic("EhcacheTopicDest");
        TopicPublisher publisher = publisherSession.createPublisher(topic);
        publisher.send(message);

        connection.stop();

        Thread.sleep(100);


        MimeTypeByteArray payload = ((MimeTypeByteArray)manager1.getCache("sampleCacheAsync").get("1234").getObjectValue());
        assertEquals("application/x-greg", payload.getMimeType());
        assertEquals(new String(bytes), new String(payload.getValue()));
    }

    @Test
    public void testNonCachePublisherByteMessageNoMimeTypePut() throws JMSException, InterruptedException {

        byte[] bytes = "these are bytes".getBytes();
        TopicConnection connection = getMQConnection();
        connection.start();

        TopicSession publisherSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        BytesMessage message = publisherSession.createBytesMessage();
        message.writeBytes(bytes);
        message.setStringProperty(ACTION_PROPERTY, JMSEventMessage.Action.PUT.name());
        message.setStringProperty(CACHE_NAME_PROPERTY, "sampleCacheAsync");
//        message.setStringProperty(MIME_TYPE_PROPERTY, "application/octet-stream");
        message.setStringProperty(KEY_PROPERTY, "1234");


        Topic topic = publisherSession.createTopic("EhcacheTopicDest");
        TopicPublisher publisher = publisherSession.createPublisher(topic);
        publisher.send(message);

        connection.stop();

        Thread.sleep(100);


        MimeTypeByteArray payload = ((MimeTypeByteArray)manager1.getCache("sampleCacheAsync").get("1234").getObjectValue());
        assertEquals("application/octet-stream", payload.getMimeType());
        assertEquals(new String(bytes), new String(payload.getValue()));
    }


    @Test
    public void testNonCachePublisherTextMessagePut() throws JMSException, InterruptedException {

        TopicConnection connection = getMQConnection();
        connection.start();

        TopicSession publisherSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        String value = "<?xml version=\"1.0\"?>\n" +
                "<oldjoke>\n" +
                "<burns>Say <quote>goodnight</quote>,\n" +
                "Gracie.</burns>\n" +
                "<allen><quote>Goodnight, \n" +
                "Gracie.</quote></allen>\n" +
                "<applause/>\n" +
                "</oldjoke>";

        TextMessage message = publisherSession.createTextMessage(value);
        message.setStringProperty(ACTION_PROPERTY, JMSEventMessage.Action.PUT.name());
        message.setStringProperty(CACHE_NAME_PROPERTY, "sampleCacheAsync");
        message.setStringProperty(MIME_TYPE_PROPERTY, "text/x-greg");
        message.setStringProperty(KEY_PROPERTY, "1234");


        Topic topic = publisherSession.createTopic("EhcacheTopicDest");
        TopicPublisher publisher = publisherSession.createPublisher(topic);
        publisher.send(message);

        connection.stop();

        Thread.sleep(100);


        MimeTypeByteArray payload = ((MimeTypeByteArray)manager1.getCache("sampleCacheAsync").get("1234").getObjectValue());
        assertEquals("text/x-greg", payload.getMimeType());
        assertEquals(value, new String(payload.getValue()));
    }

    @Test
    public void testNonCachePublisherTextMessageNoMimeTypePut() throws JMSException, InterruptedException {

        TopicConnection connection = getMQConnection();
        connection.start();

        TopicSession publisherSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        String value = "this is a string";
        TextMessage message = publisherSession.createTextMessage(value);
        message.setStringProperty(ACTION_PROPERTY, JMSEventMessage.Action.PUT.name());
        message.setStringProperty(CACHE_NAME_PROPERTY, "sampleCacheAsync");
//        message.setStringProperty(MIME_TYPE_PROPERTY, "text/plain");
        message.setStringProperty(KEY_PROPERTY, "1234");


        Topic topic = publisherSession.createTopic("EhcacheTopicDest");
        TopicPublisher publisher = publisherSession.createPublisher(topic);
        publisher.send(message);

        connection.stop();

        Thread.sleep(100);


        MimeTypeByteArray payload = ((MimeTypeByteArray)manager1.getCache("sampleCacheAsync").get("1234").getObjectValue());
        assertEquals("text/plain", payload.getMimeType());
        assertEquals(value, new String(payload.getValue()));
    }



    @Test
    public void testNonCachePublisherObjectMessageRemove() throws JMSException, InterruptedException {

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

@Test
    public void testNonCachePublisherBytesMessageRemove() throws JMSException, InterruptedException {

        //make sure there is an element
        testNonCachePublisherElementMessagePut();
        TopicConnection connection = getMQConnection();
        connection.start();

        TopicSession publisherSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        BytesMessage message = publisherSession.createBytesMessage();
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
    public void testNonCachePublisherTextMessageRemove() throws JMSException, InterruptedException {

        //make sure there is an element
        testNonCachePublisherElementMessagePut();
        TopicConnection connection = getMQConnection();
        connection.start();

        TopicSession publisherSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        TextMessage message = publisherSession.createTextMessage();
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
    public void testNonCachePublisherObjectMessageRemoveAll() throws JMSException, InterruptedException {

        //make sure there is an element
        testNonCachePublisherElementMessagePut();
        TopicConnection connection = getMQConnection();
        connection.start();

        TopicSession publisherSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        ObjectMessage message = publisherSession.createObjectMessage();
        message.setStringProperty(ACTION_PROPERTY, JMSEventMessage.Action.REMOVE_ALL.name());
        message.setStringProperty(CACHE_NAME_PROPERTY, "sampleCacheAsync");

        Topic topic = publisherSession.createTopic("EhcacheTopicDest");
        TopicPublisher publisher = publisherSession.createPublisher(topic);
        publisher.send(message);

        connection.stop();

        Thread.sleep(100);


        assertEquals(null, manager1.getCache("sampleCacheAsync").get("1234"));
        assertEquals(null, manager2.getCache("sampleCacheAsync").get("1234"));
    }

    @Test
    public void testNonCachePublisherElementMessageRemoveAll() throws JMSException, InterruptedException {

        //make sure there is an element
        testNonCachePublisherElementMessagePut();
        TopicConnection connection = getMQConnection();
        connection.start();

        TopicSession publisherSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        ObjectMessage message = publisherSession.createObjectMessage(new Element("1", "dog"));
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

    @Test
    public void testNonCachePublisherTextMessageRemoveAll() throws JMSException, InterruptedException {

        //make sure there is an element
        testNonCachePublisherElementMessagePut();
        TopicConnection connection = getMQConnection();
        connection.start();

        TopicSession publisherSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        TextMessage message = publisherSession.createTextMessage();
        message.setStringProperty(ACTION_PROPERTY, JMSEventMessage.Action.REMOVE_ALL.name());
        message.setStringProperty(CACHE_NAME_PROPERTY, "sampleCacheAsync");


        Topic topic = publisherSession.createTopic("EhcacheTopicDest");
        TopicPublisher publisher = publisherSession.createPublisher(topic);
        publisher.send(message);

        connection.stop();

        Thread.sleep(100);


        assertEquals(null, manager1.getCache("sampleCacheAsync").get("1234"));
        assertEquals(null, manager2.getCache("sampleCacheAsync").get("1234"));
    }

    @Test
    public void testNonCachePublisherBytesMessageRemoveAll() throws JMSException, InterruptedException {

        //make sure there is an element
        testNonCachePublisherElementMessagePut();
        TopicConnection connection = getMQConnection();
        connection.start();

        TopicSession publisherSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        BytesMessage message = publisherSession.createBytesMessage();
        message.setStringProperty(ACTION_PROPERTY, JMSEventMessage.Action.REMOVE_ALL.name());
        message.setStringProperty(CACHE_NAME_PROPERTY, "sampleCacheAsync");


        Topic topic = publisherSession.createTopic("EhcacheTopicDest");
        TopicPublisher publisher = publisherSession.createPublisher(topic);
        publisher.send(message);

        connection.stop();

        Thread.sleep(100);


        assertEquals(null, manager1.getCache("sampleCacheAsync").get("1234"));
        assertEquals(null, manager2.getCache("sampleCacheAsync").get("1234"));
    }


    /**
     * Malformed Test - no properties at all set
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
     * Malformed test
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

    /**
     * Gets a connection without using JNDI, so that it is fully independent.
     * @throws JMSException
     */
    private TopicConnection getMQConnection() throws JMSException {
        com.sun.messaging.ConnectionFactory factory = new com.sun.messaging.ConnectionFactory();
        factory.setProperty(ConnectionConfiguration.imqAddressList, "localhost:7676");
        factory.setProperty(ConnectionConfiguration.imqReconnectEnabled, "true");
        TopicConnection myConnection = factory.createTopicConnection();
        return myConnection;
    }


}
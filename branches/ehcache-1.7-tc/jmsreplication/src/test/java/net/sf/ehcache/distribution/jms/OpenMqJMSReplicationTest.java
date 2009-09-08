package net.sf.ehcache.distribution.jms;

import com.sun.messaging.ConnectionConfiguration;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.MimeTypeByteArray;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import static net.sf.ehcache.distribution.jms.JMSEventMessage.ACTION_PROPERTY;
import static net.sf.ehcache.distribution.jms.JMSEventMessage.CACHE_NAME_PROPERTY;
import static net.sf.ehcache.distribution.jms.JMSEventMessage.KEY_PROPERTY;
import static net.sf.ehcache.distribution.jms.JMSEventMessage.MIME_TYPE_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.Before;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import java.io.Serializable;
import java.util.Date;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Run the tests using Open MQ
 * The create_administered_objects needs to have been run first
 *
 * @author Greg Luck
 */
public class OpenMqJMSReplicationTest extends ActiveMQJMSReplicationTest {

    private static final Logger LOG = Logger.getLogger(OpenMqJMSReplicationTest.class.getName());

    protected String getConfigurationFile() {
        return "distribution/jms/ehcache-distributed-jms-openmq.xml";
    }


    @Test
    public void testNonCachePublisherElementMessagePut() throws JMSException, InterruptedException {

        Element payload = new Element("1234", "dog");
        TopicConnection connection = getMQConnection();
        connection.start();

        TopicSession publisherSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

        ObjectMessage message = publisherSession.createObjectMessage(payload);
        message.setStringProperty(ACTION_PROPERTY, Action.PUT.name());
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
        message.setStringProperty(ACTION_PROPERTY, Action.PUT.name());
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
        message.setStringProperty(ACTION_PROPERTY, Action.PUT.name());
        message.setStringProperty(CACHE_NAME_PROPERTY, "sampleCacheAsync");
        message.setStringProperty(MIME_TYPE_PROPERTY, "application/x-greg");
        message.setStringProperty(KEY_PROPERTY, "1234");


        Topic topic = publisherSession.createTopic("EhcacheTopicDest");
        TopicPublisher publisher = publisherSession.createPublisher(topic);
        publisher.send(message);

        connection.stop();

        Thread.sleep(100);


        MimeTypeByteArray payload = ((MimeTypeByteArray) manager1.getCache("sampleCacheAsync").get("1234").getObjectValue());
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
        message.setStringProperty(ACTION_PROPERTY, Action.PUT.name());
        message.setStringProperty(CACHE_NAME_PROPERTY, "sampleCacheAsync");
//        message.setStringProperty(MIME_TYPE_PROPERTY, "application/octet-stream");
        message.setStringProperty(KEY_PROPERTY, "1234");


        Topic topic = publisherSession.createTopic("EhcacheTopicDest");
        TopicPublisher publisher = publisherSession.createPublisher(topic);
        publisher.send(message);

        connection.stop();

        Thread.sleep(100);


        MimeTypeByteArray payload = ((MimeTypeByteArray) manager1.getCache("sampleCacheAsync").get("1234").getObjectValue());
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
        message.setStringProperty(ACTION_PROPERTY, Action.PUT.name());
        message.setStringProperty(CACHE_NAME_PROPERTY, "sampleCacheAsync");
        message.setStringProperty(MIME_TYPE_PROPERTY, "text/x-greg");
        message.setStringProperty(KEY_PROPERTY, "1234");


        Topic topic = publisherSession.createTopic("EhcacheTopicDest");
        TopicPublisher publisher = publisherSession.createPublisher(topic);
        publisher.send(message);

        connection.stop();

        Thread.sleep(100);


        MimeTypeByteArray payload = ((MimeTypeByteArray) manager1.getCache("sampleCacheAsync").get("1234").getObjectValue());
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
        message.setStringProperty(ACTION_PROPERTY, Action.PUT.name());
        message.setStringProperty(CACHE_NAME_PROPERTY, "sampleCacheAsync");
//        message.setStringProperty(MIME_TYPE_PROPERTY, "text/plain");
        message.setStringProperty(KEY_PROPERTY, "1234");


        Topic topic = publisherSession.createTopic("EhcacheTopicDest");
        TopicPublisher publisher = publisherSession.createPublisher(topic);
        publisher.send(message);

        connection.stop();

        Thread.sleep(100);


        MimeTypeByteArray payload = ((MimeTypeByteArray) manager1.getCache("sampleCacheAsync").get("1234").getObjectValue());
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
        message.setStringProperty(ACTION_PROPERTY, Action.REMOVE.name());
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
        message.setStringProperty(ACTION_PROPERTY, Action.REMOVE.name());
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
        message.setStringProperty(ACTION_PROPERTY, Action.REMOVE.name());
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
        message.setStringProperty(ACTION_PROPERTY, Action.REMOVE.name());
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
        message.setStringProperty(ACTION_PROPERTY, Action.REMOVE_ALL.name());
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
        message.setStringProperty(ACTION_PROPERTY, Action.REMOVE_ALL.name());
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
        message.setStringProperty(ACTION_PROPERTY, Action.REMOVE_ALL.name());
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
        message.setStringProperty(ACTION_PROPERTY, Action.REMOVE_ALL.name());
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
        message.setStringProperty(ACTION_PROPERTY, Action.REMOVE.name());
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
     *
     * @throws JMSException
     */
    private TopicConnection getMQConnection() throws JMSException {
        com.sun.messaging.ConnectionFactory factory = new com.sun.messaging.ConnectionFactory();
        factory.setProperty(ConnectionConfiguration.imqAddressList, "localhost:7676");
        factory.setProperty(ConnectionConfiguration.imqReconnectEnabled, "true");
        factory.setProperty(ConnectionConfiguration.imqDefaultUsername, "test");
        factory.setProperty(ConnectionConfiguration.imqDefaultPassword, "test");
        TopicConnection myConnection = factory.createTopicConnection();
        return myConnection;
    }


    @Test
    public void testGetConcurrent() throws Exception {

        final long maxTime = 5000;
        cacheName = SAMPLE_CACHE_SYNC;
        manager3.shutdown();
        manager4.shutdown();
        final Ehcache cache1 = manager1.getCache("sampleCacheNorep");
        final Ehcache cache2 = manager2.getCache("sampleCacheNorep");


        long start = System.currentTimeMillis();
        final List executables = new ArrayList();
        final Random random = new Random();


        //some of the time get data
        for (int i = 0; i < 50; i++) {
            final int i1 = i;
            final TestUtil.Executable executable = new TestUtil.Executable() {
                public void execute() throws Exception {


                    final Serializable key = "" + i1;
                    final Serializable value = new Date();
                    Element element = new Element(key, i1);

                    //Put
                    cache1.put(element);
                    Thread.sleep(1050);

                    //Should load from cache1
                    for (int i = 0; i < 20; i++) {
                        final TestUtil.StopWatch stopWatch = new TestUtil.StopWatch();
                        long start = stopWatch.getElapsedTime();
                        Element element2 = cache2.getWithLoader(key, null, null);
                        assertEquals(i1, element2.getValue());
                        cache2.remove(key);
                        long end = stopWatch.getElapsedTime();
                        long elapsed = end - start;
                        assertTrue("Get time outside of allowed time: " + elapsed, elapsed < maxTime);
                    }

                }
            };
            executables.add(executable);
        }


        TestUtil.runThreads(executables);
        long end = System.currentTimeMillis();
        LOG.info("Total time for the test: " + (end - start) + " ms");
    }


    /**
     * Same as get, but this one tests out a few things that can cause problems with message queues (and have been
     * reproduced with this test - until the code was corrected that is)
     * <p/>
     * 1. Do two loops of 1000 requests. If there is any resource leakage this will fail
     * 2. Find a UID so that the reqestor does not satisfy its own request
     * 3. Pause for 125 seconds between the two runs. Open MQ closes unused destinations after 120 seconds.
     */
    @Test
    public void testGetStability() throws InterruptedException {
        cacheName = SAMPLE_CACHE_SYNC;
        manager3.shutdown();
        manager4.shutdown();
        Ehcache cache1 = manager1.getCache("sampleCacheNorep");
        Ehcache cache2 = manager2.getCache("sampleCacheNorep");

        Serializable key = "1";
        Serializable value = new Date();
        Element element = new Element(key, value);

        //Put
        cache1.put(element);
        long version = element.getVersion();
        Thread.sleep(1050);


        //Should not have been replicated to cache2.
        Element element2 = cache2.get(key);
        assertEquals(null, element2);

        //Should load from cache1
        for (int i = 0; i < 1000; i++) {
            element2 = cache2.getWithLoader(key, null, null);
            assertEquals(value, element2.getValue());
            cache2.remove(key);
        }
    }

    /**
     * Manual test.
     * <p/>
     * Run the test, stop the message queue and then start the message queue. load should throw exceptions but then
     * start loading again shortly after the message queue restarts.
     */
    //@Test
    public void testGetMessageQueueFailure() throws InterruptedException {
        cacheName = SAMPLE_CACHE_SYNC;
        manager3.shutdown();
        manager4.shutdown();
        Ehcache cache1 = manager1.getCache("sampleCacheNorep");
        Ehcache cache2 = manager2.getCache("sampleCacheNorep");

        Serializable key = "1";
        Serializable value = new Date();
        Element element = new Element(key, value);

        //Put
        cache1.put(element);
        long version = element.getVersion();
        Thread.sleep(1050);


        //Should not have been replicated to cache2.
        Element element2 = cache2.get(key);
        assertEquals(null, element2);

        //Should load from cache1
        for (int i = 0; i < 1000; i++) {
            Thread.sleep(2000);
            try {
                element2 = cache2.getWithLoader(key, null, null);
            } catch (CacheException e) {
                e.printStackTrace();
            }
            assertEquals(value, element2.getValue());
            cache2.remove(key);
        }
    }

    /**
     * Uses the JMSCacheLoader.
     * <p/>
     * We do not put an item in cache1, which does not replicate.
     * <p/>
     * We then do a get on cache2, which has a JMSCacheLoader which should ask the cluster for the answer.
     * If a cache does not have an element it should leave the message on the queue for the next node to process.
     */
    @Test
    public void testGetNull() throws InterruptedException {
        cacheName = SAMPLE_CACHE_SYNC;
        Ehcache cache1 = manager1.getCache("sampleCacheNorep");
        Ehcache cache2 = manager2.getCache("sampleCacheNorep");

        Serializable key = "1";

        //Should not have been replicated to cache2.
        Element element2 = cache2.get(key);
        assertEquals(null, element2);

        //Should load from cache1
        for (int i = 0; i < 100; i++) {
            Element element = cache2.getWithLoader(key, null, null);
            assertNull("" + element2, element2);
        }
    }

    @Test
    public void testOneWayReplicate() throws Exception {

        CacheManager managerA, managerB, managerC;

        String nonListeningConfigurationFile = "distribution/jms/ehcache-distributed-nonlistening-jms-openmq.xml";
        String listeningConfigurationFile = "distribution/jms/ehcache-distributed-jms-openmq.xml";

        managerA = new CacheManager(TestUtil.TEST_CONFIG_DIR + nonListeningConfigurationFile);
        managerA.setName("managerA");
        managerB = new CacheManager(TestUtil.TEST_CONFIG_DIR + listeningConfigurationFile);
        managerB.setName("managerB");
        managerC = new CacheManager(TestUtil.TEST_CONFIG_DIR + nonListeningConfigurationFile);
        managerC.setName("managerC");

        Element element = new Element("1", "value");
        managerA.getCache(SAMPLE_CACHE_ASYNC).put(element);

        Thread.sleep(1000);

        assertNotNull(managerB.getCache(SAMPLE_CACHE_ASYNC).get("1"));
        assertNull(managerC.getCache(SAMPLE_CACHE_ASYNC).get("1"));

        managerA.shutdown();
        managerB.shutdown();
        managerC.shutdown();
    }


    /**
     * Tests the JMSCacheLoader.
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

    /**
     * Tests the JMSCacheLoader.
     * <p/>
     * We put an item in cache1, which does not replicate.
     * <p/>
     * We then do a get on cache2, which has a JMSCacheLoader which should ask the cluster for the answer.
     * If a cache does not have an element it should leave the message on the queue for the next node to process.
     */
    @Override
    @Test
    public void testGetAll() throws InterruptedException {
        super.testGetAll();
    }
}
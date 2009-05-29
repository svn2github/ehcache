package net.sf.ehcache.distribution.jms;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import com.sun.messaging.ConnectionConfiguration;

import javax.jms.JMSException;
import javax.jms.Connection;
import javax.jms.Session;
import javax.jms.DeliveryMode;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Topic;
import java.util.Hashtable;
import java.util.logging.Logger;

/**
 * @author Greg Luck
 * @version $Id$
 */
public class OpenMessageQueueIntegrationTest {

    private static final Logger LOG = Logger.getLogger(OpenMessageQueueIntegrationTest.class.getName());


    String MYCF_LOOKUP_NAME = "MyConnectionFactory";
    String MYQUEUE_LOOKUP_NAME = "ehcache";

    ConnectionFactory connectionFactory;
    Connection connection;
    Session session;
    MessageProducer msgProducer;
    MessageConsumer msgConsumer;
    Topic queue;
    TextMessage msg, rcvMsg;



    /**
     * Can we create a connection to Open Message Queue
     */
    @Test
    public void testConnectionFactory() throws JMSException {
        com.sun.messaging.ConnectionFactory factory = new com.sun.messaging.ConnectionFactory();
        factory.setProperty(ConnectionConfiguration.imqAddressList, "localhost:7676");
        factory.setProperty(ConnectionConfiguration.imqReconnectEnabled, "true");
        factory.setProperty(ConnectionConfiguration.imqDefaultUsername, "test");
        factory.setProperty(ConnectionConfiguration.imqDefaultPassword, "test");
        Connection myConnection = factory.createConnection();
        assertNotNull(myConnection);
    }

    /**
     * Test with JNDI lookup using a previously created ObjectStore at /tmp
     */
    @Test
    public void testConnectionFactoryUsingJNDI() {
        Hashtable<String, String> env;
        Context ctx = null;
        String url = "file:///tmp";


        env = new Hashtable<String, String>();

        // Store the environment variables that tell JNDI which initial context
        // to use and where to find the provider.

        // For use with the File System JNDI Service Provider
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "com.sun.jndi.fscontext.RefFSContextFactory");
        env.put(Context.PROVIDER_URL, url);

        try {
            // Create the initial context.
            ctx = new InitialContext(env);
        } catch (NamingException ne) {
            System.err.println("Failed to create InitialContext.");
            System.err.println("The Context.PROVIDER_URL used/specified was: " + url);
            System.err.println("Please make sure that the path to the above URL exists");
            System.err.println("and matches with the objstore.attrs.java.naming.provider.url");
            System.err.println("property value specified in the imqobjmgr command files:");
            System.err.println("\tadd_cf.props");
            System.err.println("\tadd_q.props");
            System.err.println("\tdelete_cf.props");
            System.err.println("\tdelete_q.props");
            System.err.println("\tlist.props\n");


            System.err.println("\nThe exception details:");
            ne.printStackTrace();
            System.exit(-1);
        }

        LOG.info("");

        try {
            // Lookup my connection factory from the admin object store.
            // The name used here here must match the lookup name
            // used when the admin object was stored.
            LOG.info("Looking up Connection Factory object with lookup name: " + MYCF_LOOKUP_NAME);
            connectionFactory = (javax.jms.ConnectionFactory) ctx.lookup(MYCF_LOOKUP_NAME);
            LOG.info("Connection Factory object found.");
        } catch (NamingException ne) {
            System.err.println("Failed to lookup Connection Factory object.");
            System.err.println("Please make sure you have created the Connection Factory object using the command:");
            System.err.println("\timqobjmgr -i add_cf.props");

            System.err.println("\nThe exception details:");
            ne.printStackTrace();
            System.exit(-1);
        }

        LOG.info("");

        try {
            // Lookup my queue from the admin object store.
            // The name I search for here must match the lookup name used when
            // the admin object was stored.
            LOG.info("Looking up Queue object with lookup name: " + MYQUEUE_LOOKUP_NAME);
            queue = (javax.jms.Topic) ctx.lookup(MYQUEUE_LOOKUP_NAME);
            LOG.info("Queue object found.");
        } catch (NamingException ne) {
            System.err.println("Failed to lookup Queue object.");
            System.err.println("Please make sure you have created the Queue object using the command:");
            System.err.println("\timqobjmgr -i add_q.props");

            System.err.println("\nThe exception details:");
            ne.printStackTrace();
            System.exit(-1);
        }

        LOG.info("");

        try {
            LOG.info("Creating connection to broker.");
            connection = connectionFactory.createConnection();
            LOG.info("Connection to broker created.");
        } catch (JMSException e) {
            System.err.println("Failed to create connection.");
            System.err.println("Please make sure that the broker was started.");

            System.err.println("\nThe exception details:");
            e.printStackTrace();
            System.exit(-1);
        }

        LOG.info("");

        try {
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // Create the MessageProducer and MessageConsumer
            msgProducer = session.createProducer(queue);
            msgConsumer = session.createConsumer(queue);

            // Tell the provider to start sending messages.
            connection.start();

            msg = session.createTextMessage("Hello World");

            // Publish the message
            LOG.info("Publishing a message to Queue: " + queue.getTopicName());
            msgProducer.send(msg, DeliveryMode.NON_PERSISTENT, 4, 0);

            // Wait for it to be sent back.
            rcvMsg = (TextMessage) msgConsumer.receive();

            LOG.info("Received the following message: " + rcvMsg.getText());

            connection.close();

        } catch (JMSException e) {
            System.err.println("JMS Exception: " + e);
            e.printStackTrace();
            System.exit(-1);
        }
    }

}

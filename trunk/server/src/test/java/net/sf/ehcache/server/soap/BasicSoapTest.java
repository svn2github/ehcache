package net.sf.ehcache.server.soap;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.xml.sax.SAXException;

import javax.xml.ws.Endpoint;
import javax.xml.parsers.ParserConfigurationException;
import java.net.HttpURLConnection;
import java.io.IOException;

import net.sf.ehcache.server.HttpUtil;


/**
 * Tests the Soap server
 * @author Greg Luck
 * @version $Id$
 */
public class BasicSoapTest {
    private static Object implementor;
    private static String address;
    private static WebServiceThread webServiceThread;


    @BeforeClass
    public static void startService() {
        implementor = new EhcacheWebServiceEndpoint();
        address = "http://localhost:9000/temp";

        webServiceThread = new WebServiceThread();
        webServiceThread.start();
        assertTrue(webServiceThread.isAlive());


    }

    @AfterClass
    public static void stopService() {
        webServiceThread.interrupt();
    }


    /**
     * Used to initialise the debugger and run its monitoring in another thread so we can keep doing stuff
     */
    static class WebServiceThread extends Thread {

        /**
         * If this thread was constructed using a separate
         * <code>Runnable</code> run object, then that
         * <code>Runnable</code> object's <code>run</code> method is called;
         * otherwise, this method does nothing and returns.
         * <p/>
         * Subclasses of <code>Thread</code> should override this method.
         *
         * @see Thread#start()
         * @see Thread#stop()
         * @see Thread#Thread(ThreadGroup,
         *      Runnable, String)
         * @see Runnable#run()
         */
        public void run() {
            try {
                Endpoint.publish(address, implementor);
            } catch (Throwable e) {
                fail(e.getMessage());
            }
        }
    }

}

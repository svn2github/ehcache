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

    @Test
    public void testEhcacheWebServiceEndPointExists() throws IOException, ParserConfigurationException, SAXException {

        HttpURLConnection response = HttpUtil.get("http://localhost:9000/temp/EhcacheWebServiceEndpoint");
        assertEquals(200, response.getResponseCode());
        String responseBody = HttpUtil.inputStreamToText(response.getInputStream());
        assertTrue(responseBody.indexOf("Implementation class:") != 0);
    }

    @BeforeClass
    public static void startService() throws InterruptedException {
        implementor = new EhcacheWebServiceEndpoint();
        address = "http://localhost:9000/temp";

        webServiceThread = new WebServiceThread();
        webServiceThread.start();
        assertTrue(webServiceThread.isAlive());
        //Wait to start up
        Thread.sleep(5000);


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

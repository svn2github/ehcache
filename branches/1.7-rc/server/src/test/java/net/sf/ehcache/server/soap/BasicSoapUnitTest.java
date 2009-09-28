package net.sf.ehcache.server.soap;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Before;
import org.xml.sax.SAXException;

import javax.xml.ws.Endpoint;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;
import javax.xml.parsers.ParserConfigurationException;
import java.net.HttpURLConnection;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import net.sf.ehcache.server.HttpUtil;
import net.sf.ehcache.server.soap.jaxws.EhcacheWebServiceEndpointService;
import net.sf.ehcache.server.soap.jaxws.CacheException_Exception;
import net.sf.ehcache.server.soap.jaxws.IllegalStateException_Exception;


/**
 * Tests the Soap server. This relies on the lightweight http server.
 *
 * @author Greg Luck
 * @version $Id$
 */
public class BasicSoapUnitTest {
    private static Object implementor;
    private static String address;
    private static WebServiceThread webServiceThread;
    private static Endpoint endpoint;


    @Test
    public void testEhcacheWebServiceEndPointExists() throws IOException, ParserConfigurationException, SAXException, InterruptedException {

        HttpURLConnection response = HttpUtil.get("http://localhost:8080/ehcache/soap/EhcacheWebServiceEndpoint?wsdl");
        assertEquals(200, response.getResponseCode());
        String responseBody = HttpUtil.inputStreamToText(response.getInputStream());
        assertTrue(responseBody.indexOf("Implementation class:") != 0);
    }

    /**
     * Security should be enabled using XWSS.
     */
    @Test
    public void testEhcacheWebServiceEndPointSecurity() throws IOException, ParserConfigurationException, SAXException {

        net.sf.ehcache.server.soap.jaxws.EhcacheWebServiceEndpoint cacheService =
                new EhcacheWebServiceEndpointService().getEhcacheWebServiceEndpointPort();

        System.setProperty("Ron", "noR");

        //uncomment to run tests with security on. Must also rename XWSS config files to activate.
//         ((BindingProvider)cacheService).getRequestContext().put(BindingProvider.USERNAME_PROPERTY, "Ron");
//         ((BindingProvider)cacheService).getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, "noR");


        //invoke business method
        String result = cacheService.ping();
        assertEquals("pong", result);
    }


    @BeforeClass
    public static void startService() throws InterruptedException {
        implementor = new EhcacheWebServiceEndpoint();
        address = "http://localhost:8080/ehcache/soap/";

        webServiceThread = new WebServiceThread();
        webServiceThread.start();
        assertTrue(webServiceThread.isAlive());
        //Wait to start up

        Thread.sleep(10000);


    }

    @AfterClass
    public static void stopService() throws InterruptedException {
        endpoint.stop();
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
                endpoint = Endpoint.publish(address, implementor);
                System.out.println("Web Service listening at URI " + address);
            } catch (Throwable e) {
                fail(e.getMessage());
            }
        }


    }

}

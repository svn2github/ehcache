/**
 *  Copyright 2003-2009 Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache.server.soap;

import net.sf.ehcache.server.util.HttpUtil;
import net.sf.ehcache.server.soap.jaxws.EhcacheWebServiceEndpointService;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.ws.Endpoint;
import java.io.IOException;
import java.net.HttpURLConnection;


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


    private static final Logger LOG = LoggerFactory.getLogger(BasicSoapUnitTest.class);


    @Test
    public void testEhcacheWebServiceEndPointExists() throws Exception {

        HttpURLConnection response = HttpUtil.get("http://localhost:9090/ehcache/soap/EhcacheWebServiceEndpoint?wsdl");
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
        address = "http://localhost:9090/ehcache/soap/";

        webServiceThread = new WebServiceThread();
        webServiceThread.start();
        assertTrue(webServiceThread.isAlive());
        //Wait to start up

        Thread.sleep(15000);


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
                LOG.info("Web Service listening at URI " + address);
            } catch (Throwable e) {
                fail(e.getMessage());
            }
        }


    }

}

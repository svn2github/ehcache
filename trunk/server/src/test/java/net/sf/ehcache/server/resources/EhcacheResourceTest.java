package net.sf.ehcache.server.resources;

import org.junit.Test;
import static org.junit.Assert.*;

import com.sun.net.httpserver.HttpServer;
import com.sun.ws.rest.api.container.httpserver.HttpServerFactory;


/**
 * Tests the REST web resource using the lightweight http container
 *
 * @author Greg Luck
 * @version $Id$
 */
public class EhcacheResourceTest {
    private Object implementor;
    private String address;


    @Test
    public void testEhcacheResource() throws InterruptedException {

        RESTfulWebServiceThread restfulWebServiceThread = new RESTfulWebServiceThread();
        restfulWebServiceThread.start();
        assertTrue(restfulWebServiceThread.isAlive());

        Thread.sleep(30000);
        restfulWebServiceThread.interrupt();
    }


    /**
     * Used to initialise the debugger and run its monitoring in another thread so we can keep doing stuff
     */
    class RESTfulWebServiceThread extends Thread {

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

                HttpServer server = HttpServerFactory.create("http://localhost:9998/");
                server.start();

                System.out.println("Server running");
                System.out.println("Visit: http://localhost:9998/helloworld");
                System.out.println("Hit return to stop...");
                System.in.read();
                System.out.println("Stopping server");
                server.stop(0);
                System.out.println("Server stopped");
            } catch (Throwable t) {
                t.printStackTrace();
                fail(t.getMessage());
            }
        }
    }

}
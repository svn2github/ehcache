package net.sf.ehcache.server;

import org.junit.Test;

import javax.xml.ws.Endpoint;

import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: gluck
 * Date: May 20, 2008
 * Time: 9:50:20 AM
 * To change this template use File | Settings | File Templates.
 */
public class EhcacheTest extends TestCase {
    private Object implementor;
    private String address;


    @Test
    public void testEhcache() throws InterruptedException {
        implementor = new Ehcache();
        address = "http://localhost:9000/temp";

        WebServiceThread webServiceThread = new WebServiceThread();
        webServiceThread.start();
        assertTrue(webServiceThread.isAlive());

        Thread.sleep(2000);
        webServiceThread.interrupt();
    }


    /**
     * Used to initialise the debugger and run its monitoring in another thread so we can keep doing stuff
     */
    class WebServiceThread extends Thread {

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

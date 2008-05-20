package net.sf.ehcache.server.soap;

import org.junit.Test;
import static org.junit.Assert.*;

import javax.xml.ws.Endpoint;

import net.sf.ehcache.server.soap.Ehcache;


/**
 * @author Greg Luck
 * @version $Id$
 */
public class EhcacheTest {
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

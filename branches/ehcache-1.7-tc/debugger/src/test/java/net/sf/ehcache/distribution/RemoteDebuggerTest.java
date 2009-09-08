package net.sf.ehcache.distribution;

import junit.framework.TestCase;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

/**
 * A place holder to stop maven from falling over with an NPE
 */
public class RemoteDebuggerTest extends TestCase {

    private static final float JDK_1_5 = 1.5f;
    private static final int FIRST_THREE_CHARS = 3;

    private RemoteDebugger remoteDebugger;



    /**
     * Make sure main with no params does not explode
     */
    public void testMainEmpty() throws InterruptedException {
        RemoteDebugger.main(new String[] {});
    }

    /**
     * Make sure main with one param does not explode
     */
    public void testMainOneParamJunk() throws InterruptedException {
        RemoteDebugger.main(new String[] {"rubbish path"});
    }

    /**
     * Make sure main with one param does not explode
     */
    public void testMainOneParamCorrect() throws InterruptedException {
        RemoteDebugger.main(new String[] {"/Users/gluck/work/ehcache/debugger/target/test-classes/ehcache-distributed6.xml"});

    }

    /**
     * Make sure main with two params does not explode
     */
    public void testMainTwoParams() throws InterruptedException {
        RemoteDebugger.main(new String[] {"rubbish path", "rubbish name"});
    }

    /**
     * Make sure main with three params does not explode
     */
    public void testMainThreeParams() throws InterruptedException {
        RemoteDebugger.main(new String[] {"rubbish path", "rubbish name", "only two allowed"});
    }


    /**
     * JDK Bug Id 4267864 affecting JDKs limits the number of RMI registries to one per virtual
     * machine. Because tests rely on creating multiple they will only work on JDK1.5.
     *
     * This method is used to not run the affected tests on JDK1.4.
     * @return true if the JDK is limited to one RMI Registry per VM, else false
     */
    public static boolean isSingleRMIRegistryPerVM() {
        String version = System.getProperty("java.version");
        String majorVersion = version.substring(0, FIRST_THREE_CHARS);
        float majorVersionFloat = Float.parseFloat(majorVersion);
        return majorVersionFloat < JDK_1_5;
    }

    /**
     * The real deal
     */
    public void testMonitorPeerCacheManager() throws InterruptedException {

        if (isSingleRMIRegistryPerVM()) {
            return;
        }



        remoteDebugger = new RemoteDebugger("/Users/gluck/work/ehcache/debugger/target/test-classes/ehcache-distributed6.xml", "sampleCache1");
        RemoteDebuggerThread remoteDebuggerThread = new RemoteDebuggerThread();
        remoteDebuggerThread.start();
        assertTrue(remoteDebuggerThread.isAlive());
        assertTrue(remoteDebugger.getDistributedCacheNames().length > 50);

        CacheManager manager6 = new CacheManager(this.getClass().getResource("/ehcache-distributed6.xml"));
        Cache sendingCache = manager6.getCache("sampleCache1");


        //Allow the cluster to form and therefore the debugger CacheManager to connect
        //Have bootstrap which automatically waits for cluster formation

        ConsolePrintingCacheEventListener consolePrintingCacheEventListener =
                remoteDebugger.getConsolePrintingCacheEventListener();
        sendingCache.put(new Element("this is an id", "this is a value"));
        Thread.sleep(1000);
        assertEquals(1, consolePrintingCacheEventListener.getEventsReceivedCount());

        sendingCache.put(new Element("this is an id", "this is a value"));
        Thread.sleep(1000);
        assertEquals(2, consolePrintingCacheEventListener.getEventsReceivedCount());

        sendingCache.remove("this is an id");
        Thread.sleep(1000);
        assertEquals(3, consolePrintingCacheEventListener.getEventsReceivedCount());

        sendingCache.removeAll();
        Thread.sleep(1000);
        assertEquals(4, consolePrintingCacheEventListener.getEventsReceivedCount());
    }

    /**
     * Used to initialise the debugger and run its monitoring in another thread so we can keep doing stuff
     */
    class RemoteDebuggerThread extends Thread {

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
                remoteDebugger.init();
            } catch (Throwable e) {
                fail(e.getMessage());
            }
        }
    }

}

package net.sf.ehcache.distribution;

import net.sf.ehcache.AbstractCachePerfTest;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.CachePerfTest;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.StopWatch;
import net.sf.ehcache.management.ManagementService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Alex Snaps
 */
public class RMICacheReplicatorPerfTest extends AbstractCachePerfTest {


    private static final Logger LOG = LoggerFactory.getLogger(RMICacheReplicatorPerfTest.class.getName());


    /**
     * CacheManager 1 in the cluster
     */
    protected CacheManager manager1;
    /**
     * CacheManager 2 in the cluster
     */
    protected CacheManager manager2;
    /**
     * CacheManager 3 in the cluster
     */
    protected CacheManager manager3;
    /**
     * CacheManager 4 in the cluster
     */
    protected CacheManager manager4;
    /**
     * CacheManager 5 in the cluster
     */
    protected CacheManager manager5;
    /**
     * CacheManager 6 in the cluster
     */
    protected CacheManager manager6;

    /**
     * The name of the cache under test
     */
    protected String cacheName = "sampleCache1";
    /**
     * CacheManager 1 of 2s cache being replicated
     */
    protected Ehcache cache1;

    /**
     * CacheManager 2 of 2s cache being replicated
     */
    protected Ehcache cache2;

    /**
     * {@inheritDoc}
     * Sets up two caches: cache1 is local. cache2 is to be receive updates
     *
     * @throws Exception
     */
    @Override
    @Before
    public void setUp() throws Exception {

        //Required to get SoftReference tests to pass. The VM clean up SoftReferences rather than allocating
        // memory to -Xmx!
//        forceVMGrowth();
//        System.gc();
        MulticastKeepaliveHeartbeatSender.setHeartBeatInterval(1000);

        manager1 = new CacheManager(AbstractCachePerfTest.TEST_CONFIG_DIR + "ehcache-distributed1.xml");
        manager2 = new CacheManager(AbstractCachePerfTest.TEST_CONFIG_DIR + "ehcache-distributed2.xml");
        manager3 = new CacheManager(AbstractCachePerfTest.TEST_CONFIG_DIR + "ehcache-distributed3.xml");
        manager4 = new CacheManager(AbstractCachePerfTest.TEST_CONFIG_DIR + "ehcache-distributed4.xml");
        manager5 = new CacheManager(AbstractCachePerfTest.TEST_CONFIG_DIR + "ehcache-distributed5.xml");

        //manager6 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed-jndi6.xml");

        //allow cluster to be established
        Thread.sleep(1020);

        cache1 = manager1.getCache(cacheName);
        cache1.removeAll();

        cache2 = manager2.getCache(cacheName);
        cache2.removeAll();

        //enable distributed removeAlls to finish
        Thread.sleep(1500);


    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    @Override
    @After
    public void tearDown() throws Exception {

        if (manager1 != null) {
            manager1.shutdown();
        }
        if (manager2 != null) {
            manager2.shutdown();
        }
        if (manager3 != null) {
            manager3.shutdown();
        }
        if (manager4 != null) {
            manager4.shutdown();
        }
        if (manager5 != null) {
            manager5.shutdown();
        }
        if (manager6 != null) {
            manager6.shutdown();
        }
        Thread.sleep(2000);

        List threads = enumerateThreads();
        for (int i = 0; i < threads.size(); i++) {
            Thread thread = (Thread) threads.get(i);
            if (thread.getName().equals("Replication Thread")) {
                fail("There should not be any replication threads running after shutdown");
            }
        }

    }

    public static List enumerateThreads() {

        /**
         * A class for visiting threads
         */
        class ThreadVisitor {

            private List threadList = new ArrayList();

            // This method recursively visits all thread groups under `group'.
            private void visit(ThreadGroup group, int level) {
                // Get threads in `group'
                int numThreads = group.activeCount();
                Thread[] threads = new Thread[numThreads * 2];
                numThreads = group.enumerate(threads, false);

                // Enumerate each thread in `group'
                for (int i = 0; i < numThreads; i++) {
                    // Get thread
                    Thread thread = threads[i];
                    threadList.add(thread);
                }

                // Get thread subgroups of `group'
                int numGroups = group.activeGroupCount();
                ThreadGroup[] groups = new ThreadGroup[numGroups * 2];
                numGroups = group.enumerate(groups, false);

                // Recursively visit each subgroup
                for (int i = 0; i < numGroups; i++) {
                    visit(groups[i], level + 1);
                }
            }
        }

        // Find the root thread group
        ThreadGroup root = Thread.currentThread().getThreadGroup().getParent();
        while (root.getParent() != null) {
            root = root.getParent();
        }

        // Visit each thread group
        ThreadVisitor visitor = new ThreadVisitor();
        visitor.visit(root, 0);
        return visitor.threadList;
    }    


    /**
     * Performance and capacity tests.
     * <p/>
     * The numbers given are for the remote peer tester (java -jar ehcache-1.x-remote-debugger.jar ehcache-distributed1.xml)
     * running on a 10Mbit ethernet network and are measured from the time the peer starts receiving to when
     * it has fully received.
     * <p/>
     * r37 and earlier - initial implementation
     * 38 seconds to get all notifications with 6 peers, 2000 Elements and 400 byte payload
     * 18 seconds to get all notifications with 2 peers, 2000 Elements and 400 byte payload
     * 40 seconds to get all notifications with 2 peers, 2000 Elements and 10k payload
     * 22 seconds to get all notifications with 2 peers, 2000 Elements and 1k payload
     * 26 seconds to get all notifications with 2 peers, 200 Elements and 100k payload
     * <p/>
     * r38 - RMI stub lookup on registration rather than at each lookup. Saves quite a few lookups. Also change to 5 second heartbeat
     * 38 seconds to get 2000 notifications with 6 peers, Elements with 400 byte payload (1 second heartbeat)
     * 16 seconds to get 2000 notifications with 6 peers, Elements with 400 byte payload (5 second heartbeat)
     * 13 seconds to get 2000 notifications with 2 peers, Elements with 400 byte payload
     * <p/>
     * r39 - Batching asyn replicator. Send all queued messages in one RMI call once per second.
     * 2 seconds to get 2000 notifications with 6 peers, Elements with 400 byte payload (5 second heartbeat)
     */
    @Test
    public void testBigPutsProgagatesAsynchronous() throws CacheException, InterruptedException {

        //Give everything a chance to startup
        //Thread.sleep(10000);
        StopWatch stopWatch = new StopWatch();
        Integer index = null;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 1000; j++) {
                index = Integer.valueOf(((1000 * i) + j));
                cache1.put(new Element(index,
                        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            }

        }
        long elapsed = stopWatch.getElapsedTime();
        long putTime = ((elapsed / 1000));
        LOG.info("Put Elapsed time: " + putTime);
        //assertTrue(putTime < 8);

        assertEquals(2000, cache1.getSize());

        Thread.sleep(2000);
        assertEquals(2000, manager2.getCache("sampleCache1").getSize());
        assertEquals(2000, manager3.getCache("sampleCache1").getSize());
        assertEquals(2000, manager4.getCache("sampleCache1").getSize());
        assertEquals(2000, manager5.getCache("sampleCache1").getSize());

    }


    /**
     * Performance and capacity tests.
     * <p/>
     */

    @Test
    public void testBootstrap() throws CacheException, InterruptedException, RemoteException {

        //load up some data
        StopWatch stopWatch = new StopWatch();
        Integer index = null;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 1000; j++) {
                index = Integer.valueOf(((1000 * i) + j));
                cache1.put(new Element(index,
                        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            }

        }
        long elapsed = stopWatch.getElapsedTime();
        long putTime = ((elapsed / 1000));
        LOG.info("Put Elapsed time: " + putTime);

        assertEquals(2000, cache1.getSize());

        Thread.sleep(7000);
        assertEquals(2000, manager2.getCache("sampleCache1").getSize());
        assertEquals(2000, manager3.getCache("sampleCache1").getSize());
        assertEquals(2000, manager4.getCache("sampleCache1").getSize());
        assertEquals(2000, manager5.getCache("sampleCache1").getSize());

        //now test bootstrap
        manager1.addCache("bootStrapResults");
        Cache cache = manager1.getCache("bootStrapResults");
        List cachePeers = manager1.getCacheManagerPeerProvider("RMI").listRemoteCachePeers(cache1);
        CachePeer cachePeer = (CachePeer) cachePeers.get(0);

        List keys = cachePeer.getKeys();
        assertEquals(2000, keys.size());

        Element firstElement = cachePeer.getQuiet((Serializable) keys.get(0));
        long size = firstElement.getSerializedSize();
        assertEquals(514, size);

        int chunkSize = (int) (5000000 / size);

        List requestChunk = new ArrayList();
        for (int i = 0; i < keys.size(); i++) {
            Serializable serializable = (Serializable) keys.get(i);
            requestChunk.add(serializable);
            if (requestChunk.size() == chunkSize) {
                fetchAndPutElements(cache, requestChunk, cachePeer);
                requestChunk.clear();
            }
        }
        //get leftovers
        fetchAndPutElements(cache, requestChunk, cachePeer);

        assertEquals(keys.size(), cache.getSize());

    }

    private void fetchAndPutElements(Ehcache cache, List requestChunk, CachePeer cachePeer) throws RemoteException {
        List receivedChunk = cachePeer.getElements(requestChunk);
        for (int i = 0; i < receivedChunk.size(); i++) {
            Element element = (Element) receivedChunk.get(i);
            assertNotNull(element);
            cache.put(element, true);
        }

    }


    /**
     * Drive everything to point of breakage within a 64MB VM.
     */
    public void xTestHugePutsBreaksAsynchronous() throws CacheException, InterruptedException {

        //Give everything a chance to startup
        StopWatch stopWatch = new StopWatch();
        Integer index = null;
        for (int i = 0; i < 500; i++) {
            for (int j = 0; j < 1000; j++) {
                index = Integer.valueOf(((1000 * i) + j));
                cache1.put(new Element(index,
                        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            }

        }
        long elapsed = stopWatch.getElapsedTime();
        long putTime = ((elapsed / 1000));
        LOG.info("Put Elapsed time: " + putTime);
        //assertTrue(putTime < 8);

        assertEquals(100000, cache1.getSize());

        Thread.sleep(100000);
        assertEquals(20000, manager2.getCache("sampleCache1").getSize());
        assertEquals(20000, manager3.getCache("sampleCache1").getSize());
        assertEquals(20000, manager4.getCache("sampleCache1").getSize());
        assertEquals(20000, manager5.getCache("sampleCache1").getSize());

    }


    /**
     * Performance and capacity tests.
     * <p/>
     * The numbers given are for the remote peer tester (java -jar ehcache-1.x-remote-debugger.jar ehcache-distributed1.xml)
     * running on a 10Mbit ethernet network and are measured from the time the peer starts receiving to when
     * it has fully received.
     * <p/>
     * 4 seconds to get all remove notifications with 6 peers, 5000 Elements and 400 byte payload
     */
    @Test
    public void testBigRemovesProgagatesAsynchronous() throws CacheException, InterruptedException {

        //Give everything a chance to startup
        Integer index = null;
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 1000; j++) {
                index = Integer.valueOf(((1000 * i) + j));
                cache1.put(new Element(index,
                        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            }

        }


        Ehcache[] caches = {
            cache1,
            manager2.getCache("sampleCache1"),
            manager3.getCache("sampleCache1"),
            manager4.getCache("sampleCache1"),
            manager5.getCache("sampleCache1") };

        waitForCacheSize(5000, 25, caches);
        //Let the disk stores catch up before the next stage of the test
        Thread.sleep(2000);

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 1000; j++) {
                cache1.remove(Integer.valueOf(((1000 * i) + j)));
            }
        }

        long timeForPropagate = waitForCacheSize(0, 25, caches);
        LOG.info("Remove Elapsed time: " + timeForPropagate);

    }

    public long waitForCacheSize(long size, int maxSeconds, Ehcache... caches) throws InterruptedException {

        StopWatch stopWatch = new StopWatch();
        while(checkForCacheSize(size, caches)) {
            Thread.sleep(500);
            if(stopWatch.getElapsedTime() > maxSeconds * 1000) {
                fail("Caches still haven't reached the expected size after " + maxSeconds + " seconds");
            }
        }

        return stopWatch.getElapsedTime();
    }

    private boolean checkForCacheSize(long size, Ehcache... caches) {
        boolean sizeReached = true;
        for (Ehcache cache : caches) {
            if(cache.getSize() != size) {
                sizeReached = false;
                break;
            }
        }
        return sizeReached;
    }


    /**
     * Performance and capacity tests.
     * <p/>
     * 5 seconds to send all notifications synchronously with 5 peers, 2000 Elements and 400 byte payload
     * The numbers given below are for the remote peer tester (java -jar ehcache-1.x-remote-debugger.jar ehcache-distributed1.xml)
     * running on a 10Mbit ethernet network and are measured from the time the peer starts receiving to when
     * it has fully received.
     */
    @Test
    public void testBigPutsProgagatesSynchronous() throws CacheException, InterruptedException {

        //Give everything a chance to startup
        StopWatch stopWatch = new StopWatch();
        Integer index;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 1000; j++) {
                index = Integer.valueOf(((1000 * i) + j));
                manager1.getCache("sampleCache3").put(new Element(index,
                        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            }

        }
        long elapsed = stopWatch.getElapsedTime();
        long putTime = ((elapsed / 1000));
        LOG.info("Put and Propagate Synchronously Elapsed time: " + putTime + " seconds");

        assertEquals(2000, manager1.getCache("sampleCache3").getSize());
        assertEquals(2000, manager2.getCache("sampleCache3").getSize());
        assertEquals(2000, manager3.getCache("sampleCache3").getSize());
        assertEquals(2000, manager4.getCache("sampleCache3").getSize());
        assertEquals(2000, manager5.getCache("sampleCache3").getSize());

    }

    /**
     * Enables long stabilty runs using replication to be done.
     * <p/>
     * This test has been run in a profile for 15 hours without any observed issues.
     *
     * @throws InterruptedException
     */
    public void manualStabilityTest() throws InterruptedException {
        forceVMGrowth();

        ManagementService.registerMBeans(manager3, createMBeanServer(), true, true, true, true, true);
        while (true) {
            testBigPutsProgagatesAsynchronous();
        }
    }

    /**
     * Non JUnit invocation of stability test to get cleaner run
     *
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) throws Exception {
        RMICacheReplicatorPerfTest replicatorTest = new RMICacheReplicatorPerfTest();
        replicatorTest.setUp();
        replicatorTest.manualStabilityTest();
    }


}

/**
 *  Copyright 2003-2010 Terracotta, Inc.
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

package net.sf.ehcache.distribution;

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.StopWatch;
import net.sf.ehcache.ThreadKiller;
import net.sf.ehcache.event.CountingCacheEventListener;
import net.sf.ehcache.management.ManagementService;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests replication of Cache events
 * <p/>
 * Note these tests need a live network interface running in multicast mode to work
 * <p/>
 * If running involving RMIAsynchronousCacheReplicator individually the test will fail because
 * the VM will gobble up the SoftReferences rather than allocating more memory. Uncomment the
 * forceVMGrowth() method usage in setup.
 *
 * @author Greg Luck
 * @version $Id$
 */


//
// Please close jira MNK-1377 after fixing ignored tests below
//

public class RMICacheReplicatorTest extends AbstractCacheTest {


    /**
     * A value to represent replicate asynchronously
     */
    protected static final boolean ASYNCHRONOUS = true;

    /**
     * A value to represent replicate synchronously
     */
    protected static final boolean SYNCHRONOUS = false;

    private static final Logger LOG = LoggerFactory.getLogger(RMICacheReplicatorTest.class.getName());


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
     * Allows setup to be the same
     */
    protected String cacheNameBase = "ehcache-distributed";

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

        CountingCacheEventListener.resetCounters();
        manager1 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed1.xml");
        manager2 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed2.xml");
        manager3 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed3.xml");
        manager4 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed4.xml");
        manager5 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed5.xml");

        //manager6 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed-jndi6.xml");

        //allow cluster to be established
        Thread.sleep(1020);

        cache1 = manager1.getCache(cacheName);
        cache1.removeAll();

        cache2 = manager2.getCache(cacheName);
        cache2.removeAll();

        //enable distributed removeAlls to finish
        waitForPropagate();


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

        List threads = JVMUtil.enumerateThreads();
        for (int i = 0; i < threads.size(); i++) {
            Thread thread = (Thread) threads.get(i);
            if (thread.getName().equals("Replication Thread")) {
                fail("There should not be any replication threads running after shutdown");
            }
        }

    }

    /**
     * 5 cache managers should means that each cache has four remote peers
     */
    
    @Test
    public void testRemoteCachePeersEqualsNumberOfCacheManagersInCluster() {

        CacheManagerPeerProvider provider = manager1.getCacheManagerPeerProvider("RMI");
        List remotePeersOfCache1 = provider.listRemoteCachePeers(cache1);
        assertEquals(4, remotePeersOfCache1.size());
    }

    /**
     * Does a new cache manager in the cluster get detected?
     */
    
    @Test
    public void testRemoteCachePeersDetectsNewCacheManager() throws InterruptedException {

        CacheManagerPeerProvider provider = manager1.getCacheManagerPeerProvider("RMI");
        List remotePeersOfCache1 = provider.listRemoteCachePeers(cache1);
        assertEquals(4, remotePeersOfCache1.size());

        //Add new CacheManager to cluster
        manager6 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed6.xml");

        //Allow detection to occur
        Thread.sleep(10020);

        remotePeersOfCache1 = provider.listRemoteCachePeers(cache1);
        assertEquals(5, remotePeersOfCache1.size());
    }

    /**
     * Does a down cache manager in the cluster get removed?
     */
    
    @Test
    public void testRemoteCachePeersDetectsDownCacheManager() throws InterruptedException {

        CacheManagerPeerProvider provider = manager1.getCacheManagerPeerProvider("RMI");
        List remotePeersOfCache1 = provider.listRemoteCachePeers(cache1);
        assertEquals(4, remotePeersOfCache1.size());

        //Drop a CacheManager from the cluster
        manager5.shutdown();

        //Allow change detection to occur. Heartbeat 1 second and is not stale until 5000
        Thread.sleep(11020);
        remotePeersOfCache1 = provider.listRemoteCachePeers(cache1);


        assertEquals(3, remotePeersOfCache1.size());
    }

    /**
     * Does a down cache manager in the cluster get removed?
     */

    @Test
    public void testRemoteCachePeersDetectsDownCacheManagerSlow() throws InterruptedException {

        try {
            CacheManagerPeerProvider provider = manager1.getCacheManagerPeerProvider("RMI");
            List remotePeersOfCache1 = provider.listRemoteCachePeers(cache1);
            assertEquals(4, remotePeersOfCache1.size());

            MulticastKeepaliveHeartbeatSender.setHeartBeatInterval(2000);
            Thread.sleep(2000);

            //Drop a CacheManager from the cluster
            manager5.shutdown();

            //Insufficient time for it to timeout
            remotePeersOfCache1 = provider.listRemoteCachePeers(cache1);
            assertEquals(4, remotePeersOfCache1.size());
        } finally {
            MulticastKeepaliveHeartbeatSender.setHeartBeatInterval(1000);
            Thread.sleep(2000);
        }


    }

    /**
     * Tests put and remove initiated from cache1 in a cluster
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    @Test
    public void testPutProgagatesFromAndToEveryCacheManagerAndCache() throws CacheException, InterruptedException {

        //Put
        String[] cacheNames = manager1.getCacheNames();
        int numberOfCaches = getNumberOfReplicatingCachesInCacheManager();
        Arrays.sort(cacheNames);
        for (int i = 0; i < cacheNames.length; i++) {
            String name = cacheNames[i];
            manager1.getCache(name).put(new Element("" + i, Integer.valueOf(i)));
            //Add some non serializable elements that should not get propagated
            manager1.getCache(name).put(new Element("nonSerializable" + i, new Object()));
        }

        waitForPropagate();

        int count2 = 0;
        int count3 = 0;
        int count4 = 0;
        int count5 = 0;
        for (int i = 0; i < cacheNames.length; i++) {
            String name = cacheNames[i];
            Element element2 = manager2.getCache(name).get("" + i);
            if (element2 != null) {
                count2++;
            }
            Element nonSerializableElement2 = manager2.getCache(name).get("nonSerializable" + i);
            if (nonSerializableElement2 != null) {
                count2++;
            }
            Element element3 = manager3.getCache(name).get("" + i);
            if (element3 != null) {
                count3++;
            }
            Element element4 = manager4.getCache(name).get("" + i);
            if (element4 != null) {
                count4++;
            }
            Element element5 = manager5.getCache(name).get("" + i);
            if (element5 != null) {
                count5++;
            }
        }
        //sampleCache2 in manager1 replicates puts via invalidate, so the count will be 1 less
        assertEquals(numberOfCaches - 1, count2);
        assertEquals(numberOfCaches - 1, count3);
        assertEquals(numberOfCaches - 1, count4);
        assertEquals(numberOfCaches - 1, count5);


    }

    /**
     * Tests what happens when a CacheManager in the cluster comes and goes. In ehcache-1.2.4 this would cause the new RMI CachePeers in the CacheManager to
     * be permanently corrupt.
     */
    
    @Test
    public void testPutProgagatesFromAndToEveryCacheManagerAndCacheDirty() throws CacheException, InterruptedException {

        manager3.shutdown();

        Thread.sleep(11020);

        manager3 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed3.xml");
        Thread.sleep(11020);

        //Put
        String[] cacheNames = manager1.getCacheNames();
        int numberOfCaches = getNumberOfReplicatingCachesInCacheManager();
        Arrays.sort(cacheNames);
        for (int i = 0; i < cacheNames.length; i++) {
            String name = cacheNames[i];
            manager1.getCache(name).put(new Element("" + i, Integer.valueOf(i)));
            //Add some non serializable elements that should not get propagated
            manager1.getCache(name).put(new Element("nonSerializable" + i, new Object()));
        }

        waitForPropagate();

        int count2 = 0;
        int count3 = 0;
        int count4 = 0;
        int count5 = 0;
        for (int i = 0; i < cacheNames.length; i++) {
            String name = cacheNames[i];
            Element element2 = manager2.getCache(name).get("" + i);
            if (element2 != null) {
                count2++;
            }
            Element nonSerializableElement2 = manager2.getCache(name).get("nonSerializable" + i);
            if (nonSerializableElement2 != null) {
                count2++;
            }
            Element element3 = manager3.getCache(name).get("" + i);
            if (element3 != null) {
                count3++;
            }
            Element element4 = manager4.getCache(name).get("" + i);
            if (element4 != null) {
                count4++;
            }
            Element element5 = manager5.getCache(name).get("" + i);
            if (element5 != null) {
                count5++;
            }
        }
        //sampleCache2 in manager1 replicates puts via invalidate, so the count will be 1 less
        assertEquals(numberOfCaches - 1, count2);
        assertEquals(numberOfCaches - 1, count3);
        assertEquals(numberOfCaches - 1, count4);
        assertEquals(numberOfCaches - 1, count5);


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

        ManagementService.registerMBeans(manager3, createMBeanServer(), true, true, true, true);
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
        RMICacheReplicatorTest replicatorTest = new RMICacheReplicatorTest();
        replicatorTest.setUp();
        replicatorTest.manualStabilityTest();
    }

    /**
     * The number of caches there should be.
     */
    protected int getNumberOfReplicatingCachesInCacheManager() {
        return 55;
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

        CountingCacheEventListener.resetCounters();

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
        assertEquals(504, size);

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
     * manager1 adds a replicating cache, then manager2 and so on. Then we remove one. Does everything work as expected?
     */
    @Test
    public void testPutWithNewCacheAddedProgressively() throws InterruptedException {

        manager1.addCache("progressiveAddCache");
        manager2.addCache("progressiveAddCache");

        //The cluster will not have formed yet, so it will fail
        try {
            putTest(manager1.getCache("progressiveAddCache"), manager2.getCache("progressiveAddCache"), ASYNCHRONOUS);
            fail();
        } catch (AssertionError e) {
            //expected
        }

        //The cluster will now have formed yet, so it will succeed
        putTest(manager1.getCache("progressiveAddCache"), manager2.getCache("progressiveAddCache"), ASYNCHRONOUS);

        Cache secondCache = manager2.getCache("progressiveAddCache");

        //The second peer disappears. The test will fail.
        manager2.removeCache("progressiveAddCache");
        try {
            putTest(manager1.getCache("progressiveAddCache"), secondCache, ASYNCHRONOUS);
            fail();
        } catch (IllegalStateException e) {
            //The second cache will not alive. Expected. But no other exception is caught and this will otherwise fail.

        }


    }


    /**
     * Test various cache configurations for cache1 - explicit setting of:
     * properties="replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true "/>
     */
    @Test
    public void testPutWithExplicitReplicationConfig() throws InterruptedException {

        putTest(manager1.getCache("sampleCache1"), manager2.getCache("sampleCache1"), ASYNCHRONOUS);
    }


    
    /**
     * Test various cache configurations for cache1 - explicit setting of:
     * properties="replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true "/>
     */
    @Test
    public void testPutWithThreadKiller() throws InterruptedException {

        putTestWithThreadKiller(manager1.getCache("sampleCache1"), manager2.getCache("sampleCache1"), ASYNCHRONOUS);
    }

    /**
     * CacheEventListeners that are not CacheReplicators should receive cache events originated from receipt
     * of a remote event by a CachePeer.
     */
    
    @Test
    public void testRemotelyReceivedPutNotifiesCountingListener() throws InterruptedException {

        putTest(manager1.getCache("sampleCache1"), manager2.getCache("sampleCache1"), ASYNCHRONOUS);
        assertEquals(1, CountingCacheEventListener.getCacheElementsPut(manager1.getCache("sampleCache1")).size());
        assertEquals(1, CountingCacheEventListener.getCacheElementsPut(manager2.getCache("sampleCache1")).size());

    }

    /**
     * Test various cache configurations for cache1 - explicit setting of:
     * properties="replicateAsynchronously=false, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true "/>
     */
    @Test
    public void testPutWithExplicitReplicationSynchronousConfig() throws InterruptedException {
        putTest(manager1.getCache("sampleCache3"), manager2.getCache("sampleCache3"), SYNCHRONOUS);
    }


    /**
     * Test put replicated for cache4 - no properties.
     * Defaults should be replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true
     */
    @Test
    public void testPutWithEmptyReplicationPropertiesConfig() throws InterruptedException {
        putTest(manager1.getCache("sampleCache4"), manager2.getCache("sampleCache4"), ASYNCHRONOUS);
    }

    /**
     * Test put replicated for cache4 - missing replicatePuts property.
     * replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true
     * should equal replicateAsynchronously=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true
     */
    @Test
    public void testPutWithOneMissingReplicationPropertyConfig() throws InterruptedException {
        putTest(manager1.getCache("sampleCache5"), manager2.getCache("sampleCache5"), ASYNCHRONOUS);
    }


    /**
     * Tests put and remove initiated from cache1 in a cluster
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    public void putTest(Ehcache fromCache, Ehcache toCache, boolean asynchronous) throws CacheException, InterruptedException {

        Serializable key = new Date();
        Serializable value = new Date();
        Element sourceElement = new Element(key, value);

        //Put
        fromCache.put(sourceElement);
        int i = 0;

        if (asynchronous) {
            waitForPropagate();
        }

        //Should have been replicated to toCache.
        Element deliveredElement = toCache.get(key);
        assertEquals(sourceElement, deliveredElement);

    }


    /**
     * Tests put and remove initiated from cache1 in a cluster
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    public void putTestWithThreadKiller(Ehcache fromCache, Ehcache toCache, boolean asynchronous)
            throws CacheException, InterruptedException {

        fromCache.put(new Element("thread killer", new ThreadKiller()));
        if (asynchronous) {
            waitForPropagate();
        }

        Serializable key = new Date();
        Serializable value = new Date();
        Element sourceElement = new Element(key, value);

        //Put
        fromCache.put(sourceElement);

        if (asynchronous) {
            waitForPropagate();
        }

        //Should have been replicated to toCache.
        Element deliveredElement = toCache.get(key);
        assertEquals(sourceElement, deliveredElement);

    }


    /**
     * Checks that a put received from a remote cache notifies any registered listeners.
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    @Test
    public void testRemotePutNotificationGetsToOtherListeners() throws CacheException, InterruptedException {

        Serializable key = new Date();
        Serializable value = new Date();
        Element element1 = new Element(key, value);

        //Put
        cache1.put(new Element("1", new Date()));
        cache1.put(new Element("2", new Date()));
        cache1.put(new Element("3", new Date()));

        //Nonserializable and non deliverable put
        Object nonSerializableObject = new Object();
        cache1.put(new Element(nonSerializableObject, new Object()));


        waitForPropagate();

        //local initiating cache's counting listener should have been notified
        assertEquals(4, CountingCacheEventListener.getCacheElementsPut(cache1).size());
        //remote receiving caches' counting listener should have been notified
        assertEquals(3, CountingCacheEventListener.getCacheElementsPut(cache2).size());

        //Update
        cache1.put(new Element("1", new Date()));
        cache1.put(new Element("2", new Date()));
        cache1.put(new Element("3", new Date()));

        //Nonserializable and non deliverable put
        cache1.put(new Element(nonSerializableObject, new Object()));

        waitForPropagate();

        //local initiating cache's counting listener should have been notified
        assertEquals(4, CountingCacheEventListener.getCacheElementsUpdated(cache1).size());
        //remote receiving caches' counting listener should have been notified
        assertEquals(3, CountingCacheEventListener.getCacheElementsUpdated(cache2).size());

        //Remove
        cache1.remove("1");
        cache1.remove("2");
        cache1.remove("3");
        cache1.remove(nonSerializableObject);

        waitForPropagate();

        //local initiating cache's counting listener should have been notified
        assertEquals(4, CountingCacheEventListener.getCacheElementsRemoved(cache1).size());
        //remote receiving caches' counting listener should have been notified
        assertEquals(3, CountingCacheEventListener.getCacheElementsRemoved(cache2).size());

    }


    /**
     * Test various cache configurations for cache1 - explicit setting of:
     * properties="replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true "/>
     */
    @Test
    public void testRemoveWithExplicitReplicationConfig() throws InterruptedException {
        removeTest(manager1.getCache("sampleCache1"), manager2.getCache("sampleCache1"), ASYNCHRONOUS);
    }

    /**
     * Test various cache configurations for cache1 - explicit setting of:
     * properties="replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true "/>
     */
    @Test
    public void testRemoveWithExplicitReplicationSynchronousConfig() throws InterruptedException {
        removeTest(manager1.getCache("sampleCache3"), manager2.getCache("sampleCache3"), SYNCHRONOUS);
    }


    /**
     * Test put replicated for cache4 - no properties.
     * Defaults should be replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true
     */
    @Test
    public void testRemoveWithEmptyReplicationPropertiesConfig() throws InterruptedException {
        removeTest(manager1.getCache("sampleCache4"), manager2.getCache("sampleCache4"), ASYNCHRONOUS);
    }

    /**
     * Tests put and remove initiated from a cache to another cache in a cluster
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    public void removeTest(Ehcache fromCache, Ehcache toCache, boolean asynchronous) throws CacheException, InterruptedException {

        Serializable key = new Date();
        Serializable value = new Date();
        Element element1 = new Element(key, value);

        //Put
        fromCache.put(element1);

        if (asynchronous) {
            waitForPropagate();
        }

        //Should have been replicated to cache2.
        Element element2 = toCache.get(key);
        assertEquals(element1, element2);

        //Remove
        fromCache.remove(key);
        if (asynchronous) {
            waitForPropagate();
        }

        //Should have been replicated to cache2.
        element2 = toCache.get(key);
        assertNull(element2);

    }


    /**
     * test removeAll sync
     */
    @Test
    public void testRemoveAllAsynchronous() throws Exception {
        removeAllTest(manager1.getCache("sampleCache1"), manager2.getCache("sampleCache1"), ASYNCHRONOUS);
    }

    /**
     * test removeAll async
     */
    @Test
    public void testRemoveAllSynchronous() throws Exception {
        removeAllTest(manager1.getCache("sampleCache3"), manager2.getCache("sampleCache3"), SYNCHRONOUS);
    }

    /**
     * Tests removeAll initiated from a cache to another cache in a cluster
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    public void removeAllTest(Ehcache fromCache, Ehcache toCache, boolean asynchronous) throws Exception {

        //removeAll is distributed. Stop it colliding with the rest of the test
        waitForPropagate();


        Serializable key = new Date();
        Serializable value = new Date();
        Element element1 = new Element(key, value);

        //Put
        fromCache.put(element1);


        if (asynchronous) {
            waitForPropagate();
        }

        //Should have been replicated to cache2.
        Element element2 = toCache.get(key);
        assertEquals(element1, element2);

        //Remove
        fromCache.removeAll();
        if (asynchronous) {
            waitForPropagate();
        }

        //Should have been replicated to cache2.
        element2 = toCache.get(key);
        assertNull(element2);
        assertEquals(0, toCache.getSize());

    }


    /**
     * Test various cache configurations for cache1 - explicit setting of:
     * properties="replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true "/>
     */
    @Test
    public void testUpdateWithExplicitReplicationConfig() throws Exception {
        updateViaCopyTest(manager1.getCache("sampleCache1"), manager2.getCache("sampleCache1"), ASYNCHRONOUS);
    }

    /**
     * Test various cache configurations for cache1 - explicit setting of:
     * properties="replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true "/>
     */
    @Test
    public void testUpdateWithExplicitReplicationSynchronousConfig() throws Exception {
        updateViaCopyTest(manager1.getCache("sampleCache3"), manager2.getCache("sampleCache3"), SYNCHRONOUS);
    }


    /**
     * Test put replicated for cache4 - no properties.
     * Defaults should be replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true
     */
    @Test
    public void testUpdateWithEmptyReplicationPropertiesConfig() throws Exception {
        updateViaCopyTest(manager1.getCache("sampleCache4"), manager2.getCache("sampleCache4"), ASYNCHRONOUS);
    }

    /**
     * Tests put and update through copy initiated from cache1 in a cluster
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    public void updateViaCopyTest(Ehcache fromCache, Ehcache toCache, boolean asynchronous) throws Exception {

        fromCache.removeAll();
        toCache.removeAll();

        //removeAll is distributed. Stop it colliding with the rest of the test
        waitForPropagate();

        Serializable key = new Date();
        Serializable value = new Date();
        Element element1 = new Element(key, value);

        //Put
        fromCache.put(element1);
        if (asynchronous) {
            waitForPropagate();
        }

        //Should have been replicated to cache2.
        Element element2 = toCache.get(key);
        assertEquals(element1, element2);

        //Update
        Element updatedElement1 = new Element(key, new Date());

        fromCache.put(updatedElement1);
        if (asynchronous) {
            waitForPropagate();
        }

        //Should have been replicated to cache2.
        Element receivedUpdatedElement2 = toCache.get(key);
        assertEquals(updatedElement1, receivedUpdatedElement2);

    }



    /**
     * Tests put through invalidation initiated from cache1 in a cluster
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    @Test
    public void testPutViaInvalidate() throws CacheException, InterruptedException, IOException {

        cache1 = manager1.getCache("sampleCache2");
        cache1.removeAll();

        cache2 = manager2.getCache("sampleCache2");
        cache2.removeAll();

        //removeAll is distributed. Stop it colliding with the rest of the test
        waitForPropagate();

        String key = "1";
        Serializable value = new Date();
        Element element1 = new Element(key, value);

        Element element3 = new Element("key2", "two");

        //Put into 2. 2 is configured to replicate puts via copy
        cache2.put(element1);
        assertNotNull(cache2.get(key));
        waitForPropagate();

        //Should have been replicated to cache1.
        Element element2 = cache1.get(key);
        assertEquals(element1, element2);

        //Put
        cache1.put(element3);
        waitForPropagate();

        //Invalidate should have been replicated to cache2.
        assertNull(cache2.get("key2"));

        //Update
        cache1.put(element3);
        waitForPropagate();

        //Should have been removed in cache2.
        element2 = cache2.get("key2");
        assertNull(element2);

    }


    /**
     * Tests put and update through invalidation initiated from cache1 in a cluster
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    @Test
    public void testUpdateViaInvalidate() throws CacheException, InterruptedException, IOException {

        cache1 = manager1.getCache("sampleCache2");
        cache1.removeAll();

        cache2 = manager2.getCache("sampleCache2");
        cache2.removeAll();

        //removeAll is distributed. Stop it colliding with the rest of the test
        waitForPropagate();

        String key = "1";
        Serializable value = new Date();
        Element element1 = new Element(key, value);

        //Put
        cache2.put(element1);
        Element element2 = cache2.get(key);
        assertEquals(element1, element2);

        //Update
        cache1.put(element1);
        waitForPropagate();

        //Should have been removed in cache2.
        element2 = cache2.get(key);
        assertNull(element2);

    }


    /**
     * Tests put and update through invalidation initiated from cache1 in a cluster
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    @Test
    public void testUpdateViaInvalidateNonSerializableValue() throws CacheException, InterruptedException, IOException {

        cache1 = manager1.getCache("sampleCache2");
        cache1.removeAll();

        cache2 = manager2.getCache("sampleCache2");
        cache2.removeAll();

        //removeAll is distributed. Stop it colliding with the rest of the test
        waitForPropagate();

        String key = "1";
        Serializable value = new Date();

        /**
         * Non-serializable test class
         */
        class NonSerializable {
            //
        }

        NonSerializable value1 = new NonSerializable();
        Element element1 = new Element(key, value1);

        //Put
        cache2.put(element1);
        Element element2 = cache2.get(key);
        assertEquals(element1, element2);

        //Update
        cache1.put(element1);
        waitForPropagate();

        //Should have been removed in cache2.
        element2 = cache2.get(key);
        assertNull(element2);

    }


    /**
     * What happens when two cache instances replicate to each other and a change is initiated
     */
    @Test
    public void testInfiniteNotificationsLoop() throws InterruptedException {

        Serializable key = "1";
        Serializable value = new Date();
        Element element = new Element(key, value);

        //Put
        cache1.put(element);
        waitForPropagate();

        //Should have been replicated to cache2.
        Element element2 = cache2.get(key);
        assertEquals(element, element2);

        //Remove
        cache1.remove(key);
        assertNull(cache1.get(key));

        //Should have been replicated to cache2.
        waitForPropagate();
        element2 = cache2.get(key);
        assertNull(element2);

        //Put into 2
        Element element3 = new Element("3", "ddsfds");
        cache2.put(element3);
        waitForPropagate();
        Element element4 = cache2.get("3");
        assertEquals(element3, element4);

    }


    /**
     * Shows result of perf problem and fix in flushReplicationQueue
     * <p/>
     * Behaviour before change:
     * <p/>
     * INFO: Items written: 10381
     * Oct 29, 2007 11:40:04 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 29712
     * Oct 29, 2007 11:40:57 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 1
     * Oct 29, 2007 11:40:58 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 32354
     * Oct 29, 2007 11:42:34 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 322
     * Oct 29, 2007 11:42:35 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 41909
     * <p/>
     * Behaviour after change:
     * INFO: Items written: 26356
     * Oct 29, 2007 11:44:39 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 33656
     * Oct 29, 2007 11:44:40 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 32234
     * Oct 29, 2007 11:44:42 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 38677
     * Oct 29, 2007 11:44:43 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 43418
     * Oct 29, 2007 11:44:44 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 31277
     * Oct 29, 2007 11:44:45 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 27769
     * Oct 29, 2007 11:44:46 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 29596
     * Oct 29, 2007 11:44:47 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 17142
     * Oct 29, 2007 11:44:48 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 14775
     * Oct 29, 2007 11:44:49 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 4088
     * Oct 29, 2007 11:44:51 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 5492
     * Oct 29, 2007 11:44:52 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 10188
     * <p/>
     * Also no pauses noted.
     */
    @Test
    public void testReplicatePerf() throws InterruptedException {

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

        //wait for cluster to drop back to just one: manager1
        waitForPropagate();


        long start = System.currentTimeMillis();
        final String keyBase = Long.toString(start);
        int count = 0;

        for (int i = 0; i < 100000; i++) {
            final String key = keyBase + ':' + Integer.toString((int) (Math.random() * 1000.0));
            cache1.put(new Element(key, "My Test"));
            cache1.get(key);
            cache1.remove(key);
            count++;

            final long end = System.currentTimeMillis();
            if (end - start >= 1000) {
                start = end;
                LOG.info("Items written: " + count);
                //make sure it does not choke
                assertTrue(count > 1000);
                count = 0;
            }
        }
    }


    /**
     * Need to wait for async
     *
     * @throws InterruptedException
     */
    protected void waitForPropagate() throws InterruptedException {
        Thread.sleep(1500);
    }

    /**
     * Need to wait for async
     *
     * @throws InterruptedException
     */
    protected void waitForSlowPropagate() throws InterruptedException {
        Thread.sleep(6000);
    }


    /**
     * Distributed operations create extra scope for deadlock.
     * This test checks whether a distributed deadlock scenario exists for synchronous replication
     * of each distributed operation all at once.
     * It shows that no distributed deadlock exists for asynchronous replication. It is multi thread
     * and multi process safe.
     * <p/>
     * Carefully tailored to exercise:
     * <ol>
     * <li>overflow to disk. We put in 20 things and the memory size is 10
     * <li>each peer is working on the same set of keys thus maximising contention
     * <li>we do puts, gets and removes to explore all the execution paths
     * </ol>
     * If a deadlock occurs, processing will stop until a SocketTimeout exception is thrown and
     * the deadlock will be released.
     */
    @Test
    public void testCacheOperationsSynchronousMultiThreaded() throws Exception, InterruptedException {

        // Run a set of threads, that attempt to fetch the elements
        final List executables = new ArrayList();

        executables.add(new ClusterExecutable(manager1, "sampleCache3"));
        executables.add(new ClusterExecutable(manager2, "sampleCache3"));
        executables.add(new ClusterExecutable(manager3, "sampleCache3"));

        runThreads(executables);
    }


    /**
     * Distributed operations create extra scope for deadlock.
     * This test checks whether a distributed deadlock scenario exists for asynchronous replication
     * of each distributed operation all at once.
     * It shows that no distributed deadlock exists for asynchronous replication. It is multi thread
     * and multi process safe.
     * It uses sampleCache2, which is configured to be asynchronous
     * <p/>
     * Carefully tailored to exercise:
     * <ol>
     * <li>overflow to disk. We put in 20 things and the memory size is 10
     * <li>each peer is working on the same set of keys thus maximising contention
     * <li>we do puts, gets and removes to explore all the execution paths
     * </ol>
     */
    @Test
    public void testCacheOperationsAynchronousMultiThreaded() throws Exception, InterruptedException {

        // Run a set of threads, that attempt to fetch the elements
        final List executables = new ArrayList();

        executables.add(new ClusterExecutable(manager1, "sampleCache2"));
        executables.add(new ClusterExecutable(manager2, "sampleCache2"));
        executables.add(new ClusterExecutable(manager3, "sampleCache2"));

        runThreads(executables);
    }

    /**
     * An Exececutable which allows the CacheManager to be set
     */
    class ClusterExecutable implements Executable {

        private CacheManager manager;
        private String cacheName;

        /**
         * Construct with CacheManager
         *
         * @param manager
         */
        public ClusterExecutable(CacheManager manager, String cacheName) {
            this.manager = manager;
            this.cacheName = cacheName;
        }

        /**
         * Execute
         *
         * @throws Exception
         */
        public void execute() throws Exception {
            Random random = new Random();

            for (int i = 0; i < 20; i++) {
                Integer key = Integer.valueOf((i));
                int operationSelector = random.nextInt(4);
                Cache cache = manager.getCache(cacheName);
                if (operationSelector == 100) {
                    cache.get(key);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(cache.getGuid() + ": get " + key);
                    }
                } else if (operationSelector == 100) {
                    cache.remove(key);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(cache.getGuid() + ": remove " + key);
                    }
                } else if (operationSelector == 2) {
                    cache.put(new Element(key,
                            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                    + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                    + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                    + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                    + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(cache.getGuid() + ": put " + key);
                    }
                } else {
                    //every twelfth time 1/4 * 1/3 = 1/12
                    if (random.nextInt(3) == 1) {
                        LOG.debug("cache.removeAll()");
                        cache.removeAll();
                    }
                }
            }

        }
    }

}

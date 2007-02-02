/**
 *  Copyright 2003-2007 Greg Luck
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

import net.sf.ehcache.event.CountingCacheEventListener;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.StopWatch;
import net.sf.ehcache.Cache;
import net.sf.ehcache.ThreadKiller;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.rmi.RemoteException;
import java.io.Serializable;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import junit.framework.AssertionFailedError;


/**
 * Tests the manually configured caches
 *
 * todo get these tests/or remove them and address startup issue
 * @author Greg Luck
 * @version $Id$
 */
public class ManualRMICacheReplicatorTest extends AbstractCacheTest {

    /**
     * A value to represent replicate asynchronously
     */
    protected static final boolean ASYNCHRONOUS = true;

    /**
     * A value to represent replicate synchronously
     */
    protected static final boolean SYNCHRONOUS = false;

    private static final Log LOG = LogFactory.getLog(ManualRMICacheReplicatorTest.class.getName());


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
    protected void setUp() throws Exception {
        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        //Required to get SoftReference tests to pass. The VM clean up SoftReferences rather than allocating
        // memory to -Xmx!
        //forceVMGrowth();
        //System.gc();
        MulticastKeepaliveHeartbeatSender.setHeartBeatInterval(1000);

        CountingCacheEventListener.resetCounters();

        manager1 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-manual-distributed1.xml");
        manager2 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-manual-distributed2.xml");
        manager3 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-manual-distributed3.xml");
//        manager4 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-manual-distributed4.xml");
//        manager5 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-manual-distributed5.xml");

        //manager6 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-manual-distributed-jndi6.xml");

        //Give things a breather
        Thread.sleep(1010);

        cache1 = manager1.getCache(cacheName);
        cache1.removeAll();

        cache2 = manager2.getCache(cacheName);
        cache2.removeAll();

        //enable distributed removeAlls to finish
        waitForProgagate();


    }

    /**
     * Force the VM to grow to its full size. This stops SoftReferences from being reclaimed in favour of
     * Heap growth. Only an issue when a VM is cold.
     */
    protected void forceVMGrowth() {
        byte[] forceVMGrowth = new byte[50000000];
    }


    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    protected void tearDown() throws Exception {

        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        if (manager1 != null) {
            manager1.shutdown();
        }
        if (manager2 != null) {
            manager2.shutdown();
        }
        if (manager3 != null) {
            manager3.shutdown();
        }

        if (manager6 != null) {
            manager6.shutdown();
        }
        Thread.sleep(50);
    }

    /**
     * 5 cache managers should means that each cache has four remote peers
     */
    public void testRemoteCachePeersEqualsNumberOfCacheManagersInCluster() {

        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        CacheManagerPeerProvider provider = manager1.getCachePeerProvider();
        List remotePeersOfCache1 = provider.listRemoteCachePeers(cache1);
        assertEquals(2, remotePeersOfCache1.size());
    }

    /**
     * Does a new cache manager in the cluster get detected?
     * In a manual configuration the answer is no.
     */
    public void testRemoteCachePeersDetectsNewCacheManager() throws InterruptedException {

        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        CacheManagerPeerProvider provider = manager1.getCachePeerProvider();
        List remotePeersOfCache1 = provider.listRemoteCachePeers(cache1);
        assertEquals(2, remotePeersOfCache1.size());

        //Add new CacheManager to cluster
        manager6 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed6.xml");

        //Allow detection to occur
        Thread.sleep(10010);

        remotePeersOfCache1 = provider.listRemoteCachePeers(cache1);
        assertEquals(2, remotePeersOfCache1.size());
    }

    /**
     * Does a down cache manager in the cluster get removed?
     */
    public void testRemoteCachePeersDetectsDownCacheManager() throws InterruptedException {

        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }


        CacheManagerPeerProvider provider = manager1.getCachePeerProvider();
        List remotePeersOfCache1 = provider.listRemoteCachePeers(cache1);
        assertEquals(2, remotePeersOfCache1.size());

        //Drop a CacheManager from the cluster
        manager2.shutdown();

        //Allow change detection to occur. Heartbeat 1 second and is not stale until 5000
        Thread.sleep(11010);
        remotePeersOfCache1 = provider.listRemoteCachePeers(cache1);


        assertEquals(1, remotePeersOfCache1.size());
    }

    /**
     * Does a down cache manager in the cluster get removed?
     */
    public void testRemoteCachePeersDetectsDownCacheManagerSlow() throws InterruptedException {

        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        try {
            CacheManagerPeerProvider provider = manager1.getCachePeerProvider();
            List remotePeersOfCache1 = provider.listRemoteCachePeers(cache1);
            assertEquals(2, remotePeersOfCache1.size());

            MulticastKeepaliveHeartbeatSender.setHeartBeatInterval(2000);
            Thread.sleep(2000);

            //Drop a CacheManager from the cluster
            manager2.shutdown();

            //Insufficient time for it to timeout
            remotePeersOfCache1 = provider.listRemoteCachePeers(cache1);
            assertEquals(1, remotePeersOfCache1.size());
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
    public void xtestPutProgagatesFromAndToEveryCacheManagerAndCache() throws CacheException, InterruptedException {

        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        //Put
        String[] cacheNames = manager1.getCacheNames();
        int numberOfCaches = getNumberOfReplicatingCachesInCacheManager();
        Arrays.sort(cacheNames);
        for (int i = 0; i < cacheNames.length; i++) {
            String name = cacheNames[i];
            manager1.getCache(name).put(new Element("" + i, new Integer(i)));
            //Add some non serializable elements that should not get propagated
            manager1.getCache(name).put(new Element("nonSerializable" + i, new Object()));
        }

        waitForProgagate();

        int count2 = 0;
//        int count3 = 0;
//        int count4 = 0;
//        int count5 = 0;
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
//            Element element3 = manager3.getCache(name).get("" + i);
//            if (element3 != null) {
//                count3++;
//            }
//            Element element4 = manager4.getCache(name).get("" + i);
//            if (element4 != null) {
//                count4++;
//            }
//            Element element5 = manager5.getCache(name).get("" + i);
//            if (element5 != null) {
//                count5++;
//            }
        }
        assertEquals(numberOfCaches, count2);
//        assertEquals(numberOfCaches, count3);
//        assertEquals(numberOfCaches, count4);
//        assertEquals(numberOfCaches, count5);


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
        ManualRMICacheReplicatorTest replicatorTest = new ManualRMICacheReplicatorTest();
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
    public void testBigPutsProgagatesAsynchronous() throws CacheException, InterruptedException {

        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        //Give everything a chance to startup
        Thread.sleep(10000);
        StopWatch stopWatch = new StopWatch();
        Integer index = null;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 1000; j++) {
                index = new Integer(((1000 * i) + j));
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

        Thread.sleep(5000);
//        assertEquals(2000, manager2.getCache("sampleCache1").getSize());
//        assertEquals(2000, manager3.getCache("sampleCache1").getSize());
//        assertEquals(2000, manager4.getCache("sampleCache1").getSize());
//        assertEquals(2000, manager5.getCache("sampleCache1").getSize());

        CountingCacheEventListener.resetCounters();

    }


    /**
     * Performance and capacity tests.
     * <p/>
     */
    public void testBootstrap() throws CacheException, InterruptedException, RemoteException {

        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        //load up some data
        StopWatch stopWatch = new StopWatch();
        Integer index = null;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 1000; j++) {
                index = new Integer(((1000 * i) + j));
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

        Thread.sleep(8000);
//        assertEquals(2000, manager2.getCache("sampleCache1").getSize());
//        assertEquals(2000, manager3.getCache("sampleCache1").getSize());
//        assertEquals(2000, manager4.getCache("sampleCache1").getSize());
//        assertEquals(2000, manager5.getCache("sampleCache1").getSize());

        //now test bootstrap
        manager1.addCache("bootStrapResults");
        Cache cache = manager1.getCache("bootStrapResults");
        List cachePeers = manager1.getCacheManagerPeerProvider().listRemoteCachePeers(cache1);
        CachePeer cachePeer = (CachePeer) cachePeers.get(0);

        List keys = cachePeer.getKeys();
        assertEquals(2000, keys.size());

        Element firstElement = cachePeer.getQuiet((Serializable) keys.get(0));
        long size = firstElement.getSerializedSize();
        assertEquals(574, size);

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

        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        //Give everything a chance to startup
        StopWatch stopWatch = new StopWatch();
        Integer index = null;
        for (int i = 0; i < 500; i++) {
            for (int j = 0; j < 1000; j++) {
                index = new Integer(((1000 * i) + j));
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
//        assertEquals(20000, manager3.getCache("sampleCache1").getSize());
//        assertEquals(20000, manager4.getCache("sampleCache1").getSize());
//        assertEquals(20000, manager5.getCache("sampleCache1").getSize());

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
    public void xtestBigRemovesProgagatesAsynchronous() throws CacheException, InterruptedException {

        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        //Give everything a chance to startup
        Integer index = null;
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 1000; j++) {
                index = new Integer(((1000 * i) + j));
                cache1.put(new Element(index,
                        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            }

        }
        Thread.sleep(8000);
        assertEquals(5000, cache1.getSize());
        assertEquals(5000, manager2.getCache("sampleCache1").getSize());
//        assertEquals(5000, manager3.getCache("sampleCache1").getSize());
//        assertEquals(5000, manager4.getCache("sampleCache1").getSize());
//        assertEquals(5000, manager5.getCache("sampleCache1").getSize());

        //Let the disk stores catch up before the next stage of the test
        Thread.sleep(2000);

        StopWatch stopWatch = new StopWatch();

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 1000; j++) {
                index = new Integer(((1000 * i) + j));
                cache1.remove(index);
            }
        }


        int timeForPropagate = 10000;

        Thread.sleep(timeForPropagate);
        assertEquals(0, cache1.getSize());
        assertEquals(0, manager2.getCache("sampleCache1").getSize());
//        assertEquals(0, manager3.getCache("sampleCache1").getSize());
//        assertEquals(0, manager4.getCache("sampleCache1").getSize());
//        assertEquals(0, manager5.getCache("sampleCache1").getSize());

        LOG.info("Remove Elapsed time: " + timeForPropagate);


    }


    /**
     * Performance and capacity tests.
     * <p/>
     * 5 seconds to send all notifications synchronously with 5 peers, 2000 Elements and 400 byte payload
     * The numbers given below are for the remote peer tester (java -jar ehcache-1.x-remote-debugger.jar ehcache-distributed1.xml)
     * running on a 10Mbit ethernet network and are measured from the time the peer starts receiving to when
     * it has fully received.
     */
    public void xtestBigPutsProgagatesSynchronous() throws CacheException, InterruptedException {

        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        //Give everything a chance to startup
        StopWatch stopWatch = new StopWatch();
        Integer index;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 1000; j++) {
                index = new Integer(((1000 * i) + j));
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
//        assertEquals(2000, manager3.getCache("sampleCache3").getSize());
//        assertEquals(2000, manager4.getCache("sampleCache3").getSize());
//        assertEquals(2000, manager5.getCache("sampleCache3").getSize());

    }


    /**
     * manager1 adds a replicating cache, then manager2 and so on. Then we remove one. Does everything work as expected?
     */
    public void xtestPutWithNewCacheAddedProgressively() throws InterruptedException {

        manager1.addCache("progressiveAddCache");
        manager2.addCache("progressiveAddCache");

        //The cluster will not have formed yet, so it will fail
        try {
            putTest(manager1.getCache("progressiveAddCache"), manager2.getCache("progressiveAddCache"), ASYNCHRONOUS);
            fail();
        } catch (AssertionFailedError e) {
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
    public void testPutWithExplicitReplicationConfig() throws InterruptedException {
        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }
        putTest(manager1.getCache("sampleCache1"), manager2.getCache("sampleCache1"), ASYNCHRONOUS);
    }


    /**
     * Test various cache configurations for cache1 - explicit setting of:
     * properties="replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true "/>
     */
    public void testPutWithThreadKiller() throws InterruptedException {
        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }
        putTestWithThreadKiller(manager1.getCache("sampleCache1"), manager2.getCache("sampleCache1"), ASYNCHRONOUS);
    }

    /**
     * CacheEventListeners that are not CacheReplicators should receive cache events originated from receipt
     * of a remote event by a CachePeer.
     */
    public void xtestRemotelyReceivedPutNotifiesCountingListener() throws InterruptedException {
        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }
        putTest(manager1.getCache("sampleCache1"), manager2.getCache("sampleCache1"), ASYNCHRONOUS);
        assertEquals(1, CountingCacheEventListener.getCacheElementsPut(manager1.getCache("sampleCache1")).size());
        assertEquals(1, CountingCacheEventListener.getCacheElementsPut(manager2.getCache("sampleCache1")).size());

    }

    /**
     * Test various cache configurations for cache1 - explicit setting of:
     * properties="replicateAsynchronously=false, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true "/>
     */
    public void xtestPutWithExplicitReplicationSynchronousConfig() throws InterruptedException {
        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }
        putTest(manager1.getCache("sampleCache3"), manager2.getCache("sampleCache3"), SYNCHRONOUS);
    }


    /**
     * Test put replicated for cache4 - no properties.
     * Defaults should be replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true
     */
    public void xtestPutWithEmptyReplicationPropertiesConfig() throws InterruptedException {
        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }
        putTest(manager1.getCache("sampleCache4"), manager2.getCache("sampleCache4"), ASYNCHRONOUS);
    }

    /**
     * Test put replicated for cache4 - missing replicatePuts property.
     * replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true
     * should equal replicateAsynchronously=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true
     */
    public void testPutWithOneMissingReplicationPropertyConfig() throws InterruptedException {
        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }
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
            waitForProgagate();
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
            waitForProgagate();
        }

        Serializable key = new Date();
        Serializable value = new Date();
        Element sourceElement = new Element(key, value);

        //Put
        fromCache.put(sourceElement);

        if (asynchronous) {
            waitForProgagate();
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
    public void xtestRemotePutNotificationGetsToOtherListeners() throws CacheException, InterruptedException {

        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

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


        waitForProgagate();

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

        waitForProgagate();

        //local initiating cache's counting listener should have been notified
        assertEquals(4, CountingCacheEventListener.getCacheElementsUpdated(cache1).size());
        //remote receiving caches' counting listener should have been notified
        assertEquals(3, CountingCacheEventListener.getCacheElementsUpdated(cache2).size());

        //Remove
        cache1.remove("1");
        cache1.remove("2");
        cache1.remove("3");
        cache1.remove(nonSerializableObject);

        waitForProgagate();

        //local initiating cache's counting listener should have been notified
        assertEquals(4, CountingCacheEventListener.getCacheElementsRemoved(cache1).size());
        //remote receiving caches' counting listener should have been notified
        assertEquals(3, CountingCacheEventListener.getCacheElementsRemoved(cache2).size());

    }


    /**
     * Test various cache configurations for cache1 - explicit setting of:
     * properties="replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true "/>
     */
    public void testRemoveWithExplicitReplicationConfig() throws InterruptedException {
        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }
        removeTest(manager1.getCache("sampleCache1"), manager2.getCache("sampleCache1"), ASYNCHRONOUS);
    }

    /**
     * Test various cache configurations for cache1 - explicit setting of:
     * properties="replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true "/>
     */
    public void xtestRemoveWithExplicitReplicationSynchronousConfig() throws InterruptedException {
        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }
        removeTest(manager1.getCache("sampleCache3"), manager2.getCache("sampleCache3"), SYNCHRONOUS);
    }


    /**
     * Test put replicated for cache4 - no properties.
     * Defaults should be replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true
     */
    public void xtestRemoveWithEmptyReplicationPropertiesConfig() throws InterruptedException {
        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }
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
            waitForProgagate();
        }

        //Should have been replicated to cache2.
        Element element2 = toCache.get(key);
        assertEquals(element1, element2);

        //Remove
        fromCache.remove(key);
        if (asynchronous) {
            waitForProgagate();
        }

        //Should have been replicated to cache2.
        element2 = toCache.get(key);
        assertNull(element2);

    }


    /**
     * test removeAll sync
     */
    public void testRemoveAllAsynchronous() throws Exception {
        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }
        removeAllTest(manager1.getCache("sampleCache1"), manager2.getCache("sampleCache1"), ASYNCHRONOUS);
    }

    /**
     * test removeAll async
     */
    public void xtestRemoveAllSynchronous() throws Exception {
        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }
        removeAllTest(manager1.getCache("sampleCache3"), manager2.getCache("sampleCache3"), SYNCHRONOUS);
    }

    /**
     * Tests removeAll initiated from a cache to another cache in a cluster
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    public void removeAllTest(Ehcache fromCache, Ehcache toCache, boolean asynchronous) throws Exception {

        //removeAll is distributed. Stop it colliding with the rest of the test
        waitForProgagate();


        Serializable key = new Date();
        Serializable value = new Date();
        Element element1 = new Element(key, value);

        //Put
        fromCache.put(element1);


        if (asynchronous) {
            waitForProgagate();
        }

        //Should have been replicated to cache2.
        Element element2 = toCache.get(key);
        assertEquals(element1, element2);

        //Remove
        fromCache.removeAll();
        if (asynchronous) {
            waitForProgagate();
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
    public void testUpdateWithExplicitReplicationConfig() throws Exception {
        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }
        updateViaCopyTest(manager1.getCache("sampleCache1"), manager2.getCache("sampleCache1"), ASYNCHRONOUS);
    }

    /**
     * Test various cache configurations for cache1 - explicit setting of:
     * properties="replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true "/>
     */
    public void xtestUpdateWithExplicitReplicationSynchronousConfig() throws Exception {
        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }
        updateViaCopyTest(manager1.getCache("sampleCache3"), manager2.getCache("sampleCache3"), SYNCHRONOUS);
    }


    /**
     * Test put replicated for cache4 - no properties.
     * Defaults should be replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true
     */
    public void xtestUpdateWithEmptyReplicationPropertiesConfig() throws Exception {
        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }
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
        waitForProgagate();

        Serializable key = new Date();
        Serializable value = new Date();
        Element element1 = new Element(key, value);

        //Put
        fromCache.put(element1);
        if (asynchronous) {
            waitForProgagate();
        }

        //Should have been replicated to cache2.
        Element element2 = toCache.get(key);
        assertEquals(element1, element2);

        //Update
        Element updatedElement1 = new Element(key, new Date());

        fromCache.put(updatedElement1);
        if (asynchronous) {
            waitForProgagate();
        }

        //Should have been replicated to cache2.
        Element receivedUpdatedElement2 = toCache.get(key);
        assertEquals(updatedElement1, receivedUpdatedElement2);

    }


    /**
     * Tests put and update through invalidation initiated from cache1 in a cluster
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    public void xtestUpdateViaInvalidate() throws CacheException, InterruptedException, IOException {

        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        cache1 = manager1.getCache("sampleCache2");
        cache1.removeAll();

        cache2 = manager2.getCache("sampleCache2");
        cache2.removeAll();

        //removeAll is distributed. Stop it colliding with the rest of the test
        waitForProgagate();

        Serializable key = "1";
        Serializable value = new Date();
        Element element1 = new Element(key, value);

        //Put
        cache1.put(element1);
        waitForProgagate();

        //Should have been replicated to cache2.
        Element element2 = cache2.get(key);
        assertEquals(element1, element2);

        //Update
        cache1.put(element1);
        waitForProgagate();

        //Should have been removed in cache2.
        element2 = cache2.get(key);
        assertNull(element2);

    }

    /**
     * What happens when two cache instances replicate to each other and a change is initiated
     */
    public void testInfiniteNotificationsLoop() throws InterruptedException {

        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        Serializable key = "1";
        Serializable value = new Date();
        Element element = new Element(key, value);

        //Put
        cache1.put(element);
        waitForProgagate();

        //Should have been replicated to cache2.
        Element element2 = cache2.get(key);
        assertEquals(element, element2);

        //Remove
        cache1.remove(key);
        assertNull(cache1.get(key));

        //Should have been replicated to cache2.
        waitForProgagate();
        element2 = cache2.get(key);
        assertNull(element2);

        //Put into 2
        Element element3 = new Element("3", "ddsfds");
        cache2.put(element3);
        waitForProgagate();
        Element element4 = cache2.get("3");
        assertEquals(element3, element4);

    }


    /**
     * Need to wait for async
     *
     * @throws InterruptedException
     */
    protected void waitForProgagate() throws InterruptedException {
        Thread.sleep(2000);
    }

    /**
     * Need to wait for async
     *
     * @throws InterruptedException
     */
    protected void waitForSlowProgagate() throws InterruptedException {
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
    public void testCacheOperationsSynchronousMultiThreaded() throws Exception, InterruptedException {

        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        // Run a set of threads, that attempt to fetch the elements
        final List executables = new ArrayList();

        executables.add(new ClusterExecutable(manager1, "sampleCache3"));
        executables.add(new ClusterExecutable(manager2, "sampleCache3"));
//        executables.add(new ClusterExecutable(manager3, "sampleCache3"));

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
    public void testCacheOperationsAynchronousMultiThreaded() throws Exception, InterruptedException {

        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        // Run a set of threads, that attempt to fetch the elements
        final List executables = new ArrayList();

        executables.add(new ClusterExecutable(manager1, "sampleCache2"));
        executables.add(new ClusterExecutable(manager2, "sampleCache2"));
//        executables.add(new ClusterExecutable(manager3, "sampleCache2"));

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
                Integer key = new Integer((i));
                int operationSelector = random.nextInt(4);
                Cache cache = manager.getCache(cacheName);
                if (operationSelector == 100) {
                    cache.get(key);
                    LOG.info(cache.getGuid() + ": get " + key);
                } else if (operationSelector == 100) {
                    cache.remove(key);
                    LOG.info(cache.getGuid() + ": remove " + key);
                } else if (operationSelector == 2) {
                    cache.put(new Element(key,
                            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                    + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                    + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                    + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                    + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                    LOG.info(cache.getGuid() + ": put " + key);
                } else {
                    //every twelfth time 1/4 * 1/3 = 1/12
                    if (random.nextInt(3) == 1) {
                        LOG.info("cache.removeAll()");
                        cache.removeAll();
                    }
                }
            }

        }
    }

}

package net.sf.ehcache.distribution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.StopWatch;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import net.sf.ehcache.management.ManagementService;
import net.sf.ehcache.util.RetryAssert;

import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.sf.ehcache.distribution.AbstractRMITest.getActiveReplicationThreads;
import static org.junit.Assert.assertThat;

/**
 * @author Alex Snaps
 */
public class RMICacheReplicatorPerfTest extends AbstractRMITest {


    private static final Logger LOG = LoggerFactory.getLogger(RMICacheReplicatorPerfTest.class.getName());

    private static final String DEFAULT_TEST_CACHE = "sampleCache1";

    private static List<CacheManager> createCluster(int size, String ... caches){
        LOG.info("Creating Cluster");
        Collection<String> required = Arrays.asList(caches);
        List<Configuration> configurations = new ArrayList<Configuration>(size);
        for (int i = 1; i <= size; i++) {
            Configuration config = ConfigurationFactory.parseConfiguration(RMICacheReplicatorPerfTest.class.getResource("/ehcache-perf-distributed" + i + ".xml")).name("cm" + i);
            if (!required.isEmpty()) {
                for (Iterator<Map.Entry<String, CacheConfiguration>> it = config.getCacheConfigurations().entrySet().iterator(); it.hasNext(); ) {
                    if (!required.contains(it.next().getKey())) {
                        it.remove();
                    }
                }
            }
            configurations.add(config);
        }
        LOG.info("Created Configurations");

        List<CacheManager> members = startupManagers(configurations);
        try {
          LOG.info("Created Managers");
          if (required.isEmpty()) {
              waitForClusterMembership(120, TimeUnit.SECONDS, members);
              LOG.info("Cluster Membership Complete");
              emptyCaches(120, TimeUnit.SECONDS, members);
              LOG.info("Caches Emptied");
          } else {
              waitForClusterMembership(120, TimeUnit.SECONDS, required, members);
              LOG.info("Cluster Membership Complete");
              emptyCaches(120, TimeUnit.SECONDS, required, members);
              LOG.info("Caches Emptied");
          }
          return members;
        } catch (RuntimeException e) {
          destroyCluster(members);
          throw e;
        } catch (Error e) {
          destroyCluster(members);
          throw e;
        }
    }

    private static void destroyCluster(List<CacheManager> members) {
        for (CacheManager manager : members) {
            if (manager != null) {
                manager.shutdown();
            }
        }
    }

    /**
     * {@inheritDoc}
     * Sets up two caches: cache1 is local. cache2 is to be receive updates
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        MulticastKeepaliveHeartbeatSender.setHeartBeatInterval(1000);
        assertThat(getActiveReplicationThreads(), IsEmptyCollection.<Thread>empty());
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    @After
    public void noReplicationThreads() throws Exception {
        RetryAssert.assertBy(30, TimeUnit.SECONDS, new Callable<Set<Thread>>() {
            @Override
            public Set<Thread> call() throws Exception {
                return getActiveReplicationThreads();
            }
        }, IsEmptyCollection.<Thread>empty());
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
        List<CacheManager> cluster = createCluster(5, DEFAULT_TEST_CACHE);
        try {
            final Ehcache cache1 = cluster.get(0).getEhcache(DEFAULT_TEST_CACHE);

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

            for (CacheManager manager : cluster) {
              RetryAssert.assertBy(2, TimeUnit.SECONDS, RetryAssert.sizeOf(manager.getCache(DEFAULT_TEST_CACHE)), Is.is(2000));
            }
        } finally {
            destroyCluster(cluster);
        }
    }


    /**
     * Performance and capacity tests.
     * <p/>
     */

    @Test
    public void testBootstrap() throws CacheException, InterruptedException, RemoteException {
        List<CacheManager> cluster = createCluster(5, DEFAULT_TEST_CACHE);
        try {
            final Ehcache cache1 = cluster.get(0).getEhcache(DEFAULT_TEST_CACHE);
            
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

            for (CacheManager manager : cluster) {
              RetryAssert.assertBy(7, TimeUnit.SECONDS, RetryAssert.sizeOf(manager.getCache(DEFAULT_TEST_CACHE)), Is.is(2000));
            }

            //now test bootstrap
            cluster.get(0).addCache("bootStrapResults");
            Cache cache = cluster.get(0).getCache("bootStrapResults");
            List cachePeers = cluster.get(0).getCacheManagerPeerProvider("RMI").listRemoteCachePeers(cache1);
            CachePeer cachePeer = (CachePeer) cachePeers.get(0);

            List keys = cachePeer.getKeys();
            assertEquals(2000, keys.size());

            Element firstElement = cachePeer.getQuiet((Serializable) keys.get(0));
            long size = firstElement.getSerializedSize();
            assertEquals(517, size);

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
        } finally {
            destroyCluster(cluster);
        }
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
        List<CacheManager> cluster = createCluster(5, DEFAULT_TEST_CACHE);
        try {
            final Ehcache cache1 = cluster.get(0).getEhcache(DEFAULT_TEST_CACHE);
            
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

            for (CacheManager manager : cluster) {
              RetryAssert.assertBy(100, TimeUnit.SECONDS, RetryAssert.sizeOf(manager.getCache(DEFAULT_TEST_CACHE)), Is.is(20000));
            }
        } finally {
            destroyCluster(cluster);
        }
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
        List<CacheManager> cluster = createCluster(5, DEFAULT_TEST_CACHE);
        try {
            final Ehcache cache1 = cluster.get(0).getEhcache(DEFAULT_TEST_CACHE);
            
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
                cluster.get(1).getCache(DEFAULT_TEST_CACHE),
                cluster.get(2).getCache(DEFAULT_TEST_CACHE),
                cluster.get(3).getCache(DEFAULT_TEST_CACHE),
                cluster.get(4).getCache(DEFAULT_TEST_CACHE) };

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
        } finally {
            destroyCluster(cluster);
        }
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
        List<CacheManager> cluster = createCluster(5, "sampleCache3");
        try {
            //Give everything a chance to startup
            StopWatch stopWatch = new StopWatch();
            Integer index;
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 1000; j++) {
                    index = Integer.valueOf(((1000 * i) + j));
                    cluster.get(0).getCache("sampleCache3").put(new Element(index,
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

            for (CacheManager manager : cluster) {
                assertThat(manager.getName(), manager.getCache("sampleCache3").getSize(), Is.is(2000));
            }
        } finally {
            destroyCluster(cluster);
        }
    }

    /**
     * Enables long stabilty runs using replication to be done.
     * <p/>
     * This test has been run in a profile for 15 hours without any observed issues.
     *
     * @throws InterruptedException
     */
    public void manualStabilityTest() throws InterruptedException {
        List<CacheManager> cluster = createCluster(5, DEFAULT_TEST_CACHE);
        try {
            AbstractCacheTest.forceVMGrowth();

            ManagementService.registerMBeans(cluster.get(2), AbstractCacheTest.createMBeanServer(), true, true, true, true, true);
            while (true) {
                final Ehcache cache1 = cluster.get(0).getEhcache(DEFAULT_TEST_CACHE);

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

                for (CacheManager manager : cluster) {
                  RetryAssert.assertBy(2, TimeUnit.SECONDS, RetryAssert.sizeOf(manager.getCache(DEFAULT_TEST_CACHE)), Is.is(2000));
                }
            }
        } finally {
            destroyCluster(cluster);
        }
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
        List<CacheManager> cluster = createCluster(1, DEFAULT_TEST_CACHE);
        try {
            Ehcache cache1 = cluster.get(0).getEhcache(DEFAULT_TEST_CACHE);
            long start = System.nanoTime();
            final String keyBase = Long.toString(start);
            int count = 0;

            for (int i = 0; i < 100000; i++) {
                final String key = keyBase + ':' + Integer.toString((int) (Math.random() * 1000.0));
                cache1.put(new Element(key, "My Test"));
                cache1.get(key);
                cache1.remove(key);
                count++;

                final long end = System.nanoTime();
                if (end - start >= TimeUnit.SECONDS.toNanos(1)) {
                    start = end;
                    LOG.info("Items written: " + count);
                    //make sure it does not choke
                    assertTrue("Got only to " + count + " in 1 second!", count > 1000);
                    count = 0;
                }
            }
        } finally {
            destroyCluster(cluster);
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
        try {
            replicatorTest.manualStabilityTest();
        } finally {
            replicatorTest.noReplicationThreads();
        }
    }


}

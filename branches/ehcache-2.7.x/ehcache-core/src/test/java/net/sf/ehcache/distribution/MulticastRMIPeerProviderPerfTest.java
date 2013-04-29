package net.sf.ehcache.distribution;

import java.rmi.RemoteException;
import java.util.List;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.StopWatch;
import net.sf.ehcache.config.ConfigurationFactory;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alex Snaps
 */
public class MulticastRMIPeerProviderPerfTest {

    private static final Logger LOG = LoggerFactory.getLogger(MulticastRMIPeerProviderPerfTest.class.getName());

    /**
     * Cache Manager 1
     */
    protected CacheManager manager1;
    /**
     * Cache Manager 2
     */
    protected CacheManager manager2;
    /**
     * Cache Manager 3
     */
    protected CacheManager manager3;

    /**
     * {@inheritDoc}
     */
    @Before
    public void setUp() throws Exception {
        MulticastKeepaliveHeartbeatSender.setHeartBeatInterval(1000);
        manager1 = new CacheManager(ConfigurationFactory.parseConfiguration(
                MulticastRMIPeerProviderPerfTest.class.getResource("/ehcache-perf-distributed1.xml")).name("cm-1"));
        manager2 = new CacheManager(ConfigurationFactory.parseConfiguration(
                MulticastRMIPeerProviderPerfTest.class.getResource("/ehcache-perf-distributed2.xml")).name("cm-2"));
        manager3 = new CacheManager(ConfigurationFactory.parseConfiguration(
                MulticastRMIPeerProviderPerfTest.class.getResource("/ehcache-perf-distributed3.xml")).name("cm-3"));

        // wait for cluster to establish
        Thread.sleep(2000);
    }

    /**
     * Tests the speed of remotely looking up.
     *
     * @throws java.rmi.RemoteException
     * @throws InterruptedException .19ms
     *             This seems to imply a maximum of 5000 per second best case. Not bad.
     */
    @Test
    public void testRemoteGetName() throws RemoteException, InterruptedException {

        Ehcache m1sampleCache1 = manager1.getCache("sampleCache1");
        Thread.sleep(2000);
        List peerUrls = manager1.getCacheManagerPeerProvider("RMI").listRemoteCachePeers(m1sampleCache1);

        CachePeer m1SampleCach1Peer = (CachePeer) peerUrls.get(0);

        for (int i = 0; i < 100; i++) {
            m1SampleCach1Peer.getName();
        }
        Thread.sleep(2000);

        StopWatch stopWatch = new StopWatch();
        for (int i = 0; i < 1000; i++) {
            m1SampleCach1Peer.getName();
        }
        long time = stopWatch.getElapsedTime();

        LOG.info("Remote name lookup time in ms: " + time / 1000f);

    }

}

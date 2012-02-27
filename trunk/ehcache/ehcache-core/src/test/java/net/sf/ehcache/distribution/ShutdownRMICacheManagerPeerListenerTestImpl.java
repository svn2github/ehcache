package net.sf.ehcache.distribution;

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Status;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Alex Snaps
 */
public class ShutdownRMICacheManagerPeerListenerTestImpl extends RMICacheManagerPeerListenerTest {
    /**
     * Does the RMI listener stop?
     * <p/>
     * This test does not actually do test the shutdown hook automatically. But you should be able to
     * see "VM shutting down with the RMICacheManagerPeerListener for localhost still active. Calling dispose..."
     * in the log with FINE level when this test is run individually or as the last test in the run. i.e. on VM shutdown.
     */
    @Test
    public void testListenerShutsdownFromShutdownHook() {
        CacheManager manager = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed6.xml");
        try {
            CacheManagerPeerListener cachePeerListener = manager.getCachePeerListener("RMI");
            List cachePeers1 = cachePeerListener.getBoundCachePeers();
            assertEquals(55, cachePeers1.size());
            assertEquals(Status.STATUS_ALIVE, cachePeerListener.getStatus());
        } finally {
            /*
             * This test intentionally doesn't call shutdown - the VM should do it for us.
             */
            //manager.shutdown();
        }
    }
}

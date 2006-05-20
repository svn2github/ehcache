/**
 *  Copyright 2003-2006 Greg Luck
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

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.StopWatch;
import net.sf.ehcache.event.CountingCacheEventListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Tests replication of Cache events with 
 * JNDIRMICacheManagerPeerListener and JNDIManualRMICacheManagerPeerProvider.
 * <p>
 * MockContextFactory and MockContext are used to mock JNDI.
 * @see RMICacheReplicatorTest
 * @author Andy McNutt
 * @author Greg Luck
 * @version $Id$
 */
public class JNDICacheReplicatorTest extends TestCase {
    
    private static final Log LOG = LogFactory.getLog(JNDICacheReplicatorTest.class.getName());
    
    private static final boolean ASYNCHRONOUS = true;
    private static final boolean SYNCHRONOUS = false;
    
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
    protected Cache cache1;

    /**
     * CacheManager 2 of 2s cache being replicated
     */
    protected Cache cache2;


    /**
     * {@inheritDoc}
     * Sets up two caches: cache1 is local. cache2 is to be receive updates
     *
     * @throws Exception 
     */
    protected void setUp() throws Exception {
        LOG.debug("setup start");
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, 
                MockContextFactory.class.getName());
        new MockContextFactory().clear();
        CountingCacheEventListener.resetCounters();
        manager1 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR 
                + "distribution/ehcache-distributed-jndi1.xml");
        manager2 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR 
                + "distribution/ehcache-distributed-jndi2.xml");
        manager3 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR 
                + "distribution/ehcache-distributed-jndi3.xml");
        manager4 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR 
                + "distribution/ehcache-distributed-jndi4.xml");
        manager5 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR 
                + "distribution/ehcache-distributed-jndi5.xml");

//        manager6 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR
//                + "distribution/ehcache-distributed-jndi6.xml");

        cache1 = manager1.getCache(cacheName);
        
        cache1.removeAll();

        cache2 = manager2.getCache(cacheName);
        cache2.removeAll();

        //allow cluster to be established
        Thread.sleep(6000);
        LOG.debug("setup end");
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    protected void tearDown() throws Exception {
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
    }

    /**
     * 5 cache managers should means that each cache has four remote peers
     */
    public void testRemoteCachePeersEqualsNumberOfCacheManagersInCluster()
            throws NamingException, Exception {
            CacheManagerPeerProvider provider = manager1.getCachePeerProvider();
            List remotePeersOfCache1 = provider.listRemoteCachePeers(cache1);
            assertEquals(4, remotePeersOfCache1.size());
    }

    /**
     * Does a new cache manager in the cluster get detected?
     */
    public void testRemoteCachePeersDetectsNewCacheManager() throws InterruptedException {
            CacheManagerPeerProvider provider = manager1.getCachePeerProvider();
            List remotePeersOfCache1 = provider.listRemoteCachePeers(cache1);
            assertEquals(4, remotePeersOfCache1.size());

            // Add new CacheManager to cluster
            manager6 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed-jndi6.xml");

            // Allow detection to occur
            Thread.sleep(1010);

            remotePeersOfCache1 = provider.listRemoteCachePeers(cache1);
            assertEquals(5, remotePeersOfCache1.size());
    }

    /**
     * Does a down cache manager in the cluster get removed?
     */
    public void testRemoteCachePeersDetectsDownCacheManager() 
            throws InterruptedException, Exception {
        try {
            
            // manager1 has stashContexts=false, stashRemoteCachePeers=false
            // so it looks up the peers each call of listRemoteCachePeers
            CacheManagerPeerProvider providerManager1 = manager1.getCachePeerProvider();
            List remotePeersOfCache1 = providerManager1.listRemoteCachePeers(cache1);
            int targetNumberPeers = 4;
            assertEquals(remotePeersOfCache1.size() 
                    + " remotePeersOfCache1 should be " + targetNumberPeers
                    + " for manager1, remotePeersOfCache1=" + remotePeersOfCache1
                    + "  " + getContextFactory(),
                    targetNumberPeers, remotePeersOfCache1.size());
            
            CacheManagerPeerProvider providerManager2 = manager2.getCachePeerProvider();
            remotePeersOfCache1 = providerManager2.listRemoteCachePeers(cache1);
            assertEquals(remotePeersOfCache1.size() 
                    + " remotePeersOfCache1 should be " + targetNumberPeers
                    + " for manager2, remotePeersOfCache1=" + remotePeersOfCache1
                    + "  " + getContextFactory(),
                    targetNumberPeers, remotePeersOfCache1.size());

            // Drop a CacheManager from the cluster
            manager5.shutdown();

            remotePeersOfCache1 = providerManager1.listRemoteCachePeers(cache1);
            targetNumberPeers = 3;
            assertEquals(remotePeersOfCache1.size() 
                    + " remotePeersOfCache1 should be " + targetNumberPeers
                    + " for manager1, remotePeersOfCache1=" + remotePeersOfCache1
                    + "  " + getContextFactory(),
                    targetNumberPeers, remotePeersOfCache1.size());
            
            // manager2 defaults to stashContexts=true, stashRemoteCachePeers=true
            // so it finds stashed peers each call of listRemoteCachePeers
            targetNumberPeers = 4;
            remotePeersOfCache1 = providerManager2.listRemoteCachePeers(cache1);
            assertEquals(remotePeersOfCache1.size() 
                    + " remotePeersOfCache1 should be " + targetNumberPeers
                    + " for manager2, remotePeersOfCache1=" + remotePeersOfCache1
                    + "  " + getContextFactory(),
                    targetNumberPeers, remotePeersOfCache1.size());
            
            // Drop a CacheManager from the cluster
            manager4.shutdown();
            
            remotePeersOfCache1 = providerManager1.listRemoteCachePeers(cache1);
            // didn't stash this peer either
            targetNumberPeers = 2;
            assertEquals(remotePeersOfCache1.size() 
                    + " remotePeersOfCache1 should be " + targetNumberPeers
                    + " for manager1, remotePeersOfCache1=" + remotePeersOfCache1
                    + "  " + getContextFactory(),
                    targetNumberPeers, remotePeersOfCache1.size());

            remotePeersOfCache1 = providerManager2.listRemoteCachePeers(cache1);
            // still 4 because stash peers
            targetNumberPeers = 4;
            assertEquals(remotePeersOfCache1.size() 
                    + " remotePeersOfCache1 should be " + targetNumberPeers
                    + " for manager2, remotePeersOfCache1=" + remotePeersOfCache1
                    + "  " + getContextFactory(),
                    targetNumberPeers, remotePeersOfCache1.size());
        } catch (AssertionFailedError afe) {
            LOG.error(afe.toString(), afe);
            throw afe;
        } catch (Exception ex) {
            LOG.error(ex.toString(), ex);
            throw ex;
        }
    }

    /**
     * Tests put and remove initiated from cache1 in a cluster
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    public void testPutProgagatesFromAndToEveryCacheManagerAndCache() 
            throws CacheException, InterruptedException, Exception {
        try {

            // Put
            String[] cacheNames = manager1.getCacheNames();
            LOG.debug("cacheNames.length=" + cacheNames.length 
                    + " " + Arrays.asList(cacheNames));
            int numberOfCaches = 7;
            assertEquals(numberOfCaches, cacheNames.length);
            String nonSerializableElement = "nonSerializable";
            Arrays.sort(cacheNames);
            for (int i = 0; i < cacheNames.length; i++) {
                String name = cacheNames[i];
                manager1.getCache(name)
                        .put(new Element("" + i, new Integer(i)));
                // Add some non serializable elements that should not get
                // propagated
                manager1.getCache(name).put(
                        new Element("nonSerializableElement" + i, new Object()));
            }

            waitForProgagate();

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
                Element nonSerializableElement2 = manager2.getCache(name)
                        .get(nonSerializableElement + i);
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
                LOG.debug("element propagated to cache named " + name + ": "
                        + i + " count2=" + count2 + " count3=" + count3
                        + " count4=" + count4 + " count5=" + count5);
            }
            assertEquals(numberOfCaches, count2);
            assertEquals(numberOfCaches, count3);
            assertEquals(numberOfCaches, count4);
            assertEquals(numberOfCaches, count5);
        } catch (AssertionFailedError afe) {
            LOG.error(afe.toString(), afe);
            throw afe;
        } catch (Exception ex) {
            LOG.error(ex.toString(), ex);
            throw ex;
        }
    }

    /**
     * Performance and capacity tests.
     * <p/>
     * The numbers given are for the remote peer tester (java -jar ehcache-test.jar ehcache-distributed-jndi1.xml)
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
    public void xTestBigPutsProgagatesAsynchronous() throws CacheException,
            InterruptedException, Exception {
        try {

            // Give everything a chance to startup
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
            // assertTrue(putTime < 8);
            int targetSize = 2000;
            assertEquals(targetSize, cache1.getSize());

            // Wait for slow propagate.
            Thread.sleep(90000);
            assertEquals(targetSize, manager2.getCache("sampleCache1").getSize());
            assertEquals(targetSize, manager3.getCache("sampleCache1").getSize());
            assertEquals(targetSize, manager4.getCache("sampleCache1").getSize());
            assertEquals(targetSize, manager5.getCache("sampleCache1").getSize());
        } catch (AssertionFailedError afe) {
            LOG.error(afe.toString(), afe);
            throw afe;
        } catch (Exception ex) {
            LOG.error(ex.toString(), ex);
            throw ex;
        }
    }


    /**
     * Drive everything to point of breakage within a 64MB VM.
     */
    public void xTestHugePutsBreaksAsynchronous() 
            throws CacheException, InterruptedException {

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
        assertEquals(20000, manager3.getCache("sampleCache1").getSize());
        assertEquals(20000, manager4.getCache("sampleCache1").getSize());
        assertEquals(20000, manager5.getCache("sampleCache1").getSize());
    }


    /**
     * Performance and capacity tests.
     * <p/>
     * The numbers given are for the remote peer tester (java -jar ehcache-test.jar ehcache-distributed-jndi1.xml)
     * running on a 10Mbit ethernet network and are measured from the time the peer starts receiving to when
     * it has fully received.
     * <p/>
     * 4 seconds to get all remove notifications with 6 peers, 5000 Elements and 400 byte payload
     */
    public void xTestBigRemovesProgagatesAsynchronous() throws CacheException,
            InterruptedException, Exception {
        try {

            // Give everything a chance to startup
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
            Thread.sleep(130000);
            int targetSize = 5000;
            assertEquals(targetSize, cache1.getSize());
            assertEquals(targetSize, manager2.getCache("sampleCache1").getSize());
            assertEquals(targetSize, manager3.getCache("sampleCache1").getSize());
            assertEquals(targetSize, manager4.getCache("sampleCache1").getSize());
            assertEquals(targetSize, manager5.getCache("sampleCache1").getSize());

            // Let the disk stores catch up before the next stage of the test
            Thread.sleep(2000);

            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 1000; j++) {
                    index = new Integer(((1000 * i) + j));
                    cache1.remove(index);
                }
            }

            Thread.sleep(130000);
            targetSize = 0;
            assertEquals(targetSize, cache1.getSize());
            assertEquals(targetSize, manager2.getCache("sampleCache1").getSize());
            assertEquals(targetSize, manager3.getCache("sampleCache1").getSize());
            assertEquals(targetSize, manager4.getCache("sampleCache1").getSize());
            assertEquals(targetSize, manager5.getCache("sampleCache1").getSize());
        } catch (AssertionFailedError afe) {
            LOG.error(afe.toString(), afe);
            throw afe;
        } catch (Exception ex) {
            LOG.error(ex.toString(), ex);
            throw ex;
        }
    }


    /**
     * Performance and capacity tests.
     * <p/>
     * 5 seconds to send all notifications synchronously with 5 peers, 2000 Elements and 400 byte payload
     * The numbers given below are for the remote peer tester (java -jar ehcache-test.jar ehcache-distributed-jndi1.xml)
     * running on a 10Mbit ethernet network and are measured from the time the peer starts receiving to when
     * it has fully received.
     */
    public void testBigPutsProgagatesSynchronous() 
            throws CacheException, InterruptedException {

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
        assertEquals(2000, manager3.getCache("sampleCache3").getSize());
        assertEquals(2000, manager4.getCache("sampleCache3").getSize());
        assertEquals(2000, manager5.getCache("sampleCache3").getSize());
    }


    /**
     * Test various cache configurations for cache1 - explicit setting of:
     * properties="replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true "/>
     */
    public void testPutWithExplicitReplicationConfig() 
            throws InterruptedException {       
        putTest(manager1.getCache("sampleCache1"), 
                manager2.getCache("sampleCache1"), ASYNCHRONOUS);
    }

    /**
     * Test various cache configurations for cache1 - explicit setting of:
     * properties="replicateAsynchronously=false, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true "/>
     */
    public void testPutWithExplicitReplicationSynchronousConfig() 
            throws InterruptedException {       
        putTest(manager1.getCache("sampleCache3"), 
                manager2.getCache("sampleCache3"), SYNCHRONOUS);
    }


    /**
     * Test put replicated for cache4 - no properties.
     * Defaults should be replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true
     */
    public void testPutWithEmptyReplicationPropertiesConfig() 
            throws InterruptedException {      
        putTest(manager1.getCache("sampleCache4"), 
                manager2.getCache("sampleCache4"), ASYNCHRONOUS);
    }

    /**
     * Test put replicated for cache4 - missing replicatePuts property.
     * replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true
     * should equal replicateAsynchronously=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true
     */
    public void testPutWithOneMissingReplicationPropertyConfig() 
            throws InterruptedException {       
        putTest(manager1.getCache("sampleCache5"), 
                manager2.getCache("sampleCache5"), ASYNCHRONOUS);
    }


    /**
     * Tests put and remove initiated from cache1 in a cluster
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    public void putTest(Cache fromCache, Cache toCache, boolean asynchronous) 
            throws CacheException, InterruptedException {
        Serializable key = new Date();
        Serializable value = new Date();
        Element sourceElement = new Element(key, value);

        //Put
        fromCache.put(sourceElement);
        int i = 0;

        if (asynchronous) {
            waitForProgagate();
        }

        int j = 0;

        Thread.sleep(5000);

        LOG.info("" + manager1.getCache("sampleCache1").getSize());
        LOG.info("" + manager2.getCache("sampleCache1").getSize());
        LOG.info("" + manager3.getCache("sampleCache1").getSize());
        LOG.info("" + manager4.getCache("sampleCache1").getSize());
        LOG.info("" + manager5.getCache("sampleCache1").getSize());

        //Should have been replicated to toCache.
        Element deliveredElement = toCache.get(key);
        assertEquals(sourceElement, deliveredElement);
    }

    /**
     * Checks that a put received from a remote cache notifies any registered listeners.
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    public void testRemotePutNotificationGetsToOtherListeners()
            throws CacheException, InterruptedException, Exception {
        try {
            
            // Put
            cache1.put(new Element("1", new Date()));
            cache1.put(new Element("2", new Date()));
            cache1.put(new Element("3", new Date()));

            waitForProgagate();

            // local initiating cache's counting listener should have been
            // notified
            assertEquals(3, CountingCacheEventListener.getCacheElementsPut(
                    cache1).size());
            // remote receiving caches' counting listener should have been
            // notified
            assertEquals(3, CountingCacheEventListener.getCacheElementsPut(
                    cache2).size());

            // Update
            cache1.put(new Element("1", new Date()));
            cache1.put(new Element("2", new Date()));
            cache1.put(new Element("3", new Date()));

            waitForProgagate();

            // local initiating cache's counting listener should have been
            // notified
            assertEquals(3, CountingCacheEventListener.getCacheElementsUpdated(
                    cache1).size());
            // remote receiving caches' counting listener should have been
            // notified
            assertEquals(3, CountingCacheEventListener.getCacheElementsUpdated(
                    cache2).size());

            // Remove
            cache1.remove("1");
            cache1.remove("2");
            cache1.remove("3");

            waitForProgagate();

            // local initiating cache's counting listener should have been
            // notified
            assertEquals(3, CountingCacheEventListener.getCacheElementsRemoved(
                    cache1).size());
            // remote receiving caches' counting listener should have been
            // notified
            assertEquals(3, CountingCacheEventListener.getCacheElementsRemoved(
                    cache2).size());
        } catch (AssertionFailedError afe) {
            LOG.error(afe.toString(), afe);
            throw afe;
        } catch (Exception ex) {
            LOG.error(ex.toString(), ex);
            throw ex;
        }
    }


    /**
     * Test various cache configurations for cache1 - explicit setting of:
     * properties="replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true "/>
     */
    public void testRemoveWithExplicitReplicationConfig() 
            throws InterruptedException {        
        removeTest(manager1.getCache("sampleCache1"), 
                manager2.getCache("sampleCache1"), ASYNCHRONOUS);
    }

    /**
     * Test various cache configurations for cache1 - explicit setting of:
     * properties="replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true "/>
     */
    public void testRemoveWithExplicitReplicationSynchronousConfig() 
            throws InterruptedException {
        removeTest(manager1.getCache("sampleCache3"), 
                manager2.getCache("sampleCache3"), SYNCHRONOUS);
    }

    /**
     * Test put replicated for cache4 - no properties.
     * Defaults should be replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true
     */
    public void testRemoveWithEmptyReplicationPropertiesConfig() 
            throws InterruptedException {
        removeTest(manager1.getCache("sampleCache4"), 
                manager2.getCache("sampleCache4"), ASYNCHRONOUS);
    }

    /**
     * Tests put and remove initiated from a cache to another cache in a cluster
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    public void removeTest(Cache fromCache, Cache toCache, boolean asynchronous) 
            throws CacheException, InterruptedException {
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
     * Test various cache configurations for cache1 - explicit setting of:
     * properties="replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true "/>
     */
    public void testUpdateWithExplicitReplicationConfig() throws Exception {      
        updateViaCopyTest(manager1.getCache("sampleCache1"), 
                manager2.getCache("sampleCache1"), ASYNCHRONOUS);
    }

    /**
     * Test various cache configurations for cache1 - explicit setting of:
     * properties="replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true "/>
     */
    public void testUpdateWithExplicitReplicationSynchronousConfig() 
            throws Exception {       
        updateViaCopyTest(manager1.getCache("sampleCache3"), manager2.getCache("sampleCache3"), SYNCHRONOUS);
    }


    /**
     * Test put replicated for cache4 - no properties.
     * Defaults should be replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true
     */
    public void testUpdateWithEmptyReplicationPropertiesConfig() 
            throws Exception {       
        updateViaCopyTest(manager1.getCache("sampleCache4"), 
                manager2.getCache("sampleCache4"), ASYNCHRONOUS);
    }

    /**
     * Tests put and update through copy initiated from cache1 in a cluster
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    public void updateViaCopyTest(Cache fromCache, Cache toCache, 
            boolean asynchronous) throws Exception {
        fromCache.removeAll();
        toCache.removeAll();

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
    public void testUpdateViaInvalidate() 
            throws CacheException, InterruptedException, IOException {
        cache1 = manager1.getCache("sampleCache2");
        cache1.removeAll();

        cache2 = manager2.getCache("sampleCache2");
        cache2.removeAll();

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
    
    private InitialContextFactory getContextFactory() {
        return new MockContextFactory();
    }

}

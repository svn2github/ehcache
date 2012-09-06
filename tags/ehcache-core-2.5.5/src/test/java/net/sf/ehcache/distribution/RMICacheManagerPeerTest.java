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
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.util.RetryAssert;

import org.hamcrest.collection.IsEmptyCollection;
import org.junit.After;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import java.net.SocketTimeoutException;
import java.rmi.RemoteException;
import java.rmi.UnmarshalException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import net.sf.ehcache.distribution.RmiEventMessage.RmiEventType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for RMICachePeer
 * <p/>
 * Note these tests need a live network interface running in multicast mode to work
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public class RMICacheManagerPeerTest extends AbstractRMITest {

    private static final Logger LOG = LoggerFactory.getLogger(RMICacheManagerPeerTest.class.getName());


    /**
     * manager
     */
    protected CacheManager manager;
    private final String hostName = "localhost";
    private final Integer port = Integer.valueOf(40000);
    private RMICacheManagerPeerListener peerListener;
    private Cache cache;


    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        manager = CacheManager.create(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache.xml");
        cache = new Cache("test", 10, false, false, 10, 10);

        peerListener = new RMICacheManagerPeerListener(hostName, port, Integer.valueOf(0), manager, Integer.valueOf(2000));
    }

    /**
     * Shutdown the cache
     */
    @After
    public void tearDown() throws InterruptedException {
        Thread.sleep(20);
        if (peerListener != null) {
            peerListener.dispose();
        }
        manager.shutdown();

        RetryAssert.assertBy(30, TimeUnit.SECONDS, new Callable<Set<Thread>>() {
            public Set<Thread> call() throws Exception {
                return getActiveReplicationThreads();
            }
        }, IsEmptyCollection.<Thread>empty());
    }


    /**
     * Can we create the peer using remote port of 0?
     */
    @Test
    public void testCreatePeerWithAutomaticRemotePort() throws RemoteException {
        for (int i = 0; i < 10; i++) {
            new RMICachePeer(cache, hostName, port, Integer.valueOf(0), Integer.valueOf(2000));
        }
    }


    /**
     * Can we create the peer using a specified free remote port of 45000
     */
    @Test
    public void testCreatePeerWithSpecificRemotePort() throws RemoteException {
        for (int i = 0; i < 10; i++) {
            new RMICachePeer(cache, hostName, port, Integer.valueOf(45000), Integer.valueOf(2000));
        }
    }


    /**
     * See if socket.setSoTimeout(socketTimeoutMillis) works. Should throw a SocketTimeoutException
     *
     * @throws RemoteException
     */
    @Test
    public void testFailsIfTimeoutExceeded() throws Exception {

        RMICachePeer rmiCachePeer = new SlowRMICachePeer(cache, hostName, port, Integer.valueOf(1000));
        peerListener.addCachePeer(cache.getName(), rmiCachePeer);
        peerListener.init();


        try {
            CachePeer cachePeer = new ManualRMICacheManagerPeerProvider().lookupRemoteCachePeer(rmiCachePeer.getUrl());
            cachePeer.put(new Element("1", new Date()));
            fail();
        } catch (UnmarshalException e) {
            assertEquals(SocketTimeoutException.class, e.getCause().getClass());
        }
    }

    /**
     * See if socket.setSoTimeout(socketTimeoutMillis) works.
     * Should not fail because the put takes less than the timeout.
     *
     * @throws RemoteException
     */
    @Test
    public void testWorksIfTimeoutNotExceeded() throws Exception {

        cache = new Cache("test", 10, false, false, 10, 10);
        RMICachePeer rmiCachePeer = new SlowRMICachePeer(cache, hostName, port, Integer.valueOf(2100));

        peerListener.addCachePeer(cache.getName(), rmiCachePeer);
        peerListener.init();

        CachePeer cachePeer = new ManualRMICacheManagerPeerProvider().lookupRemoteCachePeer(rmiCachePeer.getUrl());
        cachePeer.put(new Element("1", new Date()));
    }

    /**
     * Test send.
     * <p/>
     * This is a unit test because it was throwing AbstractMethodError if a method has changed signature,
     * or NoSuchMethodError is a new one is added. The problem is that rmic needs
     * to recompile the stub after any changes are made to the CachePeer source, something done by ant
     * compile but not by the IDE.
     */
    @Test
    public void testSend() throws Exception {

        cache = new Cache("test", 10, false, false, 10, 10);
        RMICachePeer rmiCachePeer = new RMICachePeer(cache, hostName, port, Integer.valueOf(0), Integer.valueOf(2100));
        manager.addCache(cache);

        peerListener.addCachePeer(cache.getName(), rmiCachePeer);
        peerListener.init();

        CachePeer cachePeer = new ManualRMICacheManagerPeerProvider().lookupRemoteCachePeer(rmiCachePeer.getUrl());
        Element element = new Element("1", new Date());
        RmiEventMessage eventMessage = new RmiEventMessage(null, RmiEventType.PUT, null, element);
        List eventMessages = new ArrayList();
        eventMessages.add(eventMessage);
        cachePeer.send(eventMessages);
    }


    /**
     * RMICachePeer that breaks in lots of interesting ways.
     */
    class SlowRMICachePeer extends RMICachePeer {

        /**
         * Constructor
         *
         * @param cache
         * @param hostName
         * @param port
         * @param socketTimeoutMillis
         * @throws RemoteException
         */
        public SlowRMICachePeer(Ehcache cache, String hostName, Integer port, Integer socketTimeoutMillis)
                throws RemoteException {
            super(cache, hostName, port, Integer.valueOf(0), socketTimeoutMillis);
        }

        /**
         * Puts an Element into the underlying cache without notifying listeners or updating statistics.
         *
         * @param element
         * @throws java.rmi.RemoteException
         * @throws IllegalArgumentException
         * @throws IllegalStateException
         */
        @Override
        public void put(Element element) throws RemoteException, IllegalArgumentException, IllegalStateException {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException exception) {
                LOG.error(exception.getMessage(), exception);
            }
        }
    }
}

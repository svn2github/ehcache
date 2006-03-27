/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 - 2004 Greg Luck.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by Greg Luck
 *       (http://sourceforge.net/users/gregluck) and contributors.
 *       See http://sourceforge.net/project/memberlist.php?group_id=93232
 *       for a list of contributors"
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "EHCache" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For written
 *    permission, please contact Greg Luck (gregluck at users.sourceforge.net).
 *
 * 5. Products derived from this software may not be called "EHCache"
 *    nor may "EHCache" appear in their names without prior written
 *    permission of Greg Luck.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL GREG LUCK OR OTHER
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by contributors
 * individuals on behalf of the EHCache project.  For more
 * information on EHCache, please see <http://ehcache.sourceforge.net/>.
 *
 */
package net.sf.ehcache.distribution;

import junit.framework.TestCase;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.AbstractCacheTest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.SocketTimeoutException;
import java.rmi.RemoteException;
import java.rmi.UnmarshalException;
import java.util.Date;

/**
 * Unit tests for RMICachePeer
 *
 * Note these tests need a live network interface running in multicast mode to work
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id: RMICacheManagerPeerTest.java,v 1.1 2006/03/09 06:38:20 gregluck Exp $
 */
public class RMICacheManagerPeerTest extends TestCase {

    private static final Log LOG = LogFactory.getLog(RMICacheManagerPeerTest.class.getName());


    /**
     * manager
     */
    protected CacheManager manager;
    private String hostName = "localhost";
    private Integer port = new Integer(40000);
    private RMICacheManagerPeerListener peerListener;
    private Cache cache;


    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    protected void setUp() throws Exception {
        manager = CacheManager.create(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache.xml");
        cache = new Cache("test", 10, false, false, 10, 10);
        peerListener = new RMICacheManagerPeerListener(hostName, port, manager, new Integer(2000));
    }

    /**
     * Shutdown the cache
     */
    protected void tearDown() throws InterruptedException {
        Thread.sleep(10);
        if (peerListener != null) {
            peerListener.dispose();
        }
        manager.shutdown();
    }


    /**
     * Can we create the peer?
     */
    public void testCreatePeer() throws RemoteException {
        for (int i = 0; i < 10; i++) {
            new RMICachePeer(cache, hostName, port, new Integer(2000));
        }
    }


    /**
     * See if socket.setSoTimeout(socketTimeoutMillis) works. Should throw a SocketTimeoutException
     *
     * @throws RemoteException
     */
    public void testFailsIfTimeoutExceeded() throws Exception {

        RMICachePeer rmiCachePeer = new SlowRMICachePeer(cache, hostName, port, new Integer(1000));
        peerListener.getCachePeers().add(rmiCachePeer);
        peerListener.init();

        try {
            CachePeer cachePeer = RMICacheManagerPeerProvider.lookupRemoteCachePeer(rmiCachePeer.getUrl());
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
    public void testWorksIfTimeoutNotExceeded() throws Exception {

        cache = new Cache("test", 10, false, false, 10, 10);
        RMICachePeer rmiCachePeer = new SlowRMICachePeer(cache, hostName, port, new Integer(2100));

        peerListener.getCachePeers().add(rmiCachePeer);
        peerListener.init();

        CachePeer cachePeer = RMICacheManagerPeerProvider.lookupRemoteCachePeer(rmiCachePeer.getUrl());
        cachePeer.put(new Element("1", new Date()));
    }


    /**
     * RMICachePeer that breaks in lots of interesting ways.
     */
    class SlowRMICachePeer extends RMICachePeer {

        /**
         * Constructor
         * @param cache
         * @param hostName
         * @param port
         * @param socketTimeoutMillis
         * @throws RemoteException
         */
        public SlowRMICachePeer(Cache cache, String hostName, Integer port, Integer socketTimeoutMillis) throws RemoteException {
            super(cache, hostName, port, socketTimeoutMillis);
        }

        /**
         * Puts an Element into the underlying cache without notifying listeners or updating statistics.
         *
         * @param element
         * @throws java.rmi.RemoteException
         * @throws IllegalArgumentException
         * @throws IllegalStateException
         */
        public void put(Element element) throws RemoteException, IllegalArgumentException, IllegalStateException {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException exception) {
                LOG.info(exception.getMessage(), exception);
            }
        }
    }
}

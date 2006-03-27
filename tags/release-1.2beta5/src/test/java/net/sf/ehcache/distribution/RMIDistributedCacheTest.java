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

import java.rmi.Naming;
import java.util.Date;

/**
 *
 * Note these tests need a live network interface running in multicast mode to work
 *
 * @author Greg Luck
 * @version $Id: RMIDistributedCacheTest.java,v 1.1 2006/03/09 06:38:20 gregluck Exp $
 */
public class RMIDistributedCacheTest extends TestCase {


    /**
     * manager
     */
    protected CacheManager manager;
    /**
     * the cache name we wish to test
     */
    private String cacheName1 = "sampleCache1";
    private String cacheName2 = "sampleCache2";
    /**
     * the cache we wish to test
     */
    private Cache sampleCache1;
    private Cache sampleCache2;


    private String hostName = "localhost";

    private Integer port = new Integer(40000);
    private Element element;
    private CachePeer cache1Peer;
    private CachePeer cache2Peer;

    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    protected void setUp() throws Exception {
        if (DistributionUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        manager = CacheManager.create(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed1.xml");
        sampleCache1 = manager.getCache(cacheName1);
        sampleCache2 = manager.getCache(cacheName2);
        sampleCache1.removeAll();
        element = new Element("key", new Date());
        sampleCache1.put(element);
        CacheManagerPeerListener cacheManagerPeerListener =
                new RMICacheManagerPeerListener(hostName, port, manager, new Integer(2000));
        cacheManagerPeerListener.init();
        cache1Peer = (CachePeer) Naming.lookup(createNamingUrl() + cacheName1);
        cache2Peer = (CachePeer) Naming.lookup(createNamingUrl() + cacheName2);
    }

    /**
     * Shutdown the cache
     */
    protected void tearDown() throws InterruptedException {
        if (DistributionUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        Thread.sleep(10);
        manager.shutdown();
    }


    /**
     * Getting an RMI Server going is a big deal
     */
    public void testCreation() throws Exception {
        if (DistributionUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        assertNotNull(cache1Peer);
        assertNotNull(cache2Peer);
    }

    /**
     * The use of one-time registry creation and Naming.rebind should mean we can create as many listeneres as we like.
     * They will simply replace the ones that were there.
     */
    public void testMultipleCreationOfRMIServers() throws Exception {
        if (DistributionUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        for (int i = 0; i < 100; i++) {
            new RMICacheManagerPeerListener(hostName, port, manager, new Integer(2000));
        }
        cache1Peer = (CachePeer) Naming.lookup(createNamingUrl() + cacheName1);
        assertNotNull(cache1Peer);
    }

    private String createNamingUrl() {
        return "//" + hostName + ":" + port + "/";
    }

    /**
     * Attempts to get the cache name
     *
     * @throws java.net.MalformedURLException
     * @throws java.rmi.NotBoundException
     * @throws java.rmi.RemoteException
     */
    public void testGetName() throws Exception {
        if (DistributionUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        String lookupCacheName = cache1Peer.getName();
        assertEquals(cacheName1, lookupCacheName);
        lookupCacheName = cache2Peer.getName();
        assertEquals(cacheName2, lookupCacheName);
    }


}

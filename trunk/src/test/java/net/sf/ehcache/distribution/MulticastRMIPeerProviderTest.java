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
import net.sf.ehcache.StopWatch;
import net.sf.ehcache.AbstractCacheTest;

import java.util.List;
import java.rmi.RemoteException;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Multicast tests. These require special machine configuration.
 * <p/>
 * Running on a single machine, as these tests do, you need to add a route command so that two multiCast sockets
 * can be added at the same time.
 * <ol>
 * <li>Mac OSX: <code>route add -net 224.0.0.0 -interface lo0</code>
 * <li>Linux (from JGroups doco, untested): <code>route add -net 224.0.0.0 netmask 224.0.0.0 dev lo</code>
 * </ol>
 *
 * @author Greg Luck
 * @version $Id: MulticastRMIPeerProviderTest.java,v 1.1 2006/03/09 06:38:20 gregluck Exp $
 */
public class MulticastRMIPeerProviderTest extends TestCase {

    private static final Log LOG = LogFactory.getLog(MulticastRMIPeerProviderTest.class.getName());

    /** Cache Manager 1 */
    protected CacheManager manager1;
    /** Cache Manager 2 */
    protected CacheManager manager2;

    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception {
        if (DistributionUtil.isSingleRMIRegistryPerVM()) {
            return;
        }
        manager1 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed1.xml");
        manager2 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed2.xml");
    }

    /**
     * {@inheritDoc}
     */
    protected void tearDown() throws Exception {

        if (DistributionUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        manager1.shutdown();
        manager2.shutdown();
    }

    /**
     * test remote cache peers
     */
    public void testProviderFromCacheManager() throws InterruptedException {

        if (DistributionUtil.isSingleRMIRegistryPerVM()) {
            return;
        }


        Cache m1sampleCache1 = manager1.getCache("sampleCache1");
        Thread.sleep(2000);

        List peerUrls = manager1.getCachePeerProvider().listRemoteCachePeers(m1sampleCache1);
        assertEquals(1, peerUrls.size());



        Cache m2sampleCache1 = manager2.getCache("sampleCache1");
        assertFalse(m1sampleCache1.getGuid().equals(m2sampleCache1.getGuid()));

        List peerUrls2 = manager2.getCachePeerProvider().listRemoteCachePeers(m2sampleCache1);
        assertEquals(1, peerUrls2.size());
    }



    /**
     * Tests the speed of remotely looking up.
     * @throws RemoteException
     * @throws InterruptedException
     * .19ms
     * This seems to imply a maximum of 5000 per second best case. Not bad.
     */
    public void testRemoteGetName() throws RemoteException, InterruptedException {

        if (DistributionUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        Cache m1sampleCache1 = manager1.getCache("sampleCache1");
        Thread.sleep(2000);
        List peerUrls = manager1.getCachePeerProvider().listRemoteCachePeers(m1sampleCache1);

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

/**
 *  Copyright 2003-2009 Terracotta, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.StopWatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Note these tests need a live network interface running in multicast mode to work
 * 
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public class PayloadUtilTest {

    private static final Logger LOG = LoggerFactory.getLogger(PayloadUtilTest.class.getName());
    private static final Random RANDOM = new Random(System.currentTimeMillis());
    private static final String RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ.";
    private CacheManager manager;

    /**
     * setup test
     * 
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        String fileName = AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-big.xml";
        manager = new CacheManager(fileName);
    }

    /**
     * Shuts down the cachemanager
     * 
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        manager.shutdown();
    }

    /**
     * The maximum Ethernet MTU is 1500 bytes.
     * <p/>
     * We want to be able to work with 100 caches
     */
    @Test
    public void testMaximumDatagram() throws IOException {
        String payload = createReferenceString();

        final byte[] compressed = PayloadUtil.gzip(payload.getBytes());

        int length = compressed.length;
        LOG.info("gzipped size: " + length);
        assertTrue("Heartbeat too big for one Datagram " + length, length <= 1500);

    }

    /**
     * 376 µs per one gzipping each time.
     * .1 µs if we compare hashCodes on the String and only gzip as necessary.
     * 
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void testGzipSanityAndPerformance() throws IOException, InterruptedException {
        String payload = createReferenceString();
        // warmup vm
        for (int i = 0; i < 10; i++) {
            byte[] compressed = PayloadUtil.gzip(payload.getBytes());
            // make sure we don't forget to close the stream
            assertTrue(compressed.length > 300);
            Thread.sleep(20);
        }
        int hashCode = payload.hashCode();
        StopWatch stopWatch = new StopWatch();
        for (int i = 0; i < 10000; i++) {
            if (hashCode != payload.hashCode()) {
                PayloadUtil.gzip(payload.getBytes());
            }
        }
        long elapsed = stopWatch.getElapsedTime();
        LOG.info("Gzip took " + elapsed / 10F + " µs");
    }

    /**
     * 169 µs per one.
     * 
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void testUngzipPerformance() throws IOException, InterruptedException {
        String payload = createReferenceString();
        int length = payload.toCharArray().length;
        byte[] original = payload.getBytes();
        int byteLength = original.length;
        assertEquals(length, byteLength);
        byte[] compressed = PayloadUtil.gzip(original);
        // warmup vm
        for (int i = 0; i < 10; i++) {
            byte[] uncompressed = PayloadUtil.ungzip(compressed);
            uncompressed.hashCode();
            assertEquals(original.length, uncompressed.length);
            Thread.sleep(20);
        }
        StopWatch stopWatch = new StopWatch();
        for (int i = 0; i < 10000; i++) {
            PayloadUtil.ungzip(compressed);
        }
        long elapsed = stopWatch.getElapsedTime();
        LOG.info("Ungzip took " + elapsed / 10000F + " µs");
    }

    private String createReferenceString() {

        String[] names = manager.getCacheNames();
        String urlBase = "//localhost.localdomain:12000/";
        StringBuilder buffer = new StringBuilder();
        for (String name : names) {
            buffer.append(urlBase);
            buffer.append(name);
            buffer.append("|");
        }
        String payload = buffer.toString();
        return payload;
    }

    @Test
    public void testBigPayload() throws RemoteException {
        List<CachePeer> bigPayloadList = new ArrayList<CachePeer>();
        // create 5000 peers, each peer having cache name between 50 - 500 char length
        int peers = 5000;
        int minCacheNameSize = 50;
        int maxCacheNameSize = 500;
        for (int i = 0; i < peers; i++) {
            bigPayloadList.add(new PayloadUtilTestCachePeer(getRandomName(minCacheNameSize, maxCacheNameSize)));
        }

        doTestBigPayLoad(bigPayloadList, 5);
        doTestBigPayLoad(bigPayloadList, 10);
        doTestBigPayLoad(bigPayloadList, 50);
        doTestBigPayLoad(bigPayloadList, 100);
        doTestBigPayLoad(bigPayloadList, 150);
        doTestBigPayLoad(bigPayloadList, 300);
        doTestBigPayLoad(bigPayloadList, 500);

        // do a big test where maximumPeersPerSend is a large value, try to accomodate all peers in one payload
        // this should result in payload breaking up by MTU size
        doTestBigPayLoad(bigPayloadList, 1000000);

        // test heartbeat won't work when single cache has very very long cacheName
        bigPayloadList.clear();
        bigPayloadList.add(new PayloadUtilTestCachePeer(getRandomName(3000, 3001)));
        List<byte[]> compressedList = PayloadUtil.createCompressedPayloadList(bigPayloadList, 150);
        assertEquals(0, compressedList.size());

    }

    private void doTestBigPayLoad(List<CachePeer> bigPayloadList, int maximumPeersPerSend) throws RemoteException {
        List<byte[]> compressedList = PayloadUtil.createCompressedPayloadList(bigPayloadList, maximumPeersPerSend);
        // the big list cannot be compressed in 1 entry
        assertTrue(compressedList.size() > 1);
        StringBuilder actual = new StringBuilder();
        for (byte[] bytes : compressedList) {
            assertTrue("One payload should not be greater than MTU, actual size: " + bytes.length + ", MTU: " + PayloadUtil.MTU,
                    bytes.length <= PayloadUtil.MTU);
            String urlList = new String(PayloadUtil.ungzip(bytes));
            String[] urls = urlList.split(PayloadUtil.URL_DELIMITER_REGEXP);
            assertTrue("Number of URL's in one payload should not exceed maximumPeersPerSend (=" + maximumPeersPerSend + "), actual: "
                    + urls.length, urls.length <= maximumPeersPerSend);

            if (bytes == compressedList.get(compressedList.size() - 1)) {
                actual.append(urlList);
            } else {
                actual.append(urlList + PayloadUtil.URL_DELIMITER);
            }
        }
        StringBuilder expected = new StringBuilder();
        for (CachePeer peer : bigPayloadList) {
            if (peer != bigPayloadList.get(bigPayloadList.size() - 1)) {
                expected.append(peer.getUrl() + PayloadUtil.URL_DELIMITER);
            } else {
                expected.append(peer.getUrl());
            }
        }
        assertEquals(expected.toString(), actual.toString());
    }

    private String getRandomName(final int minLength, final int maxLength) {
        int length = minLength + RANDOM.nextInt(maxLength - minLength);
        StringBuilder rv = new StringBuilder();
        for (int i = 0; i < length; i++) {
            rv.append(RANDOM_CHARS.charAt(RANDOM.nextInt(RANDOM_CHARS.length())));
        }
        return rv.toString();
    }

    /**
     * A test class which implements only {@link #getUrl()} to test PayloadUtil.createCompressedPayloadList()
     * 
     * @author Abhishek Sanoujam
     * 
     */
    private static class PayloadUtilTestCachePeer implements CachePeer {

        public static final String URL_BASE = "//localhost.localdomain:12000/";
        private final String cacheName;

        public PayloadUtilTestCachePeer(String cacheName) {
            this.cacheName = cacheName;
        }

        /**
         * {@inheritDoc}
         * 
         * @see net.sf.ehcache.distribution.CachePeer#getUrl()
         */
        public String getUrl() throws RemoteException {
            return URL_BASE + cacheName;
        }

        /**
         * {@inheritDoc}
         * 
         * @see net.sf.ehcache.distribution.CachePeer#getElements(java.util.List)
         */
        public List getElements(List keys) throws RemoteException {
            // no-op
            return null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see net.sf.ehcache.distribution.CachePeer#getGuid()
         */
        public String getGuid() throws RemoteException {
            // no-op
            return null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see net.sf.ehcache.distribution.CachePeer#getKeys()
         */
        public List getKeys() throws RemoteException {
            // no-op
            return null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see net.sf.ehcache.distribution.CachePeer#getName()
         */
        public String getName() throws RemoteException {
            // no-op
            return null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see net.sf.ehcache.distribution.CachePeer#getQuiet(java.io.Serializable)
         */
        public Element getQuiet(Serializable key) throws RemoteException {
            // no-op
            return null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see net.sf.ehcache.distribution.CachePeer#getUrlBase()
         */
        public String getUrlBase() throws RemoteException {
            // no-op
            return null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see net.sf.ehcache.distribution.CachePeer#put(net.sf.ehcache.Element)
         */
        public void put(Element element) throws IllegalArgumentException, IllegalStateException, RemoteException {
            // no-op

        }

        /**
         * {@inheritDoc}
         * 
         * @see net.sf.ehcache.distribution.CachePeer#remove(java.io.Serializable)
         */
        public boolean remove(Serializable key) throws IllegalStateException, RemoteException {
            // no-op
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see net.sf.ehcache.distribution.CachePeer#removeAll()
         */
        public void removeAll() throws RemoteException, IllegalStateException {
            // no-op
        }

        /**
         * {@inheritDoc}
         * 
         * @see net.sf.ehcache.distribution.CachePeer#send(java.util.List)
         */
        public void send(List eventMessages) throws RemoteException {
            // no-op

        }

    }

}

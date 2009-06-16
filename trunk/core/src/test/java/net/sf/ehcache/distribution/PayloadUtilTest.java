/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
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
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.StopWatch;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Note these tests need a live network interface running in multicast mode to work
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public class PayloadUtilTest {

    private static final Logger LOG = Logger.getLogger(PayloadUtilTest.class.getName());
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
        LOG.log(Level.INFO, "gzipped size: " + length);
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
        //warmup vm
        for (int i = 0; i < 10; i++) {
            byte[] compressed = PayloadUtil.gzip(payload.getBytes());
            //make sure we don't forget to close the stream
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
        LOG.log(Level.INFO, "Gzip took " + elapsed / 10F + " µs");
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
        //warmup vm
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
        LOG.log(Level.INFO, "Ungzip took " + elapsed / 10000F + " µs");
    }


    private String createReferenceString() {

        String[] names = manager.getCacheNames();
        String urlBase = "//localhost.localdomain:12000/";
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            buffer.append(urlBase);
            buffer.append(name);
            buffer.append("|");
        }
        String payload = buffer.toString();
        return payload;
    }


}

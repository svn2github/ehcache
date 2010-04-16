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

package net.sf.ehcache;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performance test for the Disk store
 * @author James Abley
 * @author Greg Luck
 */
public class DiskStorePerformanceTest extends AbstractCacheTest {

    private static final Logger LOG = LoggerFactory.getLogger(DiskStorePerformanceTest.class);

    /**
     * Various times recorded:
     * 24ms
     * 1076ms
     * 594ms
     * 642ms
     * @throws Exception
     */
    @Test
    public void concurrencyThroughputOfReads() throws Exception {
        Cache cache = new Cache("test", 0, MemoryStoreEvictionPolicy.LRU, true, null, false, 500, 500, true, 10000, null);
        manager.addCache(cache);
        
        /* Stick over half a GB of stuff into the DiskStore. */
        ValueObject small = new ValueObject("image/x-icon", getResource("/small.ico"));
        ValueObject large = new ValueObject("image/jpg", getResource("/large.jpg"));
        
        int total = 100000;
        
        fillCache(cache, small, large, total);
        
        /* 
         * Now simulate n concurrent requests for different keys and see how we go. As far as possible, we want them 
         * all to attempt to hit the cache at the same time, hence the use of CountDownLatch.
         */
        int clientCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(clientCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(clientCount);
        
        WorkerRequest [] requests = new WorkerRequest[clientCount];
        
        Random r = new Random();
        
        for (int i = 0, n = requests.length; i < n; ++i) {
            int key = r.nextInt(total);
            requests[i] = new WorkerRequest(key, cache, key % 2 == 1 ? small: large, startGate, endGate);
            executor.execute(requests[i]);
        }
        
        /* Let them all go at once. */
        StopWatch watch = new StopWatch();
        
        startGate.countDown();
        
        /* and run to completion. */
        endGate.await();
        
        long elapsedTime = watch.getElapsedTime();
        
        /* Check that the cache implementation is still correct, when doing any performance enhancements. */
        for (WorkerRequest request : requests) {
            assertTrue("cache behaved correctly: " + request, request.success);
        }

        LOG.info("Elapsed time ms: " + elapsedTime);

        /* Check that lots of concurrent access was reasonably quick as well. */
        assertTrue("expected to be less than 1000 but was " + elapsedTime, elapsedTime < 1500);

    }

    private void fillCache(Cache cache, ValueObject small, ValueObject large, int total) throws InterruptedException {
        for (int i = 0; i < total;) {
            cache.put(new Element(Integer.valueOf(++i), small));
            cache.put(new Element(Integer.valueOf(++i), large));
        }
        
        /* Make sure it's all stored. */
        while (cache.getStore().bufferFull()) {
            Thread.sleep(100);
        }
        
        cache.flush();
        
        Thread.sleep(1000);
    }

    private byte[] getResource(String name) throws IOException {
        InputStream in = getClass().getResourceAsStream(name);
        assertNotNull("resource does not exist: " + name, in);
        
        int read = -1;
        byte[] buff = new byte[2048];
        
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        
        while ((read = in.read(buff)) != -1) {
            result.write(buff, 0, read);
        }
        
        result.flush();
        in.close();
        
        return result.toByteArray();
    }
    
    /**
     * {@link Runnable} implementation that simulates a request coming in which hits the application-wide cache.
     * 
     * @author jabley
     *
     */
    private class WorkerRequest implements Runnable {

        private final Integer key;
        
        private final ValueObject expected;

        private final CountDownLatch startGate;

        private final CountDownLatch endGate;

        private final Cache cache;
        
        private boolean success = false;

        public WorkerRequest(Integer key, Cache cache, ValueObject expected, CountDownLatch startGate, CountDownLatch endGate) {
            this.key = key;
            this.cache = cache;
            this.expected = expected;
            this.startGate = startGate;
            this.endGate = endGate;
        }

        /**
         * {@inheritDoc}
         */
        public void run() {
            try {
                startGate.await();
                
                try {
                    Element e = cache.get(key);
                    success = this.expected.equals(e.getValue());
                } finally {
                    endGate.countDown();
                }
            } catch (InterruptedException ignore) {
                ignore.printStackTrace();
            }
            
        }
        
    }

}

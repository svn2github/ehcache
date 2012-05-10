/**
 *  Copyright Terracotta, Inc.
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

package net.sf.ehcache.constructs.locking;

import java.util.Date;
import java.util.concurrent.CyclicBarrier;

import junit.framework.TestCase;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Abhishek Sanoujam
 */
public class ExplicitLockApiTest extends TestCase {

    private static final Logger LOG = LoggerFactory.getLogger(ExplicitLockApiTest.class);

    public void testExplicitLockApi() throws Exception {
        CacheManager cm = CacheManager.create(ExplicitLockApiTest.class.getResourceAsStream("/nonstop/nonstop-config-test.xml"));
        Cache cache = cm.getCache("defaultConfig");
        basicCacheTest(cache);
        explicitApiTest(cache);
    }

    private void basicCacheTest(Cache cache) {
        debug("Basic Cache Test");
        assertNotNull(cache);
        cache.put(new Element("key", "value"));
        Element element = cache.get("key");
        assertNotNull(element);
        assertEquals("value", element.getValue());
        debug("Basic Cache Test Done");
    }

    private void explicitApiTest(Cache cache) throws Exception {
        debug("Explicit API Test");
        String key = "key";
        CyclicBarrier barrier = new CyclicBarrier(3);
        final Reader reader = new Reader(barrier, cache, key);
        final Writer writer = new Writer(barrier, cache, key);

        Thread t1 = new Thread(reader, "Reader Thread");
        Thread t2 = new Thread(writer, "Writer Thread");

        debug("Old Element for key: " + cache.get(key));
        assertNotSame("new-value", cache.get(key).getValue());

        assertFalse(writer.writeLockAcquired);
        assertFalse(writer.updatedValue);
        assertFalse(writer.writeLockReleased);
        assertFalse(writer.finished);

        assertFalse(reader.readLockAcquired);
        assertFalse(reader.assertedNewValue);
        assertFalse(reader.finished);

        t1.start();
        t2.start();

        barrier.await();

        // acquire write lock
        debug("Signalling writer to acquire write lock");
        writer.signal();
        writer.waitUntilSignalProcessed();
        assertTrue(writer.writeLockAcquired);
        assertFalse(writer.updatedValue);
        assertFalse(writer.writeLockReleased);

        // attempt read lock
        debug("Letting reader to attempt read lock");
        reader.signal();

        long start = System.currentTimeMillis();
        while (true) {
            if (System.currentTimeMillis() - start >= 5000) {
                break;
            }
            debug("Asserting Read lock call is blocked");
            assertFalse(reader.readLockAcquired);
            Thread.sleep(1000);
        }

        // update the value
        debug("Signalling writer to update value");
        writer.signal();
        writer.waitUntilSignalProcessed();
        Thread.sleep(1000);
        assertTrue(writer.writeLockAcquired);
        assertTrue(writer.updatedValue);
        assertFalse(writer.writeLockReleased);

        // assert read lock is still blocked
        start = System.currentTimeMillis();
        while (true) {
            if (System.currentTimeMillis() - start >= 5000) {
                break;
            }
            debug("Asserting Read lock call is blocked");
            assertFalse(reader.readLockAcquired);
            Thread.sleep(1000);
        }

        // release write lock
        debug("Signalling writer to release lock");
        writer.signal();
        writer.waitUntilSignalProcessed();
        assertTrue(writer.writeLockAcquired);
        assertTrue(writer.updatedValue);
        assertTrue(writer.writeLockReleased);

        // wait until reader has processed the initial attempt-read-lock signal
        reader.waitUntilSignalProcessed();
        // assert acquired read lock
        debug("Asserting read lock acquired");
        assertTrue(reader.readLockAcquired);

        // assert new updated value in reader
        debug("Letting reader check new updated value");
        reader.signal();
        reader.waitUntilSignalProcessed();
        debug("Asserting reader got new updated value");
        assertTrue(reader.assertedNewValue);

        t1.join();
        t2.join();

        assertTrue(reader.finished);
        assertTrue(writer.finished);
        assertNull(reader.error);
        assertNull(writer.error);

        debug("Explicit API Test Done");
    }

    private static void debug(String string) {
        LOG.info("[" + Thread.currentThread().getName() + "] [" + new Date().toString() + "] " + string);
    }

    private abstract static class SignalRunnable implements Runnable {
        private volatile boolean signalReceived = false;
        private volatile boolean signalProcessed = true;
        private final String name;

        public SignalRunnable(String name) {
            this.name = name;
        }

        public void waitUntilSignalProcessed() {
            while (!signalProcessed) {
                try {
                    debug("Signal[" + name + "]  not processed yet... sleeping for 1 sec");
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            debug("Last signal[" + name + "]  processed");
        }

        protected void waitUntilSignalled() throws InterruptedException {
            while (!signalReceived) {
                synchronized (this) {
                    this.wait(500);
                }
            }
            debug("Received signal[" + name + "]  to go ahead");
        }

        public void signal() {
            synchronized (this) {
                signalReceived = true;
                signalProcessed = false;
                this.notifyAll();
            }
        }

        protected void markSignalProcessed() {
            synchronized (this) {
                signalProcessed = true;
                signalReceived = false;
            }
        }
    }

    private static class Reader extends SignalRunnable {
        private final Cache cache;
        private final String key;
        private volatile Throwable error;
        private volatile boolean finished;
        private volatile boolean readLockAcquired;
        private volatile boolean assertedNewValue;
        private final CyclicBarrier barrier;

        public Reader(CyclicBarrier barrier, Cache cache, String key) {
            super("Reader");
            this.barrier = barrier;
            this.cache = cache;
            this.key = key;
        }

        public void run() {
            try {
                barrier.await();

                waitUntilSignalled();
                cache.acquireReadLockOnKey(key);
                readLockAcquired = true;
                debug("Acquired read lock");
                markSignalProcessed();

                waitUntilSignalled();
                Element element = cache.get(key);
                debug("Got element: " + element);
                cache.releaseReadLockOnKey(key);
                assertNotNull(element);
                assertEquals("new-value", element.getValue());
                assertedNewValue = true;
                markSignalProcessed();

            } catch (Throwable e) {
                e.printStackTrace();
                error = e;
            } finally {
                finished = true;
            }
        }

    }

    private static class Writer extends SignalRunnable {
        private final Cache cache;
        private final String key;
        private volatile Throwable error;
        private volatile boolean finished;
        private volatile boolean writeLockAcquired;
        private volatile boolean updatedValue;
        private volatile boolean writeLockReleased;
        private final CyclicBarrier barrier;

        public Writer(CyclicBarrier barrier, Cache cache, String key) {
            super("Writer");
            this.barrier = barrier;
            this.cache = cache;
            this.key = key;
        }

        public void run() {
            try {
                barrier.await();

                waitUntilSignalled();
                cache.acquireWriteLockOnKey(key);
                writeLockAcquired = true;
                debug("Write Lock Acquired");
                markSignalProcessed();

                waitUntilSignalled();
                debug("Old Element for key: " + cache.get(key));
                cache.put(new Element(key, "new-value"));
                updatedValue = true;
                debug("Updated value");
                debug("Updated Element for key: " + cache.get(key));
                markSignalProcessed();

                waitUntilSignalled();
                debug("Element for key: " + cache.get(key));
                cache.releaseWriteLockOnKey(key);
                writeLockReleased = true;
                debug("Write lock released");
                markSignalProcessed();

            } catch (Throwable e) {
                e.printStackTrace();
                error = e;
            } finally {
                finished = true;
            }
        }

    }

}

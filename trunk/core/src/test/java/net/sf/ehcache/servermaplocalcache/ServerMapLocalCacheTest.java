/**
 * All content copyright 2010 (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All
 * rights reserved.
 */
package net.sf.ehcache.servermaplocalcache;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;
import junit.framework.TestCase;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;

public class ServerMapLocalCacheTest extends TestCase {

    public void testServerMapLocalCacheUseCaseEviction() throws Exception {
        final TCObjectSelfFactory factory = new TCObjectSelfFactory();
        final CacheManager cacheManager = CacheManager.create(new Configuration().name("test-cm"));
        Cache ehcache = new Cache(new CacheConfiguration().name("test-cache").maxEntriesLocalHeap(2000));
        cacheManager.addCache(ehcache);

        TCObjectSelfStore tcoSelfStore = new TCObjectSelfStore(ehcache);
        final ServerMapLocalCache serverMapLocalCache = new ServerMapLocalCache(tcoSelfStore, ehcache);
        final int numMutators = 5;
        final int numReaders = 100;

        final AtomicInteger readTpsCounter = new AtomicInteger();
        final AtomicInteger nonNullReadsCounter = new AtomicInteger();
        final AtomicInteger writeTpsCounter = new AtomicInteger();
        final TestStatus testStatus = new TestStatus();

        Thread[] mutators = new Thread[numMutators];
        for (int i = 0; i < numMutators; i++) {
            mutators[i] = new Thread(new Mutator(testStatus, factory, serverMapLocalCache, writeTpsCounter), "Mutator thread - " + i);
            mutators[i].setDaemon(true);
            mutators[i].start();
        }

        Thread[] readers = new Thread[numReaders];
        for (int i = 0; i < numReaders; i++) {
            readers[i] = new Thread(new Reader(testStatus, factory, serverMapLocalCache, readTpsCounter, nonNullReadsCounter),
                    "Reader thread - " + i);
            readers[i].setDaemon(true);
            readers[i].start();
        }

        Thread reporter = new Thread(new Runnable() {
            long zeroTpsStartTime = Long.MAX_VALUE;

            public void run() {

                try {
                    final int sleepSecs = 2;
                    while (!testStatus.isStopTestRequested()) {
                        long timeInSecsSinceZeroTPS = getTimeInSecsSinceZeroTPS();
                        if (timeInSecsSinceZeroTPS > 60) {
                            throw new RuntimeException("TPS went to zero for more than 1 min");
                        }

                        int reads = readTpsCounter.getAndSet(0);
                        int nonNullReads = nonNullReadsCounter.getAndSet(0);
                        int writes = writeTpsCounter.getAndSet(0);
                        int readTps = reads / sleepSecs;
                        int writeTps = writes / sleepSecs;
                        int nonNullReadsTps = nonNullReads / sleepSecs;

                        System.out.println("============ Iteration Stats ================");
                        System.out.println("Reads: " + reads + ", Read TPS: " + readTps);
                        System.out.println("Non-null Reads: " + nonNullReads + ", Non-null Reads TPS: " + nonNullReadsTps);
                        System.out.println("Writes: " + writes + ", Write TPS: " + writeTps);
                        System.out.println("Time since zero TPS: " + timeInSecsSinceZeroTPS + " secs");
                        if (readTps == 0 && nonNullReadsTps == 0 && writeTps == 0) {
                            if (zeroTpsStartTime == Long.MAX_VALUE) {
                                zeroTpsStartTime = System.nanoTime();
                            }
                        } else {
                            zeroTpsStartTime = Long.MAX_VALUE;
                        }
                        try {
                            Thread.sleep(sleepSecs * 1000);
                        } catch (InterruptedException e) {
                            // ignored
                        }
                    }
                } catch (Throwable t) {
                    testStatus.requestStopWithError(t);
                }
            }

            private long getTimeInSecsSinceZeroTPS() {
                if (zeroTpsStartTime != Long.MAX_VALUE) {
                    long currentTime = System.nanoTime();
                    long diff = currentTime - zeroTpsStartTime;
                    Assert.assertTrue("Diff should be > 0", diff > 0);
                    return TimeUnit.NANOSECONDS.toSeconds(diff);
                } else {
                    return -1;
                }
            }

        }, "Reporter thread");
        reporter.setDaemon(true);
        reporter.start();

        // stop after 5 mins
        testStatus.stopAfterSeconds(60 * 5);
        if (testStatus.getError() != null) {
            testStatus.getError().printStackTrace();
            Assert.fail("Test failed with exception");
        }

        System.out.println("Test completed");

        cacheManager.shutdown();
    }

    private static class TestStatus {
        private final AtomicBoolean stopTest = new AtomicBoolean(false);
        private volatile Throwable error;

        public boolean isStopTestRequested() {
            return stopTest.get();
        }

        public synchronized void waitUntilStopped() {
            while (!isStopTestRequested()) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    // ignored
                }
            }
        }

        public synchronized void requestStopWithError(Throwable e) {
            error = e;
            stopTest.set(true);
            notifyAll();
        }

        public synchronized void stopAfterSeconds(long seconds) {
            try {
                Thread.sleep(seconds * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            stopTest.set(true);
        }

        public Throwable getError() {
            return error;
        }
    }

    private static class Mutator implements Runnable {

        private final TCObjectSelfFactory factory;
        private final ServerMapLocalCache serverMapLocalCache;
        private final AtomicInteger writeTps;
        private final TestStatus testStatus;

        public Mutator(TestStatus testStatus, TCObjectSelfFactory factory, ServerMapLocalCache serverMapLocalCache, AtomicInteger writeTps) {
            this.testStatus = testStatus;
            this.factory = factory;
            this.serverMapLocalCache = serverMapLocalCache;
            this.writeTps = writeTps;
        }

        public void run() {
            try {
                while (!testStatus.isStopTestRequested()) {
                    TCObjectSelf tcoSelf = factory.newTCObjectSelf();
                    serverMapLocalCache.put(tcoSelf);
                    writeTps.incrementAndGet();
                }
            } catch (Throwable t) {
                testStatus.requestStopWithError(t);
            }
        }

    }

    private static class Reader implements Runnable {
        private static final Random random = new Random(System.currentTimeMillis());

        private final TCObjectSelfFactory factory;
        private final ServerMapLocalCache serverMapLocalCache;
        private final AtomicInteger readTps;

        private final TestStatus testStatus;

        private final AtomicInteger nonNullReadsCounter;

        public Reader(TestStatus testStatus, TCObjectSelfFactory factory, ServerMapLocalCache serverMapLocalCache, AtomicInteger readTps,
                AtomicInteger nonNullReadsCounter) {
            this.testStatus = testStatus;
            this.factory = factory;
            this.serverMapLocalCache = serverMapLocalCache;
            this.readTps = readTps;
            this.nonNullReadsCounter = nonNullReadsCounter;
        }

        public void run() {
            try {
                while (!testStatus.isStopTestRequested()) {
                    long oid = random.nextInt((int) factory.getOidCounter().get());
                    TCObjectSelf tcoSelf = serverMapLocalCache.getFromTCObjectSelfStore(oid);
                    if (tcoSelf != null) {
                        nonNullReadsCounter.incrementAndGet();
                        Assert.assertEquals(oid, tcoSelf.getOid());
                        Assert.assertEquals(factory.getKeyForId(tcoSelf.getOid()), tcoSelf.getKey());
                        Assert.assertEquals(factory.getValueForId(tcoSelf.getOid()), tcoSelf.getValue());
                    }
                    readTps.incrementAndGet();
                }
            } catch (Throwable e) {
                testStatus.requestStopWithError(e);
            }
        }
    }
}

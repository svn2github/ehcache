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

package net.sf.ehcache;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Assert;
import junit.framework.TestCase;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.MemoryUnit;
import net.sf.ehcache.pool.SizeOfEngine;
import net.sf.ehcache.pool.impl.DefaultSizeOfEngine;

public class RecalculateSizeTest extends TestCase {
    private static final Random random;

    static {
        final long seed = System.currentTimeMillis();
        System.out.println("*** RANDOM SEEDED WITH " + seed + " ***");
        random = new Random(seed);
    }

    private final SizeOfEngine engine = new DefaultSizeOfEngine(1000, true);

    public void testCustomValue() {
        DynamicSizedValue value = new DynamicSizedValue('A');

        value.shrinkSize();
        long shrinkedSize = engine.sizeOf(value, null, null).getCalculated();

        value.expandSize();
        long expandedSize = engine.sizeOf(value, null, null).getCalculated();

        System.out.println("Shrinked size: " + shrinkedSize + ", expanded size: " + expandedSize);

        Assert.assertTrue(expandedSize > shrinkedSize);
        // consumes 12mb, assert diff is at least 11mb
        Assert.assertTrue(expandedSize - shrinkedSize > MemoryUnit.MEGABYTES.toBytes(11));

        value.shrinkSize();
        long shrinkedSize2 = engine.sizeOf(value, null, null).getCalculated();

        value.expandSize();
        long expandedSize2 = engine.sizeOf(value, null, null).getCalculated();
        System.out.println("Shrinked size2: " + shrinkedSize2 + ", expanded size2: " + expandedSize2);

        Assert.assertEquals(shrinkedSize, shrinkedSize2);
        Assert.assertEquals(expandedSize, expandedSize2);

        value.shrinkSize();
        value = null;
    }

    public void testRecalculateSizeGrowing() {
        CacheManager cm = createCacheManager();
        cm.addCache(createCache("test-cache-growing"));

        Cache cache = cm.getCache("test-cache-growing");
        Assert.assertEquals(0, cache.getStatistics().getLocalHeapSizeInBytes());

        DynamicSizedValue value = new DynamicSizedValue('A');
        value.shrinkSize();
        long valueShrinkedSize = engine.sizeOf(value, null, null).getCalculated();
        value.expandSize();
        long valueExpandedSize = engine.sizeOf(value, null, null).getCalculated();
        long valueSizeDiff = valueExpandedSize - valueShrinkedSize;

        value.shrinkSize();
        cache.put(new Element("key", value));
        long shrinkedSize = cache.getStatistics().getLocalHeapSizeInBytes();

        value.expandSize();
        long expandedSizeBeforeRecalculate = cache.getStatistics().getLocalHeapSizeInBytes();

        cache.recalculateSize("key");
        long expandedSize = cache.getStatistics().getLocalHeapSizeInBytes();

        long expectedExpandedSize = shrinkedSize + valueSizeDiff;

        System.out.println("Shrinked size: " + shrinkedSize + ", expandedSizeBeforeRecalculate: " + expandedSizeBeforeRecalculate
                + ", expandedSize: " + expandedSize + ", expectedExpandedSize: " + expectedExpandedSize + ", valueShrinkedSize: "
                + valueShrinkedSize + ", valueExpandedSize: " + valueExpandedSize);
        Assert.assertEquals(shrinkedSize, expandedSizeBeforeRecalculate);
        Assert.assertTrue(expandedSize > shrinkedSize);
        Assert.assertEquals(valueSizeDiff, expandedSize - expandedSizeBeforeRecalculate);
        Assert.assertEquals(expectedExpandedSize, expandedSize);

        cache.remove("key");
        cm.shutdown();
    }

    public void testRecalculateSizeShrinking() {
        CacheManager cm = createCacheManager();
        cm.addCache(createCache("test-cache-shrinking"));

        Cache cache = cm.getCache("test-cache-shrinking");
        Assert.assertEquals(0, cache.getStatistics().getLocalHeapSizeInBytes());

        DynamicSizedValue value = new DynamicSizedValue('A');
        value.shrinkSize();
        long valueShrinkedSize = engine.sizeOf(value, null, null).getCalculated();
        value.expandSize();
        long valueExpandedSize = engine.sizeOf(value, null, null).getCalculated();
        long valueSizeDiff = valueExpandedSize - valueShrinkedSize;

        value.expandSize();
        cache.put(new Element("key", value));
        long expandedSize = cache.getStatistics().getLocalHeapSizeInBytes();

        value.shrinkSize();
        long shrinkedSizeBeforeRecalculate = cache.getStatistics().getLocalHeapSizeInBytes();

        cache.recalculateSize("key");
        long shrinkedSize = cache.getStatistics().getLocalHeapSizeInBytes();

        long expectedShrinkedSize = expandedSize - valueSizeDiff;

        System.out.println("Shrinked size: " + shrinkedSize + ", shrinkedSizeBeforeRecalculate: " + shrinkedSizeBeforeRecalculate
                + ", expandedSize: " + expandedSize + ", expectedShrinkedSize: " + expectedShrinkedSize + ", valueShrinkedSize: "
                + valueShrinkedSize + ", valueExpandedSize: " + valueExpandedSize);
        Assert.assertEquals(expandedSize, shrinkedSizeBeforeRecalculate);
        Assert.assertTrue(expandedSize > shrinkedSize);
        Assert.assertEquals(valueSizeDiff, expandedSize - shrinkedSize);
        Assert.assertEquals(expectedShrinkedSize, shrinkedSize);

        cache.remove("key");
        cm.shutdown();
    }

    private Cache createCache(String name) {
        return new Cache(new CacheConfiguration().name(name));
    }

    private CacheManager createCacheManager() {
        return CacheManager.create(new Configuration().name("test-cm").maxBytesLocalHeap(40, MemoryUnit.MEGABYTES));
    }

    public void testMultipleRecalculates() throws Exception {
        System.out.println("Testing multiple recalculates...");
        final CacheManager cacheManager = createCacheManager();
        cacheManager.addCache(createCache("test-cache"));
        final Cache cache = cacheManager.getCache("test-cache");
        final AtomicBoolean stop = new AtomicBoolean(false);
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        final AtomicLong numRecalculates = new AtomicLong();

        final String key = "the-key";
        DynamicSizedValue value = new DynamicSizedValue('A');
        value.setSize(random.nextInt(100000) + 100000);
        cache.put(new Element(key, value));
        long initialInMemorySizeBytes = cache.getStatistics().getLocalHeapSizeInBytes();
        System.out.println("Initial: inMemorySizeBytes: " + initialInMemorySizeBytes);

        final int numThreads = 50;
        final Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(new Runnable() {

                public void run() {
                    try {
                        while (!stop.get()) {
                            cache.recalculateSize(key);
                            numRecalculates.incrementAndGet();
                        }
                    } catch (Throwable e) {
                        error.set(e);
                        stop.set(true);
                    }
                }

            }, "Test thread - " + i);

            threads[i].setDaemon(true);
            threads[i].start();
        }

        for (int i = 0; i < 10; i++) {
            cache.recalculateSize(key);
            long inMemorySizeBytes = cache.getStatistics().getLocalHeapSizeInBytes();
            System.out.println("initialInMemorySizeBytes: " + initialInMemorySizeBytes + ", calculatedInMemorySizeBytes: "
                    + inMemorySizeBytes + ", numRecalculates: " + numRecalculates.get());
            Assert.assertEquals(initialInMemorySizeBytes, cache.getStatistics().getLocalHeapSizeInBytes());
            Thread.sleep(1000);
        }

        stop.set(true);
        for (Thread t : threads) {
            t.join();
        }

        Assert.assertEquals(initialInMemorySizeBytes, cache.getStatistics().getLocalHeapSizeInBytes());

        cacheManager.shutdown();
    }

    public void testMultipleRecalculatesAndMutates() throws Exception {
        System.out.println("Testing multiple recalculates with mutation...");
        final CacheManager cacheManager = createCacheManager();
        cacheManager.addCache(createCache("test-cache"));
        final Cache cache = cacheManager.getCache("test-cache");
        final AtomicBoolean stop = new AtomicBoolean(false);
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        final AtomicLong numRecalculates = new AtomicLong();

        final String key = "the-key";
        DynamicSizedValue value = new DynamicSizedValue('A');
        value.setSize(random.nextInt(100000));
        cache.put(new Element(key, value));

        final int numThreads = 50;
        final Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(new Runnable() {

                public void run() {
                    try {
                        while (!stop.get()) {
                            cache.recalculateSize(key);
                            numRecalculates.incrementAndGet();
                        }
                    } catch (Throwable e) {
                        error.set(e);
                        stop.set(true);
                    }
                }

            }, "Test thread - " + i);

            threads[i].setDaemon(true);
            threads[i].start();
        }
        int numChanges = 1;
        final int maxNumChanges = 100;
        int lastValueSize = 0;
        List<ValueSizeToCalculatedSizeTuple> valueSizeToCalculatedSizeList = new ArrayList<RecalculateSizeTest.ValueSizeToCalculatedSizeTuple>();
        while (numChanges <= maxNumChanges && !stop.get()) {
            lastValueSize = random.nextInt((int) MemoryUnit.MEGABYTES.toBytes(6));
            value = new DynamicSizedValue('A');
            value.setSize(lastValueSize);
            cache.put(new Element(key, value));
            long calculatedInMemorySizeBytes = cache.getStatistics().getLocalHeapSizeInBytes();
            if (numChanges % 10 == 0) {
                System.out.println("numChanges: " + numChanges + ", numRecalculates: " + numRecalculates.get() + ", InMemorySizeBytes: "
                        + calculatedInMemorySizeBytes + ", lastValueSize: " + lastValueSize);
            }
            valueSizeToCalculatedSizeList.add(new ValueSizeToCalculatedSizeTuple(lastValueSize, calculatedInMemorySizeBytes));
            numChanges++;
        }

        for (int i = 0; i < 3; i++) {
            cache.recalculateSize(key);
            long calculatedMemorySizeBytes = cache.getStatistics().getLocalHeapSizeInBytes();
            System.out.println("calculatedMemorySizeBytes: " + calculatedMemorySizeBytes + ", lastValueSize: " + lastValueSize
                    + ", numRecalculates: " + numRecalculates.get());
            valueSizeToCalculatedSizeList.add(new ValueSizeToCalculatedSizeTuple(lastValueSize, calculatedMemorySizeBytes));
            Thread.sleep(1000);
        }

        System.out.println("Done with changing size randomly. Waiting for all test threads to finish");

        stop.set(true);
        for (Thread t : threads) {
            t.join();
        }

        cache.remove(key);
        long sizeAfterRemove = cache.getStatistics().getLocalHeapSizeInBytes();
        Assert.assertEquals(0, sizeAfterRemove);

        for (ValueSizeToCalculatedSizeTuple tuple : valueSizeToCalculatedSizeList) {
            int valueSize = tuple.valueSize;
            long calculatedMemorySizeBytes = tuple.calculatedInMemorySizeBytes;

            Assert.assertEquals(0, cache.getStatistics().getLocalHeapSizeInBytes());
            DynamicSizedValue v = new DynamicSizedValue('A');
            v.setSize(valueSize);
            cache.put(new Element(key, v));
            long actualCaculatedInMemorySize = cache.getStatistics().getLocalHeapSizeInBytes();
            System.out.println("valueSize: " + valueSize + ", expectedCalculatedMemorySizeBytes: " + calculatedMemorySizeBytes
                    + ", actual: " + actualCaculatedInMemorySize);
            Assert.assertEquals(calculatedMemorySizeBytes, actualCaculatedInMemorySize);

            cache.remove(key);
        }

        cacheManager.shutdown();
    }

    private static class ValueSizeToCalculatedSizeTuple {
        final int valueSize;
        final long calculatedInMemorySizeBytes;

        public ValueSizeToCalculatedSizeTuple(int valueSize, long calculatedInMemorySizeBytes) {
            this.valueSize = valueSize;
            this.calculatedInMemorySizeBytes = calculatedInMemorySizeBytes;
        }

    }

    public static class DynamicSizedValue {
        private volatile char[] chars;
        private final char singleChar;

        public DynamicSizedValue(char singleChar) {
            this.singleChar = singleChar;
        }

        public void shrinkSize() {
            this.chars = null;
        }

        public void expandSize() {
            setSize((int) MemoryUnit.MEGABYTES.toBytes(6));
        }

        private void setSize(int bytes) {
            this.chars = null;
            this.chars = new char[bytes];
            for (int i = 0; i < chars.length; i++) {
                chars[i] = singleChar;
            }
        }

    }

}

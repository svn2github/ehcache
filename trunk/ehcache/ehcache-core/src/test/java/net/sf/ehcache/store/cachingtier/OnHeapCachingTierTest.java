package net.sf.ehcache.store.cachingtier;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.store.CachingTier;
import net.sf.ehcache.store.LruPolicy;
import net.sf.ehcache.store.Policy;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Alex Snaps
 */
public class OnHeapCachingTierTest {

    public static final String KEY = "value";

    @Test
    public void testOnlyPopulatesOnce() throws InterruptedException, BrokenBarrierException {
        final int threadCount = Runtime.getRuntime().availableProcessors() * 2;

        final CachingTier<String, String> cache = new OnHeapCachingTier<String, String>(new CountBasedBackEnd<String, Object>(threadCount * 2));
        final AtomicBoolean failure = new AtomicBoolean();
        final AtomicInteger invocationCounter = new AtomicInteger();
        final AtomicInteger valuesRead = new AtomicInteger();
        final CyclicBarrier barrier = new CyclicBarrier(threadCount + 1);
        final Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread() {
                @Override
                public void run() {
                    try {
                        final int await = barrier.await();
                        cache.get(KEY, new Callable<String>() {
                            @Override
                            public String call() throws Exception {
                                invocationCounter.incrementAndGet();
                                return "0x" + Integer.toHexString(await);
                            }
                        }, false);
                        valuesRead.getAndIncrement();
                    } catch (Exception e) {
                        e.printStackTrace();
                        failure.set(true);
                    }
                }
            };
            threads[i].start();
        }

        barrier.await();
        for (Thread thread : threads) {
            thread.join();
        }

        assertThat(failure.get(), is(false));
        assertThat(invocationCounter.get(), is(1));
        assertThat(valuesRead.get(), is(threadCount));
    }

    @Test
    public void testEvictsAtCapacityAndNotifies() {
        final Map<String, String> evictions = new HashMap<String, String>();
        CachingTier<String, String> cache = new OnHeapCachingTier<String, String>(new NoCapacityHeapCacheBackEnd<String>());
        cache.addListener(new CachingTier.Listener<String, String>() {
            @Override
            public void evicted(final String key, final String value) {
                evictions.put(key, value);
            }
        });
        final AtomicInteger createdValues = new AtomicInteger();
        final int loops = 100;

        for (int i = 0; i < loops; i++) {
            final String key = Integer.toString(i);
            final Callable<String> source = new Callable<String>() {
                @Override
                public String call() throws Exception {
                    createdValues.incrementAndGet();
                    return key;
                }
            };
            cache.get(key, source, false);
        }
        assertThat(createdValues.get(), is(loops));
        final Set<String> keys = new HashSet<String>();

        for (int i = 0; i < loops; i++) {
            final String key = Integer.toString(i);
            if (cache.get(key, new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return null;
                }
            }, false) != null) {
                keys.add(key);
            }
        }
        assertThat(keys.size(), is(0));
        assertThat(evictions.size(), is(loops));
    }

    @Test
    public void testDoesNotLeakFaults() throws InterruptedException {

        final CountBasedBackEnd<Integer, Object> backEnd = new CountBasedBackEnd<Integer, Object>(10);
        final OnHeapCachingTier<Integer, Element> cachingTier = new OnHeapCachingTier<Integer, Element>(
            backEnd);
        cachingTier.addListener(new CachingTier.Listener<Integer, Element>() {
            @Override
            public void evicted(final Integer key, final Element value) {
                throw new RuntimeException("Just to piss you off...");
            }
        });

        final Runnable runnable = new Runnable() {
            
            ThreadLocal<Integer> counter = new ThreadLocal<Integer>() {
                @Override
                protected Integer initialValue() {
                    return 0;
                }
            };
            
            @Override
            public void run() {
                while (counter.get() < 1000) {
                    try {
                        final Integer val = counter.get();
                        counter.set(val + 1);
                        final Element element = new Element(val, val);
                        cachingTier.get(val, new Callable<Element>() {
                            @Override
                            public Element call() throws Exception {
                                if (val % 100 == 0) {
                                    throw new OutOfMemoryError("Nope, I don't want to create this for you!") {
                                        @Override
                                        public synchronized Throwable fillInStackTrace() {
                                            return null;
                                        }
                                    };
                                } else if (val % 100 == 50) {
                                    return new Element(val, val);
                                }
                                return element;
                            }
                        }, false);
                    } catch (CacheException e) {
                        // happily keep on doing whatever you were doing
                    }
                }
                System.out.println(Thread.currentThread().getName() + " is done!");
            }
        };

        Thread[] threads = new Thread[2];
        for (int i = 0, threadsLength = threads.length; i < threadsLength; i++) {
            threads[i] = new Thread(null, runnable, "Accessor thread #" + (i + 1));
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join(TimeUnit.SECONDS.toMillis(10));
            final boolean alive = thread.isAlive();
            if(alive) {
                thread.getStackTrace();
            }
            assertThat(thread.getName() + " should be done by now!", alive, is(false));
        }

        assertThat(cachingTier.getInMemorySize(), is(10));
        for (Map.Entry<Integer, Object> entry : backEnd.entrySet()) {
            assertThat(entry.getKey() + " should point to an element", entry.getValue(), instanceOf(Element.class));
        }
    }

    @Test
    public void testSupportsDynamicCapacityChanges() throws NoSuchFieldException, IllegalAccessException {
        Cache cache = new Cache(new CacheConfiguration("foo", 10));
        OnHeapCachingTier cachingTier = OnHeapCachingTier.createOnHeapCache(cache, null);
        final Field backEndField = OnHeapCachingTier.class.getDeclaredField("backEnd");
        backEndField.setAccessible(true);
        final CountBasedBackEnd backend = (CountBasedBackEnd)backEndField.get(cachingTier);
        Assert.assertThat(backend.getMaxEntriesLocalHeap(), is(10L));
        cache.getCacheConfiguration().setMaxEntriesLocalHeap(20);
        Assert.assertThat(backend.getMaxEntriesLocalHeap(), is(20L));
    }

    @Test
    @Ignore("This takes a while to run... hopefully will never be useful again though!")
    public void testCRQ302() throws InterruptedException {

        final int entriesOnHeap = 20 * 1000;
        final int population = 800 * 1000;

//        final long seed = 1392742339502L;
        final long seed = System.currentTimeMillis();
        final ConcurrentHashMap<Object, Object> evicted = new ConcurrentHashMap<Object, Object>();
//        final ConcurrentMap<Object, Object> map = new ConcurrentHashMap<Object, Object>();
//        final HeapCacheBackEnd<Object, Object> backEnd = new FakeHeapCacheBackEnd(entriesOnHeap, map);
        final HeapCacheBackEnd<Object, Object> backEnd = new CountBasedBackEnd<Object, Object>(entriesOnHeap, new LruPolicy());
        final OnHeapCachingTier<Object, Element> cachingTier = new OnHeapCachingTier<Object, Element>(
            backEnd);
        cachingTier.addListener(new CachingTier.Listener<Object, Element>() {
            @Override
            public void evicted(final Object key, final Element value) {
//                evicted.put(key, value);
//                cachingTier.remove(value.getObjectValue());
            }
        });
        Thread[] threads = new Thread[1];
        System.out.println("Running " + threads.length + " threads, seeded with " + seed);
        for (int i = 0; i < threads.length; i++) {
//            threads[i] = new Thread(new CachingTierAccessor(200000, cachingTier, seed, backEnd, evicted));
            threads[i] = new Thread(new CachingTierAccessor(population, cachingTier, seed / (i + 1), backEnd, evicted));
            threads[i].start();
        }

        long start = System.currentTimeMillis();
        while(CachingTierAccessor.gotNull == null) {
            for (Thread thread : threads) {
                thread.join(5000);
                if(thread.isAlive()) {
                    break;
                }
            }
            System.out.println("CachingTier's size: " + cachingTier.getInMemorySize());
            System.out.println("Evictions: " + evicted.size());
            if(System.currentTimeMillis() - start > TimeUnit.MINUTES.toMillis(3)) {
                CachingTierAccessor.gotNull = new UTF8ByteDataHolder("That's enough!");
            }
        }
        System.out.println("Value for key " + CachingTierAccessor.gotNull + " : " + backEnd.get(CachingTierAccessor.gotNull));

        System.out.println("Ran " + threads.length + " threads, seeded with " + seed);
        int faults = 0;
        int count = 0;
        for(int i = 0 ; i < population; i++) {
            final UTF8ByteDataHolder key = new UTF8ByteDataHolder(Integer.toString(i));
            Object e = backEnd.get(key);
            if(e != null) {
                if(!(e instanceof Element)) {
                    ++faults;
                    System.out.println(key + " maps to non-Element: " + e);
                } else {
                    ++count;
                }
            }
            e = backEnd.get((long) i);
            if(e != null) {
                if(!(e instanceof Element)) {
                    ++faults;
                    System.out.println(key + " maps to non-Element: " + e);
                } else {
                    ++count;
                }
            }
        }
        assertThat("We're leaking faults on get!!!", faults, is(0));
        assertThat("Didn't see all values!", count, is(backEnd.size()));

        count = 0;
        for (Map.Entry<Object, Object> entry : backEnd.entrySet()) {
            if(!(entry.getValue() instanceof Element)) {
                ++faults;
                System.err.println("Failed for key " + entry.getKey() + ": " + entry.getValue());
            } else {
                ++count;
            }
        }
        System.err.println("Faults: " + faults + " vs. Elements: " + (backEnd.size() - faults));
        assertThat("We're leaking faults!!!", faults, is(0));
        assertThat("Didn't see all values!", count, is(backEnd.size()));

        for(int i = 0 ; i < population; i++) {
            final UTF8ByteDataHolder key = new UTF8ByteDataHolder(Integer.toString(i));
            final Object value = new Element(key, "");
            final Object put = backEnd.putIfAbsent(key, value);
            if(put != null) {
                if(!(put instanceof Element)) {
                    ++faults;
                    System.out.println(key + " maps to non-Element: " + put);
                } else {
                    ++count;
                }
            }
        }
        assertThat("We're leaking faults through putIfAbsent ONLY!!!", faults, is(0));
    }

    private static class CachingTierAccessor implements Runnable {

        private static volatile UTF8ByteDataHolder gotNull = null;

        private final int population;
        private final OnHeapCachingTier<Object, Element> cachingTier;
        private final HeapCacheBackEnd<Object, Object> backEnd;
        private final Map<Object, Object> evicted;
        private final Random random;

        public CachingTierAccessor(final int population, final OnHeapCachingTier<Object, Element> cachingTier, long seed, final HeapCacheBackEnd<Object, Object> backEnd, Map<Object, Object> evicted) {
            this.population = population;
            this.cachingTier = cachingTier;
            this.backEnd = backEnd;
            this.evicted = evicted;
            this.random = new Random(seed);
        }

        @Override
        public void run() {
            while (gotNull == null) {
                final long longValue = random.nextInt(population);
                final UTF8ByteDataHolder key = new UTF8ByteDataHolder(Long.toString(longValue));
                switch(random.nextInt(3)) {
                    case 0:
                        cachingTier.remove(key);
                        break;
                    default:
                        final Element newE = new Element(key, longValue);
                        Element e = cachingTier.get(key, new Callable<Element>() {
                                @Override
                                public Element call() throws Exception {
                                    if (random.nextBoolean()) {
//                                        evicted.remove(key);
                                        return newE;
                                    } else return null;
                                }
                            }, true);
                        if(newE == e) {
                            cachingTier.get(e.getObjectValue(), new Callable<Element>() {
                                @Override
                                public Element call() throws Exception {
                                    return new Element(newE.getObjectValue(), newE.getObjectKey());
                                }
                            }, false);
                        }
                        final long start = System.nanoTime();
                        int loops = 0;
                        while(e == null) {
                            ++loops;
                            e = cachingTier.get(key, new Callable<Element>() {
                                @Override
                                public Element call() throws Exception {
//                                    evicted.remove(key);
                                    return newE;
                                }
                            }, true);
                            if (e == null && loops > 100) {
                                System.out.println(Thread.currentThread()
                                                       .getName() + " =>" + '\n' + "Got null for " + key + '\n' + "cachingTier.contains(): " + cachingTier
                                                       .contains(key) + '\n' + "backEnd.get(): " + backEnd.get(key) + '\n' + "evicted.contains(): " + evicted
                                                       .containsKey(key) + '\n' + "cachingTier.get(): " + cachingTier.get(key, new Callable<Element>() {
                                    @Override
                                    public Element call() throws Exception {
                                        return new Element(key, "EVIL !!!!");
                                    }
                                }, false) + '\n' + "Loops: " + loops + '\n');
                            } else if(newE == e) {
                                cachingTier.get(e.getObjectValue(), new Callable<Element>() {
                                    @Override
                                    public Element call() throws Exception {
                                        return new Element(newE.getObjectValue(), newE.getObjectKey());
                                    }
                                }, false);
                            }

                            if (System.nanoTime() - start > TimeUnit.MILLISECONDS.toNanos(500) && loops > 1) {
                                System.err.println(Thread.currentThread()
                                                       .getName() + " looped " + loops + " on key " + key);
                                gotNull = key;
                            }
                        }
                }
            }
        }
    }

    public static class UTF8ByteDataHolder implements Serializable { //}, Comparable<UTF8ByteDataHolder> {
        private static final int HASH_SEED = 1704124966;
        private static final int FNV_32_PRIME = 0x01000193;

        private final byte[] bytes;

        // Used for tests
        public UTF8ByteDataHolder(String str) {
            try {
                this.bytes = str.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new AssertionError(e);
            }
        }

        public byte[] getBytes() {
            return bytes;
        }

        public String asString() {
            return getString();
        }

        private String getString() {
            try {
                return new String(bytes, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        public String toString() {
            return asString();
        }

        @Override
        public int hashCode() {
            return computeHashCode(HASH_SEED);
        }

        protected int computeHashCode(int init) {
            int hash = init;
            for (byte b : bytes) {
                hash ^= b;
                hash *= FNV_32_PRIME;
            }
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof UTF8ByteDataHolder) {
                UTF8ByteDataHolder other = (UTF8ByteDataHolder) obj;
                return (Arrays.equals(this.bytes, other.bytes)) && this.getClass().equals(other.getClass());
            }
            return false;
        }

        public int compareTo(final UTF8ByteDataHolder o) {
            return asString().compareTo(o.asString());
        }
    }

    /**
     * No need for V here, this can hold anything, as it holds... nothing!
     * @param <K> the key type
     */
    private static class NoCapacityHeapCacheBackEnd<K> implements HeapCacheBackEnd<K, Object> {

        private EvictionCallback<K, Object> callback;

        @Override
        public Object get(final K key) {
            return null;
        }

        @Override
        public Object putIfAbsent(final K key, final Object value) {
            callback.evicted(key, value);
            return null;
        }

        @Override
        public boolean remove(final K key, final Object value) {
            return false;
        }

        @Override
        public boolean replace(final K key, final Object oldValue, final Object newValue) {
            return false;
        }

        @Override
        public Object remove(final K key) {
            return null;
        }

        @Override
        public void clear(final boolean notify) {
            // nothing to clear when you hold nothing!
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public Set<Map.Entry<K, Object>> entrySet() {
            return Collections.EMPTY_SET;
        }

        @Override
        public void registerEvictionCallback(final EvictionCallback<K, Object> callback) {
            this.callback = callback;
        }

        @Override
        public Policy getPolicy() {
            return null;
        }

        @Override
        public void setPolicy(final Policy policy) {
            // NO OP
        }

        @Override
        public void recalculateSize(final Object key) {
            // NO OP
        }

        @Override
        public boolean hasSpace() {
            return true;
        }
    }
}
